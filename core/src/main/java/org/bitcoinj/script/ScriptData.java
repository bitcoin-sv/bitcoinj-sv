package org.bitcoinj.script;

import java.util.Arrays;

/**
 * Simple interface for script data byte arrays
 */
public interface ScriptData {

    byte[] data();

    default int length() {
        return data().length;
    }

    default ScriptData copy() {
        byte[] copy = Arrays.copyOf(data(), length());
        return new SimpleScriptData(copy);
    }

    static ScriptData of(byte[] data) {
        return new SimpleScriptData(data);
    }

    class SimpleScriptData implements ScriptData {

        private final byte[] data;

        private SimpleScriptData(byte[] data) {
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
