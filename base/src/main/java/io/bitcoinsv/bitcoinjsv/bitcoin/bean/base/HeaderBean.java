/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.bean.base;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.AbstractBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Header;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HeaderBean extends HashableImpl<Header> implements Header<Header> {

    private AbstractBlock block;

    // Fields defined as part of the protocol format.
    private long version;
    private Sha256Hash prevBlockHash;
    private Sha256Hash merkleRoot;
    private long time;
    private long difficultyTarget; // "nBits"
    private long nonce;

    public HeaderBean(AbstractBlock parent) {
        super(parent);
        this.block = parent;
    }

    public HeaderBean(AbstractBlock parent, byte[] payload, int offset) {
        super(parent, payload, offset);
        this.block = parent;
    }

    public HeaderBean(AbstractBlock parent, byte[] payload) {
        this(parent, payload, 0);
    }

    public HeaderBean(byte[] payload) {
        this(null, payload, 0);
    }

    public HeaderBean(AbstractBlock parent, InputStream in) {
        super(parent, in);
        this.block = parent;
    }

    @Override
    public AbstractBlock getBlock() {
        return block;
    }

    /**
     * set the hash with a mutability check, to clear the hash without the check
     * call clearHash()
     * @param hash
     */
    @Override
    public void setHash(Sha256Hash hash) {
        clearHash();
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
