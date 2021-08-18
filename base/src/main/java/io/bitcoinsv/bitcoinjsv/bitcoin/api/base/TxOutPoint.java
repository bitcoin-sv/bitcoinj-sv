/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.base;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;

public interface TxOutPoint extends BitcoinObject<TxOutPoint>, HashProvider {

    public static final int FIXED_MESSAGE_SIZE = 36;

    /** index used by genesis block coinbase input */
    public static final long UNCONNECTED = 0xFFFFFFFFL;

    Sha256Hash getHash();

    void setHash(Sha256Hash hash);

    long getIndex();

    void setIndex(long index);

    default int fixedSize() {
        return FIXED_MESSAGE_SIZE;
    }

}
