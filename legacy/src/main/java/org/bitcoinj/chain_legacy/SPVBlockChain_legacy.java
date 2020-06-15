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

package org.bitcoinj.chain_legacy;

import static com.google.common.base.Preconditions.checkArgument;

import org.bitcoinj.blockchain.ChainEventListener;
import org.bitcoinj.core.*;
import org.bitcoinj.exception.PrunedException;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.msg.p2p.FilteredBlock;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.store_legacy.BlockStore_legacy;
import org.bitcoinj.exception.BlockStoreException;
import org.bitcoinj.store_legacy.MemoryBlockStore_legacy;
import org.bitcoinj.store_legacy.SPVBlockStore_legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A BlockChain implements the <i>simplified payment verification</i> mode of the Bitcoin protocol. It is the right
 * choice to use for programs that have limited resources as it won't verify transactions signatures or attempt to store
 * all of the block chain. Really, this class should be called SPVBlockChain but for backwards compatibility it is not.
 */
public class SPVBlockChain_legacy extends AbstractBlockChain_legacy {
    /** Keeps a map of block hashes to StoredBlocks. */
    protected final BlockStore_legacy blockStore;

    /**
     * <p>Constructs a BlockChain connected to the given wallet and store.
     *
     * <p>For the store, you should use {@link SPVBlockStore_legacy} or you could also try a
     * {@link MemoryBlockStore_legacy} if you want to hold all headers in RAM and don't care about
     * disk serialization (this is rare).</p>
     */
    public SPVBlockChain_legacy(NetworkParameters params, ChainEventListener chainEventListener, BlockStore_legacy blockStore) throws BlockStoreException {
        this(params, Collections.singletonList(chainEventListener), blockStore);
    }

    /** See {@link #SPVBlockChain_legacy(NetworkParameters, BlockStore_legacy)} */
    public SPVBlockChain_legacy(NetworkParameters params, BlockStore_legacy blockStore) throws BlockStoreException {
        this(params, new ArrayList<ChainEventListener>(), blockStore);
    }

    /**
     * Constructs a BlockChain connected to the given list of listeners and a store.
     */
    public SPVBlockChain_legacy(NetworkParameters params, List<? extends ChainEventListener> chainEventListeners, BlockStore_legacy blockStore) throws BlockStoreException {
        super(params, chainEventListeners, blockStore);
        this.blockStore = blockStore;
    }

    @Override
    protected StoredBlock_legacy addToBlockStore(StoredBlock_legacy storedPrev, Block blockHeader, TransactionOutputChanges txOutChanges)
            throws BlockStoreException, VerificationException {
        StoredBlock_legacy newBlock = storedPrev.build(blockHeader);
        blockStore.put(newBlock);
        return newBlock;
    }
    
    @Override
    protected StoredBlock_legacy addToBlockStore(StoredBlock_legacy storedPrev, Block blockHeader)
            throws BlockStoreException, VerificationException {
        if (blockHeader.hasTransactions())
            blockHeader = blockHeader.cloneAsHeader();
        StoredBlock_legacy newBlock = storedPrev.build(blockHeader);
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
            StoredBlock_legacy newChainHead = blockStore.getChainHead();
            while (newChainHead.getHeight() > height) {
                newChainHead = newChainHead.getPrev(blockStore);
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
    public boolean shouldVerifyTransactions() {
        return false;
    }

    @Override
    protected TransactionOutputChanges connectTransactions(int height, Block block) {
        // Don't have to do anything as this is only called if(shouldVerifyTransactions())
        throw new UnsupportedOperationException();
    }

    @Override
    protected TransactionOutputChanges connectTransactions(StoredBlock_legacy newBlock) {
        // Don't have to do anything as this is only called if(shouldVerifyTransactions())
        throw new UnsupportedOperationException();
    }

    @Override
    protected void disconnectTransactions(StoredBlock_legacy block) {
        // Don't have to do anything as this is only called if(shouldVerifyTransactions())
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doSetChainHead(StoredBlock_legacy chainHead) throws BlockStoreException {
        blockStore.setChainHead(chainHead);
    }

    @Override
    protected void notSettingChainHead() throws BlockStoreException {
        // We don't use DB transactions here, so we don't need to do anything
    }

    @Override
    protected StoredBlock_legacy getStoredBlockInCurrentScope(Sha256Hash hash) throws BlockStoreException {
        return blockStore.get(hash);
    }

    @Override
    public boolean add(FilteredBlock block) throws VerificationException, PrunedException {
        boolean success = super.add(block);
        if (success) {
            trackFilteredTransactions(block.getTransactionCount());
        }
        return success;
    }
}
