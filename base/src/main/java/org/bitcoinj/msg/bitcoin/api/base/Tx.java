package org.bitcoinj.msg.bitcoin.api.base;

import org.bitcoinj.core.Sha256Hash;

import java.util.List;

public interface Tx extends Hashable<Tx> {

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
