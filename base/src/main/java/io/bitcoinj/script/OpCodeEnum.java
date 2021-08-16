/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.script;

import io.bitcoinj.script.interpreter.StackItem;

import java.util.*;

public enum OpCodeEnum {

    //constants
    OP_0(0x00, "-- b", "_FALSE"),
    OP_1(0x01, "-- b", "_TRUE"),
    OP_PUSHDATA1(0x4c, "-- a", "_PD1"),
    OP_PUSHDATA2(0x4d, "-- a", "_PD2"),
    OP_PUSHDATA4(0x4e, "-- a", "_PD4"),
    OP_1NEGATE(0x4f, "-- n", "-1"),
    OP_RESERVED(0x50, "--"),
    OP_2(0x52, "-- n"),
    OP_3(0x53, "-- n"),
    OP_4(0x54, "-- n"),
    OP_5(0x55, "-- n"),
    OP_6(0x56, "-- n"),
    OP_7(0x57, "-- n"),
    OP_8(0x58, "-- n"),
    OP_9(0x59, "-- n"),
    OP_10(0x5a, "-- n"),
    OP_11(0x5b, "-- n"),
    OP_12(0x5c, "-- n"),
    OP_13(0x5d, "-- n"),
    OP_14(0x5e, "-- n"),
    OP_15(0x5f, "-- n"),
    OP_16(0x60, "-- n"),

    //flow control
    OP_NOP(0x61, "--"),
    OP_VER(0x62, "-- n"),
    OP_IF(0x63, "b --"),
    OP_NOTIF(0x64, "b --"),
    OP_VERIF( 0x65, "n --"),
    OP_VERNOTIF( 0x66, "n --"),
    OP_ELSE(0x67, "--"),
    OP_ENDIF(0x68, "--"),
    OP_VERIFY( 0x69, "b --"),
    OP_RETURN( 0x6a, "--"),

    //stack
    OP_TOALTSTACK(0x6b, "a --", "_TAS", ">R"),
    OP_FROMALTSTACK(0x6c, "-- a", "_FAS", "R>"),
    OP_2DROP(0x6d, "a1 a2 --"),
    OP_2DUP(0x6e, "a1 a2 -- a1 a2 a1 a2"),
    OP_3DUP(0x6f, "a1 a2 a3 -- a1 a2 a3 a1 a2 a3"),
    OP_2OVER(0x70, "a1 a2 a3 a4 -- a1 a2 a3 a4 a1 a2"),
    OP_2ROT(0x71, "a1 a2 a3 a4 a5 a6 -- a3 a4 a5 a6 a1 a2"),
    OP_2SWAP(0x72, "a1 a2 a3 a4 -- a3 a4 a1 a2"),

    OP_IFDUP(0x73, "! a -- a | a a", Integer.MIN_VALUE),
    OP_DEPTH(0x74, "! -- <stack size>"),
    OP_DROP(0x75, "a --"),
    OP_DUP(0x76, "a -- a a"),
    OP_NIP(0x77, "a1 a2 -- a2"),
    OP_OVER(0x78, "a1 a2 -- a1 a2 a1"),
    OP_PICK(0x79, "! an .. a3 a2 a1 n -- an .. a3 a2 a1 an"),
    OP_ROLL(0x7a, "! an .. a3 a2 a1 n -- .. a3 a2 a1 an"),
    OP_ROT(0x7b, "a1 a2 a3 -- a2 a3 a1"),
    OP_SWAP(0x7c, "a1 a2 -- a2 a1"),
    OP_TUCK(0x7d, "a1 a2 -- a2 a1 a2"),

    //splice
    OP_CAT(0x7e, "s1 s2 -- s3"),
    OP_SPLIT(0x7f, "s1 n -- s2 s3"),
    OP_NUM2BIN(0x80, "n1 n2 -- n3"),
    OP_BIN2NUM(0x81, "a -- n"),
    OP_SIZE(0x82, "a -- n"),

    //bitwise logic
    OP_INVERT(0x83, "a1 -- a2", "~"),
    OP_AND(0x84, "a1 a2 -- a3", "&"),
    OP_OR(0x85, "a1 a2 -- a3", "|"),
    OP_XOR(0x86, "a1 a2 -- a3", "^"),
    OP_EQUAL(0x87, "a1 a2 -- b", "===", "EQ"),
    OP_EQUALVERIFY(0x88, "a1 a2 --", "===v, EQV"),
    OP_RESERVED1(0x89, "--"),
    OP_RESERVED2(0x8a, "--"),

