package io.bitcoinj.bitcoin;

import io.bitcoinj.bitcoin.bean.base.TxInputBean;
import io.bitcoinj.core.UnsafeByteArrayOutputStream;
import io.bitcoinj.script.Script;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author q.zhou@nchain.com
 * Copyright (c) 2021 nChain Ltd
 */
public class TxInputBeanTest extends TxBeanTestBase {

    @Test
    public void testSerializeTo() {
        try (ByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream()) {
            input0.serializeTo(outputStream);
            assertArrayEquals(input0Bytes, outputStream.toByteArray());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testParse() {
        assertEquals(outPoint0, input0.getOutpoint());
        assertArrayEquals(input0ScriptBytes, input0.getScriptBytes());
        assertEquals(new Script(input0ScriptBytes), input0.getScriptSig());
        assertEquals(0xFFFFFFFFL, input0.getSequenceNumber());
    }

    @Test
    public void testParseWithInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input0Bytes);
        TxInputBean newBean = new TxInputBean(null, byteArrayInputStream);
        assertTxInputEquals(input0, newBean);
    }
}
