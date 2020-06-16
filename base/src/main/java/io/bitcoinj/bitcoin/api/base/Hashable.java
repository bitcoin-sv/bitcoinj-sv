/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.bitcoin.api.base;

import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.bitcoin.api.BitcoinObject;

public interface Hashable<C extends BitcoinObject> extends BitcoinObject<C>, HashProvider {

    default public Sha256Hash calculateHash() {
        return Sha256Hash.wrapReversed(Sha256Hash.hashTwice(serialize()));
    }

    void clearHash();
}
