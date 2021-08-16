/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.bitcoin.api.base;

import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.params.Net;

import java.util.Date;

public interface AbstractBlock<C extends AbstractBlock> extends Header<C> {

    Header getHeader();

    void setHeader(Header header);

    default void verifyHeader(Net net) {
        getHeader().verifyHeader(net);
    }

    default long getVersion() {
        return getHeader().getVersion();
    }

    default Sha256Hash getPrevBlockHash() {
        return getHeader().getPrevBlockHash();
    }

    default Sha256Hash getMerkleRoot() {
        return getHeader().getMerkleRoot();
    }

    default long getTime() {
        return getHeader().getTime();
    }

    default Date getTimeAsDate() {
        return getHeader().getTimeAsDate();
    }

    default long getDifficultyTarget() {
        return getHeader().getDifficultyTarget();
    }

    default long getNonce() {
        return getHeader().getNonce();
    }

    default void setHash(Sha256Hash hash) {
        getHeader().setHash(hash);
    }

    default void setVersion(long version) {
        getHeader().setVersion(version);
    }

    default void setPrevBlockHash(Sha256Hash prevBlockHash) {
        getHeader().setPrevBlockHash(prevBlockHash);
    }

    default void setMerkleRoot(Sha256Hash merkleRoot) {
        getHeader().setMerkleRoot(merkleRoot);
    }

    default void setTime(long time) {
        getHeader().setTime(time);
    }

    default void setDifficultyTarget(long difficultyTarget) {
        getHeader().setDifficultyTarget(difficultyTarget);
    }

    default void setNonce(long nonce) {
        getHeader().setNonce(nonce);
    }

    /**
     * gets the block hash, calculating it if necessary.  If the block is mutable this make also trigger calculation of
     * the merkle tree.
     * @return
     */
    default Sha256Hash getHash() {
        return getHeader().getHash();
    }

    @Override
    default Sha256Hash calculateHash() {
        return getHeader().calculateHash();
    }

    Sha256Hash calculateMerkleRoot();

    @Override
    default int fixedSize() {
        return UNKNOWN_MESSAGE_LENGTH;
    }
}
