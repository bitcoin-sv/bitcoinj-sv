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

package io.bitcoinsv.bitcoinjsv.examples;

import io.bitcoinsv.bitcoinjsv.chain_legacy.SPVBlockChain_legacy;
import io.bitcoinsv.bitcoinjsv.core.Peer;
import io.bitcoinsv.bitcoinjsv.core.PeerGroup;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Block;
import io.bitcoinsv.bitcoinjsv.msg.p2p.PeerAddress;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.params.TestNet3Params;
import io.bitcoinsv.bitcoinjsv.store_legacy.BlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.store_legacy.MemoryBlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.utils.BriefLogFormatter;

import java.net.InetAddress;
import java.util.concurrent.Future;

/**
 * Downloads the block given a block hash from the localhost node and prints it out.
 */
public class FetchBlock {
    public static void main(String[] args) throws Exception {
        BriefLogFormatter.init();
        System.out.println("Connecting to node");
        final NetworkParameters params = TestNet3Params.get();

        BlockStore_legacy blockStore = new MemoryBlockStore_legacy(params);
        SPVBlockChain_legacy chain = new SPVBlockChain_legacy(params, blockStore);
        PeerGroup peerGroup = new PeerGroup(params, chain);
        peerGroup.start();
        PeerAddress addr = new PeerAddress(InetAddress.getLocalHost(), params.getPort());
        peerGroup.addAddress(addr);
        peerGroup.waitForPeers(1).get();
        Peer peer = peerGroup.getConnectedPeers().get(0);

        Sha256Hash blockHash = Sha256Hash.wrap(args[0]);
        Future<Block> future = peer.getBlock(blockHash);
        System.out.println("Waiting for node to send us the requested block: " + blockHash);
        Block block = future.get();
        System.out.println(block);
        peerGroup.stopAsync();
    }
}
