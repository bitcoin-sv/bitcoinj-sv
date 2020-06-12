package org.bitcoinj.pow_legacy.factory;

import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.chain_legacy.StoredBlock_legacy;
import org.bitcoinj.pow_legacy.AbstractRuleCheckerFactory_legacy;
import org.bitcoinj.pow_legacy.RulesPoolChecker_legacy;

public class RuleCheckerFactory_legacy extends AbstractRuleCheckerFactory_legacy {

    private AbstractRuleCheckerFactory_legacy daaRulesFactory;
    private AbstractRuleCheckerFactory_legacy edaRulesFactory;

    public static RuleCheckerFactory_legacy create(NetworkParameters parameters) {
        return new RuleCheckerFactory_legacy(parameters);
    }

    private RuleCheckerFactory_legacy(NetworkParameters parameters) {
        super(parameters);
        this.daaRulesFactory = new DAARuleCheckerFactory_legacy(parameters);
        this.edaRulesFactory = new EDARuleCheckerFactory_legacy(parameters);
    }

    @Override
    public RulesPoolChecker_legacy getRuleChecker(StoredBlock_legacy storedPrev, Block nextBlock) {
        if (isNewDaaActivated(storedPrev, networkParameters)) {
            return daaRulesFactory.getRuleChecker(storedPrev, nextBlock);
        } else {
            return edaRulesFactory.getRuleChecker(storedPrev, nextBlock);
        }
    }

    private boolean isNewDaaActivated(StoredBlock_legacy storedPrev, NetworkParameters parameters) {
        return storedPrev.getHeight() >= parameters.getDAAUpdateHeight();
    }

}
