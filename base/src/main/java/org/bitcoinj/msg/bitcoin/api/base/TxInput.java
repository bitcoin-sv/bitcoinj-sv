package org.bitcoinj.msg.bitcoin.api.base;

import org.bitcoinj.core.Coin;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;
import org.bitcoinj.script.Script;

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
