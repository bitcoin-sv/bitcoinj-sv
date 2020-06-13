/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.msg.bitcoin.api.extended;

import org.bitcoinj.msg.bitcoin.api.base.HeaderReadOnly;

import java.math.BigInteger;

public interface ChainInfoReadOnly {

    HeaderReadOnly getHeader();

    BigInteger getChainWork();

    int getHeight();
}
