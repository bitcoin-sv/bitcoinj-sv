package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.BitcoinJ;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.bitcoin.api.base.*;
import org.bitcoinj.msg.bitcoin.api.extended.ChainInfo;
import org.bitcoinj.msg.bitcoin.api.extended.LiteBlock;
import org.bitcoinj.msg.bitcoin.bean.base.*;
import org.bitcoinj.msg.bitcoin.bean.extended.LiteBlockBean;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.Net;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;

import static com.google.common.base.Preconditions.checkState;
import static org.bitcoinj.core.Coin.FIFTY_COINS;

/**
 * Placeholder class for genesis blocks to seperate Message classes from
 * NetworkParameters
 */
public class Genesis {

    private static final EnumMap<Net, FullBlock> GENESIS_BLOCKS = new EnumMap(Net.class);
    private static final EnumMap<Net, LiteBlock> GENESIS_BLOCKS_LITE = new EnumMap(Net.class);

    public static FullBlock getFor(Net net) {
        FullBlock genesis = GENESIS_BLOCKS.get(net);
        if (genesis == null) {
            synchronized (Genesis.class) {
                genesis = GENESIS_BLOCKS.get(net);
                if (genesis == null) {
                    //Ensure NetworkParams has been created
                    net.ensureParams();
                    genesis = createGenesis(net);
                    configureGenesis(net, genesis);

                    checkState(genesis.getHashAsString().equals(net.params().genesisHash()));
                    GENESIS_BLOCKS.put(net, genesis);
                }
            }
        }
        return genesis;
    }

    public static LiteBlock getHeaderFor(Net net) {
        LiteBlock genesis = GENESIS_BLOCKS_LITE.get(net);
        if (genesis == null) {
            synchronized (Genesis.class) {
                genesis = GENESIS_BLOCKS_LITE.get(net);
                if (genesis == null) {
                    //Ensure NetworkParams has been created
                    net.ensureParams();

                    genesis = new LiteBlockBean(Genesis.getFor(net).serialize(), 0);
                    ChainInfo info = genesis.getChainInfo();
                    info.makeMutable();
                    info.setHeight(0);
                    info.setChainWork(genesis.getWork());
                    info.makeImmutable();

                    checkState(genesis.getHashAsString().equals(net.params().genesisHash()));
                    GENESIS_BLOCKS_LITE.put(net, genesis);
                }
            }
        }
        return genesis;
    }

    private static FullBlock createGenesis(Net n) {
        FullBlock genesis = new FullBlockBean();
        genesis.setVersion(BitcoinJ.BLOCK_VERSION_GENESIS);
        genesis.setDifficultyTarget(0x1d07fff8L);
        genesis.setPrevBlockHash(Sha256Hash.ZERO_HASH);
        genesis.setTime(System.currentTimeMillis() / 1000);

        Tx coinbase = new TxBean(genesis);
        try {
            // A script containing the difficulty bits and the following message:
            //
            //   "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks"
            byte[] bytes = Utils.HEX.decode
                    ("04ffff001d0104455468652054696d65732030332f4a616e2f32303039204368616e63656c6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f757420666f722062616e6b73");

            TxInput input = new TxInputBean(coinbase);

            TxOutPoint outPoint = new TxOutPointBean(input);
            outPoint.setHash(Sha256Hash.ZERO_HASH);
            outPoint.setIndex(TxOutPoint.UNCONNECTED);

            input.setScriptBytes(bytes);
            input.setSequenceNumber(TxInput.NO_SEQUENCE);
            input.setOutpoint(outPoint);

            coinbase.getInputs().add(input);

            ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
            ScriptChunk.writeBytes(scriptPubKeyBytes, Utils.HEX.decode
                    ("04678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5f"));
            scriptPubKeyBytes.write(ScriptOpCodes.OP_CHECKSIG);

            TxOutput output = new TxOutputBean(coinbase);
            output.setScriptBytes(scriptPubKeyBytes.toByteArray());
            output.setValue(FIFTY_COINS);

            coinbase.getOutputs().add(output);

        } catch (Exception e) {
            // Cannot happen.
            throw new RuntimeException(e);
        }
        genesis.getTransactions().add(coinbase);
        return genesis;
    }

    private static void configureGenesis(Net net, FullBlock genesis) {
        NetworkParameters params = net.params();
        genesis.setDifficultyTarget(params.genesisDifficulty());
        genesis.setTime(params.genesisTime());
        genesis.setNonce(params.genesisNonce());
        if (net == Net.UNITTEST) {
            genesis.solve();
        } else {
            String genesisHash = genesis.getHashAsString();
            boolean genesisHashCorrect = genesisHash.equals(params.genesisHash());
            checkState(genesisHashCorrect);


        }
    }

}
