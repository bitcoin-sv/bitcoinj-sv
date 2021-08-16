/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.bitcoin.bean.validator;

import io.bitcoinj.bitcoin.api.base.Hashable;
import io.bitcoinj.bitcoin.api.base.Tx;
import io.bitcoinj.exception.VerificationException;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 02/02/2021
 */
public interface IBeanValidator<T extends Hashable> {

    void validate(Tx txBean) throws VerificationException;
}
