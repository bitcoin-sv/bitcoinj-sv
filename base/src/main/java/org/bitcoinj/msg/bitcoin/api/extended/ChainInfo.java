package org.bitcoinj.msg.bitcoin.api.extended;

import org.bitcoinj.msg.bitcoin.api.BitcoinObject;

import java.math.BigInteger;

public interface ChainInfo<C extends BitcoinObject> extends BitcoinObject<C> {

    BigInteger getChainWork();

    void setChainWork(BigInteger chainWork);

    int getHeight();

    void setHeight(int height);
}
