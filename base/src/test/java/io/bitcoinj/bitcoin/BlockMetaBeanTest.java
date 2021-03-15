package io.bitcoinj.bitcoin;

import io.bitcoinj.bitcoin.api.extended.BlockMeta;
import io.bitcoinj.bitcoin.bean.extended.BlockMetaBean;
import io.bitcoinj.core.UnsafeByteArrayOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 12/03/2021
 */
public class BlockMetaBeanTest {

    @Test
    public void testBlockMetaBean() {
        BlockMeta blockMeta = new BlockMetaBean();

        blockMeta.setBlockSize(1000);
        blockMeta.setTxCount(1000);

        try (ByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream()) {
            blockMeta.serializeTo(outputStream);

            BlockMeta resultBlockMeta = null;
            try(ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())){
                resultBlockMeta = new BlockMetaBean(inputStream);
            } catch (Exception ex){
                fail(ex);
            }

            assertArrayEquals(resultBlockMeta.serialize(), blockMeta.serialize());
        } catch (Exception ex) {
            fail(ex);
        }

    }
}
