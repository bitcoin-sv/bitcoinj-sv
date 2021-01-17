package io.bitcoinj.blockchain.pow.factory;

import io.bitcoinj.blockchain.pow.AbstractRuleCheckerFactory;
import io.bitcoinj.blockchain.pow.RulesPoolChecker;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

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
