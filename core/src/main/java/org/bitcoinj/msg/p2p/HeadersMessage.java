/*
 * Copyright 2011 Google Inc.
 * Copyright 2015 Andreas Schildbach
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

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.msg.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>A protocol message that contains a repeated series of block headers, sent in response to the "getheaders" command.
 * This is useful when you want to traverse the chain but know you don't care about the block contents, for example,
 * because you have a freshly created wallet with no keys.</p>
 *
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class HeadersMessage extends Message {
    private static final Logger log = LoggerFactory.getLogger(HeadersMessage.class);

    // The main client will never send us more than this number of headers.
    public static final int MAX_HEADERS = 2000;

    private List<Block> blockHeaders;

    public HeadersMessage(NetworkParameters params, byte[] payload) throws ProtocolException {
        super(params, payload, 0);
    }

    public HeadersMessage(NetworkParameters params, Block... headers) throws ProtocolException {
        super(params);
        blockHeaders = Arrays.asList(headers);
    }

    public HeadersMessage(NetworkParameters params, List<Block> headers) throws ProtocolException {
        super(params);
        blockHeaders = headers;
    }

    @Override
    public void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        stream.write(new VarInt(blockHeaders.size()).encode());
        for (Block header : blockHeaders) {
            header.cloneAsHeader().bitcoinSerializeToStream(stream);
            stream.write(0);
        }
    }

    @Override
    protected void parseLite() throws ProtocolException {
        if (length() == UNKNOWN_LENGTH) {
            int saveCursor = cursor;
            long numHeaders = readVarInt();
            cursor = saveCursor;

            // Each header has 80 bytes and one more byte for transactions number which is 00.
            setLength(81 * (int)numHeaders);
        }
    }

    @Override
    protected void parse() throws ProtocolException {
        long numHeaders = readVarInt();
        if (numHeaders > MAX_HEADERS)
            throw new ProtocolException("Too many headers: got " + numHeaders + " which is larger than " +
                                         MAX_HEADERS);

        blockHeaders = new ArrayList<>((int) numHeaders);

        for (int i = 0; i < numHeaders; ++i) {
            //final Block newBlockHeader = serializeMode.makeBlock(payload, cursor, 81);
            final Block newBlockHeader = new Block(params, payload, cursor, null, 81);
            if (newBlockHeader.hasTransactions()) {
                throw new ProtocolException("Block header does not end with a null byte");
            }
            cursor += newBlockHeader.getOptimalEncodingMessageSize();
            blockHeaders.add(newBlockHeader);
        }

        if (length() == UNKNOWN_LENGTH) {
            setLength(cursor - offset);
        }

        if (log.isDebugEnabled()) {
            for (int i = 0; i < numHeaders; ++i) {
                log.debug(this.blockHeaders.get(i).toString());
            }
        }
    }


    public List<Block> getBlockHeaders() {
        maybeParse();
        return blockHeaders;
    }
}
