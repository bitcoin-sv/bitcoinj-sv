/*
 * Modifications described in the NOTICE.txt file are licensed under the Open BSV Licence.
 * Modifications Copyright 2020 Bitcoin Association
 *
 * Copyright 2013 Google Inc.
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

package io.bitcoinsv.bitcoinjsv.script;

import com.google.common.collect.Lists;
import io.bitcoinsv.bitcoinjsv.core.Addressable;
import io.bitcoinsv.bitcoinjsv.ecc.ECKeyBytes;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.ecc.TransactionSignature;
import io.bitcoinsv.bitcoinjsv.script.interpreter.StackItem;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Tools for the construction of commonly used script types. You don't normally need this as it's hidden behind
 * convenience methods on {Transaction}, but they are useful when working with the
 * protocol at a lower level.</p>
 */
public class ScriptBuilder {
    private List<ScriptChunk> chunks;

    /**
     * Creates a fresh ScriptBuilder with an empty program.
     */
    public ScriptBuilder() {
        chunks = Lists.newLinkedList();
    }

    /**
     * Creates a fresh ScriptBuilder with the given program as the starting point.
     */
    public ScriptBuilder(Script template) {
        chunks = new ArrayList<ScriptChunk>(template.getChunks());
    }

    /**
     * Adds the given chunk to the end of the program
     */
    public ScriptBuilder addChunk(ScriptChunk chunk) {
        return addChunk(chunks.size(), chunk);
    }

    /**
     * Adds the given chunk at the given index in the program
     */
    public ScriptBuilder addChunk(int index, ScriptChunk chunk) {
        chunks.add(index, chunk);
        return this;
    }

    /**
     * Adds the given opcode to the end of the program.
     */
    public ScriptBuilder op(int opcode) {
        return op(chunks.size(), opcode);
    }

    /**
     * Adds the given opcode to the end of the program.
     */
    public ScriptBuilder op(int opcode, Object context) {
        return op(chunks.size(), opcode, context);
    }

    /**
     * Adds the given opcode to the given index in the program
     */
    public ScriptBuilder op(int index, int opcode) {
        return op(index, opcode, null);
    }

    /**
     * Adds the given opcode to the given index in the program
     */
    public ScriptBuilder op(int index, int opcode, Object context) {
        checkArgument(opcode > ScriptOpCodes.OP_PUSHDATA4);
        return addChunk(index, new ScriptChunk(opcode, (ScriptData) null, context));
    }

    public ScriptBuilder directive(String directive, Object context, boolean isBreakpointSoft, boolean isBreakpointHard) {
        return addChunk(chunks.size(), new ScriptChunk(ScriptOpCodes.OP_INVALIDOPCODE, null, null, -1, context, true, directive, isBreakpointSoft, isBreakpointHard));
    }

    /**
     * Adds a copy of the given byte array as a data element (i.e. PUSHDATA) at the end of the program.
     */
    public ScriptBuilder data(byte[] data) {
        return data(data, null);
    }

    /**
     * Adds a copy of the given byte array as a data element (i.e. PUSHDATA) at the end of the program.
     */
    public ScriptBuilder data(byte[] data, Object context) {
        return data(ScriptData.of(data), context);
    }

    /**
     * Adds a copy of the given byte array as a data element (i.e. PUSHDATA) at the end of the program.
     */
    public ScriptBuilder data(ScriptData data, Object context) {
        return data(data, null, context);
    }

    public ScriptBuilder data(ScriptData data, StackItem.Type type, Object context) {
        if (data.length() == 0)
            return smallNum(0, context);
        else
            return data(chunks.size(), data, type, context);
    }

    public ScriptBuilder data(int index, byte[] data) {
        return data(index, ScriptData.of(data));
    }

    /**
     * Adds a copy of the given byte array as a data element (i.e. PUSHDATA) at the given index in the program.
     */
    public ScriptBuilder data(int index, ScriptData data) {
        return data(index, data, null);
    }

    /**
     * Adds a copy of the given byte array as a data element (i.e. PUSHDATA) at the given index in the program.
     */
    public ScriptBuilder data(int index, byte[] data, Object context) {
        return data(index, ScriptData.of(data), context);
    }

    public ScriptBuilder data(int index, ScriptData data, Object context) {
        return data(index, data, null, context);
    }

