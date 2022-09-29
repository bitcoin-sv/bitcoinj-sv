/*
 * Modifications described in the NOTICE.txt file are licensed under the Open BSV Licence.
 * Modifications Copyright 2020 Bitcoin Association
 *
 * Copyright 2013 Google Inc.
 * Copyright (c) 2020 Steve Shadders
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

import com.google.common.collect.ImmutableMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Various constants that define the assembly-like scripting language that forms part of the Bitcoin protocol.
 * See {io.bitcoinsv.bitcoinjsv.script.Script} for details. Also provides a method to convert them to a string.
 */
public class ScriptOpCodes {
    // push value
    public static final int OP_0 = 0x00; // push empty vector
    public static final int OP_FALSE = OP_0;
    public static final int OP_PUSHDATA1 = 0x4c;
    public static final int OP_PUSHDATA2 = 0x4d;
    public static final int OP_PUSHDATA4 = 0x4e;
    public static final int OP_1NEGATE = 0x4f;
    public static final int OP_RESERVED = 0x50;
    public static final int OP_1 = 0x51;
    public static final int OP_TRUE = OP_1;
    public static final int OP_2 = 0x52;
    public static final int OP_3 = 0x53;
    public static final int OP_4 = 0x54;
    public static final int OP_5 = 0x55;
    public static final int OP_6 = 0x56;
    public static final int OP_7 = 0x57;
    public static final int OP_8 = 0x58;
    public static final int OP_9 = 0x59;
    public static final int OP_10 = 0x5a;
    public static final int OP_11 = 0x5b;
    public static final int OP_12 = 0x5c;
    public static final int OP_13 = 0x5d;
    public static final int OP_14 = 0x5e;
    public static final int OP_15 = 0x5f;
    public static final int OP_16 = 0x60;

    // control
    public static final int OP_NOP = 0x61;
    public static final int OP_VER = 0x62;
    public static final int OP_IF = 0x63;
    public static final int OP_NOTIF = 0x64;
    public static final int OP_VERIF = 0x65;
    public static final int OP_VERNOTIF = 0x66;
    public static final int OP_ELSE = 0x67;
    public static final int OP_ENDIF = 0x68;
    public static final int OP_VERIFY = 0x69;
    public static final int OP_RETURN = 0x6a;

    // stack ops
    public static final int OP_TOALTSTACK = 0x6b;
    public static final int OP_FROMALTSTACK = 0x6c;
    public static final int OP_2DROP = 0x6d;
    public static final int OP_2DUP = 0x6e;
    public static final int OP_3DUP = 0x6f;
    public static final int OP_2OVER = 0x70;
    public static final int OP_2ROT = 0x71;
    public static final int OP_2SWAP = 0x72;
    public static final int OP_IFDUP = 0x73;
    public static final int OP_DEPTH = 0x74;
    public static final int OP_DROP = 0x75;
    public static final int OP_DUP = 0x76;
    public static final int OP_NIP = 0x77;
    public static final int OP_OVER = 0x78;
    public static final int OP_PICK = 0x79;
    public static final int OP_ROLL = 0x7a;
    public static final int OP_ROT = 0x7b;
    public static final int OP_SWAP = 0x7c;
    public static final int OP_TUCK = 0x7d;

    // splice ops
    public static final int OP_CAT = 0x7e;
    public static final int OP_SPLIT = 0x7f;
    public static final int OP_NUM2BIN = 0x80;
    public static final int OP_BIN2NUM = 0x81;
    public static final int OP_SIZE = 0x82;

    // bit logic
    public static final int OP_INVERT = 0x83;
    public static final int OP_AND = 0x84;
    public static final int OP_OR = 0x85;
    public static final int OP_XOR = 0x86;
    public static final int OP_EQUAL = 0x87;
    public static final int OP_EQUALVERIFY = 0x88;
    public static final int OP_RESERVED1 = 0x89;
    public static final int OP_RESERVED2 = 0x8a;

