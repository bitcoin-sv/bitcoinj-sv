/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */


package io.bitcoinj.script.interpreter;

import io.bitcoinj.exception.VerificationException;

@SuppressWarnings("serial")
public class ScriptExecutionException extends VerificationException {

    private ScriptExecutionState state;

    public ScriptExecutionException(ScriptExecutionState state, String msg) {
        super(appendDebugMessage(state, msg));
        this.state = state;
    }

    public ScriptExecutionException(String msg) {
        super(appendDebugMessage(null, msg));
        state = (ScriptExecutionState) Interpreter.SCRIPT_STATE_THREADLOCAL.get();
    }

    public ScriptExecutionState getState() {
        return state;
    }

    private static String appendDebugMessage(ScriptExecutionState state, String msg) {
        if (state == null)
            return msg;
        String contextString = "";
        if (state.currentOpCode != null && state.currentOpCode.context != null) {
            contextString = " - " + state.currentOpCode.context.toString();
        }
        return msg + contextString;
    }
}
