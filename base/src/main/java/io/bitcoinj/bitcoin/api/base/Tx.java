/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
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

}
