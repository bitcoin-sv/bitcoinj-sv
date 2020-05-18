package org.bitcoinj.script;

import java.util.Arrays;

/**
 * Simple interface for script data byte arrays
 */
public interface ScriptBytes {

    byte[] data();

    default int length() {
        return data().length;
    }

    default ScriptBytes copy() {
        byte[] copy = Arrays.copyOf(data(), length());
        return new SimpleScriptBytes(copy);
    }

    static ScriptBytes of(byte[] data) {
        return new SimpleScriptBytes(data);
    }

    class SimpleScriptBytes implements ScriptBytes {

        private final byte[] data;

        private SimpleScriptBytes(byte[] data) {
            this.data = data;
        }

        @Override
        public byte[] data() {
            return data;
        }

        @Override
        public int length() {
            return data.length;
        }


    }
}
