package org.bitcoinj.pow_legacy.rule;

import org.bitcoinj.chain_legacy.SPVBlockChain_legacy;
import org.bitcoinj.chain_legacy.StoredBlock_legacy;
import org.bitcoinj.core.*;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.pow_legacy.AbstractPowRulesChecker_legacy;
import org.bitcoinj.store_legacy.BlockStore_legacy;
import org.bitcoinj.exception.BlockStoreException;

import java.math.BigInteger;

public class EmergencyDifficultyAdjustmentRuleChecker_legacy extends AbstractPowRulesChecker_legacy {

    private static final long TARGET_PRODUCTION_TIME_IN_SECONDS = 12 * 60 * 60; // 12 hours
    private static final int REFERENCE_OF_BLOCKS_PRODUCED_SIZE = 6;

    public EmergencyDifficultyAdjustmentRuleChecker_legacy(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(StoredBlock_legacy storedPrev, Block nextBlock, BlockStore_legacy blockStore) throws VerificationException, BlockStoreException {

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

    private long getMedianProducingTimeInSeconds(int sizeOfBlocks, StoredBlock_legacy storedPrev, BlockStore_legacy blockStore) throws BlockStoreException {
        StoredBlock_legacy cursor = blockStore.get(storedPrev.getHeader().getHash());
        for (int i = 0; i < sizeOfBlocks; i++) {
            if (cursor == null) {
                throw new NullPointerException("Not enough blocks to check difficulty.");
            }
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }
        //Check to see if there are enough blocks before cursor to correctly calculate the median time
        StoredBlock_legacy beforeCursor = cursor;
        for (int i = 0; i < 10; i++) {
            beforeCursor = blockStore.get(beforeCursor.getHeader().getPrevBlockHash());
            if(beforeCursor == null)
                throw new NullPointerException("Not enough blocks to check difficulty.");
        }
        return SPVBlockChain_legacy.getMedianTimestampOfRecentBlocks(storedPrev, blockStore) -
                SPVBlockChain_legacy.getMedianTimestampOfRecentBlocks(cursor, blockStore);
    }

    private void checkEDARules(StoredBlock_legacy storedPrev, Block nextBlock, long lastBlocksMPTinSeconds) {
        Block prevBlock = storedPrev.getHeader();
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

    private BigInteger calculateReducedDifficulty(Block prevBlock) {
        BigInteger pow = prevBlock.getDifficultyTargetAsInteger();
        // Divide difficulty target by 1/4 (which reduces the difficulty by 20%)
        pow = pow.add(pow.shiftRight(2));

        if (pow.compareTo(networkParameters.getMaxTarget()) > 0) {
            pow = networkParameters.getMaxTarget();
        }
        return pow;
    }

    private void throwUnexpectedReducedDifficultyException(StoredBlock_legacy storedPrev, Block nextBlock, BigInteger nPow) {
        throw new VerificationException("Unexpected change in difficulty [6 blocks >12 hours] at height " + storedPrev.getHeight() +
                ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                Utils.encodeCompactBits(nPow));
    }

    private void throwUnexpectedDifficultyChangedException(Block prevBlock, Block nextBlock, StoredBlock_legacy storedPrev) {
        throw new VerificationException("Unexpected change in difficulty at height " + storedPrev.getHeight() +
                ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                Long.toHexString(prevBlock.getDifficultyTarget()));
    }

}