    // numeric
    public static final int OP_1ADD = 0x8b;
    public static final int OP_1SUB = 0x8c;
    public static final int OP_2MUL = 0x8d;
    public static final int OP_2DIV = 0x8e;
    public static final int OP_NEGATE = 0x8f;
    public static final int OP_ABS = 0x90;
    public static final int OP_NOT = 0x91;
    public static final int OP_0NOTEQUAL = 0x92;
    public static final int OP_ADD = 0x93;
    public static final int OP_SUB = 0x94;
    public static final int OP_MUL = 0x95;
    public static final int OP_DIV = 0x96;
    public static final int OP_MOD = 0x97;
    public static final int OP_LSHIFT = 0x98;
    public static final int OP_RSHIFT = 0x99;
    public static final int OP_BOOLAND = 0x9a;
    public static final int OP_BOOLOR = 0x9b;
    public static final int OP_NUMEQUAL = 0x9c;
    public static final int OP_NUMEQUALVERIFY = 0x9d;
    public static final int OP_NUMNOTEQUAL = 0x9e;
    public static final int OP_LESSTHAN = 0x9f;
    public static final int OP_GREATERTHAN = 0xa0;
    public static final int OP_LESSTHANOREQUAL = 0xa1;
    public static final int OP_GREATERTHANOREQUAL = 0xa2;
    public static final int OP_MIN = 0xa3;
    public static final int OP_MAX = 0xa4;
    public static final int OP_WITHIN = 0xa5;

    // crypto
    public static final int OP_RIPEMD160 = 0xa6;
    public static final int OP_SHA1 = 0xa7;
    public static final int OP_SHA256 = 0xa8;
    public static final int OP_HASH160 = 0xa9;
    public static final int OP_HASH256 = 0xaa;
    public static final int OP_CODESEPARATOR = 0xab;
    public static final int OP_CHECKSIG = 0xac;
    public static final int OP_CHECKSIGVERIFY = 0xad;
    public static final int OP_CHECKMULTISIG = 0xae;
    public static final int OP_CHECKMULTISIGVERIFY = 0xaf;

    // block state
    /** Check lock time of the block. Introduced in BIP 65, replacing OP_NOP2 */
    public static final int OP_CHECKLOCKTIMEVERIFY = 0xb1;

    // expansion
    public static final int OP_NOP1 = 0xb0;
    /** Deprecated by BIP 65 */
    @Deprecated
    public static final int OP_NOP2 = OP_CHECKLOCKTIMEVERIFY;
    public static final int OP_NOP3 = 0xb2;
    public static final int OP_NOP4 = 0xb3;
    public static final int OP_NOP5 = 0xb4;
    public static final int OP_NOP6 = 0xb5;
    public static final int OP_NOP7 = 0xb6;
    public static final int OP_NOP8 = 0xb7;
    public static final int OP_NOP9 = 0xb8;
    public static final int OP_NOP10 = 0xb9;

    /**
     * Meta op codes.  These are treated by the script engine as reserved op codes
     * and will cause a script to fail if encountered in normal script execution.
     * They are used to encode "meta script" which is a template language for scripts
     * that can be unrolled into a canonical script.
     *
     * Meta cop codes are prefixed with "MOP_" by convention
     *
     * MOP_VER can be used at the start of a meta script to indicate that it is
     * meta script.  Reserved op codes will not fail a script if they are in an
     * unexecuted IF branch so simply playing the script does not guarantee failure if
     * it's not been unrolled.
     *
     * MOP_VER expects the next pushdata to be read as meta op code language version.
     * If no MOP_VER is encountered before another MOP_ then version 0 is assumed.
     * In the context of meta script evaluation if the ver is immediately followed
     * by a pushdata then OP_DROP the OP_DROP is not emitted in the unrolled script.
     *
     * If for some reason you wanted to use a script that started
     * with MOP_VER it would behave identically aside from pushing and dropping the
     * version from the stack, as the unroll process will do nothing
     * unless it encounters more meta op codes.
     */

    //next pushdata is the meta op code language version. Default is 0 and is used
    //if no MOP_VER is encountered before another MOP code.
    //The definition of the remainder of MOP codes is specific to version 0
    //and may change in future versions.
    public static final int MOP_VER = 0xc0;

