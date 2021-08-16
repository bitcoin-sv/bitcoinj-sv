/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
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
