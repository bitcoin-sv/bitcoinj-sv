/*
 * Copyright 2011 Google Inc.
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

package io.bitcoinsv.bitcoinjsv.core;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Resources;
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedLongs;
import io.bitcoinsv.bitcoinjsv.exception.AddressFormatException;
import org.spongycastle.crypto.digests.RIPEMD160Digest;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

/**
 * A collection of various utility methods that are helpful for working with the Bitcoin protocol.
 * To enable debug logging from the library, run with -Dbitcoinj.logging=true on your command line.
 */
public class Utils {

    /** The string that prefixes all text messages signed using Bitcoin keys. */
    public static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";
    public static final byte[] BITCOIN_SIGNED_MESSAGE_HEADER_BYTES = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes(Charsets.UTF_8);

    // zero length arrays are immutable so we can save some object allocation by reusing the same instance.
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private static final Joiner SPACE_JOINER = Joiner.on(" ");

    private static BlockingQueue<Boolean> mockSleepQueue;

    public static InputStream bufferAsInputStream(ByteBuffer buffer) {
        return new BufferBackedInputStream(buffer);
    }

    /**
     * Reads up to the specified number of bytes and then resets the input stream to the starting position.
     * If the requested number of bytes are not available the returned byte array will be truncated to the available length
     * @param in
     * @param requested
     * @return
     * @throws IOException
     */
    public static byte[] readBytesAndReset(InputStream in, int requested) throws IOException {
        in.mark(requested);
        byte[] buf = readBytes(in, requested);
        in.reset();
        return buf;
    }

    /**
     * Reads up to the specified number of bytes. If the requested number of bytes are not available the returned
     * byte array will be truncated to the available length.
     * @param in
     * @param requested
     * @return
     * @throws IOException
     */
    public static byte[] readBytes(InputStream in, int requested) throws IOException {
        //In the vast majority of cases we can do this in one read operation.
        byte[] buf = new byte[requested];
        int read = in.read(buf, 0, requested);
        if (read == requested)
            return buf;

        //for the edge case where this doesn't work we need some additional checks.
        int cursor = read;
        while (read != -1 && cursor < requested) {
            read = in.read(buf, cursor, requested - cursor);
            cursor += read;
        }
        return cursor == requested ? buf : Arrays.copyOf(buf, cursor);
    }

    /**
     * Reads exactly the specified number of bytes. If requested number of bytes are not available throws IOException
     * @param in
     * @param requested requested number of bytes
     * @return
     * @throws IOException if stream ends before requested bytes are read
     */
    public static byte[] readBytesStrict(InputStream in, int requested) throws IOException {
        //In the vast majority of cases we can do this in one read operation.
        byte[] buf = new byte[requested];
        int read = in.read(buf, 0, requested);;
        if (read == requested)
            return buf;

        //for the edge case where this doesn't work we need some additional checks.
        int cursor = read;
        while (read != -1 && cursor < requested) {
            read = in.read(buf, cursor, requested - cursor);
            cursor += read;
        }
        if (read == -1)
            throw new IOException("requested bytes unavailable before stream EOF");
        return buf;
    }

