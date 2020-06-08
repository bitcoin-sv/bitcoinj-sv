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

package org.bitcoinj.msg.protocol;

import com.google.common.base.Objects;
import org.bitcoinj.core.*;
import org.bitcoinj.msg.ChildMessage;
import org.bitcoinj.msg.Message;
import org.bitcoinj.msg.bitcoin.OutPoint;
import org.bitcoinj.params.SerializeMode;
import org.bitcoinj.params.Net;

import javax.annotation.*;
import java.io.*;

/**
 * <p>This message is a reference or pointer to an output of a different transaction.</p>
 * 
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class TransactionOutPoint extends ChildMessage {

    static final int MESSAGE_LENGTH = 36;

    /** Hash of the transaction to which we refer. */
    private Sha256Hash hash;
    /** Which output of that transaction we are talking about. */
    private long index;

    // This is not part of bitcoin serialization. It points to the connected transaction.
    Transaction fromTx;

    // The connected output.
    private TransactionOutput connectedOutput;

    public TransactionOutPoint(Net net, long index, @Nullable Transaction fromTx) {
        super(net);
        this.index = index;
        if (fromTx != null) {
            this.hash = fromTx.getHash();
            this.fromTx = fromTx;
        } else {
            // This happens when constructing the genesis block.
            hash = Sha256Hash.ZERO_HASH;
        }
        setLength(MESSAGE_LENGTH);
    }

    public TransactionOutPoint(Net net, long index, Sha256Hash hash) {
        super(net);
        this.index = index;
        this.hash = hash;
        setLength(MESSAGE_LENGTH);
    }

    public TransactionOutPoint(Net net, TransactionOutput connectedOutput) {
        this(net, connectedOutput.getIndex(), connectedOutput.getParentTransactionHash());
        this.connectedOutput = connectedOutput;
    }

    /**
    /**
     * Deserializes the message. This is usually part of a transaction message.
     */
    public TransactionOutPoint(Net net, byte[] payload, int offset) throws ProtocolException {
        super(net, payload, offset);
    }

    /**
     * Deserializes the message. This is usually part of a transaction message.
     * @param net NetworkParameters object.
     * @param offset The location of the first payload byte within the array.
     * @param serializeMode the serializeMode to use for this message.
     * @throws ProtocolException
     */
    public TransactionOutPoint(Net net, byte[] payload, int offset, Message parent, SerializeMode serializeMode) throws ProtocolException {
        super(net, payload, offset, parent, serializeMode, MESSAGE_LENGTH);
    }

    @Override
    protected void parseLite() throws ProtocolException {
        setLength(MESSAGE_LENGTH);
    }

    @Override
    protected void parse() throws ProtocolException {
        hash = readHash();
        index = readUint32();
    }

    /* (non-Javadoc)
      * @see Message#getMessageSize()
      */
    @Override
    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }

    @Override
    public void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        stream.write(hash.getReversedBytes());
        Utils.uint32ToByteStreamLE(index, stream);

    }

    /**
     * An outpoint is a part of a transaction input that points to the output of another transaction. If we have both
     * sides in memory, and they have been linked together, this returns a pointer to the connected output, or null
     * if there is no such connection.
     */
    @Nullable
    public TransactionOutput getConnectedOutput() {
        if (fromTx != null) {
            return fromTx.getOutputs().get((int) index);
        } else if (connectedOutput != null) {
            return connectedOutput;
        }
        return null;
    }

    @Override
    public String toString() {
        return hash + ":" + index;
    }

    /**
     * Returns the hash of the transaction this outpoint references/spends/is connected to.
     */
    @Override
    public Sha256Hash getHash() {
        maybeParse();
        return hash;
    }

    public void setHash(Sha256Hash hash) {
        this.hash = hash;
    }

    public long getIndex() {
        maybeParse();
        return index;
    }
    
    public void setIndex(long index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionOutPoint other = (TransactionOutPoint) o;
        return getIndex() == other.getIndex() && getHash().equals(other.getHash());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getIndex(), getHash());
    }
}
