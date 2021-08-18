package io.bitcoinsv.bitcoinjsv.bitcoin;

import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HeaderBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.UnsafeByteArrayOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author q.zhou@nchain.com
 * Copyright (c) 2021 nChain Ltd
 */
public class HeaderBeanTest {

    // Block #0 header
    // 000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f
    private final static byte[] serialized = Hex.decode("01000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "3ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4a" +
            "29ab5f49" +
            "ffff001d" +
            "1dac2b7c");
    private final static HeaderBean headerBean = new HeaderBean(serialized);

    @Test
    public void testSerializeTo() {
        try (ByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream()) {
            headerBean.serializeTo(outputStream);
            assertArrayEquals(serialized, outputStream.toByteArray());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testParse() {
        assertEquals(1L, headerBean.getVersion());
        Assertions.assertEquals(Sha256Hash.ZERO_HASH, headerBean.getPrevBlockHash());
        Assertions.assertEquals(Sha256Hash.wrap("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"), headerBean.getMerkleRoot());
        assertEquals(1231006505L, headerBean.getTime());
        assertEquals(486604799L, headerBean.getDifficultyTarget());
        assertEquals(2083236893L, headerBean.getNonce());
        Assertions.assertEquals(Sha256Hash.wrap("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"), headerBean.getHash());
    }

    @Test
    public void testParseWithInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serialized);
        HeaderBean header = new HeaderBean(null, byteArrayInputStream);
        Assertions.assertEquals(headerBean.getHash(), header.getHash());
    }
}