    /**
     * Adds a copy of the given byte array as a data element (i.e. PUSHDATA) at the given index in the program.
     */
    public ScriptBuilder data(int index, ScriptData data, StackItem.Type type, Object context) {
        // implements BIP62
        ScriptData copy = data.copy();
        int opcode;
        if (data.length() == 0) {
            return addChunk(index, new ScriptChunk(ScriptOpCodes.OP_0, (ScriptData) null, type, -1, context));
        } else if (data.length() == 1) {
            byte b = data.data()[0];
            if (b >= 1 && b <= 16)
                return addChunk(index, new ScriptChunk(ScriptOpCodes.encodeToOpN(b), (ScriptData) null, type, -1, context));
            else
                opcode = 1;
        } else if (data.length() < ScriptOpCodes.OP_PUSHDATA1) {
            opcode = data.length();
        } else if (data.length() < 256) {
            opcode = ScriptOpCodes.OP_PUSHDATA1;
        } else if (data.length() < 65536) {
            opcode = ScriptOpCodes.OP_PUSHDATA2;
        } else if (data.length() <= Integer.MAX_VALUE) {
            //note the largest possible size is actually uint32.MAX_VALUE
            //but due to java arrays being integer indexed it is not possible to handle
            //data chunks larger that 2^31-1
            opcode = ScriptOpCodes.OP_PUSHDATA4;
        } else {
            throw new RuntimeException("Unimplemented");
        }
        return addChunk(index, new ScriptChunk(opcode, copy, type, -1, context));
    }

    /**
     * Adds the given number to the end of the program. Automatically uses
     * shortest encoding possible.
     */
    public ScriptBuilder number(long num) {
        return number(num, null);
    }


    /**
     * Adds the given number to the end of the program. Automatically uses
     * shortest encoding possible.
     */
    public ScriptBuilder number(long num, Object context) {
        if (num >= 0 && num <= 16) {
            return smallNum((int) num, context);
        } else {
            return bigNum(num, context);
        }
    }

    /**
     * Adds the given number to the given index in the program. Automatically
     * uses shortest encoding possible.
     */
    public ScriptBuilder number(int index, long num) {
        return number(index, num, null);
    }

    /**
     * Adds the given number to the given index in the program. Automatically
     * uses shortest encoding possible.
     */
    public ScriptBuilder number(int index, long num, Object context) {
        if (num >= 0 && num <= 16) {
            return addChunk(index, new ScriptChunk(ScriptOpCodes.encodeToOpN((int) num), (ScriptData) null, context));
        } else {
            return bigNum(index, num, context);
        }
    }

    /**
     * Adds the given number as a OP_N opcode to the end of the program.
     * Only handles values 0-16 inclusive.
     *
     * @see #number(long)
     */
    public ScriptBuilder smallNum(int num) {
        return smallNum(num, null);
    }

    /**
     * Adds the given number as a OP_N opcode to the end of the program.
     * Only handles values 0-16 inclusive.
     *
     * @see #number(long)
     */
    public ScriptBuilder smallNum(int num, Object context) {
        return smallNum(chunks.size(), num, context);
    }

    /**
     * Adds the given number as a push data chunk.
     * This is intended to use for negative numbers or values > 16, and although
     * it will accept numbers in the range 0-16 inclusive, the encoding would be
     * considered non-standard.
     *
     * @see #number(long)
     */
    protected ScriptBuilder bigNum(long num) {
        return bigNum(num, null);
    }

    /**
     * Adds the given number as a push data chunk.
     * This is intended to use for negative numbers or values > 16, and although
     * it will accept numbers in the range 0-16 inclusive, the encoding would be
     * considered non-standard.
     *
     * @see #number(long)
     */
    protected ScriptBuilder bigNum(long num, Object context) {
        return bigNum(chunks.size(), num, context);
    }

    /**
     * Adds the given number as a OP_N opcode to the given index in the program.
     * Only handles values 0-16 inclusive.
     *
     * @see #number(long)
     */
    public ScriptBuilder smallNum(int index, int num) {
        return smallNum(index, num, null);
    }

    /**
     * Adds the given number as a OP_N opcode to the given index in the program.
     * Only handles values 0-16 inclusive.
     *
     * @see #number(long)
     */
    public ScriptBuilder smallNum(int index, int num, Object context) {
        checkArgument(num >= 0, "Cannot encode negative numbers with smallNum");
        checkArgument(num <= 16, "Cannot encode numbers larger than 16 with smallNum");
        return addChunk(index, new ScriptChunk(ScriptOpCodes.encodeToOpN(num), (ScriptData) null, context));
    }

