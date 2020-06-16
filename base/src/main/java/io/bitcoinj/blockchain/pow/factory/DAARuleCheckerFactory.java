package io.bitcoinj.blockchain.pow.factory;

import io.bitcoinj.blockchain.pow.AbstractRuleCheckerFactory;
import io.bitcoinj.blockchain.pow.rule.MinimalDifficultyRuleChecker;
import io.bitcoinj.blockchain.pow.RulesPoolChecker;
import io.bitcoinj.blockchain.pow.rule.NewDifficultyAdjustmentAlgorithmRulesChecker;
import io.bitcoinj.core.Verification;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

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