    //next pushdata is a URI to obtain a chunk of script
    public static final int MOP_DATA_REFERENCE = 0xc2;
    public static final int MOP_RESERVED1 = 0xc3;
    public static final int MOP_RESERVED2 = 0xc4;
    public static final int MOP_RESERVED3 = 0xc5;


    // next pushdata is an integer or hash160 reference to a script template
    // library.
    public static final int MOP_LOADLIB = 0xc6;

    //next push data is function call defined by the MOP_LOADLIB
    //the definition of the that function determines whether parameters
    //are expected to follow. If so then a specified number of pushdata
    //ops are expected to follow to specify the params.  In the case where
    //the function takes a variable number of params the list should be preceeded
    //with a push of the count of params.
    public static final int MOP_FN = 0xc7;

    //same as above but the first parameter is expected to be a number
    //of iterrations. The function is called that many times. The loop iteration
    //is available using MOP_LOOPINDEX and the loop count with MOP_LOOPCOUNT;
    public static final int MOP_FN_LOOP = 0xc8;

    //same as above except each iteration will be wrapped in
    //an OP_IF ... OP_ENDIF
    public static final int MOP_FN_WHILE = 0xc9;

    //same as above except the *2nd* and subsequent iterations will be wrapped
    //in a nested OP_IF ... OP_ENDIF
    public static final int MOP_FN_DOWHILE = 0xca;

    //the current loop index is emitted as a pushdata operation.
    public static final int MOP_LOOPINDEX = 0xcb;
    //the total loop count is emitted as a pushdata operation.
    public static final int MOP_LOOPCOUNT = 0xcd;


    public static final int MOP_SETVARLOCAL = 0xd0;
    public static final int MOP_SETARRAYLOCAL = 0xd1;

    public static final int MOP_PUSHVARLOCAL = 0xd2;
    public static final int MOP_PUSHARRAYLOCAL = 0xd3;

    public static final int MOP_PUSHVARGLOBAL = 0xd4;
    public static final int MOP_PUSHARRAYGLOBAL = 0xd5;

    public static final int MOP_PUSH_FNCALLRESULT = 0xd4;
    public static final int MOP_PUSHARRAY_FN_CALL_RESULT= 0xd5;

    //The next pushdata is the index of the function param
    //this param should be emitted as a pushdata operation.
    //If called outside the context of a function call this is invalid.
    public static final int MOP_PUSHPARAM = 0xde;


    //direct pushparam mops
    public static final int MOP_PUSHPARAM0 = 0xe0;
    public static final int MOP_PUSHPARAM1 = 0xe1;
    public static final int MOP_PUSHPARAM2 = 0xe2;
    public static final int MOP_PUSHPARAM3 = 0xe3;
    public static final int MOP_PUSHPARAM4 = 0xe4;
    public static final int MOP_PUSHPARAM5 = 0xe5;
    public static final int MOP_PUSHPARAM6 = 0xe6;
    public static final int MOP_PUSHPARAM7 = 0xe7;
    public static final int MOP_PUSHPARAM8 = 0xe8;
    public static final int MOP_PUSHPARAM9 = 0xe9;


    public static final int OP_INVALIDOPCODE = 0xff;

