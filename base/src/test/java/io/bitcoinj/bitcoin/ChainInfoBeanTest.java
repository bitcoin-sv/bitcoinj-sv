package io.bitcoinj.bitcoin;

import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.bitcoin.bean.base.HeaderBean;
import io.bitcoinj.bitcoin.bean.extended.ChainInfoBean;
import io.bitcoinj.bitcoin.bean.extended.LiteBlockBean;
import io.bitcoinj.core.UnsafeByteArrayOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 12/03/2021
 */
public class ChainInfoBeanTest {

    @Test
    public void testSerializeTo() {
        ChainInfo chainInfo = new ChainInfoBean(new HeaderBean(new LiteBlockBean()));

        chainInfo.setChainWork(BigInteger.TEN);
        chainInfo.setTotalChainTxs(1000);
        chainInfo.setHeight(50);

        try (ByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream()) {
            chainInfo.serializeTo(outputStream);

            ChainInfo resultChainInfo = null;
            try(ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())){
                resultChainInfo = new ChainInfoBean(new HeaderBean(new LiteBlockBean()), inputStream);
            } catch (Exception ex){
                fail(ex);
            }

            assertArrayEquals(resultChainInfo.serialize(), chainInfo.serialize());
        } catch (Exception ex) {
            fail(ex);
        }

    }
}
