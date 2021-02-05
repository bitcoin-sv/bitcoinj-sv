/*
 * Copyright 2015 Ross Nicoll.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.bitcoinj.utils;

import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.bitcoin.bean.extended.LiteBlockBean;
import io.bitcoinj.blockchain.VersionTally;
import io.bitcoinj.blockstore.MemoryBlockStore;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.params.UnitTestParams;
import io.bitcoinj.exception.BlockStoreException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class VersionTallyTest {
    private static final NetworkParameters PARAMS = UnitTestParams.get();

    public VersionTallyTest() {
    }

    @BeforeAll
    public static void setUp() throws Exception {
        BriefLogFormatter.initVerbose();
    }

    /**
     * Verify that the version tally returns null until it collects enough data.
     */
    @Test
    public void testNullWhileEmpty() {
        VersionTally instance = new VersionTally(PARAMS);
        for (int i = 0; i < PARAMS.getMajorityWindow(); i++) {
            assertNull(instance.getCountAtOrAbove(1));
            instance.add(1);
        }
        assertEquals(PARAMS.getMajorityWindow(), instance.getCountAtOrAbove(1).intValue());
    }

    /**
     * Verify that the size of the version tally matches the network parameters.
     */
    @Test
    public void testSize() {
        VersionTally instance = new VersionTally(PARAMS);
        assertEquals(PARAMS.getMajorityWindow(), instance.size());
    }

    /**
     * Verify that version count and substitution works correctly.
     */
    @Test
    public void testVersionCounts() {
        VersionTally instance = new VersionTally(PARAMS);

        // Fill the tally with 1s
        for (int i = 0; i < PARAMS.getMajorityWindow(); i++) {
            assertNull(instance.getCountAtOrAbove(1));
            instance.add(1);
        }
        assertEquals(PARAMS.getMajorityWindow(), instance.getCountAtOrAbove(1).intValue());

        // Check the count updates as we replace with 2s
        for (int i = 0; i < PARAMS.getMajorityWindow(); i++) {
            assertEquals(i, instance.getCountAtOrAbove(2).intValue());
            instance.add(2);
        }
 
        // Inject a rogue 1
        instance.add(1);
        assertEquals(PARAMS.getMajorityWindow() - 1, instance.getCountAtOrAbove(2).intValue());

        // Check we accept high values as well
        instance.add(10);
        assertEquals(PARAMS.getMajorityWindow() - 1, instance.getCountAtOrAbove(2).intValue());
    }

    @Test
    public void testInitialize() throws BlockStoreException {
        final MemoryBlockStore blockStore = new MemoryBlockStore(PARAMS);

        LiteBlock checkPointBlock = Genesis.getHeaderFor(PARAMS.getNet());

        // Build a historical chain of version 2 blocks
        long timeSeconds = 1231006505;
        for (int height = 0; height < PARAMS.getMajorityWindow(); height++) {
            LiteBlock connectedBlock = new LiteBlockBean();

            // Connect the block
            connectedBlock.setPrevBlockHash(checkPointBlock.getHash());
            connectedBlock.setDifficultyTarget(checkPointBlock.getDifficultyTarget());
            connectedBlock.setTime(timeSeconds);
            connectedBlock.setVersion(2);
            connectedBlock.setNonce(checkPointBlock.getNonce() + 1);
            connectedBlock.setMerkleRoot(checkPointBlock.getMerkleRoot());
            connectedBlock.setHash(connectedBlock.calculateHash());

            blockStore.put(connectedBlock);
            blockStore.setChainHead(connectedBlock);

            checkPointBlock = connectedBlock;

            assertEquals(2, blockStore.getChainHead().getHeader().getVersion());
            timeSeconds += 60;
        }

        VersionTally instance = new VersionTally(PARAMS);
        instance.initialize(blockStore, checkPointBlock);
        assertEquals(PARAMS.getMajorityWindow(), instance.getCountAtOrAbove(2).intValue());
    }
}
