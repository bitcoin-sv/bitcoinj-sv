/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * An unsynchronized implementation of ByteArrayOutputStream that will return the backing byte array if its length == size().
 * This avoids unneeded array copy where the BOS is simply being used to extract a byte array of known length from a
 * 'serialized to stream' method.
 * <p/>
 * Unless the final length can be accurately predicted the only performance this will yield is due to unsynchronized
 * methods.
 *
 * @author Steve Shadders
 *
 */
public class UnsafeByteArrayOutputStream extends ByteArrayOutputStream {

    public UnsafeByteArrayOutputStream() {
        super(32);
    }

    public UnsafeByteArrayOutputStream(int size) {
        super(size);
    }

    /**
     * Writes the specified byte to this byte array output stream.
     *
     * @param b the byte to be written.
     */
    @Override
    public void write(int b) {
        int newcount = count + 1;
        if (newcount > buf.length) {
            buf = Utils.copyOf(buf, Math.max(buf.length << 1, newcount));
        }
        buf[count] = (byte) b;
        count = newcount;
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this byte array output stream.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     */
    @Override
    public void write(byte[] b, int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        int newcount = count + len;
        if (newcount > buf.length) {
            buf = Utils.copyOf(buf, Math.max(buf.length << 1, newcount));
        }
        System.arraycopy(b, off, buf, count, len);
        count = newcount;
    }

    /**
     * Writes the complete contents of this byte array output stream to
     * the specified output stream argument, as if by calling the output
     * stream's write method using <code>out.write(buf, 0, count)</code>.
     *
     * @param out the output stream to which to write the data.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void writeTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    public void writeTo(RandomAccessFile file) throws IOException {
        file.write(buf, 0, count);
    }

    public void writeTo(ByteBuffer buffer) throws IOException {
        buffer.put(buf, 0, count);
    }

    /**
     * Write all available bytes from the inputstream
     * @param in
     * @throws IOException
     */
    public void writeFrom(InputStream in) throws IOException {
        int available;
        while ((available = in.available()) > 0) {
            if (count + available > buf.length) {
                buf = copyOf(buf, Math.max(buf.length << 1, count + available));
            }
            int read = in.read(buf, count, buf.length - count);
            count += read;
        }
    }

    static byte[] copyOf(byte[] in, int length) {
        byte[] out = new byte[length];
        System.arraycopy(in, 0, out, 0, Math.min(length, in.length));
        return out;
    }

    /**
     * Resets the <code>count</code> field of this byte array output
     * stream to zero, so that all currently accumulated output in the
     * output stream is discarded. The output stream can be used again,
     * reusing the already allocated buffer space.
     *
     */
    @Override
    public void reset() {
        count = 0;
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     *
     * @return the current contents of this output stream, as a byte array.
     * @see java.io.ByteArrayOutputStream#size()
     */
    @Override
    public byte[] toByteArray() {
        return count == buf.length ? buf : Utils.copyOf(buf, count);
    }

    /**
     * Returns the backing array of this stream. The length of the array may be greater or equal to size()
     *
     * If the array is modified behaviour is unspecified.
     * @return
     */
    public byte[] getBackingArray() {
        return buf;
    }

    /**
     * Returns the current size of the buffer.
     *
     * @return the value of the <code>count</code> field, which is the number
     *         of valid bytes in this output stream.
     * @see java.io.ByteArrayOutputStream#count
     */
    @Override
    public int size() {
        return count;
    }

    public String toString() {
        return Utils.HEX.encode(Arrays.copyOf(buf, count));
    }

}
