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


import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Network parameters used by the bitcoinj unit tests (and potentially your own). This lets you solve a block using
 * { Block#solve()} by setting difficulty to the easiest possible.
 */
public class UnitTestParams extends AbstractBitcoinNetParams {
    public static final int UNITNET_MAJORITY_WINDOW = 8;
    public static final int TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 6;
    public static final int TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 4;

    public UnitTestParams(Net net) {
        super(net);
        id = ID_UNITTESTNET;
        packetMagic = 0xf4e5f3f4L;      // must be same as testnet3
        addressHeader = 111;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        maxTarget = new BigInteger("00ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
        port = 18333;
        interval = 10;
        dumpedPrivateKeyHeader = 239;
        targetTimespan = 200000000;  // 6 years. Just a very big number.
        spendableCoinbaseDepth = 5;
        subsidyDecreaseBlockCount = 100;
        dnsSeeds = null;
        addrSeeds = null;
        bip32HeaderPub = 0x043587CF;
        bip32HeaderPriv = 0x04358394;

        majorityEnforceBlockUpgrade = 3;
        majorityRejectBlockOutdated = 4;
        majorityWindow = 7;

        // support for legacy tests - dont test BCH 2017-11-13 hardfork by default
        daaUpdateHeight = 1000000;
    }

    @Override
    protected void configureGenesis() {
        genesisDifficulty = EASIEST_DIFFICULTY_TARGET;
        genesisTime = System.currentTimeMillis() / 1000;
        genesisNonce = 0;
        genesisHash = "unknown";
    }

    private static UnitTestParams instance = new UnitTestParams(Net.UNITTEST);
    static {
        Net.register(instance.net, instance);}
    public static synchronized UnitTestParams get() {
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return "unittest";
    }
}