    private static final Map<Integer, String> opCodeMap = ImmutableMap.<Integer, String>builder()
        .put(OP_0, "0")
        .put(OP_PUSHDATA1, "PUSHDATA1")
        .put(OP_PUSHDATA2, "PUSHDATA2")
        .put(OP_PUSHDATA4, "PUSHDATA4")
        .put(OP_1NEGATE, "1NEGATE")
        .put(OP_RESERVED, "RESERVED")
        .put(OP_1, "1")
        .put(OP_2, "2")
        .put(OP_3, "3")
        .put(OP_4, "4")
        .put(OP_5, "5")
        .put(OP_6, "6")
        .put(OP_7, "7")
        .put(OP_8, "8")
        .put(OP_9, "9")
        .put(OP_10, "10")
        .put(OP_11, "11")
        .put(OP_12, "12")
        .put(OP_13, "13")
        .put(OP_14, "14")
        .put(OP_15, "15")
        .put(OP_16, "16")
        .put(OP_NOP, "NOP")
        .put(OP_VER, "VER")
        .put(OP_IF, "IF")
        .put(OP_NOTIF, "NOTIF")
        .put(OP_VERIF, "VERIF")
        .put(OP_VERNOTIF, "VERNOTIF")
        .put(OP_ELSE, "ELSE")
        .put(OP_ENDIF, "ENDIF")
        .put(OP_VERIFY, "VERIFY")
        .put(OP_RETURN, "RETURN")
        .put(OP_TOALTSTACK, "TOALTSTACK")
        .put(OP_FROMALTSTACK, "FROMALTSTACK")
        .put(OP_2DROP, "2DROP")
        .put(OP_2DUP, "2DUP")
        .put(OP_3DUP, "3DUP")
        .put(OP_2OVER, "2OVER")
        .put(OP_2ROT, "2ROT")
        .put(OP_2SWAP, "2SWAP")
        .put(OP_IFDUP, "IFDUP")
        .put(OP_DEPTH, "DEPTH")
        .put(OP_DROP, "DROP")
        .put(OP_DUP, "DUP")
        .put(OP_NIP, "NIP")
        .put(OP_OVER, "OVER")
        .put(OP_PICK, "PICK")
        .put(OP_ROLL, "ROLL")
        .put(OP_ROT, "ROT")
        .put(OP_SWAP, "SWAP")
        .put(OP_TUCK, "TUCK")
        .put(OP_CAT, "CAT")
        .put(OP_SPLIT, "SPLIT")
        .put(OP_NUM2BIN, "NUM2BIN")
        .put(OP_BIN2NUM, "BIN2NUM")
        .put(OP_SIZE, "SIZE")
        .put(OP_INVERT, "INVERT")
        .put(OP_AND, "AND")
        .put(OP_OR, "OR")
        .put(OP_XOR, "XOR")
        .put(OP_EQUAL, "EQUAL")
        .put(OP_EQUALVERIFY, "EQUALVERIFY")
        .put(OP_RESERVED1, "RESERVED1")
        .put(OP_RESERVED2, "RESERVED2")
        .put(OP_1ADD, "1ADD")
        .put(OP_1SUB, "1SUB")
        .put(OP_2MUL, "2MUL")
        .put(OP_2DIV, "2DIV")
        .put(OP_NEGATE, "NEGATE")
        .put(OP_ABS, "ABS")
        .put(OP_NOT, "NOT")
        .put(OP_0NOTEQUAL, "0NOTEQUAL")
        .put(OP_ADD, "ADD")
        .put(OP_SUB, "SUB")
        .put(OP_MUL, "MUL")
        .put(OP_DIV, "DIV")
        .put(OP_MOD, "MOD")
        .put(OP_LSHIFT, "LSHIFT")
        .put(OP_RSHIFT, "RSHIFT")
        .put(OP_BOOLAND, "BOOLAND")
        .put(OP_BOOLOR, "BOOLOR")
        .put(OP_NUMEQUAL, "NUMEQUAL")
        .put(OP_NUMEQUALVERIFY, "NUMEQUALVERIFY")
        .put(OP_NUMNOTEQUAL, "NUMNOTEQUAL")
        .put(OP_LESSTHAN, "LESSTHAN")
        .put(OP_GREATERTHAN, "GREATERTHAN")
        .put(OP_LESSTHANOREQUAL, "LESSTHANOREQUAL")
        .put(OP_GREATERTHANOREQUAL, "GREATERTHANOREQUAL")
        .put(OP_MIN, "MIN")
        .put(OP_MAX, "MAX")
        .put(OP_WITHIN, "WITHIN")
        .put(OP_RIPEMD160, "RIPEMD160")
        .put(OP_SHA1, "SHA1")
        .put(OP_SHA256, "SHA256")
        .put(OP_HASH160, "HASH160")
        .put(OP_HASH256, "HASH256")
        .put(OP_CODESEPARATOR, "CODESEPARATOR")
        .put(OP_CHECKSIG, "CHECKSIG")
        .put(OP_CHECKSIGVERIFY, "CHECKSIGVERIFY")
        .put(OP_CHECKMULTISIG, "CHECKMULTISIG")
        .put(OP_CHECKMULTISIGVERIFY, "CHECKMULTISIGVERIFY")
        .put(OP_NOP1, "NOP1")
        .put(OP_CHECKLOCKTIMEVERIFY, "CHECKLOCKTIMEVERIFY")
        .put(OP_NOP3, "NOP3")
        .put(OP_NOP4, "NOP4")
        .put(OP_NOP5, "NOP5")
        .put(OP_NOP6, "NOP6")
        .put(OP_NOP7, "NOP7")
        .put(OP_NOP8, "NOP8")
        .put(OP_NOP9, "NOP9")
        .put(OP_NOP10, "NOP10").build();

