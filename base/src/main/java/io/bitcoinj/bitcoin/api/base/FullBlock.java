/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.bitcoin.api.base;

import io.bitcoinj.bitcoin.api.extended.LiteBlock;

import java.util.List;

public interface FullBlock extends AbstractBlock<FullBlock> {

    List<Tx> getTransactions();

    void setTransactions(List<Tx> transactions);

    LiteBlock asLiteBlock();
}
