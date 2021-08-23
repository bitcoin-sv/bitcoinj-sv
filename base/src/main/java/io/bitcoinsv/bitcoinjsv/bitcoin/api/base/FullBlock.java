/*
 * Copyright (c) 2017 Steve Shadders
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.base;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;

import java.util.List;

public interface FullBlock extends AbstractBlock<FullBlock> {

    List<Tx> getTransactions();

    void setTransactions(List<Tx> transactions);

    LiteBlock asLiteBlock();
}
