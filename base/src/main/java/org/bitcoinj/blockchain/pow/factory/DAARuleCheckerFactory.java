package org.bitcoinj.blockchain.pow.factory;

import org.bitcoinj.blockchain.pow.AbstractRuleCheckerFactory;
import org.bitcoinj.blockchain.pow.rule.MinimalDifficultyRuleChecker;
import org.bitcoinj.blockchain.pow.RulesPoolChecker;
import org.bitcoinj.blockchain.pow.rule.NewDifficultyAdjustmentAlgorithmRulesChecker;
import org.bitcoinj.core.Verification;
import org.bitcoinj.msg.bitcoin.api.extended.LiteBlock;
import org.bitcoinj.params.NetworkParameters;

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
