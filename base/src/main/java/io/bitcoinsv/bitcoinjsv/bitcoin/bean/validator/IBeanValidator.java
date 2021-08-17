/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.bean.validator;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Hashable;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 02/02/2021
 */
public interface IBeanValidator<T extends Hashable> {

    void validate(Tx txBean) throws VerificationException;
}
