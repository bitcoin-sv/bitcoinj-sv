package io.bitcoinsv.bitcoinjsv.bitcoin;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxInput;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxOutput;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxBean;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxInputBean;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxOutPointBean;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxOutputBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import org.junit.jupiter.api.Assertions;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author q.zhou@nchain.com
 * Copyright (c) 2021 nChain Ltd
 */
public class TxBeanTestBase {

    // Transaction fff2525b8931402dd09222c50775608f75787bd2b87e56995a7bdd30f79702c4
    protected final static byte[] txBytes = Hex.decode("01000000" +
            "01" +
            "032e38e9c0a84c6046d687d10556dcacc41d275ec55fc00779ac88fdf357a187000000008c493046022100c352d3dd993a981beba4a63ad15c209275ca9470abfcd57da93b58e4eb5dce82022100840792bc1f456062819f15d33ee7055cf7b5ee1af1ebcc6028d9cdb1c3af7748014104f46db5e9d61a9dc27b8d64ad23e7383a4e6ca164593c2527c038c0857eb67ee8e825dca65046b82c9331586c82e0fd1f633f25f87c161bc6f8a630121df2b3d3ffffffff" +
            "02" +
            "00e32321000000001976a914c398efa9c392ba6013c5e04ee729755ef7f58b3288ac" +
            "000fe208010000001976a914948c765a6914d43f2a7ac177da2c2f6b52de3d7c88ac" +
            "00000000");
    protected final static TxBean tx = new TxBean(txBytes);
    protected final static Sha256Hash txHash = Sha256Hash.wrap("fff2525b8931402dd09222c50775608f75787bd2b87e56995a7bdd30f79702c4");

    protected final static byte[] outPoint0Bytes = Hex.decode("032e38e9c0a84c6046d687d10556dcacc41d275ec55fc00779ac88fdf357a18700000000");
    protected final static Sha256Hash outPoint0Hash = Sha256Hash.wrap("87a157f3fd88ac7907c05fc55e271dc4acdc5605d187d646604ca8c0e9382e03");
    protected final static TxOutPointBean outPoint0 = new TxOutPointBean(outPoint0Bytes);

    protected final static byte[] input0Bytes = Hex.decode("032e38e9c0a84c6046d687d10556dcacc41d275ec55fc00779ac88fdf357a187000000008c493046022100c352d3dd993a981beba4a63ad15c209275ca9470abfcd57da93b58e4eb5dce82022100840792bc1f456062819f15d33ee7055cf7b5ee1af1ebcc6028d9cdb1c3af7748014104f46db5e9d61a9dc27b8d64ad23e7383a4e6ca164593c2527c038c0857eb67ee8e825dca65046b82c9331586c82e0fd1f633f25f87c161bc6f8a630121df2b3d3ffffffff");
    protected final static byte[] input0ScriptBytes = Hex.decode("493046022100c352d3dd993a981beba4a63ad15c209275ca9470abfcd57da93b58e4eb5dce82022100840792bc1f456062819f15d33ee7055cf7b5ee1af1ebcc6028d9cdb1c3af7748014104f46db5e9d61a9dc27b8d64ad23e7383a4e6ca164593c2527c038c0857eb67ee8e825dca65046b82c9331586c82e0fd1f633f25f87c161bc6f8a630121df2b3d3");
    protected final static TxInputBean input0 = new TxInputBean(input0Bytes);

    protected final static byte[] output0Bytes = Hex.decode("00e32321000000001976a914c398efa9c392ba6013c5e04ee729755ef7f58b3288ac");
    protected final static byte[] output0ScriptBytes = Hex.decode("76a914c398efa9c392ba6013c5e04ee729755ef7f58b3288ac");
    protected final static TxOutputBean output0 = new TxOutputBean(output0Bytes);

    protected final static byte[] output1Bytes = Hex.decode("000fe208010000001976a914948c765a6914d43f2a7ac177da2c2f6b52de3d7c88ac");
    protected final static TxOutputBean output1 = new TxOutputBean(output1Bytes);

    protected List<TxInput> inputs = new ArrayList<>();
    protected List<TxOutput> outputs = new ArrayList<>();

    protected TxBeanTestBase() {
        inputs.add(input0);
        outputs.add(output0);
        outputs.add(output1);
    }

    protected void assertTxInputEquals(TxInput expected, TxInput actual) {
        assertEquals(expected.getOutpoint(), actual.getOutpoint());
        assertArrayEquals(expected.getScriptBytes(), actual.getScriptBytes());
        assertEquals(expected.getScriptSig(), actual.getScriptSig());
        assertEquals(expected.getSequenceNumber(), actual.getSequenceNumber());
    }

    protected void assertTxOutputEquals(TxOutput expected, TxOutput actual) {
        Assertions.assertEquals(expected.getValue(), actual.getValue());
        assertArrayEquals(expected.getScriptBytes(), actual.getScriptBytes());
        assertEquals(expected.getScriptPubKey(), actual.getScriptPubKey());
    }
}
