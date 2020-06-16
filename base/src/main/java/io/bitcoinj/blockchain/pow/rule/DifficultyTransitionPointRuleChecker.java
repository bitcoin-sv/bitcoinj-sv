package io.bitcoinj.blockchain.pow.rule;

import io.bitcoinj.blockstore.BlockStore;
import io.bitcoinj.blockchain.pow.AbstractPowRulesChecker;
import io.bitcoinj.core.Utils;
import io.bitcoinj.core.Verification;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

import java.math.BigInteger;

public class DifficultyTransitionPointRuleChecker extends AbstractPowRulesChecker {

    public DifficultyTransitionPointRuleChecker(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore blockStore) throws VerificationException, BlockStoreException {

        LiteBlock prevBlock = storedPrev;

        LiteBlock lastBlockInterval = findLastBlockInterval(prevBlock, blockStore);
        int timeSpan = (int) (prevBlock.getTime() - lastBlockInterval.getTime());
        BigInteger newTarget = calculateNewTarget(prevBlock, timeSpan);

        Verification.verifyDifficulty(networkParameters, newTarget, nextBlock);
    }

    private LiteBlock findLastBlockInterval(LiteBlock prevBlock, BlockStore blockStore) throws BlockStoreException {
        LiteBlock cursor = blockStore.get(prevBlock.getHash());
        for (int i = 0; i < networkParameters.getInterval() - 1; i++) {
            if (cursor == null) {
                throw new VerificationException("Difficulty transition point but we did " +
                        "not find a way back to the genesis block.");
            }
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }
        return cursor;
    }

    private BigInteger calculateNewTarget(LiteBlock prevBlock, int timeSpan) {
        int targetTimespan = networkParameters.getTargetTimespan();
        if (timeSpan < targetTimespan / 4) {
            timeSpan = targetTimespan / 4;
        } else if (timeSpan > targetTimespan * 4) {
            timeSpan = targetTimespan * 4;
        }

        return Utils.decodeCompactBits(prevBlock.getDifficultyTarget())
                .multiply(BigInteger.valueOf(timeSpan))
                .divide(BigInteger.valueOf(targetTimespan));
    }

}
