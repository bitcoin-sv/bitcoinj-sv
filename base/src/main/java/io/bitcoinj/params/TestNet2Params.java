/*
 * Copyright 2013 Google Inc.
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

package io.bitcoinj.params;

import io.bitcoinj.core.Utils;

/**
 * Parameters for the old version 2 testnet. This is not useful to you - it exists only because some unit tests are
 * based on it.
 *
 *
 */
public class TestNet2Params extends AbstractBitcoinNetParams {
    public static final int TESTNET_MAJORITY_WINDOW = 100;
    public static final int TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 75;
    public static final int TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 51;

    public TestNet2Params(Net net) {
        super(net);
        id = ID_TESTNET;
        packetMagic = 0xdab5bffaL;
        oldPacketMagic = 0xfabfb5daL;
        port = 18333;
        addressHeader = 111;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(0x1d0fffffL);
        dumpedPrivateKeyHeader = 239;
        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 210000;
        dnsSeeds = null;
        addrSeeds = null;
        bip32HeaderPub = 0x043587CF;
        bip32HeaderPriv = 0x04358394;

        majorityEnforceBlockUpgrade = TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = TESTNET_MAJORITY_WINDOW;

        daaUpdateHeight = 1188697;
    }

    @Override
    protected void configureGenesis() {
        genesisDifficulty = 0x1d07fff8L;
        genesisTime = 1296688602L;
        genesisNonce = 384568319;
        genesisHash = "00000007199508e34a9ff81e6ec0c477a4cccff2a4767a8eee39c11db367b008";
   }

    private static TestNet2Params instance = new TestNet2Params(Net.TESTNET2);
    static {
        Net.register(instance.net, instance);}
    public static synchronized TestNet2Params get() {
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return null;
    }
}
