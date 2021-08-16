/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.exception;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 03/02/2021
 */
public final class BadPassphraseException extends Exception {
    public BadPassphraseException() { super(); }

    public BadPassphraseException(String message) {
        super(message);
    }

    public BadPassphraseException(Throwable t) {
        super(t);
    }

    public BadPassphraseException(String message, Throwable t) {
        super(message, t);
    }
}