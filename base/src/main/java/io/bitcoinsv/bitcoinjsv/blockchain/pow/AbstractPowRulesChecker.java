package io.bitcoinsv.bitcoinjsv.blockchain.pow;

import io.bitcoinsv.bitcoinjsv.blockstore.BlockStore;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Header;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;

import java.math.BigInteger;

public abstract class AbstractPowRulesChecker {

    protected NetworkParameters networkParameters;

    public AbstractPowRulesChecker(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
    }

    public abstract void checkRules(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore blockStore) throws VerificationException, BlockStoreException;

    public static boolean hasEqualDifficulty(Header prevBlock, Header nextBlock) {
        return prevBlock.getDifficultyTarget() == nextBlock.getDifficultyTarget();
    }

    public static boolean hasEqualDifficulty(long a, BigInteger b) {
        return a == Utils.encodeCompactBits(b);
    }

}
