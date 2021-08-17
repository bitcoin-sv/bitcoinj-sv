/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.extended;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Hashable;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;

public interface CoinbaseInfo<C extends BitcoinObject> extends Hashable<C> {

    Tx getCoinbase();

    void setCoinbase(Tx coinbase);

    Object getMerkleProof();

    void setMerkleProof(Object merkleProof);

    Object getTxCountProof();

    void setTxCountProof(Object txCountProof);
}
