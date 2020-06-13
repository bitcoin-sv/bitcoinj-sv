/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.msg.bitcoin.bean.extended;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.bitcoin.api.base.AbstractBlock;
import org.bitcoinj.msg.bitcoin.api.base.Header;
import org.bitcoinj.msg.bitcoin.api.extended.BlockMeta;
import org.bitcoinj.msg.bitcoin.api.extended.ChainInfo;
import org.bitcoinj.msg.bitcoin.api.extended.LiteBlock;
import org.bitcoinj.msg.bitcoin.bean.base.HashableImpl;
import org.bitcoinj.msg.bitcoin.bean.base.HeaderBean;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LiteBlockBean<C extends LiteBlock> extends HashableImpl<LiteBlock> implements LiteBlock<LiteBlock> {

    protected Header header;
    protected BlockMeta blockMeta;
    protected ChainInfo chainInfo;

    public LiteBlockBean(byte[] payload, int offset) {
        super(null, payload, offset);
    }

    public LiteBlockBean(InputStream in) {
        super(null, in);
    }

    public LiteBlockBean() {
        super(null);
        header = new HeaderBean(this);
        blockMeta = new BlockMetaBean(this);
        chainInfo = new ChainInfoBean(this);
    }

    @Override
    public Header getHeader() {
        return header;
    }

    @Override
    public void setHeader(Header header) {
        this.header = header;
    }

    @Override
    public Sha256Hash calculateMerkleRoot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockMeta getBlockMeta() {
        return blockMeta;
    }

    @Override
    public void setBlockMeta(BlockMeta blockMeta) {
        this.blockMeta = blockMeta;
    }

    @Override
    public ChainInfo getChainInfo() {
        return chainInfo;
    }

    @Override
    public void setChainInfo(ChainInfo chainInfo) {
        this.chainInfo = chainInfo;
    }

    @Override
    protected void parse() {
        header = new HeaderBean(this, payload, cursor);
        cursor += header.getMessageSize();

        blockMeta = new BlockMetaBean(payload, cursor);
        cursor += blockMeta.getMessageSize();

        chainInfo = new ChainInfoBean(this, payload, cursor);
        cursor += chainInfo.getMessageSize();

    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        header.serializeTo(stream);
        blockMeta.serializeTo(stream);
        chainInfo.serializeTo(stream);
    }

    @Override
    public LiteBlock makeNew(byte[] serialized) {
        return new LiteBlockBean(serialized, 0);
    }

    @Override
    public AbstractBlock getBlock() {
        return this;
    }
}