    //numeric
    OP_1ADD(0x8b, "n1 -- n2", "++"),
    OP_1SUB(0x8c, "n1 -- n2", "--"),
    OP_2MUL(0x8d, "n1 -- n2", "2*"),
    OP_2DIV(0x8e, "n1 -- n2", "2/"),
    OP_NEGATE(0x8f, "n1 -- n2", "0-"),
    OP_ABS(0x90, "n1 -- n2"),
    OP_NOT(0x91, "n1 -- b", "!"),
    OP_0NOTEQUAL(0x92, "n1 -- b", "0!="),
    OP_ADD(0x93, "n1 n2 == n3", "+"),
    OP_SUB(0x94, "n1 n2 -- n3", "-"),
    OP_MUL(0x95, "n1 n2 -- n3", "*"),
    OP_DIV(0x96, "n1 n2 -- n3", "/"),
    OP_MOD(0x97, "n1 n2 -- n3", "%"),
    OP_LSHIFT(0x98, "a1 n -- a2", "<<"),
    OP_RSHIFT(0x99, "a1 n -- a2", ">>"),
    OP_BOOLAND(0x9a, "b1 b2 -- b3", "&&"),
    OP_BOOLOR(0x9b, "b1 b2 -- b3", "||"),
    OP_NUMEQUAL(0x9c, "n1 n2 -- b", "==", "_NEQ"),
    OP_NUMEQUALVERIFY(0x9d, "n1 n2 --", "==v", "_NEQV"),
    OP_NUMNOTEQUAL(0x9e, "n1 n2 -- b", "!="),
    OP_LESSTHAN(0x9f, "n1 n2 -- b", "<", "_LT"),
    OP_GREATERTHAN(0xa0, "n1 n2 -- b", ">", "_GT"),
    OP_LESSTHANOREQUAL(0xa1, "n1 n2 -- b", "<=", "_LTE"),
    OP_GREATERTHANOREQUAL(0xa2, "n1 n2 -- b", ">=", "_GTE"),
    OP_MIN(0xa3, "n1 n2 -- n"),
    OP_MAX(0xa4, "n1 n2 -- n"),
    OP_WITHIN(0xa5, "n1 n2 n3 -- b"),

    //crypto
    OP_RIPEMD160(0xa6, "a1 -- a2"),
    OP_SHA1(0xa7, "a1 -- a2"),
    OP_SHA256(0xa8, "a1 -- a2"),
    OP_HASH160(0xa9, "a1 -- a2"),
    OP_HASH256(0xaa, "a1 -- a2"),
    OP_CODESEPERATOR(0xab, "aSig aPubKey --"),
    OP_CHECKMULTISIG(0xae, "! aX aSig1 .. aSigN numSigs aPk1 .. aPkN nPk -- b", Integer.MIN_VALUE),
    OP_CHECKMULTISIGVERIFY(0xaf, "! aX aSig1 .. aSigN numSigs aPk1 .. aPkN nPk --", Integer.MIN_VALUE),

    //nops
    OP_NOP1(0xb0, "--"),
    OP_NOP2(0xb1, "--", "_CLTV", "_CHECKLOCKTIMEVERIFY"),
    //they really made a pigs breakfast out of this!
    OP_CHECKLOCKTIMEVERIFY(0xb1, "--", "_NOP2", "_CLTV"),
    OP_NOP3(0xb2, "--"),
    OP_NOP4(0xb3, "--"),
    OP_NOP5(0xb4, "--"),
    OP_NOP6(0xb5, "--"),
    OP_NOP7(0xb6, "--"),
    OP_NOP8(0xb7, "--"),
    OP_NOP9(0xb8, "--"),
    OP_NOP10(0xb9, "--")
    ;

    private static Map<Category, List<OpCodeEnum>> categories = new LinkedHashMap<>();
    private static Map<String, Integer> aliasToOpcode = new LinkedHashMap<>();
    private static OpCodeEnum[] byOpCode = new OpCodeEnum[255];
    private static Map<String, OpCodeEnum> byName = new LinkedHashMap<>();

    static {
        for (OpCodeEnum e: EnumSet.allOf(OpCodeEnum.class)) {
            categorize(e);
            byOpCode[e.op] = e;
            byName.put(e.name(), e);
            byName.put(e.name().toLowerCase(), e);
            byName.put(e.name().substring(3), e);
            byName.put(e.name().substring(3).toLowerCase(), e);

            List<String> allAliases = new ArrayList();
            for (String alias: e.aliases) {
                if (alias.startsWith("_")) {
                    alias = alias.substring(1);
                    allAliases.add(alias.toUpperCase());
                    allAliases.add("OP_" + alias.toUpperCase());
                } else {
                    allAliases.add(alias);
                }
            }

            for (String alias: allAliases) {
                aliasToOpcode.put(alias, e.op);
                aliasToOpcode.put(alias.toLowerCase(), e.op);
                byName.put(alias, e);
                byName.put(alias.toLowerCase(), e);
            }
        }

    }


