/*
 * Modifications described in the NOTICE.txt file are licensed under the Open BSV Licence.
 * Modifications Copyright 2020 Bitcoin Association
 */
package io.bitcoinsv.bitcoinjsv.blockchain;

import io.bitcoinsv.bitcoinjsv.core.listeners.NewBestBlockListener;
import io.bitcoinsv.bitcoinjsv.core.listeners.ReorganizeListener;

/**
 * Proxy for wallet to enable refactoring
 */
public interface ChainEventListener extends NewBestBlockListener, ReorganizeListener {
    int getLastBlockSeenHeight();
}
