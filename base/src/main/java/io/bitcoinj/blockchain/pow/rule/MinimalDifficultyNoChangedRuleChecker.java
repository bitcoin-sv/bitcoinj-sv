package io.bitcoinj.blockchain.pow.rule;

import io.bitcoinj.blockchain.pow.AbstractPowRulesChecker;
import io.bitcoinj.blockstore.BlockStore;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

import java.math.BigInteger;

public class MinimalDifficultyNoChangedRuleChecker extends AbstractPowRulesChecker {

    public MinimalDifficultyNoChangedRuleChecker(NetworkParameters networkParameters) {
        super(networkParameters);
    }

    @Override
    public void checkRules(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore blockStore) throws VerificationException, BlockStoreException {
        LiteBlock prevBlock = storedPrev;
        BigInteger minDifficulty = networkParameters.getMaxTarget();

        if (hasEqualDifficulty(prevBlock.getDifficultyTarget(), minDifficulty)) {
            if (!hasEqualDifficulty(prevBlock, nextBlock)) {
                throw new VerificationException("Unexpected change in difficulty at height " +
                        storedPrev.getChainInfo().getHeight() + ": " +
                        Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(prevBlock.getDifficultyTarget()));
            }
        }
    }

}
