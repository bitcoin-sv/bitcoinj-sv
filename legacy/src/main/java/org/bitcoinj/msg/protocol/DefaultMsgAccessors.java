/**
 * Copyright (c) 2020 Steve Shadders
 */
package org.bitcoinj.msg.protocol;

import org.bitcoinj.params.Net;

/**
 * Accessor class for package private constructors and methods
 * Should be removed after refactoring.
 */
public class DefaultMsgAccessors {

    public static Block newBlock(Net net, long version) {
        return new Block(net, version);
    }

    public static TransactionInput newTransactionInput(Net net, Transaction parentTransaction, TransactionOutput output) {
        return new TransactionInput(net, parentTransaction, output);
    }

//    public static void bitcoinSerializeToStream(Message msg, OutputStream stream) throws IOException {
//        msg.bitcoinSerializeToStream(stream);
//    }

}
