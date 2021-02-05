package io.bitcoinj.blockstore;/*
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



import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.base.*;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.bitcoin.bean.base.*;
import io.bitcoinj.bitcoin.bean.extended.ChainInfoBean;
import io.bitcoinj.core.*;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.params.UnitTestParams;
import io.bitcoinj.script.ScriptBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class SPVBlockStoreTest {

    @Test
    public void basics() throws Exception {
        NetworkParameters params = UnitTestParams.get();
        File file = File.createTempFile("spvblockstore", null);
        file.delete();
        file.deleteOnExit();
        SPVBlockStore store = new SPVBlockStore(params, file);

        // Check the first block in a new store is the genesis block.
        LiteBlock genesisBlock = store.getChainHead();
        assertEquals(Genesis.getFor(params.getNet()), genesisBlock.getHeader());
        assertEquals(0, genesisBlock.getHeight());


        // Create a new block to connect to genesis
        FullBlock connectedBlock = new FullBlockBean();

        // Connect the block
        connectedBlock.setPrevBlockHash(genesisBlock.getHash());
        connectedBlock.setDifficultyTarget(genesisBlock.getDifficultyTarget());
        connectedBlock.setTime(genesisBlock.getTime() + 1);
        connectedBlock.setVersion(genesisBlock.getVersion());
        connectedBlock.setNonce(genesisBlock.getNonce() + 1);
        connectedBlock.setMerkleRoot(genesisBlock.getMerkleRoot());
        connectedBlock.setHash(connectedBlock.calculateHash());

        // Adjust it's information reletive to chain
        ChainInfo chainInfo = new ChainInfoBean(connectedBlock.getHeader());
        chainInfo.setChainWork(genesisBlock.getChainWork().add(BigInteger.ONE));
        chainInfo.setHeight(genesisBlock.getHeight() + 1);
        chainInfo.setTotalChainTxs(1);

        //Tx receiving the coinbase
        ECKeyLite coinbasePubKey = new ECKeyLite();

        //A simply tx to keep asLiteBlock() satisfied when calculating merkle tree
        Tx connectedBlockCoinbase = new TxBean(connectedBlock);

        TxInput txInput = new TxInputBean(connectedBlockCoinbase);

        TxOutPoint txOutPoint = new TxOutPointBean(txInput);
        txOutPoint.setIndex(0);
        txOutPoint.setHash(Sha256Hash.ZERO_HASH);

        txInput.setOutpoint(txOutPoint);

        TxOutput txOutput = new TxOutputBean(connectedBlockCoinbase);
        txOutput.setScriptPubKey(ScriptBuilder.createOutputScript(coinbasePubKey));
        txOutput.setValue(Coin.FIFTY_COINS);

        connectedBlockCoinbase.setInputs(Arrays.asList(txInput));
        connectedBlockCoinbase.setOutputs(Arrays.asList(txOutput));

        connectedBlock.setTransactions(Arrays.asList(connectedBlockCoinbase));

        // We only store lite blocks
        LiteBlock connectedBlockLite = connectedBlock.asLiteBlock();
        // This block is now the header
        connectedBlockLite.setChainInfo(chainInfo);

        store.put(connectedBlockLite);
        store.setChainHead(connectedBlockLite);

        // Check we can get it back out again if we rebuild the store object.
        store.close();

        store = new SPVBlockStore(params, file);

        //check we can retrieve the newly connected block
        LiteBlock connectedBlockRetrieved = store.get(connectedBlock.getHash());
        assertEquals(connectedBlock, connectedBlockRetrieved);

        // Check the chain head was stored correctly also.
        LiteBlock connectedBlockChainHead = store.getChainHead();
        assertEquals(connectedBlock, connectedBlockChainHead);

    }
}
