/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.bitcoin.api.base;

import io.bitcoinj.core.Coin;
import io.bitcoinj.bitcoin.api.BitcoinObject;
import io.bitcoinj.script.Script;

public interface TxOutput extends BitcoinObject<TxOutput> {
    Coin getValue();

    void setValue(Coin value);

    byte[] getScriptBytes();

    void setScriptBytes(byte[] scriptBytes);

    Script getScriptPubKey();

    void setScriptPubKey(Script scriptPubKey);
}
