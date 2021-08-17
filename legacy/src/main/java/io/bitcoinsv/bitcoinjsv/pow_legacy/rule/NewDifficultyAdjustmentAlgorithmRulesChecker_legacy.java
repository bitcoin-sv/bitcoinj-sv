package io.bitcoinsv.bitcoinjsv.pow_legacy.rule;

import com.google.common.base.Preconditions;
import io.bitcoinsv.bitcoinjsv.chain_legacy.StoredBlock_legacy;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Block;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.pow_legacy.AbstractPowRulesChecker_legacy;
import io.bitcoinsv.bitcoinjsv.store_legacy.BlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.core.Verification;

import java.math.BigInteger;

/**
 * The new DAA algorithm seeks to accomplish the following objectives:
 * - Adjust difficulty to hash rate to target a mean block interval of 600 seconds.
 * - Avoid sudden changes in difficulty when hash rate is fairly stable.
 * - Adjust difficulty rapidly when hash rate changes rapidly.
 * - Avoid oscillations from feedback between hash rate and difficulty.
 * - Be resilient to attacks such as timestamp manipulation.
 * <p>
 * https://www.bitcoinabc.org/november
 */
public class NewDifficultyAdjustmentAlgorithmRulesChecker_legacy extends AbstractPowRulesChecker_legacy {

    private static final int AVERAGE_BLOCKS_PER_DAY = 144;

    public NewDifficultyAdjustmentAlgorithmRulesChecker_legacy(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(StoredBlock_legacy storedPrev, Block nextBlock, BlockStore_legacy blockStore) throws VerificationException, BlockStoreException {
        checkNextCashWorkRequired(storedPrev, nextBlock, blockStore);
    }

    /**
     * Compute the next required proof of work using a weighted average of the
     * estimated hashrate per block.
     * <p>
     * Using a weighted average ensure that the timestamp parameter cancels out in
     * most of the calculation - except for the timestamp of the first and last
     * block. Because timestamps are the least trustworthy information we have as
     * input, this ensures the algorithm is more resistant to malicious inputs.
     */
    private void checkNextCashWorkRequired(StoredBlock_legacy storedPrev, Block nextBlock, BlockStore_legacy blockStore) {
        int prevHeight = storedPrev.getHeight();
        Preconditions.checkState(prevHeight >= networkParameters.getInterval());

        try {
            StoredBlock_legacy last = GetMostSuitableBlock(storedPrev, blockStore);
            StoredBlock_legacy first = getFirst(storedPrev, blockStore);

            BigInteger nextTarget = Verification.ComputeTarget(
                    first.getChainWork(), first.getHeader().getTime(), first.getHeight(),
                    last.getChainWork(), last.getHeader().getTime(), last.getHeight());
            Verification.verifyDifficulty(networkParameters, nextTarget, nextBlock);
        } catch (BlockStoreException x) {
            // We don't have enough blocks, yet
        }
    }

    /**
     * To reduce the impact of timestamp manipulation, we select the block we are
     * basing our computation on via a median of 3.
     */
    private StoredBlock_legacy GetMostSuitableBlock(StoredBlock_legacy pindex, BlockStore_legacy blockStore) throws BlockStoreException {
        /**
         * In order to avoid a block is a very skewed timestamp to have too much
         * influence, we select the median of the 3 top most blocks as a starting
         * point.
         */
        StoredBlock_legacy blocks[] = new StoredBlock_legacy[3];
        blocks[2] = pindex;
        blocks[1] = pindex.getPrev(blockStore);
        if(blocks[1] == null)
            throw new BlockStoreException("Not enough blocks in blockStore to calculate difficulty");
        blocks[0] = blocks[1].getPrev(blockStore);
        if(blocks[0] == null)
            throw new BlockStoreException("Not enough blocks in blockStore to calculate difficulty");

        // Sorting network.
        if (blocks[0].getHeader().getTime() > blocks[2].getHeader().getTime()) {
            //std::swap(blocks[0], blocks[2]);
            StoredBlock_legacy temp = blocks[0];
            blocks[0] = blocks[2];
            blocks[2] = temp;
        }

        if (blocks[0].getHeader().getTime() > blocks[1].getHeader().getTime()) {
            //std::swap(blocks[0], blocks[1]);
            StoredBlock_legacy temp = blocks[0];
            blocks[0] = blocks[1];
            blocks[1] = temp;
        }

        if (blocks[1].getHeader().getTime() > blocks[2].getHeader().getTime()) {
            //std::swap(blocks[1], blocks[2]);
            StoredBlock_legacy temp = blocks[1];
            blocks[1] = blocks[2];
            blocks[2] = temp;
        }

        // We should have our candidate in the middle now.
        return blocks[1];
    }

    private StoredBlock_legacy getFirst(StoredBlock_legacy storedPrev, BlockStore_legacy blockStore) throws BlockStoreException {
        StoredBlock_legacy first = storedPrev;
        for (int i = AVERAGE_BLOCKS_PER_DAY; i > 0; --i) {
            first = first.getPrev(blockStore);
            if (first == null) {
                throw new BlockStoreException("The previous block no longer exists");
            }
        }
        return GetMostSuitableBlock(first, blockStore);
    }

}
