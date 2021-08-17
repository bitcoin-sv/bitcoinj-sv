package io.bitcoinsv.bitcoinjsv.blockchain.pow.factory;

import io.bitcoinsv.bitcoinjsv.blockchain.pow.AbstractRuleCheckerFactory;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.rule.MinimalDifficultyRuleChecker;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.RulesPoolChecker;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.rule.NewDifficultyAdjustmentAlgorithmRulesChecker;
import io.bitcoinsv.bitcoinjsv.core.Verification;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;

public class DAARuleCheckerFactory extends AbstractRuleCheckerFactory {

    public DAARuleCheckerFactory(NetworkParameters parameters) {
        super(parameters);
    }

    @Override
    public RulesPoolChecker getRuleChecker(LiteBlock storedPrev, LiteBlock nextBlock) {
        RulesPoolChecker rulesChecker = new RulesPoolChecker(networkParameters);
        if (isTestNet() && Verification.isValidTestnetDateBlock(nextBlock)) {
            rulesChecker.addRule(new MinimalDifficultyRuleChecker(networkParameters));
        } else {
            rulesChecker.addRule(new NewDifficultyAdjustmentAlgorithmRulesChecker(networkParameters));
        }
        return rulesChecker;
    }

}
