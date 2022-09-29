package io.bitcoinsv.bitcoinjsv.blockstore;

import io.bitcoinsv.bitcoinjsv.bitcoin.Genesis;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.FullBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.FullBlockBean;
import io.bitcoinsv.bitcoinjsv.params.Net;
import io.bitcoinsv.bitcoinjsv.utils.FileUtil;
import io.bitcoinsv.bitcoinjsv.blockchain.SPVBlockChain;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.exception.PrunedException;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.tools.BlkDatParser;
import io.bitcoinsv.bitcoinjsv.tools.BlockParsedListener;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

public class ExampleHeaderChain {

    @Test
    @Disabled
    public void testHeaderChain() throws FileNotFoundException, BlockStoreException {

        /**
         * To run we need a bitcoind data directory, usually at /home/user/.bitcoin/blocks
         *
         * we also need a data directory somewhere in the folder heirarchy.
         *
         * FIXME this test probably needs a different source of data.
         */

        String blocksDir = "/data/mainnet/bitcoin-data-full/blocks/";
        boolean deleteDirs = true;
        boolean storeCoinbases = true;
        boolean storeTxids = true;


        File dataDir = FileUtil.findDirInParents("data");
        NetworkParameters params = Net.MAINNET.params();

        FullBlockStore.initMemoryOnly(params);

        BlockStore<LiteBlock>  storeCb = null;
        BlockStore<LiteBlock>  storeHeadersOnly = null;
        try {
            storeHeadersOnly = new FullHeadersBlockStore(params.getNet(), new File(dataDir, "spv-file-block-store-ho"), storeCoinbases, deleteDirs, storeTxids);
            ((FullHeadersBlockStore) storeHeadersOnly).start();

        } catch (BlockStoreException e) {
            throw new RuntimeException(e);
        }

        final SPVBlockChain chainHeadersOnly = new SPVBlockChain(params, storeHeadersOnly);

        BlkDatParser parser = new BlkDatParser(params, blocksDir, true, 0, new BlockParsedListener() {
            int blocks = 0;

            @Override
            public void onBlockParsed(FullBlock block, int numParsed, File currentFile, long start, long len) {
                blocks++;

                try {
                    //FullBlockStore.get().putBlock(block);
                    LiteBlock liteBlock = block.asLiteBlock();
                    Sha256Hash hash = block.getHash();

                    byte[] bytes = block.serialize();
                    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                    ByteBuffer buf = ByteBuffer.wrap(bytes);

                    FullBlock b1 = new FullBlockBean(bytes);
                    b1 = new FullBlockBean(bis);
                    b1 = new FullBlockBean(Utils.bufferAsInputStream(buf));

                    Tx cb = b1.getTransactions().get(0);

                    //b1.setTime(0); //
                    b1.makeMutable();
                    b1 = b1.copy();
                    b1 = b1.mutableCopy();

                    b1 = new FullBlockBean();
                    Genesis.getFor(params.getNet());


                    if (chainHeadersOnly.getBlockStore().get(hash) == null)
                        chainHeadersOnly.add(liteBlock);
                } catch (PrunedException | BlockStoreException e) {
                   throw new RuntimeException(e);
                }

            }

            @Override
            public void onComplete(long timeToProcess) {

            }
        });

        parser.run();
    }

}
