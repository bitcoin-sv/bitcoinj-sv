/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.msg;

import io.bitcoinj.bitcoin.api.base.*;
import io.bitcoinj.bitcoin.bean.base.*;
import io.bitcoinj.msg.protocol.*;

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
