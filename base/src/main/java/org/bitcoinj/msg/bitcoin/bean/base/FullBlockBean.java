/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.msg.bitcoin.bean.base;

import com.google.common.annotations.VisibleForTesting;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.msg.bitcoin.api.base.AbstractBlock;
import org.bitcoinj.msg.bitcoin.api.extended.LiteBlock;
import org.bitcoinj.msg.bitcoin.bean.MerkleBuilder;
import org.bitcoinj.msg.bitcoin.bean.extended.BlockMetaBean;
import org.bitcoinj.msg.bitcoin.api.base.FullBlock;
import org.bitcoinj.msg.bitcoin.api.base.Header;
import org.bitcoinj.msg.bitcoin.api.base.Tx;
import org.bitcoinj.msg.bitcoin.bean.extended.LiteBlockBean;
import org.bitcoinj.utils.Threading;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static org.bitcoinj.core.Sha256Hash.hashTwice;
import static org.bitcoinj.core.Sha256Hash.of;

public class FullBlockBean extends HashableImpl<FullBlock> implements FullBlock {

    private Header header;

    private BlockMetaBean metaData;

    private List<Tx> transactions;

    public FullBlockBean(byte[] payload, int offset) {
        super(null, payload, offset);
    }

    public FullBlockBean(byte[] payload) {
        super(null, payload, 0);
    }

    public FullBlockBean(InputStream in) {
        super(null, in);
    }

    /**
     * Special case constructor for Genesis block
     */
    @VisibleForTesting
    public FullBlockBean() {
        super(null);
        setHeader(new HeaderBean(this));
        setTransactions(new ArrayList<>());
    }

    @Override
    public Header getHeader() {
        return header;
    }

    @Override
    public void setHeader(Header header) {
        checkMutable();
        this.header = header;
    }

    @Override
    public List<Tx> getTransactions() {
        return isMutable() ? transactions : Collections.unmodifiableList(transactions);
    }

    @Override
    public void setTransactions(List<Tx> transactions) {
        checkMutable();
        this.transactions = transactions;
    }

    public void calculateTxHashes() {
        CountDownLatch hashLatch = new CountDownLatch(getTransactions().size());
        for (Tx tx: getTransactions()) {
            //we are doing batches of hashes so we can take advantage of multi threading
            Threading.THREAD_POOL.execute(new Runnable() {
                @Override
                public void run() {
                    //calculates hash and caches it, this is 99% of the runtime
                    tx.getHash();
                    hashLatch.countDown();
                }
            });

            try {
                ExecutorService exec = Threading.THREAD_POOL;
                long awaiting = hashLatch.getCount();
                hashLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void parse() {

        header = new HeaderBean(this, payload, offset);
        cursor += header.getMessageSize();

        int numTransactions = (int) readVarInt();
        transactions = new ArrayList<>(numTransactions);
        CountDownLatch hashLatch = new CountDownLatch(numTransactions);

        for (int i = 0; i < numTransactions; i++) {
            TxBean tx = new TxBean(this, payload, cursor);
            transactions.add(tx);
            cursor += tx.getMessageSize();
        }

        //fill in the meta data
        buildMetaData();
    }

    @Override
    protected int parse(InputStream in) throws IOException {
        int read = 0;
        header = new HeaderBean(this, in);
        read += header.getMessageSize();

        int numTransactions = (int) new VarInt(in).value;
        read += VarInt.sizeOf(numTransactions);
        transactions = new ArrayList<>(numTransactions);

        for (int i = 0; i < numTransactions; i++) {
            TxBean tx = new TxBean(this, in);
            transactions.add(tx);
            read += tx.getMessageSize();
        }

        //fill in the meta data
        buildMetaData();

        return read;
    }

    private void buildMetaData() {
        //have to pass null so calling the setters doesn't clear parent payload.
        //it isn't part of the same serialized payload though so it doesn't need a parent
        metaData = new BlockMetaBean();
        metaData.setTxCount(transactions.size());
        metaData.setBlockSize(cursor - offset);
        metaData.makeImmutable();
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        header.serializeTo(stream);
        stream.write(new VarInt(transactions.size()).encode());
        for (Tx tx : transactions) {
            tx.serializeTo(stream);
        }
    }

    @Override
    protected int estimateMessageLength() {
        int len = Header.FIXED_MESSAGE_SIZE;
        for (Tx tx: getTransactions())
            len += tx instanceof TxBean ? ((TxBean) tx).estimateMessageLength() : 500;
        return len;
    }

    @Override
    public FullBlock makeNew(byte[] serialized) {
        return new FullBlockBean(serialized);
    }

    @Override
    public void makeSelfMutable() {
        super.makeSelfMutable();
        if (header != null) {
            header.makeSelfMutable(); //also nulls block hash
            header.setMerkleRoot(null); //needs to be nulled in case txs change.
        }
        if (transactions != null) {
            for (Tx tx : getTransactions())
                tx.makeSelfMutable();
        }
    }

    /**
     * This is here for early testing, we'll replace it with something more efficient.
     * @return
     */

    @Override
    public Sha256Hash calculateMerkleRoot() {
        return MerkleBuilder.calculateMerkleRootFromTxs(transactions);
    }

    @Override
    public AbstractBlock getBlock() {
        return this;
    }

    @Override
    public LiteBlock asLiteBlock() {
        LiteBlock lite = new LiteBlockBean();
        lite.getHeader().copyFrom(getHeader());
        lite.getBlockMeta().setBlockSize(getMessageSize());
        lite.getBlockMeta().setTxCount(getTransactions().size());
        return lite;
    }
}
