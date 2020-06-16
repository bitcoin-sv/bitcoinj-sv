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

package io.bitcoinj.store;

import io.bitcoinj.core.Address;
import io.bitcoinj.core.ECKey;
import io.bitcoinj.msg.Genesis_legacy;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.chain_legacy.StoredBlock_legacy;
import io.bitcoinj.params.UnitTestParams;
import io.bitcoinj.store_legacy.SPVBlockStore_legacy;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class SPVBlockStoreTest_legacy {

    @Test
    public void basics() throws Exception {
        NetworkParameters params = UnitTestParams.get();
        File f = File.createTempFile("spvblockstore", null);
        f.delete();
        f.deleteOnExit();
        SPVBlockStore_legacy store = new SPVBlockStore_legacy(params, f);

        Address to = new ECKey().toAddress(params);
        // Check the first block in a new store is the genesis block.
        StoredBlock_legacy genesis = store.getChainHead();
        assertEquals(Genesis_legacy.getFor(params), genesis.getHeader());
        assertEquals(0, genesis.getHeight());


        // Build a new block.
        StoredBlock_legacy b1 = genesis.build(genesis.getHeader().createNextBlock(to).cloneAsHeader());
        store.put(b1);
        store.setChainHead(b1);
        store.close();

        // Check we can get it back out again if we rebuild the store object.
        store = new SPVBlockStore_legacy(params, f);
        StoredBlock_legacy b2 = store.get(b1.getHeader().getHash());
        assertEquals(b1, b2);
        // Check the chain head was stored correctly also.
        StoredBlock_legacy chainHead = store.getChainHead();
        assertEquals(b1, chainHead);
    }
}
