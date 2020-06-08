package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Sha256Hash;

import java.util.List;

public interface FullBlock extends BitcoinObject<FullBlock> {
    HeaderBean getHeader();

    void setHeader(HeaderBean header);

    /**
     * gets the block hash, calculating it if necessary.  If the block is mutable this make also trigger calculation of
     * the merkle tree.
     * @return
     */
    Sha256Hash getHash();

    List<Tx> getTransactions();

    void setTransactions(List<Tx> transactions);

    Sha256Hash calculateMerkleRoot();
}
