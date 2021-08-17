/*
 * Copyright 2011 Steve Shadders.
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

import io.bitcoinsv.bitcoinjsv.core.ProtocolException;
import io.bitcoinsv.bitcoinjsv.params.Net;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Parent class for header only messages that don't have a payload.
 * Currently this includes getaddr, verack and special bitcoinj class UnknownMessage.</p>
 * 
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public abstract class EmptyMessage extends Message {

    public EmptyMessage() {
        setLength(0);
    }

    public EmptyMessage(Net net) {
        super(net);
        setLength(0);
    }

    public EmptyMessage(Net net, byte[] payload, int offset) throws ProtocolException {
        super(net, payload, offset);
        setLength(0);
    }

    @Override
    protected final void bitcoinSerializeToStream(OutputStream stream) throws IOException {
    }

    @Override
    public int getMessageSize() {
        return 0;
    }

    /* (non-Javadoc)
      * @see Message#parse()
      */
    @Override
    protected void parse() throws ProtocolException {
    }

    /* (non-Javadoc)
      * @see Message#parseLite()
      */
    @Override
    protected void parseLite() throws ProtocolException {
        setLength(0);
    }

    /* (non-Javadoc)
      * @see Message#ensureParsed()
      */
    @Override
    public void ensureParsed() throws ProtocolException {
        parsed = true;
    }

    /* (non-Javadoc)
      * @see Message#bitcoinSerialize()
      */
    @Override
    public byte[] bitcoinSerialize() {
        return new byte[0];
    }


}
