package org.bitcoinj.pow_legacy.factory;

import org.bitcoinj.core.Verification;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.chain_legacy.StoredBlock_legacy;
import org.bitcoinj.pow_legacy.AbstractPowRulesChecker_legacy;
import org.bitcoinj.pow_legacy.AbstractRuleCheckerFactory_legacy;
import org.bitcoinj.pow_legacy.RulesPoolChecker_legacy;
import org.bitcoinj.pow_legacy.rule.DifficultyTransitionPointRuleChecker_legacy;
import org.bitcoinj.pow_legacy.rule.EmergencyDifficultyAdjustmentRuleChecker_legacy;
import org.bitcoinj.pow_legacy.rule.LastNonMinimalDifficultyRuleChecker_legacy;
import org.bitcoinj.pow_legacy.rule.MinimalDifficultyNoChangedRuleChecker_legacy;

public class EDARuleCheckerFactory_legacy extends AbstractRuleCheckerFactory_legacy {

    public EDARuleCheckerFactory_legacy(NetworkParameters parameters) {
        super(parameters);
    }

    @Override
    public RulesPoolChecker_legacy getRuleChecker(StoredBlock_legacy storedPrev, Block nextBlock) {
        if (Verification.isDifficultyTransitionPoint(storedPrev.getHeight(), networkParameters)) {
            return getTransitionPointRulesChecker();
        } else {
            return getNoTransitionPointRulesChecker(storedPrev, nextBlock);
        }
    }

    private RulesPoolChecker_legacy getTransitionPointRulesChecker() {
        RulesPoolChecker_legacy rulesChecker = new RulesPoolChecker_legacy(networkParameters);
        rulesChecker.addRule(new DifficultyTransitionPointRuleChecker_legacy(networkParameters));
        return rulesChecker;
    }

    private RulesPoolChecker_legacy getNoTransitionPointRulesChecker(StoredBlock_legacy storedPrev, Block nextBlock) {
        RulesPoolChecker_legacy rulesChecker = new RulesPoolChecker_legacy(networkParameters);
        if (isTestNet() && Verification.isValidTestnetDateBlock(nextBlock)) {
            rulesChecker.addRule(new LastNonMinimalDifficultyRuleChecker_legacy(networkParameters));
        } else {
            if (AbstractPowRulesChecker_legacy.hasEqualDifficulty(
                    storedPrev.getHeader().getDifficultyTarget(), networkParameters.getMaxTarget())) {
                rulesChecker.addRule(new MinimalDifficultyNoChangedRuleChecker_legacy(networkParameters));
            } else {
                rulesChecker.addRule(new EmergencyDifficultyAdjustmentRuleChecker_legacy(networkParameters));
            }
        }
        return rulesChecker;
    }

}
