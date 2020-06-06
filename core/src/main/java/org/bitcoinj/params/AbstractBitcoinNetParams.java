/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.params;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bitcoinj.msg.BitcoinSerializer;

/**
 * Parameters for Bitcoin-like networks.
 */
public abstract class AbstractBitcoinNetParams extends NetworkParameters {
    /**
     * Scheme part for Bitcoin URIs.
     */
    public static final String BITCOIN_SCHEME = "bitcoin";

    private static final Logger log = LoggerFactory.getLogger(AbstractBitcoinNetParams.class);

    // Aug, 1 hard fork
    int uahfHeight = 478559;

    public AbstractBitcoinNetParams(Net net) {
        super(net);
    }

    @Override
    public Coin getMaxMoney() {
        return MAX_MONEY;
    }

    @Override
    public Coin getMinNonDustOutput() {
        return Coin.MIN_NONDUST_OUTPUT;
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        return new MonetaryFormat();
    }

    @Override
    public int getProtocolVersionNum(final ProtocolVersion version) {
        return version.getBitcoinProtocolVersion();
    }

    @Override
    public BitcoinSerializer getSerializer(boolean parseLazy, boolean parseRetain, boolean compactTransactionsInBlock) {
        return new BitcoinSerializer(this, parseLazy, parseRetain, compactTransactionsInBlock);
    }

    public BitcoinSerializer getSerializer(boolean parseLazy, boolean parseRetain) {
        return getSerializer(parseLazy, parseRetain, defaultSerializeMode.isCompactTransactionsInBlock());
    }

    @Override
    public String getUriScheme() {
        return BITCOIN_SCHEME;
    }

    @Override
    public boolean hasMaxMoney() {
        return true;
    }
}
