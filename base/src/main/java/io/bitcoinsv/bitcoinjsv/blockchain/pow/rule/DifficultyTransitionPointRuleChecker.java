package io.bitcoinsv.bitcoinjsv.blockchain.pow.rule;

import io.bitcoinsv.bitcoinjsv.blockstore.BlockStore;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.AbstractPowRulesChecker;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.core.Verification;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;

import java.math.BigInteger;

public class DifficultyTransitionPointRuleChecker extends AbstractPowRulesChecker {

    public DifficultyTransitionPointRuleChecker(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore<LiteBlock> blockStore) throws VerificationException, BlockStoreException {

        LiteBlock prevBlock = storedPrev;

        LiteBlock lastBlockInterval = findLastBlockInterval(prevBlock, blockStore);
        int timeSpan = (int) (prevBlock.getTime() - lastBlockInterval.getTime());
        BigInteger newTarget = calculateNewTarget(prevBlock, timeSpan);

        Verification.verifyDifficulty(networkParameters, newTarget, nextBlock);
    }

    private LiteBlock findLastBlockInterval(LiteBlock prevBlock, BlockStore<LiteBlock> blockStore) throws BlockStoreException {
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