    private static final Map<String, Integer> opCodeNameMap = ImmutableMap.<String, Integer>builder()
        .put("0", OP_0)
        .put("PUSHDATA1", OP_PUSHDATA1)
        .put("PUSHDATA2", OP_PUSHDATA2)
        .put("PUSHDATA4", OP_PUSHDATA4)
        .put("1NEGATE", OP_1NEGATE)
        .put("RESERVED", OP_RESERVED)
        .put("1", OP_1)
        .put("2", OP_2)
        .put("3", OP_3)
        .put("4", OP_4)
        .put("5", OP_5)
        .put("6", OP_6)
        .put("7", OP_7)
        .put("8", OP_8)
        .put("9", OP_9)
        .put("10", OP_10)
        .put("11", OP_11)
        .put("12", OP_12)
        .put("13", OP_13)
        .put("14", OP_14)
        .put("15", OP_15)
        .put("16", OP_16)
        .put("NOP", OP_NOP)
        .put("VER", OP_VER)
        .put("IF", OP_IF)
        .put("NOTIF", OP_NOTIF)
        .put("VERIF", OP_VERIF)
        .put("VERNOTIF", OP_VERNOTIF)
        .put("ELSE", OP_ELSE)
        .put("ENDIF", OP_ENDIF)
        .put("VERIFY", OP_VERIFY)
        .put("RETURN", OP_RETURN)
        .put("TOALTSTACK", OP_TOALTSTACK)
        .put("FROMALTSTACK", OP_FROMALTSTACK)
        .put("2DROP", OP_2DROP)
        .put("2DUP", OP_2DUP)
        .put("3DUP", OP_3DUP)
        .put("2OVER", OP_2OVER)
        .put("2ROT", OP_2ROT)
        .put("2SWAP", OP_2SWAP)
        .put("IFDUP", OP_IFDUP)
        .put("DEPTH", OP_DEPTH)
        .put("DROP", OP_DROP)
        .put("DUP", OP_DUP)
        .put("NIP", OP_NIP)
        .put("OVER", OP_OVER)
        .put("PICK", OP_PICK)
        .put("ROLL", OP_ROLL)
        .put("ROT", OP_ROT)
        .put("SWAP", OP_SWAP)
        .put("TUCK", OP_TUCK)
        .put("CAT", OP_CAT)
        .put("SPLIT", OP_SPLIT)
        .put("NUM2BIN", OP_NUM2BIN)
        .put("BIN2NUM", OP_BIN2NUM)
        .put("SIZE", OP_SIZE)
        .put("INVERT", OP_INVERT)
        .put("AND", OP_AND)
        .put("OR", OP_OR)
        .put("XOR", OP_XOR)
        .put("EQUAL", OP_EQUAL)
        .put("EQUALVERIFY", OP_EQUALVERIFY)
        .put("RESERVED1", OP_RESERVED1)
        .put("RESERVED2", OP_RESERVED2)
        .put("1ADD", OP_1ADD)
        .put("1SUB", OP_1SUB)
        .put("2MUL", OP_2MUL)
        .put("2DIV", OP_2DIV)
        .put("NEGATE", OP_NEGATE)
        .put("ABS", OP_ABS)
        .put("NOT", OP_NOT)
        .put("0NOTEQUAL", OP_0NOTEQUAL)
        .put("ADD", OP_ADD)
        .put("SUB", OP_SUB)
        .put("MUL", OP_MUL)
        .put("DIV", OP_DIV)
        .put("MOD", OP_MOD)
        .put("LSHIFT", OP_LSHIFT)
        .put("RSHIFT", OP_RSHIFT)
        .put("BOOLAND", OP_BOOLAND)
        .put("BOOLOR", OP_BOOLOR)
        .put("NUMEQUAL", OP_NUMEQUAL)
        .put("NUMEQUALVERIFY", OP_NUMEQUALVERIFY)
        .put("NUMNOTEQUAL", OP_NUMNOTEQUAL)
        .put("LESSTHAN", OP_LESSTHAN)
        .put("GREATERTHAN", OP_GREATERTHAN)
        .put("LESSTHANOREQUAL", OP_LESSTHANOREQUAL)
        .put("GREATERTHANOREQUAL", OP_GREATERTHANOREQUAL)
        .put("MIN", OP_MIN)
        .put("MAX", OP_MAX)
        .put("WITHIN", OP_WITHIN)
        .put("RIPEMD160", OP_RIPEMD160)
        .put("SHA1", OP_SHA1)
        .put("SHA256", OP_SHA256)
        .put("HASH160", OP_HASH160)
        .put("HASH256", OP_HASH256)
        .put("CODESEPARATOR", OP_CODESEPARATOR)
        .put("CHECKSIG", OP_CHECKSIG)
        .put("CHECKSIGVERIFY", OP_CHECKSIGVERIFY)
        .put("CHECKMULTISIG", OP_CHECKMULTISIG)
        .put("CHECKMULTISIGVERIFY", OP_CHECKMULTISIGVERIFY)
        .put("NOP1", OP_NOP1)
        .put("CHECKLOCKTIMEVERIFY", OP_CHECKLOCKTIMEVERIFY)
        .put("NOP2", OP_NOP2)
        .put("NOP3", OP_NOP3)
        .put("NOP4", OP_NOP4)
        .put("NOP5", OP_NOP5)
        .put("NOP6", OP_NOP6)
        .put("NOP7", OP_NOP7)
        .put("NOP8", OP_NOP8)
        .put("NOP9", OP_NOP9)
        .put("NOP10", OP_NOP10).build();

