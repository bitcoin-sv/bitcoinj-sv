package io.bitcoinsv.bitcoinjsv.blockchain;

import io.bitcoinsv.bitcoinjsv.bitcoin.Genesis;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.blockstore.SPVBlockStore;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.exception.PrunedException;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.params.UnitTestParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.utils.TestBlockGenerator;

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
    NetworkParameters params = UnitTestParams.get();

    @BeforeEach
    public void init() throws IOException, BlockStoreException {
        blockchainDataFile = File.createTempFile("testblockstore", null);
        blockchainDataFile.delete();
        blockchainDataFile.deleteOnExit();
        blockChain = new SPVBlockChain(params, new SPVBlockStore(params, blockchainDataFile));
    }


    @Test
    public void testIsMoreWorkThan() throws PrunedException, BlockStoreException, IOException {
        LiteBlock genesisBlock = Genesis.getHeaderFor(params.getNet());
        LiteBlock blockOneChainOne = TestBlockGenerator.nextLiteBlock(params.getNet(), genesisBlock);
        LiteBlock blockOneChainTwo = TestBlockGenerator.nextLiteBlock(params.getNet(), genesisBlock);

        blockOneChainOne.getChainInfo().setChainWork(BigInteger.ONE);
        blockOneChainTwo.getChainInfo().setChainWork(BigInteger.TWO);

        assertTrue(ChainUtils.isMoreWorkThan(blockOneChainTwo, blockOneChainOne), "chain two should have more work");
    }
    
}
