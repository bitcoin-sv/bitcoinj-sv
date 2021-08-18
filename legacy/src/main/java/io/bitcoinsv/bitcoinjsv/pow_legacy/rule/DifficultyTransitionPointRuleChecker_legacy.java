package io.bitcoinsv.bitcoinjsv.pow_legacy.rule;

import io.bitcoinsv.bitcoinjsv.chain_legacy.StoredBlock_legacy;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Block;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.pow_legacy.AbstractPowRulesChecker_legacy;
import io.bitcoinsv.bitcoinjsv.store_legacy.BlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.core.Verification;

import java.math.BigInteger;

public class DifficultyTransitionPointRuleChecker_legacy extends AbstractPowRulesChecker_legacy {

    public DifficultyTransitionPointRuleChecker_legacy(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(StoredBlock_legacy storedPrev, Block nextBlock, BlockStore_legacy blockStore) throws VerificationException, BlockStoreException {

        Block prevBlock = storedPrev.getHeader();

        Block lastBlockInterval = findLastBlockInterval(prevBlock, blockStore);
        int timeSpan = (int) (prevBlock.getTime() - lastBlockInterval.getTime());
        BigInteger newTarget = calculateNewTarget(prevBlock, timeSpan);

        Verification.verifyDifficulty(networkParameters, newTarget, nextBlock);
    }

    private Block findLastBlockInterval(Block prevBlock, BlockStore_legacy blockStore) throws BlockStoreException {
        StoredBlock_legacy cursor = blockStore.get(prevBlock.getHash());
        for (int i = 0; i < networkParameters.getInterval() - 1; i++) {
            if (cursor == null) {
                throw new VerificationException("Difficulty transition point but we did " +
                        "not find a way back to the genesis block.");
            }
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }
        return cursor.getHeader();
    }

    private BigInteger calculateNewTarget(Block prevBlock, int timeSpan) {
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
