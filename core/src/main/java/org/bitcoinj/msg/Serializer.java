package org.bitcoinj.msg;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.Network;

import java.util.EnumMap;

public class Serializer {

    private static final EnumMap<Network, MessageSerializer> SERIALIZERS = new EnumMap(Network.class);

    public static MessageSerializer defaultFor(Network network) {
        MessageSerializer serializer = SERIALIZERS.get(network);
        if (serializer == null) {
            NetworkParameters params = Network.of(network);
            serializer = new BitcoinSerializer(params);
            register(network, serializer);
        }
        return serializer;
    }
    static void register(Network network, MessageSerializer instance) {
        SERIALIZERS.put(network, instance);
    }
}
