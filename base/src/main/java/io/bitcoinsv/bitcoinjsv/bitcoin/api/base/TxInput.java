/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.base;

import io.bitcoinsv.bitcoinjsv.core.Coin;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;
import io.bitcoinsv.bitcoinjsv.script.Script;

import javax.annotation.Nullable;

public interface TxInput extends BitcoinObject<TxInput> {

    long NO_SEQUENCE = 0xFFFFFFFFL;

    long getSequenceNumber();

    void setSequenceNumber(long sequenceNumber);

    TxOutPoint getOutpoint();

    void setOutpoint(TxOutPoint outpoint);

    byte[] getScriptBytes();

    void setScriptBytes(byte[] scriptBytes);

    Script getScriptSig();

    void setScriptSig(Script scriptSig);

    void setValue(@Nullable Coin value);

    default boolean hasSequence() {
        return getSequenceNumber() != NO_SEQUENCE;
    }

    @Nullable
    Coin getValue();

}
