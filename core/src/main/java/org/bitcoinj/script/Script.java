/*
 * Copyright 2011 Google Inc.
 * Copyright 2012 Matt Corallo.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.script;

import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.digests.RIPEMD160Digest;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.bitcoinj.script.ScriptOpCodes.*;
import static com.google.common.base.Preconditions.*;

// TODO: Redesign this entire API to be more type safe and organised.

/**
 * <p>Programs embedded inside transactions that control redemption of payments.</p>
 *
 * <p>Bitcoin transactions don't specify what they do directly. Instead <a href="https://en.bitcoin.it/wiki/Script">a
 * small binary stack language</a> is used to define programs that when evaluated return whether the transaction
 * "accepts" or rejects the other transactions connected to it.</p>
 *
 * <p>In SPV mode, scripts are not run, because that would require all transactions to be available and lightweight
 * clients don't have that data. In full mode, this class is used to run the interpreted language. It also has
 * static methods for building scripts.</p>
 */
public class Script {

    /** Enumeration to encapsulate the type of this script. */
    public enum ScriptType {
        // Do NOT change the ordering of the following definitions because their ordinals are stored in databases.
        NO_TYPE,
        P2PKH,
        PUB_KEY,
        P2SH
    }

    /** Flags to pass to {@link Script#correctlySpends(Transaction, long, Script, Coin, Set)}.
     * Note currently only P2SH, DERSIG and NULLDUMMY are actually supported.
     */
    public enum VerifyFlag {
        P2SH, // Enable BIP16-style subscript evaluation.
        STRICTENC, // Passing a non-strict-DER signature or one with undefined hashtype to a checksig operation causes script failure.
        DERSIG, // Passing a non-strict-DER signature to a checksig operation causes script failure (softfork safe, BIP66 rule 1)
        LOW_S, // Passing a non-strict-DER signature or one with S > order/2 to a checksig operation causes script failure
        NULLDUMMY, // Verify dummy stack item consumed by CHECKMULTISIG is of zero-length.
        SIGPUSHONLY, // Using a non-push operator in the scriptSig causes script failure (softfork safe, BIP62 rule 2).
        MINIMALDATA, // Require minimal encodings for all push operations and number encodings
        DISCOURAGE_UPGRADABLE_NOPS, // Discourage use of NOPs reserved for upgrades (NOP1-10)
        CLEANSTACK, // Require that only a single stack element remains after evaluation.
        CHECKLOCKTIMEVERIFY, // Enable CHECKLOCKTIMEVERIFY operation
        ENABLESIGHASHFORKID,
        MONOLITH_ACTIVE, // May 15, 2018 Hard fork
        MAGNETIC_ACTIVE, //Nov 15 2018 Hard fork
        GENESIS_ACTIVE, // Feb 4th, 2020 Hard fork
        CHRONICLE_ACTIVE // Future Chronicle hard fork
    }
    public static final EnumSet<VerifyFlag> ALL_VERIFY_FLAGS = EnumSet.allOf(VerifyFlag.class);

    public static final EnumSet<VerifyFlag> MONOLITH_SET = EnumSet.of(VerifyFlag.MONOLITH_ACTIVE);
    public static final EnumSet<VerifyFlag> MAGNETIC_SET = EnumSet.of(VerifyFlag.MONOLITH_ACTIVE, VerifyFlag.MAGNETIC_ACTIVE);
    public static final EnumSet<VerifyFlag> GENESIS_SET = EnumSet.of(VerifyFlag.MONOLITH_ACTIVE, VerifyFlag.MAGNETIC_ACTIVE, VerifyFlag.GENESIS_ACTIVE);
    public static final EnumSet<VerifyFlag> CHRONICLE_SET = EnumSet.of(VerifyFlag.MONOLITH_ACTIVE, VerifyFlag.GENESIS_ACTIVE, VerifyFlag.CHRONICLE_ACTIVE);


    private static final Logger log = LoggerFactory.getLogger(Script.class);
    public static final long MAX_SCRIPT_ELEMENT_SIZE = 520;  // bytes
    public static final int DEFAULT_MAX_NUM_ELEMENT_SIZE = 4;
    public static final int MAX_OPCOUNT_PRE_MAGNETIC = 201;
    public static final int MAX_OPCOUNT_PRE_GENESIS = 500;
    public static final int SIG_SIZE = 75;
    /** Max number of sigops allowed in a standard p2sh redeem script */
    public static final int MAX_P2SH_SIGOPS = 15;

    // The program is a set of chunks where each element is either [opcode] or [data, data, data ...]
    protected List<ScriptChunk> chunks;
    // Unfortunately, scripts are not ever re-serialized or canonicalized when used in signature hashing. Thus we
    // must preserve the exact bytes that we read off the wire, along with the parsed form.
    protected byte[] program;

    // Creation time of the associated keys in seconds since the epoch.
    private long creationTimeSeconds;

    /** Creates an empty script that serializes to nothing. */
    private Script() {
        chunks = Lists.newArrayList();
    }

    // Used from ScriptBuilder.
    Script(List<ScriptChunk> chunks) {
        this.chunks = Collections.unmodifiableList(new ArrayList<ScriptChunk>(chunks));
        creationTimeSeconds = Utils.currentTimeSeconds();
    }

    /**
     * Construct a Script that copies and wraps the programBytes array. The array is parsed and checked for syntactic
     * validity.
     * @param programBytes Array of program bytes from a transaction.
     */
    public Script(byte[] programBytes) throws ScriptException {
        program = programBytes;
        parse(programBytes);
        creationTimeSeconds = 0;
    }

    public Script(byte[] programBytes, long creationTimeSeconds) throws ScriptException {
        program = programBytes;
        parse(programBytes);
        this.creationTimeSeconds = creationTimeSeconds;
    }

    public long getCreationTimeSeconds() {
        return creationTimeSeconds;
    }

    public void setCreationTimeSeconds(long creationTimeSeconds) {
        this.creationTimeSeconds = creationTimeSeconds;
    }

    /**
     * Returns the program opcodes as a string, for example "[1234] DUP HASH160"
     */
    @Override
    public String toString() {
        return Utils.join(chunks);
    }

