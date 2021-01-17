package io.bitcoinj.pow_legacy;

import io.bitcoinj.msg.protocol.Block;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.chain_legacy.StoredBlock_legacy;

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
