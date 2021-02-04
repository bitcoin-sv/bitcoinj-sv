/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.bitcoin.api.base;

import io.bitcoinj.core.Sha256Hash;

import java.util.List;

public interface Tx extends Hashable<Tx> {

    void setHash(Sha256Hash hash);

    long getVersion();

    void setVersion(long version);

    List<TxInput> getInputs();

    void setInputs(List<TxInput> inputs);

    List<TxOutput> getOutputs();

    void setOutputs(List<TxOutput> outputs);

    long getLockTime();

    void setLockTime(long lockTime);

    boolean isCoinBase();

}
