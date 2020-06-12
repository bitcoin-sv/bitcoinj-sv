package org.bitcoinj.pow_legacy.rule;

import org.bitcoinj.chain_legacy.StoredBlock_legacy;
import org.bitcoinj.core.*;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.pow_legacy.AbstractPowRulesChecker_legacy;
import org.bitcoinj.store.BlockStore_legacy;
import org.bitcoinj.exception.BlockStoreException;

import java.math.BigInteger;

/**
 * After 15th February 2012 the rules on the testnet change to avoid people running up the difficulty
 * and then leaving, making it too hard to mine a block. On non-difficulty transition points, easy
 * blocks are allowed if there has been a span of 20 minutes without one.
 */
public class MinimalDifficultyRuleChecker_legacy extends AbstractPowRulesChecker_legacy {

    public MinimalDifficultyRuleChecker_legacy(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(StoredBlock_legacy storedPrev, Block nextBlock, BlockStore_legacy blockStore) throws VerificationException, BlockStoreException {
        Block prevBlock = storedPrev.getHeader();
        if (isPeriodExceed(prevBlock, nextBlock)) {
            checkMinimalDifficultyIsSet(nextBlock);
        }
    }

    /**
     * There is an integer underflow bug in bitcoin-qt that means mindiff blocks are accepted
     * when time goes backwards.
     */
    private boolean isPeriodExceed(Block prevBlock, Block nextBlock) {
        final long timeDelta = nextBlock.getTime() - prevBlock.getTime();
        return timeDelta >= 0 && timeDelta > NetworkParameters.TARGET_SPACING * 2;
    }

    private void checkMinimalDifficultyIsSet(Block nextBlock) {
        BigInteger maxTarget = networkParameters.getMaxTarget();
        if (!hasEqualDifficulty(nextBlock.getDifficultyTarget(), maxTarget)) {
            throw new VerificationException("Testnet block transition that is not allowed: " +
                    Long.toHexString(Utils.encodeCompactBits(maxTarget)) + " (required min difficulty) vs " +
                    Long.toHexString(nextBlock.getDifficultyTarget()));
        }
    }

}
