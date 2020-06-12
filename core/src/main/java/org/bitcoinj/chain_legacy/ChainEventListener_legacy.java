package org.bitcoinj.chain_legacy;

import org.bitcoinj.core.listeners.NewBestBlockListener;
import org.bitcoinj.core.listeners.ReorganizeListener;
import org.bitcoinj.core.listeners.TransactionReceivedInBlockListener;

public interface ChainEventListener_legacy extends NewBestBlockListener, ReorganizeListener, TransactionReceivedInBlockListener {
    int getLastBlockSeenHeight();
}
