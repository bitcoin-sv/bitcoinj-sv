package org.bitcoinj.pow_legacy;

import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.chain_legacy.StoredBlock_legacy;

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
