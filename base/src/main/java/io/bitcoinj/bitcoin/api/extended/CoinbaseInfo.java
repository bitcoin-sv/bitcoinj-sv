/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.bitcoin.api.extended;

import io.bitcoinj.bitcoin.api.BitcoinObject;
import io.bitcoinj.bitcoin.api.base.Hashable;
import io.bitcoinj.bitcoin.api.base.Tx;

public interface CoinbaseInfo<C extends BitcoinObject> extends Hashable<C> {

    Tx getCoinbase();

    void setCoinbase(Tx coinbase);

    Object getMerkleProof();

    void setMerkleProof(Object merkleProof);

    Object getTxCountProof();

    void setTxCountProof(Object txCountProof);
}
