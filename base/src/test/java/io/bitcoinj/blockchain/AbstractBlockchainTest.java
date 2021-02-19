package io.bitcoinj.blockchain;

import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.bitcoin.bean.extended.LiteBlockBean;
import io.bitcoinj.blockstore.BlockStore;
import io.bitcoinj.blockstore.SPVBlockStore;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.PrunedException;
import io.bitcoinj.params.NetworkParameters;
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
 * @date 19/02/2021
 *
 * Test the individual areas of functionality of the AbstractBlockchain class. Each test will create, populate and reload the blockchain to ensure state for extra hardening.
 */
public class AbstractBlockchainTest {

    File blockchainDataFile;
    AbstractBlockChain blockChain;


    @BeforeEach
    public void init() throws IOException, BlockStoreException {
        NetworkParameters params = UnitTestParams.get();
        blockchainDataFile = File.createTempFile("testblockstore", null);
        blockchainDataFile.delete();
        blockchainDataFile.deleteOnExit();
        blockChain = new SPVBlockChain(params, new SPVBlockStore(params, blockchainDataFile));
    }

    @Test
    public void testAddBlock() throws PrunedException, BlockStoreException, IOException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockChain.getBlockStore().getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);

        blockChain.add(blockOne);

        reloadBlockchainWithCurrentstate();

        assertNotNull(blockChain.getBlockStore().get(blockOne.getHash()), "block does not exist");
    }

    @Test
    public void testDrainOrphanBlocks() throws PrunedException, BlockStoreException, IOException {
        LiteBlock orhanBlock = ChainConstruct.orphanBlock(blockChain.getBlockStore().getParams().getNet());
        LiteBlock nextOrphanBlock = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), orhanBlock);

        blockChain.add(orhanBlock);
        blockChain.add(nextOrphanBlock);

        assertTrue(blockChain.isOrphan(orhanBlock.getHash()), "block has not been orphaned");
        assertTrue(blockChain.getOrphanRoot(nextOrphanBlock.getHash()).equals(orhanBlock), "incorrect orphan root");

        blockChain.drainOrphanBlocks();

        assertFalse(blockChain.isOrphan(orhanBlock.getHash()), "block is still an orphan");
    }

    @Test
    public void testBestChainHeight() throws PrunedException, BlockStoreException, IOException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockChain.getBlockStore().getParams().getNet());
        LiteBlock blockOneChainOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);
        LiteBlock blockTwoChainOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), blockOneChainOne);
        LiteBlock blockOneChainTwo = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);


        blockChain.add(blockOneChainOne);
        blockChain.add(blockTwoChainOne);
        blockChain.add(blockOneChainTwo);

        reloadBlockchainWithCurrentstate();

        assertTrue(blockChain.getBestChainHeight() == 2, "invalid chain height");
    }


    /*
     * Reloads a previously initialised and populated blockchain
     */
    private void reloadBlockchainWithCurrentstate() throws IOException, BlockStoreException {
        blockChain.getBlockStore().close();
        blockChain = new SPVBlockChain(UnitTestParams.get(), new SPVBlockStore(UnitTestParams.get(), blockchainDataFile));
    }

}

