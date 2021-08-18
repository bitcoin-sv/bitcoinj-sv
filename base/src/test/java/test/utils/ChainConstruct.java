package test.utils;

import io.bitcoinsv.bitcoinjsv.bitcoin.Genesis;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.*;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.*;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.extended.LiteBlockBean;
import io.bitcoinsv.bitcoinjsv.blockchain.ChainUtils;
import io.bitcoinsv.bitcoinjsv.core.Coin;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.params.Net;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 19/02/2021
 */
public class ChainConstruct {

    public static LiteBlock nextLiteBlock(Net networkParams, LiteBlock prevBlock){
        LiteBlock nextBlock = new LiteBlockBean();
        nextBlock.setHeader(new HeaderBean(nextBlock));

        nextBlock.setNonce(new Random().nextLong());
        nextBlock.setMerkleRoot(Sha256Hash.ZERO_HASH);
        nextBlock.setDifficultyTarget(Utils.encodeCompactBits(networkParams.params().getMaxTarget()));
        nextBlock.setTime(prevBlock.getTime() + (TimeUnit.MINUTES.toSeconds(10)));
        nextBlock.setVersion(prevBlock.getVersion());
        nextBlock.setPrevBlockHash(prevBlock.getHash());
        nextBlock.setHash(nextBlock.calculateHash());

        nextBlock.solve(networkParams);

        nextBlock = ChainUtils.buildNextInChain(prevBlock, nextBlock);

        return nextBlock;
    }

    /*
     * Creates a new full block, linking the next blocks transaction to the previous, transfering the full amount. If the previous block does not have a transaction, then create one.
     */
    public static FullBlock nextFullBlock(Net networkParams, FullBlock prevBlock, boolean withTransaction){
        FullBlock nextBlock = new FullBlockBean();
        nextBlock.setHeader(new HeaderBean(nextBlock));

        nextBlock.setNonce(new Random().nextLong());
        nextBlock.setMerkleRoot(Sha256Hash.ZERO_HASH);
        nextBlock.setDifficultyTarget(Utils.encodeCompactBits(networkParams.params().getMaxTarget()));
        nextBlock.setTime(prevBlock.getTime() + (TimeUnit.MINUTES.toSeconds(10)));
        nextBlock.setVersion(prevBlock.getVersion());
        nextBlock.setPrevBlockHash(prevBlock.getHash());
        nextBlock.setHash(nextBlock.calculateHash());

        nextBlock.solve(networkParams);

        if(withTransaction) {
            //Add a transaction to the previous block
            if(prevBlock.getTransactions().isEmpty()){
                prevBlock.setTransactions(Arrays.asList(nextTransaction(null, nextBlock)));
            }

            Tx nextTx = nextTransaction(prevBlock, nextBlock);

            nextBlock.setTransactions(Arrays.asList(nextTx));
        }

        return nextBlock;
    }

    /*
     * Takes the previous transaction at index 0, and create a new transaction at index 0 within the next block. If prevBlock is null, a ZERO transaction will be created with an initial 50 coins.
     */
    public static Tx nextTransaction(FullBlock prevBlock, FullBlock nextBlock){
        Tx tx = new TxBean(nextBlock);
        TxInput txInput = new TxInputBean(tx);
        TxOutPoint txOutPoint = new TxOutPointBean(txInput);
        TxOutput txOutput = new TxOutputBean(tx);


        if(prevBlock == null) {
            txOutPoint.setHash(Sha256Hash.ZERO_HASH);
            txInput.setValue(Coin.FIFTY_COINS);
            txOutput.setValue(Coin.FIFTY_COINS);
        } else {
            txOutPoint.setHash(prevBlock.getTransactions().get(0).getHash());
            txInput.setValue(prevBlock.getTransactions().get(0).getOutputs().get(0).getValue());
            txOutput.setValue(prevBlock.getTransactions().get(0).getOutputs().get(0).getValue());
        }

        //outpoint
        txOutPoint.setIndex(0);

        //input
        txInput.setSequenceNumber(0);
        txInput.setScriptBytes(new byte[]{});
        txInput.setOutpoint(txOutPoint);
        txInput.setSequenceNumber(0);

        //output
        txOutput.setScriptBytes(new byte[]{});

        //tx
        tx.setInputs(Arrays.asList(txInput));
        tx.setOutputs(Arrays.asList(txOutput));

        tx.setVersion(0);
        tx.setLockTime(0);

        return tx;
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
