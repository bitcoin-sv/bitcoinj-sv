package org.bitcoinj.script.interpreter;

import org.bitcoinj.core.*;
import org.bitcoinj.ecc.TransactionSignature;
import org.bitcoinj.ecc.ECDSA;
import org.bitcoinj.msg.bitcoin.api.base.Tx;
import org.bitcoinj.script.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.digests.RIPEMD160Digest;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.bitcoinj.script.ScriptOpCodes.*;

/**
 * This is the actual script interpreter than runs Bitcoin Scripts.  It is completely stateless and contians
 * only static methods.  The script itself is now encapsulated in the Script class which uses only instance methods.
 *
 * Some other utility methods are in ScriptUtils, this is main there to assist in untangling the web of dependencies
 * so we can break BitcoinJ up into logically grouped libs.
 */
public class Interpreter {

    static final Logger log = LoggerFactory.getLogger(Interpreter.class);

    public static final long MAX_SCRIPT_ELEMENT_SIZE = 520;  // bytes
    public static final int MAX_NUM_ELEMENT_SIZE_PRE_GENESIS = 4;
    public static final int MAX_NUM_ELEMENT_SIZE_POST_GENESIS = 750000;
    public static final int MAX_MULTISIG_PUBKEYS_PRE_GENESIS = 20;
    public static final int MAX_MULTISIG_PUBKEYS_POST_GENESIS = Integer.MAX_VALUE;
    public static final int MAX_OPCOUNT_PRE_MAGNETIC = 201;
    public static final int MAX_OPCOUNT_PRE_GENESIS = 500;
    public static final long MAX_STACK_MEMORY_USAGE_CONSENSUS = 100 * 1000 * 1000;

    //ugly hack for obtaining script state when exceptions are thrown if they haven't
    //been passed to the ScriptException constructor. This should be safe as scripts
    //run single threaded.
    public static final ThreadLocal SCRIPT_STATE_THREADLOCAL = new ThreadLocal();

    private static final int[] RSHIFT_MASKS = new int[]{0xFF, 0xFE, 0xFC, 0xF8, 0xF0, 0xE0, 0xC0, 0x80};
    private static final int[] LSHIFT_MASKS = new int[]{0xFF, 0x7F, 0x3F, 0x1F, 0x0F, 0x07, 0x03, 0x01};


    ////////////////////// Script verification and helpers ////////////////////////////////

    public static boolean castToBool(StackItem data) {
        return castToBool(data.bytes());
    }

