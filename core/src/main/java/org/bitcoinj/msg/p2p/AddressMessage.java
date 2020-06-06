package org.bitcoinj.msg.p2p;

import org.bitcoinj.core.*;
import org.bitcoinj.msg.Message;
import org.bitcoinj.params.SerializeMode;
import org.bitcoinj.params.Net;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Represents an "addr" message on the P2P network, which contains broadcast IP addresses of other peers. This is
 * one of the ways peers can find each other without using the DNS or IRC discovery mechanisms. However storing and
 * using addr messages is not presently implemented.</p>
 * 
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class AddressMessage extends Message {

    private static final long MAX_ADDRESSES = 1024;
    private List<PeerAddress> addresses;

    /**
     * Contruct a new 'addr' message.
     * @param net NetworkParameters object.
     * @param offset The location of the first payload byte within the array.
     * If true and the backing byte array is invalidated due to modification of a field then
     * the cached bytes may be repopulated and retained if the message is serialized again in the future.
     * @param length The length of message if known.  Usually this is provided when deserializing of the wire
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    AddressMessage(Net net, byte[] payload, int offset, SerializeMode serializeMode, int length) throws ProtocolException {
        super(net, payload, offset, serializeMode, length);
    }

    /**
     * Contruct a new 'addr' message.
     * @param net NetworkParameters object.
     * @param serializeMode the serializeMode to use for this block.
     * @param length The length of message if known.  Usually this is provided when deserializing of the wire
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    public AddressMessage(Net net, byte[] payload, SerializeMode serializeMode, int length) throws ProtocolException {
        super(net, payload, 0, serializeMode, length);
    }

    AddressMessage(Net net, byte[] payload, int offset) throws ProtocolException {
        super(net, payload, offset, null, UNKNOWN_LENGTH);
    }

    AddressMessage(Net net, byte[] payload) throws ProtocolException {
        super(net, payload, 0, null, UNKNOWN_LENGTH);
    }

    @Override
    protected void parseLite() throws ProtocolException {
    }

    @Override
    protected void parse() throws ProtocolException {
        long numAddresses = readVarInt();
        // Guard against ultra large messages that will crash us.
        if (numAddresses > MAX_ADDRESSES)
            throw new ProtocolException("Address message too large.");
        addresses = new ArrayList<PeerAddress>((int) numAddresses);
        for (int i = 0; i < numAddresses; i++) {
            PeerAddress addr = new PeerAddress(net, payload, cursor, protocolVersion, this, serializeMode);
            addresses.add(addr);
            cursor += addr.getMessageSize();
        }
        setLength(cursor - offset);
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        if (addresses == null)
            return;
        stream.write(new VarInt(addresses.size()).encode());
        for (PeerAddress addr : addresses) {
            addr.bitcoinSerialize(stream);
        }
    }

    @Override
    public int getMessageSize() {
        if (length() != UNKNOWN_LENGTH)
            return length();
        if (addresses != null) {
            setLength(new VarInt(addresses.size()).getSizeInBytes());
            // The 4 byte difference is the uint32 timestamp that was introduced in version 31402
            setLength(length() + addresses.size() * (protocolVersion > 31402 ? PeerAddress.MESSAGE_SIZE : PeerAddress.MESSAGE_SIZE - 4));
        }
        return length();
    }

    /**
     * @return An unmodifiableList view of the backing List of addresses.  Addresses contained within the list may be safely modified.
     */
    public List<PeerAddress> getAddresses() {
        maybeParse();
        return Collections.unmodifiableList(addresses);
    }

    public void addAddress(PeerAddress address) {
        unCache();
        maybeParse();
        address.setParent(this);
        addresses.add(address);
        if (length() == UNKNOWN_LENGTH)
            getMessageSize();
        else
            setLength(length() + address.getMessageSize());
    }

    public void removeAddress(int index) {
        unCache();
        PeerAddress address = addresses.remove(index);
        address.setParent(null);
        if (length() == UNKNOWN_LENGTH)
            getMessageSize();
        else
            setLength(length() - address.getMessageSize());
    }

    @Override
    public String toString() {
        return "addr: " + Utils.join(addresses);
    }

}
