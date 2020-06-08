package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Sha256Hash;

public interface MerkleRootProvider {

    Sha256Hash calculateMerkleRoot();
}
