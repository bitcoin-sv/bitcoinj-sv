package org.bitcoinj.msg.protocol;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.msg.IChildMessage;
import org.bitcoinj.script.Script;

import javax.annotation.Nullable;

public interface ITransactionInput extends IChildMessage {
    boolean isCoinBase();


    long getSequenceNumber();

    void setSequenceNumber(long i);

    boolean hasSequence();

    <TOP extends ITransactionOutPoint> TOP getOutpoint();

    Script getScriptSig() throws ScriptException;

    byte[] getScriptBytes();

    void setScriptBytes(byte[] connectedScript);

    void clearScriptBytes();

    <T extends ITransaction> T getParentTransaction();

    @Nullable
    Coin getValue();

    void setValue(Coin value);


}


