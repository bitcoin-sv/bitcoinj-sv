/**
 * Copyright (c) 2020 Steve Shadders
 */
package io.bitcoinj.script;

import io.bitcoinj.exception.VerificationException;

public class ScriptParseException extends VerificationException {

    public ScriptParseException(String msg) {
        super(msg);
    }

    public ScriptParseException(Exception e) {
        super(e);
    }

    public ScriptParseException(String msg, Throwable t) {
        super(msg, t);
    }
}
