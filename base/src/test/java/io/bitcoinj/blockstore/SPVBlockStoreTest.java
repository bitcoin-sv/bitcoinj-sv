package io.bitcoinj.blockstore;


import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.blockchain.SPVBlockChain;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.params.UnitTestParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.utils.ChainConstruct;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 09/03/2021
 */
public class SPVBlockStoreTest {

    File blockchainDataFile;
    BlockStore blockStore;

    @BeforeEach
    public void init() throws IOException, BlockStoreException {
        blockchainDataFile = File.createTempFile("testblockstore", null);
        blockchainDataFile.delete();
        blockchainDataFile.deleteOnExit();
        blockStore = new SPVBlockStore(UnitTestParams.get(), blockchainDataFile);
    }

    @Test
    public void testPutAndGet() throws BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockStore.getParams().getNet());

        blockStore.put(genesisBlock);

        assertTrue(blockStore.get(genesisBlock.getHash()).equals(genesisBlock));
    }

    @Test
    public void testGetChainHeader() throws BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockStore.getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockStore.getParams().getNet(), genesisBlock);

        blockStore.put(genesisBlock);
        blockStore.put(blockOne);

        blockStore.setChainHead(blockOne);

        assertTrue(blockStore.getChainHead().equals(blockOne));
    }

    @Test
    public void testClose() throws BlockStoreException, IOException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockStore.getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockStore.getParams().getNet(), genesisBlock);

        blockStore.put(blockOne);
        blockStore.close();

        assertThrows( BlockStoreException.class , () -> blockStore.get(genesisBlock.getHash()));

        blockStore = new SPVBlockStore(UnitTestParams.get(), blockchainDataFile);

        assertNotNull(blockStore.get(blockOne.getHash()));
    }


}
