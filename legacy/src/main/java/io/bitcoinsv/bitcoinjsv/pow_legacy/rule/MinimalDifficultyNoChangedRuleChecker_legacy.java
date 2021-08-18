package io.bitcoinsv.bitcoinjsv.pow_legacy.rule;

import io.bitcoinsv.bitcoinjsv.chain_legacy.StoredBlock_legacy;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Block;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.pow_legacy.AbstractPowRulesChecker_legacy;
import io.bitcoinsv.bitcoinjsv.store_legacy.BlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;

import java.math.BigInteger;

public class MinimalDifficultyNoChangedRuleChecker_legacy extends AbstractPowRulesChecker_legacy {

    public MinimalDifficultyNoChangedRuleChecker_legacy(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(StoredBlock_legacy storedPrev, Block nextBlock, BlockStore_legacy blockStore) throws VerificationException, BlockStoreException {
        Block prevBlock = storedPrev.getHeader();
        BigInteger minDifficulty = networkParameters.getMaxTarget();

        if (hasEqualDifficulty(prevBlock.getDifficultyTarget(), minDifficulty)) {
            if (!hasEqualDifficulty(prevBlock, nextBlock)) {
                throw new VerificationException("Unexpected change in difficulty at height " +
                        storedPrev.getHeight() + ": " +
                        Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(prevBlock.getDifficultyTarget()));
            }
        }
    }

}
