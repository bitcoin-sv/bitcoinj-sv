package org.bitcoinj.pow_legacy.rule;

import org.bitcoinj.chain_legacy.StoredBlock_legacy;
import org.bitcoinj.core.*;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.pow_legacy.AbstractPowRulesChecker_legacy;
import org.bitcoinj.store_legacy.BlockStore_legacy;
import org.bitcoinj.exception.BlockStoreException;

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
