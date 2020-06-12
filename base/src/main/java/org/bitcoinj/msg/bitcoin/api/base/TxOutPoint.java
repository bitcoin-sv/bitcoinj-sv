package org.bitcoinj.msg.bitcoin.api.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;

public interface TxOutPoint extends BitcoinObject<TxOutPoint>, HashProvider {

    public static final int FIXED_MESSAGE_SIZE = 36;

    /** index used by genesis block coinbase input */
    public static final long UNCONNECTED = 0xFFFFFFFFL;

    Sha256Hash getHash();

    void setHash(Sha256Hash hash);

    long getIndex();

    void setIndex(long index);

    default int fixedSize() {
        return FIXED_MESSAGE_SIZE;
    }

}
