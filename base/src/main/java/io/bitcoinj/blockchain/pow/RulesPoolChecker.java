package io.bitcoinj.blockchain.pow;

import io.bitcoinj.blockstore.BlockStore;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.bitcoin.api.extended.LiteBlock;
import io.bitcoinj.params.NetworkParameters;

import java.util.ArrayList;
import java.util.List;

public class RulesPoolChecker extends AbstractPowRulesChecker {

    private List<AbstractPowRulesChecker> rules;

    public RulesPoolChecker(NetworkParameters networkParameters) {
        super(networkParameters);
        this.rules = new ArrayList<AbstractPowRulesChecker>();
    }

    @Override
    public void checkRules(LiteBlock storedPrev, LiteBlock nextBlock, BlockStore blockStore) throws VerificationException, BlockStoreException {
        for (AbstractPowRulesChecker rule : rules) {
            rule.checkRules(storedPrev, nextBlock, blockStore);
        }
    }

    public void addRule(AbstractPowRulesChecker rule) {
        this.rules.add(rule);
    }

}
