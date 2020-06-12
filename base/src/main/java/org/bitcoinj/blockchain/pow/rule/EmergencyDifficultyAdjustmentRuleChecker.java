package org.bitcoinj.blockchain.pow.rule;

import org.bitcoinj.blockchain.pow.AbstractPowRulesChecker;
import org.bitcoinj.blockstore.BlockStore;
import org.bitcoinj.blockchain.ChainUtils;
import org.bitcoinj.core.Utils;
import org.bitcoinj.exception.BlockStoreException;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.bitcoin.api.extended.LiteBlock;
import org.bitcoinj.params.NetworkParameters;

import java.math.BigInteger;

public class EmergencyDifficultyAdjustmentRuleChecker extends AbstractPowRulesChecker {

    private static final long TARGET_PRODUCTION_TIME_IN_SECONDS = 12 * 60 * 60; // 12 hours
    private static final int REFERENCE_OF_BLOCKS_PRODUCED_SIZE = 6;

    public EmergencyDifficultyAdjustmentRuleChecker(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore blockStore) throws VerificationException, BlockStoreException {

        try {
            long lastBlocksMPTinSeconds = getMedianProducingTimeInSeconds(REFERENCE_OF_BLOCKS_PRODUCED_SIZE,
                    storedPrev, blockStore);
            checkEDARules(storedPrev, nextBlock, lastBlocksMPTinSeconds);
        } catch (NullPointerException ex) {
            // We don't have enough blocks, yet
        } catch (BlockStoreException ex) {
            // We don't have enough blocks, yet
        }
    }

    private long getMedianProducingTimeInSeconds(int sizeOfBlocks, LiteBlock storedPrev, BlockStore blockStore) throws BlockStoreException {
        LiteBlock cursor = blockStore.get(storedPrev.getHeader().getHash());
        for (int i = 0; i < sizeOfBlocks; i++) {
            if (cursor == null) {
                throw new NullPointerException("Not enough blocks to check difficulty.");
            }
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }
        //Check to see if there are enough blocks before cursor to correctly calculate the median time
        LiteBlock beforeCursor = cursor;
        for (int i = 0; i < 10; i++) {
            beforeCursor = blockStore.get(beforeCursor.getHeader().getPrevBlockHash());
            if(beforeCursor == null)
                throw new NullPointerException("Not enough blocks to check difficulty.");
        }
        return ChainUtils.getMedianTimestampOfRecentBlocks(storedPrev, blockStore) -
                ChainUtils.getMedianTimestampOfRecentBlocks(cursor, blockStore);
    }

    private void checkEDARules(LiteBlock storedPrev, LiteBlock nextBlock, long lastBlocksMPTinSeconds) {
        LiteBlock prevBlock = storedPrev;
        if (needToReduceTheDifficulty(lastBlocksMPTinSeconds)) {
            BigInteger nPow = calculateReducedDifficulty(prevBlock);
            if (!hasEqualDifficulty(nextBlock.getDifficultyTarget(), nPow)) {
                throwUnexpectedReducedDifficultyException(storedPrev, nextBlock, nPow);
            }
        } else {
            if (!hasEqualDifficulty(prevBlock, nextBlock)) {
                throwUnexpectedDifficultyChangedException(prevBlock, nextBlock, storedPrev);
            }
        }
    }

    private boolean needToReduceTheDifficulty(long lastBlocksMPTinSeconds) {
        return lastBlocksMPTinSeconds >= TARGET_PRODUCTION_TIME_IN_SECONDS;
    }

    private BigInteger calculateReducedDifficulty(LiteBlock prevBlock) {
        BigInteger pow = prevBlock.getDifficultyTargetAsInteger();
        // Divide difficulty target by 1/4 (which reduces the difficulty by 20%)
        pow = pow.add(pow.shiftRight(2));

        if (pow.compareTo(networkParameters.getMaxTarget()) > 0) {
            pow = networkParameters.getMaxTarget();
        }
        return pow;
    }

    private void throwUnexpectedReducedDifficultyException(LiteBlock storedPrev, LiteBlock nextBlock, BigInteger nPow) {
        throw new VerificationException("Unexpected change in difficulty [6 blocks >12 hours] at height " + storedPrev.getChainInfo().getHeight() +
                ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                Utils.encodeCompactBits(nPow));
    }

    private void throwUnexpectedDifficultyChangedException(LiteBlock prevBlock, LiteBlock nextBlock, LiteBlock storedPrev) {
        throw new VerificationException("Unexpected change in difficulty at height " + storedPrev.getChainInfo().getHeight() +
                ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                Long.toHexString(prevBlock.getDifficultyTarget()));
    }

}
