/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.extended;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.AbstractBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Header;

import java.util.List;

public interface TxIdBlock<C extends TxIdBlock> extends AbstractBlock<C> {

    void setHeader(Header header);

    List<Sha256Hash> getTxids();

    void setTxids(List<Sha256Hash> txids);

    default int fixedSize() {
        return UNKNOWN_MESSAGE_LENGTH;
    }
}
