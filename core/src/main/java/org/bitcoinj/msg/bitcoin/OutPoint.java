package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Sha256Hash;

public interface OutPoint extends BitcoinObject {
    Sha256Hash getHash();

    void setHash(Sha256Hash hash);

    long getIndex();

    void setIndex(long index);

}
