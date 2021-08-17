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

package io.bitcoinsv.bitcoinjsv.store;

import io.bitcoinsv.bitcoinjsv.blockstore.SPVBlockStore;
import io.bitcoinsv.bitcoinjsv.core.Address;
import io.bitcoinsv.bitcoinjsv.core.ECKey;
import io.bitcoinsv.bitcoinjsv.bitcoin.Genesis;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.params.UnitTestParams;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class SPVBlockStoreTest {

    @Test
    public void basics() throws Exception {
        NetworkParameters params = UnitTestParams.get();
        File f = File.createTempFile("spvblockstore", null);
        f.delete();
        f.deleteOnExit();
        SPVBlockStore store = new SPVBlockStore(params, f);

        Address to = new ECKey().toAddress(params);
        // Check the first block in a new store is the genesis block.
        LiteBlock genesis = store.getChainHead();
        assertEquals(Genesis.getFor(params.getNet()), genesis.getHeader());
        assertEquals(0, genesis.getHeight());


        // Build a new block.
//        LiteBlock b1 = genesis.build(genesis.getHeader().createNextBlock(to).cloneAsHeader());
//        store.put(b1);
//        store.setChainHead(b1);
//        store.close();

        // Check we can get it back out again if we rebuild the store object.
//        store = new SPVBlockStore(params, f);
//        LiteBlock b2 = store.get(b1.getHeader().getHash());
//        assertEquals(b1, b2);
//        // Check the chain head was stored correctly also.
//        LiteBlock chainHead = store.getChainHead();
//        assertEquals(b1, chainHead);
    }
}
