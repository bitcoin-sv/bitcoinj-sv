package org.bitcoinj.msg.bitcoin.api.extended;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.bitcoin.api.base.AbstractBlock;
import org.bitcoinj.msg.bitcoin.api.base.Header;

import java.util.List;

public interface TxIdBlock<C extends TxIdBlock> extends AbstractBlock<C> {

    void setHeader(Header header);

    List<Sha256Hash> getTxids();

    void setTxids(List<Sha256Hash> txids);

    default int fixedSize() {
        return UNKNOWN_MESSAGE_LENGTH;
    }
}
