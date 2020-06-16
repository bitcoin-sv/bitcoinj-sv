package io.bitcoinj.blockchain.pow.factory;

import io.bitcoinj.blockchain.pow.AbstractPowRulesChecker;
import io.bitcoinj.blockchain.pow.AbstractRuleCheckerFactory;
import io.bitcoinj.blockchain.pow.RulesPoolChecker;
import io.bitcoinj.blockchain.pow.rule.DifficultyTransitionPointRuleChecker;
import io.bitcoinj.blockchain.pow.rule.EmergencyDifficultyAdjustmentRuleChecker;
import io.bitcoinj.blockchain.pow.rule.LastNonMinimalDifficultyRuleChecker;
import io.bitcoinj.blockchain.pow.rule.MinimalDifficultyNoChangedRuleChecker;
import io.bitcoinj.core.Verification;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

public class EDARuleCheckerFactory extends AbstractRuleCheckerFactory {

    public EDARuleCheckerFactory(NetworkParameters parameters) {
        super(parameters);
    }

    @Override
    public RulesPoolChecker getRuleChecker(LiteBlock storedPrev, LiteBlock nextBlock) {
        if (Verification.isDifficultyTransitionPoint(storedPrev.getChainInfo().getHeight(), networkParameters)) {
            return getTransitionPointRulesChecker();
        } else {
            return getNoTransitionPointRulesChecker(storedPrev, nextBlock);
        }
    }

    private RulesPoolChecker getTransitionPointRulesChecker() {
        RulesPoolChecker rulesChecker = new RulesPoolChecker(networkParameters);
        rulesChecker.addRule(new DifficultyTransitionPointRuleChecker(networkParameters));
        return rulesChecker;
    }

    private RulesPoolChecker getNoTransitionPointRulesChecker(LiteBlock storedPrev, LiteBlock nextBlock) {
        RulesPoolChecker rulesChecker = new RulesPoolChecker(networkParameters);
        if (isTestNet() && Verification.isValidTestnetDateBlock(nextBlock)) {
            rulesChecker.addRule(new LastNonMinimalDifficultyRuleChecker(networkParameters));
        } else {
            if (AbstractPowRulesChecker.hasEqualDifficulty(
                    storedPrev.getHeader().getDifficultyTarget(), networkParameters.getMaxTarget())) {
                rulesChecker.addRule(new MinimalDifficultyNoChangedRuleChecker(networkParameters));
            } else {
                rulesChecker.addRule(new EmergencyDifficultyAdjustmentRuleChecker(networkParameters));
            }
        }
        return rulesChecker;
    }

}
