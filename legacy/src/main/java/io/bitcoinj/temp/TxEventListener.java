package io.bitcoinj.temp;

import io.bitcoinj.core.PeerFilterProvider;
import io.bitcoinj.script.interpreter.ScriptExecutionException;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.TransactionBroadcaster;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.msg.p2p.FilteredBlock;
import io.bitcoinj.msg.protocol.Transaction;
import io.bitcoinj.temp.listener.KeyChainEventListener;
import io.bitcoinj.temp.listener.ScriptsChangeEventListener;
import io.bitcoinj.temp.listener.WalletCoinsReceivedEventListener;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Executor;

public interface TxEventListener extends PeerFilterProvider {

    boolean checkForFilterExhaustion(FilteredBlock block);

    void receivePending(Transaction tx, @Nullable List<Transaction> dependencies) throws VerificationException;

    boolean isPendingTransactionRelevant(Transaction tx) throws ScriptExecutionException;

    Transaction getTransaction(Sha256Hash hash);

    void setTransactionBroadcaster(TransactionBroadcaster broadcaster);

    void addCoinsReceivedEventListener(Executor executor, WalletCoinsReceivedEventListener listener);

    void addKeyChainEventListener(Executor executor, KeyChainEventListener listener);

    void addScriptChangeEventListener(Executor executor, ScriptsChangeEventListener listener);

    boolean removeCoinsReceivedEventListener(WalletCoinsReceivedEventListener listener);

    boolean removeKeyChainEventListener(KeyChainEventListener listener);

    boolean removeScriptChangeEventListener(ScriptsChangeEventListener listener);

}
