package org.bitcoinj.msg.bitcoin.bean.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;
import org.bitcoinj.msg.bitcoin.api.base.FullBlock;
import org.bitcoinj.msg.bitcoin.api.base.Header;
import org.bitcoinj.msg.bitcoin.api.base.Tx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HeaderBean extends HashableImpl<Header> implements Header<Header> {

    // block hash which is hash of serialized header
    private FullBlock block;

    // Fields defined as part of the protocol format.
    private long version;
    private Sha256Hash prevBlockHash;
    private Sha256Hash merkleRoot;
    private long time;
    private long difficultyTarget; // "nBits"
    private long nonce;

    //full block meta data, not part of protocol
    private boolean hasBlockMetaData = false;

    private long txCount;
    private Tx coinbase;
    private long serializedLength;

    public HeaderBean(FullBlock parent) {
        super(parent);
    }

    public HeaderBean(FullBlock parent, byte[] payload, int offset) {
        super(parent, payload, offset);
        block = parent;
        //block may be in the process of parsing so we won't add the block meta data yet.
    }

    public HeaderBean(FullBlock parent, byte[] payload) {
        this(parent, payload, 0);
    }

    public HeaderBean(byte[] payload) {
        super(null, payload, 0);
    }

    public HeaderBean(FullBlock parent, InputStream in) {
        super(parent, in);
        block = parent;
    }

    public void afterBlockParse() {
        txCount = block.getTransactions().size();
        coinbase = block.getTransactions().get(0);
        serializedLength = block.getMessageSize();
        hasBlockMetaData = true;
    }

    @Override
    public boolean hasBlockMetaData() {
        return hasBlockMetaData;
    }

    @Override
    public FullBlock getBlock() {
        return block;
    }

    @Override
    public void setHash(Sha256Hash hash) {
        checkMutable();
        this.hash = hash;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        checkMutable();
        this.version = version;
    }

    @Override
    public Sha256Hash getPrevBlockHash() {
        return prevBlockHash;
    }

    @Override
    public void setPrevBlockHash(Sha256Hash prevBlockHash) {
        checkMutable();
        this.prevBlockHash = prevBlockHash;
    }

    @Override
    public Sha256Hash getMerkleRoot() {
        if (merkleRoot == null && block != null) {
            checkMutable();
            merkleRoot = block.calculateMerkleRoot();
        }
        return merkleRoot;
    }

    @Override
    public void setMerkleRoot(Sha256Hash merkleRoot) {
        checkMutable();
        this.merkleRoot = merkleRoot;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void setTime(long time) {
        checkMutable();
        this.time = time;
    }

    @Override
    public long getDifficultyTarget() {
        return difficultyTarget;
    }

    @Override
    public void setDifficultyTarget(long difficultyTarget) {
        checkMutable();
        this.difficultyTarget = difficultyTarget;
    }

    @Override
    public long getNonce() {
        return nonce;
    }

    @Override
    public void setNonce(long nonce) {
        checkMutable();
        this.nonce = nonce;
    }

    @Override
    public long getTxCount() {
        return txCount;
    }

    @Override
    public void setTxCount(long txCount) {
        checkMutable();
        this.txCount = txCount;
    }

    @Override
    public Tx getCoinbase() {
        checkMutable();
        return coinbase;
    }

    @Override
    public void setCoinbase(Tx coinbase) {
        checkMutable();
        this.coinbase = coinbase;
    }

    @Override
    public long getSerializedLength() {
        return serializedLength;
    }

    @Override
    public void setSerializedLength(long serializedLength) {
        checkMutable();
        this.serializedLength = serializedLength;
    }

    @Override
    protected void parse() {
        cursor = offset;
        version = readUint32();
        prevBlockHash = readHash();
        merkleRoot = readHash();
        time = readUint32();
        difficultyTarget = readUint32();
        nonce = readUint32();

        hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(payload, offset, cursor - offset));
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        Utils.uint32ToByteStreamLE(version, stream);
        stream.write(prevBlockHash.getReversedBytes());
        stream.write(getMerkleRoot().getReversedBytes());
        Utils.uint32ToByteStreamLE(time, stream);
        Utils.uint32ToByteStreamLE(difficultyTarget, stream);
        Utils.uint32ToByteStreamLE(nonce, stream);
    }

    @Override
    public Header makeNew(byte[] serialized) {
        return new HeaderBean(serialized);
    }

    @Override
    public void makeSelfMutable() {
        super.makeSelfMutable();
        this.hash = null;
        //we aren't zeroing the merkle root as that's taken care of by full block.
    }

}
