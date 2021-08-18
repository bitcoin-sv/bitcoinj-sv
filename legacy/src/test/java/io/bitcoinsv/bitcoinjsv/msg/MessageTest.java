/*
 * Copyright 2014 Piotr Włodarek
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
import io.bitcoinsv.bitcoinjsv.core.VarInt;
import io.bitcoinsv.bitcoinjsv.params.Net;
import org.junit.Test;

public class MessageTest {

    // If readStr() is vulnerable this causes OutOfMemory
    @Test(expected = ProtocolException.class)
    public void readStrOfExtremeLength() throws Exception {
        Net net = Net.UNITTEST;
        VarInt length = new VarInt(Integer.MAX_VALUE);
        byte[] payload = length.encode();
        new VarStrMessage(net, payload);
    }

    static class VarStrMessage extends Message {
        public VarStrMessage(Net net, byte[] payload) {
            super(net, payload, 0);
        }

        @Override
        protected void parse() throws ProtocolException {
            readStr();
        }

        @Override
        protected void parseLite() throws ProtocolException {}
    }

    // If readBytes() is vulnerable this causes OutOfMemory
    @Test(expected = ProtocolException.class)
    public void readByteArrayOfExtremeLength() throws Exception {
        Net net = Net.UNITTEST;
        VarInt length = new VarInt(Integer.MAX_VALUE);
        byte[] payload = length.encode();
        new VarBytesMessage(net, payload);
    }

    static class VarBytesMessage extends Message {
        public VarBytesMessage(Net net, byte[] payload) {
            super(net, payload, 0);
        }

        @Override
        protected void parse() throws ProtocolException {
            readByteArray();
        }

        @Override
        protected void parseLite() throws ProtocolException {}
    }

}
