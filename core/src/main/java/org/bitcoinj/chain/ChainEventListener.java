package org.bitcoinj.chain;

import org.bitcoinj.core.listeners.NewBestBlockListener;
import org.bitcoinj.core.listeners.ReorganizeListener;
import org.bitcoinj.core.listeners.TransactionReceivedInBlockListener;

public interface ChainEventListener extends NewBestBlockListener, ReorganizeListener, TransactionReceivedInBlockListener {
    int getLastBlockSeenHeight();
}
