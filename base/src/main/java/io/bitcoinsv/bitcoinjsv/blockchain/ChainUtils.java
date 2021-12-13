/*
 * Modifications described in the NOTICE.txt file are licensed under the Open BSV Licence.
 * Modifications Copyright 2020 Bitcoin Association
 */
package io.bitcoinsv.bitcoinjsv.blockchain;

import io.bitcoinsv.bitcoinjsv.blockstore.BlockStore;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;

import java.math.BigInteger;
import java.util.Arrays;

public class ChainUtils {

    /** Returns true if this objects chainWork is higher than the others. */
    public static boolean isMoreWorkThan(LiteBlock thisBlock, LiteBlock other) {
        return thisBlock.getChainInfo().getChainWork().compareTo(other.getChainInfo().getChainWork()) > 0;
    }

    /**
     * Creates a new StoredBlock, calculating the additional fields by adding to the values in this block.
     */
    public static LiteBlock buildNextInChain(LiteBlock thisBlock, LiteBlock nextBlock) throws VerificationException {
        // Stored blocks track total work done in this chain, because the canonical chain is the one that represents
        // the largest amount of work done not the tallest.
        BigInteger chainWork = thisBlock.getChainInfo().getChainWork().add(nextBlock.getHeader().getWork());
        int height = thisBlock.getChainInfo().getHeight() + 1;
        nextBlock = (LiteBlock) nextBlock.copy();
        nextBlock.getChainInfo().makeMutable();
        nextBlock.getChainInfo().setChainWork(chainWork);
        nextBlock.getChainInfo().setHeight(height);
        nextBlock.getChainInfo().setTotalChainTxs(thisBlock.getTotalChainTxs() + nextBlock.getBlockMeta().getTxCount());
        return nextBlock;
    }

    /**
     * Gets the median timestamp of the last 11 blocks
     */
    public static long getMedianTimestampOfRecentBlocks(LiteBlock storedBlock, BlockStore<LiteBlock>  store) throws BlockStoreException {
        long[] timestamps = new long[11];
        int unused = 9;
        timestamps[10] = storedBlock.getHeader().getTime();
        while (unused >= 0 && (storedBlock = store.getPrev(storedBlock)) != null)
            timestamps[unused--] = storedBlock.getHeader().getTime();

        Arrays.sort(timestamps, unused+1, 11);
        return timestamps[unused + (11-unused)/2];
    }
}
