/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxInput;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxOutPoint;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

public class TxActor {

    public static boolean isCoinBase(Tx tx) {
        return tx.getInputs().size() == 1 && isCoinBase(tx.getInputs().get(0));
    }

    public static boolean isCoinBase(TxInput input) {
        TxOutPoint outpoint = input.getOutpoint();
        return outpoint.getHash().equals(Sha256Hash.ZERO_HASH) &&
                (outpoint.getIndex() & 0xFFFFFFFFL) == 0xFFFFFFFFL;  // -1 but all is serialized to the wire as unsigned int.
    }
}
