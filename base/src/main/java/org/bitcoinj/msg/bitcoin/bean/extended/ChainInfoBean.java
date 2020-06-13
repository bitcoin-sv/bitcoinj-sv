/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.msg.bitcoin.bean.extended;

import org.bitcoinj.core.Utils;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;
import org.bitcoinj.msg.bitcoin.api.base.HeaderReadOnly;
import org.bitcoinj.msg.bitcoin.api.extended.ChainInfo;
import org.bitcoinj.msg.bitcoin.bean.BitcoinObjectImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkState;

public class ChainInfoBean<C extends BitcoinObject> extends BitcoinObjectImpl<ChainInfo> implements ChainInfo<ChainInfo> {

    private static final byte[] EMPTY_BYTES = new byte[CHAIN_WORK_BYTES];

    private BigInteger chainWork = null;
    private int height = -1;

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
    protected void parse() {
        byte[] chainWorkBytes = readBytes(CHAIN_WORK_BYTES);
        chainWork = new BigInteger(1, chainWorkBytes);
        height = (int) readUint32();
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
