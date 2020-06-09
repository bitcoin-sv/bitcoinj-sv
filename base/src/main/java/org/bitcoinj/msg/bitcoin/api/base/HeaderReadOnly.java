package org.bitcoinj.msg.bitcoin.api.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;

public interface HeaderReadOnly<C extends BitcoinObject> extends Hashable<C> {

    public static final int FIXED_MESSAGE_SIZE = 80;

    boolean hasBlockMetaData();

    FullBlock getBlock();

    long getVersion();

    Sha256Hash getPrevBlockHash();

    Sha256Hash getMerkleRoot();

    long getTime();

    long getDifficultyTarget();

    long getNonce();

    default int fixedSize() {
        return FIXED_MESSAGE_SIZE;
    }

}
