package io.bitcoinj.msg;

import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.params.Net;

import java.util.EnumMap;

public class Serializer {

    private static final EnumMap<Net, MessageSerializer> SERIALIZERS = new EnumMap(Net.class);

    public static MessageSerializer get(NetworkParameters params, boolean parzeLazy, boolean parseRetain, boolean compactTransactionsInBlock) {
        return new BitcoinSerializer(params, parzeLazy, parseRetain, compactTransactionsInBlock);
    }

    public static MessageSerializer get(NetworkParameters params, boolean parzeLazy, boolean parseRetain) {
        return get(params, parzeLazy, parseRetain, params.getDefaultSerializeMode().isCompactTransactionsInBlock());
    }

    public static MessageSerializer get(Net net, boolean parzeLazy, boolean parseRetain, boolean compactTransactionsInBlock) {
        NetworkParameters params = Net.of(net);
        return get(params, parzeLazy, parseRetain, compactTransactionsInBlock);
    }

    public static MessageSerializer get(Net net, boolean parzeLazy, boolean parseRetain) {
        NetworkParameters params = Net.of(net);
        return get(params, parzeLazy, parseRetain, params.getDefaultSerializeMode().isCompactTransactionsInBlock());
    }

    public static MessageSerializer forMessage(Message message) {
        if (message.getSerializeMode() != null) {
            return new BitcoinSerializer(message.getNet(), message.getSerializeMode());
        }
        return defaultFor(message.getNet());
    }

    public static MessageSerializer defaultFor(NetworkParameters params) {
        return defaultFor(params.getNet());
    }

    public static MessageSerializer defaultMainnet() {
        return defaultFor(Net.MAINNET);
    }

    public static MessageSerializer defaultTestnet() {
        return defaultFor(Net.TESTNET3);
    }

    public static MessageSerializer defaultSTN() {
        return defaultFor(Net.STN);
    }

    public static MessageSerializer defaultRegtest() {
        return defaultFor(Net.REGTEST);
    }

    /**
     * Return the default serializeMode for this network. This is a shared serializeMode.
     * @return
     */
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
