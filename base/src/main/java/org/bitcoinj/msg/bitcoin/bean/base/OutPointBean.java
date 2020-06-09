package org.bitcoinj.msg.bitcoin.bean.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.msg.bitcoin.api.base.Input;
import org.bitcoinj.msg.bitcoin.api.base.OutPoint;
import org.bitcoinj.msg.bitcoin.bean.BitcoinObjectImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OutPointBean extends BitcoinObjectImpl<OutPoint> implements OutPoint {

    /** Hash of the transaction to which we refer. */
    private Sha256Hash hash;
    /** Which output of that transaction we are talking about. */
    private long index;

    public OutPointBean(Input input, byte[] payload, int offset) {
        super(input, payload, offset);
    }

    public OutPointBean(byte[] payload) {
        super(null, payload, 0);
    }

    public OutPointBean(Input inputBean, InputStream in) {
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
    public OutPoint makeNew(byte[] serialized) {
        return new OutPointBean(serialized);
    }
}
