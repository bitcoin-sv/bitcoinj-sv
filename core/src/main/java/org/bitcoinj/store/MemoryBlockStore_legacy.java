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

package org.bitcoinj.store;

import org.bitcoinj.chain_legacy.StoredBlock_legacy;
import org.bitcoinj.core.*;
import org.bitcoinj.exception.BlockStoreException;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.Genesis_legacy;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps {@link StoredBlock_legacy}s in memory. Used primarily for unit testing.
 */
public class MemoryBlockStore_legacy implements BlockStore_legacy {
    private LinkedHashMap<Sha256Hash, StoredBlock_legacy> blockMap = new LinkedHashMap<Sha256Hash, StoredBlock_legacy>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Sha256Hash, StoredBlock_legacy> eldest) {
            return blockMap.size() > 5000;
        }
    };
    private StoredBlock_legacy chainHead;
    private NetworkParameters params;

    public MemoryBlockStore_legacy(NetworkParameters params) {
        // Insert the genesis block.
        try {
            Block genesisHeader = Genesis_legacy.getFor(params).cloneAsHeader();
            StoredBlock_legacy storedGenesis = new StoredBlock_legacy(genesisHeader, genesisHeader.getWork(), 0);
            put(storedGenesis);
            setChainHead(storedGenesis);
            this.params = params;
        } catch (BlockStoreException e) {
            throw new RuntimeException(e);  // Cannot happen.
        } catch (VerificationException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    @Override
    public synchronized final void put(StoredBlock_legacy block) throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        Sha256Hash hash = block.getHeader().getHash();
        blockMap.put(hash, block);
    }

    @Override
    public synchronized StoredBlock_legacy get(Sha256Hash hash) throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        return blockMap.get(hash);
    }

    @Override
    public StoredBlock_legacy getChainHead() throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        return chainHead;
    }

    @Override
    public final void setChainHead(StoredBlock_legacy chainHead) throws BlockStoreException {
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
