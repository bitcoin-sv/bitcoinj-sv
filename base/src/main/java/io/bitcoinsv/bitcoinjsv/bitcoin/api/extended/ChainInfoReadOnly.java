/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.extended;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;

import java.math.BigInteger;

public interface ChainInfoReadOnly {

    HeaderReadOnly getHeader();

    BigInteger getChainWork();

    int getHeight();

    long getTotalChainTxs();
}