    /**
     * Adds the given number as a push data chunk to the given index in the program.
     * This is intended to use for negative numbers or values > 16, and although
     * it will accept numbers in the range 0-16 inclusive, the encoding would be
     * considered non-standard.
     *
     * @see #number(long)
     */
    protected ScriptBuilder bigNum(int index, long num, Object context) {
        final byte[] data;

        if (num == 0) {
            data = new byte[0];
        } else {
            Stack<Byte> result = new Stack<Byte>();
            final boolean neg = num < 0;
            long absvalue = Math.abs(num);

            while (absvalue != 0) {
                result.push((byte) (absvalue & 0xff));
                absvalue >>= 8;
            }

            if ((result.peek() & 0x80) != 0) {
                // The most significant byte is >= 0x80, so push an extra byte that
                // contains just the sign of the value.
                result.push((byte) (neg ? 0x80 : 0));
            } else if (neg) {
                // The most significant byte is < 0x80 and the value is negative,
                // set the sign bit so it is subtracted and interpreted as a
                // negative when converting back to an integral.
                result.push((byte) (result.pop() | 0x80));
            }

            data = new byte[result.size()];
            for (int byteIdx = 0; byteIdx < data.length; byteIdx++) {
                data[byteIdx] = result.get(byteIdx);
            }
        }

        // At most the encoded value could take up to 8 bytes, so we don't need
        // to use OP_PUSHDATA opcodes
        return addChunk(index, new ScriptChunk(data.length, data, context));
    }

    /**
     * Creates a new immutable Script based on the state of the builder.
     */
    public Script build() {
        return new Script(chunks);
    }

    /**
     * Creates a scriptPubKey that encodes payment to the given address.
     */
    public static Script createOutputScript(Addressable to) {
        if (to.isP2SHAddress()) {
            // OP_HASH160 <scriptHash> OP_EQUAL
            return new ScriptBuilder()
                    .op(ScriptOpCodes.OP_HASH160)
                    .data(to.getHash160())
                    .op(ScriptOpCodes.OP_EQUAL)
                    .build();
        } else {
            // OP_DUP OP_HASH160 <pubKeyHash> OP_EQUALVERIFY OP_CHECKSIG
            return new ScriptBuilder()
                    .op(ScriptOpCodes.OP_DUP)
                    .op(ScriptOpCodes.OP_HASH160)
                    .data(to.getHash160())
                    .op(ScriptOpCodes.OP_EQUALVERIFY)
                    .op(ScriptOpCodes.OP_CHECKSIG)
                    .build();
        }
    }

    /**
     * Creates a scriptPubKey that encodes payment to the given raw public key.
     */
    public static Script createOutputScript(ECKeyBytes key) {
        return new ScriptBuilder().data(key.getPubKey()).op(ScriptOpCodes.OP_CHECKSIG).build();
    }

    public static Script createInputScript(@Nullable TransactionSignature signature, ECKeyBytes pubKey) {
        return createInputScript(signature, pubKey.getPubKey());
    }

    /**
     * Creates a scriptSig that can redeem a pay-to-address output.
     * If given signature is null, incomplete scriptSig will be created with OP_0 instead of signature
     */
    public static Script createInputScript(@Nullable TransactionSignature signature, byte[] pubkeyBytes) {
        byte[] sigBytes = signature != null ? signature.encodeToBitcoin() : new byte[]{};
        return new ScriptBuilder().data(sigBytes).data(pubkeyBytes).build();
    }

    /**
     * Creates a scriptSig that can redeem a pay-to-pubkey output.
     * If given signature is null, incomplete scriptSig will be created with OP_0 instead of signature
     */
    public static Script createInputScript(@Nullable TransactionSignature signature) {
        byte[] sigBytes = signature != null ? signature.encodeToBitcoin() : new byte[]{};
        return new ScriptBuilder().data(sigBytes).build();
    }

    /**
     * Creates a program that requires at least N of the given keys to sign, using OP_CHECKMULTISIG.
     */
    public static Script createMultiSigOutputScript(int threshold, List<? extends ECKeyBytes> pubkeys) {
        checkArgument(threshold > 0);
        checkArgument(threshold <= pubkeys.size());
        checkArgument(pubkeys.size() <= 16);  // That's the max we can represent with a single opcode.
        ScriptBuilder builder = new ScriptBuilder();
        builder.smallNum(threshold);
        for (ECKeyBytes key : pubkeys) {
            builder.data(key.getPubKey());
        }
        builder.smallNum(pubkeys.size());
        builder.op(ScriptOpCodes.OP_CHECKMULTISIG);
        return builder.build();
    }

    /**
     * Create a program that satisfies an OP_CHECKMULTISIG program.
     */
    public static Script createMultiSigInputScript(List<TransactionSignature> signatures) {
        List<byte[]> sigs = new ArrayList<byte[]>(signatures.size());
        for (TransactionSignature signature : signatures) {
            sigs.add(signature.encodeToBitcoin());
        }

        return createMultiSigInputScriptBytes(sigs, null);
    }

