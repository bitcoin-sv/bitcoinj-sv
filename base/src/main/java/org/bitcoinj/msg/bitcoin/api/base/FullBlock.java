package org.bitcoinj.msg.bitcoin.api.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.bitcoin.bean.base.HeaderBean;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;

import java.util.List;

public interface FullBlock extends BitcoinObject<FullBlock>, HashProvider {

    HeaderBean getHeader();

    void setHeader(HeaderBean header);

    /**
     * gets the block hash, calculating it if necessary.  If the block is mutable this make also trigger calculation of
     * the merkle tree.
     * @return
     */
    default Sha256Hash getHash() {
        return getHeader().getHash();
    }

    List<Tx> getTransactions();

    void setTransactions(List<Tx> transactions);

    Sha256Hash calculateMerkleRoot();
}
