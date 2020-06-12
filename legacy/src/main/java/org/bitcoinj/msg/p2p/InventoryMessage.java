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

package org.bitcoinj.msg.p2p;

import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.params.SerializeMode;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.msg.protocol.Transaction;
import org.bitcoinj.params.Net;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>Represents the "inv" P2P network message. An inv contains a list of hashes of either blocks or transactions. It's
 * a bandwidth optimization - on receiving some data, a (fully validating) peer sends every connected peer an inv
 * containing the hash of what it saw. It'll only transmit the full thing if a peer asks for it with a
 * {@link GetDataMessage}.</p>
 * 
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class InventoryMessage extends ListMessage {

    /** A hard coded constant in the protocol. */
    public static final int MAX_INV_SIZE = 50000;

    public InventoryMessage(Net net, byte[] bytes) throws ProtocolException {
        super(net, bytes);
    }

    /**
     * Deserializes an 'inv' message.
     * @param net NetworkParameters object.
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param serializeMode the serializeMode to use for this message.
     * @param length The length of message if known.  Usually this is provided when deserializing of the wire
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    public InventoryMessage(Net net, byte[] payload, SerializeMode serializeMode, int length)
            throws ProtocolException {
        super(net, payload, serializeMode, length);
    }

    public InventoryMessage(Net net) {
        super(net);
    }

    public void addBlock(Block block) {
        addItem(new InventoryItem(InventoryItem.Type.Block, block.getHash()));
    }

    public void addTransaction(Transaction tx) {
        addItem(new InventoryItem(InventoryItem.Type.Transaction, tx.getHash()));
    }

    /** Creates a new inv message for the given transactions. */
    public static InventoryMessage with(Transaction... txns) {
        checkArgument(txns.length > 0);
        InventoryMessage result = new InventoryMessage(txns[0].getNet());
        for (Transaction tx : txns)
            result.addTransaction(tx);
        return result;
    }
}
