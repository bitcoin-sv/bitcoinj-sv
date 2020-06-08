package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public abstract class BitcoinObjectImpl<C extends BitcoinObject> implements BitcoinObject<C> {

    public static final int MAX_SIZE = Integer.MAX_VALUE / 2; // 1GB - we can get problems converting to hex as we double the size of the backing array.

    private BitcoinObject parent;
    private boolean mutable = false;

    public static final int UNKNOWN_LENGTH = Integer.MIN_VALUE;

    // The offset is how many bytes into the provided byte array this message payload starts at.
    protected int offset;
    // The cursor keeps track of where we are in the byte array as we parse it.
    // Note that it's relative to the start of the array NOT the start of the message payload.
    protected int cursor;

    private int length = UNKNOWN_LENGTH;

    // The raw message payload bytes themselves.
    protected byte[] payload;

    /**
     * Constructor for parsing an object from payload.
     * @param parent
     * @param payload
     * @param offset
     */
    public BitcoinObjectImpl(BitcoinObject parent, byte[] payload, int offset) {
        this.parent = parent;
        this.payload = payload;
        this.offset = offset;
        _parse();
    }

    /**
     * Constructor for manually building an object, returned object is mutable
     * @param parent
     */
    public BitcoinObjectImpl(BitcoinObject parent) {
        this.parent = parent;
        makeMutable();
    }

    /**
     * parse the given byte array
     */
    protected abstract void parse();

    @Override
    public int getMessageSize() {
        return length;
    }

    private void _parse() {
        cursor = offset;
        parse();
        length = cursor - offset;
        byte[] trimmed = new byte[length];
        System.arraycopy(payload, offset, trimmed, 0, length);
        payload = trimmed;
        offset = 0;
        cursor = 0;
    }

    protected final void setLength(int length) {
        this.length = length;
    }

    @Override
    public  byte[] serialize() {
        if (!isMutable() && payload != null
                && cursor == 0
                && payload.length == length) {
            return payload;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            serializeTo(stream);
        } catch (IOException e) {
            // Cannot happen, we are serializing to a memory stream.
        }
        return stream.toByteArray();
    }

    @Override
    public boolean isMutable() {
        return mutable;
    }

    public C makeMutable() {
        makeSelfMutable();
        if (parent != null)
            parent.makeMutable();
        return (C) this;
    }

    @Override
    public void makeSelfMutable() {
        payload = null;
        length = UNKNOWN_LENGTH;
        mutable = true;
    }

    protected void checkMutable() {
        if (!isMutable())
            throw new IllegalStateException("modifying fields on immutable object, call makeMutable() first");
    }

    public BitcoinObject rootObject() {
        return parent == null ? this : parent.rootObject();
    }

    protected long readUint32() throws ProtocolException {
        try {
            long u = Utils.readUint32(payload, cursor);
            cursor += 4;
            return u;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }

    protected long readInt64() throws ProtocolException {
        try {
            long u = Utils.readInt64(payload, cursor);
            cursor += 8;
            return u;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }

    protected BigInteger readUint64() throws ProtocolException {
        // Java does not have an unsigned 64 bit type. So scrape it off the wire then flip.
        return new BigInteger(Utils.reverseBytes(readBytes(8)));
    }

    protected long readVarInt() throws ProtocolException {
        return readVarInt(0);
    }

    protected long readVarInt(int offset) throws ProtocolException {
        try {
            VarInt varint = new VarInt(payload, cursor + offset);
            cursor += offset + varint.getOriginalSizeInBytes();
            return varint.value;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }

    protected byte[] readBytes(int length) throws ProtocolException {
        if (length > MAX_SIZE) {
            throw new ProtocolException("Claimed value length too large: " + length);
        }
        try {
            byte[] b = new byte[length];
            System.arraycopy(payload, cursor, b, 0, length);
            cursor += length;
            return b;
        } catch (IndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }

    protected byte[] readByteArray() throws ProtocolException {
        long len = readVarInt();
        return readBytes((int) len);
    }

    protected String readStr() throws ProtocolException {
        long length = readVarInt();
        return length == 0 ? "" : Utils.toString(readBytes((int) length), "UTF-8"); // optimization for empty strings
    }

    protected Sha256Hash readHash() throws ProtocolException {
        // We have to flip it around, as it's been read off the wire in little endian.
        // Not the most efficient way to do this but the clearest.
        return Sha256Hash.wrapReversed(readBytes(32));
    }

    protected boolean hasMoreBytes() {
        return cursor < payload.length;
    }

}
