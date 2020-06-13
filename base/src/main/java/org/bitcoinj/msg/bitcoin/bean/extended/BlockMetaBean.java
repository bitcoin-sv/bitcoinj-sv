/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.msg.bitcoin.bean.extended;

import org.bitcoinj.core.Utils;
import org.bitcoinj.msg.bitcoin.api.base.AbstractBlock;
import org.bitcoinj.msg.bitcoin.api.extended.BlockMeta;
import org.bitcoinj.msg.bitcoin.bean.BitcoinObjectImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BlockMetaBean extends BitcoinObjectImpl<BlockMeta> implements BlockMeta<BlockMeta> {

    private int txCount = -1;
    private long blockSize = -1;

    public BlockMetaBean(byte[] payload, int offset) {
        super(null, payload, offset);
    }

    public BlockMetaBean(InputStream in) {
        super(null, in);
    }

    public BlockMetaBean(AbstractBlock parent) {
        super(parent);
    }

    public BlockMetaBean() {
        super(null);
    }

    @Override
    public int getTxCount() {
        return txCount;
    }

    @Override
    public void setTxCount(int txCount) {
        checkMutable();
        this.txCount = txCount;
    }

    @Override
    public long getBlockSize() {
        return blockSize;
    }

    @Override
    public void setBlockSize(long blockSize) {
        checkMutable();
        this.blockSize = blockSize;
    }

    @Override
    protected void parse() {
        cursor = offset;
        txCount = (int) readUint32();
        blockSize = readInt64();
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        Utils.uint32ToByteStreamLE(txCount, stream);
        Utils.int64ToByteStreamLE(blockSize, stream);
    }

    @Override
    public BlockMetaBean makeNew(byte[] serialized) {
        return new BlockMetaBean(serialized, 0);
    }

}
