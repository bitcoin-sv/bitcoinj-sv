package org.bitcoinj.script;

import org.bitcoinj.core.Utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Stack;

public class StackItem<C> {

    public final byte[] bytes;
    public final int length;
    public final Type type;
    public final C context;
    /**
     * true if the creation of this item is dependent on a value provided
     * to the script (was on the stack before the script started)
     */
    private final boolean derived;

    /**
     * Wraps a byte array assuming it is not derived
     * @param bytes
     * @return
     */
    public static StackItem wrap(byte[] bytes) {
        return new StackItem(bytes, Type.BYTES, false);
    }

    public static StackItem wrapDerived(byte[] bytes, boolean derived) {
        return new StackItem(bytes, Type.BYTES, true);
    }

    public static StackItem from(StackItem from, StackItem ... derivedFrom) {
        return new StackItem(from.bytes, from.type, from.derived, derivedFrom);
    }

    public static StackItem from(byte[] bytes, StackItem ... derivedFrom) {
        return new StackItem(bytes, Type.BYTES, false, derivedFrom);
    }

    private StackItem(byte[] bytes, Type type, boolean derived, StackItem ... derivedFrom) {
        this.bytes = bytes;
        this.type = type;
        this.length = bytes.length;
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
        return Utils.decodeMPI(bytes, false);
    }

    /**
     * return backing bytes as a UTF-8 string
     * @return
     */
    public String getAsString() {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public int length() {
        return bytes.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StackItem)) return false;
        StackItem stackItem = (StackItem) o;
        return Arrays.equals(bytes, stackItem.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    public String toString() {
        String rendered = null;
        if (type == StackItem.Type.STRING) {
            rendered = getAsString();
        } else if (type == StackItem.Type.INT) {
            rendered = getInteger().toString();
        } else {
            rendered = "0x" + Utils.HEX.encode(bytes);
        }
        return type +": " + rendered;
    }

    public enum Type {
        BYTES, INT, STRING;
    }
}

