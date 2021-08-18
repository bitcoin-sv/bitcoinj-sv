package io.bitcoinsv.bitcoinjsv.bitcoin;

import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.UnsafeByteArrayOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class TxBeanProfilerTest extends TxBeanTestBase {

    private static ByteArrayInputStream txInputStream = new ByteArrayInputStream(txBytes);
    private static UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(txBytes.length);
    private static Sha256Hash txHash = tx.getHash();

    public static void main(String[] args) throws IOException {
        TxBeanProfilerTest test = new TxBeanProfilerTest();
        while (true) {
            test.testDeserializeFromStream();
            test.testDeserializeBytes();
            test.testSerializeToStream();
            test.testSerializeToBytes();
        }
    }

    @Test
    public void testDeserializeBytes() {
        TxBean tx = new TxBean(txBytes, txHash);
    }

    @Test
    public void testDeserializeFromStream() {
        txInputStream.reset();
        TxBean tx = new TxBean(txInputStream, txHash);
    }

    @Test
    public void testSerializeToBytes() {
        tx.serialize();
    }

    @Test
    public void testSerializeToStream() throws IOException {
        bos.reset();
        tx.serializeTo(bos);
    }

}
