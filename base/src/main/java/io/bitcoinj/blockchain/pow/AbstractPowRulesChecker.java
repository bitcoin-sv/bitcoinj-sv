package io.bitcoinj.blockchain.pow;

import io.bitcoinj.blockstore.BlockStore;
import io.bitcoinj.core.Utils;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.bitcoin.api.base.Header;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

import java.math.BigInteger;

public abstract class AbstractPowRulesChecker {

    protected NetworkParameters networkParameters;

    public AbstractPowRulesChecker(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
    }

    public abstract  void checkRules(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore<LiteBlock> blockStore) throws VerificationException, BlockStoreException;

    public static boolean hasEqualDifficulty(Header prevBlock, Header nextBlock) {
        return prevBlock.getDifficultyTarget() == nextBlock.getDifficultyTarget();
    }

    public static boolean hasEqualDifficulty(long a, BigInteger b) {
        return a == Utils.encodeCompactBits(b);
    }

}
