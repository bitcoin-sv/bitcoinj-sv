/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.msg;

import org.bitcoinj.msg.bitcoin.api.base.*;
import org.bitcoinj.msg.bitcoin.bean.base.*;
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

    public static TxInput toInput(TransactionInput input) {
        return new TxInputBean(input.unsafeBitcoinSerialize());
    }

    public static TxOutPoint toOutPoint(TransactionOutPoint outPoint) {
        return new TxOutPointBean(outPoint.unsafeBitcoinSerialize());
    }

    public static TxOutput toOutput(TransactionOutput output) {
        return new TxOutputBean(output.unsafeBitcoinSerialize());
    }

}
