package org.bitcoinj.blockchain;

import org.bitcoinj.core.listeners.NewBestBlockListener;
import org.bitcoinj.core.listeners.ReorganizeListener;

/**
 * Proxy for wallet to enable refactoring
 */
public interface ChainEventListener extends NewBestBlockListener, ReorganizeListener {
    int getLastBlockSeenHeight();
}