    /**
     * Converts the given OpCode into a string (eg "0", "PUSHDATA", or "NON_OP(10)")
     */
    public static String getOpCodeName(int opcode) {
        if (opCodeMap.containsKey(opcode))
            return opCodeMap.get(opcode);

        return "NON_OP(" + opcode + ")";
    }

    /**
     * Converts the given pushdata OpCode into a string (eg "PUSHDATA2", or "PUSHDATA(23)")
     */
    public static String getPushDataName(int opcode) {
        if (opCodeMap.containsKey(opcode))
            return opCodeMap.get(opcode);

        return "PUSHDATA(" + opcode + ")";
    }

    /**
     * Converts the given OpCodeName into an int
     */
    public static int getOpCode(String opCodeName) {
        if (opCodeNameMap.containsKey(opCodeName))
            return opCodeNameMap.get(opCodeName);

        return OP_INVALIDOPCODE;
    }

    /**
     * Return the numeric value associated with a push op from OP_1NEGATE to OP_16
     * @param opcode
     * @return
     */
    public static int decodeFromOpN(int opcode) {
        checkArgument((opcode == OP_0 || opcode == OP_1NEGATE) || (opcode >= OP_1 && opcode <= OP_16), "decodeFromOpN called on non OP_N opcode");
        if (opcode == OP_0)
            return 0;
        else if (opcode == OP_1NEGATE)
            return -1;
        else
            return opcode + 1 - OP_1;
    }

    /**
     * Return the opcode required to push a numeric between -1 and 16
     * @param value
     * @return
     */
    public static int encodeToOpN(int value) {
        checkArgument(value >= -1 && value <= 16, "encodeToOpN called for " + value + " which we cannot encode in an opcode.");
        if (value == 0)
            return OP_0;
        else if (value == -1)
            return OP_1NEGATE;
        else
            return value - 1 + OP_1;
    }
}
