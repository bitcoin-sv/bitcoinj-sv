package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Sha256Hash;

import java.util.List;

public interface FullBlock extends BitcoinObject {
    HeaderBean getHeader();

    void setHeader(HeaderBean header);

    List<Tx> getTransactions();

    void setTransactions(List<Tx> transactions);

    Sha256Hash calculateMerkleRoot();
}
