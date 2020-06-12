package org.bitcoinj.msg.bitcoin.api.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.bitcoin.api.extended.LiteBlock;
import org.bitcoinj.msg.bitcoin.bean.base.HeaderBean;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;

import java.util.List;

public interface FullBlock extends AbstractBlock<FullBlock> {

    List<Tx> getTransactions();

    void setTransactions(List<Tx> transactions);

    LiteBlock asLiteBlock();
}