    /**
     * The regular {@link java.math.BigInteger#toByteArray()} method isn't quite what we often need: it appends a
     * leading zero to indicate that the number is positive and may need padding.
     *
     * @param b the integer to format into a byte array
     * @param numBytes the desired size of the resulting byte array
     * @return numBytes byte long array.
     */
    public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
        if (b == null) {
            return null;
        }
        byte[] bytes = new byte[numBytes];
        byte[] biBytes = b.toByteArray();
        int start = (biBytes.length == numBytes + 1) ? 1 : 0;
        int length = Math.min(biBytes.length, numBytes);
        System.arraycopy(biBytes, start, bytes, numBytes - length, length);
        return bytes;        
    }

    public static void uint32ToByteArrayBE(long val, byte[] out, int offset) {
        out[offset] = (byte) (0xFF & (val >> 24));
        out[offset + 1] = (byte) (0xFF & (val >> 16));
        out[offset + 2] = (byte) (0xFF & (val >> 8));
        out[offset + 3] = (byte) (0xFF & val);
    }

    public static void uint32ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
    }

    public static void uint48ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset] = (byte) ((int) (255L & val));
        out[offset + 1] = (byte) ((int) (255L & val >> 8));
        out[offset + 2] = (byte) ((int) (255L & val >> 16));
        out[offset + 3] = (byte) ((int) (255L & val >> 24));
        out[offset + 4] = (byte) ((int) (255L & val >> 32));
        out[offset + 5] = (byte) ((int) (255L & val >> 40));
    }

    public static void uint64ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
        out[offset + 4] = (byte) (0xFF & (val >> 32));
        out[offset + 5] = (byte) (0xFF & (val >> 40));
        out[offset + 6] = (byte) (0xFF & (val >> 48));
        out[offset + 7] = (byte) (0xFF & (val >> 56));
    }

    public static void uint32ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int) (0xFF & val));
        stream.write((int) (0xFF & (val >> 8)));
        stream.write((int) (0xFF & (val >> 16)));
        stream.write((int) (0xFF & (val >> 24)));
    }
    
    public static void int64ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int) (0xFF & val));
        stream.write((int) (0xFF & (val >> 8)));
        stream.write((int) (0xFF & (val >> 16)));
        stream.write((int) (0xFF & (val >> 24)));
        stream.write((int) (0xFF & (val >> 32)));
        stream.write((int) (0xFF & (val >> 40)));
        stream.write((int) (0xFF & (val >> 48)));
        stream.write((int) (0xFF & (val >> 56)));
    }


    public static void uint64ToByteStreamLE(BigInteger val, OutputStream stream) throws IOException {
        byte[] bytes = val.toByteArray();
        if (bytes.length > 8) {
            throw new RuntimeException("Input too large to encode into a uint64");
        }
        bytes = reverseBytes(bytes);
        stream.write(bytes);
        if (bytes.length < 8) {
            for (int i = 0; i < 8 - bytes.length; i++)
                stream.write(0);
        }
    }

    public static void uint16ToByteArrayLE(int val, byte[] out, int offset) {
        out[offset] = (byte) (255L & val);
        out[offset + 1] = (byte) (255L & val >> 8);
    }

    public static void uint16ToByteStreamLE(int val, OutputStream stream) throws IOException {
        stream.write((0xFF & val));
        stream.write((0xFF & (val >> 8)));
    }

    /**
     * Work around lack of unsigned types in Java.
     */
    public static boolean isLessThanUnsigned(long n1, long n2) {
        return UnsignedLongs.compare(n1, n2) < 0;
    }

    /**
     * Work around lack of unsigned types in Java.
     */
    public static boolean isLessThanOrEqualToUnsigned(long n1, long n2) {
        return UnsignedLongs.compare(n1, n2) <= 0;
    }

    /**
     * Hex encoding used throughout the framework. Use with HEX.encode(byte[]) or HEX.decode(CharSequence).
     */
    public static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

    /**
     * Returns a copy of the given byte array in reverse order.
     */
    public static byte[] reverseBytes(byte[] bytes) {
        // We could use the XOR trick here but it's easier to understand if we don't. If we find this is really a
        // performance issue the matter can be revisited.
        byte[] buf = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            buf[i] = bytes[bytes.length - 1 - i];
        return buf;
    }
    
    /**
     * Returns a copy of the given byte array with the bytes of each double-word (4 bytes) reversed.
     * 
     * @param bytes length must be divisible by 4.
     * @param trimLength trim output to this length.  If positive, must be divisible by 4.
     */
    public static byte[] reverseDwordBytes(byte[] bytes, int trimLength) {
        checkArgument(bytes.length % 4 == 0);
        checkArgument(trimLength < 0 || trimLength % 4 == 0);
        
        byte[] rev = new byte[trimLength >= 0 && bytes.length > trimLength ? trimLength : bytes.length];
        
        for (int i = 0; i < rev.length; i += 4) {
            System.arraycopy(bytes, i, rev, i , 4);
            for (int j = 0; j < 4; j++) {
                rev[i + j] = bytes[i + 3 - j];
            }
        }
        return rev;
    }

    /** Parse 2 bytes from the byte array (starting at the offset) as unsigned 16-bit integer in little endian format. */
    public static int readUint16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) |
                ((bytes[offset + 1] & 0xff) << 8);
    }

    /** Parse 2 bytes from the stream as unsigned 16-bit integer in little endian format. */
    public static int readUint16(InputStream in) throws IOException {
        return readUint16(readBytesStrict(in, 2), 0);
    }

    /** Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in little endian format. */
    public static long readUint32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffl) |
                ((bytes[offset + 1] & 0xffl) << 8) |
                ((bytes[offset + 2] & 0xffl) << 16) |
                ((bytes[offset + 3] & 0xffl) << 24);
    }

    /** Parse 4 bytes from the stream as unsigned 32-bit integer in little endian format. */
    public static long readUint32(InputStream in) throws IOException {
        return readUint32(readBytesStrict(in, 4), 0);
    }

    /** Parse 8 bytes from the byte array (starting at the offset) as unsigned 64-bit integer in little endian format. */
    public static long readUint64(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffl) |
                ((bytes[offset + 1] & 0xffl) << 8) |
                ((bytes[offset + 2] & 0xffl) << 16) |
                ((bytes[offset + 3] & 0xffl) << 24) |
                ((bytes[offset + 4] & 0xffl) << 32) |
                ((bytes[offset + 5] & 0xffl) << 40) |
                ((bytes[offset + 6] & 0xffl) << 48) |
                ((bytes[offset + 7] & 0xffl) << 56);
    }

    /** Parse 8 bytes from the stream as unsigned 64-bit integer in little endian format. */
    public static long readUint64(InputStream in) throws IOException {
        return readUint64(readBytesStrict(in, 8), 0);
    }

    /** Parse 6 bytes from the byte array (starting at the offset) as signed 64-bit integer in little endian format. */
    public static long readInt48(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffl) |
            ((bytes[offset + 1] & 0xffl) << 8) |
            ((bytes[offset + 2] & 0xffl) << 16) |
            ((bytes[offset + 3] & 0xffl) << 24) |
            ((bytes[offset + 4] & 0xffl) << 32) |
            ((bytes[offset + 5] & 0xffl) << 40);
    }

    /** Parse 6 bytes from the stream as signed 64-bit integer in little endian format. */
    public static long readInt48(InputStream in) throws IOException {
        return readInt64(readBytesStrict(in, 6), 0);
    }

    /** Parse 8 bytes from the byte array (starting at the offset) as signed 64-bit integer in little endian format. */
    public static long readInt64(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffl) |
                ((bytes[offset + 1] & 0xffl) << 8) |
                ((bytes[offset + 2] & 0xffl) << 16) |
                ((bytes[offset + 3] & 0xffl) << 24) |
                ((bytes[offset + 4] & 0xffl) << 32) |
                ((bytes[offset + 5] & 0xffl) << 40) |
                ((bytes[offset + 6] & 0xffl) << 48) |
                ((bytes[offset + 7] & 0xffl) << 56);
    }

    /** Parse 8 bytes from the stream as signed 64-bit integer in little endian format. */
    public static long readInt64(InputStream in) throws IOException {
        return readInt64(readBytesStrict(in, 8), 0);
    }

    /** Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in big endian format. */
    public static long readUint32BE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xffl) << 24) |
                ((bytes[offset + 1] & 0xffl) << 16) |
                ((bytes[offset + 2] & 0xffl) << 8) |
                (bytes[offset + 3] & 0xffl);
    }

    /** Parse 4 bytes from the stream as unsigned 32-bit integer in big endian format. */
    public static long readUint32BE(InputStream in) throws IOException {
        return readUint32BE(readBytesStrict(in, 4), 0);
    }

    /** Parse 2 bytes from the byte array (starting at the offset) as unsigned 16-bit integer in big endian format. */
    public static int readUint16BE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xff) << 8) |
                (bytes[offset + 1] & 0xff);
    }

    /** Parse 2 bytes from the byte array (starting at the offset) as unsigned 16-bit integer in big endian format. */
    public static int readUint16BE(InputStream in) throws IOException {
        return readUint16BE(readBytesStrict(in, 2), 0);
    }

    /**
     * Calculates RIPEMD160(SHA256(input)). This is used in Address calculations.
     */
    public static byte[] sha256hash160(byte[] input) {
        byte[] sha256 = Sha256Hash.hash(input);
        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(sha256, 0, sha256.length);
        byte[] out = new byte[20];
        digest.doFinal(out, 0);
        return out;
    }

    /**
     * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They consist of
     * a 4 byte big endian length field, followed by the stated number of bytes representing
     * the number in big endian format (with a sign bit).
     * @param hasLength can be set to false if the given array is missing the 4 byte length field
     */
    public static BigInteger decodeMPI(byte[] mpi, boolean hasLength) {
        byte[] buf;
        if (hasLength) {
            int length = (int) readUint32BE(mpi, 0);
            buf = new byte[length];
            System.arraycopy(mpi, 4, buf, 0, length);
        } else
            buf = mpi;
        if (buf.length == 0)
            return BigInteger.ZERO;
        boolean isNegative = (buf[0] & 0x80) == 0x80;
        if (isNegative)
            buf[0] &= 0x7f;
        BigInteger result = new BigInteger(buf);
        return isNegative ? result.negate() : result;
    }

    /**
     * Returns a minimally encoded encoded version of the data. That is, a version will pass the check
     * in checkMinimallyEncodedLE(byte[] bytesLE).
     *
     * If the data is already minimally encoded the original byte array will be returned.
     *
     * inspired by: https://reviews.bitcoinabc.org/D1219
     *
     * @param dataLE
     * @return
     */
    public static byte[] minimallyEncodeLE(byte[] dataLE) {

        if (dataLE.length == 0) {
            return dataLE;
        }

        // If the last byte is not 0x00 or 0x80, we are minimally encoded.
        int last = dataLE[dataLE.length - 1];
        if ((last & 0x7f) != 0) {
            return dataLE;
        }

        // If the script is one byte long, then we have a zero, which encodes as an
        // empty array.
        if (dataLE.length == 1) {
            return EMPTY_BYTE_ARRAY;
        }

        // If the next byte has it sign bit set, then we are minimaly encoded.
        if ((dataLE[dataLE.length - 2] & 0x80) != 0) {
            return dataLE;
        }

        //we might modify the array so clone it
        dataLE = dataLE.clone();

        // We are not minimally encoded, we need to figure out how much to trim.
        // we are using i - 1 indexes here as we want to ignore the last byte (first byte in BE)
        for (int i = dataLE.length - 1; i > 0; i--) {
            // We found a non zero byte, time to encode.
            if (dataLE[i - 1] != 0) {
                if ((dataLE[i - 1] & 0x80) != 0) {
                    // We found a byte with it's sign bit set so we need one more
                    // byte.
                    dataLE[i++] = (byte) last;
                } else {
                    // the sign bit is clear, we can use it.
                    // add the sign bit from the last byte
                    dataLE[i - 1] |= last;
                }

                return Arrays.copyOf(dataLE, i);
            }
        }

        // If we the whole thing is zeros, then we have a zero.
        return EMPTY_BYTE_ARRAY;
    }

    /**
     * checks that LE encoded number is minimally represented.  That is that there are no leading zero bytes except in
     * the case: if there's more than one byte and the most significant bit of the second-most-significant-byte is set it
     * would conflict with the sign bit.
     * @param bytesLE
     * @return
     */
    public static boolean checkMinimallyEncodedLE(byte[] bytesLE, int maxNumSize) {

        if (bytesLE.length > maxNumSize) {
            return false;
        }

        if (bytesLE.length > 0) {
            // Check that the number is encoded with the minimum possible number
            // of bytes.
            //
            // If the most-significant-byte - excluding the sign bit - is zero
            // then we're not minimal. Note how this test also rejects the
            // negative-zero encoding, 0x80.
            if ((bytesLE[bytesLE.length - 1] & 0x7f) == 0) {
                // One exception: if there's more than one byte and the most
                // significant bit of the second-most-significant-byte is set it
                // would conflict with the sign bit. An example of this case is
                // +-255, which encode to 0xff00 and 0xff80 respectively.
                // (big-endian).
                if (bytesLE.length <= 1 || (bytesLE[bytesLE.length - 2] & 0x80) == 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They consist of
     * a 4 byte big endian length field, followed by the stated number of bytes representing
     * the number in big endian format (with a sign bit).
     * @param includeLength indicates whether the 4 byte length field should be included
     */
    public static byte[] encodeMPI(BigInteger value, boolean includeLength) {
        if (value.equals(BigInteger.ZERO)) {
            if (!includeLength)
                return new byte[] {};
            else
                return new byte[] {0x00, 0x00, 0x00, 0x00};
        }
        boolean isNegative = value.signum() < 0;
        if (isNegative)
            value = value.negate();
        byte[] array = value.toByteArray();
        int length = array.length;
        if ((array[0] & 0x80) == 0x80)
            length++;
        if (includeLength) {
            byte[] result = new byte[length + 4];
            System.arraycopy(array, 0, result, length - array.length + 3, array.length);
            uint32ToByteArrayBE(length, result, 0);
            if (isNegative)
                result[4] |= 0x80;
            return result;
        } else {
            byte[] result;
            if (length != array.length) {
                result = new byte[length];
                System.arraycopy(array, 0, result, 1, array.length);
            }else
                result = array;
            if (isNegative)
                result[0] |= 0x80;
            return result;
        }
    }

    /**
     * <p>The "compact" format is a representation of a whole number N using an unsigned 32 bit number similar to a
     * floating point format. The most significant 8 bits are the unsigned exponent of base 256. This exponent can
     * be thought of as "number of bytes of N". The lower 23 bits are the mantissa. Bit number 24 (0x800000) represents
     * the sign of N. Therefore, N = (-1^sign) * mantissa * 256^(exponent-3).</p>
     *
     * <p>Satoshi's original implementation used BN_bn2mpi() and BN_mpi2bn(). MPI uses the most significant bit of the
     * first byte as sign. Thus 0x1234560000 is compact 0x05123456 and 0xc0de000000 is compact 0x0600c0de. Compact
     * 0x05c0de00 would be -0x40de000000.</p>
     *
     * <p>Bitcoin only uses this "compact" format for encoding difficulty targets, which are unsigned 256bit quantities.
     * Thus, all the complexities of the sign bit and using base 256 are probably an implementation accident.</p>
     */
    public static BigInteger decodeCompactBits(long compact) {
        int size = ((int) (compact >> 24)) & 0xFF;
        byte[] bytes = new byte[4 + size];
        bytes[3] = (byte) size;
        if (size >= 1) bytes[4] = (byte) ((compact >> 16) & 0xFF);
        if (size >= 2) bytes[5] = (byte) ((compact >> 8) & 0xFF);
        if (size >= 3) bytes[6] = (byte) (compact & 0xFF);
        return decodeMPI(bytes, true);
    }

    /**
     * @see Utils#decodeCompactBits(long)
     */
    public static long encodeCompactBits(BigInteger value) {
        long result;
        int size = value.toByteArray().length;
        if (size <= 3)
            result = value.longValue() << 8 * (3 - size);
        else
            result = value.shiftRight(8 * (size - 3)).longValue();
        // The 0x00800000 bit denotes the sign.
        // Thus, if it is already set, divide the mantissa by 256 and increase the exponent.
        if ((result & 0x00800000L) != 0) {
            result >>= 8;
            size++;
        }
        result |= size << 24;
        result |= value.signum() == -1 ? 0x00800000 : 0;
        return result;
    }

    /**
     * If non-null, overrides the return value of now().
     */
    public static volatile Date mockTime;

    /**
     * Advances (or rewinds) the mock clock by the given number of seconds.
     */
    public static Date rollMockClock(int seconds) {
        return rollMockClockMillis(seconds * 1000);
    }

    /**
     * Advances (or rewinds) the mock clock by the given number of milliseconds.
     */
    public static Date rollMockClockMillis(long millis) {
        if (mockTime == null)
            throw new IllegalStateException("You need to use setMockClock() first.");
        mockTime = new Date(mockTime.getTime() + millis);
        return mockTime;
    }

    /**
     * Sets the mock clock to the current time.
     */
    public static void setMockClock() {
        mockTime = new Date();
    }

    /**
     * Sets the mock clock to the given time (in seconds).
     */
    public static void setMockClock(long mockClockSeconds) {
        mockTime = new Date(mockClockSeconds * 1000);
    }

    /**
     * Returns the current time, or a mocked out equivalent.
     */
    public static Date now() {
        return mockTime != null ? mockTime : new Date();
    }

    // TODO: Replace usages of this where the result is / 1000 with currentTimeSeconds.
    /** Returns the current time in milliseconds since the epoch, or a mocked out equivalent. */
    public static long currentTimeMillis() {
        return mockTime != null ? mockTime.getTime() : System.currentTimeMillis();
    }

    public static long currentTimeSeconds() {
        return currentTimeMillis() / 1000;
    }

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * Formats a given date+time value to an ISO 8601 string.
     * @param dateTime value to format, as a Date
     */
    public static String dateTimeFormat(Date dateTime) {
        DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601.setTimeZone(UTC);
        return iso8601.format(dateTime);
    }

    /**
     * Formats a given date+time value to an ISO 8601 string.
     * @param dateTime value to format, unix time (ms)
     */
    public static String dateTimeFormat(long dateTime) {
        DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601.setTimeZone(UTC);
        return iso8601.format(dateTime);
    }

    /**
     * Returns a string containing the string representation of the given items,
     * delimited by a single space character.
     *
     * @param items the items to join
     * @param <T> the item type
     * @return the joined space-delimited string
     */
    public static <T> String join(Iterable<T> items) {
        return SPACE_JOINER.join(items);
    }

    public static byte[] copyOf(byte[] in, int length) {
        byte[] out = new byte[length];
        System.arraycopy(in, 0, out, 0, Math.min(length, in.length));
        return out;
    }

    /**
     * Creates a copy of bytes and appends b to the end of it
     */
    public static byte[] appendByte(byte[] bytes, byte b) {
        byte[] result = Arrays.copyOf(bytes, bytes.length + 1);
        result[result.length - 1] = b;
        return result;
    }

    /**
     * Constructs a new String by decoding the given bytes using the specified charset.
     * <p>
     * This is a convenience method which wraps the checked exception with a RuntimeException.
     * The exception can never occur given the charsets
     * US-ASCII, ISO-8859-1, UTF-8, UTF-16, UTF-16LE or UTF-16BE.
     *
     * @param bytes the bytes to be decoded into characters
     * @param charsetName the name of a supported {@linkplain java.nio.charset.Charset charset}
     * @return the decoded String
     */
    public static String toString(byte[] bytes, String charsetName) {
        try {
            return new String(bytes, charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encodes the given string into a sequence of bytes using the named charset.
     * <p>
     * This is a convenience method which wraps the checked exception with a RuntimeException.
     * The exception can never occur given the charsets
     * US-ASCII, ISO-8859-1, UTF-8, UTF-16, UTF-16LE or UTF-16BE.
     *
     * @param str the string to encode into bytes
     * @param charsetName the name of a supported {@linkplain java.nio.charset.Charset charset}
     * @return the encoded bytes
     */
    public static byte[] toBytes(CharSequence str, String charsetName) {
        try {
            return str.toString().getBytes(charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Attempts to parse the given string as arbitrary-length hex or base58 and then return the results, or null if
     * neither parse was successful.
     */
    public static byte[] parseAsHexOrBase58(String data) {
        try {
            return HEX.decode(data);
        } catch (Exception e) {
            // Didn't decode as hex, try base58.
            try {
                return Base58.decodeChecked(data);
            } catch (AddressFormatException e1) {
                return null;
            }
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * <p>Given a textual message, returns a byte buffer formatted as follows:</p>
     *
     * <tt><p>[24] "Bitcoin Signed Message:\n" [message.length as a varint] message</p></tt>
     */
    public static byte[] formatMessageForSigning(String message) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES.length);
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES);
            byte[] messageBytes = message.getBytes(Charsets.UTF_8);
            VarInt size = new VarInt(messageBytes.length);
            bos.write(size.encode());
            bos.write(messageBytes);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }
    
    // 00000001, 00000010, 00000100, 00001000, ...
    private static final int[] bitMask = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80};
    
    /** Checks if the given bit is set in data, using little endian (not the same as Java native big endian) */
    public static boolean checkBitLE(byte[] data, int index) {
        return (data[index >>> 3] & bitMask[7 & index]) != 0;
    }
    
    /** Sets the given bit in data to one, using little endian (not the same as Java native big endian) */
    public static void setBitLE(byte[] data, int index) {
        data[index >>> 3] |= bitMask[7 & index];
    }

    /** Sleep for a span of time, or mock sleep if enabled */
    public static void sleep(long millis) {
        if (mockSleepQueue == null) {
            sleepUninterruptibly(millis, TimeUnit.MILLISECONDS);
        } else {
            try {
                boolean isMultiPass = mockSleepQueue.take();
                rollMockClockMillis(millis);
                if (isMultiPass)
                    mockSleepQueue.offer(true);
            } catch (InterruptedException e) {
                // Ignored.
            }
        }
    }

    /** Enable or disable mock sleep.  If enabled, set mock time to current time. */
    public static void setMockSleep(boolean isEnable) {
        if (isEnable) {
            mockSleepQueue = new ArrayBlockingQueue<Boolean>(1);
            mockTime = new Date(System.currentTimeMillis());
        } else {
            mockSleepQueue = null;
        }
    }

    /** Let sleeping thread pass the synchronization point.  */
    public static void passMockSleep() {
        mockSleepQueue.offer(false);
    }

    /** Let the sleeping thread pass the synchronization point any number of times. */
    public static void finishMockSleep() {
        if (mockSleepQueue != null) {
            mockSleepQueue.offer(true);
        }
    }

    private static int isAndroid = -1;
    public static boolean isAndroidRuntime() {
        if (isAndroid == -1) {
            final String runtime = System.getProperty("java.runtime.name");
            isAndroid = (runtime != null && runtime.equals("Android Runtime")) ? 1 : 0;
        }
        return isAndroid == 1;
    }

    private static class Pair implements Comparable<Pair> {
        int item, count;
        public Pair(int item, int count) { this.count = count; this.item = item; }
        // note that in this implementation compareTo() is not consistent with equals()
        @Override public int compareTo(Pair o) { return -Ints.compare(count, o.count); }
    }

    public static int maxOfMostFreq(int... items) {
        // Java 6 sucks.
        ArrayList<Integer> list = new ArrayList<Integer>(items.length);
        for (int item : items) list.add(item);
        return maxOfMostFreq(list);
    }

    public static int maxOfMostFreq(List<Integer> items) {
        if (items.isEmpty())
            return 0;
        // This would be much easier in a functional language (or in Java 8).
        items = Ordering.natural().reverse().sortedCopy(items);
        LinkedList<Pair> pairs = Lists.newLinkedList();
        pairs.add(new Pair(items.get(0), 0));
        for (int item : items) {
            Pair pair = pairs.getLast();
            if (pair.item != item)
                pairs.add((pair = new Pair(item, 0)));
            pair.count++;
        }
        // pairs now contains a uniqified list of the sorted inputs, with counts for how often that item appeared.
        // Now sort by how frequently they occur, and pick the max of the most frequent.
        Collections.sort(pairs);
        int maxCount = pairs.getFirst().count;
        int maxItem = pairs.getFirst().item;
        for (Pair pair : pairs) {
            if (pair.count != maxCount)
                break;
            maxItem = Math.max(maxItem, pair.item);
        }
        return maxItem;
    }

    /**
     * Reads and joins together with LF char (\n) all the lines from given file. It's assumed that file is in UTF-8.
     */
    public static String getResourceAsString(URL url) throws IOException {
        List<String> lines = Resources.readLines(url, Charsets.UTF_8);
        return Joiner.on('\n').join(lines);
    }

    // Can't use Closeable here because it's Java 7 only and Android devices only got that with KitKat.
    public static InputStream closeUnchecked(InputStream stream) {
        try {
            stream.close();
            return stream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static OutputStream closeUnchecked(OutputStream stream) {
        try {
            stream.close();
            return stream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String humanReadableByteCount(final long bytes) {
        return humanReadableByteCount(bytes, true);
    }


    /**
     * formats the bytes to a human readable format
     *
     * @param si true if each kilo==1000, false if kilo==1024
     */
    public static String humanReadableByteCount(final long bytes, final boolean si) {
        final int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        double result = bytes;
        final String unitsToUse = (si ? "k" : "K") + "MGTPE";
        int i = 0;
        final int unitsCount = unitsToUse.length();
        while (true) {
            result /= unit;
            if (result < unit)
                break;
            // check if we can go further:
            if (i == unitsCount - 1)
                break;
            ++i;
        }
        final StringBuilder sb = new StringBuilder(9);
        sb.append(String.format("%.1f ", result));
        sb.append(unitsToUse.charAt(i));
        if (si)
            sb.append('B');
        else sb.append('i').append('B');
        final String resultStr = sb.toString();
        return resultStr;
    }

    static long ForkBlockTime = 1501593374; // 6 blocks after the fork time
    public static boolean isAfterFork(long time) { return time >= ForkBlockTime; }
}
