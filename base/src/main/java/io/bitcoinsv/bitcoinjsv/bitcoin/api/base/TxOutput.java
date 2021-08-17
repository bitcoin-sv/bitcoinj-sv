/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.base;

import io.bitcoinsv.bitcoinjsv.core.Coin;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;
import io.bitcoinsv.bitcoinjsv.script.Script;

public interface TxOutput extends BitcoinObject<TxOutput> {
    Coin getValue();

    void setValue(Coin value);

    byte[] getScriptBytes();

    void setScriptBytes(byte[] scriptBytes);

    Script getScriptPubKey();

    void setScriptPubKey(Script scriptPubKey);
}