    /** Returns the serialized program as a newly created byte array. */
    public byte[] getProgram() {
        try {
            // Don't round-trip as Bitcoin Core doesn't and it would introduce a mismatch.
            if (program != null)
                return Arrays.copyOf(program, program.length);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (ScriptChunk chunk : chunks) {
                chunk.write(bos);
            }
            program = bos.toByteArray();
            return program;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    /** Returns an immutable list of the scripts parsed form. Each chunk is either an opcode or data element. */
    public List<ScriptChunk> getChunks() {
        return Collections.unmodifiableList(chunks);
    }

    private static final ScriptChunk[] STANDARD_TRANSACTION_SCRIPT_CHUNKS = {
        new ScriptChunk(ScriptOpCodes.OP_DUP, null, 0),
        new ScriptChunk(ScriptOpCodes.OP_HASH160, null, 1),
        new ScriptChunk(ScriptOpCodes.OP_EQUALVERIFY, null, 23),
        new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, null, 24),
    };

    /**
     * <p>To run a script, first we parse it which breaks it up into chunks representing pushes of data or logical
     * opcodes. Then we can run the parsed chunks.</p>
     *
     * <p>The reason for this split, instead of just interpreting directly, is to make it easier
     * to reach into a programs structure and pull out bits of data without having to run it.
     * This is necessary to render the to/from addresses of transactions in a user interface.
     * Bitcoin Core does something similar.</p>
     */
    private void parse(byte[] program) throws ScriptException {
        chunks = new ArrayList<ScriptChunk>(5);   // Common size.
        ByteArrayInputStream bis = new ByteArrayInputStream(program);
        int initialSize = bis.available();
        while (bis.available() > 0) {
            int startLocationInProgram = initialSize - bis.available();
            int opcode = bis.read();

            long dataToRead = -1;
            if (opcode >= 0 && opcode < OP_PUSHDATA1) {
                // Read some bytes of data, where how many is the opcode value itself.
                dataToRead = opcode;
            } else if (opcode == OP_PUSHDATA1) {
                if (bis.available() < 1) throw new ScriptException("Unexpected end of script");
                dataToRead = bis.read();
            } else if (opcode == OP_PUSHDATA2) {
                // Read a short, then read that many bytes of data.
                if (bis.available() < 2) throw new ScriptException("Unexpected end of script");
                dataToRead = bis.read() | (bis.read() << 8);
            } else if (opcode == OP_PUSHDATA4) {
                // Read a uint32, then read that many bytes of data.
                // Though this is allowed, because its value cannot be > 520, it should never actually be used
                if (bis.available() < 4) throw new ScriptException("Unexpected end of script");
                dataToRead = ((long)bis.read()) | (((long)bis.read()) << 8) | (((long)bis.read()) << 16) | (((long)bis.read()) << 24);
            }

            ScriptChunk chunk;
            if (dataToRead == -1) {
                chunk = new ScriptChunk(opcode, null, startLocationInProgram);
            } else {
                if (dataToRead > bis.available())
                    throw new ScriptException("Push of data element that is larger than remaining data");
                byte[] data = new byte[(int)dataToRead];
                checkState(dataToRead == 0 || bis.read(data, 0, (int)dataToRead) == dataToRead);
                chunk = new ScriptChunk(opcode, data, startLocationInProgram);
            }
            // Save some memory by eliminating redundant copies of the same chunk objects.
            for (ScriptChunk c : STANDARD_TRANSACTION_SCRIPT_CHUNKS) {
                if (c.equals(chunk)) chunk = c;
            }
            chunks.add(chunk);
        }
    }

    /**
     * Returns true if this script is of the form <pubkey> OP_CHECKSIG. This form was originally intended for transactions
     * where the peers talked to each other directly via TCP/IP, but has fallen out of favor with time due to that mode
     * of operation being susceptible to man-in-the-middle attacks. It is still used in coinbase outputs and can be
     * useful more exotic types of transaction, but today most payments are to addresses.
     */
    public boolean isSentToRawPubKey() {
        return chunks.size() == 2 && chunks.get(1).equalsOpCode(OP_CHECKSIG) &&
               !chunks.get(0).isOpCode() && chunks.get(0).data.length > 1;
    }

    /**
     * Returns true if this script is of the form DUP HASH160 <pubkey hash> EQUALVERIFY CHECKSIG, ie, payment to an
     * address like 1VayNert3x1KzbpzMGt2qdqrAThiRovi8. This form was originally intended for the case where you wish
     * to send somebody money with a written code because their node is offline, but over time has become the standard
     * way to make payments due to the short and recognizable base58 form addresses come in.
     */
    public boolean isSentToAddress() {
        return chunks.size() == 5 &&
               chunks.get(0).equalsOpCode(OP_DUP) &&
               chunks.get(1).equalsOpCode(OP_HASH160) &&
               chunks.get(2).data.length == Address.LENGTH &&
               chunks.get(3).equalsOpCode(OP_EQUALVERIFY) &&
               chunks.get(4).equalsOpCode(OP_CHECKSIG);
    }

    /**
     * An alias for isPayToScriptHash.
     */
    @Deprecated
    public boolean isSentToP2SH() {
        return isPayToScriptHash();
    }

    /**
     * <p>If a program matches the standard template DUP HASH160 &lt;pubkey hash&gt; EQUALVERIFY CHECKSIG
     * then this function retrieves the third element.
     * In this case, this is useful for fetching the destination address of a transaction.</p>
     * 
     * <p>If a program matches the standard template HASH160 &lt;script hash&gt; EQUAL
     * then this function retrieves the second element.
     * In this case, this is useful for fetching the hash of the redeem script of a transaction.</p>
     * 
     * <p>Otherwise it throws a ScriptException.</p>
     *
     */
    public byte[] getPubKeyHash() throws ScriptException {
        if (isSentToAddress())
            return chunks.get(2).data;
        else if (isPayToScriptHash())
            return chunks.get(1).data;
        else
            throw new ScriptException("Script not in the standard scriptPubKey form");
    }

    /**
     * Returns the public key in this script. If a script contains two constants and nothing else, it is assumed to
     * be a scriptSig (input) for a pay-to-address output and the second constant is returned (the first is the
     * signature). If a script contains a constant and an OP_CHECKSIG opcode, the constant is returned as it is
     * assumed to be a direct pay-to-key scriptPubKey (output) and the first constant is the public key.
     *
     * @throws ScriptException if the script is none of the named forms.
     */
    public byte[] getPubKey() throws ScriptException {
        if (chunks.size() != 2) {
            throw new ScriptException("Script not of right size, expecting 2 but got " + chunks.size());
        }
        final ScriptChunk chunk0 = chunks.get(0);
        final byte[] chunk0data = chunk0.data;
        final ScriptChunk chunk1 = chunks.get(1);
        final byte[] chunk1data = chunk1.data;
        if (chunk0data != null && chunk0data.length > 2 && chunk1data != null && chunk1data.length > 2) {
            // If we have two large constants assume the input to a pay-to-address output.
            return chunk1data;
        } else if (chunk1.equalsOpCode(OP_CHECKSIG) && chunk0data != null && chunk0data.length > 2) {
            // A large constant followed by an OP_CHECKSIG is the key.
            return chunk0data;
        } else {
            throw new ScriptException("Script did not match expected form: " + this);
        }
    }

    /**
     * Retrieves the sender public key from a LOCKTIMEVERIFY transaction
     * @throws ScriptException
     */
    public byte[] getCLTVPaymentChannelSenderPubKey() throws ScriptException {
        if (!isSentToCLTVPaymentChannel()) {
            throw new ScriptException("Script not a standard CHECKLOCKTIMVERIFY transaction: " + this);
        }
        return chunks.get(8).data;
    }

    /**
     * Retrieves the recipient public key from a LOCKTIMEVERIFY transaction
     * @throws ScriptException
     */
    public byte[] getCLTVPaymentChannelRecipientPubKey() throws ScriptException {
        if (!isSentToCLTVPaymentChannel()) {
            throw new ScriptException("Script not a standard CHECKLOCKTIMVERIFY transaction: " + this);
        }
        return chunks.get(1).data;
    }

    public BigInteger getCLTVPaymentChannelExpiry() {
        if (!isSentToCLTVPaymentChannel()) {
            throw new ScriptException("Script not a standard CHECKLOCKTIMEVERIFY transaction: " + this);
        }
        //FIXME We may actually need to enforce minimal encoding here.  But we don't have access
        //to the verify flags
        return castToBigInteger(chunks.get(4).data, 5, false);
    }

    /**
     * For 2-element [input] scripts assumes that the paid-to-address can be derived from the public key.
     * The concept of a "from address" isn't well defined in Bitcoin and you should not assume the sender of a
     * transaction can actually receive coins on it. This method may be removed in future.
     */
    @Deprecated
    public Address getFromAddress(NetworkParameters params) throws ScriptException {
        return new Address(params, Utils.sha256hash160(getPubKey()));
    }

    /**
     * Gets the destination address from this script, if it's in the required form (see getPubKey).
     */
    public Address getToAddress(NetworkParameters params) throws ScriptException {
        return getToAddress(params, false);
    }

    /**
     * Gets the destination address from this script, if it's in the required form (see getPubKey).
     * 
     * @param forcePayToPubKey
     *            If true, allow payToPubKey to be casted to the corresponding address. This is useful if you prefer
     *            showing addresses rather than pubkeys.
     */
    public Address getToAddress(NetworkParameters params, boolean forcePayToPubKey) throws ScriptException {
        if (isSentToAddress())
            return new Address(params, getPubKeyHash());
        else if (isPayToScriptHash())
            return Address.fromP2SHScript(params, this);
        else if (forcePayToPubKey && isSentToRawPubKey())
            return ECKey.fromPublicOnly(getPubKey()).toAddress(params);
        else
            throw new ScriptException("Cannot cast this script to a pay-to-address type");
    }

    ////////////////////// Interface for writing scripts from scratch ////////////////////////////////

    /**
     * Writes out the given byte buffer to the output stream with the correct opcode prefix
     * To write an integer call writeBytes(out, Utils.reverseBytes(Utils.encodeMPI(val, false)));
     */
    public static void writeBytes(OutputStream os, byte[] buf) throws IOException {
        if (buf.length < OP_PUSHDATA1) {
            os.write(buf.length);
            os.write(buf);
        } else if (buf.length < 256) {
            os.write(OP_PUSHDATA1);
            os.write(buf.length);
            os.write(buf);
        } else if (buf.length < 65536) {
            os.write(OP_PUSHDATA2);
            os.write(0xFF & (buf.length));
            os.write(0xFF & (buf.length >> 8));
            os.write(buf);
        } else {
            throw new RuntimeException("Unimplemented");
        }
    }

    /** Creates a program that requires at least N of the given keys to sign, using OP_CHECKMULTISIG. */
    public static byte[] createMultiSigOutputScript(int threshold, List<ECKey> pubkeys) {
        checkArgument(threshold > 0);
        checkArgument(threshold <= pubkeys.size());
        checkArgument(pubkeys.size() <= 16);  // That's the max we can represent with a single opcode.
        if (pubkeys.size() > 3) {
            log.warn("Creating a multi-signature output that is non-standard: {} pubkeys, should be <= 3", pubkeys.size());
        }
        try {
            ByteArrayOutputStream bits = new ByteArrayOutputStream();
            bits.write(encodeToOpN(threshold));
            for (ECKey key : pubkeys) {
                writeBytes(bits, key.getPubKey());
            }
            bits.write(encodeToOpN(pubkeys.size()));
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
            writeBytes(bits, signature);
            writeBytes(bits, pubkey);
            return bits.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] createInputScript(byte[] signature) {
        try {
            // TODO: Do this by creating a Script *first* then having the script reassemble itself into bytes.
            ByteArrayOutputStream bits = new UnsafeByteArrayOutputStream(signature.length + 2);
            writeBytes(bits, signature);
            return bits.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an incomplete scriptSig that, once filled with signatures, can redeem output containing this scriptPubKey.
     * Instead of the signatures resulting script has OP_0.
     * Having incomplete input script allows to pass around partially signed tx.
     * It is expected that this program later on will be updated with proper signatures.
     */
    public Script createEmptyInputScript(@Nullable ECKey key, @Nullable Script redeemScript) {
        if (isSentToAddress()) {
            checkArgument(key != null, "Key required to create pay-to-address input script");
            return ScriptBuilder.createInputScript(null, key);
        } else if (isSentToRawPubKey()) {
            return ScriptBuilder.createInputScript(null);
        } else if (isPayToScriptHash()) {
            checkArgument(redeemScript != null, "Redeem script required to create P2SH input script");
            return ScriptBuilder.createP2SHMultiSigInputScript(null, redeemScript);
        } else {
            throw new ScriptException("Do not understand script type: " + this);
        }
    }

    /**
     * Returns a copy of the given scriptSig with the signature inserted in the given position.
     */
    public Script getScriptSigWithSignature(Script scriptSig, byte[] sigBytes, int index) {
        int sigsPrefixCount = 0;
        int sigsSuffixCount = 0;
        if (isPayToScriptHash()) {
            sigsPrefixCount = 1; // OP_0 <sig>* <redeemScript>
            sigsSuffixCount = 1;
        } else if (isSentToMultiSig()) {
            sigsPrefixCount = 1; // OP_0 <sig>*
        } else if (isSentToAddress()) {
            sigsSuffixCount = 1; // <sig> <pubkey>
        }
        return ScriptBuilder.updateScriptWithSignature(scriptSig, sigBytes, index, sigsPrefixCount, sigsSuffixCount);
    }


    /**
     * Returns the index where a signature by the key should be inserted.  Only applicable to
     * a P2SH scriptSig.
     */
    public int getSigInsertionIndex(Sha256Hash hash, ECKey signingKey) {
        // Iterate over existing signatures, skipping the initial OP_0, the final redeem script
        // and any placeholder OP_0 sigs.
        List<ScriptChunk> existingChunks = chunks.subList(1, chunks.size() - 1);
        ScriptChunk redeemScriptChunk = chunks.get(chunks.size() - 1);
        checkNotNull(redeemScriptChunk.data);
        Script redeemScript = new Script(redeemScriptChunk.data);

        int sigCount = 0;
        int myIndex = redeemScript.findKeyInRedeem(signingKey);
        for (ScriptChunk chunk : existingChunks) {
            if (chunk.opcode == OP_0) {
                // OP_0, skip
            } else {
                checkNotNull(chunk.data);
                if (myIndex < redeemScript.findSigInRedeem(chunk.data, hash))
                    return sigCount;
                sigCount++;
            }
        }
        return sigCount;
    }

    private int findKeyInRedeem(ECKey key) {
        checkArgument(chunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = Script.decodeFromOpN(chunks.get(chunks.size() - 2).opcode);
        for (int i = 0 ; i < numKeys ; i++) {
            if (Arrays.equals(chunks.get(1 + i).data, key.getPubKey())) {
                return i;
            }
        }

        throw new IllegalStateException("Could not find matching key " + key.toString() + " in script " + this);
    }

    /**
     * Returns a list of the keys required by this script, assuming a multi-sig script.
     *
     * @throws ScriptException if the script type is not understood or is pay to address or is P2SH (run this method on the "Redeem script" instead).
     */
    public List<ECKey> getPubKeys() {
        if (!isSentToMultiSig())
            throw new ScriptException("Only usable for multisig scripts.");

        ArrayList<ECKey> result = Lists.newArrayList();
        int numKeys = Script.decodeFromOpN(chunks.get(chunks.size() - 2).opcode);
        for (int i = 0 ; i < numKeys ; i++)
            result.add(ECKey.fromPublicOnly(chunks.get(1 + i).data));
        return result;
    }

    private int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        checkArgument(chunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = Script.decodeFromOpN(chunks.get(chunks.size() - 2).opcode);
        TransactionSignature signature = TransactionSignature.decodeFromBitcoin(signatureBytes, true);
        for (int i = 0 ; i < numKeys ; i++) {
            if (ECKey.fromPublicOnly(chunks.get(i + 1).data).verify(hash, signature)) {
                return i;
            }
        }

        throw new IllegalStateException("Could not find matching key for signature on " + hash.toString() + " sig " + Utils.HEX.encode(signatureBytes));
    }



    ////////////////////// Interface used during verification of transactions/blocks ////////////////////////////////

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
                        sigOps += decodeFromOpN(lastOpCode);
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

    static int decodeFromOpN(int opcode) {
        checkArgument((opcode == OP_0 || opcode == OP_1NEGATE) || (opcode >= OP_1 && opcode <= OP_16), "decodeFromOpN called on non OP_N opcode");
        if (opcode == OP_0)
            return 0;
        else if (opcode == OP_1NEGATE)
            return -1;
        else
            return opcode + 1 - OP_1;
    }

    static int encodeToOpN(int value) {
        checkArgument(value >= -1 && value <= 16, "encodeToOpN called for " + value + " which we cannot encode in an opcode.");
        if (value == 0)
            return OP_0;
        else if (value == -1)
            return OP_1NEGATE;
        else
            return value - 1 + OP_1;
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
                Script subScript =  new Script();
                subScript.parse(script.chunks.get(i).data);
                return getSigOpCount(subScript.chunks, true);
            }
        return 0;
    }

    /**
     * Returns number of signatures required to satisfy this script.
     */
    public int getNumberOfSignaturesRequiredToSpend() {
        if (isSentToMultiSig()) {
            // for N of M CHECKMULTISIG script we will need N signatures to spend
            ScriptChunk nChunk = chunks.get(0);
            return Script.decodeFromOpN(nChunk.opcode);
        } else if (isSentToAddress() || isSentToRawPubKey()) {
            // pay-to-address and pay-to-pubkey require single sig
            return 1;
        } else if (isPayToScriptHash()) {
            throw new IllegalStateException("For P2SH number of signatures depends on redeem script");
        } else {
            throw new IllegalStateException("Unsupported script type");
        }
    }

    /**
     * Returns number of bytes required to spend this script. It accepts optional ECKey and redeemScript that may
     * be required for certain types of script to estimate target size.
     */
    public int getNumberOfBytesRequiredToSpend(@Nullable ECKey pubKey, @Nullable Script redeemScript) {
        if (isPayToScriptHash()) {
            // scriptSig: <sig> [sig] [sig...] <redeemscript>
            checkArgument(redeemScript != null, "P2SH script requires redeemScript to be spent");
            return redeemScript.getNumberOfSignaturesRequiredToSpend() * SIG_SIZE + redeemScript.getProgram().length;
        } else if (isSentToMultiSig()) {
            // scriptSig: OP_0 <sig> [sig] [sig...]
            return getNumberOfSignaturesRequiredToSpend() * SIG_SIZE + 1;
        } else if (isSentToRawPubKey()) {
            // scriptSig: <sig>
            return SIG_SIZE;
        } else if (isSentToAddress()) {
            // scriptSig: <sig> <pubkey>
            int uncompressedPubKeySize = 65;
            return SIG_SIZE + (pubKey != null ? pubKey.getPubKey().length : uncompressedPubKeySize);
        } else {
            throw new IllegalStateException("Unsupported script type");
        }
    }

    /**
     * <p>Whether or not this is a scriptPubKey representing a pay-to-script-hash output. In such outputs, the logic that
     * controls reclamation is not actually in the output at all. Instead there's just a hash, and it's up to the
     * spending input to provide a program matching that hash. This rule is "soft enforced" by the network as it does
     * not exist in Bitcoin Core. It means blocks containing P2SH transactions that don't match
     * correctly are considered valid, but won't be mined upon, so they'll be rapidly re-orgd out of the chain. This
     * logic is defined by <a href="https://github.com/bitcoin/bips/blob/master/bip-0016.mediawiki">BIP 16</a>.</p>
     *
     * <p>bitcoinj does not support creation of P2SH transactions today. The goal of P2SH is to allow short addresses
     * even for complex scripts (eg, multi-sig outputs) so they are convenient to work with in things like QRcodes or
     * with copy/paste, and also to minimize the size of the unspent output set (which improves performance of the
     * Bitcoin system).</p>
     */
    public boolean isPayToScriptHash() {
        // We have to check against the serialized form because BIP16 defines a P2SH output using an exact byte
        // template, not the logical program structure. Thus you can have two programs that look identical when
        // printed out but one is a P2SH script and the other isn't! :(
        byte[] program = getProgram();
        return program.length == 23 &&
                (program[0] & 0xff) == OP_HASH160 &&
                (program[1] & 0xff) == 0x14 &&
                (program[22] & 0xff) == OP_EQUAL;
    }

    /**
     * Returns whether this script matches the format used for multisig outputs: [n] [keys...] [m] CHECKMULTISIG
     */
    public boolean isSentToMultiSig() {
        if (chunks.size() < 4) return false;
        ScriptChunk chunk = chunks.get(chunks.size() - 1);
        // Must end in OP_CHECKMULTISIG[VERIFY].
        if (!chunk.isOpCode()) return false;
        if (!(chunk.equalsOpCode(OP_CHECKMULTISIG) || chunk.equalsOpCode(OP_CHECKMULTISIGVERIFY))) return false;
        try {
            // Second to last chunk must be an OP_N opcode and there should be that many data chunks (keys).
            ScriptChunk m = chunks.get(chunks.size() - 2);
            if (!m.isOpCode()) return false;
            int numKeys = decodeFromOpN(m.opcode);
            if (numKeys < 1 || chunks.size() != 3 + numKeys) return false;
            for (int i = 1; i < chunks.size() - 2; i++) {
                if (chunks.get(i).isOpCode()) return false;
            }
            // First chunk must be an OP_N opcode too.
            if (decodeFromOpN(chunks.get(0).opcode) < 1) return false;
        } catch (IllegalArgumentException e) { // thrown by decodeFromOpN()
            return false;   // Not an OP_N opcode.
        }
        return true;
    }

    public boolean isSentToCLTVPaymentChannel() {
        if (chunks.size() != 10) return false;
        // Check that opcodes match the pre-determined format.
        if (!chunks.get(0).equalsOpCode(OP_IF)) return false;
        // chunk[1] = recipient pubkey
        if (!chunks.get(2).equalsOpCode(OP_CHECKSIGVERIFY)) return false;
        if (!chunks.get(3).equalsOpCode(OP_ELSE)) return false;
        // chunk[4] = locktime
        if (!chunks.get(5).equalsOpCode(OP_CHECKLOCKTIMEVERIFY)) return false;
        if (!chunks.get(6).equalsOpCode(OP_DROP)) return false;
        if (!chunks.get(7).equalsOpCode(OP_ENDIF)) return false;
        // chunk[8] = sender pubkey
        if (!chunks.get(9).equalsOpCode(OP_CHECKSIG)) return false;
        return true;
    }

    private static boolean equalsRange(byte[] a, int start, byte[] b) {
        if (start + b.length > a.length)
            return false;
        for (int i = 0; i < b.length; i++)
            if (a[i + start] != b[i])
                return false;
        return true;
    }
    
    /**
     * Returns the script bytes of inputScript with all instances of the specified script object removed
     */
    public static byte[] removeAllInstancesOf(byte[] inputScript, byte[] chunkToRemove) {
        // We usually don't end up removing anything
        UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(inputScript.length);

        int cursor = 0;
        while (cursor < inputScript.length) {
            boolean skip = equalsRange(inputScript, cursor, chunkToRemove);
            
            int opcode = inputScript[cursor++] & 0xFF;
            int additionalBytes = 0;
            if (opcode >= 0 && opcode < OP_PUSHDATA1) {
                additionalBytes = opcode;
            } else if (opcode == OP_PUSHDATA1) {
                additionalBytes = (0xFF & inputScript[cursor]) + 1;
            } else if (opcode == OP_PUSHDATA2) {
                additionalBytes = ((0xFF & inputScript[cursor]) |
                                  ((0xFF & inputScript[cursor+1]) << 8)) + 2;
            } else if (opcode == OP_PUSHDATA4) {
                additionalBytes = ((0xFF & inputScript[cursor]) |
                                  ((0xFF & inputScript[cursor+1]) << 8) |
                                  ((0xFF & inputScript[cursor+1]) << 16) |
                                  ((0xFF & inputScript[cursor+1]) << 24)) + 4;
            }
            if (!skip) {
                try {
                    bos.write(opcode);
                    bos.write(Arrays.copyOfRange(inputScript, cursor, cursor + additionalBytes));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            cursor += additionalBytes;
        }
        return bos.toByteArray();
    }
    
    /**
     * Returns the script bytes of inputScript with all instances of the given op code removed
     */
    public static byte[] removeAllInstancesOfOp(byte[] inputScript, int opCode) {
        return removeAllInstancesOf(inputScript, new byte[] {(byte)opCode});
    }
    
    ////////////////////// Script verification and helpers ////////////////////////////////
    
    public static boolean castToBool(byte[] data) {
        for (int i = 0; i < data.length; i++)
        {
            // "Can be negative zero" - Bitcoin Core (see OpenSSL's BN_bn2mpi)
            if (data[i] != 0)
                return !(i == data.length - 1 && (data[i] & 0xFF) == 0x80);
        }
        return false;
    }
    
    /**
     * Cast a script chunk to a BigInteger.
     *
     * @see #castToBigInteger(byte[], boolean) for values with different maximum
     * sizes.
     * @throws ScriptException if the chunk is longer than 4 bytes.
     */
    private static BigInteger castToBigInteger(byte[] chunk, boolean enforceMinimal) throws ScriptException {
        if (chunk.length > DEFAULT_MAX_NUM_ELEMENT_SIZE)
            throw new ScriptException("Script attempted to use an integer larger than 4 bytes");
        if (enforceMinimal && !Utils.checkMinimallyEncodedLE(chunk, DEFAULT_MAX_NUM_ELEMENT_SIZE))
            throw new ScriptException("Number is not minimally encoded");
        //numbers on the stack or stored LE so convert as MPI requires BE.
        byte[] bytesBE = Utils.reverseBytes(chunk);
        return Utils.decodeMPI(bytesBE, false);
    }

    /**
     * Cast a script chunk to a BigInteger. Normally you would want
     * {@link #castToBigInteger(byte[], boolean)} instead, this is only for cases where
     * the normal maximum length does not apply (i.e. CHECKLOCKTIMEVERIFY).
     *
     * @param maxLength the maximum length in bytes.
     * @throws ScriptException if the chunk is longer than the specified maximum.
     */
    private static BigInteger castToBigInteger(final byte[] chunk, final int maxLength, boolean enforceMinimal) throws ScriptException {
        if (chunk.length > maxLength)
            throw new ScriptException("Script attempted to use an integer larger than "
                + maxLength + " bytes");
        if (enforceMinimal && !Utils.checkMinimallyEncodedLE(chunk, 5))
            throw new ScriptException("Number is not minimally encoded");
        return Utils.decodeMPI(Utils.reverseBytes(chunk), false);
    }

    public boolean isOpReturn() {
        return chunks.size() > 0 && chunks.get(0).equalsOpCode(OP_RETURN);
    }

    /**
     * Exposes the script interpreter. Normally you should not use this directly, instead use
     * {@link org.bitcoinj.core.TransactionInput#verify(org.bitcoinj.core.TransactionOutput)} or
     * {@link org.bitcoinj.script.Script#correctlySpends(org.bitcoinj.core.Transaction, long, Script)}. This method
     * is useful if you need more precise control or access to the final state of the stack. This interface is very
     * likely to change in future.
     *
     * @deprecated Use {@link #executeScript(org.bitcoinj.core.Transaction, long, org.bitcoinj.script.Script, org.bitcoinj.script.ScriptStack, java.util.Set)}
     * instead.
     */
    @Deprecated
    public static void executeScript(@Nullable Transaction txContainingThis, long index,
                                     Script script, ScriptStack stack, boolean enforceNullDummy) throws ScriptException {
        final EnumSet<VerifyFlag> flags = enforceNullDummy
            ? EnumSet.of(VerifyFlag.NULLDUMMY)
            : EnumSet.noneOf(VerifyFlag.class);

        executeScript(txContainingThis, index, script, stack, Coin.ZERO, flags);
    }

    @Deprecated
    public static void executeScript(@Nullable Transaction txContainingThis, long index,
                                     Script script, ScriptStack stack, Set<VerifyFlag> verifyFlags) throws ScriptException {
         executeScript(txContainingThis, index, script, stack, Coin.ZERO, verifyFlags);
    }

    private static boolean isOpcodeDisabled(int opcode, Set<VerifyFlag> verifyFlags) {


        switch (opcode) {
            case OP_INVERT:
            case OP_LSHIFT:
            case OP_RSHIFT:

            case OP_2MUL:
            case OP_2DIV:
            case OP_MUL:
                //disabled codes
                return true;

            case OP_CAT:
            case OP_SPLIT:
            case OP_AND:
            case OP_OR:
            case OP_XOR:
            case OP_DIV:
            case OP_MOD:
            case OP_NUM2BIN:
            case OP_BIN2NUM:
                //enabled codes, still disabled if flag is not activated
                return !verifyFlags.contains(VerifyFlag.MONOLITH_ACTIVE);

            default:
                //not an opcode that was ever disabled
                break;
        }



        return false;

    }

    /**
     * Exposes the script interpreter. Normally you should not use this directly, instead use
     * {@link org.bitcoinj.core.TransactionInput#verify(org.bitcoinj.core.TransactionOutput)} or
     * {@link org.bitcoinj.script.Script#correctlySpends(org.bitcoinj.core.Transaction, long, Script)}. This method
     * is useful if you need more precise control or access to the final state of the stack. This interface is very
     * likely to change in future.
     */
    public static void executeScript(@Nullable Transaction txContainingThis, long index,
                                     Script script, ScriptStack stack, Coin value, Set<VerifyFlag> verifyFlags) throws ScriptException {
        executeScript(txContainingThis,index, new SimpleScriptStream(script), stack, value, verifyFlags, null);
    }

    /**
     * Executes a script in debug mode with the provided ScriptStateListener.  Exceptions (which are thrown when a script fails) are caught
     * and passed to the listener before being rethrown.
     */
    public static void executeDebugScript(@Nullable Transaction txContainingThis, long index,
                                     ScriptStream script, ScriptStack stack, Coin value, Set<VerifyFlag> verifyFlags, ScriptStateListener scriptStateListener) throws ScriptException {
        try {
            executeScript(txContainingThis, index, script, stack, value, verifyFlags, scriptStateListener);
        } catch (ScriptException e) {
            scriptStateListener.onExceptionThrown(e);
            try {
                //pause to hopefully give the System.out time to beat System.err
                Thread.sleep(200);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            throw e;
        }
    }

    /**
     * Exposes the script interpreter. Normally you should not use this directly, instead use
     * {@link org.bitcoinj.core.TransactionInput#verify(org.bitcoinj.core.TransactionOutput)} or
     * {@link org.bitcoinj.script.Script#correctlySpends(org.bitcoinj.core.Transaction, long, Script)}. This method
     * is useful if you need more precise control or access to the final state of the stack. This interface is very
     * likely to change in future.
     */
    public static void executeScript(@Nullable Transaction txContainingThis, long index,
                                     ScriptStream script, ScriptStack stack, Coin value, Set<VerifyFlag> verifyFlags, ScriptStateListener scriptStateListener) throws ScriptException {
        int opCount = 0;
        int lastCodeSepLocation = 0;

        //for scriptSig this can be set to true as the stack state is known to be empty
        boolean initialStackStateKnown = false;

        //mark all stack items as derived if initial stack state is not known to this execution context
        stack.setDerivations(!initialStackStateKnown);
        ScriptStack altstack = new ScriptStack();
        LinkedList<Boolean> ifStack = new LinkedList<Boolean>();
        final boolean enforceMinimal = verifyFlags.contains(VerifyFlag.MINIMALDATA);

        final long maxScriptElementSize = verifyFlags.contains(VerifyFlag.GENESIS_ACTIVE) ? Long.MAX_VALUE : MAX_SCRIPT_ELEMENT_SIZE;
        final int maxOpCount = verifyFlags.contains(VerifyFlag.GENESIS_ACTIVE) ? Integer.MAX_VALUE :
                verifyFlags.contains(VerifyFlag.MAGNETIC_ACTIVE) ? MAX_OPCOUNT_PRE_GENESIS :
                        MAX_OPCOUNT_PRE_MAGNETIC;

        if (scriptStateListener != null) {
            scriptStateListener.setInitialState(
                    txContainingThis,
                    index,
                    script,
                    Collections.unmodifiableList(stack),
                    Collections.unmodifiableList(altstack),
                    Collections.unmodifiableList(ifStack),
                    value,
                    verifyFlags
            );
        }
        for (ScriptChunk chunk : script) {
            boolean shouldExecute = !ifStack.contains(false);

            if (scriptStateListener != null) {
                scriptStateListener._onBeforeOpCodeExecuted(chunk, shouldExecute);
            }

            if (chunk.opcode == OP_0) {
                if (!shouldExecute)
                    continue;

                stack.add(new byte[]{});
            } else if (!chunk.isOpCode()) {
                if (chunk.data.length > maxScriptElementSize)
                    throw new ScriptException("Attempted to push a data string larger than 520 bytes");
                
                if (!shouldExecute)
                    continue;
                
                stack.add(chunk.data);
            } else {
                int opcode = chunk.opcode;
                if (opcode > OP_16) {
                    opCount++;
                    if (opCount > maxOpCount)
                        throw new ScriptException("More script operations than is allowed");
                }
                
                if (opcode == OP_VERIF || opcode == OP_VERNOTIF)
                    throw new ScriptException("Script included OP_VERIF or OP_VERNOTIF");

                // Some opcodes are disabled.
                if (isOpcodeDisabled(opcode, verifyFlags)) {
                    throw new ScriptException("Script included a disabled Script Op.");
                }

                switch (opcode) {
                case OP_IF:
                    if (!shouldExecute) {
                        ifStack.add(false);
                        continue;
                    }
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_IF on an empty stack");
                    ifStack.add(castToBool(stack.pollLast().bytes));
                    continue;
                case OP_NOTIF:
                    if (!shouldExecute) {
                        ifStack.add(false);
                        continue;
                    }
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_NOTIF on an empty stack");
                    ifStack.add(!castToBool(stack.pollLast().bytes));
                    continue;
                case OP_ELSE:
                    if (ifStack.isEmpty())
                        throw new ScriptException("Attempted OP_ELSE without OP_IF/NOTIF");
                    ifStack.add(!ifStack.pollLast());
                    continue;
                case OP_ENDIF:
                    if (ifStack.isEmpty())
                        throw new ScriptException("Attempted OP_ENDIF without OP_IF/NOTIF");
                    ifStack.pollLast();
                    continue;
                }
                
                if (!shouldExecute)
                    continue;
                
                switch(opcode) {
                // OP_0 is no opcode
                case OP_1NEGATE:
                    stack.add(Utils.reverseBytes(Utils.encodeMPI(BigInteger.ONE.negate(), false)));
                    break;
                case OP_1:
                case OP_2:
                case OP_3:
                case OP_4:
                case OP_5:
                case OP_6:
                case OP_7:
                case OP_8:
                case OP_9:
                case OP_10:
                case OP_11:
                case OP_12:
                case OP_13:
                case OP_14:
                case OP_15:
                case OP_16:
                    stack.add(Utils.reverseBytes(Utils.encodeMPI(BigInteger.valueOf(decodeFromOpN(opcode)), false)));
                    break;
                case OP_NOP:
                    break;
                case OP_VERIFY:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_VERIFY on an empty stack");
                    if (!castToBool(stack.pollLast().bytes))
                        throw new ScriptException("OP_VERIFY failed");
                    break;
                case OP_RETURN:
                    throw new ScriptException("Script called OP_RETURN");
                case OP_TOALTSTACK:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_TOALTSTACK on an empty stack");
                    altstack.add(stack.pollLast());
                    break;
                case OP_FROMALTSTACK:
                    if (altstack.size() < 1)
                        throw new ScriptException("Attempted OP_FROMALTSTACK on an empty altstack");
                    stack.add(altstack.pollLast());
                    break;
                case OP_2DROP:
                    if (stack.size() < 2)
                        throw new ScriptException("Attempted OP_2DROP on a stack with size < 2");
                    stack.pollLast();
                    stack.pollLast();
                    break;
                case OP_2DUP:
                    if (stack.size() < 2)
                        throw new ScriptException("Attempted OP_2DUP on a stack with size < 2");
                    Iterator<StackItem> it2DUP = stack.descendingIterator();
                    StackItem OP2DUPtmpChunk2 = it2DUP.next();
                    stack.add(it2DUP.next());
                    stack.add(OP2DUPtmpChunk2);
                    break;
                case OP_3DUP:
                    if (stack.size() < 3)
                        throw new ScriptException("Attempted OP_3DUP on a stack with size < 3");
                    Iterator<StackItem> it3DUP = stack.descendingIterator();
                    StackItem OP3DUPtmpChunk3 = it3DUP.next();
                    StackItem OP3DUPtmpChunk2 = it3DUP.next();
                    stack.add(it3DUP.next());
                    stack.add(OP3DUPtmpChunk2);
                    stack.add(OP3DUPtmpChunk3);
                    break;
                case OP_2OVER:
                    if (stack.size() < 4)
                        throw new ScriptException("Attempted OP_2OVER on a stack with size < 4");
                    Iterator<StackItem> it2OVER = stack.descendingIterator();
                    it2OVER.next();
                    it2OVER.next();
                    StackItem OP2OVERtmpChunk2 = it2OVER.next();
                    stack.add(it2OVER.next());
                    stack.add(OP2OVERtmpChunk2);
                    break;
                case OP_2ROT:
                    if (stack.size() < 6)
                        throw new ScriptException("Attempted OP_2ROT on a stack with size < 6");
                    StackItem OP2ROTtmpChunk6 = stack.pollLast();
                    StackItem OP2ROTtmpChunk5 = stack.pollLast();
                    StackItem OP2ROTtmpChunk4 = stack.pollLast();
                    StackItem OP2ROTtmpChunk3 = stack.pollLast();
                    StackItem OP2ROTtmpChunk2 = stack.pollLast();
                    StackItem OP2ROTtmpChunk1 = stack.pollLast();
                    stack.add(OP2ROTtmpChunk3);
                    stack.add(OP2ROTtmpChunk4);
                    stack.add(OP2ROTtmpChunk5);
                    stack.add(OP2ROTtmpChunk6);
                    stack.add(OP2ROTtmpChunk1);
                    stack.add(OP2ROTtmpChunk2);
                    break;
                case OP_2SWAP:
                    if (stack.size() < 4)
                        throw new ScriptException("Attempted OP_2SWAP on a stack with size < 4");
                    StackItem OP2SWAPtmpChunk4 = stack.pollLast();
                    StackItem OP2SWAPtmpChunk3 = stack.pollLast();
                    StackItem OP2SWAPtmpChunk2 = stack.pollLast();
                    StackItem OP2SWAPtmpChunk1 = stack.pollLast();
                    stack.add(OP2SWAPtmpChunk3);
                    stack.add(OP2SWAPtmpChunk4);
                    stack.add(OP2SWAPtmpChunk1);
                    stack.add(OP2SWAPtmpChunk2);
                    break;
                case OP_IFDUP:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_IFDUP on an empty stack");
                    StackItem ifdupBool = stack.getLast();
                    if (castToBool(ifdupBool.bytes))
                        stack.add(stack.getLast(), ifdupBool);
                    break;
                case OP_DEPTH:
                    //depth can't be known at runtime unless you already know the size of the initial stack.
                    stack.add(StackItem.wrapDerived(Utils.reverseBytes(Utils.encodeMPI(BigInteger.valueOf(stack.size()), false)), true));
                    break;
                case OP_DROP:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_DROP on an empty stack");
                    stack.pollLast();
                    break;
                case OP_DUP:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_DUP on an empty stack");
                    stack.add(stack.getLast());
                    break;
                case OP_NIP:
                    if (stack.size() < 2)
                        throw new ScriptException("Attempted OP_NIP on a stack with size < 2");
                    StackItem OPNIPtmpChunk = stack.pollLast();
                    stack.pollLast();
                    stack.add(OPNIPtmpChunk);
                    break;
                case OP_OVER:
                    if (stack.size() < 2)
                        throw new ScriptException("Attempted OP_OVER on a stack with size < 2");
                    Iterator<StackItem> itOVER = stack.descendingIterator();
                    itOVER.next();
                    stack.add(itOVER.next());
                    break;
                case OP_PICK:
                case OP_ROLL:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_PICK/OP_ROLL on an empty stack");

                    StackItem rollVal = stack.pollLast();

                    long val = castToBigInteger(rollVal.bytes, enforceMinimal).longValue();
                    if (val < 0 || val >= stack.size())
                        throw new ScriptException("OP_PICK/OP_ROLL attempted to get data deeper than stack size");
                    Iterator<StackItem> itPICK = stack.descendingIterator();
                    for (long i = 0; i < val; i++)
                        itPICK.next();
                    StackItem OPROLLtmpChunk = itPICK.next();
                    if (opcode == OP_ROLL)
                        itPICK.remove();
                    //whether the value is derived doesn't depend on where in the stack
                    //it's picked from so just add the original StackItem
                    stack.add(OPROLLtmpChunk);
                    break;
                case OP_ROT:
                    if (stack.size() < 3)
                        throw new ScriptException("Attempted OP_ROT on a stack with size < 3");
                    StackItem OPROTtmpChunk3 = stack.pollLast();
                    StackItem OPROTtmpChunk2 = stack.pollLast();
                    StackItem OPROTtmpChunk1 = stack.pollLast();
                    stack.add(OPROTtmpChunk2);
                    stack.add(OPROTtmpChunk3);
                    stack.add(OPROTtmpChunk1);
                    break;
                case OP_SWAP:
                case OP_TUCK:
                    if (stack.size() < 2)
                        throw new ScriptException("Attempted OP_SWAP on a stack with size < 2");
                    StackItem OPSWAPtmpChunk2 = stack.pollLast();
                    StackItem OPSWAPtmpChunk1 = stack.pollLast();
                    stack.add(OPSWAPtmpChunk2);
                    stack.add(OPSWAPtmpChunk1);
                    if (opcode == OP_TUCK)
                        stack.add(OPSWAPtmpChunk2);
                    break;
                //byte string operations
                case OP_CAT:
                    if (stack.size() < 2)
                        throw new ScriptException("Invalid stack operation.");
                    StackItem catBytes2 = stack.pollLast();
                    StackItem catBytes1 = stack.pollLast();

                    int len = catBytes1.length + catBytes2.length;
                    if (len > maxScriptElementSize)
                        throw new ScriptException("Push value size limit exceeded.");

                    byte[] catOut = new byte[len];
                    System.arraycopy(catBytes1.bytes, 0, catOut, 0, catBytes1.length);
                    System.arraycopy(catBytes2.bytes, 0, catOut, catBytes1.length, catBytes2.length);
                    stack.addLast(catOut, catBytes1, catBytes2);

                    break;

                case OP_SPLIT:
                    if (stack.size() < 2)
                        throw new ScriptException("Invalid stack operation.");
                    StackItem biSplitPosItem = stack.pollLast();
                    StackItem splitBytesItem = stack.pollLast();
                    BigInteger biSplitPos = castToBigInteger(biSplitPosItem.bytes, enforceMinimal);

                    //sanity check in case we aren't enforcing minimal number encoding
                    //we will check that the biSplitPos value can be safely held in an int
                    //before we cast it as BigInteger will behave similar to casting if the value
                    //is greater than the target type can hold.
                    BigInteger biMaxInt = BigInteger.valueOf((long) Integer.MAX_VALUE);
                    if (biSplitPos.compareTo(biMaxInt) >= 0)
                        throw new ScriptException("Invalid OP_SPLIT range.");

                    int splitPos = biSplitPos.intValue();
                    byte[] splitBytes = splitBytesItem.bytes;

                    if (splitPos > splitBytes.length || splitPos < 0)
                        throw new ScriptException("Invalid OP_SPLIT range.");

                    byte[] splitOut1 = new byte[splitPos];
                    byte[] splitOut2 = new byte[splitBytes.length - splitPos];

                    System.arraycopy(splitBytes, 0, splitOut1, 0, splitPos);
                    System.arraycopy(splitBytes, splitPos, splitOut2, 0, splitOut2.length);

                    stack.addLast(splitOut1, splitBytesItem, biSplitPosItem);
                    stack.addLast(splitOut2, splitBytesItem, biSplitPosItem);
                    break;

                case OP_NUM2BIN:
                    if (stack.size() < 2)
                        throw new ScriptException("Invalid stack operation.");

                    StackItem numSizeItem = stack.pollLast();
                    int numSize = castToBigInteger(numSizeItem.bytes, enforceMinimal).intValue();

                    if (numSize > maxScriptElementSize)
                        throw new ScriptException("Push value size limit exceeded.");

                    StackItem rawNumItem = stack.pollLast();

                    // Try to see if we can fit that number in the number of
                    // byte requested.
                    byte[] minimalNumBytes = Utils.minimallyEncodeLE(rawNumItem.bytes);
                    if (minimalNumBytes.length > numSize) {
                        //we can't
                        throw new ScriptException("The requested encoding is impossible to satisfy.");
                    }

                    if (minimalNumBytes.length == numSize) {
                        //already the right size so just push it to stack
                        stack.addLast(minimalNumBytes, numSizeItem, rawNumItem);
                    } else if (numSize == 0) {
                        stack.addLast(Utils.EMPTY_BYTE_ARRAY, numSizeItem, rawNumItem);
                    } else {
                        int signBit = 0x00;
                        if (minimalNumBytes.length > 0) {
                            signBit = minimalNumBytes[minimalNumBytes.length - 1] & 0x80;
                            minimalNumBytes[minimalNumBytes.length - 1] &= 0x7f;
                        }
                        int minimalBytesToCopy = minimalNumBytes.length > numSize ? numSize : minimalNumBytes.length;
                        byte[] expandedNumBytes = new byte[numSize]; //initialized to all zeroes
                        System.arraycopy(minimalNumBytes, 0, expandedNumBytes, 0, minimalBytesToCopy);
                        expandedNumBytes[expandedNumBytes.length - 1] = (byte) signBit;
                        stack.addLast(expandedNumBytes, rawNumItem, numSizeItem);
                    }
                    break;

                case OP_BIN2NUM:
                    if (stack.size() < 1)
                        throw new ScriptException("Invalid stack operation.");
                    StackItem binBytes = stack.pollLast();
                    byte[] numBytes = Utils.minimallyEncodeLE(binBytes.bytes);

                    if (!Utils.checkMinimallyEncodedLE(numBytes, DEFAULT_MAX_NUM_ELEMENT_SIZE))
                        throw new ScriptException("Given operand is not a number within the valid range [-2^31...2^31]");

                    stack.addLast(numBytes, binBytes);

                    break;
                case OP_SIZE:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_SIZE on an empty stack");
                    StackItem sizeItem = stack.getLast();
                    stack.add(Utils.reverseBytes(Utils.encodeMPI(BigInteger.valueOf(sizeItem.length), false)), sizeItem);
                    break;
                case OP_INVERT:
                    throw new ScriptException("Attempted to use disabled Script Op.");
                case OP_AND:
                case OP_OR:
                case OP_XOR:
                    // (x1 x2 - out)
                    if (stack.size() < 2) {
                        throw new ScriptException("Invalid stack operation.");
                    }

                    //valtype &vch1 = stacktop(-2);
                    //valtype &vch2 = stacktop(-1);
                    StackItem vch2Item = stack.pollLast();
                    StackItem vch1Item = stack.pollLast();
                    byte[] vch2 = vch2Item.bytes;
                    byte[] vch1 = vch1Item.bytes;

                    // Inputs must be the same size
                    if (vch1.length != vch2.length) {
                        throw new ScriptException("Invalid operand size.");
                    }

                    // To avoid allocating, we modify vch1 in place.
                    switch (opcode) {
                        case OP_AND:
                            for (int i = 0; i < vch1.length; i++) {
                                vch1[i] &= vch2[i];
                            }
                            break;
                        case OP_OR:
                            for (int i = 0; i < vch1.length; i++) {
                                vch1[i] |= vch2[i];
                            }
                            break;
                        case OP_XOR:
                            for (int i = 0; i < vch1.length; i++) {
                                vch1[i] ^= vch2[i];
                            }
                            break;
                        default:
                            break;
                    }

                    // And pop vch2.
                    //popstack(stack);

                    //put vch1 back on stack
                    stack.addLast(vch1, vch1Item, vch2Item);

                    break;

                case OP_EQUAL:
                    if (stack.size() < 2)
                        throw new ScriptException("Attempted OP_EQUAL on a stack with size < 2");
                    StackItem eq2 = stack.pollLast();
                    StackItem eq1 = stack.pollLast();
                    byte[] eqResult = Objects.equals(eq2, eq1) ? new byte[] {1} : new byte[] {};
                    stack.add(eqResult, eq1, eq2);
                    break;
                case OP_EQUALVERIFY:
                    if (stack.size() < 2)
                        throw new ScriptException("Attempted OP_EQUALVERIFY on a stack with size < 2");
                    if (!Objects.equals(stack.pollLast(), stack.pollLast()))
                        throw new ScriptException("OP_EQUALVERIFY: non-equal data");
                    break;
                case OP_1ADD:
                case OP_1SUB:
                case OP_NEGATE:
                case OP_ABS:
                case OP_NOT:
                case OP_0NOTEQUAL:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted a numeric op on an empty stack");
                    StackItem numericOpItem = stack.pollLast();
                    BigInteger numericOPnum = castToBigInteger(numericOpItem.bytes, enforceMinimal);
                                        
                    switch (opcode) {
                    case OP_1ADD:
                        numericOPnum = numericOPnum.add(BigInteger.ONE);
                        break;
                    case OP_1SUB:
                        numericOPnum = numericOPnum.subtract(BigInteger.ONE);
                        break;
                    case OP_NEGATE:
                        numericOPnum = numericOPnum.negate();
                        break;
                    case OP_ABS:
                        if (numericOPnum.signum() < 0)
                            numericOPnum = numericOPnum.negate();
                        break;
                    case OP_NOT:
                        if (numericOPnum.equals(BigInteger.ZERO))
                            numericOPnum = BigInteger.ONE;
                        else
                            numericOPnum = BigInteger.ZERO;
                        break;
                    case OP_0NOTEQUAL:
                        if (numericOPnum.equals(BigInteger.ZERO))
                            numericOPnum = BigInteger.ZERO;
                        else
                            numericOPnum = BigInteger.ONE;
                        break;
                    default:
                        throw new AssertionError("Unreachable");
                    }
                    
                    stack.add(Utils.reverseBytes(Utils.encodeMPI(numericOPnum, false)), numericOpItem);
                    break;
                case OP_2MUL:
                case OP_2DIV:
                    throw new ScriptException("Attempted to use disabled Script Op.");
                case OP_ADD:
                case OP_SUB:
                case OP_DIV:
                case OP_MOD:
                case OP_BOOLAND:
                case OP_BOOLOR:
                case OP_NUMEQUAL:
                case OP_NUMNOTEQUAL:
                case OP_LESSTHAN:
                case OP_GREATERTHAN:
                case OP_LESSTHANOREQUAL:
                case OP_GREATERTHANOREQUAL:
                case OP_MIN:
                case OP_MAX:
                    if (stack.size() < 2)
                        throw new ScriptException("Attempted a numeric op on a stack with size < 2");
                    StackItem numericOpItem2 = stack.pollLast();
                    StackItem numericOpItem1 = stack.pollLast();
                    BigInteger numericOPnum2 = castToBigInteger(numericOpItem2.bytes, enforceMinimal);
                    BigInteger numericOPnum1 = castToBigInteger(numericOpItem1.bytes, enforceMinimal);

                    BigInteger numericOPresult;
                    switch (opcode) {
                    case OP_ADD:
                        numericOPresult = numericOPnum1.add(numericOPnum2);
                        break;
                    case OP_SUB:
                        numericOPresult = numericOPnum1.subtract(numericOPnum2);
                        break;

                    case OP_DIV:
                        if (numericOPnum2.intValue() == 0)
                            throw new ScriptException("Division by zero error");
                        numericOPresult = numericOPnum1.divide(numericOPnum2);
                        break;

                        case OP_MOD:
                            if (numericOPnum2.intValue() == 0)
                                throw new ScriptException("Modulo by zero error");

                            /**
                             * BigInteger doesn't behave the way we want for modulo operations.  Firstly it's
                             * always garunteed to return a +ve result.  Secondly it will throw an exception
                             * if the 2nd operand is negative.  So we'll convert the values to longs and use native
                             * modulo.  When we expand the number limits to arbitrary length we will likely need
                             * a new BigNum implementation to handle this correctly.
                             */
                            long lOp1 = numericOPnum1.longValue();
                            if (!BigInteger.valueOf(lOp1).equals(numericOPnum1)) {
                                //in case the value is larger than a long can handle we need to crash and burn.
                                throw new RuntimeException("Cannot handle large negative operand for modulo operation");
                            }
                            long lOp2 = numericOPnum2.longValue();
                            if (!BigInteger.valueOf(lOp2).equals(numericOPnum2)) {
                                //in case the value is larger than a long can handle we need to crash and burn.
                                throw new RuntimeException("Cannot handle large negative operand for modulo operation");
                            }
                            long lOpResult = lOp1 % lOp2;
                            numericOPresult = BigInteger.valueOf(lOpResult);

                            break;

                        case OP_BOOLAND:
                        if (!numericOPnum1.equals(BigInteger.ZERO) && !numericOPnum2.equals(BigInteger.ZERO))
                            numericOPresult = BigInteger.ONE;
                        else
                            numericOPresult = BigInteger.ZERO;
                        break;
                    case OP_BOOLOR:
                        if (!numericOPnum1.equals(BigInteger.ZERO) || !numericOPnum2.equals(BigInteger.ZERO))
                            numericOPresult = BigInteger.ONE;
                        else
                            numericOPresult = BigInteger.ZERO;
                        break;
                    case OP_NUMEQUAL:
                        if (numericOPnum1.equals(numericOPnum2))
                            numericOPresult = BigInteger.ONE;
                        else
                            numericOPresult = BigInteger.ZERO;
                        break;
                    case OP_NUMNOTEQUAL:
                        if (!numericOPnum1.equals(numericOPnum2))
                            numericOPresult = BigInteger.ONE;
                        else
                            numericOPresult = BigInteger.ZERO;
                        break;
                    case OP_LESSTHAN:
                        if (numericOPnum1.compareTo(numericOPnum2) < 0)
                            numericOPresult = BigInteger.ONE;
                        else
                            numericOPresult = BigInteger.ZERO;
                        break;
                    case OP_GREATERTHAN:
                        if (numericOPnum1.compareTo(numericOPnum2) > 0)
                            numericOPresult = BigInteger.ONE;
                        else
                            numericOPresult = BigInteger.ZERO;
                        break;
                    case OP_LESSTHANOREQUAL:
                        if (numericOPnum1.compareTo(numericOPnum2) <= 0)
                            numericOPresult = BigInteger.ONE;
                        else
                            numericOPresult = BigInteger.ZERO;
                        break;
                    case OP_GREATERTHANOREQUAL:
                        if (numericOPnum1.compareTo(numericOPnum2) >= 0)
                            numericOPresult = BigInteger.ONE;
                        else
                            numericOPresult = BigInteger.ZERO;
                        break;
                    case OP_MIN:
                        if (numericOPnum1.compareTo(numericOPnum2) < 0)
                            numericOPresult = numericOPnum1;
                        else
                            numericOPresult = numericOPnum2;
                        break;
                    case OP_MAX:
                        if (numericOPnum1.compareTo(numericOPnum2) > 0)
                            numericOPresult = numericOPnum1;
                        else
                            numericOPresult = numericOPnum2;
                        break;
                    default:
                        throw new RuntimeException("Opcode switched at runtime?");
                    }
                    
                    stack.add(Utils.reverseBytes(Utils.encodeMPI(numericOPresult, false)), numericOpItem1, numericOpItem2);
                    break;
                case OP_MUL:
                case OP_LSHIFT:
                case OP_RSHIFT:
                    throw new ScriptException("Attempted to use disabled Script Op.");
                case OP_NUMEQUALVERIFY:
                    if (stack.size() < 2)
                        throw new ScriptException("Attempted OP_NUMEQUALVERIFY on a stack with size < 2");
                    BigInteger OPNUMEQUALVERIFYnum2 = castToBigInteger(stack.pollLast().bytes, enforceMinimal);
                    BigInteger OPNUMEQUALVERIFYnum1 = castToBigInteger(stack.pollLast().bytes, enforceMinimal);
                    
                    if (!OPNUMEQUALVERIFYnum1.equals(OPNUMEQUALVERIFYnum2))
                        throw new ScriptException("OP_NUMEQUALVERIFY failed");
                    break;
                case OP_WITHIN:
                    if (stack.size() < 3)
                        throw new ScriptException("Attempted OP_WITHIN on a stack with size < 3");
                    StackItem OPWITHINitem3 = stack.pollLast();
                    StackItem OPWITHINitem2 = stack.pollLast();
                    StackItem OPWITHINitem1 = stack.pollLast();
                    BigInteger OPWITHINnum3 = castToBigInteger(OPWITHINitem3.bytes, enforceMinimal);
                    BigInteger OPWITHINnum2 = castToBigInteger(OPWITHINitem2.bytes, enforceMinimal);
                    BigInteger OPWITHINnum1 = castToBigInteger(OPWITHINitem1.bytes, enforceMinimal);
                    byte[] OPWITHINresult;
                    if (OPWITHINnum2.compareTo(OPWITHINnum1) <= 0 && OPWITHINnum1.compareTo(OPWITHINnum3) < 0)
                        OPWITHINresult = Utils.encodeMPI(BigInteger.ONE, false);
                    else
                        OPWITHINresult = Utils.encodeMPI(BigInteger.ZERO, false);
                    stack.add(Utils.reverseBytes(OPWITHINresult), OPWITHINitem1, OPWITHINitem2, OPWITHINitem3);
                    break;
                case OP_RIPEMD160:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_RIPEMD160 on an empty stack");
                    RIPEMD160Digest digest = new RIPEMD160Digest();
                    StackItem r160data = stack.pollLast();
                    digest.update(r160data.bytes, 0, r160data.length);
                    byte[] ripmemdHash = new byte[20];
                    digest.doFinal(ripmemdHash, 0);
                    stack.add(ripmemdHash, r160data);
                    break;
                case OP_SHA1:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_SHA1 on an empty stack");
                    try {
                        StackItem sha1Data = stack.pollLast();
                        stack.add(MessageDigest.getInstance("SHA-1").digest(sha1Data.bytes), sha1Data);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);  // Cannot happen.
                    }
                    break;
                case OP_SHA256:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_SHA256 on an empty stack");
                    StackItem sha256Data = stack.pollLast();
                    stack.add(Sha256Hash.hash(sha256Data.bytes), sha256Data);
                    break;
                case OP_HASH160:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_HASH160 on an empty stack");
                    StackItem hash160Data = stack.pollLast();
                    stack.add(Utils.sha256hash160(hash160Data.bytes), hash160Data);
                    break;
                case OP_HASH256:
                    if (stack.size() < 1)
                        throw new ScriptException("Attempted OP_SHA256 on an empty stack");
                    StackItem hash256Data = stack.pollLast();
                    stack.add(Sha256Hash.hashTwice(hash256Data.bytes), hash256Data);
                    break;
                case OP_CODESEPARATOR:
                    lastCodeSepLocation = chunk.getStartLocationInProgram() + 1;
                    break;
                case OP_CHECKSIG:
                case OP_CHECKSIGVERIFY:
                    if (txContainingThis == null)
                        throw new IllegalStateException("Script attempted signature check but no tx was provided");
                    executeCheckSig(txContainingThis, (int) index, script, stack, lastCodeSepLocation, opcode, value, verifyFlags);
                    break;
                case OP_CHECKMULTISIG:
                case OP_CHECKMULTISIGVERIFY:
                    if (txContainingThis == null)
                        throw new IllegalStateException("Script attempted signature check but no tx was provided");
                    opCount = executeMultiSig(txContainingThis, (int) index, script, stack, opCount, maxOpCount, lastCodeSepLocation, opcode, value, verifyFlags);
                    break;
                case OP_CHECKLOCKTIMEVERIFY:
                    if (!verifyFlags.contains(VerifyFlag.CHECKLOCKTIMEVERIFY)) {
                        // not enabled; treat as a NOP2
                        if (verifyFlags.contains(VerifyFlag.DISCOURAGE_UPGRADABLE_NOPS)) {
                            throw new ScriptException("Script used a reserved opcode " + opcode);
                        }
                        break;
                    }
                    executeCheckLockTimeVerify(txContainingThis, (int) index, stack, lastCodeSepLocation, opcode, verifyFlags);
                    break;
                case OP_NOP1:
                case OP_NOP3:
                case OP_NOP4:
                case OP_NOP5:
                case OP_NOP6:
                case OP_NOP7:
                case OP_NOP8:
                case OP_NOP9:
                case OP_NOP10:
                    if (verifyFlags.contains(VerifyFlag.DISCOURAGE_UPGRADABLE_NOPS)) {
                        throw new ScriptException("Script used a reserved opcode " + opcode);
                    }
                    break;
                    
                default:
                    throw new ScriptException("Script used a reserved opcode " + opcode);
                }
            }
            
            if (stack.size() + altstack.size() > 1000 || stack.size() + altstack.size() < 0)
                throw new ScriptException("Stack size exceeded range");

            if (scriptStateListener != null) {
                scriptStateListener.onAfterOpCodeExectuted();
            }
        }
        
        if (!ifStack.isEmpty())
            throw new ScriptException("OP_IF/OP_NOTIF without OP_ENDIF");

        if (scriptStateListener != null) {
            scriptStateListener.onScriptComplete();
        }

    }

    // This is more or less a direct translation of the code in Bitcoin Core
    private static void executeCheckLockTimeVerify(Transaction txContainingThis, int index, ScriptStack stack,
                                        int lastCodeSepLocation, int opcode,
                                        Set<VerifyFlag> verifyFlags) throws ScriptException {
        if (stack.size() < 1)
            throw new ScriptException("Attempted OP_CHECKLOCKTIMEVERIFY on a stack with size < 1");

        // Thus as a special case we tell CScriptNum to accept up
        // to 5-byte bignums to avoid year 2038 issue.
        StackItem nLockTimeItem = stack.getLast();
        //we don't modify the stack so no need to worry about passing on derivation status of stack items.
        final BigInteger nLockTime = castToBigInteger(nLockTimeItem.bytes, 5, verifyFlags.contains(VerifyFlag.MINIMALDATA));

        if (nLockTime.compareTo(BigInteger.ZERO) < 0)
            throw new ScriptException("Negative locktime");

        // There are two kinds of nLockTime, need to ensure we're comparing apples-to-apples
        if (!(
            ((txContainingThis.getLockTime() <  Transaction.LOCKTIME_THRESHOLD) && (nLockTime.compareTo(Transaction.LOCKTIME_THRESHOLD_BIG)) < 0) ||
            ((txContainingThis.getLockTime() >= Transaction.LOCKTIME_THRESHOLD) && (nLockTime.compareTo(Transaction.LOCKTIME_THRESHOLD_BIG)) >= 0))
        )
            throw new ScriptException("Locktime requirement type mismatch");

        // Now that we know we're comparing apples-to-apples, the
        // comparison is a simple numeric one.
        if (nLockTime.compareTo(BigInteger.valueOf(txContainingThis.getLockTime())) > 0)
            throw new ScriptException("Locktime requirement not satisfied");

        // Finally the nLockTime feature can be disabled and thus
        // CHECKLOCKTIMEVERIFY bypassed if every txin has been
        // finalized by setting nSequence to maxint. The
        // transaction would be allowed into the blockchain, making
        // the opcode ineffective.
        //
        // Testing if this vin is not final is sufficient to
        // prevent this condition. Alternatively we could test all
        // inputs, but testing just this input minimizes the data
        // required to prove correct CHECKLOCKTIMEVERIFY execution.
        if (!txContainingThis.getInput(index).hasSequence())
            throw new ScriptException("Transaction contains a final transaction input for a CHECKLOCKTIMEVERIFY script.");
    }

    private static void executeCheckSig(Transaction txContainingThis, int index, ScriptStream script, ScriptStack stack,
                                        int lastCodeSepLocation, int opcode, Coin value,
                                        Set<VerifyFlag> verifyFlags) throws ScriptException {
        final boolean requireCanonical = verifyFlags.contains(VerifyFlag.STRICTENC)
            || verifyFlags.contains(VerifyFlag.DERSIG)
            || verifyFlags.contains(VerifyFlag.LOW_S);
        if (stack.size() < 2)
            throw new ScriptException("Attempted OP_CHECKSIG(VERIFY) on a stack with size < 2");
        StackItem pubKey = stack.pollLast();
        StackItem sigBytes = stack.pollLast();

        byte[] connectedScript = script.getProgramFrom(script.getLastCodeSepIndex());

        UnsafeByteArrayOutputStream outStream = new UnsafeByteArrayOutputStream(sigBytes.length + 1);
        try {
            writeBytes(outStream, sigBytes.bytes);
        } catch (IOException e) {
            throw new RuntimeException(e); // Cannot happen
        }
        connectedScript = removeAllInstancesOf(connectedScript, outStream.toByteArray());

        // TODO: Use int for indexes everywhere, we can't have that many inputs/outputs
        boolean sigValid = false;
        try {
            TransactionSignature sig  = TransactionSignature.decodeFromBitcoin(sigBytes.bytes, requireCanonical,
                verifyFlags.contains(VerifyFlag.LOW_S));

            // TODO: Should check hash type is known
            Sha256Hash hash = sig.useForkId() ?
                                txContainingThis.hashForSignatureWitness(index, connectedScript, value, sig.sigHashMode(), sig.anyoneCanPay()) :
                                txContainingThis.hashForSignature(index, connectedScript, (byte) sig.sighashFlags);
            sigValid = ECKey.verify(hash.getBytes(), sig, pubKey.bytes);
        } catch (Exception e1) {
            // There is (at least) one exception that could be hit here (EOFException, if the sig is too short)
            // Because I can't verify there aren't more, we use a very generic Exception catch

            // This RuntimeException occurs when signing as we run partial/invalid scripts to see if they need more
            // signing work to be done inside LocalTransactionSigner.signInputs.
            if (!e1.getMessage().contains("Reached past end of ASN.1 stream"))
                log.warn("Signature checking failed!", e1);
        }

        if (opcode == OP_CHECKSIG)
            stack.add(sigValid ? new byte[] {1} : new byte[] {}, pubKey, sigBytes);
        else if (opcode == OP_CHECKSIGVERIFY)
            if (!sigValid)
                throw new ScriptException("Script failed OP_CHECKSIGVERIFY");
    }

    private static int executeMultiSig(Transaction txContainingThis, int index, ScriptStream script, ScriptStack stack,
                                       int opCount, int maxOpCount, int lastCodeSepLocation, int opcode, Coin value,
                                       Set<VerifyFlag> verifyFlags) throws ScriptException {
        final boolean requireCanonical = verifyFlags.contains(VerifyFlag.STRICTENC)
            || verifyFlags.contains(VerifyFlag.DERSIG)
            || verifyFlags.contains(VerifyFlag.LOW_S);
        final boolean enforceMinimal = verifyFlags.contains(VerifyFlag.MINIMALDATA);
        if (stack.size() < 2)
            throw new ScriptException("Attempted OP_CHECKMULTISIG(VERIFY) on a stack with size < 2");

        List<StackItem> polledStackItems = new LinkedList<>();

        StackItem pubKeyCountItem = stack.pollLast();
        polledStackItems.add(pubKeyCountItem);
        int pubKeyCount = castToBigInteger(pubKeyCountItem.bytes, enforceMinimal).intValue();
        if (pubKeyCount < 0 || pubKeyCount > 20)
            throw new ScriptException("OP_CHECKMULTISIG(VERIFY) with pubkey count out of range");
        opCount += pubKeyCount;
        if (opCount > maxOpCount)
            throw new ScriptException("Total op count > " + maxOpCount + " during OP_CHECKMULTISIG(VERIFY)");
        if (stack.size() < pubKeyCount + 1)
            throw new ScriptException("Attempted OP_CHECKMULTISIG(VERIFY) on a stack with size < num_of_pubkeys + 2");


        LinkedList<StackItem> pubkeys = new LinkedList<>();
        for (int i = 0; i < pubKeyCount; i++) {
            StackItem pubKey = stack.pollLast();
            pubkeys.add(pubKey);
        }
        polledStackItems.addAll(pubkeys);

        StackItem sigCountItem = stack.pollLast();
        polledStackItems.add(sigCountItem);
        int sigCount = castToBigInteger(sigCountItem.bytes, enforceMinimal).intValue();
        if (sigCount < 0 || sigCount > pubKeyCount)
            throw new ScriptException("OP_CHECKMULTISIG(VERIFY) with sig count out of range");
        if (stack.size() < sigCount + 1)
            throw new ScriptException("Attempted OP_CHECKMULTISIG(VERIFY) on a stack with size < num_of_pubkeys + num_of_signatures + 3");

        LinkedList<StackItem> sigs = new LinkedList<>();
        for (int i = 0; i < sigCount; i++) {
            StackItem sig = stack.pollLast();
            sigs.add(sig);
        }
        polledStackItems.addAll(sigs);

        byte[] connectedScript = script.getProgramFrom(script.getLastCodeSepIndex());

        for (StackItem sig : sigs) {
            UnsafeByteArrayOutputStream outStream = new UnsafeByteArrayOutputStream(sig.length + 1);
            try {
                writeBytes(outStream, sig.bytes);
            } catch (IOException e) {
                throw new RuntimeException(e); // Cannot happen
            }
            connectedScript = removeAllInstancesOf(connectedScript, outStream.toByteArray());
        }

        boolean valid = true;
        while (sigs.size() > 0) {
            StackItem pubKey = pubkeys.pollFirst();
            // We could reasonably move this out of the loop, but because signature verification is significantly
            // more expensive than hashing, its not a big deal.
            try {
                TransactionSignature sig = TransactionSignature.decodeFromBitcoin(sigs.getFirst().bytes, requireCanonical);
                Sha256Hash hash = sig.useForkId() ?
                        txContainingThis.hashForSignatureWitness(index, connectedScript, value, sig.sigHashMode(), sig.anyoneCanPay()):
                        txContainingThis.hashForSignature(index, connectedScript, (byte) sig.sighashFlags);
                if (ECKey.verify(hash.getBytes(), sig, pubKey.bytes))
                    sigs.pollFirst();
            } catch (Exception e) {
                // There is (at least) one exception that could be hit here (EOFException, if the sig is too short)
                // Because I can't verify there aren't more, we use a very generic Exception catch
            }

            if (sigs.size() > pubkeys.size()) {
                valid = false;
                break;
            }
        }

        // We uselessly remove a stack object to emulate a Bitcoin Core bug.
        StackItem nullDummy = stack.pollLast();
        //this could have been provided in scriptSig so still has an impact on whether the result is derived
        polledStackItems.add(nullDummy);
        if (verifyFlags.contains(VerifyFlag.NULLDUMMY) && nullDummy.length > 0)
            throw new ScriptException("OP_CHECKMULTISIG(VERIFY) with non-null nulldummy: " + Arrays.toString(nullDummy.bytes));

        if (opcode == OP_CHECKMULTISIG) {
            StackItem[] polledItems = polledStackItems.toArray(new StackItem[polledStackItems.size()]);
            stack.add(valid ? new byte[] {1} : new byte[] {}, polledItems);
        } else if (opcode == OP_CHECKMULTISIGVERIFY) {
            if (!valid)
                throw new ScriptException("Script failed OP_CHECKMULTISIGVERIFY");
        }
        return opCount;
    }

    /**
     * Verifies that this script (interpreted as a scriptSig) correctly spends the given scriptPubKey, enabling all
     * validation rules.
     * @param txContainingThis The transaction in which this input scriptSig resides.
     *                         Accessing txContainingThis from another thread while this method runs results in undefined behavior.
     * @param scriptSigIndex The index in txContainingThis of the scriptSig (note: NOT the index of the scriptPubKey).
     * @param scriptPubKey The connected scriptPubKey containing the conditions needed to claim the value.
     * @deprecated Use {@link #correctlySpends(org.bitcoinj.core.Transaction, long, org.bitcoinj.script.Script, java.util.Set)}
     * instead so that verification flags do not change as new verification options
     * are added.
     */
    @Deprecated
    public void correctlySpends(Transaction txContainingThis, long scriptSigIndex, Script scriptPubKey)
            throws ScriptException {
        correctlySpends(txContainingThis, scriptSigIndex, scriptPubKey, Coin.ZERO, ALL_VERIFY_FLAGS);
    }

    @Deprecated
    public void correctlySpends(Transaction txContainingThis, long scriptSigIndex, Script scriptPubKey,
                                Set<VerifyFlag> verifyFlags)
            throws ScriptException {
        correctlySpends(txContainingThis, scriptSigIndex, scriptPubKey, Coin.ZERO, verifyFlags);
    }
    /**
     * Verifies that this script (interpreted as a scriptSig) correctly spends the given scriptPubKey.
     * @param txContainingThis The transaction in which this input scriptSig resides.
     *                         Accessing txContainingThis from another thread while this method runs results in undefined behavior.
     * @param scriptSigIndex The index in txContainingThis of the scriptSig (note: NOT the index of the scriptPubKey).
     * @param scriptPubKey The connected scriptPubKey containing the conditions needed to claim the value.
     * @param verifyFlags Each flag enables one validation rule. If in doubt, use {@link #correctlySpends(Transaction, long, Script)}
     *                    which sets all flags.
     */
    public void correctlySpends(Transaction txContainingThis, long scriptSigIndex, Script scriptPubKey, Coin value,
                                Set<VerifyFlag> verifyFlags) throws ScriptException {
        // Clone the transaction because executing the script involves editing it, and if we die, we'll leave
        // the tx half broken (also it's not so thread safe to work on it directly.
        try {
            txContainingThis = txContainingThis.getParams().getDefaultSerializer().makeTransaction(txContainingThis.bitcoinSerialize());
        } catch (ProtocolException e) {
            throw new RuntimeException(e);   // Should not happen unless we were given a totally broken transaction.
        }
        if (getProgram().length > 10000 || scriptPubKey.getProgram().length > 10000)
            throw new ScriptException("Script larger than 10,000 bytes");

        ScriptStack stack = new ScriptStack();
        ScriptStack p2shStack = null;
        
        executeScript(txContainingThis, scriptSigIndex, this, stack, value, verifyFlags);
        if (verifyFlags.contains(VerifyFlag.P2SH))
            p2shStack = new ScriptStack(stack);
        executeScript(txContainingThis, scriptSigIndex, scriptPubKey, stack, value, verifyFlags);
        
        if (stack.size() == 0)
            throw new ScriptException("Stack empty at end of script execution.");
        
        if (!castToBool(stack.pollLast().bytes))
            throw new ScriptException("Script resulted in a non-true stack: " + stack);

        // P2SH is pay to script hash. It means that the scriptPubKey has a special form which is a valid
        // program but it has "useless" form that if evaluated as a normal program always returns true.
        // Instead, miners recognize it as special based on its template - it provides a hash of the real scriptPubKey
        // and that must be provided by the input. The goal of this bizarre arrangement is twofold:
        //
        // (1) You can sum up a large, complex script (like a CHECKMULTISIG script) with an address that's the same
        //     size as a regular address. This means it doesn't overload scannable QR codes/NFC tags or become
        //     un-wieldy to copy/paste.
        // (2) It allows the working set to be smaller: nodes perform best when they can store as many unspent outputs
        //     in RAM as possible, so if the outputs are made smaller and the inputs get bigger, then it's better for
        //     overall scalability and performance.

        // TODO: Check if we can take out enforceP2SH if there's a checkpoint at the enforcement block.
        if (verifyFlags.contains(VerifyFlag.P2SH) && scriptPubKey.isPayToScriptHash()) {
            for (ScriptChunk chunk : chunks)
                if (chunk.isOpCode() && chunk.opcode > OP_16)
                    throw new ScriptException("Attempted to spend a P2SH scriptPubKey with a script that contained script ops");
            
            StackItem scriptPubKeyBytes = p2shStack.pollLast();
            Script scriptPubKeyP2SH = new Script(scriptPubKeyBytes.bytes);
            
            executeScript(txContainingThis, scriptSigIndex, scriptPubKeyP2SH, p2shStack, value, verifyFlags);
            
            if (p2shStack.size() == 0)
                throw new ScriptException("P2SH stack empty at end of script execution.");
            
            if (!castToBool(p2shStack.pollLast().bytes))
                throw new ScriptException("P2SH script execution resulted in a non-true stack");
        }
    }

    // Utility that doesn't copy for internal use
    private byte[] getQuickProgram() {
        if (program != null)
            return program;
        return getProgram();
    }

    /**
     * Get the {@link org.bitcoinj.script.Script.ScriptType}.
     * @return The script type.
     */
    public ScriptType getScriptType() {
        ScriptType type = ScriptType.NO_TYPE;
        if (isSentToAddress()) {
            type = ScriptType.P2PKH;
        } else if (isSentToRawPubKey()) {
            type = ScriptType.PUB_KEY;
        } else if (isPayToScriptHash()) {
            type = ScriptType.P2SH;
        }
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Arrays.equals(getQuickProgram(), ((Script)o).getQuickProgram());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getQuickProgram());
    }
}
