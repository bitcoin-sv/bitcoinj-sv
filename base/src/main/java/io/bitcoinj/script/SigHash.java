/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.script;

import io.bitcoinj.core.Coin;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.UnsafeByteArrayOutputStream;
import io.bitcoinj.core.VarInt;
import io.bitcoinj.ecc.TransactionSignature;
import io.bitcoinj.bitcoin.api.BitcoinObject;
import io.bitcoinj.bitcoin.api.base.TxInput;
import io.bitcoinj.bitcoin.api.base.TxOutput;
import io.bitcoinj.bitcoin.api.base.Tx;
import io.bitcoinj.bitcoin.bean.BitcoinObjectImpl;
import io.bitcoinj.bitcoin.bean.base.TxOutputBean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

import static io.bitcoinj.core.Utils.uint32ToByteStreamLE;
import static io.bitcoinj.core.Utils.uint64ToByteStreamLE;
import static io.bitcoinj.script.ScriptOpCodes.*;

/**
 * Utility class for calculating transaction SigHash during CHECKSIG ops. This class only knows about
 * the {@link BitcoinObject} heirarchy of classes/interfaces.  A helper class 'Translate'
 * is provided to convert the legacy Message based classes of Transactions into this one.
 *
 * @author shadders
 */
public class SigHash {

    /**
     * These constants are a part of a scriptSig signature on the inputs. They define the details of how a
     * transaction can be redeemed, specifically, they control how the hash of the transaction is calculated.
     */
    public enum Flags {
        ALL(1),
        NONE(2),
        SINGLE(3),
        FORKID(0x40),
        ANYONECANPAY(0x80), // Caution: Using this type in isolation is non-standard. Treated similar to ANYONECANPAY_ALL.
        ANYONECANPAY_ALL(0x81),
        ANYONECANPAY_NONE(0x82),
        ANYONECANPAY_SINGLE(0x83),
        UNSET(0); // Caution: Using this type in isolation is non-standard. Treated similar to ALL.

        public final int value;

        /**
         * @param value
         */
        private Flags(final int value) {
            this.value = value;
        }

        /**
         * @return the value as a byte
         */
        public byte byteValue() {
            return (byte) this.value;
        }
    }

