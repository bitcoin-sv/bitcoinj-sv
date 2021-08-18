package io.bitcoinsv.bitcoinjsv.temp;

import io.bitcoinsv.bitcoinjsv.core.PeerFilterProvider;
import io.bitcoinsv.bitcoinjsv.script.interpreter.ScriptExecutionException;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.TransactionBroadcaster;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.msg.p2p.FilteredBlock;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Transaction;
import io.bitcoinsv.bitcoinjsv.temp.listener.KeyChainEventListener;
import io.bitcoinsv.bitcoinjsv.temp.listener.ScriptsChangeEventListener;
import io.bitcoinsv.bitcoinjsv.temp.listener.WalletCoinsReceivedEventListener;

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
