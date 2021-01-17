package io.bitcoinj.pow_legacy.rule;

import io.bitcoinj.chain_legacy.StoredBlock_legacy;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.msg.protocol.Block;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.pow_legacy.AbstractPowRulesChecker_legacy;
import io.bitcoinj.store_legacy.BlockStore_legacy;
import io.bitcoinj.exception.BlockStoreException;

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
