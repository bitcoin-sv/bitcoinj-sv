/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.msg.bitcoin.bean.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.msg.bitcoin.api.base.TxInput;
import org.bitcoinj.msg.bitcoin.api.base.TxOutPoint;
import org.bitcoinj.msg.bitcoin.bean.BitcoinObjectImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TxOutPointBean extends BitcoinObjectImpl<TxOutPoint> implements TxOutPoint {

    /** Hash of the transaction to which we refer. */
    private Sha256Hash hash;
    /** Which output of that transaction we are talking about. */
    private long index;

    public TxOutPointBean(TxInput input, byte[] payload, int offset) {
        super(input, payload, offset);
    }

    public TxOutPointBean(byte[] payload) {
        super(null, payload, 0);
    }

    /**
     * Constructor for building manually
     * @param parent
     */
    public TxOutPointBean(TxInput parent) {
        super(parent);
    }

    public TxOutPointBean(TxInput inputBean, InputStream in) {
        super(inputBean, in);
    }


    @Override
    public Sha256Hash getHash() {
        return hash;
    }

    @Override
    public void setHash(Sha256Hash hash) {
        checkMutable();
        this.hash = hash;
    }

    @Override
    public long getIndex() {
        return index;
    }

    @Override
    public void setIndex(long index) {
        checkMutable();
        this.index = index;
    }

    @Override
    protected void parse() {
        hash = readHash();
        index = readUint32();
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        stream.write(hash.getReversedBytes());
        Utils.uint32ToByteStreamLE(index, stream);
    }

    @Override
    public TxOutPoint makeNew(byte[] serialized) {
        return new TxOutPointBean(serialized);
    }
}
