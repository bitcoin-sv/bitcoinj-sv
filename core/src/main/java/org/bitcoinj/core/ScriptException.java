/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.core;

import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.script.Interpreter;

@SuppressWarnings("serial")
public class ScriptException extends VerificationException {

    private Interpreter.ScriptExecutionState state;

    public ScriptException(Interpreter.ScriptExecutionState state, String msg) {
        super(msg);
        this.state = state;
    }

    public ScriptException(String msg) {
        super(msg);
        state = (Interpreter.ScriptExecutionState) Interpreter.SCRIPT_STATE_THREADLOCAL.get();
    }

//    public ScriptException(String msg, Exception e) {
//        super(msg, e);
//    }

    public Interpreter.ScriptExecutionState getState() {
        return state;
    }
}
