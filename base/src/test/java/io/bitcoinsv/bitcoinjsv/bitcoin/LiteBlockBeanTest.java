package io.bitcoinsv.bitcoinjsv.bitcoin;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.extended.LiteBlockBean;
import io.bitcoinsv.bitcoinjsv.core.UnsafeByteArrayOutputStream;
import io.bitcoinsv.bitcoinjsv.params.UnitTestParams;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 12/03/2021
 */
public class LiteBlockBeanTest {

    @Test
    public void testLiteBlockBean() {
        LiteBlock liteBlock = Genesis.getHeaderFor(UnitTestParams.get().getNet());

        try (ByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream()) {
            liteBlock.serializeTo(outputStream);

            LiteBlock resultLiteBlock = null;
            try(ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())){
                resultLiteBlock = new LiteBlockBean(inputStream);
            } catch (Exception ex){
                fail(ex);
            }

            assertEquals(resultLiteBlock, liteBlock);
        } catch (Exception ex) {
            fail(ex);
        }

    }
}
