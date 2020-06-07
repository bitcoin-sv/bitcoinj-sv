package org.bitcoinj.pow;

import org.bitcoinj.chain.AbstractBlockChain;
import org.bitcoinj.chain.StoredBlock;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.exception.BlockStoreException;

import java.util.ArrayList;
import java.util.List;

public class RulesPoolChecker extends AbstractPowRulesChecker {

    private List<AbstractPowRulesChecker> rules;

    public RulesPoolChecker(NetworkParameters networkParameters) {
        super(networkParameters);
        this.rules = new ArrayList<AbstractPowRulesChecker>();
    }

    @Override
    public void checkRules(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore, AbstractBlockChain blockChain) throws VerificationException, BlockStoreException {
        for (AbstractPowRulesChecker rule : rules) {
            rule.checkRules(storedPrev, nextBlock, blockStore, blockChain);
        }
    }

    public void addRule(AbstractPowRulesChecker rule) {
        this.rules.add(rule);
    }

}
