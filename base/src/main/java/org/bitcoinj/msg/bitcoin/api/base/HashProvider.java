package org.bitcoinj.msg.bitcoin.api.base;

import org.bitcoinj.core.Sha256Hash;

public interface HashProvider {
    /**
     * gets the header hash, calculating it if necessary.  If the header is part of a mutable FullBlock
     * this make also trigger calculation of the merkle tree.
     * @return
     */
    Sha256Hash getHash();

    default String getHashAsString() {
        return String.valueOf(getHash());
    }
}
