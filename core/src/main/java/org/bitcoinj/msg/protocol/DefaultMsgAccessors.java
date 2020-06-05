package org.bitcoinj.msg.protocol;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.msg.protocol.Transaction;
import org.bitcoinj.msg.protocol.TransactionInput;
import org.bitcoinj.msg.protocol.TransactionOutput;

/**
 * Accessor class for package private constructors and methods
 * Should be removed after refactoring.
 */
public class DefaultMsgAccessors {

    public static Block newBlock(NetworkParameters params, long version) {
        return new Block(params, version);
    }

    public static TransactionInput newTransactionInput(NetworkParameters params, Transaction parentTransaction, TransactionOutput output) {
        return new TransactionInput(params, parentTransaction, output);
    }

//    public static void bitcoinSerializeToStream(Message msg, OutputStream stream) throws IOException {
//        msg.bitcoinSerializeToStream(stream);
//    }

}
