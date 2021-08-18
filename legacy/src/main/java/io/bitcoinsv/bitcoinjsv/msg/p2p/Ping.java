/*
 * Copyright 2011 Noa Resare
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
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.msg.Message;
import io.bitcoinsv.bitcoinjsv.params.Net;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class Ping extends Message {
    private long nonce;
    private boolean hasNonce;
    
    public Ping(Net net, byte[] payloadBytes) throws ProtocolException {
        super(net, payloadBytes, 0);
    }
    
    /**
     * Create a Ping with a nonce value.
     * Only use this if the remote node has a protocol version > 60000
     */
    public Ping(long nonce) {
        this.nonce = nonce;
        this.hasNonce = true;
    }
    
    /**
     * Create a Ping without a nonce value.
     * Only use this if the remote node has a protocol version <= 60000
     */
    public Ping() {
        this.hasNonce = false;
    }
    
    @Override
    public void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        if (hasNonce)
            Utils.int64ToByteStreamLE(nonce, stream);
    }

    @Override
    protected void parse() throws ProtocolException {
        try {
            nonce = readInt64();
            hasNonce = true;
        } catch(ProtocolException e) {
            hasNonce = false;
        }
        setLength(hasNonce ? 8 : 0);
    }
    
    @Override
    protected void parseLite() {
    }
    
    public boolean hasNonce() {
        return hasNonce;
    }
    
    public long getNonce() {
        return nonce;
    }
}
