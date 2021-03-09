package io.bitcoinj.bitcoin;

import io.bitcoinj.bitcoin.bean.base.TxBean;
import io.bitcoinj.core.UnsafeByteArrayOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author q.zhou@nchain.com
 * Copyright (c) 2021 nChain Ltd
 */
public class TxBeanTest extends TxBeanTestBase {

    @Test
    public void testSerializeTo() {
        try (ByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream()) {
            tx.serializeTo(outputStream);
            assertArrayEquals(txBytes, outputStream.toByteArray());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testParse() {
        assertEquals(txHash, tx.getHash());
        assertEquals(txHash.toString(), tx.getHashAsString());
        assertEquals(1L, tx.getVersion());
        assertEquals(inputs.size(), tx.getInputs().size());
        for (int i = 0; i < inputs.size(); ++i) {
            assertTxInputEquals(inputs.get(i), tx.getInputs().get(i));
        }
        assertEquals(outputs.size(), tx.getOutputs().size());
        for (int i = 0; i < outputs.size(); ++i) {
            assertTxOutputEquals(outputs.get(i), tx.getOutputs().get(i));
        }
        assertEquals(0L, tx.getLockTime());
    }

    @Test
    public void testParseWithInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(txBytes);
        TxBean newBean = new TxBean(null, byteArrayInputStream);
        assertEquals(tx.getHash(), newBean.getHash());
    }
}
