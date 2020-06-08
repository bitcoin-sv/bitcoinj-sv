package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Sha256Hash;

public interface ReadableHeader {
    /**
     * gets the header hash, calculating it if necessary.  If the header is part of a mutable FullBlock
     * this make also trigger calculation of the merkle tree.
     * @return
     */
    Sha256Hash getHash();

    long getVersion();

    Sha256Hash getPrevBlockHash();

    Sha256Hash getMerkleRoot();

    long getTime();

    long getDifficultyTarget();

    long getNonce();
}
