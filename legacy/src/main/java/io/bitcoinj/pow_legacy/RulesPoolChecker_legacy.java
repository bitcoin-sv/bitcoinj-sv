package io.bitcoinj.pow_legacy;

import io.bitcoinj.chain_legacy.StoredBlock_legacy;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.msg.protocol.Block;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.store_legacy.BlockStore_legacy;
import io.bitcoinj.exception.BlockStoreException;

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
