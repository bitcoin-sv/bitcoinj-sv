/*
 * Modifications described in the NOTICE.txt file are licensed under the Open BSV Licence.
 * Modifications Copyright 2020 Bitcoin Association
 *
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

package io.bitcoinj.blockchain;

import io.bitcoinj.blockstore.BlockStore;
import io.bitcoinj.blockstore.MemoryBlockStore;
import io.bitcoinj.blockstore.SPVBlockStore;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A BlockChain implements the <i>simplified payment verification</i> mode of the Bitcoin protocol. It is the right
 * choice to use for programs that have limited resources as it won't verify transactions signatures or attempt to store
 * all of the block chain. Really, this class should be called SPVBlockChain but for backwards compatibility it is not.
 */
public class SPVBlockChain extends AbstractBlockChain {
    /** Keeps a map of block hashes to StoredBlocks. */
    protected final BlockStore blockStore;

    /**
     * <p>Constructs a BlockChain connected to the given wallet and store.
     *
     * <p>For the store, you should use {@link SPVBlockStore} or you could also try a
     * {@link MemoryBlockStore} if you want to hold all headers in RAM and don't care about
     * disk serialization (this is rare).</p>
     */
    public SPVBlockChain(NetworkParameters params, ChainEventListener chainEventListener, BlockStore blockStore) throws BlockStoreException {
        this(params, Collections.singletonList(chainEventListener), blockStore);
    }

    /** See {@link #SPVBlockChain(NetworkParameters, BlockStore)} */
    public SPVBlockChain(NetworkParameters params, BlockStore blockStore) throws BlockStoreException {
        this(params, new ArrayList<ChainEventListener>(), blockStore);
    }

    /**
     * Constructs a BlockChain connected to the given list of listeners and a store.
     */
    public SPVBlockChain(NetworkParameters params, List<? extends ChainEventListener> chainEventListeners, BlockStore blockStore) throws BlockStoreException {
        super(params, chainEventListeners, blockStore);
        this.blockStore = blockStore;
    }

    @Override
    protected LiteBlock addToBlockStore(LiteBlock storedPrev, LiteBlock blockHeader)
            throws BlockStoreException, VerificationException {
        LiteBlock newBlock = ChainUtils.buildNextInChain(storedPrev, blockHeader);
        blockStore.put(newBlock);
        return newBlock;
    }

    @Override
    protected void rollbackBlockStore(int height) throws BlockStoreException {
        lock.lock();
        try {
            int currentHeight = getBestChainHeight();
            checkArgument(height >= 0 && height <= currentHeight, "Bad height: %s", height);
            if (height == currentHeight)
                return; // nothing to do

            // Look for the block we want to be the new chain head
            LiteBlock newChainHead = blockStore.getChainHead();
            while (newChainHead.getHeight() > height) {
                newChainHead = blockStore.getPrev(newChainHead);
                if (newChainHead == null)
                    throw new BlockStoreException("Unreachable height");
            }

            // Modify store directly
            blockStore.put(newChainHead);
            this.setChainHead(newChainHead);
        } finally {
            lock.unlock();
        }
    }
    @Override
    protected void doSetChainHead(LiteBlock chainHead) throws BlockStoreException {
        blockStore.setChainHead(chainHead);
    }

    @Override
    protected void notSettingChainHead() throws BlockStoreException {
        // We don't use DB transactions here, so we don't need to do anything
    }

    @Override
    protected LiteBlock getStoredBlockInCurrentScope(Sha256Hash hash) throws BlockStoreException {
        return blockStore.get(hash);
    }

}
