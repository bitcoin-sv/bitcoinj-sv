/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.bitcoin.bean.validator;

import io.bitcoinj.bitcoin.TxActor;
import io.bitcoinj.bitcoin.api.base.*;
import io.bitcoinj.bitcoin.bean.base.TxBean;
import io.bitcoinj.core.Coin;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.params.TxParams;

import java.util.HashSet;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 02/02/2021
 */
public class TxBeanValidator implements IBeanValidator<Tx> {

    private NetworkParameters networkParameters;

    public TxBeanValidator(NetworkParameters networkParameters){
        this.networkParameters = networkParameters;
    }

    @Override
    public void validate(Tx txBean) throws VerificationException {
            if (txBean.getInputs().size() == 0 || txBean.getOutputs().size() == 0)
                throw new VerificationException.EmptyInputsOrOutputs();
            if (txBean.getMessageSize() > TxParams.MAX_TRANSACTION_SIZE_PARAM)
                throw new VerificationException.LargerThanMaxTransactionSize();

            Coin valueOut = Coin.ZERO;
            HashSet<TxOutPoint> outpoints = new HashSet<>();
            for (TxInput input : txBean.getInputs()) {
                if (outpoints.contains(input.getOutpoint()))
                    throw new VerificationException.DuplicatedOutPoint();
                outpoints.add(input.getOutpoint());
            }
            try {
                for (TxOutput output : txBean.getOutputs()) {
                    if (output.getValue().signum() < 0)    // getValue() can throw IllegalStateException
                        throw new VerificationException.NegativeValueOutput();
                    valueOut = valueOut.add(output.getValue());
                    if (networkParameters.hasMaxMoney() && valueOut.compareTo(networkParameters.getMaxMoney()) > 0)
                        throw new IllegalArgumentException();
                }
            } catch (IllegalStateException e) {
                throw new VerificationException.ExcessiveValue();
            } catch (IllegalArgumentException e) {
                throw new VerificationException.ExcessiveValue();
            }

            if (TxActor.isCoinBase(txBean)) {
                if (txBean.getInputs().get(0).getScriptBytes().length < 2 || txBean.getInputs().get(0).getScriptBytes().length > 100)
                    throw new VerificationException.CoinbaseScriptSizeOutOfRange();
            } else {
                for (TxInput input : txBean.getInputs())
                    if (TxActor.isCoinBase(input))
                        throw new VerificationException.UnexpectedCoinbaseInput();
            }
        }


}
