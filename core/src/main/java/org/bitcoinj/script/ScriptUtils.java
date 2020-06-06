package org.bitcoinj.script;

import com.google.common.collect.Lists;
import org.bitcoinj.core.*;
import org.bitcoinj.ecc.ECDSA;
import org.bitcoinj.ecc.TransactionSignature;
import org.bitcoinj.ecc.ECKeyBytes;
import org.bitcoinj.params.NetworkParameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.bitcoinj.script.ScriptOpCodes.*;

public class ScriptUtils {

    private static int getSigOpCount(List<ScriptChunk> chunks, boolean accurate) throws ScriptException {
        int sigOps = 0;
        int lastOpCode = OP_INVALIDOPCODE;
        for (ScriptChunk chunk : chunks) {
            if (chunk.isOpCode()) {
                switch (chunk.opcode) {
                    case OP_CHECKSIG:
                    case OP_CHECKSIGVERIFY:
                        sigOps++;
                        break;
                    case OP_CHECKMULTISIG:
                    case OP_CHECKMULTISIGVERIFY:
                        if (accurate && lastOpCode >= OP_1 && lastOpCode <= OP_16)
                            sigOps += Interpreter.decodeFromOpN(lastOpCode);
                        else
                            sigOps += 20;
                        break;
                    default:
                        break;
                }
                lastOpCode = chunk.opcode;
            }
        }
        return sigOps;
    }

    /**
     * Gets the count of regular SigOps in the script program (counting multisig ops as 20)
     */
    public static int getSigOpCount(byte[] program) throws ScriptException {
        Script script = new Script();
        try {
            script.parse(program);
        } catch (ScriptException e) {
            // Ignore errors and count up to the parse-able length
        }
        return getSigOpCount(script.chunks, false);
    }

    /**
     * Gets the count of P2SH Sig Ops in the Script scriptSig
     */
    public static long getP2SHSigOpCount(byte[] scriptSig) throws ScriptException {
        Script script = new Script();
        try {
            script.parse(scriptSig);
        } catch (ScriptException e) {
            // Ignore errors and count up to the parse-able length
        }
        for (int i = script.chunks.size() - 1; i >= 0; i--)
            if (!script.chunks.get(i).isOpCode()) {
                Script subScript = new Script();
                subScript.parse(script.chunks.get(i).data());
                return getSigOpCount(subScript.chunks, true);
            }
        return 0;
    }

    /**
     * Gets the destination address from this script, if it's in the required form (see getPubKey).
     */
    public static Addressable getToAddress(Script script, NetworkParameters params) throws ScriptException {
        return getToAddress(script, params, false);
    }

    /**
     * Gets the destination address from this script, if it's in the required form (see getPubKey).
     *
     * @param script
     * @param forcePayToPubKey If true, allow payToPubKey to be casted to the corresponding address. This is useful if you prefer
     *                         showing addresses rather than pubkeys.
     */
    public static Addressable getToAddress(Script script, NetworkParameters params, boolean forcePayToPubKey) throws ScriptException {
        if (script.isSentToAddress())
            return new BasicAddress(params, script.getPubKeyHash());
        else if (script.isPayToScriptHash())
            return fromP2SHScript(params, script);
        else if (forcePayToPubKey && script.isSentToRawPubKey())
            return new BasicAddress(params, Utils.sha256hash160(script.getPubKey()));
        else
            throw new ScriptException("Cannot cast this script to a pay-to-address type");
    }

    /** Returns an Address that represents the script hash extracted from the given scriptPubKey */
    public static Addressable fromP2SHScript(NetworkParameters params, Script scriptPubKey) {
        checkArgument(scriptPubKey.isPayToScriptHash(), "Not a P2SH script");
        return new BasicAddress(params, params.getP2SHHeader(), scriptPubKey.getPubKeyHash());
    }

