package org.bitcoinj.pow;

import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.core.StoredBlock;

public abstract class AbstractRuleCheckerFactory {

    protected NetworkParameters networkParameters;

    public AbstractRuleCheckerFactory(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
    }

    public abstract RulesPoolChecker getRuleChecker(StoredBlock storedPrev, Block nextBlock);

    protected boolean isTestNet() {
        return NetworkParameters.ID_TESTNET.equals(networkParameters.getId());
    }

}
