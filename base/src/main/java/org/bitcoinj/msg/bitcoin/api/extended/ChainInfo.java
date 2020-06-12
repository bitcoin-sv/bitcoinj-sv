package org.bitcoinj.msg.bitcoin.api.extended;

import org.bitcoinj.msg.bitcoin.api.BitcoinObject;

import java.math.BigInteger;

public interface ChainInfo<C extends BitcoinObject> extends BitcoinObject<C>, ChainInfoReadOnly {

    public static final int CHAIN_WORK_BYTES = 12;

    public static final int FIXED_MESSAGE_SIZE = CHAIN_WORK_BYTES + 4;

    void setChainWork(BigInteger chainWork);

    void setHeight(int height);

    @Override
    default int fixedSize() {
        return FIXED_MESSAGE_SIZE;
    }
}
