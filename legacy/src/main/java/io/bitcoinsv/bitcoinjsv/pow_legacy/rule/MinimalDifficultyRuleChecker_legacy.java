package io.bitcoinsv.bitcoinjsv.pow_legacy.rule;

import io.bitcoinsv.bitcoinjsv.chain_legacy.StoredBlock_legacy;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Block;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.pow_legacy.AbstractPowRulesChecker_legacy;
import io.bitcoinsv.bitcoinjsv.store_legacy.BlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.core.Utils;

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
