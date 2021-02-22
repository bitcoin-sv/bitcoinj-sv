package test.utils;

import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.bitcoin.bean.base.HeaderBean;
import io.bitcoinj.bitcoin.bean.extended.LiteBlockBean;
import io.bitcoinj.blockchain.ChainUtils;
import io.bitcoinj.blockchain.ChainUtilsTest;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.Utils;
import io.bitcoinj.params.Net;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 19/02/2021
 */
public class ChainConstruct {

    public static LiteBlock nextLiteBlock(Net networkParams, LiteBlock block){
        LiteBlock nextBlock = new LiteBlockBean();
        nextBlock.setHeader(new HeaderBean(nextBlock));

        nextBlock.setNonce(new Random().nextLong());
        nextBlock.setMerkleRoot(Sha256Hash.ZERO_HASH);
        nextBlock.setDifficultyTarget(Utils.encodeCompactBits(networkParams.params().getMaxTarget()));
        nextBlock.setTime(block.getTime() + (TimeUnit.MINUTES.toSeconds(10)));
        nextBlock.setVersion(block.getVersion());
        nextBlock.setPrevBlockHash(block.getHash());
        nextBlock.setHash(nextBlock.calculateHash());

        nextBlock.solve(networkParams);

        nextBlock = ChainUtils.buildNextInChain(block, nextBlock);

        return nextBlock;
    }

    public static LiteBlock orphanBlock(Net networkParams) {
        LiteBlock nextBlock = new LiteBlockBean();
        nextBlock.setHeader(new HeaderBean(nextBlock));

        LiteBlock genesisBlock = Genesis.getHeaderFor(networkParams);

        nextBlock.setNonce(new Random().nextLong());
        nextBlock.setMerkleRoot(Sha256Hash.ZERO_HASH);
        nextBlock.setDifficultyTarget(Utils.encodeCompactBits(networkParams.params().getMaxTarget()));
        nextBlock.setTime(genesisBlock.getTime() + (TimeUnit.MINUTES.toSeconds(10)));
        nextBlock.setVersion(1);
        nextBlock.setPrevBlockHash(Sha256Hash.ZERO_HASH);
        nextBlock.setHash(nextBlock.calculateHash());

        nextBlock.solve(networkParams);

        nextBlock = ChainUtils.buildNextInChain(genesisBlock, nextBlock);

        return nextBlock;
    }


}
