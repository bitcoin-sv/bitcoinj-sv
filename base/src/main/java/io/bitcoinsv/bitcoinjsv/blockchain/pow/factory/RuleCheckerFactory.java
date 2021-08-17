/*
 * Modifications described in the NOTICE.txt file are licensed under the Open BSV Licence.
 * Modifications Copyright 2020 Bitcoin Association
 */
package io.bitcoinsv.bitcoinjsv.blockchain.pow.factory;

import io.bitcoinsv.bitcoinjsv.blockchain.pow.AbstractRuleCheckerFactory;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.RulesPoolChecker;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;

public class RuleCheckerFactory extends AbstractRuleCheckerFactory {

    private AbstractRuleCheckerFactory daaRulesFactory;
    private AbstractRuleCheckerFactory edaRulesFactory;

    public static RuleCheckerFactory create(NetworkParameters parameters) {
        return new RuleCheckerFactory(parameters);
    }

    private RuleCheckerFactory(NetworkParameters parameters) {
        super(parameters);
        this.daaRulesFactory = new DAARuleCheckerFactory(parameters);
        this.edaRulesFactory = new EDARuleCheckerFactory(parameters);
    }

    @Override
    public RulesPoolChecker getRuleChecker(LiteBlock storedPrev, LiteBlock nextBlock) {
        if (isNewDaaActivated(storedPrev, networkParameters)) {
            return daaRulesFactory.getRuleChecker(storedPrev, nextBlock);
        } else {
            return edaRulesFactory.getRuleChecker(storedPrev, nextBlock);
        }
    }

    private boolean isNewDaaActivated(LiteBlock storedPrev, NetworkParameters parameters) {
        return storedPrev.getChainInfo().getHeight() >= parameters.getDAAUpdateHeight();
    }

}
