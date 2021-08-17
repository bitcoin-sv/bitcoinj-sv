/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
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
