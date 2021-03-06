/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.bean.extended;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.CoinbaseInfo;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HashableImpl;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxBean;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Coinbase transaction plus merkle proofs.  We don't add this to LiteBlock because it's variable length
 * and because the coinbase data probably isn't useful for every type of light client, it almost triples the storage
 * requirement and may get larger in future with growing miner id coinbase documents.
 */
public class CoinbaseInfoBean extends HashableImpl<CoinbaseInfo> implements CoinbaseInfo<CoinbaseInfo> {

    private Tx coinbase;
    private Object merkleProof;
    private Object txCountProof;

    public CoinbaseInfoBean(BitcoinObject parent, byte[] payload, int offset) {
        super(parent, payload, offset);
    }

    public CoinbaseInfoBean(BitcoinObject parent) {
        super(parent);
    }

    public CoinbaseInfoBean(BitcoinObject parent, InputStream in) {
        super(parent, in);
    }

    @Override
    public Tx getCoinbase() {
        return coinbase;
    }

    @Override
    public void setCoinbase(Tx coinbase) {
        this.coinbase = coinbase;
    }

    @Override
    public Object getMerkleProof() {
        return merkleProof;
    }

    @Override
    public void setMerkleProof(Object merkleProof) {
        this.merkleProof = merkleProof;
    }

    @Override
    public Object getTxCountProof() {
        return txCountProof;
    }

    @Override
    public void setTxCountProof(Object txCountProof) {
        this.txCountProof = txCountProof;
    }

    @Override
    protected void parse() {
        coinbase = new TxBean(this, payload, cursor);
        cursor += coinbase.getMessageSize();
        //TODO add merkle proofs
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        coinbase.serializeTo(stream);
        //TODO add merkle proofs
    }

    @Override
    public CoinbaseInfo makeNew(byte[] serialized) {
        return new CoinbaseInfoBean(null, serialized, 0);
    }
}
