package io.bitcoinsv.bitcoinjsv.pow_legacy;

import io.bitcoinsv.bitcoinjsv.chain_legacy.StoredBlock_legacy;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Block;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.store_legacy.BlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.core.Utils;

import java.math.BigInteger;

public abstract class AbstractPowRulesChecker_legacy {

    protected NetworkParameters networkParameters;

    public AbstractPowRulesChecker_legacy(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
    }

    public abstract void checkRules(StoredBlock_legacy storedPrev, Block nextBlock, BlockStore_legacy blockStore) throws VerificationException, BlockStoreException;

    public static boolean hasEqualDifficulty(Block prevBlock, Block nextBlock) {
        return prevBlock.getDifficultyTarget() == nextBlock.getDifficultyTarget();
    }

    public static boolean hasEqualDifficulty(long a, BigInteger b) {
        return a == Utils.encodeCompactBits(b);
    }

}
