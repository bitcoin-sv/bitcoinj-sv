package io.bitcoinj.blockchain.pow.rule;

import com.google.common.base.Preconditions;
import io.bitcoinj.blockchain.pow.AbstractPowRulesChecker;
import io.bitcoinj.blockstore.BlockStore;
import io.bitcoinj.core.Verification;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

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
public class NewDifficultyAdjustmentAlgorithmRulesChecker extends AbstractPowRulesChecker {

    private static final int AVERAGE_BLOCKS_PER_DAY = 144;

    public NewDifficultyAdjustmentAlgorithmRulesChecker(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore<LiteBlock> blockStore) throws VerificationException {
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
    private void checkNextCashWorkRequired(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore<LiteBlock>  blockStore) {
        int prevHeight = storedPrev.getChainInfo().getHeight();
        Preconditions.checkState(prevHeight >= networkParameters.getInterval());

        try {
            LiteBlock last = GetMostSuitableBlock(storedPrev, blockStore);
            LiteBlock first = getFirst(storedPrev, blockStore);

            BigInteger nextTarget = Verification.ComputeTarget(
                    first.getChainInfo().getChainWork(), first.getTime(), first.getChainInfo().getHeight(),
                    last.getChainInfo().getChainWork(), last.getTime(), last.getChainInfo().getHeight());
            Verification.verifyDifficulty(networkParameters, nextTarget, nextBlock);
        } catch (BlockStoreException x) {
            // We don't have enough blocks, yet
        }
    }

    /**
     * To reduce the impact of timestamp manipulation, we select the block we are
     * basing our computation on via a median of 3.
     */
    private LiteBlock GetMostSuitableBlock(LiteBlock pindex, BlockStore<LiteBlock>  blockStore) throws BlockStoreException {
        /**
         * In order to avoid a block is a very skewed timestamp to have too much
         * influence, we select the median of the 3 top most blocks as a starting
         * point.
         */
        LiteBlock blocks[] = new LiteBlock[3];
        blocks[2] = pindex;
        blocks[1] = blockStore.getPrev(pindex);
        if(blocks[1] == null)
            throw new BlockStoreException("Not enough blocks in blockStore to calculate difficulty");
        blocks[0] = blockStore.getPrev(blocks[1]);
        if(blocks[0] == null)
            throw new BlockStoreException("Not enough blocks in blockStore to calculate difficulty");

        // Sorting network.
        if (blocks[0].getHeader().getTime() > blocks[2].getHeader().getTime()) {
            //std::swap(blocks[0], blocks[2]);
            LiteBlock temp = blocks[0];
            blocks[0] = blocks[2];
            blocks[2] = temp;
        }

        if (blocks[0].getHeader().getTime() > blocks[1].getHeader().getTime()) {
            //std::swap(blocks[0], blocks[1]);
            LiteBlock temp = blocks[0];
            blocks[0] = blocks[1];
            blocks[1] = temp;
        }

        if (blocks[1].getHeader().getTime() > blocks[2].getHeader().getTime()) {
            //std::swap(blocks[1], blocks[2]);
            LiteBlock temp = blocks[1];
            blocks[1] = blocks[2];
            blocks[2] = temp;
        }

        // We should have our candidate in the middle now.
        return blocks[1];
    }

    private LiteBlock getFirst(LiteBlock storedPrev, BlockStore<LiteBlock>  blockStore) throws BlockStoreException {
        LiteBlock first = storedPrev;
        for (int i = AVERAGE_BLOCKS_PER_DAY; i > 0; --i) {
            first = blockStore.getPrev(first);
            if (first == null) {
                throw new BlockStoreException("The previous block no longer exists");
            }
        }
        return GetMostSuitableBlock(first, blockStore);
    }

}
