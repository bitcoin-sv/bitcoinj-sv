package io.bitcoinj.blockchain;

import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
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
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 22/02/2021
 */
public class ChainUtilsTest {

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
    public void testIsMoreWorkThan() throws PrunedException, BlockStoreException, IOException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(blockChain.getBlockStore().getParams().getNet());
        LiteBlock blockOneChainOne = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);
        LiteBlock blockOneChainTwo = ChainConstruct.nextLiteBlock(blockChain.getBlockStore().getParams().getNet(), genesisBlock);

        blockOneChainOne.getChainInfo().setChainWork(BigInteger.ONE);
        blockOneChainTwo.getChainInfo().setChainWork(BigInteger.TWO);

        assertTrue(ChainUtils.isMoreWorkThan(blockOneChainTwo, blockOneChainOne), "chain two should have more work");
    }
    
}
