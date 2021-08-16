/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.script.interpreter;

import io.bitcoinj.core.Utils;
import io.bitcoinj.script.ScriptData;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

public class StackItem<C> {

    private static final byte[] NULL_BYTES = new byte[]{(byte) 0x80};

    private final ScriptData bytes;
    private final int length;
    private final Type type;
    private final C context;
    /**
     * true if the creation of this item is dependent on a value provided
     * to the script (was on the stack before the script started)
     */
    private final boolean derived;

    static final ScriptData[] SMALL_NUM_STACKBYTES = new ScriptData[17];

    static {
        for (int i = 0; i < SMALL_NUM_STACKBYTES.length; i++) {
            byte[] bytes;
            if (i == 0) {
                bytes = new byte[0];
            } else {
                bytes = Utils.reverseBytes(Utils.encodeMPI(BigInteger.valueOf(i), false));
            }
            SMALL_NUM_STACKBYTES[i] = ScriptData.of(bytes);
        }
    }

    /**
     * Creates a stack item for number <= 16
     * @param num
     * @return
     */
    public static final StackItem forSmallNum(int num) {
        checkArgument(num >= 0 && num <= 16, "smallnum stack bytes outside range 0-16");
        return StackItem.forBytes(SMALL_NUM_STACKBYTES[num].copy(), StackItem.Type.INT, false);
    }

    /**
     * Wraps a byte array assuming it is not derived
     * @param bytes
     * @return
     */
    public static StackItem wrap(byte[] bytes) {
        return new StackItem(ScriptData.of(bytes), Type.BYTES, false);
    }

    public static StackItem wrapDerived(byte[] bytes, Type type, boolean derived) {
        return new StackItem(ScriptData.of(bytes), type, true);
    }

    public static StackItem from(StackItem from, StackItem ... derivedFrom) {
        return new StackItem(from.bytes, from.type, from.derived, derivedFrom);
    }

    public static StackItem forBytes(byte[] bytes, Type type, boolean derived) {
        return new StackItem(ScriptData.of(bytes), type, derived);
    }

    public static StackItem forBytes(byte[] bytes, Type type, StackItem ... derivedFrom) {
        return new StackItem(ScriptData.of(bytes), type, false, derivedFrom);
    }
    public static StackItem forBytes(byte[] bytes, Type type, boolean derived, StackItem ... derivedFrom) {
        return new StackItem(ScriptData.of(bytes), Type.BYTES, derived, derivedFrom);
    }

    public static StackItem forBytes(ScriptData bytes, Type type, boolean derived) {
        return new StackItem(bytes, type, derived);
    }

    public static StackItem forBytes(ScriptData bytes, Type type, StackItem ... derivedFrom) {
        return new StackItem(bytes, type, false, derivedFrom);
    }

    private StackItem(ScriptData bytes, Type type, boolean derived, StackItem ... derivedFrom) {
        this.bytes = bytes;
        this.type = type;
        this.length = bytes.length();
        this.context = null;
        if (!derived) {
            for (StackItem item : derivedFrom) {
                if (item.derived) {
                    derived = true;
                    break;
                }
            }
        }
        this.derived = derived;
    }
    /**
     * @return the value as an integer if possible.
     */
    public BigInteger getInteger() {
        return Utils.decodeMPI(Utils.reverseBytes(bytes.data()), false);
    }

    public Type getType() {
        return type;
    }

    public C getContext() {
        return context;
    }

    public boolean isDerived() {
        return derived;
    }

    /**
     * return backing bytes as a UTF-8 string
     * @return
     */
    public String getAsString() {
        try {
            return "\"" + new String(bytes.data(), "UTF-8") + "\"";
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public int length() {
        return length;
    }

    public byte[] bytes() {
        return bytes.data();
    }

    public ScriptData wrappedBytes() {
        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StackItem)) return false;
        StackItem stackItem = (StackItem) o;
        return Arrays.equals(bytes.data(), stackItem.bytes.data());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes.data());
    }

    public String toString() {
        return toString(false, null);
    }

    public String toString(boolean withType, Type forceType) {
        String rendered = null;
        Type type = forceType == Type.ANY || forceType == null ? this.type : forceType;
        if (type == StackItem.Type.STRING) {
            rendered = getAsString();
        } else if (type == StackItem.Type.INT) {
            rendered = getInteger().toString();
        } else if (type == Type.BOOL) {
            rendered = getInteger().equals(BigInteger.ZERO) ? "false" : "true";
        } else if (Arrays.equals(bytes.data(), NULL_BYTES)) {
            rendered = "null";
        } else {
            rendered = "0x" + Utils.HEX.encode(bytes.data());
        }
        return withType ? type +": " + rendered : rendered;
    }

    public enum Type {
        BYTES, INT, BOOL, STRING, ANY;
    }
}