    /**
     * Create a program that satisfies an OP_CHECKMULTISIG program.
     */
    public static Script createMultiSigInputScript(TransactionSignature... signatures) {
        return createMultiSigInputScript(Arrays.asList(signatures));
    }

    /**
     * Create a program that satisfies an OP_CHECKMULTISIG program, using pre-encoded signatures.
     */
    public static Script createMultiSigInputScriptBytes(List<byte[]> signatures) {
        return createMultiSigInputScriptBytes(signatures, null);
    }

    /**
     * Create a program that satisfies a pay-to-script hashed OP_CHECKMULTISIG program.
     * If given signature list is null, incomplete scriptSig will be created with OP_0 instead of signatures
     */
    public static Script createP2SHMultiSigInputScript(@Nullable List<TransactionSignature> signatures,
                                                       Script multisigProgram) {
        List<byte[]> sigs = new ArrayList<byte[]>();
        if (signatures == null) {
            // create correct number of empty signatures
            int numSigs = multisigProgram.getNumberOfSignaturesRequiredToSpend();
            for (int i = 0; i < numSigs; i++)
                sigs.add(new byte[]{});
        } else {
            for (TransactionSignature signature : signatures) {
                sigs.add(signature.encodeToBitcoin());
            }
        }
        return createMultiSigInputScriptBytes(sigs, multisigProgram.getProgram());
    }

    /**
     * Create a program that satisfies an OP_CHECKMULTISIG program, using pre-encoded signatures.
     * Optionally, appends the script program bytes if spending a P2SH output.
     */
    public static Script createMultiSigInputScriptBytes(List<byte[]> signatures, @Nullable byte[] multisigProgramBytes) {
        checkArgument(signatures.size() <= 16);
        ScriptBuilder builder = new ScriptBuilder();
        builder.smallNum(0);  // Work around a bug in CHECKMULTISIG that is now a required part of the protocol.
        for (byte[] signature : signatures)
            builder.data(signature);
        if (multisigProgramBytes != null)
            builder.data(multisigProgramBytes);
        return builder.build();
    }

    /**
     * Returns a copy of the given scriptSig with the signature inserted in the given position.
     * <p>
     * This function assumes that any missing sigs have OP_0 placeholders. If given scriptSig already has all the signatures
     * in place, IllegalArgumentException will be thrown.
     *
     * @param targetIndex     where to insert the signature
     * @param sigsPrefixCount how many items to copy verbatim (e.g. initial OP_0 for multisig)
     * @param sigsSuffixCount how many items to copy verbatim at end (e.g. redeemScript for P2SH)
     */
    public static Script updateScriptWithSignature(Script scriptSig, byte[] signature, int targetIndex,
                                                   int sigsPrefixCount, int sigsSuffixCount) {
        ScriptBuilder builder = new ScriptBuilder();
        List<ScriptChunk> inputChunks = scriptSig.getChunks();
        int totalChunks = inputChunks.size();

        // Check if we have a place to insert, otherwise just return given scriptSig unchanged.
        // We assume here that OP_0 placeholders always go after the sigs, so
        // to find if we have sigs missing, we can just check the chunk in latest sig position
        boolean hasMissingSigs = inputChunks.get(totalChunks - sigsSuffixCount - 1).equalsOpCode(ScriptOpCodes.OP_0);
        checkArgument(hasMissingSigs, "ScriptSig is already filled with signatures");

        // copy the prefix
        for (ScriptChunk chunk : inputChunks.subList(0, sigsPrefixCount))
            builder.addChunk(chunk);

        // copy the sigs
        int pos = 0;
        boolean inserted = false;
        for (ScriptChunk chunk : inputChunks.subList(sigsPrefixCount, totalChunks - sigsSuffixCount)) {
            if (pos == targetIndex) {
                inserted = true;
                builder.data(signature);
                pos++;
            }
            if (!chunk.equalsOpCode(ScriptOpCodes.OP_0)) {
                builder.addChunk(chunk);
                pos++;
            }
        }

        // add OP_0's if needed, since we skipped them in the previous loop
        while (pos < totalChunks - sigsPrefixCount - sigsSuffixCount) {
            if (pos == targetIndex) {
                inserted = true;
                builder.data(signature);
            } else {
                builder.addChunk(new ScriptChunk(ScriptOpCodes.OP_0, null));
            }
            pos++;
        }

        // copy the suffix
        for (ScriptChunk chunk : inputChunks.subList(totalChunks - sigsSuffixCount, totalChunks))
            builder.addChunk(chunk);

        checkState(inserted);
        return builder.build();
    }

