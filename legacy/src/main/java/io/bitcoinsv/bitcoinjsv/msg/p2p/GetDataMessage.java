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

package io.bitcoinsv.bitcoinjsv.msg.p2p;

import io.bitcoinsv.bitcoinjsv.core.ProtocolException;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.params.SerializeMode;
import io.bitcoinsv.bitcoinjsv.params.Net;

/**
 * <p>Represents the "getdata" P2P network message, which requests the contents of blocks or transactions given their
 * hashes.</p>
 * 
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class GetDataMessage extends ListMessage {

    public GetDataMessage(Net net, byte[] payloadBytes) throws ProtocolException {
        super(net, payloadBytes);
    }

    /**
     * Deserializes a 'getdata' message.
     * @param net NetworkParameters object.
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param serializeMode the serializeMode to use for this message.
     * @param length The length of message if known.  Usually this is provided when deserializing of the wire
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    public GetDataMessage(Net net, byte[] payload, SerializeMode serializeMode, int length)
            throws ProtocolException {
        super(net, payload, serializeMode, length);
    }

    public GetDataMessage(Net net) {
        super(net);
    }

    public void addTransaction(Sha256Hash hash) {
        addItem(new InventoryItem(InventoryItem.Type.Transaction, hash));
    }

    public void addBlock(Sha256Hash hash) {
        addItem(new InventoryItem(InventoryItem.Type.Block, hash));
    }

    public void addFilteredBlock(Sha256Hash hash) {
        addItem(new InventoryItem(InventoryItem.Type.FilteredBlock, hash));
    }

    public Sha256Hash getHashOf(int i) {
        return getItems().get(i).hash;
    }
}
