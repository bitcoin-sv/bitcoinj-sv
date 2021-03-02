/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.bitcoin.bean.extended;

import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.Utils;
import io.bitcoinj.bitcoin.api.BitcoinObject;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.bitcoin.bean.BitcoinObjectImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkState;

public class ChainInfoBean<C extends BitcoinObject> extends BitcoinObjectImpl<ChainInfo> implements ChainInfo<ChainInfo> {

    private static final byte[] EMPTY_BYTES = new byte[CHAIN_WORK_BYTES];

    private BigInteger chainWork;
    private int height;
    //total number of txs in chain including this block
    private long totalChainTxs;
    //this should ONLY be set for blocks in forked chain as it's constantly
    //updating and would be a burden to update for every block.  If the block
    //is part of the main chain is should be null and indicates the current
    //chainhead is the tip for this block.
    private LiteBlock longestChainTip;

    private transient LiteBlock prevBlock;
    private transient LiteBlock prev10Block;
    private transient LiteBlock prev100Block;
    private transient LiteBlock prev1000Block;


    private HeaderReadOnly header;

    public ChainInfoBean(HeaderReadOnly parent, byte[] payload, int offset) {
        super(parent, payload, offset);
        this.header = parent;
    }

    public ChainInfoBean(HeaderReadOnly parent, InputStream in) {
        super(parent, in);
        this.header = parent;
    }

    public ChainInfoBean(HeaderReadOnly parent) {
        super(parent);
        this.header = parent;
        this.height = -1;
        this.totalChainTxs = -1;
    }



    @Override
    public BigInteger getChainWork() {
        return chainWork;
    }

    @Override
    public void setChainWork(BigInteger chainWork) {
        checkMutable();
        this.chainWork = chainWork;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        checkMutable();
        this.height = height;
    }

    @Override
    public long getTotalChainTxs() {
        return totalChainTxs;
    }

    @Override
    public void setTotalChainTxs(long totalChainTxs) {
        this.totalChainTxs = totalChainTxs;
    }

    @Override
    protected void parse() {
        byte[] chainWorkBytes = readBytes(CHAIN_WORK_BYTES);
        chainWork = new BigInteger(1, chainWorkBytes);
        height = (int) readUint32();
        totalChainTxs = readInt64();
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        byte[] chainWorkBytes = chainWork == null ? EMPTY_BYTES : chainWork.toByteArray();
        checkState(chainWorkBytes.length <= CHAIN_WORK_BYTES, "Ran out of space to store chain work!");
        stream.write(chainWorkBytes);
        if (chainWorkBytes.length < CHAIN_WORK_BYTES) {
            // Pad to the right size.
            stream.write(EMPTY_BYTES, 0, CHAIN_WORK_BYTES - chainWorkBytes.length);
        }
        Utils.uint32ToByteStreamLE(getHeight(), stream);
        Utils.int64ToByteStreamLE(totalChainTxs, stream);
    }

    @Override
    public ChainInfo makeNew(byte[] serialized) {
        return new ChainInfoBean(getHeader(), serialized, 0);
    }

    @Override
    public HeaderReadOnly getHeader() {
        return header;
    }
}
