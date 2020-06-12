package org.bitcoinj.pow_legacy;

import org.bitcoinj.chain_legacy.StoredBlock_legacy;
import org.bitcoinj.core.*;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.store.BlockStore_legacy;
import org.bitcoinj.exception.BlockStoreException;

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
