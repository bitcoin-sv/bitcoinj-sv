package io.bitcoinsv.bitcoinjsv.blockchain.pow.factory;

import io.bitcoinsv.bitcoinjsv.blockchain.pow.AbstractPowRulesChecker;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.AbstractRuleCheckerFactory;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.RulesPoolChecker;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.rule.DifficultyTransitionPointRuleChecker;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.rule.EmergencyDifficultyAdjustmentRuleChecker;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.rule.LastNonMinimalDifficultyRuleChecker;
import io.bitcoinsv.bitcoinjsv.blockchain.pow.rule.MinimalDifficultyNoChangedRuleChecker;
import io.bitcoinsv.bitcoinjsv.core.Verification;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;

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
