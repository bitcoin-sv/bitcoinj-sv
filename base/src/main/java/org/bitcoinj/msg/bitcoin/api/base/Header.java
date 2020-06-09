package org.bitcoinj.msg.bitcoin.api.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;

public interface Header<C extends Header> extends BitcoinObject<C>, HeaderReadOnly<C> {

    void setHash(Sha256Hash hash);

    void setVersion(long version);

    void setPrevBlockHash(Sha256Hash prevBlockHash);

    void setMerkleRoot(Sha256Hash merkleRoot);

    void setTime(long time);

    void setDifficultyTarget(long difficultyTarget);

    void setNonce(long nonce);

    long getTxCount();

    void setTxCount(long txCount);

    Tx getCoinbase();

    void setCoinbase(Tx coinbase);

    long getSerializedLength();

    void setSerializedLength(long serializedLength);

}
