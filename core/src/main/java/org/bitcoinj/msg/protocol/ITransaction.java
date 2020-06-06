package org.bitcoinj.msg.protocol;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.IChildMessage;
import org.bitcoinj.msg.IMessage;

import java.util.List;

public interface ITransaction extends IChildMessage {
    Sha256Hash getHash();

    String getHashAsString();

    Coin getInputSum();

    Coin getOutputSum();

    Coin getFee();

    boolean isCoinBase();

    long getLockTime();

    long getVersion();

    <TI extends ITransactionInput> List<TI> getInputs();

    <TI extends ITransactionInput> void setInputs(List<TI> inputs);

    <TO extends ITransactionOutput> List<TO> getOutputs();

     void setOutputs(List<TransactionOutput> outputs);

    <TI extends ITransactionInput> TI getInput(long index);

    <TO extends ITransactionOutput> TO getOutput(long index);

    boolean isTimeLocked();

    boolean isFinal(int height, long blockTimeSeconds);
}