    public static Sha256Hash hashForForkIdSignature(Tx transaction,
                                                    int inputIndex,
                                                    byte[] connectedScript,
                                                    Coin prevValue,
                                                    Flags type,
                                                    boolean anyoneCanPay) {
        byte sigHashType = (byte) TransactionSignature.calcSigHashValue(type, anyoneCanPay, true);
        ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(transaction.getMessageSize() == BitcoinObjectImpl.UNKNOWN_MESSAGE_LENGTH ? 512 : transaction.getMessageSize() + 4);
        try {
            byte[] hashPrevouts = new byte[32];
            byte[] hashSequence = new byte[32];
            byte[] hashOutputs = new byte[32];
            anyoneCanPay = (sigHashType & Flags.ANYONECANPAY.value) == Flags.ANYONECANPAY.value;

            TxOutput indexedOutput = transaction.getOutputs().get(inputIndex);
            TxInput indexedInput = transaction.getInputs().get(inputIndex);

            if (!anyoneCanPay) {
                ByteArrayOutputStream bosHashPrevouts = new UnsafeByteArrayOutputStream(256);
                for (int i = 0; i < transaction.getInputs().size(); ++i) {
                    TxInput input = transaction.getInputs().get(i);
                    bosHashPrevouts.write(input.getOutpoint().getHash().getReversedBytes());
                    uint32ToByteStreamLE(input.getOutpoint().getIndex(), bosHashPrevouts);
                }
                hashPrevouts = Sha256Hash.hashTwice(bosHashPrevouts.toByteArray());
            }

            if (!anyoneCanPay && type != Flags.SINGLE && type != Flags.NONE) {
                ByteArrayOutputStream bosSequence = new UnsafeByteArrayOutputStream(256);
                for (int i = 0; i < transaction.getInputs().size(); ++i) {
                    uint32ToByteStreamLE(transaction.getInputs().get(i).getSequenceNumber(), bosSequence);
                }
                hashSequence = Sha256Hash.hashTwice(bosSequence.toByteArray());
            }

            if (type != Flags.SINGLE && type != Flags.NONE) {
                ByteArrayOutputStream bosHashOutputs = new UnsafeByteArrayOutputStream(256);
                for (int i = 0; i < transaction.getOutputs().size(); ++i) {
                    TxOutput output = transaction.getOutputs().get(i);
                    uint64ToByteStreamLE(
                            BigInteger.valueOf(output.getValue().getValue()),
                            bosHashOutputs
                    );
                    bosHashOutputs.write(new VarInt(output.getScriptBytes().length).encode());
                    bosHashOutputs.write(output.getScriptBytes());
                }
                hashOutputs = Sha256Hash.hashTwice(bosHashOutputs.toByteArray());
            } else if (type == Flags.SINGLE && inputIndex < transaction.getOutputs().size()) {
                ByteArrayOutputStream bosHashOutputs = new UnsafeByteArrayOutputStream(256);
                uint64ToByteStreamLE(
                        BigInteger.valueOf(indexedOutput.getValue().getValue()),
                        bosHashOutputs
                );
                bosHashOutputs.write(new VarInt(indexedOutput.getScriptBytes().length).encode());
                bosHashOutputs.write(indexedOutput.getScriptBytes());
                hashOutputs = Sha256Hash.hashTwice(bosHashOutputs.toByteArray());
            }
            uint32ToByteStreamLE(transaction.getVersion(), bos);
            bos.write(hashPrevouts);
            bos.write(hashSequence);
            bos.write(indexedInput.getOutpoint().getHash().getReversedBytes());
            uint32ToByteStreamLE(indexedInput.getOutpoint().getIndex(), bos);
            bos.write(new VarInt(connectedScript.length).encode());
            bos.write(connectedScript);
            uint64ToByteStreamLE(BigInteger.valueOf(prevValue.getValue()), bos);
            uint32ToByteStreamLE(indexedInput.getSequenceNumber(), bos);
            bos.write(hashOutputs);
            uint32ToByteStreamLE(transaction.getLockTime(), bos);
            uint32ToByteStreamLE(0x000000ff & sigHashType, bos);
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
        return Sha256Hash.wrap(Sha256Hash.hashTwice(bos.toByteArray(), 0, bos.size()));

    }

    /**
     * This is required for signatures which use a sigHashType which cannot be represented using SigHash and anyoneCanPay
     * See transaction c99c49da4c38af669dea436d3e73780dfdb6c1ecf9958baa52960e8baee30e73, which has sigHashType 0
     */
    public static Sha256Hash hashForLegacySignature(Tx transaction, int inputIndex, byte[] connectedScript, byte sigHashType) {
        // The SIGHASH flags are used in the design of contracts, please see this page for a further understanding of
        // the purposes of the code in this method:
        //
        //   https://en.bitcoin.it/wiki/Contracts

        try {
            // Create a copy of this transaction to operate upon because we need make changes to the inputs and outputs.
            // It would not be thread-safe to change the attributes of the transaction object itself.
            Tx tx = transaction.mutableCopy();

            // Clear input scripts in preparation for signing. If we're signing a fresh
            // transaction that step isn't very helpful, but it doesn't add much cost relative to the actual
            // EC math so we'll do it anyway.
            for (int i = 0; i < tx.getInputs().size(); i++) {
                tx.getInputs().get(i).setScriptBytes(null);
            }

            // This step has no purpose beyond being synchronized with Bitcoin Core's bugs. OP_CODESEPARATOR
            // is a legacy holdover from a previous, broken design of executing scripts that shipped in Bitcoin 0.1.
            // It was seriously flawed and would have let anyone take anyone elses money. Later versions switched to
            // the design we use today where scripts are executed independently but share a stack. This left the
            // OP_CODESEPARATOR instruction having no purpose as it was only meant to be used internally, not actually
            // ever put into scripts. Deleting OP_CODESEPARATOR is a step that should never be required but if we don't
            // do it, we could split off the main chain.
            connectedScript = removeAllInstancesOfOp(connectedScript, ScriptOpCodes.OP_CODESEPARATOR);

            // Set the input to the script of its output. Bitcoin Core does this but the step has no obvious purpose as
            // the signature covers the hash of the prevout transaction which obviously includes the output script
            // already. Perhaps it felt safer to him in some way, or is another leftover from how the code was written.
            TxInput input = tx.getInputs().get(inputIndex);
            input.setScriptBytes(connectedScript);

            if ((sigHashType & 0x1f) == Flags.NONE.value) {
                // SIGHASH_NONE means no outputs are signed at all - the signature is effectively for a "blank cheque".
                tx.setOutputs(new ArrayList<TxOutput>(0));
                // The signature isn't broken by new versions of the transaction issued by other parties.
                for (int i = 0; i < tx.getInputs().size(); i++)
                    if (i != inputIndex)
                        tx.getInputs().get(i).setSequenceNumber(0);
            } else if ((sigHashType & 0x1f) == Flags.SINGLE.value) {
                // SIGHASH_SINGLE means only sign the output at the same index as the input (ie, my output).
                if (inputIndex >= tx.getOutputs().size()) {
                    // The input index is beyond the number of outputs, it's a buggy signature made by a broken
                    // Bitcoin implementation. Bitcoin Core also contains a bug in handling this case:
                    // any transaction output that is signed in this case will result in both the signed output
                    // and any future outputs to this public key being steal-able by anyone who has
                    // the resulting signature and the public key (both of which are part of the signed tx input).

                    // Bitcoin Core's bug is that SignatureHash was supposed to return a hash and on this codepath it
                    // actually returns the constant "1" to indicate an error, which is never checked for. Oops.
                    return Sha256Hash.wrap("0100000000000000000000000000000000000000000000000000000000000000");
                }
                // In SIGHASH_SINGLE the outputs after the matching input index are deleted, and the outputs before
                // that position are "nulled out". Unintuitively, the value in a "null" transaction is set to -1.
                tx.setOutputs(new ArrayList<TxOutput>(tx.getOutputs().subList(0, inputIndex + 1)));
                for (int i = 0; i < inputIndex; i++) {
                    TxOutput output = new TxOutputBean(tx).makeMutable();
                    output.setValue(Coin.NEGATIVE_SATOSHI);
                    output.setScriptBytes(new byte[]{});
                    tx.getOutputs().set(i, output);
                }
                // The signature isn't broken by new versions of the transaction issued by other parties.
                for (int i = 0; i < tx.getInputs().size(); i++)
                    if (i != inputIndex)
                        tx.getInputs().get(i).setSequenceNumber(0);
            }

            if ((sigHashType & Flags.ANYONECANPAY.value) == Flags.ANYONECANPAY.value) {
                // SIGHASH_ANYONECANPAY means the signature in the input is not broken by changes/additions/removals
                // of other inputs. For example, this is useful for building assurance contracts.
                tx.setInputs(new ArrayList<>());
                tx.getInputs().add(input);
            }

            ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(transaction.getMessageSize() + 64);
            tx.serializeTo(bos);
            // We also have to write a hash type (sigHashType is actually an unsigned char)
            uint32ToByteStreamLE(0x000000ff & sigHashType, bos);
            // Note that this is NOT reversed to ensure it will be signed correctly. If it were to be printed out
            // however then we would expect that it is IS reversed.
            Sha256Hash hash = Sha256Hash.wrap(Sha256Hash.hashTwice(bos.toByteArray(), 0, bos.size()));

            return hash;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    /**
     * Returns the script bytes of inputScript with all instances of the given op code removed
     */
    public static byte[] removeAllInstancesOfOp(byte[] inputScript, int opCode) {
        return removeAllInstancesOf(inputScript, new byte[]{(byte) opCode});
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
                        ((0xFF & inputScript[cursor + 1]) << 8)) + 2;
            } else if (opcode == OP_PUSHDATA4) {
                additionalBytes = ((0xFF & inputScript[cursor]) |
                        ((0xFF & inputScript[cursor + 1]) << 8) |
                        ((0xFF & inputScript[cursor + 1]) << 16) |
                        ((0xFF & inputScript[cursor + 1]) << 24)) + 4;
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

    private static boolean equalsRange(byte[] a, int start, byte[] b) {
        if (start + b.length > a.length)
            return false;
        for (int i = 0; i < b.length; i++)
            if (a[i + start] != b[i])
                return false;
        return true;
    }

}
