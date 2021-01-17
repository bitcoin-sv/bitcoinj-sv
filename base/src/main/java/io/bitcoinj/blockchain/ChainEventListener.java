package io.bitcoinj.blockchain;

import io.bitcoinj.core.listeners.NewBestBlockListener;
import io.bitcoinj.core.listeners.ReorganizeListener;

/**
 * Proxy for wallet to enable refactoring
 */
public interface ChainEventListener extends NewBestBlockListener, ReorganizeListener {
    int getLastBlockSeenHeight();
}
