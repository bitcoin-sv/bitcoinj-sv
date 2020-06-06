package org.bitcoinj.params;

import com.google.common.annotations.VisibleForTesting;
import org.bitcoinj.core.NetworkParameters;

import java.util.EnumMap;

public enum Net {
    MAINNET, REGTEST, TESTNET2, TESTNET3, STN, UNITTEST;

    public NetworkParameters params() {
        return of(this);
    }

    private static final EnumMap<Net, NetworkParameters> PARAMS = new EnumMap(Net.class);

    public static NetworkParameters of(Net net) {
        return PARAMS.get(net);
    }
    static void register(Net net, NetworkParameters instance) {
        PARAMS.put(net, instance);
    }

    @VisibleForTesting
    public static NetworkParameters replaceForTesting(Net net, NetworkParameters params) {
        NetworkParameters original = PARAMS.put(net, params);
        return original;
    }
}
