/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.extended;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;

public interface BlockMeta<C extends BitcoinObject> extends BitcoinObject<C> {

    public static final int FIXED_MESSAGE_SIZE = 4 + 8;

    int getTxCount();

    void setTxCount(int txCount);

    long getBlockSize();

    void setBlockSize(long blockSize);

    @Override
    default int fixedSize() {
        return FIXED_MESSAGE_SIZE;
    }
}
