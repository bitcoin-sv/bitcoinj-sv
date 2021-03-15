package io.bitcoinj.bitcoin;

import io.bitcoinj.bitcoin.api.base.FullBlock;
import io.bitcoinj.bitcoin.bean.base.FullBlockBean;
import io.bitcoinj.core.UnsafeByteArrayOutputStream;
import io.bitcoinj.params.Net;
import io.bitcoinj.params.UnitTestParams;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 12/03/2021
 */
public class FullBlockBeanTest {

    @Test
    public void testFullBlockBean() {
        FullBlock fullBlock = Genesis.getFor(UnitTestParams.get().getNet());

        try (ByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream()) {
            fullBlock.serializeTo(outputStream);

            FullBlock resultFullBlock = null;
            try(ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())){
                resultFullBlock = new FullBlockBean(inputStream);
            } catch (Exception ex){
                fail(ex);
            }

            assertEquals(resultFullBlock, fullBlock);
        } catch (Exception ex) {
            fail(ex);
        }

    }
}
