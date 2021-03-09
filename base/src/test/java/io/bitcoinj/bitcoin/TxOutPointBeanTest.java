package io.bitcoinj.bitcoin;

import io.bitcoinj.bitcoin.bean.base.TxOutPointBean;
import io.bitcoinj.core.UnsafeByteArrayOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author q.zhou@nchain.com
 * Copyright (c) 2021 nChain Ltd
 */
public class TxOutPointBeanTest extends TxBeanTestBase {

    @Test
    public void testSerializeTo() {
        try (ByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream()) {
            outPoint0.serializeTo(outputStream);
            assertArrayEquals(outPoint0Bytes, outputStream.toByteArray());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testParse() {
        assertEquals(outPoint0Hash, outPoint0.getHash());
        assertEquals(outPoint0Hash.toString(), outPoint0.getHashAsString());
        assertEquals(0L, outPoint0.getIndex());
    }

    @Test
    public void testParseWithInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(outPoint0Bytes);
        TxOutPointBean newBean = new TxOutPointBean(null, byteArrayInputStream);
        assertEquals(outPoint0, newBean);
    }
}
