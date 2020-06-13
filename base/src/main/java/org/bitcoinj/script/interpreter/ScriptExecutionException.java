/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */


package org.bitcoinj.script.interpreter;

import org.bitcoinj.exception.VerificationException;

@SuppressWarnings("serial")
public class ScriptExecutionException extends VerificationException {

    private Interpreter.ScriptExecutionState state;

    public ScriptExecutionException(Interpreter.ScriptExecutionState state, String msg) {
        super(msg);
        this.state = state;
    }

    public ScriptExecutionException(String msg) {
        super(msg);
        state = (Interpreter.ScriptExecutionState) Interpreter.SCRIPT_STATE_THREADLOCAL.get();
    }

    public Interpreter.ScriptExecutionState getState() {
        return state;
    }
}
