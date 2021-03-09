package io.bitcoinj.blockstore;


import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.params.UnitTestParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.utils.ChainConstruct;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 09/03/2021
 */
public class MemoryBlockStoreTest {

    BlockStore blockStore;

    @BeforeEach
    public void init() throws IOException, BlockStoreException {
        blockStore = new MemoryBlockStore(UnitTestParams.get());
    }

    @Test
    public void testPutAndGet() throws BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockStore.getParams().getNet());

        assertTrue(blockStore.get(genesisBlock.getHash()).equals(genesisBlock));
    }

    @Test
    public void testGetChainHeader() throws BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockStore.getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockStore.getParams().getNet(), genesisBlock);

        blockStore.put(blockOne);

        blockStore.setChainHead(blockOne);

        assertTrue(blockStore.getChainHead().equals(blockOne));
    }

    @Test
    public void testClose() throws BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockStore.getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockStore.getParams().getNet(), genesisBlock);

        blockStore.put(blockOne);
        blockStore.close();

        assertThrows( BlockStoreException.class , () -> blockStore.get(genesisBlock.getHash()));

        blockStore = new MemoryBlockStore(UnitTestParams.get());

        assertNull(blockStore.get(blockOne.getHash()));
    }

    @Test
    public void testGetPrev() throws BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockStore.getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockStore.getParams().getNet(), genesisBlock);

        blockStore.put(blockOne);

        assertTrue(blockStore.getPrev(blockOne).equals(genesisBlock));
    }



}
