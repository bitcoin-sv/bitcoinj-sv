package io.bitcoinsv.bitcoinjsv.blockchain;

import io.bitcoinsv.bitcoinjsv.bitcoin.Genesis;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.blockstore.SPVBlockStore;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.exception.PrunedException;
import io.bitcoinsv.bitcoinjsv.params.UnitTestParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.utils.ChainConstruct;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 19/02/2021
 *
 * Test the individual areas of functionality of the AbstractBlockchain class. Each test will create, populate and reload the blockchain to ensure state for extra hardening.
 */
public class SPVBlockChainTest {

    File blockchainDataFile;
    SPVBlockChain blockChain;

    @BeforeEach
    public void init() throws IOException, BlockStoreException {
        blockchainDataFile = File.createTempFile("testblockstore", null);
        blockchainDataFile.delete();
        blockchainDataFile.deleteOnExit();
        blockChain = new SPVBlockChain(UnitTestParams.get(), new SPVBlockStore(UnitTestParams.get(), blockchainDataFile));
    }

    @Test
    public void testRollbackBlockStore() throws PrunedException, BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockChain.getBlockStore().getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);
        LiteBlock blockTwo = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), blockOne);

        blockChain.add(blockOne);
        blockChain.add(blockTwo);

        blockChain.rollbackBlockStore(1);

        reloadBlockchainWithCurrentstate();

        assertTrue(blockChain.getBestChainHeight() == 1, "unexpected blockchain height");

        assertThrows(IllegalArgumentException.class, () -> blockChain.rollbackBlockStore(55));
        assertThrows(IllegalArgumentException.class, () -> blockChain.rollbackBlockStore(-1));
    }

    @Test
    public void testGetStoredBlockInCurrentScope() throws PrunedException, BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockChain.getBlockStore().getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);

        blockChain.add(blockOne);

        LiteBlock block = blockChain.getStoredBlockInCurrentScope(blockOne.getHash());

        assertNotNull(block, "missing expected block");

        block = blockChain.getStoredBlockInCurrentScope(Sha256Hash.ZERO_HASH);

        assertNull(block, "unexpected block");
    }


    /*
     * Reloads a previously initialised and populated blockchain
     */
    private void reloadBlockchainWithCurrentstate() throws BlockStoreException {
        blockChain.getBlockStore().close();
        blockChain = new SPVBlockChain(UnitTestParams.get(), new SPVBlockStore(UnitTestParams.get(), blockchainDataFile));
    }

}

