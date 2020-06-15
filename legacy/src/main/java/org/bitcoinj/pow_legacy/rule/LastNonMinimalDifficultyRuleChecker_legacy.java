package org.bitcoinj.pow_legacy.rule;

import org.bitcoinj.chain_legacy.StoredBlock_legacy;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.Genesis_legacy;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.pow_legacy.AbstractPowRulesChecker_legacy;
import org.bitcoinj.store_legacy.BlockStore_legacy;
import org.bitcoinj.exception.BlockStoreException;

import java.math.BigInteger;

/**
 * Walk backwards until we find a block that doesn't have the easiest proof of work,
 * then check that difficulty is equal to that one.
 */
public class LastNonMinimalDifficultyRuleChecker_legacy extends AbstractPowRulesChecker_legacy {

    public LastNonMinimalDifficultyRuleChecker_legacy(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(StoredBlock_legacy storedPrev, Block nextBlock, BlockStore_legacy blockStore) throws VerificationException, BlockStoreException {
        Block prevBlock = storedPrev.getHeader();
        if (isUnderPeriod(prevBlock, nextBlock)) {
            checkLastNonMinimalDifficultyIsSet(storedPrev, blockStore, nextBlock);
        }
    }

    private boolean isUnderPeriod(Block prevBlock, Block nextBlock) {
        final long timeDelta = nextBlock.getTime() - prevBlock.getTime();
        return timeDelta >= 0 && timeDelta <= NetworkParameters.TARGET_SPACING * 2;
    }

    private void checkLastNonMinimalDifficultyIsSet(StoredBlock_legacy storedPrev, BlockStore_legacy blockStore, Block nextBlock) throws BlockStoreException {
        try {
            Block lastNotEasiestPowBlock = findLastNotEasiestPowBlock(storedPrev, blockStore);
            if (!hasEqualDifficulty(lastNotEasiestPowBlock, nextBlock))
                throw new VerificationException("Testnet block transition that is not allowed: " +
                        Long.toHexString(lastNotEasiestPowBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(nextBlock.getDifficultyTarget()));
        } catch (BlockStoreException ex) {
            // we don't have enough blocks, yet
        }

    }

    private Block findLastNotEasiestPowBlock(StoredBlock_legacy storedPrev, BlockStore_legacy blockStore) throws BlockStoreException {
        StoredBlock_legacy cursor = storedPrev;
        BigInteger easiestDifficulty = networkParameters.getMaxTarget();
        while (!cursor.getHeader().equals(Genesis_legacy.getFor(networkParameters)) &&
                cursor.getHeight() % networkParameters.getInterval() != 0 &&
                hasEqualDifficulty(cursor.getHeader().getDifficultyTarget(), easiestDifficulty)) {
            cursor = cursor.getPrev(blockStore);
        }
        return cursor.getHeader();
    }

}
