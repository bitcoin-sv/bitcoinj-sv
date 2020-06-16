package io.bitcoinj.pow_legacy.factory;

import io.bitcoinj.core.Verification;
import io.bitcoinj.msg.protocol.Block;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.chain_legacy.StoredBlock_legacy;
import io.bitcoinj.pow_legacy.AbstractRuleCheckerFactory_legacy;
import io.bitcoinj.pow_legacy.RulesPoolChecker_legacy;
import io.bitcoinj.pow_legacy.rule.MinimalDifficultyRuleChecker_legacy;
import io.bitcoinj.pow_legacy.rule.NewDifficultyAdjustmentAlgorithmRulesChecker_legacy;

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
