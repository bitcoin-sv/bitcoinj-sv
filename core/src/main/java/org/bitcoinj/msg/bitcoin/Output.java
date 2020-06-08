package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Coin;
import org.bitcoinj.script.Script;

public interface Output extends BitcoinObject<Output> {
    Coin getValue();

    void setValue(Coin value);

    byte[] getScriptBytes();

    void setScriptBytes(byte[] scriptBytes);

    Script getScriptPubKey();

    void setScriptPubKey(Script scriptPubKey);
}