    public final int op;
    public final Category category;
    public final String signature;
    private final int stackPopped;
    private final int stackPushed;
    private final StackItem.Type[] poppedTypes;
    private final StackItem.Type[] pushedTypes;
    //change in stack size after op code
    private final int stackSizeEffect;
    //depth of stack that has changed after op code
    //in cases like OP_ROLL this is indeterminate since
    //it relies on one of the inputs to determine which item is rolled
    private final int stackDepthAltered;
    private final String[] aliases;


    OpCodeEnum(int op, String signature) {
        this(op, signature, Integer.MIN_VALUE, null);
    }
    OpCodeEnum(int op, String signature, String ... aliases) {
        this(op, signature, Integer.MIN_VALUE, aliases);
    }
    OpCodeEnum(int op, String signature, int stackDepthAltered, String ... aliases) {
        this.op = op;
        this.signature = signature;
        this.aliases = aliases == null ? new String[0] : aliases;
        Signature sig = parseSignature(signature);
        poppedTypes = sig.poppedTypes;
        stackPopped = poppedTypes.length;
        pushedTypes = sig.pushedTypes;
        stackPushed = pushedTypes.length;
        this.stackSizeEffect = stackPushed - stackPopped;
        if (stackDepthAltered == Integer.MIN_VALUE) {
            this.stackDepthAltered = stackPopped;
        } else {
            this.stackDepthAltered = -1;
        }
        category = categorize(this);
    }

    private Signature parseSignature(String signature) {
        Signature sig = new Signature();
        if (signature == null) {
            return sig;
        }

        if (!signature.startsWith("(") || !signature.endsWith(")"))
            throw new RuntimeException("invalid signature format");

        signature = signature.substring(1, signature.length() - 1).trim().toLowerCase();

        String parts[] = signature.split("\\s+");

        List<StackItem.Type> popTypes = new ArrayList<>();
        List<StackItem.Type> pushTypes = new ArrayList<>();
        boolean delimiterHit = false;

        for (String part: parts) {
            if (part.isEmpty())
                continue;

            if ("--".equals(part)) {
                delimiterHit = true;
                continue; //delimiter to skip;
            }
            StackItem.Type type;
            String first = part.substring(0, 1);
            if (first.equals("b"))
                type = StackItem.Type.BOOL;
            else if (first.equals("n"))
                type = StackItem.Type.INT;
            else if (first.equals("s"))
                type = StackItem.Type.STRING;
            else if (first.equals("a") || part.equals(".."))
                type = StackItem.Type.BYTES;
            else
                throw new RuntimeException("stack item type unknown");
            if (delimiterHit) {
                popTypes.add(type);
            } else {
                pushTypes.add(type);
            }
        }
        sig.poppedTypes = popTypes.toArray(new StackItem.Type[popTypes.size()]);
        sig.pushedTypes = pushTypes.toArray(new StackItem.Type[pushTypes.size()]);

        return sig;
    }

    public int getOp() {
        return op;
    }

    public String getSignature() {
        return signature;
    }

    public int getStackPopped() {
        return stackPopped;
    }

    public int getStackPushed() {
        return stackPushed;
    }

    public StackItem.Type[] getPoppedTypes() {
        return poppedTypes;
    }

    public StackItem.Type[] getPushedTypes() {
        return pushedTypes;
    }

    public String[] getAliases() {
        return aliases;
    }

    private static class Signature {
        private StackItem.Type[] poppedTypes;
        private StackItem.Type[] pushedTypes;
    }

    private static Category categorize(OpCodeEnum opcode) {
        Category cat = null;
        int op = opcode.op;
        if (op >= OP_0.op && op <= OP_16.op)
            cat = Category.CONSTANT;
        else if (op >= OP_NOP.op && op <= OP_RETURN.op)
            cat = Category.FLOW;
        else if (op >= OP_TOALTSTACK.op && op <= OP_TUCK.op)
            cat = Category.STACK;
        else if (op >= OP_CAT.op && op <= OP_SIZE.op)
            cat = Category.SPLICE;
        else if (op >= OP_INVERT.op && op <= OP_EQUALVERIFY.op)
            cat = Category.BITWISE;
        else if (op >= OP_1ADD.op && op <= OP_WITHIN.op)
            cat = Category.NUMERIC;
        else if (op >= OP_RIPEMD160.op && op <= OP_CHECKMULTISIGVERIFY.op)
            cat = Category.CRYPTO;
        else if (op >= OP_NOP1.op && op <= OP_NOP10.op)
            cat = Category.NOP;
        else if (op >= OP_RESERVED1.op && op <= OP_RESERVED2.op)
            cat = Category.RESERVED;

        if (cat == null)
            throw new RuntimeException("op code category not found");
        else
            categories.get(cat).add(opcode);

        return cat;
    }

    public enum Category {
        CONSTANT, FLOW, STACK, SPLICE, BITWISE, NUMERIC, CRYPTO, NOP, RESERVED;
    }
}
