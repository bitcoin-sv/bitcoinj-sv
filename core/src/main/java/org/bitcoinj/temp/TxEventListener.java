package org.bitcoinj.temp;

import org.bitcoinj.core.PeerFilterProvider;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionBroadcaster;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.p2p.FilteredBlock;
import org.bitcoinj.msg.protocol.Transaction;
import org.bitcoinj.temp.listener.KeyChainEventListener;
import org.bitcoinj.temp.listener.ScriptsChangeEventListener;
import org.bitcoinj.temp.listener.WalletCoinsReceivedEventListener;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Executor;

public interface TxEventListener extends PeerFilterProvider {

    boolean checkForFilterExhaustion(FilteredBlock block);

    void receivePending(Transaction tx, @Nullable List<Transaction> dependencies) throws VerificationException;

    boolean isPendingTransactionRelevant(Transaction tx) throws ScriptException;

    Transaction getTransaction(Sha256Hash hash);

    void setTransactionBroadcaster(TransactionBroadcaster broadcaster);

    void addCoinsReceivedEventListener(Executor executor, WalletCoinsReceivedEventListener listener);

    void addKeyChainEventListener(Executor executor, KeyChainEventListener listener);

    void addScriptChangeEventListener(Executor executor, ScriptsChangeEventListener listener);

    boolean removeCoinsReceivedEventListener(WalletCoinsReceivedEventListener listener);

    boolean removeKeyChainEventListener(KeyChainEventListener listener);

    boolean removeScriptChangeEventListener(ScriptsChangeEventListener listener);

}
