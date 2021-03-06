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

package io.bitcoinsv.bitcoinjsv.msg;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.core.ProtocolException;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.core.VarInt;
import io.bitcoinsv.bitcoinjsv.core.UnsafeByteArrayOutputStream;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.params.Net;
import io.bitcoinsv.bitcoinjsv.params.SerializeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkState;

/**
 * <p>A Message is a data structure that can be serialized/deserialized using the Bitcoin serialization format.
 * Specific types of messages that are used both in the block chain, and on the wire, are derived from this
 * class.</p>
 *
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public abstract class Message implements BitcoinObject {
    private static final Logger log = LoggerFactory.getLogger(Message.class);

    // todo: is this right? add support for protoconf message?
    public static final int MAX_SIZE = Integer.MAX_VALUE / 2; // 1GB - we can get problems converting to hex as we double the size of the backing array.

    public static final int UNKNOWN_LENGTH = Integer.MIN_VALUE;

    // Useful to ensure serialize/deserialize are consistent with each other.
    private static final boolean SELF_CHECK = false;

    // The offset is how many bytes into the provided byte array this message payload starts at.
    protected int offset;
    // The cursor keeps track of where we are in the byte array as we parse it.
    // Note that it's relative to the start of the array NOT the start of the message payload.
    protected int cursor;

    private int length = UNKNOWN_LENGTH;

    // The raw message payload bytes themselves.
    protected byte[] payload;

    protected boolean parsed = false;
    protected boolean recached = false;
    protected SerializeMode serializeMode;

    protected int protocolVersion;

    protected Net net;

    protected Message() {
        parsed = true;
        serializeMode = SerializeMode.DEFAULT;
    }

    protected Message(Net net) {
        this.net = net;
        parsed = true;
        serializeMode = SerializeMode.DEFAULT;
    }

    Message(Net net, byte[] payload, int offset, int protocolVersion) throws ProtocolException {
        this(net, payload, offset, protocolVersion, SerializeMode.DEFAULT, UNKNOWN_LENGTH);
    }

    /**
     * @param net          NetworkParameters object.
     * @param payload         Bitcoin protocol formatted byte array containing message content.
     * @param offset          The location of the first payload byte within the array.
     * @param protocolVersion Bitcoin protocol version.
     * @param serializeMode      the serialize to use for this message.
     * @param length          The length of message payload if known.  Usually this is provided when deserializing of the wire
     *                        as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    protected Message(Net net, byte[] payload, int offset, int protocolVersion, SerializeMode serializeMode, int length) throws ProtocolException {
        serializeMode = serializeMode == null ? SerializeMode.DEFAULT : serializeMode;
        this.serializeMode = serializeMode;
        this.protocolVersion = protocolVersion;
        this.net = net;
        this.payload = payload;
        this.cursor = this.offset = offset;
        this.setLength(length);
        if (serializeMode.isParseLazyMode()) {
            parseLite();
        } else {
            parseLite();
            parse();
            parsed = true;
        }

        if (this.length() == UNKNOWN_LENGTH)
            checkState(false, "Length field has not been set in constructor for %s after %s parse. " +
                            "Refer to Message.parseLite() for detail of required Length field contract.",
                    getClass().getSimpleName(), serializeMode.isParseLazyMode() ? "lite" : "full");

        if (SELF_CHECK) {
            selfCheck(payload, offset);
        }

        if (serializeMode.isParseRetainMode() || !parsed) {
            compactPayload();
            return;
        }
        this.payload = null;
    }

    /**
     * Called after parse, allows subclass to optionally modify the retained payload to store a short version
     * the implementing function is expected to set the offset and payload fields
     */
    protected void compactPayload() {
    }

    protected void selfCheck(byte[] payload, int offset) {
        maybeParse();
        byte[] payloadBytes = new byte[cursor - offset];
        System.arraycopy(payload, offset, payloadBytes, 0, cursor - offset);
        byte[] reserialized = bitcoinSerialize();
        if (!Arrays.equals(reserialized, payloadBytes))
            throw new RuntimeException("Serialization is wrong: \n" +
                    Utils.HEX.encode(reserialized) + " vs \n" +
                    Utils.HEX.encode(payloadBytes));
    }

    protected Message(Net net, byte[] payload, int offset) throws ProtocolException {
        this(net, payload, offset, net.params().getProtocolVersionNum(NetworkParameters.ProtocolVersion.CURRENT),
                null, UNKNOWN_LENGTH);
    }

    protected Message(Net net, byte[] payload, int offset, SerializeMode serializeMode, int length) throws ProtocolException {
        this(net, payload, offset, net.params().getProtocolVersionNum(NetworkParameters.ProtocolVersion.CURRENT),
                serializeMode, length);
    }

    // These methods handle the serialization/deserialization using the custom Bitcoin protocol.

    protected abstract void parse() throws ProtocolException;

    /**
     * Perform the most minimal parse possible to calculate the length of the message payload.
     * This is only required for subclasses of ChildMessage as root level messages will have their length passed
     * into the constructor.
     * <p/>
     * Implementations should adhere to the following contract:  If parseLazy = true the 'length'
     * field must be set before returning.  If parseLazy = false the length field must be set either
     * within the parseLite() method OR the parse() method.  The overriding requirement is that length
     * must be set to non UNKNOWN_MESSAGE value by the time the constructor exits.
     *
     * @return
     * @throws ProtocolException
     */
    protected abstract void parseLite() throws ProtocolException;

    /**
     * Ensure the object is parsed if needed.  This should be called in every getter before returning a value.
     * If the lazy parse flag is not set this is a method returns immediately.
     */
    protected synchronized void maybeParse() {
        if (parsed || payload == null)
            return;
        try {
            parse();
            parsed = true;
            if (!serializeMode.isParseRetainMode())
                payload = null;
        } catch (ProtocolException e) {
            throw new LazyParseException("ProtocolException caught during lazy parse.  For safe access to fields call ensureParsed before attempting read or write access", e);
        }
    }

    /**
     * In lazy parsing mode access to getters and setters may throw an unchecked LazyParseException.  If guaranteed safe access is required
     * this method will force parsing to occur immediately thus ensuring LazyParseExeption will never be thrown from this Message.
     * If the Message contains child messages (e.g. a Block containing Transaction messages) this will not force child messages to parse.
     * <p/>
     * This could be overidden for Transaction and it's child classes to ensure the entire tree of Message objects is parsed.
     *
     * @throws ProtocolException
     */
    public void ensureParsed() throws ProtocolException {
        try {
            maybeParse();
        } catch (LazyParseException e) {
            if (e.getCause() instanceof ProtocolException)
                throw (ProtocolException) e.getCause();
            throw new ProtocolException(e);
        }
    }

    /**
     * To be called before any change of internal values including any setters.  This ensures any cached byte array is
     * removed after performing a lazy parse if necessary to ensure the object is fully populated.
     * <p/>
     * Child messages of this object(e.g. Transactions belonging to a Block) will not have their internal byte caches
     * invalidated unless they are also modified internally.
     */
    protected void unCache() {
        maybeParse();
        payload = null;
        recached = false;
    }

    protected void adjustLength(int newArraySize, int adjustment) {
        if (length() == UNKNOWN_LENGTH)
            return;
        // Our own length is now unknown if we have an unknown length adjustment.
        if (adjustment == UNKNOWN_LENGTH) {
            setLength(UNKNOWN_LENGTH);
            return;
        }
        setLength(length() + adjustment);
        // Check if we will need more bytes to encode the length prefix.
        if (newArraySize == 1)
            setLength(length() + 1);  // The assumption here is we never call adjustLength with the same arraySize as before.
        else if (newArraySize != 0)
            setLength(length() + VarInt.sizeOf(newArraySize) - VarInt.sizeOf(newArraySize - 1));
    }

    /**
     * used for unit testing
     */
    public boolean isParsed() {
        return parsed;
    }

    /**
     * used for unit testing
     */
    public boolean isCached() {
        return payload != null;
    }

    public boolean isRecached() {
        return recached;
    }

    /**
     * Returns a copy of the array returned by {@link Message#unsafeBitcoinSerialize()}, which is safe to mutate.
     * If you need extra performance and can guarantee you won't write to the array, you can use the unsafe version.
     *
     * @return a freshly allocated serialized byte array
     */
    public byte[] bitcoinSerialize() {
        byte[] bytes = unsafeBitcoinSerialize();
        byte[] copy = new byte[bytes.length];
        System.arraycopy(bytes, 0, copy, 0, bytes.length);
        return copy;
    }

    /**
     * Serialize this message to a byte array that conforms to the bitcoin wire protocol.
     * <br/>
     * This method may return the original byte array used to construct this message if the
     * following conditions are met:
     * <ol>
     * <li>1) The message was parsed from a byte array with parseRetain = true</li>
     * <li>2) The message has not been modified</li>
     * <li>3) The array had an offset of 0 and no surplus bytes</li>
     * </ol>
     * <p>
     * If condition 3 is not met then an copy of the relevant portion of the array will be returned.
     * Otherwise a full serialize will occur. For this reason you should only use this API if you can guarantee you
     * will treat the resulting array as read only.
     *
     * @return a byte array owned by this object, do NOT mutate it.
     */
    public byte[] unsafeBitcoinSerialize() {
        // 1st attempt to use a cached array.
        if (payload != null) {
            if (offset == 0 && length() == payload.length) {
                // Cached byte array is the entire message with no extras so we can return as is and avoid an array
                // copy.
                return payload;
            }

            byte[] buf = new byte[length()];
            System.arraycopy(payload, offset, buf, 0, length());
            return buf;
        }

        // No cached array available so serialize parts by stream.
        ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(length() < 32 ? 32 : length() + 32);
        try {
            bitcoinSerializeToStream(stream);
        } catch (IOException e) {
            // Cannot happen, we are serializing to a memory stream.
        }

        if (serializeMode.isParseRetainMode()) {
            // A free set of steak knives!
            // If there happens to be a call to this method we gain an opportunity to recache
            // the byte array and in this case it contains no bytes from parent messages.
            // This give a dual benefit.  Releasing references to the larger byte array so that it
            // it is more likely to be GC'd.  And preventing double serializations.  E.g. calculating
            // merkle root calls this method.  It is will frequently happen prior to serializing the block
            // which means another call to bitcoinSerialize is coming.  If we didn't recache then internal
            // serialization would occur a 2nd time and every subsequent time the message is serialized.
            payload = stream.toByteArray();
            cursor = cursor - offset;
            offset = 0;
            recached = true;
            setLength(payload.length);
            return payload;
        }
        // Record length. If this Message wasn't parsed from a byte stream it won't have length field
        // set (except for static length message types).  Setting it makes future streaming more efficient
        // because we can preallocate the ByteArrayOutputStream buffer and avoid resizing.
        byte[] buf = stream.toByteArray();
        setLength(buf.length);
        return buf;
    }

    /**
     * Attempts to get the length of the serialized block without serializing it if possible.
     *
     * @return
     */
    public long getSerializedLength() {
        if (length() != UNKNOWN_LENGTH) {
            return length();
        }
        return unsafeBitcoinSerialize().length;
    }

    public SerializeMode getSerializeMode() {
        return serializeMode;
    }

    /**
     * Serialize this message to the provided OutputStream using the bitcoin wire format.
     *
     * @param stream
     * @throws IOException
     */
    public final void bitcoinSerialize(OutputStream stream) throws IOException {
        // 1st check for cached bytes.
        if (payload != null && length() != UNKNOWN_LENGTH) {
            stream.write(payload, offset, length());
            return;
        }

        bitcoinSerializeToStream(stream);
    }

    /**
     * Serializes this message to the provided stream. If you just want the raw bytes use bitcoinSerialize().
     */
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        log.error("Error: {} class has not implemented bitcoinSerializeToStream method.  Generating message with no payload", getClass());
    }

    /**
     * This method is a NOP for all classes except Block and Transaction.  It is only declared in Message
     * so BitcoinSerializer can avoid 2 instanceof checks + a casting.
     */
    public Sha256Hash getHash() {
        throw new UnsupportedOperationException();
    }

    /**
     * This should be overridden to extract correct message size in the case of lazy parsing.  Until this method is
     * implemented in a subclass of ChildMessage lazy parsing may have no effect.
     * <p>
     * This default implementation is a safe fall back that will ensure it returns a correct value by parsing the message.
     */
    public int getMessageSize() {
        if (length() != UNKNOWN_LENGTH)
            return length();
        maybeParse();
        if (length() == UNKNOWN_LENGTH)
            checkState(false, "Length field has not been set in %s after full parse.", getClass().getSimpleName());
        return length();
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

    /**
     * Network parameters this message was created with.
     */
    public NetworkParameters getParams() {
        return net.params();
    }

    /**
     * The bitcoin network this message was created with.
     * @return
     */
    public Net getNet() {
        return net;
    }

    /**
     * Set the serializeMode for this message when deserialized by Java.
     */
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (null != net.params()) {
            this.serializeMode = SerializeMode.DEFAULT;
        }
    }

    public int length() {
        return length;
    }

    protected void setLength(int length) {
        this.length = length;
    }

    public static class LazyParseException extends RuntimeException {

        public LazyParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public LazyParseException(String message) {
            super(message);
        }

    }

    @Override
    public byte[] serialize() {
        return bitcoinSerialize();
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        bitcoinSerializeToStream(stream);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public void makeSelfMutable() {
    }

    @Override
    public BitcoinObject rootObject() {
        return this;
    }

    @Override
    public BitcoinObject makeMutable() {
        return this;
    }

    @Override
    public BitcoinObject makeNew(byte[] serialized) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeImmutable() {
        throw new UnsupportedOperationException();
    }

    public BitcoinObject parent() {
        return null;
    }
}
