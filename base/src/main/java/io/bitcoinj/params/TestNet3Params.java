/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
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
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
public class TestNet3Params extends AbstractBitcoinNetParams {
    public TestNet3Params(Net net) {
        super(net);
        id = ID_TESTNET;
        // Genesis hash is 000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943
        packetMagic = 0xf4e5f3f4L;
        oldPacketMagic = 0x0b110907;
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(0x1d00ffffL);
        port = 18333;
        addressHeader = 111;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        dumpedPrivateKeyHeader = 239;
        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 210000;
        alertSigningKey = Utils.HEX.decode("04302390343f91cc401d56d68b123028bf52e5fca1939df127f63c6467cdf9c8e2c14b61104cf817d0b780da337893ecc4aaff1309e536162dabbdb45200ca2b0a");

        dnsSeeds = new String[] {
                "testnet-seed.bitcoinsv.io",
                "testnet-seed.cascharia.com",
                "testnet-seed.bitcoincloud.net"
        };
        addrSeeds = null;
        bip32HeaderPub = 0x043587CF;
        bip32HeaderPriv = 0x04358394;

        majorityEnforceBlockUpgrade = TestNet2Params.TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = TestNet2Params.TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = TestNet2Params.TESTNET_MAJORITY_WINDOW;

        // Aug, 1 hard fork
        uahfHeight = 1155876;
        daaUpdateHeight = 1188697;
    }

    @Override
    protected void configureGenesis() {
        genesisDifficulty = 0x1d00ffffL;
        genesisTime = 1296688602L;
        genesisNonce = 414098458;
        genesisHash = "000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943";
    }

    private static TestNet3Params instance = new TestNet3Params(Net.TESTNET3);
    static {
        Net.register(instance.net, instance);}
    public static synchronized TestNet3Params get() {
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }

    //    @Override
//    protected void checkNextCashWorkRequired(StoredBlock storedPrev,
//                                             Block newBlock, BlockStore blockStore) {
//        // This cannot handle the genesis block and early blocks in general.
//        //assert(pindexPrev);
//
//
//
//        // Compute the difficulty based on the full adjustement interval.
//        int height = storedPrev.getHeight();
//        Preconditions.checkState(height >= this.interval);
//
//        // Get the last suitable block of the difficulty interval.
//        try {
//
//            // Special difficulty rule for testnet:
//            // If the new block's timestamp is more than 2* 10 minutes then allow
//            // mining of a min-difficulty block.
//
//            Block prev = storedPrev.getHeader();
//
//            final long timeDelta = newBlock.getTimeSeconds() - prev.getTimeSeconds();
//            if (timeDelta >= 0 && timeDelta > NetworkParameters.TARGET_SPACING * 2) {
//                if (!maxTarget.equals(newBlock.getDifficultyTargetAsInteger()))
//                    throw new VerificationException("Testnet block transition that is not allowed: " +
//                            Long.toHexString(Utils.encodeCompactBits(maxTarget)) + " (required min difficulty) vs " +
//                            Long.toHexString(newBlock.getDifficultyTarget()));
//                return;
//            }
//
//            StoredBlock lastBlock = GetSuitableBlock(storedPrev, blockStore);
//
//            // Get the first suitable block of the difficulty interval.
//            StoredBlock firstBlock = storedPrev;
//
//            for (int i = 144; i > 0; --i)
//            {
//                firstBlock = firstBlock.getPrev(blockStore);
//                if(firstBlock == null)
//                    return;
//            }
//
//            firstBlock = GetSuitableBlock(firstBlock, blockStore);
//
//            // Compute the target based on time and work done during the interval.
//            BigInteger nextTarget =
//                    ComputeTarget(firstBlock, lastBlock);
//
//            Verification.verifyDifficulty(this, nextTarget, newBlock);
//        }
//        catch (BlockStoreException x)
//        {
//            //this means we don't have enough blocks, yet.  let it go until we do.
//            return;
//        }
//    }

}
