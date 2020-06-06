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

package org.bitcoinj.msg.p2p;

import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Utils;
import org.bitcoinj.msg.Message;
import org.bitcoinj.params.Net;

/**
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class UnknownMessage extends Message {

    private String name;

    public UnknownMessage(Net net, String name, byte[] payloadBytes, int length) throws ProtocolException {
        super(net, payloadBytes, 0, net.params().getProtocolVersionNum(NetworkParameters.ProtocolVersion.CURRENT), null, length);
        this.name = name;
    }

    @Override
    public String toString() {
        return "Unknown message [" + name + "]: " + (payload == null ? "" : Utils.HEX.encode(payload));
    }

    @Override
    protected void parse() throws ProtocolException {
    }

    @Override
    protected void parseLite() throws ProtocolException {
    }
}
