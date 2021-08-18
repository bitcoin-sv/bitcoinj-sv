package io.bitcoinsv.bitcoinjsv.pow_legacy;

import io.bitcoinsv.bitcoinjsv.msg.protocol.Block;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.chain_legacy.StoredBlock_legacy;

public abstract class AbstractRuleCheckerFactory_legacy {

    protected NetworkParameters networkParameters;

    public AbstractRuleCheckerFactory_legacy(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
    }

    public abstract RulesPoolChecker_legacy getRuleChecker(StoredBlock_legacy storedPrev, Block nextBlock);

    protected boolean isTestNet() {
        return NetworkParameters.ID_TESTNET.equals(networkParameters.getId());
    }

}