    public static boolean castToBool(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            // "Can be negative zero" - Bitcoin Core (see OpenSSL's BN_bn2mpi)
            if (data[i] != 0)
                return !(i == data.length - 1 && (data[i] & 0xFF) == 0x80);
        }
        return false;
    }

    /**
     * Cast a script chunk to a BigInteger.
     *
     * @throws ScriptExecutionException if the chunk is longer than 4 bytes.
     * @see #castToBigInteger(byte[], boolean) for values with different maximum
     * sizes.
     */
    private static BigInteger castToBigInteger(ScriptExecutionState state, StackItem chunk, boolean enforceMinimal) throws ScriptExecutionException {
        if (chunk.length() > MAX_NUM_ELEMENT_SIZE_PRE_GENESIS)
            throw new ScriptExecutionException(state, "Script attempted to use an integer larger than 4 bytes");
        if (enforceMinimal && !Utils.checkMinimallyEncodedLE(chunk.bytes(), MAX_NUM_ELEMENT_SIZE_PRE_GENESIS))
            throw new ScriptExecutionException(state, "Number is not minimally encoded");
        //numbers on the stack or stored LE so convert as MPI requires BE.
        byte[] bytesBE = Utils.reverseBytes(chunk.bytes());
        return Utils.decodeMPI(bytesBE, false);
    }

    private static BigInteger castToBigInteger(byte[] chunk, boolean enforceMinimal) throws ScriptExecutionException {
        return castToBigInteger(null, StackItem.wrap(chunk), enforceMinimal);
    }

    /**
     * Cast a script chunk to a BigInteger. Before Genesis, normally you would want
     * {@link #castToBigInteger(byte[], boolean)} instead, this is only for cases where
     * the normal maximum length does not apply (i.e. CHECKLOCKTIMEVERIFY).
     * <p>
     * Post Genesis this is the default
     *
     * @param maxLength the maximum length in bytes.
     * @throws ScriptExecutionException if the chunk is longer than the specified maximum.
     */
    static BigInteger castToBigInteger(ScriptExecutionState state, final StackItem chunk, final int maxLength, boolean enforceMinimal) throws ScriptExecutionException {
        if (chunk.length() > maxLength)
            throw new ScriptExecutionException(state, "Script attempted to use an integer larger than "
                    + maxLength + " bytes");
        if (enforceMinimal && !Utils.checkMinimallyEncodedLE(chunk.bytes(), maxLength))
            throw new ScriptExecutionException(state, "Number is not minimally encoded");
        return Utils.decodeMPI(Utils.reverseBytes(chunk.bytes()), false);
    }

    /**
     * shift x right by n bits, implements OP_RSHIFT
     * see: https://github.com/bitcoin-sv/bitcoin-sv/commit/27d24de643dbd3cc852e1de7c90e752e19abb9d8
     * <p>
     * Note this does not support shifting more than Integer.MAX_VALUE
     *
     * @param xItem
     * @param n
     * @return
     */
    private static byte[] rShift(StackItem xItem, int n) {
        byte[] x = xItem.bytes();

        int bit_shift = n % 8;
        int byte_shift = n / 8;

        int mask = RSHIFT_MASKS[bit_shift];
        int overflow_mask = (~mask) & 0xff;

        byte[] result = new byte[x.length];
        for (int i = 0; i < x.length; i++) {
            int k = i + byte_shift;
            if (k < x.length) {
                int val = x[i] & mask;
                val = val >>> bit_shift;
                result[k] |= val;
            }

            if (k + 1 < x.length) {
                int carryval = x[i] & overflow_mask;
                carryval <<= 8 - bit_shift;
                result[k + 1] |= carryval;
            }
        }
        return result;
    }

    /**
     * shift x left by n bits, implements OP_LSHIFT
     * see: https://github.com/bitcoin-sv/bitcoin-sv/commit/27d24de643dbd3cc852e1de7c90e752e19abb9d8
     * <p>
     * Note this does not support shifting more than Integer.MAX_VALUE
     *
     * @param xItem
     * @param n
     * @return
     */
    private static byte[] lShift(StackItem xItem, int n) {
        byte[] x = xItem.bytes();
        int bit_shift = n % 8;
        int byte_shift = n / 8;

        int mask = LSHIFT_MASKS[bit_shift];
        int overflow_mask = (~mask) & 0xff;

        byte[] result = new byte[x.length];
        for (int i = x.length - 1; i >= 0; i--) {
            int k = i - byte_shift;
            if (k >= 0) {
                int val = x[i] & mask;
                val <<= bit_shift;
                result[k] |= val;
            }

            if (k - 1 >= 0) {
                int carryval = x[i] & overflow_mask;
                carryval = carryval >>> 8 - bit_shift;
                result[k - 1] |= carryval;
            }
        }
        return result;
    }

    /**
     * Exposes the script interpreter. Normally you should not use this directly, instead use
     * { Script#correctlySpends(Tx, long, Script)}. This method
     * is useful if you need more precise control or access to the final state of the stack. This interface is very
     * likely to change in future.
     *
     * @deprecated Use {@link #executeScript(Tx, long, Script, ScriptStack, Set)}
     * instead.
     */
    @Deprecated
    public static void executeScript(@Nullable Tx txContainingThis, long index,
                                     Script script, ScriptStack stack, boolean enforceNullDummy) throws ScriptExecutionException {
        final EnumSet<ScriptVerifyFlag> flags = enforceNullDummy
                ? EnumSet.of(ScriptVerifyFlag.NULLDUMMY)
                : EnumSet.noneOf(ScriptVerifyFlag.class);

        executeScript(txContainingThis, index, script, stack, Coin.ZERO, flags);
    }

    @Deprecated
    public static void executeScript(@Nullable Tx txContainingThis, long index,
                                     Script script, ScriptStack stack, Set<ScriptVerifyFlag> verifyFlags) throws ScriptExecutionException {
        executeScript(txContainingThis, index, script, stack, Coin.ZERO, verifyFlags);
    }

    private static boolean isOpcodeDisabled(int opcode, Set<ScriptVerifyFlag> verifyFlags) {


        switch (opcode) {

            case OP_2MUL:
            case OP_2DIV:
                //disabled codes
                return true;

            case OP_INVERT:
            case OP_LSHIFT:
            case OP_RSHIFT:
            case OP_MUL:
                //enabled codes, still disabled if flag is not activated
                return !verifyFlags.contains(ScriptVerifyFlag.MAGNETIC_OPCODES);

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
                return !verifyFlags.contains(ScriptVerifyFlag.MONOLITH_OPCODES);

            default:
                //not an opcode that was ever disabled
                break;
        }


        return false;

    }

    /**
     * Exposes the script interpreter. Normally you should not use this directly, instead use
     * { Script#correctlySpends(Tx, long, Script)}. This method
     * is useful if you need more precise control or access to the final state of the stack. This interface is very
     * likely to change in future.
     */
    public static void executeScript(@Nullable Tx txContainingThis, long index,
                                     Script script, ScriptStack stack, Coin value, Set<ScriptVerifyFlag> verifyFlags) throws ScriptExecutionException {
        executeScript(txContainingThis, index, new SimpleScriptStream(script), stack, value, verifyFlags, null);
    }

    /**
     * Executes a script in debug mode with the provided ScriptStateListener.  Exceptions (which are thrown when a script fails) are caught
     * and passed to the listener before being rethrown.
     */
    public static void executeDebugScript(@Nullable Tx txContainingThis, long index,
                                          ScriptStream script, ScriptStack stack, Coin value, Set<ScriptVerifyFlag> verifyFlags, ScriptStateListener scriptStateListener) throws ScriptExecutionException {
        try {
            executeScript(txContainingThis, index, script, stack, value, verifyFlags, scriptStateListener);
        } catch (ScriptExecutionException e) {
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

    public static void executeScript(@Nullable Tx txContainingThis, long index,
                                     ScriptStream script, ScriptStack stack, Coin value,
                                     Set<ScriptVerifyFlag> verifyFlags, ScriptStateListener scriptStateListener) throws ScriptExecutionException {
        executeScript(txContainingThis, index, script, stack, value, verifyFlags, scriptStateListener, false, 0L);
    }

    /**
     * Exposes the script interpreter. Normally you should not use this directly, instead use
     * { Script#correctlySpends(Tx, long, Script)}. This method
     * is useful if you need more precise control or access to the final state of the stack. This interface is very
     * likely to change in future.
     */
    public static void executeScript(@Nullable Tx txContainingThis, long index,
                                     ScriptStream script, ScriptStack stack, Coin value,
                                     Set<ScriptVerifyFlag> verifyFlags, ScriptStateListener scriptStateListener,
                                     boolean allowFakeChecksig, long fakeChecksigDelay) throws ScriptExecutionException {
        int opCount = 0;
        int lastCodeSepLocation = 0;

        //for scriptSig this can be set to true as the stack state is known to be empty
        boolean initialStackStateKnown = false;

        //mark all stack items as derived if initial stack state is not known to this execution context
        stack.setDerivations(!initialStackStateKnown);
        ScriptStack altstack = new ScriptStack();
        LinkedList<Boolean> ifStack = new LinkedList<Boolean>();
        final boolean enforceMinimal = verifyFlags.contains(ScriptVerifyFlag.MINIMALDATA);
        final boolean genesisActive = verifyFlags.contains(ScriptVerifyFlag.GENESIS_OPCODES);
        final long maxScriptElementSize = genesisActive ? Long.MAX_VALUE : MAX_SCRIPT_ELEMENT_SIZE;
        final int maxNumElementSize = genesisActive ? MAX_NUM_ELEMENT_SIZE_POST_GENESIS : MAX_NUM_ELEMENT_SIZE_PRE_GENESIS;
        final int maxMultisigKeys = genesisActive ? MAX_MULTISIG_PUBKEYS_POST_GENESIS : MAX_MULTISIG_PUBKEYS_PRE_GENESIS;
        final int maxOpCount = genesisActive ? Integer.MAX_VALUE :
                verifyFlags.contains(ScriptVerifyFlag.MAGNETIC_OPCODES) ? MAX_OPCOUNT_PRE_GENESIS :
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

        boolean opReturnCalled = false;

        //initialise script state tracker
        ScriptExecutionState state = new ScriptExecutionState();
        state.stack = stack;
        state.stackPopped = stack.getPoppedItems();
        state.altStack = altstack;
        state.altStackPopped = altstack.getPoppedItems();
        state.ifStack = ifStack;
        state.opCount = 0;
        state.verifyFlags = verifyFlags;
        state.script = script;
        state.initialStackStateKnown = initialStackStateKnown;
        SCRIPT_STATE_THREADLOCAL.set(state);

        for (ScriptChunk chunk : script) {
            state.lastOpCode = state.currentOpCode;
            state.currentOpCode = chunk;
            state.currentOpCodeIndex++;

            //clear tracked popped items from stack
            stack.clearPoppedItems();
            altstack.clearPoppedItems();

            boolean shouldExecute = !ifStack.contains(false);

            if (scriptStateListener != null) {
                scriptStateListener._onBeforeOpCodeExecuted(chunk, shouldExecute);
            }

            if (chunk.opcode == OP_0) {
                if (!shouldExecute)
                    continue;

                stack.add(new byte[]{});
            } else if (!chunk.isOpCode()) {
                if (chunk.data.length() > maxScriptElementSize)
                    throw new ScriptExecutionException(state, "Attempted to push a data string larger than 520 bytes");

                if (!shouldExecute)
                    continue;

                stack.add(chunk.data());
            } else {
                int opcode = chunk.opcode;
                if (opcode > OP_16) {
                    opCount++;
                    state.opCount = opCount;
                    if (opCount > maxOpCount)
                        throw new ScriptExecutionException(state, "More script operations than is allowed");
                }

                if (opcode == OP_VERIF || opcode == OP_VERNOTIF)
                    throw new ScriptExecutionException(state, "Script included OP_VERIF or OP_VERNOTIF");

                // Some opcodes are disabled.
                if (isOpcodeDisabled(opcode, verifyFlags)) {
                    throw new ScriptExecutionException(state, "Script included a disabled Script Op.");
                }

                switch (opcode) {
                    case OP_IF:
                        if (!shouldExecute) {
                            ifStack.add(false);
                            continue;
                        }
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_IF on an empty stack");
                        ifStack.add(castToBool(stack.pollLast().bytes()));
                        continue;
                    case OP_NOTIF:
                        if (!shouldExecute) {
                            ifStack.add(false);
                            continue;
                        }
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_NOTIF on an empty stack");
                        ifStack.add(!castToBool(stack.pollLast().bytes()));
                        continue;
                    case OP_ELSE:
                        if (ifStack.isEmpty())
                            throw new ScriptExecutionException(state, "Attempted OP_ELSE without OP_IF/NOTIF");
                        ifStack.add(!ifStack.pollLast());
                        continue;
                    case OP_ENDIF:
                        if (ifStack.isEmpty())
                            throw new ScriptExecutionException(state, "Attempted OP_ENDIF without OP_IF/NOTIF");
                        ifStack.pollLast();
                        continue;
                }

                if (!shouldExecute)
                    continue;

                switch (opcode) {
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
                        stack.add(StackItem.forSmallNum(decodeFromOpN(opcode)));
                        break;
                    case OP_NOP:
                        break;
                    case OP_VERIFY:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_VERIFY on an empty stack");
                        if (!castToBool(stack.pollLast().bytes()))
                            throw new ScriptExecutionException(state, "OP_VERIFY failed");
                        break;
                    case OP_RETURN:
                        if (genesisActive) {
                            //will exit at end of loop so all checks are completed.
                            opReturnCalled = true;
                        } else {
                            throw new ScriptExecutionException(state, "Script called OP_RETURN");
                        }
                    case OP_TOALTSTACK:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_TOALTSTACK on an empty stack");
                        altstack.add(stack.pollLast());
                        break;
                    case OP_FROMALTSTACK:
                        if (altstack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_FROMALTSTACK on an empty altstack");
                        stack.add(altstack.pollLast());
                        break;
                    case OP_2DROP:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_2DROP on a stack with size < 2");
                        stack.pollLast();
                        stack.pollLast();
                        break;
                    case OP_2DUP:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_2DUP on a stack with size < 2");
                        Iterator<StackItem> it2DUP = stack.descendingIterator();
                        StackItem OP2DUPtmpChunk2 = it2DUP.next();
                        stack.add(it2DUP.next());
                        stack.add(OP2DUPtmpChunk2);
                        break;
                    case OP_3DUP:
                        if (stack.size() < 3)
                            throw new ScriptExecutionException(state, "Attempted OP_3DUP on a stack with size < 3");
                        Iterator<StackItem> it3DUP = stack.descendingIterator();
                        StackItem OP3DUPtmpChunk3 = it3DUP.next();
                        StackItem OP3DUPtmpChunk2 = it3DUP.next();
                        stack.add(it3DUP.next());
                        stack.add(OP3DUPtmpChunk2);
                        stack.add(OP3DUPtmpChunk3);
                        break;
                    case OP_2OVER:
                        if (stack.size() < 4)
                            throw new ScriptExecutionException(state, "Attempted OP_2OVER on a stack with size < 4");
                        Iterator<StackItem> it2OVER = stack.descendingIterator();
                        it2OVER.next();
                        it2OVER.next();
                        StackItem OP2OVERtmpChunk2 = it2OVER.next();
                        stack.add(it2OVER.next());
                        stack.add(OP2OVERtmpChunk2);
                        break;
                    case OP_2ROT:
                        if (stack.size() < 6)
                            throw new ScriptExecutionException(state, "Attempted OP_2ROT on a stack with size < 6");
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
                            throw new ScriptExecutionException(state, "Attempted OP_2SWAP on a stack with size < 4");
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
                            throw new ScriptExecutionException(state, "Attempted OP_IFDUP on an empty stack");
                        StackItem ifdupBool = stack.getLast();
                        if (castToBool(ifdupBool.bytes()))
                            stack.add(stack.getLast(), ifdupBool);
                        break;
                    case OP_DEPTH:
                        //depth can't be known at runtime unless you already know the size of the initial stack.
                        stack.add(StackItem.wrapDerived(Utils.reverseBytes(Utils.encodeMPI(BigInteger.valueOf(stack.size()), false)), true));
                        break;
                    case OP_DROP:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_DROP on an empty stack");
                        stack.pollLast();
                        break;
                    case OP_DUP:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_DUP on an empty stack");
                        stack.add(stack.getLast());
                        break;
                    case OP_NIP:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_NIP on a stack with size < 2");
                        StackItem OPNIPtmpChunk = stack.pollLast();
                        stack.pollLast();
                        stack.add(OPNIPtmpChunk);
                        break;
                    case OP_OVER:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_OVER on a stack with size < 2");
                        Iterator<StackItem> itOVER = stack.descendingIterator();
                        itOVER.next();
                        stack.add(itOVER.next());
                        break;
                    case OP_PICK:
                    case OP_ROLL:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_PICK/OP_ROLL on an empty stack");

                        StackItem rollVal = stack.pollLast();

                        long val = castToBigInteger(state, rollVal, maxNumElementSize, enforceMinimal).longValue();
                        if (val < 0 || val >= stack.size())
                            throw new ScriptExecutionException(state, "OP_PICK/OP_ROLL attempted to get data deeper than stack size");
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
                            throw new ScriptExecutionException(state, "Attempted OP_ROT on a stack with size < 3");
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
                            throw new ScriptExecutionException(state, "Attempted OP_SWAP on a stack with size < 2");
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
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        StackItem catBytes2 = stack.pollLast();
                        StackItem catBytes1 = stack.pollLast();

                        int len = catBytes1.length() + catBytes2.length();
                        if (len > maxScriptElementSize)
                            throw new ScriptExecutionException(state, "Push value size limit exceeded.");

                        byte[] catOut = new byte[len];
                        System.arraycopy(catBytes1.bytes(), 0, catOut, 0, catBytes1.length());
                        System.arraycopy(catBytes2.bytes(), 0, catOut, catBytes1.length(), catBytes2.length());
                        stack.add(catOut, catBytes1, catBytes2);

                        break;

                    case OP_SPLIT:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        StackItem biSplitPosItem = stack.pollLast();
                        StackItem splitBytesItem = stack.pollLast();
                        BigInteger biSplitPos = castToBigInteger(state, biSplitPosItem, maxNumElementSize, enforceMinimal);

                        //sanity check in case we aren't enforcing minimal number encoding
                        //we will check that the biSplitPos value can be safely held in an int
                        //before we cast it as BigInteger will behave similar to casting if the value
                        //is greater than the target type can hold.
                        BigInteger biMaxInt = BigInteger.valueOf((long) Integer.MAX_VALUE);
                        if (biSplitPos.compareTo(biMaxInt) >= 0)
                            throw new ScriptExecutionException(state, "Invalid OP_SPLIT range.");

                        int splitPos = biSplitPos.intValue();
                        byte[] splitBytes = splitBytesItem.bytes();

                        if (splitPos > splitBytes.length || splitPos < 0)
                            throw new ScriptExecutionException(state, "Invalid OP_SPLIT range.");

                        byte[] splitOut1 = new byte[splitPos];
                        byte[] splitOut2 = new byte[splitBytes.length - splitPos];

                        System.arraycopy(splitBytes, 0, splitOut1, 0, splitPos);
                        System.arraycopy(splitBytes, splitPos, splitOut2, 0, splitOut2.length);

                        stack.add(splitOut1, splitBytesItem, biSplitPosItem);
                        stack.add(splitOut2, splitBytesItem, biSplitPosItem);
                        break;

                    case OP_NUM2BIN:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Invalid stack operation.");

                        StackItem numSizeItem = stack.pollLast();
                        int numSize = castToBigInteger(state, numSizeItem, maxNumElementSize, enforceMinimal).intValue();

                        if (numSize > maxScriptElementSize)
                            throw new ScriptExecutionException(state, "Push value size limit exceeded.");

                        StackItem rawNumItem = stack.pollLast();

                        // Try to see if we can fit that number in the number of
                        // byte requested.
                        byte[] minimalNumBytes = Utils.minimallyEncodeLE(rawNumItem.bytes());
                        if (minimalNumBytes.length > numSize) {
                            //we can't
                            throw new ScriptExecutionException(state, "The requested encoding is impossible to satisfy.");
                        }

                        if (minimalNumBytes.length == numSize) {
                            //already the right size so just push it to stack
                            stack.add(minimalNumBytes, numSizeItem, rawNumItem);
                        } else if (numSize == 0) {
                            stack.add(Utils.EMPTY_BYTE_ARRAY, numSizeItem, rawNumItem);
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
                            stack.add(expandedNumBytes, rawNumItem, numSizeItem);
                        }
                        break;

                    case OP_BIN2NUM:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        StackItem binBytes = stack.pollLast();
                        byte[] numBytes = Utils.minimallyEncodeLE(binBytes.bytes());

                        if (!Utils.checkMinimallyEncodedLE(numBytes, maxNumElementSize))
                            throw new ScriptExecutionException(state, "Given operand is not a number within the valid range [-2^31...2^31]");

                        stack.add(numBytes, binBytes);

                        break;
                    case OP_SIZE:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_SIZE on an empty stack");
                        StackItem sizeItem = stack.getLast();
                        stack.add(Utils.reverseBytes(Utils.encodeMPI(BigInteger.valueOf(sizeItem.length()), false)), sizeItem);
                        break;
                    case OP_INVERT:
                        // (x -- out)
                        if (stack.size() < 1) {
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        }
                        StackItem invertItem = stack.pollLast();
                        ScriptData invertBytes = invertItem.wrappedBytes().copy();
                        for (int i = 0; i < invertItem.length(); i++) {
                            invertBytes.data()[i] = (byte) ~invertItem.bytes()[i];
                        }
                        stack.add(StackItem.forBytes(invertBytes, invertItem.getType(), invertItem));
                        break;

                    case OP_AND:
                    case OP_OR:
                    case OP_XOR:
                        // (x1 x2 - out)
                        if (stack.size() < 2) {
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        }

                        //valtype &vch1 = stacktop(-2);
                        //valtype &vch2 = stacktop(-1);
                        StackItem vch2Item = stack.pollLast();
                        StackItem vch1Item = stack.pollLast();
                        byte[] vch2 = vch2Item.bytes();
                        //we will operate directly on vch1 bytes so ensure
                        //it's a copy so the original stack item isn't modified.
                        ScriptData vch1Bytes = vch1Item.wrappedBytes().copy();
                        byte[] vch1 = vch1Bytes.data();

                        // Inputs must be the same size
                        if (vch1.length != vch2.length) {
                            throw new ScriptExecutionException(state, "Invalid operand size.");
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

                        //put modified copy of vch1 back on stack
                        stack.add(StackItem.forBytes(vch1Bytes, vch1Item.getType(), vch1Item, vch2Item));

                        break;
                    case OP_LSHIFT:
                    case OP_RSHIFT:
                        // (x n -- out)
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        StackItem shiftNItem = stack.pollLast();
                        StackItem shiftData = stack.pollLast();
                        int shiftN = castToBigInteger(state, shiftNItem, maxNumElementSize, enforceMinimal).intValueExact();
                        if (shiftN < 0)
                            throw new ScriptExecutionException(state, "Invalid numer range.");

                        byte[] shifted;
                        switch (opcode) {
                            case OP_LSHIFT:
                                shifted = lShift(shiftData, shiftN);
                                break;
                            case OP_RSHIFT:
                                shifted = rShift(shiftData, shiftN);
                                break;
                            default:
                                throw new ScriptExecutionException(state, "switched opcode at runtime"); //can't happen
                        }
                        stack.add(shifted, shiftNItem, shiftData);

                        break;
                    case OP_EQUAL:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_EQUAL on a stack with size < 2");
                        StackItem eq2 = stack.pollLast();
                        StackItem eq1 = stack.pollLast();
                        byte[] eqResult = Objects.equals(eq2, eq1) ? new byte[]{1} : new byte[]{};
                        stack.add(eqResult, eq1, eq2);
                        break;
                    case OP_EQUALVERIFY:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_EQUALVERIFY on a stack with size < 2");
                        if (!Objects.equals(stack.pollLast(), stack.pollLast()))
                            throw new ScriptExecutionException(state, "OP_EQUALVERIFY: non-equal data");
                        break;
                    case OP_1ADD:
                    case OP_1SUB:
                    case OP_NEGATE:
                    case OP_ABS:
                    case OP_NOT:
                    case OP_0NOTEQUAL:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted a numeric op on an empty stack");
                        StackItem numericOpItem = stack.pollLast();
                        BigInteger numericOPnum = castToBigInteger(state, numericOpItem, maxNumElementSize, enforceMinimal);

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
                        throw new ScriptExecutionException(state, "Attempted to use disabled Script Op.");
                    case OP_ADD:
                    case OP_SUB:
                    case OP_DIV:
                    case OP_MUL:
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
                            throw new ScriptExecutionException(state, "Attempted a numeric op on a stack with size < 2");
                        StackItem numericOpItem2 = stack.pollLast();
                        StackItem numericOpItem1 = stack.pollLast();
                        BigInteger numericOPnum2 = castToBigInteger(state, numericOpItem2, maxNumElementSize, enforceMinimal);
                        BigInteger numericOPnum1 = castToBigInteger(state, numericOpItem1, maxNumElementSize, enforceMinimal);

                        BigInteger numericOPresult;
                        switch (opcode) {
                            case OP_ADD:
                                numericOPresult = numericOPnum1.add(numericOPnum2);
                                break;
                            case OP_SUB:
                                numericOPresult = numericOPnum1.subtract(numericOPnum2);
                                break;

                            case OP_MUL:
                                numericOPresult = numericOPnum1.multiply(numericOPnum2);
                                break;

                            case OP_DIV:
                                if (numericOPnum2.intValue() == 0)
                                    throw new ScriptExecutionException(state, "Division by zero error");
                                numericOPresult = numericOPnum1.divide(numericOPnum2);
                                break;

                            case OP_MOD:
                                if (numericOPnum2.intValue() == 0)
                                    throw new ScriptExecutionException(state, "Modulo by zero error");

                                /**
                                 * FIXME BigInteger doesn't behave the way we want for modulo operations.  Firstly it's
                                 * always garunteed to return a +ve result.  Secondly it will throw an exception
                                 * if the 2nd operand is negative.
                                 * The remainder method behaves as we expect
                                 */
                                numericOPresult = numericOPnum1.remainder(numericOPnum2);

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
                    case OP_NUMEQUALVERIFY:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_NUMEQUALVERIFY on a stack with size < 2");
                        BigInteger OPNUMEQUALVERIFYnum2 = castToBigInteger(state, stack.pollLast(), maxNumElementSize, enforceMinimal);
                        BigInteger OPNUMEQUALVERIFYnum1 = castToBigInteger(state, stack.pollLast(), maxNumElementSize, enforceMinimal);

                        if (!OPNUMEQUALVERIFYnum1.equals(OPNUMEQUALVERIFYnum2))
                            throw new ScriptExecutionException(state, "OP_NUMEQUALVERIFY failed");
                        break;
                    case OP_WITHIN:
                        if (stack.size() < 3)
                            throw new ScriptExecutionException(state, "Attempted OP_WITHIN on a stack with size < 3");
                        StackItem OPWITHINitem3 = stack.pollLast();
                        StackItem OPWITHINitem2 = stack.pollLast();
                        StackItem OPWITHINitem1 = stack.pollLast();
                        BigInteger OPWITHINnum3 = castToBigInteger(state, OPWITHINitem3, maxNumElementSize, enforceMinimal);
                        BigInteger OPWITHINnum2 = castToBigInteger(state, OPWITHINitem2, maxNumElementSize, enforceMinimal);
                        BigInteger OPWITHINnum1 = castToBigInteger(state, OPWITHINitem1, maxNumElementSize, enforceMinimal);
                        byte[] OPWITHINresult;
                        if (OPWITHINnum2.compareTo(OPWITHINnum1) <= 0 && OPWITHINnum1.compareTo(OPWITHINnum3) < 0)
                            OPWITHINresult = Utils.encodeMPI(BigInteger.ONE, false);
                        else
                            OPWITHINresult = Utils.encodeMPI(BigInteger.ZERO, false);
                        stack.add(Utils.reverseBytes(OPWITHINresult), OPWITHINitem1, OPWITHINitem2, OPWITHINitem3);
                        break;
                    case OP_RIPEMD160:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_RIPEMD160 on an empty stack");
                        RIPEMD160Digest digest = new RIPEMD160Digest();
                        StackItem r160data = stack.pollLast();
                        digest.update(r160data.bytes(), 0, r160data.length());
                        byte[] ripmemdHash = new byte[20];
                        digest.doFinal(ripmemdHash, 0);
                        stack.add(ripmemdHash, r160data);
                        break;
                    case OP_SHA1:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_SHA1 on an empty stack");
                        try {
                            StackItem sha1Data = stack.pollLast();
                            stack.add(MessageDigest.getInstance("SHA-1").digest(sha1Data.bytes()), sha1Data);
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);  // Cannot happen.
                        }
                        break;
                    case OP_SHA256:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_SHA256 on an empty stack");
                        StackItem sha256Data = stack.pollLast();
                        stack.add(Sha256Hash.hash(sha256Data.bytes()), sha256Data);
                        break;
                    case OP_HASH160:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_HASH160 on an empty stack");
                        StackItem hash160Data = stack.pollLast();
                        stack.add(Utils.sha256hash160(hash160Data.bytes()), hash160Data);
                        break;
                    case OP_HASH256:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_SHA256 on an empty stack");
                        StackItem hash256Data = stack.pollLast();
                        stack.add(Sha256Hash.hashTwice(hash256Data.bytes()), hash256Data);
                        break;
                    case OP_CODESEPARATOR:
                        lastCodeSepLocation = chunk.getStartLocationInProgram() + 1;
                        break;
                    case OP_CHECKSIG:
                    case OP_CHECKSIGVERIFY:
                        if (txContainingThis == null)
                            throw new IllegalStateException("Script attempted signature check but no tx was provided");
                        if (allowFakeChecksig) {

                        } else {
                            executeCheckSig(state, txContainingThis, (int) index, script, stack, lastCodeSepLocation, opcode, value, verifyFlags, allowFakeChecksig);
                        }
                        break;
                    case OP_CHECKMULTISIG:
                    case OP_CHECKMULTISIGVERIFY:
                        if (txContainingThis == null)
                            throw new IllegalStateException("Script attempted signature check but no tx was provided");
                        opCount = executeMultiSig(state, txContainingThis, (int) index, script, stack, opCount, maxOpCount, maxMultisigKeys, lastCodeSepLocation, opcode, value, verifyFlags, allowFakeChecksig);
                        state.opCount = opCount;
                        break;
                    case OP_CHECKLOCKTIMEVERIFY:
                        if (genesisActive || !verifyFlags.contains(ScriptVerifyFlag.CHECKLOCKTIMEVERIFY)) {
                            // not enabled; treat as a NOP2
                            if (verifyFlags.contains(ScriptVerifyFlag.DISCOURAGE_UPGRADABLE_NOPS)) {
                                throw new ScriptExecutionException(state, "Script used a reserved opcode " + opcode);
                            }
                            break;
                        }
                        executeCheckLockTimeVerify(state, txContainingThis, (int) index, stack, lastCodeSepLocation, opcode, verifyFlags);
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
                        if (verifyFlags.contains(ScriptVerifyFlag.DISCOURAGE_UPGRADABLE_NOPS)) {
                            throw new ScriptExecutionException(state, "Script used a reserved opcode " + opcode);
                        }
                        break;

                    default:
                        throw new ScriptExecutionException(state, "Script used a reserved opcode " + opcode);
                }
            }

            if (genesisActive) {
                long stackBytes = stack.getStackMemoryUsage() + altstack.getStackMemoryUsage();
                if (stackBytes > MAX_STACK_MEMORY_USAGE_CONSENSUS)
                    throw new ScriptExecutionException(state, "Stack memory usage consensus exceeded");
            } else {
                if (stack.size() + altstack.size() > 1000 || stack.size() + altstack.size() < 0)
                    throw new ScriptExecutionException(state, "Stack size exceeded range");
            }


            if (scriptStateListener != null) {
                scriptStateListener.onAfterOpCodeExectuted();
            }

            if (opReturnCalled) {
                break;
            }

        }

        if (!ifStack.isEmpty())
            throw new ScriptExecutionException(state, "OP_IF/OP_NOTIF without OP_ENDIF");

        if (scriptStateListener != null) {
            scriptStateListener.onScriptComplete();
        }

    }

    // This is more or less a direct translation of the code in Bitcoin Core
    private static void executeCheckLockTimeVerify(ScriptExecutionState state, Tx txContainingThis, int index, ScriptStack stack,
                                                   int lastCodeSepLocation, int opcode,
                                                   Set<ScriptVerifyFlag> verifyFlags) throws ScriptExecutionException {
        if (stack.size() < 1)
            throw new ScriptExecutionException(state, "Attempted OP_CHECKLOCKTIMEVERIFY on a stack with size < 1");

        // Thus as a special case we tell CScriptNum to accept up
        // to 5-byte bignums to avoid year 2038 issue.
        StackItem nLockTimeItem = stack.getLast();
        //we don't modify the stack so no need to worry about passing on derivation status of stack items.
        final BigInteger nLockTime = castToBigInteger(state, nLockTimeItem, 5, verifyFlags.contains(ScriptVerifyFlag.MINIMALDATA));

        if (nLockTime.compareTo(BigInteger.ZERO) < 0)
            throw new ScriptExecutionException(state, "Negative locktime");

        // There are two kinds of nLockTime, need to ensure we're comparing apples-to-apples
        if (!(
                ((txContainingThis.getLockTime() < BitcoinJ.LOCKTIME_THRESHOLD) && (nLockTime.compareTo(BitcoinJ.LOCKTIME_THRESHOLD_BIG)) < 0) ||
                        ((txContainingThis.getLockTime() >= BitcoinJ.LOCKTIME_THRESHOLD) && (nLockTime.compareTo(BitcoinJ.LOCKTIME_THRESHOLD_BIG)) >= 0))
        )
            throw new ScriptExecutionException(state, "Locktime requirement type mismatch");

        // Now that we know we're comparing apples-to-apples, the
        // comparison is a simple numeric one.
        if (nLockTime.compareTo(BigInteger.valueOf(txContainingThis.getLockTime())) > 0)
            throw new ScriptExecutionException(state, "Locktime requirement not satisfied");

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
        if (!txContainingThis.getInputs().get(index).hasSequence())
            throw new ScriptExecutionException(state, "Transaction contains a final transaction input for a CHECKLOCKTIMEVERIFY script.");
    }

    private static void executeCheckSig(ScriptExecutionState state, Tx txContainingThis, int index, ScriptStream script, ScriptStack stack,
                                        int lastCodeSepLocation, int opcode, Coin value,
                                        Set<ScriptVerifyFlag> verifyFlags, boolean allowFakeChecksig) throws ScriptExecutionException {

        final boolean requireCanonical = !allowFakeChecksig &&
                (verifyFlags.contains(ScriptVerifyFlag.STRICTENC)
                        || verifyFlags.contains(ScriptVerifyFlag.DERSIG)
                        || verifyFlags.contains(ScriptVerifyFlag.LOW_S));

        if (stack.size() < 2)
            throw new ScriptExecutionException(state, "Attempted OP_CHECKSIG(VERIFY) on a stack with size < 2");
        StackItem pubKey = stack.pollLast();
        StackItem sigBytes = stack.pollLast();

        boolean sigValid = false;

        byte[] connectedScript = script.getProgramFrom(script.getLastCodeSepIndex());

        UnsafeByteArrayOutputStream outStream = new UnsafeByteArrayOutputStream(sigBytes.length() + 1);
        try {
            ScriptChunk.writeBytes(outStream, sigBytes.bytes());
        } catch (IOException e) {
            throw new RuntimeException(e); // Cannot happen
        }
        connectedScript = SigHash.removeAllInstancesOf(connectedScript, outStream.toByteArray());

        // TODO: Use int for indexes everywhere, we can't have that many inputs/outputs
        try {
            TransactionSignature sig = TransactionSignature.decodeFromBitcoin(sigBytes.bytes(), requireCanonical,
                    verifyFlags.contains(ScriptVerifyFlag.LOW_S));

            // TODO: Should check hash type is known
            Sha256Hash hash = sig.useForkId() ?
                    SigHash.hashForForkIdSignature(txContainingThis, index, connectedScript, value, sig.sigHashMode(), sig.anyoneCanPay()) :
                    SigHash.hashForLegacySignature(txContainingThis, index, connectedScript, (byte) sig.sighashFlags);
            sigValid = allowFakeChecksig ? true : ECDSA.verify(hash.getBytes(), sig, pubKey.bytes());
        } catch (Exception e1) {
            // There is (at least) one exception that could be hit here (EOFException, if the sig is too short)
            // Because I can't verify there aren't more, we use a very generic Exception catch

            // This RuntimeException occurs when signing as we run partial/invalid scripts to see if they need more
            // signing work to be done inside LocalTransactionSigner.signInputs.
            if (!e1.getMessage().contains("Reached past end of ASN.1 stream"))
                log.warn("Signature checking failed!", e1);
        }


        if (opcode == OP_CHECKSIG)
            stack.add(sigValid ? new byte[]{1} : new byte[]{}, pubKey, sigBytes);
        else if (opcode == OP_CHECKSIGVERIFY)
            if (!sigValid)
                throw new ScriptExecutionException(state, "Script failed OP_CHECKSIGVERIFY");
    }

    private static int executeMultiSig(ScriptExecutionState state, Tx txContainingThis, int index, ScriptStream script, ScriptStack stack,
                                       int opCount, int maxOpCount, int maxKeys, int lastCodeSepLocation, int opcode, Coin value,
                                       Set<ScriptVerifyFlag> verifyFlags, boolean allowFakeChecksig) throws ScriptExecutionException {
        final boolean requireCanonical = !allowFakeChecksig &&
                (verifyFlags.contains(ScriptVerifyFlag.STRICTENC)
                        || verifyFlags.contains(ScriptVerifyFlag.DERSIG)
                        || verifyFlags.contains(ScriptVerifyFlag.LOW_S));

        final boolean enforceMinimal = !allowFakeChecksig && verifyFlags.contains(ScriptVerifyFlag.MINIMALDATA);
        if (stack.size() < 2)
            throw new ScriptExecutionException(state, "Attempted OP_CHECKMULTISIG(VERIFY) on a stack with size < 2");

        List<StackItem> polledStackItems = new LinkedList<>();

        StackItem pubKeyCountItem = stack.pollLast();
        polledStackItems.add(pubKeyCountItem);

        //we'll allow the highest possible pubKeyCount as it's immediately check after and this ensures
        //we get a meaningful error message
        int pubKeyCount = castToBigInteger(state, pubKeyCountItem, MAX_NUM_ELEMENT_SIZE_POST_GENESIS, enforceMinimal).intValue();
        if (pubKeyCount < 0 || pubKeyCount > maxKeys)
            throw new ScriptExecutionException(state, "OP_CHECKMULTISIG(VERIFY) with pubkey count out of range");
        opCount += pubKeyCount;
        if (opCount > maxOpCount)
            throw new ScriptExecutionException(state, "Total op count > " + maxOpCount + " during OP_CHECKMULTISIG(VERIFY)");
        if (stack.size() < pubKeyCount + 1)
            throw new ScriptExecutionException(state, "Attempted OP_CHECKMULTISIG(VERIFY) on a stack with size < num_of_pubkeys + 2");


        LinkedList<StackItem> pubkeys = new LinkedList<>();
        for (int i = 0; i < pubKeyCount; i++) {
            StackItem pubKey = stack.pollLast();
            pubkeys.add(pubKey);
        }
        polledStackItems.addAll(pubkeys);

        StackItem sigCountItem = stack.pollLast();
        polledStackItems.add(sigCountItem);
        int sigCount = castToBigInteger(state, sigCountItem, maxKeys, enforceMinimal).intValue();
        if (sigCount < 0 || sigCount > pubKeyCount)
            throw new ScriptExecutionException(state, "OP_CHECKMULTISIG(VERIFY) with sig count out of range");
        if (stack.size() < sigCount + 1)
            throw new ScriptExecutionException(state, "Attempted OP_CHECKMULTISIG(VERIFY) on a stack with size < num_of_pubkeys + num_of_signatures + 3");

        LinkedList<StackItem> sigs = new LinkedList<>();
        for (int i = 0; i < sigCount; i++) {
            StackItem sig = stack.pollLast();
            sigs.add(sig);
        }
        polledStackItems.addAll(sigs);

        byte[] connectedScript = script.getProgramFrom(script.getLastCodeSepIndex());

        for (StackItem sig : sigs) {
            UnsafeByteArrayOutputStream outStream = new UnsafeByteArrayOutputStream(sig.length() + 1);
            try {
                ScriptChunk.writeBytes(outStream, sig.bytes());
            } catch (IOException e) {
                throw new RuntimeException(e); // Cannot happen
            }
            connectedScript = SigHash.removeAllInstancesOf(connectedScript, outStream.toByteArray());
        }

        boolean valid = true;
        while (sigs.size() > 0) {
            StackItem pubKey = pubkeys.pollFirst();
            // We could reasonably move this out of the loop, but because signature verification is significantly
            // more expensive than hashing, its not a big deal.
            try {
                TransactionSignature sig = TransactionSignature.decodeFromBitcoin(sigs.getFirst().bytes(), requireCanonical,
                        verifyFlags.contains(ScriptVerifyFlag.LOW_S));
                Sha256Hash hash = sig.useForkId() ?
                        SigHash.hashForForkIdSignature(txContainingThis, index, connectedScript, value, sig.sigHashMode(), sig.anyoneCanPay()) :
                        SigHash.hashForLegacySignature(txContainingThis, index, connectedScript, (byte) sig.sighashFlags);
                if (allowFakeChecksig || ECDSA.verify(hash.getBytes(), sig, pubKey.bytes()))
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
        if (verifyFlags.contains(ScriptVerifyFlag.NULLDUMMY) && nullDummy.length() > 0)
            throw new ScriptExecutionException(state, "OP_CHECKMULTISIG(VERIFY) with non-null nulldummy: " + Arrays.toString(nullDummy.bytes()));

        if (opcode == OP_CHECKMULTISIG) {
            StackItem[] polledItems = polledStackItems.toArray(new StackItem[polledStackItems.size()]);
            stack.add(valid ? new byte[]{1} : new byte[]{}, polledItems);
        } else if (opcode == OP_CHECKMULTISIGVERIFY) {
            if (!valid)
                throw new ScriptExecutionException(state, "Script failed OP_CHECKMULTISIGVERIFY");
        }
        return opCount;
    }

    public static class ScriptExecutionState {
        public ScriptStack stack;
        public List<StackItem> stackPopped;
        public ScriptStack altStack;
        public List<StackItem> altStackPopped;
        public LinkedList<Boolean> ifStack;
        public ScriptStream script;
        public int opCount;
        public ScriptChunk lastOpCode;
        public ScriptChunk currentOpCode;
        public int currentOpCodeIndex = 0;
        public Set<ScriptVerifyFlag> verifyFlags;
        public boolean initialStackStateKnown;
    }
}
