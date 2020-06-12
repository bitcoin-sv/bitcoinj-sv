package org.bitcoinj.msg.bitcoin.api.extended;

import org.bitcoinj.msg.bitcoin.api.BitcoinObject;
import org.bitcoinj.msg.bitcoin.api.base.Hashable;
import org.bitcoinj.msg.bitcoin.api.base.Tx;

public interface CoinbaseInfo<C extends BitcoinObject> extends Hashable<C> {

    Tx getCoinbase();

    void setCoinbase(Tx coinbase);

    Object getMerkleProof();

    void setMerkleProof(Object merkleProof);

    Object getTxCountProof();

    void setTxCountProof(Object txCountProof);
}
