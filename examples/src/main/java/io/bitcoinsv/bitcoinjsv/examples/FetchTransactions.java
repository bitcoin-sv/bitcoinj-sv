/*
 * Copyright 2012 Google Inc.
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
import io.bitcoinsv.bitcoinjsv.msg.p2p.PeerAddress;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Transaction;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.params.TestNet3Params;
import io.bitcoinsv.bitcoinjsv.store_legacy.BlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.store_legacy.MemoryBlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.utils.BriefLogFormatter;
import com.google.common.util.concurrent.ListenableFuture;

import java.net.InetAddress;
import java.util.List;

/**
 * Downloads the given transaction and its dependencies from a peers memory pool then prints them out.
 */
public class FetchTransactions {
    public static void main(String[] args) throws Exception {
        BriefLogFormatter.init();
        System.out.println("Connecting to node");
        final NetworkParameters params = TestNet3Params.get();

        BlockStore_legacy blockStore = new MemoryBlockStore_legacy(params);
        SPVBlockChain_legacy chain = new SPVBlockChain_legacy(params, blockStore);
        PeerGroup peerGroup = new PeerGroup(params, chain);
        peerGroup.start();
        peerGroup.addAddress(new PeerAddress(InetAddress.getLocalHost(), params.getPort()));
        peerGroup.waitForPeers(1).get();
        Peer peer = peerGroup.getConnectedPeers().get(0);

        Sha256Hash txHash = Sha256Hash.wrap(args[0]);
        ListenableFuture<Transaction> future = peer.getPeerMempoolTransaction(txHash);
        System.out.println("Waiting for node to send us the requested transaction: " + txHash);
        Transaction tx = future.get();
        System.out.println(tx);

        System.out.println("Waiting for node to send us the dependencies ...");
        List<Transaction> deps = peer.downloadDependencies(tx).get();
        for (Transaction dep : deps) {
            System.out.println("Got dependency " + dep.getHashAsString());
        }

        System.out.println("Done.");
        peerGroup.stop();
    }
}
