/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.msg.bitcoin.api.extended;

import org.bitcoinj.msg.bitcoin.api.BitcoinObject;

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
