package org.bitcoinj.msg;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.Net;

import java.util.EnumMap;

public class Serializer {

    private static final EnumMap<Net, MessageSerializer> SERIALIZERS = new EnumMap(Net.class);

    public static MessageSerializer get(Net net, boolean parzeLazy, boolean parseRetain, boolean compactTransactionsInBlock) {
        NetworkParameters params = Net.of(net);
        return new BitcoinSerializer(params, parzeLazy, parseRetain, compactTransactionsInBlock);
    }

    public static MessageSerializer defaultFor(Net net) {
        MessageSerializer serializer = SERIALIZERS.get(net);
        if (serializer == null) {
            synchronized (Serializer.class) {
                serializer = SERIALIZERS.get(net);
                if (serializer == null) {
                    NetworkParameters params = Net.of(net);
                    serializer = new BitcoinSerializer(params);
                    register(net, serializer);
                }
            }
        }
        return serializer;
    }
    static void register(Net net, MessageSerializer instance) {
        SERIALIZERS.put(net, instance);
    }
}
