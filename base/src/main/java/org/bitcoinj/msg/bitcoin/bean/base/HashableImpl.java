package org.bitcoinj.msg.bitcoin.bean.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;
import org.bitcoinj.msg.bitcoin.api.base.Hashable;
import org.bitcoinj.msg.bitcoin.bean.BitcoinObjectImpl;

import java.io.InputStream;

public abstract class HashableImpl<C extends Hashable> extends BitcoinObjectImpl<C> implements Hashable<C> {

    protected Sha256Hash hash;

    public HashableImpl(BitcoinObject parent, byte[] payload, int offset) {
        super(parent, payload, offset);
    }

    public HashableImpl(BitcoinObject parent) {
        super(parent);
    }

    public HashableImpl(BitcoinObject parent, InputStream in) {
        super(parent, in);
    }

    @Override
    public Sha256Hash getHash() {
        if (hash == null) {
            hash = calculateHash();
        }
        return hash;
    }

}
