package org.bitcoinj.blockchain.pow.rule;

import org.bitcoinj.blockchain.pow.AbstractPowRulesChecker;
import org.bitcoinj.blockstore.BlockStore;
import org.bitcoinj.exception.BlockStoreException;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.bitcoin.Genesis;
import org.bitcoinj.msg.bitcoin.api.extended.LiteBlock;
import org.bitcoinj.params.NetworkParameters;

import java.math.BigInteger;

/**
 * Walk backwards until we find a block that doesn't have the easiest proof of work,
 * then check that difficulty is equal to that one.
 */
public class LastNonMinimalDifficultyRuleChecker extends AbstractPowRulesChecker {

    public LastNonMinimalDifficultyRuleChecker(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore blockStore) throws VerificationException, BlockStoreException {
        LiteBlock prevBlock = storedPrev;
        if (isUnderPeriod(prevBlock, nextBlock)) {
            checkLastNonMinimalDifficultyIsSet(storedPrev, blockStore, nextBlock);
        }
    }

    private boolean isUnderPeriod(LiteBlock prevBlock, LiteBlock nextBlock) {
        final long timeDelta = nextBlock.getTime() - prevBlock.getTime();
        return timeDelta >= 0 && timeDelta <= NetworkParameters.TARGET_SPACING * 2;
    }

    private void checkLastNonMinimalDifficultyIsSet(LiteBlock storedPrev, BlockStore blockStore, LiteBlock nextBlock) throws BlockStoreException {
        try {
            LiteBlock lastNotEasiestPowBlock = findLastNotEasiestPowBlock(storedPrev, blockStore);
            if (!hasEqualDifficulty(lastNotEasiestPowBlock, nextBlock))
                throw new VerificationException("Testnet block transition that is not allowed: " +
                        Long.toHexString(lastNotEasiestPowBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(nextBlock.getDifficultyTarget()));
        } catch (BlockStoreException ex) {
            // we don't have enough blocks, yet
        }

    }

    private LiteBlock findLastNotEasiestPowBlock(LiteBlock storedPrev, BlockStore blockStore) throws BlockStoreException {
        LiteBlock cursor = storedPrev;
        BigInteger easiestDifficulty = networkParameters.getMaxTarget();
        while (!cursor.getHeader().equals(Genesis.getFor(networkParameters.getNet())) &&
                cursor.getChainInfo().getHeight() % networkParameters.getInterval() != 0 &&
                hasEqualDifficulty(cursor.getHeader().getDifficultyTarget(), easiestDifficulty)) {
            cursor = blockStore.getPrev(cursor);
        }
        return cursor;
    }

}
