package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Sha256Hash;

import java.util.List;

public interface Tx extends BitcoinObject {
    Sha256Hash calculateHash();

    Sha256Hash getHash();

    void setHash(Sha256Hash hash);

    long getVersion();

    void setVersion(long version);

    List<Input> getInputs();

    void setInputs(List<Input> inputs);

    List<Output> getOutputs();

    void setOutputs(List<Output> outputs);

    long getLockTime();

    void setLockTime(long lockTime);
}
