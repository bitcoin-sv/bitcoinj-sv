package org.bitcoinj.msg;

import org.bitcoinj.msg.bitcoin.*;
import org.bitcoinj.msg.protocol.*;

public class Translate {


    public static Tx toTx(Transaction tx) {
        return new TxBean(tx.unsafeBitcoinSerialize());
    }

    public static Header toHeader(Block block) {
        return new HeaderBean(block.unsafeBitcoinSerialize());
    }

    public static FullBlock toFullBlock(Block block) {
        return new FullBlockBean(block.unsafeBitcoinSerialize());
    }

    public static Input toInput(TransactionInput input) {
        return new InputBean(input.unsafeBitcoinSerialize());
    }

    public static OutPoint toOutPoint(TransactionOutPoint outPoint) {
        return new OutPointBean(outPoint.unsafeBitcoinSerialize());
    }

    public static Output toOutput(TransactionOutput output) {
        return new OutputBean(output.unsafeBitcoinSerialize());
    }

}
