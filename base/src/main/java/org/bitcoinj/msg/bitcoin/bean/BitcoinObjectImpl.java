/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.msg.bitcoin.bean;

import org.bitcoinj.core.*;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;
import org.bitcoinj.msg.bitcoin.api.base.Hashable;
import org.bitcoinj.msg.bitcoin.api.base.Header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

public abstract class BitcoinObjectImpl<C extends BitcoinObject> implements BitcoinObject<C> {

    public static final int MAX_SIZE = Integer.MAX_VALUE / 2; // 1GB - we can get problems converting to hex as we double the size of the backing array.

    private final BitcoinObject parent;
    private boolean mutable = false;

    // The offset is how many bytes into the provided byte array this message payload starts at.
    protected int offset;
    // The cursor keeps track of where we are in the byte array as we parse it.
    // Note that it's relative to the start of the array NOT the start of the message payload.
    protected int cursor;

    private int length = BitcoinObject.UNKNOWN_MESSAGE_LENGTH;

    // The raw message payload bytes themselves.
    protected byte[] payload;

    /**
     * Constructor for parsing an object from byte array payload.
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
     * Constructor for parsing an object from InputStream payload.
     * @param parent
     * @param in input stream
     */
    public BitcoinObjectImpl(BitcoinObject parent, InputStream in) {
        this.parent = parent;
        try {
            _parse(in);
        } catch (IOException e) {
            throw new ProtocolException("failed to parse bitocin object: " + getClass().getSimpleName(), e);
        }
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

    private void _parse() {
        cursor = offset;
        parse();
        length = cursor - offset;
        if (length != payload.length) {
            byte[] trimmed = new byte[length];
            System.arraycopy(payload, offset, trimmed, 0, length);
            payload = trimmed;
            offset = 0;
        }
        cursor = 0;
    }

    private void _parse(InputStream in) throws IOException {
        if (isFixedSize()) {
            payload = Utils.readBytesStrict(in, fixedSize());
            length = payload.length;
            offset = 0;
            cursor = 0;
            parse();
            cursor = 0;
        } else {
            length = parse(in);
        }
    }

    /**
     * Requires implementation for variable length message types.
     * @param in
     * @return the length of bytes read assuming most compact form of varints.
     * @throws IOException
     */
    protected int parse(InputStream in) throws IOException {
        throw new UnsupportedOperationException("class is not fixed size and has not implemented parse(InputStream");
    }

    protected final void setLength(int length) {
        this.length = length;
    }

    @Override
    public int getMessageSize() {
        return isFixedSize() ? fixedSize() : length;
    }

    @Override
    public  byte[] serialize() {
        if (!isMutable() && payload != null
                && offset == 0
                && payload.length == length) {
            return payload;
        }
        int len = isFixedSize() ? fixedSize() : length == UNKNOWN_MESSAGE_LENGTH ? estimateMessageLength() : length;
        ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(len);
        try {
            serializeTo(stream);
        } catch (IOException e) {
            // Cannot happen, we are serializing to a memory stream.
        }
        byte[] bytes = stream.toByteArray();
        length = bytes.length;
        return bytes;
    }

    /**
     * estimate of message length for this type of object where length isn't known.  No need to implement for fixed length messages.
     * by the time this is called we have already checked if length is usable.
     * @return
     */
    protected int estimateMessageLength() {
        //good compromise between inputs/outputs and transactions.
        return 512;
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
    public void makeImmutable() {
        mutable = false;
    }

    @Override
    public void makeSelfMutable() {
        payload = null;
        length = fixedSize(); //if fixed size we can still know length.
        mutable = true;
    }

    protected void checkMutable() {
        if (!isMutable())
            throw new IllegalStateException("modifying fields on immutable object, call makeMutable() first");
        //since this called before modifying fields we need to clear the hash
        //to ensure it's recalculated accurately.
        BitcoinObject par = this;
        while (par != null) {
            if (par instanceof Hashable) {
                ((Hashable) par).clearHash();
            }
            par = par.parent();
        }
    }

    public BitcoinObject rootObject() {
        return parent == null ? this : parent.rootObject();
    }

    public BitcoinObject parent() {
        return parent;
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
