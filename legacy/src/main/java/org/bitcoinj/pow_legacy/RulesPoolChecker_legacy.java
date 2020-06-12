package org.bitcoinj.pow_legacy;

import org.bitcoinj.chain_legacy.StoredBlock_legacy;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.store.BlockStore_legacy;
import org.bitcoinj.exception.BlockStoreException;

import java.util.ArrayList;
import java.util.List;

public class RulesPoolChecker_legacy extends AbstractPowRulesChecker_legacy {

    private List<AbstractPowRulesChecker_legacy> rules;

    public RulesPoolChecker_legacy(NetworkParameters networkParameters) {
        super(networkParameters);
        this.rules = new ArrayList<AbstractPowRulesChecker_legacy>();
    }

    @Override
    public void checkRules(StoredBlock_legacy storedPrev, Block nextBlock, BlockStore_legacy blockStore) throws VerificationException, BlockStoreException {
        for (AbstractPowRulesChecker_legacy rule : rules) {
            rule.checkRules(storedPrev, nextBlock, blockStore);
        }
    }

    public void addRule(AbstractPowRulesChecker_legacy rule) {
        this.rules.add(rule);
    }

}
