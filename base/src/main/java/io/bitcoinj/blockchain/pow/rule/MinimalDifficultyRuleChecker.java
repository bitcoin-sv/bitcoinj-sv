package io.bitcoinj.blockchain.pow.rule;

import io.bitcoinj.blockchain.pow.AbstractPowRulesChecker;
import io.bitcoinj.blockstore.BlockStore;
import io.bitcoinj.core.Utils;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

import java.math.BigInteger;

/**
 * After 15th February 2012 the rules on the testnet change to avoid people running up the difficulty
 * and then leaving, making it too hard to mine a block. On non-difficulty transition points, easy
 * blocks are allowed if there has been a span of 20 minutes without one.
 */
public class MinimalDifficultyRuleChecker extends AbstractPowRulesChecker {

    public MinimalDifficultyRuleChecker(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore blockStore) throws VerificationException, BlockStoreException {
        LiteBlock prevBlock = storedPrev;
        if (isPeriodExceed(prevBlock, nextBlock)) {
            checkMinimalDifficultyIsSet(nextBlock);
        }
    }

    /**
     * There is an integer underflow bug in bitcoin-qt that means mindiff blocks are accepted
     * when time goes backwards.
     */
    private boolean isPeriodExceed(LiteBlock prevBlock, LiteBlock nextBlock) {
        final long timeDelta = nextBlock.getTime() - prevBlock.getTime();
        return timeDelta >= 0 && timeDelta > NetworkParameters.TARGET_SPACING * 2;
    }

    private void checkMinimalDifficultyIsSet(LiteBlock nextBlock) {
        BigInteger maxTarget = networkParameters.getMaxTarget();
        if (!hasEqualDifficulty(nextBlock.getDifficultyTarget(), maxTarget)) {
            throw new VerificationException("Testnet block transition that is not allowed: " +
                    Long.toHexString(Utils.encodeCompactBits(maxTarget)) + " (required min difficulty) vs " +
                    Long.toHexString(nextBlock.getDifficultyTarget()));
        }
    }

}
