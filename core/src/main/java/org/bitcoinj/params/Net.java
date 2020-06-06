package org.bitcoinj.params;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.bitcoinj.core.NetworkParameters;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Set;

/**
 * Simple enum key to replace NetworkParameters in many cases. It was intended
 * to hold no references to other class types but as NetworkParameters has been
 * regfactors to eliminate those dependencies it's less important. It is now
 * a placeholder and easy lookup mechanism for NetworkParameters.
 */
public enum Net {
    MAINNET(MainNetParams.class), REGTEST(RegTestParams.class),
    TESTNET2(TestNet2Params.class), TESTNET3(TestNet3Params.class),
    STN(STNParams.class), UNITTEST(UnitTestParams.class);

    private final Class<? extends NetworkParameters> paramsClass;

    Net(Class<? extends NetworkParameters> clazz) {
        paramsClass = clazz;
    }

    public NetworkParameters params() {
        return of(this);
    }

    /**
     * Only used to ensure params is initialized before constructing a genesis block
     */
    @VisibleForTesting
    public void ensureParams() {
        NetworkParameters params = of(this);
        if (params == null) {
            try {
                Method get = paramsClass.getMethod("get");
                get.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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

    /** Registered networks */
    private static Set<? extends NetworkParameters> networks = ImmutableSet.of(TestNet3Params.get(), MainNetParams.get());

    public static Set<? extends NetworkParameters> getRegistered() {
        return networks;
    }
}
