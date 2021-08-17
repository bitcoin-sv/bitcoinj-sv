/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.bean.base;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Hashable;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.BitcoinObjectImpl;

import java.io.InputStream;
import java.util.Objects;

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

    @Override
    public void clearHash() {
        hash = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hashable)) return false;
        Hashable hashable = (Hashable) o;
        return getHash().equals(hashable.getHash());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHash());
    }
}
