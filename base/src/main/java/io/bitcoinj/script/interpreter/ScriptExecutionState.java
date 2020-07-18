/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.script.interpreter;

import io.bitcoinj.script.ScriptChunk;
import io.bitcoinj.script.ScriptVerifyFlag;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ScriptExecutionState {
    public ScriptStack stack;
    public List<StackItem> stackPopped;
    public ScriptStack altStack;
    public List<StackItem> altStackPopped;
    public LinkedList<Boolean> ifStack;
    public ScriptStream script;
    public int opCount;
    public ScriptChunk lastOpCode;
    public ScriptChunk currentOpCode;
    public int currentOpCodeIndex = 0;
    public Set<ScriptVerifyFlag> verifyFlags;
    public boolean initialStackStateKnown;
}
