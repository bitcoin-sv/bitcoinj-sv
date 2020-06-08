package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Coin;
import org.bitcoinj.script.Script;

import javax.annotation.Nullable;

public interface Input extends BitcoinObject {

    public static final long NO_SEQUENCE = 0xFFFFFFFFL;

    long getSequenceNumber();

    void setSequenceNumber(long sequenceNumber);

    OutPoint getOutpoint();

    void setOutpoint(OutPoint outpoint);

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
