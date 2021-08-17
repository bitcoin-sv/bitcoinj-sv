package io.bitcoinsv.bitcoinjsv.blockchain.pow;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;

public abstract class AbstractRuleCheckerFactory {

    protected NetworkParameters networkParameters;

    public AbstractRuleCheckerFactory(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
    }

    public abstract RulesPoolChecker getRuleChecker(LiteBlock storedPrev, LiteBlock nextBlock);

    protected boolean isTestNet() {
        return NetworkParameters.ID_TESTNET.equals(networkParameters.getId());
    }

}
