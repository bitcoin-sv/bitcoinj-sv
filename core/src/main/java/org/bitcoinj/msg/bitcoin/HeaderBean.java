package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;

import java.io.IOException;
import java.io.OutputStream;

public class HeaderBean extends BitcoinObjectImpl implements Header {

    // block hash which is hash of serialized header
    private Sha256Hash hash;

    // Fields defined as part of the protocol format.
    private long version;
    private Sha256Hash prevBlockHash;
    private Sha256Hash merkleRoot;
    private long time;
    private long difficultyTarget; // "nBits"
    private long nonce;

    //not part of protocol
    private long txCount;
    private Tx coinbase;
    private long serializedLength;

    public HeaderBean(FullBlockBean parent, byte[] payload, int offset) {
        super(parent, payload, offset);
    }

    public HeaderBean(FullBlockBean parent, byte[] payload) {
        super(parent, payload, 0);
    }

    public HeaderBean(byte[] payload) {
        super(null, payload, 0);
    }

    @Override
    public Sha256Hash getHash() {
        return hash;
    }

    @Override
    public void setHash(Sha256Hash hash) {
        this.hash = hash;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public Sha256Hash getPrevBlockHash() {
        return prevBlockHash;
    }

    @Override
    public void setPrevBlockHash(Sha256Hash prevBlockHash) {
        this.prevBlockHash = prevBlockHash;
    }

    @Override
    public Sha256Hash getMerkleRoot() {
        return merkleRoot;
    }

    @Override
    public void setMerkleRoot(Sha256Hash merkleRoot) {
        this.merkleRoot = merkleRoot;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public long getDifficultyTarget() {
        return difficultyTarget;
    }

    @Override
    public void setDifficultyTarget(long difficultyTarget) {
        this.difficultyTarget = difficultyTarget;
    }

    @Override
    public long getNonce() {
        return nonce;
    }

    @Override
    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    @Override
    public long getTxCount() {
        return txCount;
    }

    @Override
    public void setTxCount(long txCount) {
        this.txCount = txCount;
    }

    @Override
    public Tx getCoinbase() {
        return coinbase;
    }

    @Override
    public void setCoinbase(Tx coinbase) {
        this.coinbase = coinbase;
    }

    @Override
    public long getSerializedLength() {
        return serializedLength;
    }

    @Override
    public void setSerializedLength(long serializedLength) {
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
}
