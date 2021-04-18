/*
 * Copyright 2011 Google Inc.
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

package io.bitcoinj.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;


/**
 * A variable-length encoded unsigned integer using Satoshi's encoding (a.k.a. "CompactSize").
 */
public class VarInt implements Serializable {
    public final long value;
    private final int originallyEncodedSize;

    /**
     * Constructs a new VarInt with the given unsigned long value.
     *
     * @param value the unsigned long value (beware widening conversion of negatives!)
     */
    public VarInt(long value) {
        this.value = value;
        originallyEncodedSize = getSizeInBytes();
    }

    /**
     * Constructs a new VarInt with the value parsed from the specified offset of the given buffer.
     *
     * @param buf    the buffer containing the value
     * @param offset the offset of the value
     */
    public VarInt(byte[] buf, int offset) {
        int first = 0xFF & buf[offset];
        if (first < 253) {
            value = first;
            originallyEncodedSize = 1; // 1 data byte (8 bits)
        } else if (first == 253) {
            value = (0xFF & buf[offset + 1]) | ((0xFF & buf[offset + 2]) << 8);
            originallyEncodedSize = 3; // 1 marker + 2 data bytes (16 bits)
        } else if (first == 254) {
            value = Utils.readUint32(buf, offset + 1);
            originallyEncodedSize = 5; // 1 marker + 4 data bytes (32 bits)
        } else {
            value = Utils.readInt64(buf, offset + 1);
            originallyEncodedSize = 9; // 1 marker + 8 data bytes (64 bits)
        }
    }

    public VarInt(InputStream in) throws IOException {
        int first = in.read();
        if (first == -1)
            throw new IOException("reached end of stream");
        first = 0xFF & first;
        if (first < 253) {
            value = first;
            originallyEncodedSize = 1; // 1 data byte (8 bits)
        } else if (first == 253) {
            byte[] buf = Utils.readBytesStrict(in, 2);
            value = (0xFF & buf[0]) | ((0xFF & buf[1]) << 8);
            originallyEncodedSize = 3; // 1 marker + 2 data bytes (16 bits)
        } else if (first == 254) {
            value = Utils.readUint32(in);
            originallyEncodedSize = 5; // 1 marker + 4 data bytes (32 bits)
        } else {
            value = Utils.readInt64(in);
            originallyEncodedSize = 9; // 1 marker + 8 data bytes (64 bits)
        }
    }

    public VarInt(ByteBuffer buf) throws BufferUnderflowException {
        int start = buf.position();
        try {
            int first = Byte.toUnsignedInt(buf.get());

            first = 0xFF & first;
            if (first < 253) {
                value = first;
                originallyEncodedSize = 1; // 1 data byte (8 bits)
            } else if (first == 253) {
                byte[] bytes = new byte[2];
                buf.get(bytes);
                value = (0xFF & bytes[0]) | ((0xFF & bytes[1]) << 8);
                originallyEncodedSize = 3; // 1 marker + 2 data bytes (16 bits)
            } else if (first == 254) {
                byte[] bytes = new byte[4];
                buf.get(bytes);
                value = Utils.readUint32(bytes, 0);
                originallyEncodedSize = 5; // 1 marker + 4 data bytes (32 bits)
            } else {
                byte[] bytes = new byte[8];
                buf.get(bytes);
                value = Utils.readInt64(bytes, 0);
                originallyEncodedSize = 9; // 1 marker + 8 data bytes (64 bits)
            }
        } catch (BufferUnderflowException e) {
            buf.position(start);
            throw e;
        }
    }

    /**
     * Utility method for reading from ByteBuffers, after reading the first byte this method will
     * return the number of additional bytes required to parse a VarInt
     *
     * @param firstByte
     * @return
     */
    public static int bytesRequired(byte firstByte) {
        int unsigned = Byte.toUnsignedInt(firstByte);
        if (unsigned < 253) {
            return 0;
        } else if (unsigned == 253) {
            return 2;
        } else if (unsigned == 254) {
            return 4;
        } else {
            return 8;
        }
    }

    /**
     * Returns the original number of bytes used to encode the value if it was
     * deserialized from a byte array, or the minimum encoded size if it was not.
     */
    public int getOriginalSizeInBytes() {
        return originallyEncodedSize;
    }

    /**
     * Returns the minimum encoded size of the value.
     */
    public final int getSizeInBytes() {
        return sizeOf(value);
    }

    /**
     * Returns the minimum encoded size of the given unsigned long value.
     *
     * @param value the unsigned long value (beware widening conversion of negatives!)
     */
    public static int sizeOf(long value) {
        // if negative, it's actually a very large unsigned long value
        if (value < 0) return 9; // 1 marker + 8 data bytes
        if (value < 253) return 1; // 1 data byte
        if (value <= 0xFFFFL) return 3; // 1 marker + 2 data bytes
        if (value <= 0xFFFFFFFFL) return 5; // 1 marker + 4 data bytes
        return 9; // 1 marker + 8 data bytes
    }

    /**
     * Encodes the value into its minimal representation.
     *
     * @return the minimal encoded bytes of the value
     */
    public byte[] encode() {
        byte[] bytes;
        switch (sizeOf(value)) {
            case 1:
                return new byte[]{(byte) value};
            case 3:
                return new byte[]{(byte) 253, (byte) (value), (byte) (value >> 8)};
            case 5:
                bytes = new byte[5];
                bytes[0] = (byte) 254;
                Utils.uint32ToByteArrayLE(value, bytes, 1);
                return bytes;
            default:
                bytes = new byte[9];
                bytes[0] = (byte) 255;
                Utils.uint64ToByteArrayLE(value, bytes, 1);
                return bytes;
        }
    }

    /**
     * Encode a value direct to a buffer without creating an object
     *
     * @param value the value to be encoded
     * @param buf   a ByteBuffer ready to be written to and must be set to ByteOrder.LITTLE_ENDIAN
     */
    public static void encode(long value, ByteBuffer buf) {
       ByteOrder order = buf.order();
       if (order != ByteOrder.LITTLE_ENDIAN) {
           buf.order(ByteOrder.LITTLE_ENDIAN);
       }
        if (value < 0) {
            //8 bytes
            buf.put((byte) 255);
            buf.putLong(value);

        } else if (value < 253) {
            buf.put((byte) value);
;
        } else if (value <= 0xFFFFL) {
            //2 bytes
            buf.put((byte) 253);
            buf.putShort((short) value);

        } else if (value <= 0xFFFFFFFFL) {
            //4 bytes
            buf.put((byte) 254);
            buf.putInt((int) value);
;
        } else {
            //8 bytes
            buf.put((byte) 255);
            buf.putLong(value);

        }
        //restore buffer order if necessary
        if (order != buf.order()) {
            buf.order(order);
        }

    }
}
