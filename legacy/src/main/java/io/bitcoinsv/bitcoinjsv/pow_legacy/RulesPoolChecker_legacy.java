package io.bitcoinsv.bitcoinjsv.pow_legacy;

import io.bitcoinsv.bitcoinjsv.chain_legacy.StoredBlock_legacy;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Block;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.store_legacy.BlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;

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
