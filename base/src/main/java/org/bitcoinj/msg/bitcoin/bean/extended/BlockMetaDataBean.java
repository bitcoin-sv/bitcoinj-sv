package org.bitcoinj.msg.bitcoin.bean.extended;

import org.bitcoinj.core.Utils;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;
import org.bitcoinj.msg.bitcoin.api.extended.BlockMetaData;
import org.bitcoinj.msg.bitcoin.bean.BitcoinObjectImpl;

import java.io.IOException;
import java.io.OutputStream;

public class BlockMetaDataBean extends BitcoinObjectImpl<BlockMetaData> implements BlockMetaData<BlockMetaData> {

    private int txCount;
    private long blockSize;

    public BlockMetaDataBean(byte[] payload, int offset) {
        super(null, payload, offset);
    }

    public BlockMetaDataBean() {
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
    public BlockMetaDataBean makeNew(byte[] serialized) {
        return new BlockMetaDataBean(serialized, 0);
    }

}
