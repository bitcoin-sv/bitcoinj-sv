package org.bitcoinj.msg.protocol;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.IChildMessage;
import org.bitcoinj.script.Script;

import javax.annotation.Nullable;

public interface ITransactionOutput extends IChildMessage {

    int getIndex();

    Script getScriptPubKey() throws ScriptException;

    Coin getValue();

    void setValue(Coin coin);

    boolean isDust();

    byte[] getScriptBytes();

    @Nullable
    <T extends ITransaction> T getParentTransaction();

    @Nullable
    Sha256Hash getParentTransactionHash();

    <TOP extends ITransactionOutPoint> TOP getOutPointFor();
}
