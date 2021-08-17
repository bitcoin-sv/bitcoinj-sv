package io.bitcoinsv.bitcoinjsv.blockstore;

import io.bitcoinsv.bitcoinjsv.bitcoin.Genesis;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.FullBlock;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.params.UnitTestParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.utils.ChainConstruct;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 15/03/2021
 */
public class FullBlockStoreTest {

    static File blockchainDataFile;
    static FullBlockStore blockStore;

    @BeforeAll
    public static void init() throws IOException, BlockStoreException {
        if(FullBlockStore.get() == null) {
            blockchainDataFile = File.createTempFile("testblockstore", null);
            blockchainDataFile.delete();
            blockchainDataFile.deleteOnExit();

            blockStore = new FullBlockStore(blockchainDataFile, UnitTestParams.get().getNet().params(), false);
        }
    }


    @Test
    public void testPutAndGet() {
        FullBlock genesisBlock = Genesis.getFor(UnitTestParams.get().getNet());
        FullBlock blockOne = ChainConstruct.nextFullBlock(UnitTestParams.get().getNet(), genesisBlock, true);

        blockStore.putBlock(genesisBlock);
        blockStore.putBlock(blockOne);

        assertTrue(blockStore.loadBlock(blockOne.getHash()).equals(blockOne));
    }

    @Test
    public void testDelete() {
        FullBlock genesisBlock = Genesis.getFor(UnitTestParams.get().getNet());
        FullBlock blockOne = ChainConstruct.nextFullBlock(UnitTestParams.get().getNet(), genesisBlock, true);

        blockStore.putBlock(genesisBlock);
        blockStore.putBlock(blockOne);

        blockStore.deleteBlock(blockOne.getHash());

        assertNull(blockStore.loadBlock(blockOne.getHash()));
    }

    @Test
    public void testHasBlock() {
        FullBlock genesisBlock = Genesis.getFor(UnitTestParams.get().getNet());
        FullBlock blockOne = ChainConstruct.nextFullBlock(UnitTestParams.get().getNet(), genesisBlock, true);

        blockStore.putBlock(genesisBlock);
        blockStore.putBlock(blockOne);

        assertTrue(blockStore.hasBlock(genesisBlock.getHash()));
    }

    /*
     * Reloads a previously initialised and populated blockchain
     */
    private void reloadBlockchainWithCurrentstate() throws BlockStoreException, IOException {
        blockStore = null;
        blockStore = new FullBlockStore(blockchainDataFile, UnitTestParams.get().getNet().params(), false);
    }

}
