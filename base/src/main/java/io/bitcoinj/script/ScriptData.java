/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.script;

import java.util.Arrays;

/**
 * Simple interface for script data byte arrays
 */
public interface ScriptData {

    byte[] data();

    default int length() {
        return data().length;
    }

    /**
     * Returns a clone. Implementations MUST return a COPY of the backing bytes
     * @return
     */
    default ScriptData copy() {
        byte[] copy = Arrays.copyOf(data(), length());
        return new SimpleScriptBytes(copy);
    }

    static ScriptData of(byte[] data) {
        return new SimpleScriptBytes(data);
    }

    class SimpleScriptBytes implements ScriptData {

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
