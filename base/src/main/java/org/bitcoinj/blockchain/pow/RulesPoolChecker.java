package org.bitcoinj.blockchain.pow;

import org.bitcoinj.blockstore.BlockStore;
import org.bitcoinj.exception.BlockStoreException;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.bitcoin.api.extended.LiteBlock;
import org.bitcoinj.params.NetworkParameters;

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
