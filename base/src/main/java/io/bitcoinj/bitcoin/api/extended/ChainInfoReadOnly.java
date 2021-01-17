/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.bitcoin.api.extended;

import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;

import java.math.BigInteger;

public interface ChainInfoReadOnly {

    HeaderReadOnly getHeader();

    BigInteger getChainWork();

    int getHeight();

    long getTotalChainTxs();
}
