package org.bitcoinj.script;

import org.bitcoinj.core.Utils;

import java.util.Arrays;
import java.util.Stack;

public class StackItem {

    public final byte[] bytes;
    public final int length;
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
        return new StackItem(bytes, false);
    }

    public static StackItem wrapDerived(byte[] bytes, boolean derived) {
        return new StackItem(bytes, true);
    }

    public static StackItem from(StackItem from, StackItem ... derivedFrom) {
        return new StackItem(from.bytes, from.derived, derivedFrom);
    }

    public static StackItem from(byte[] bytes, StackItem ... derivedFrom) {
        return new StackItem(bytes, false, derivedFrom);
    }

    private StackItem(byte[] bytes, boolean derived, StackItem ... derivedFrom) {
        this.bytes = bytes;
        this.length = bytes.length;
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

//    public byte[] bytes() {
//        return bytes;
//    }

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
        return "0x" + Utils.HEX.encode(bytes);
    }
}

