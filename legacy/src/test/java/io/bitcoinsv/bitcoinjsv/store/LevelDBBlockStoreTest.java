/*
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

package io.bitcoinsv.bitcoinjsv.store;

import io.bitcoinsv.bitcoinjsv.chain_legacy.StoredBlock_legacy;
import io.bitcoinsv.bitcoinjsv.core.Address;
import io.bitcoinsv.bitcoinjsv.msg.Genesis_legacy;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.params.UnitTestParams;
import io.bitcoinsv.bitcoinjsv.store_legacy.LevelDBBlockStore;
import org.junit.*;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class LevelDBBlockStoreTest {
    @Test
    public void basics() throws Exception {
        File f = File.createTempFile("leveldbblockstore", null);
        f.delete();

        NetworkParameters params = UnitTestParams.get();
        LevelDBBlockStore store = new LevelDBBlockStore(params, f);
        store.reset();

        // Check the first block in a new store is the genesis block.
        StoredBlock_legacy genesis = store.getChainHead();
        assertEquals(Genesis_legacy.getFor(params), genesis.getHeader());
        assertEquals(0, genesis.getHeight());

        // Build a new block.
        Address to = Address.fromBase58(params, "mrj2K6txjo2QBcSmuAzHj4nD1oXSEJE1Qo");
        StoredBlock_legacy b1 = genesis.build(genesis.getHeader().createNextBlock(to).cloneAsHeader());
        store.put(b1);
        store.setChainHead(b1);
        store.close();

        // Check we can get it back out again if we rebuild the store object.
        store = new LevelDBBlockStore(params, f);
        try {
            StoredBlock_legacy b2 = store.get(b1.getHeader().getHash());
            assertEquals(b1, b2);
            // Check the chain head was stored correctly also.
            StoredBlock_legacy chainHead = store.getChainHead();
            assertEquals(b1, chainHead);
        } finally {
            store.close();
            store.destroy();
        }
    }
}