    /**
     * Creates a program that requires at least N of the given keys to sign, using OP_CHECKMULTISIG.
     */
    public static byte[] createMultiSigOutputScript(int threshold, List<? extends ECKeyBytes> pubkeys) {
        checkArgument(threshold > 0);
        checkArgument(threshold <= pubkeys.size());
        checkArgument(pubkeys.size() <= 16);  // That's the max we can represent with a single opcode.
        if (pubkeys.size() > 3) {
            Interpreter.log.warn("Creating a multi-signature output that is non-standard: {} pubkeys, should be <= 3", pubkeys.size());
        }
        try {
            ByteArrayOutputStream bits = new ByteArrayOutputStream();
            bits.write(Interpreter.encodeToOpN(threshold));
            for (ECKeyBytes key : pubkeys) {
                ScriptUtil.writeBytes(bits, key.getPubKey());
            }
            bits.write(Interpreter.encodeToOpN(pubkeys.size()));
            bits.write(OP_CHECKMULTISIG);
            return bits.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    public static byte[] createInputScript(byte[] signature, byte[] pubkey) {
        try {
            // TODO: Do this by creating a Script *first* then having the script reassemble itself into bytes.
            ByteArrayOutputStream bits = new UnsafeByteArrayOutputStream(signature.length + pubkey.length + 2);
            ScriptUtil.writeBytes(bits, signature);
            ScriptUtil.writeBytes(bits, pubkey);
            return bits.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] createInputScript(byte[] signature) {
        try {
            // TODO: Do this by creating a Script *first* then having the script reassemble itself into bytes.
            ByteArrayOutputStream bits = new UnsafeByteArrayOutputStream(signature.length + 2);
            ScriptUtil.writeBytes(bits, signature);
            return bits.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of the keys required by this script, assuming a multi-sig script.
     *
     * @throws ScriptException if the script type is not understood or is pay to address or is P2SH (run this method on the "Redeem script" instead).
     * @param script
     */
    public static List<ECKeyBytes> getPubKeys(Script script) {
        if (!script.isSentToMultiSig())
            throw new ScriptException("Only usable for multisig scripts.");

        ArrayList<ECKeyBytes> result = Lists.newArrayList();
        int numKeys = Interpreter.decodeFromOpN(script.chunks.get(script.chunks.size() - 2).opcode);
        for (int i = 0; i < numKeys; i++)
            result.add(new BasicECKeyBytes(script.chunks.get(1 + i).data()));
        return result;
    }

    /**
     * Returns the index where a signature by the key should be inserted.  Only applicable to
     * a P2SH scriptSig.
     */
    public static int getSigInsertionIndex(Script script, Sha256Hash hash, byte[] signingKey) {
        // Iterate over existing signatures, skipping the initial OP_0, the final redeem script
        // and any placeholder OP_0 sigs.
        List<ScriptChunk> existingChunks = script.chunks.subList(1, script.chunks.size() - 1);
        ScriptChunk redeemScriptChunk = script.chunks.get(script.chunks.size() - 1);
        checkNotNull(redeemScriptChunk.data);
        Script redeemScript = new Script(redeemScriptChunk.data());

        int sigCount = 0;
        int myIndex = findKeyInRedeem(redeemScript, signingKey);
        for (ScriptChunk chunk : existingChunks) {
            if (chunk.opcode == OP_0) {
                // OP_0, skip
            } else {
                checkNotNull(chunk.data);
                if (myIndex < findSigInRedeem(redeemScript.chunks, chunk.data(), hash))
                    return sigCount;
                sigCount++;
            }
        }
        return sigCount;
    }

    private static int findKeyInRedeem(Script script, byte[] key) {
        checkArgument(script.chunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = Interpreter.decodeFromOpN(script.chunks.get(script.chunks.size() - 2).opcode);
        for (int i = 0; i < numKeys; i++) {
            if (Arrays.equals(script.chunks.get(1 + i).data(), key)) {
                return i;
            }
        }

        throw new IllegalStateException("Could not find matching key " + key.toString() + " in script " + script);
    }

    private static int findSigInRedeem(List<ScriptChunk> chunks, byte[] signatureBytes, Sha256Hash hash) {
        checkArgument(chunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = Interpreter.decodeFromOpN(chunks.get(chunks.size() - 2).opcode);
        TransactionSignature signature = TransactionSignature.decodeFromBitcoin(signatureBytes, true);
        for (int i = 0; i < numKeys; i++) {
            byte[] maybePubkey = chunks.get(i + 1).data();
            //this shouldn't really be here but we are trying to remove dependencies on ECKey.  Looks like this may
            //be decoding then reencoding but it's for a wallet class we plan to delete so it can stay for now.
            if (ECDSA.verify(hash.getBytes(), signatureBytes, ECDSA.CURVE.getCurve().decodePoint(maybePubkey).getEncoded()))
                return i;
        }

        throw new IllegalStateException("Could not find matching key for signature on " + hash.toString() + " sig " + Utils.HEX.encode(signatureBytes));
    }
}
