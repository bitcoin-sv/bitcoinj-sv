package io.bitcoinsv.bitcoinjsv.pow_legacy.factory;

import io.bitcoinsv.bitcoinjsv.core.Verification;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Block;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.chain_legacy.StoredBlock_legacy;
import io.bitcoinsv.bitcoinjsv.pow_legacy.AbstractRuleCheckerFactory_legacy;
import io.bitcoinsv.bitcoinjsv.pow_legacy.RulesPoolChecker_legacy;
import io.bitcoinsv.bitcoinjsv.pow_legacy.rule.MinimalDifficultyRuleChecker_legacy;
import io.bitcoinsv.bitcoinjsv.pow_legacy.rule.NewDifficultyAdjustmentAlgorithmRulesChecker_legacy;

public class DAARuleCheckerFactory_legacy extends AbstractRuleCheckerFactory_legacy {

    public DAARuleCheckerFactory_legacy(NetworkParameters parameters) {
        super(parameters);
    }

    @Override
    public RulesPoolChecker_legacy getRuleChecker(StoredBlock_legacy storedPrev, Block nextBlock) {
        RulesPoolChecker_legacy rulesChecker = new RulesPoolChecker_legacy(networkParameters);
        if (isTestNet() && Verification.isValidTestnetDateBlock(nextBlock)) {
            rulesChecker.addRule(new MinimalDifficultyRuleChecker_legacy(networkParameters));
        } else {
            rulesChecker.addRule(new NewDifficultyAdjustmentAlgorithmRulesChecker_legacy(networkParameters));
        }
        return rulesChecker;
    }

}