    /**
     * Creates a scriptPubKey that sends to the given script hash. Read
     * <a href="https://github.com/bitcoin/bips/blob/master/bip-0016.mediawiki">BIP 16</a> to learn more about this
     * kind of script.
     */
    public static Script createP2SHOutputScript(byte[] hash) {
        checkArgument(hash.length == 20);
        return new ScriptBuilder().op(ScriptOpCodes.OP_HASH160).data(hash).op(ScriptOpCodes.OP_EQUAL).build();
    }

    /**
     * Creates a scriptPubKey for the given redeem script.
     */
    public static Script createP2SHOutputScript(Script redeemScript) {
        byte[] hash = Utils.sha256hash160(redeemScript.getProgram());
        return ScriptBuilder.createP2SHOutputScript(hash);
    }

    /**
     * Creates a P2SH output script with given public keys and threshold. Given public keys will be placed in
     * redeem script in the lexicographical sorting order.
     */
    public static Script createP2SHOutputScript(int threshold, List<? extends ECKeyBytes> pubkeys) {
        Script redeemScript = createRedeemScript(threshold, pubkeys);
        return createP2SHOutputScript(redeemScript);
    }

    /**
     * Creates redeem script with given public keys and threshold. Given public keys will be placed in
     * redeem script in the lexicographical sorting order.
     */
    public static Script createRedeemScript(int threshold, List<? extends ECKeyBytes> pubkeys) {
        pubkeys = new ArrayList<ECKeyBytes>(pubkeys);
        Collections.sort(pubkeys, ECKeyBytes.PUBKEY_COMPARATOR);
        return ScriptBuilder.createMultiSigOutputScript(threshold, pubkeys);
    }

    /**
     * Creates a script of the form OP_RETURN [data]. This feature allows you to attach a small piece of data (like
     * a hash of something stored elsewhere) to a zero valued output which can never be spent and thus does not pollute
     * the ledger.
     */
    public static Script createOpReturnScript(byte[] data) {
        checkArgument(data.length <= 80);
        return new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(data).build();
    }

    public static Script createCLTVPaymentChannelOutput(BigInteger time, ECKeyBytes from, ECKeyBytes to) {
        byte[] timeBytes = Utils.reverseBytes(Utils.encodeMPI(time, false));
        if (timeBytes.length > 5) {
            throw new RuntimeException("Time too large to encode as 5-byte int");
        }
        return new ScriptBuilder().op(ScriptOpCodes.OP_IF)
                .data(to.getPubKey()).op(ScriptOpCodes.OP_CHECKSIGVERIFY)
                .op(ScriptOpCodes.OP_ELSE)
                .data(timeBytes).op(ScriptOpCodes.OP_CHECKLOCKTIMEVERIFY).op(ScriptOpCodes.OP_DROP)
                .op(ScriptOpCodes.OP_ENDIF)
                .data(from.getPubKey()).op(ScriptOpCodes.OP_CHECKSIG).build();
    }

    public static Script createCLTVPaymentChannelRefund(TransactionSignature signature) {
        ScriptBuilder builder = new ScriptBuilder();
        builder.data(signature.encodeToBitcoin());
        builder.data(new byte[]{0}); // Use the CHECKLOCKTIMEVERIFY if branch
        return builder.build();
    }

    public static Script createCLTVPaymentChannelP2SHRefund(TransactionSignature signature, Script redeemScript) {
        ScriptBuilder builder = new ScriptBuilder();
        builder.data(signature.encodeToBitcoin());
        builder.data(new byte[]{0}); // Use the CHECKLOCKTIMEVERIFY if branch
        builder.data(redeemScript.getProgram());
        return builder.build();
    }

    public static Script createCLTVPaymentChannelP2SHInput(byte[] from, byte[] to, Script redeemScript) {
        ScriptBuilder builder = new ScriptBuilder();
        builder.data(from);
        builder.data(to);
        builder.smallNum(1); // Use the CHECKLOCKTIMEVERIFY if branch
        builder.data(redeemScript.getProgram());
        return builder.build();
    }

    public static Script createCLTVPaymentChannelInput(TransactionSignature from, TransactionSignature to) {
        return createCLTVPaymentChannelInput(from.encodeToBitcoin(), to.encodeToBitcoin());
    }

    public static Script createCLTVPaymentChannelInput(byte[] from, byte[] to) {
        ScriptBuilder builder = new ScriptBuilder();
        builder.data(from);
        builder.data(to);
        builder.smallNum(1); // Use the CHECKLOCKTIMEVERIFY if branch
        return builder.build();
    }
}
