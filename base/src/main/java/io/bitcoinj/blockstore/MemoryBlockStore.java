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

package io.bitcoinj.blockstore;

import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps {@link LiteBlock}s in memory. Used primarily for unit testing.
 */
public class MemoryBlockStore implements BlockStore {
    private LinkedHashMap<Sha256Hash, LiteBlock> blockMap = new LinkedHashMap<Sha256Hash, LiteBlock>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Sha256Hash, LiteBlock> eldest) {
            return blockMap.size() > 5000;
        }
    };
    private LiteBlock chainHead;
    private NetworkParameters params;

    public MemoryBlockStore(NetworkParameters params) {
        // Insert the genesis block.
        try {
            LiteBlock genesis = Genesis.getHeaderFor(params.getNet());
            put(genesis);
            setChainHead(genesis);
            this.params = params;
        } catch (BlockStoreException e) {
            throw new RuntimeException(e);  // Cannot happen.
        } catch (VerificationException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    @Override
    public synchronized final void put(LiteBlock block) throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        Sha256Hash hash = block.getHeader().getHash();
        blockMap.put(hash, block);
    }

    @Override
    public synchronized LiteBlock get(Sha256Hash hash) throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        return blockMap.get(hash);
    }

    @Override
    public LiteBlock getChainHead() throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        return chainHead;
    }

    @Override
    public final void setChainHead(LiteBlock chainHead) throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        this.chainHead = chainHead;
    }
    
    @Override
    public void close() {
        blockMap = null;
    }

    @Override
    public NetworkParameters getParams() {
        return params;
    }
}
