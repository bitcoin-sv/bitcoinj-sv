package org.bitcoinj.msg.protocol;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.IChildMessage;

public interface ITransactionOutPoint extends IChildMessage {

    Sha256Hash getHash();

    void setHash(Sha256Hash hash);

    long getIndex();

    void setIndex(long index);
}
