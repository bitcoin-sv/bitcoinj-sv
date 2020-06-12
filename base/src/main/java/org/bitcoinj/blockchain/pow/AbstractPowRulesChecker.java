package org.bitcoinj.blockchain.pow;

import org.bitcoinj.blockstore.BlockStore;
import org.bitcoinj.core.Utils;
import org.bitcoinj.exception.BlockStoreException;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.bitcoin.api.base.Header;
import org.bitcoinj.msg.bitcoin.api.extended.LiteBlock;
import org.bitcoinj.params.NetworkParameters;

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
