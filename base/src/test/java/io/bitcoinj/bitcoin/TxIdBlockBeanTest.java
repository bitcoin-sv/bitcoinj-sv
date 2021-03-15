package io.bitcoinj.bitcoin;

import io.bitcoinj.bitcoin.api.extended.TxIdBlock;
import io.bitcoinj.bitcoin.bean.extended.TxIdBlockBean;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.UnsafeByteArrayOutputStream;
import io.bitcoinj.params.Net;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 12/03/2021
 */
public class TxIdBlockBeanTest {

    @Test
    public void testTxIdBlockBean() {
        TxIdBlock txIdBlock = new TxIdBlockBean();

        txIdBlock.setHeader(Genesis.getHeaderFor(Net.MAINNET).getHeader());
        txIdBlock.setTxids(Arrays.asList(Sha256Hash.wrap("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f")));

        try (ByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream()) {
            txIdBlock.serializeTo(outputStream);

            TxIdBlock resultTxIdBlock = null;
            try(ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())){
                resultTxIdBlock = new TxIdBlockBean(inputStream);
            } catch (Exception ex){
                fail(ex);
            }

            assertArrayEquals(resultTxIdBlock.serialize(), txIdBlock.serialize());
        } catch (Exception ex) {
            fail(ex);
        }

    }
}
