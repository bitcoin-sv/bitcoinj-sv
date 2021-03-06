/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.script.interpreter;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.core.Coin;
import io.bitcoinsv.bitcoinjsv.script.ScriptChunk;
import io.bitcoinsv.bitcoinjsv.script.ScriptVerifyFlag;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ScriptExecutionState {

    public Tx txContainingThis;
    public Coin value;

    public ScriptStack stack;
    public List<StackItem> stackPopped;

    public ScriptStack altStack;
    public List<StackItem> altStackPopped;

    public LinkedList<Boolean> ifStack;

    public ScriptStream script;

    public List<ScriptChunk> executedOpCodes = new ArrayList<>();

    public int opCount;
    public ScriptChunk lastOpCode;
    public ScriptChunk currentOpCode;
    public int currentOpCodeIndex = 0;
    public Set<ScriptVerifyFlag> verifyFlags;
    public boolean initialStackStateKnown;

    public Tx getTxContainingThis() {
        return txContainingThis;
    }

    public Coin getValue() {
        return value;
    }

    public ScriptStack getStack() {
        return stack;
    }

    public List<StackItem> getStackPopped() {
        return stackPopped;
    }

    public ScriptStack getAltStack() {
        return altStack;
    }

    public List<StackItem> getAltStackPopped() {
        return altStackPopped;
    }

    public LinkedList<Boolean> getIfStack() {
        return ifStack;
    }

    public ScriptStream getScript() {
        return script;
    }

    public List<ScriptChunk> getExecutedOpCodes() {
        return executedOpCodes;
    }

    public int getOpCount() {
        return opCount;
    }

    public ScriptChunk getLastOpCode() {
        return lastOpCode;
    }

    public ScriptChunk getCurrentOpCode() {
        return currentOpCode;
    }

    public int getCurrentOpCodeIndex() {
        return currentOpCodeIndex;
    }

    public Set<ScriptVerifyFlag> getVerifyFlags() {
        return verifyFlags;
    }

    public boolean isInitialStackStateKnown() {
        return initialStackStateKnown;
    }
}
