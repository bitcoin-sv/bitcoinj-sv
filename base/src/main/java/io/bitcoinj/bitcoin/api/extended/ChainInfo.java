/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.bitcoin.api.extended;

import io.bitcoinj.bitcoin.api.BitcoinObject;

import java.math.BigInteger;

public interface ChainInfo<C extends BitcoinObject> extends BitcoinObject<C>, ChainInfoReadOnly {

    public static final int CHAIN_WORK_BYTES = 12;

    public static final int FIXED_MESSAGE_SIZE = CHAIN_WORK_BYTES + 4 + 8;

    void setChainWork(BigInteger chainWork);

    void setHeight(int height);

    @Override
    default int fixedSize() {
        return FIXED_MESSAGE_SIZE;
    }

    void setTotalChainTxs(long totalChainTxs);
}
