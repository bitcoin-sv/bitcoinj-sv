/*
 * Modifications described in the NOTICE.txt file are licensed under the Open BSV Licence.
 * Modifications Copyright 2020 Bitcoin Association
 *
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
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

package io.bitcoinj.script;

import io.bitcoinj.core.Utils;
import com.google.common.base.Objects;
import io.bitcoinj.script.interpreter.StackItem;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkState;
import static io.bitcoinj.script.ScriptOpCodes.*;

/**
 * A script element that is either a data push (signature, pubkey, etc) or a non-push (logic, numeric, etc) operation.
 */
public class ScriptChunk<C> {
    /** Operation to be executed. Opcodes are defined in {@link ScriptOpCodes}. */
    public final int opcode;
    /**
     * For push operations, this is the vector to be pushed on the stack. For {@link ScriptOpCodes#OP_0}, the vector is
     * empty. Null for non-push operations.
     */
    @Nullable
    public final ScriptData data;
    public final StackItem.Type type;
    private int startLocationInProgram;

    public final boolean isDirective;
    public final String directiveFormat;
    public final boolean isBreakpointSoft;
    public final boolean isBreakpointHard;


    /**
     * user provided context object for attaching meta data to a ScriptChunk
     */
    public final C context;

    public ScriptChunk(int opcode, byte[] data) {
        this(opcode, data, -1);
    }

    public ScriptChunk(int opcode, byte[] data, C context) {
        this(opcode, ScriptData.of(data), -1, context);
    }

    public ScriptChunk(int opcode, ScriptData data, C context) {
        this(opcode, data, -1, context);
    }

    public ScriptChunk(int opcode, byte[] data, int startLocationInProgram) {
        this(opcode, ScriptData.of(data), startLocationInProgram, null);
    }

    public ScriptChunk(int opcode, ScriptData data, int startLocationInProgram, C context) {
        this(opcode, data, StackItem.Type.BYTES, startLocationInProgram, context, false, null, false, false);
    }

    public ScriptChunk(int opcode, ScriptData data, StackItem.Type type, int startLocationInProgram, C context) {
        this(opcode, data, type, startLocationInProgram, context, false, null, false, false);
    }

    public ScriptChunk(int opcode, ScriptData data, StackItem.Type type, int startLocationInProgram, C context, boolean isDirective, String directive, boolean isBreakpointSoft, boolean isBreakpointHard) {
        this.opcode = opcode;
        this.data = data;
        this.type = type;
        this.startLocationInProgram = startLocationInProgram;
        this.context = context;
        this.isDirective = isDirective;
        this.directiveFormat = directive;
        this.isBreakpointSoft = isBreakpointSoft;
        this.isBreakpointHard = isBreakpointHard;
    }

    /**
     * Writes out the given byte buffer to the output stream with the correct opcode prefix
     * To write an integer call writeBytes(out, Utils.reverseBytes(Utils.encodeMPI(val, false)));
     */
    public static void writeBytes(OutputStream os, byte[] buf) throws IOException {
        if (buf.length < OP_PUSHDATA1) {
            os.write(buf.length);
            os.write(buf);
        } else if (buf.length < 256) {
            os.write(OP_PUSHDATA1);
            os.write(buf.length);
            os.write(buf);
        } else if (buf.length < 65536) {
            os.write(OP_PUSHDATA2);
            os.write(0xFF & (buf.length));
            os.write(0xFF & (buf.length >> 8));
            os.write(buf);
        } else {
            throw new RuntimeException("Unimplemented");
        }
    }

    public byte[] data() {
        return data == null ? null : data.data();
    }

    public boolean equalsOpCode(int opcode) {
        return opcode == this.opcode;
    }

    /**
     * If this chunk is a single byte of non-pushdata content (could be OP_RESERVED or some invalid Opcode)
     */
    public boolean isOpCode() {
        return opcode > OP_PUSHDATA4;
    }

    /**
     * Returns true if this chunk is pushdata content, including the single-byte pushdatas.
     */
    public boolean isPushData() {
        return opcode <= OP_16;
    }

    public int getStartLocationInProgram() {
        checkState(startLocationInProgram >= 0);
        return startLocationInProgram;
    }

    /** If this chunk is an OP_N opcode returns the equivalent integer value. */
    public int decodeOpN() {
        checkState(isOpCode());
        return decodeFromOpN(opcode);
    }

    /**
     * Called on a pushdata chunk, returns true if it uses the smallest possible way (according to BIP62) to push the data.
     */
    public boolean isShortestPossiblePushData() {
        checkState(isPushData());
        if (data() == null)
            return true;   // OP_N
        if (data.length() == 0)
            return opcode == OP_0;
        if (data.length() == 1) {
            byte b = data()[0];
            if (b >= 0x01 && b <= 0x10)
                return opcode == OP_1 + b - 1;
            if ((b & 0xFF) == 0x81)
                return opcode == OP_1NEGATE;
        }
        if (data.length() < OP_PUSHDATA1)
            return opcode == data.length();
        if (data.length() < 256)
            return opcode == OP_PUSHDATA1;
        if (data.length() < 65536)
            return opcode == OP_PUSHDATA2;

        // can never be used, but implemented for completeness
        return opcode == OP_PUSHDATA4;
    }

    public void write(OutputStream stream) throws IOException {
        if (isOpCode()) {
            checkState(data() == null);
            stream.write(opcode);
        } else if (data() != null) {
            if (opcode < OP_PUSHDATA1) {
                checkState(data.length() == opcode);
                stream.write(opcode);
            } else if (opcode == OP_PUSHDATA1) {
                checkState(data.length() <= 0xFF);
                stream.write(OP_PUSHDATA1);
                stream.write(data.length());
            } else if (opcode == OP_PUSHDATA2) {
                checkState(data.length() <= 0xFFFF);
                stream.write(OP_PUSHDATA2);
                stream.write(0xFF & data.length());
                stream.write(0xFF & (data.length() >> 8));
            } else if (opcode == OP_PUSHDATA4) {
                //checkState(data.length <= Script.MAX_SCRIPT_ELEMENT_SIZE);
                stream.write(OP_PUSHDATA4);
                Utils.uint32ToByteStreamLE(data.length(), stream);
            } else {
                throw new RuntimeException("Unimplemented");
            }
            stream.write(data());
        } else {
            stream.write(opcode); // smallNum
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (isDirective) {
            buf.append("!!BREAKPOINT");
        } if (isOpCode()) {
            buf.append(getOpCodeName(opcode));
        } else if (data() != null) {
            // Data chunk
            buf.append(getPushDataName(opcode)).append("[").append(Utils.HEX.encode(data())).append("]");
        } else {
            // Small num
            buf.append(decodeFromOpN(opcode));
        }
        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptChunk other = (ScriptChunk) o;
        return opcode == other.opcode && startLocationInProgram == other.startLocationInProgram
            && Arrays.equals(data(), other.data());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(opcode, startLocationInProgram, Arrays.hashCode(data()));
    }
}
