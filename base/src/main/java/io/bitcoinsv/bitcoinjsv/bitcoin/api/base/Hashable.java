/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.base;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;

public interface Hashable<C extends BitcoinObject> extends BitcoinObject<C>, HashProvider {

    default public Sha256Hash calculateHash() {
        return Sha256Hash.wrapReversed(Sha256Hash.hashTwice(serialize()));
    }

    void clearHash();
}
