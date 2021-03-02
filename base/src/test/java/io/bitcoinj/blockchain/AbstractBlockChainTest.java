package io.bitcoinj.blockchain;

import com.google.common.util.concurrent.ListenableFuture;
import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.blockstore.SPVBlockStore;
import io.bitcoinj.core.listeners.NewBestBlockListener;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.PrunedException;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.params.UnitTestParams;
import io.bitcoinj.utils.Threading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.utils.ChainConstruct;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 19/02/2021
 *
 * Test the individual areas of functionality of the AbstractBlockchain class. Each test will create, populate and reload the blockchain to ensure state for extra hardening.
 */
public class AbstractBlockChainTest {

    File blockchainDataFile;
    AbstractBlockChain blockChain;


    @BeforeEach
    public void init() throws IOException, BlockStoreException {
        blockchainDataFile = File.createTempFile("testblockstore", null);
        blockchainDataFile.delete();
        blockchainDataFile.deleteOnExit();
        blockChain = new SPVBlockChain(UnitTestParams.get(), new SPVBlockStore(UnitTestParams.get(), blockchainDataFile));
    }

    @Test
    public void testAddBlock() throws PrunedException, BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockChain.getBlockStore().getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);

        blockChain.add(blockOne);

        reloadBlockchainWithCurrentstate();

        assertNotNull(blockChain.getBlockStore().get(blockOne.getHash()), "block does not exist");
    }

    @Test
    public void testDrainOrphanBlocks() throws PrunedException {
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
    public void testBestChainHeight() throws PrunedException, BlockStoreException{
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

    @Test
    public void testBestchainHead() throws PrunedException, BlockStoreException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockChain.getBlockStore().getParams().getNet());
        LiteBlock blockOneChainOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);
        LiteBlock blockTwoChainOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), blockOneChainOne);
        LiteBlock blockOneChainTwo = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);


        blockChain.add(blockOneChainOne);
        blockChain.add(blockTwoChainOne);
        blockChain.add(blockOneChainTwo);

        blockChain.setChainHead(blockOneChainOne);

        reloadBlockchainWithCurrentstate();

        assertTrue(blockChain.getChainHead().equals(blockOneChainOne), "invalid chain height");
    }


    @Test
    public void testFutureHeightCallback() throws PrunedException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockChain.getBlockStore().getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);

        AtomicBoolean heightFutureTriggered = new AtomicBoolean();

        ListenableFuture<LiteBlock> blockHeightFuture = blockChain.getHeightFuture(1);
        blockHeightFuture.addListener(() -> heightFutureTriggered.set(true), Threading.SAME_THREAD);

        blockChain.add(blockOne);

        assertTrue(heightFutureTriggered.get(), "block height future function was not called back");
    }

    @Test
    public void testNewBestBlockListener() throws PrunedException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockChain.getBlockStore().getParams().getNet());
        LiteBlock blockOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);
        LiteBlock blockTwo = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), blockOne);

        AtomicBoolean newBestBlockTriggered = new AtomicBoolean();

        NewBestBlockListener newBestBlockListener = block -> newBestBlockTriggered.set(true);
        blockChain.addNewBestBlockListener(Threading.SAME_THREAD, newBestBlockListener);

        blockChain.add(blockOne);

        assertTrue(newBestBlockTriggered.get(), "new best block function was not called back");

        blockChain.removeNewBestBlockListener(newBestBlockListener);
        newBestBlockTriggered.set(false);

        blockChain.add(blockTwo);

        assertFalse(newBestBlockTriggered.get(), "newBestBlockTrigger should not be set as listener has been removed");
    }


    /*
     * Reloads a previously initialised and populated blockchain
     */
    private void reloadBlockchainWithCurrentstate() throws BlockStoreException {
        blockChain.getBlockStore().close();
        blockChain = new SPVBlockChain(UnitTestParams.get(), new SPVBlockStore(UnitTestParams.get(), blockchainDataFile));
    }

}

