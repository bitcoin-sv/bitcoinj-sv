/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Simple {@link InputStream} implementation that exposes currently
 * available content of a {@link ByteBuffer}.
 */
public class BufferBackedInputStream extends InputStream {
    protected final ByteBuffer _b;

    public BufferBackedInputStream(ByteBuffer buf) {
        _b = buf;
    }

    @Override
    public int available() {
        return _b.remaining();
    }

    @Override
    public int read() throws IOException {
        return _b.hasRemaining() ? (_b.get() & 0xFF) : -1;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!_b.hasRemaining())
            return -1;
        len = Math.min(len, _b.remaining());
        _b.get(bytes, off, len);
        return len;
    }

    @Override
    public synchronized void mark(int readlimit) {
        _b.mark();
    }

    @Override
    public synchronized void reset() throws IOException {
        _b.reset();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

}