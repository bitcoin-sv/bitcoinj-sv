package org.bitcoinj.params;

import org.bitcoinj.core.NetworkParameters;

import java.util.EnumMap;

public enum Network {
    MAINNET, REGTEST, TESTNET2, TESTNET3, STN, UNITTEST;

    public NetworkParameters params() {
        return of(this);
    }

    private static final EnumMap<Network, NetworkParameters> PARAMS = new EnumMap(Network.class);

    public static NetworkParameters of(Network network) {
        return PARAMS.get(network);
    }
    static void register(Network network, NetworkParameters instance) {
        PARAMS.put(network, instance);
    }
}
