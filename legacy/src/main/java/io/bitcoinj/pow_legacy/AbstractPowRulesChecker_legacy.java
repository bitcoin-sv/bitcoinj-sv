package io.bitcoinj.pow_legacy;

import io.bitcoinj.chain_legacy.StoredBlock_legacy;
import io.bitcoinj.core.*;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.msg.protocol.Block;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.store_legacy.BlockStore_legacy;
import io.bitcoinj.exception.BlockStoreException;

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
