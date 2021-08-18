package io.bitcoinsv.bitcoinjsv.blockchain;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Header;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HeaderBean;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.extended.LiteBlockBean;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.AbstractPowRulesChecker;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 19/02/2021
 */
public class DifficultyRuleChecker {

    @Test
    public void testHasEqualDifficultyHeader(){
        Header header = new HeaderBean(new LiteBlockBean());
        Header header2 = new HeaderBean(new LiteBlockBean());

        header.setDifficultyTarget(100);
        header2.setDifficultyTarget(100);

        Assertions.assertTrue(AbstractPowRulesChecker.hasEqualDifficulty(header, header2), "headers have failed equals test");
        assertTrue(AbstractPowRulesChecker.hasEqualDifficulty(Utils.encodeCompactBits(BigInteger.valueOf(header.getDifficultyTarget())), BigInteger.valueOf(header2.getDifficultyTarget())), "headers have failed equals test");
    }
}
