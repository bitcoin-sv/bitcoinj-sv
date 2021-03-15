package io.bitcoinj.blockstore;

import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.params.UnitTestParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.utils.ChainConstruct;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 15/03/2021
 */
public class FullHeadersBlockStoreTest {
    File blockchainDataFile;
    FullHeadersBlockStore blockStore;

    @BeforeEach
    public void init() throws IOException, BlockStoreException {
        blockchainDataFile = File.createTempFile("testblockstore", null);
        blockchainDataFile.delete();
        blockchainDataFile.deleteOnExit();
        blockStore = new FullHeadersBlockStore(UnitTestParams.get().getNet(), blockchainDataFile, true, true, true);
    }

    @Test
    public void testPruneFullChain() throws BlockStoreException, IOException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockStore.getParams().getNet());

        blockStore.put(genesisBlock);

        //   reloadBlockchainWithCurrentstate(); TODO blockstore is not currentl writing to disk

        blockStore.pruneFullBlockStore(0);

        assertNotNull(blockStore.get(genesisBlock.getHash()));
    }

    @Test
    public void testPutAndGet() throws BlockStoreException, IOException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockStore.getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockStore.getParams().getNet(), genesisBlock);

        blockStore.put(genesisBlock);
        blockStore.put(blockOne);

        //  reloadBlockchainWithCurrentstate();

        assertTrue(blockStore.get(blockOne.getHash()).equals(blockOne));
    }

    @Test
    public void testGetChainHeader() throws BlockStoreException, IOException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockStore.getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockStore.getParams().getNet(), genesisBlock);

        blockStore.put(genesisBlock);
        blockStore.put(blockOne);

        blockStore.setChainHead(blockOne);

        //reloadBlockchainWithCurrentstate();

        assertTrue(blockStore.getChainHead().equals(blockOne));
    }

    /*
     * Reloads a previously initialised and populated blockchain
     */
    private void reloadBlockchainWithCurrentstate() throws BlockStoreException, IOException {
        blockStore.close();
        blockStore = new FullHeadersBlockStore(UnitTestParams.get().getNet(), blockchainDataFile, true, false, true);
    }
}
