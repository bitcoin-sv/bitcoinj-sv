package org.bitcoinj.msg.bitcoin.api.base;

import org.bitcoinj.core.Coin;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;
import org.bitcoinj.script.Script;

public interface TxOutput extends BitcoinObject<TxOutput> {
    Coin getValue();

    void setValue(Coin value);

    byte[] getScriptBytes();

    void setScriptBytes(byte[] scriptBytes);

    Script getScriptPubKey();

    void setScriptPubKey(Script scriptPubKey);
}
