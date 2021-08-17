package io.bitcoinsv.bitcoinjsv.bitcoin;

import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxOutputBean;
import io.bitcoinsv.bitcoinjsv.core.Coin;
import io.bitcoinsv.bitcoinjsv.core.UnsafeByteArrayOutputStream;
import io.bitcoinsv.bitcoinjsv.script.Script;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author q.zhou@nchain.com
 * Copyright (c) 2021 nChain Ltd
 */
public class TxOutputBeanTest extends TxBeanTestBase {

    @Test
    public void testSerializeTo() {
        try (ByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream()) {
            output0.serializeTo(outputStream);
            assertArrayEquals(output0Bytes, outputStream.toByteArray());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testParse() {
        assertEquals(Coin.valueOf(5, 56), output0.getValue());
        assertArrayEquals(output0ScriptBytes, output0.getScriptBytes());
        assertEquals(new Script(output0ScriptBytes), output0.getScriptPubKey());
    }

    @Test
    public void testParseWithInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(output0Bytes);
        TxOutputBean newBean = new TxOutputBean(null, byteArrayInputStream);
        assertTxOutputEquals(output0, newBean);
    }
}
