package io.bitcoinj.blockchain;

import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.blockstore.SPVBlockStore;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.PrunedException;
import io.bitcoinj.params.UnitTestParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.utils.TestBlockGenerator;

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
    UnitTestParams unitTestParams = UnitTestParams.get();

    @BeforeEach
    public void init() throws IOException, BlockStoreException {
        blockchainDataFile = File.createTempFile("testblockstore", null);
        blockchainDataFile.delete();
        blockchainDataFile.deleteOnExit();
        blockChain = new SPVBlockChain(unitTestParams, new SPVBlockStore(unitTestParams, blockchainDataFile));
    }

    @Test
    public void testRollbackBlockStore() throws PrunedException, BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(unitTestParams.getNet());
        LiteBlock blockOne = TestBlockGenerator.nextLiteBlock(unitTestParams.getNet(), genesisBlock);
        LiteBlock blockTwo = TestBlockGenerator.nextLiteBlock(unitTestParams.getNet(), blockOne);

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
        LiteBlock genesisBlock = Genesis.getHeaderFor(unitTestParams.getNet());
        LiteBlock blockOne = TestBlockGenerator.nextLiteBlock(unitTestParams.getNet(), genesisBlock);

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

