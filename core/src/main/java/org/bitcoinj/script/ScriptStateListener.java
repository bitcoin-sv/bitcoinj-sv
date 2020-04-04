package org.bitcoinj.script;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * A ScriptStateListener can be passed during script execution to allow visibility into the internal state of the script at each stage of execution.
 *
 * Note that the stacks are set as local variables in the setup stage as unModifiable lists.  However they are backed by the live list in the
 * script engine and as such will update on each call to the listener.  Also important to note that although the lists themselves cannot
 * be modified the elements are byte arrays and as such could be modified.  Here be dragons!
 *
 * This class is not recommended for real world use.  Only for testing and debugging scripts.
 *
 * See the tools project for an example implementation InteractiveScriptStateListener
 *
 * Created by shadders on 7/02/18.
 */
public abstract class ScriptStateListener {

    private Transaction txContainingThis;
    private long index;
    private Script script;
    private List<byte[]> stack;
    private List<byte[]> altstack;
    private List<Boolean> ifStack;
    private Coin value;
    private Set<Script.VerifyFlag> verifyFlags;

    private int chunkIndex;
    private ScriptChunk currentChunk;
    private List<ScriptChunk> scriptChunks;


    void setInitialState(@Nullable Transaction txContainingThis, long index,
                               Script script, List<byte[]> stack, List<byte[]> altstack, List<Boolean> ifStack, Coin value, Set<Script.VerifyFlag> verifyFlags) {
        this.chunkIndex = -1;
        this.txContainingThis = txContainingThis;
        this.index = index;
        this.script = script;
        this.stack = stack;
        this.altstack = altstack;
        this.ifStack = ifStack;
        this.value = value;
        this.verifyFlags = verifyFlags;

        this.scriptChunks = script.chunks;

    }

    void _onBeforeOpCodeExecuted(ScriptChunk chunk, boolean willExecute) {
        chunkIndex++;
        currentChunk = chunk;
        onBeforeOpCodeExecuted(willExecute);
    }

    /**
     * Called for all operations in the script.  The operation may not execute if it's inside a conditional branch.
     * @param willExecute true if the script engine will attempt execution.
     */
    public abstract void onBeforeOpCodeExecuted(boolean willExecute);

    /**
     * Called after execution of an op code and all internal state is updated.  Note that this may not get called for all op codes in the script.
     * This will only be called if the operation does not fail and the operation is inside an executed branch of code.
     */
    public abstract void onAfterOpCodeExectuted();

    /**
     * Used in conjunction with Script.executeDebugScript(...).  This will be called if any ScriptExceptions are thrown before rethrowing the exception.
      * @param exception
     */
    public abstract void onExceptionThrown(ScriptException exception);

    /**
     * Called at the very end of the script.  If this method is called the script has completed successfuly.
     */
    public abstract void onScriptComplete();

    public Transaction getTxContainingThis() {
        return txContainingThis;
    }

    public long getIndex() {
        return index;
    }

    public Script getScript() {
        return script;
    }

    public List<byte[]> getStack() {
        return stack;
    }

    public List<byte[]> getAltstack() {
        return altstack;
    }

    public List<Boolean> getIfStack() {
        return ifStack;
    }

    public Coin getValue() {
        return value;
    }

    public Set<Script.VerifyFlag> getVerifyFlags() {
        return verifyFlags;
    }

    /**
     * @return The internally tracked index of the currently executing ScriptChunk.
     */
    public int getChunkIndex() {
        return chunkIndex;
    }

    /**
     *
     * @return the currently executing ScriptChunk
     */
    public ScriptChunk getCurrentChunk() {
        return currentChunk;
    }

    public List<ScriptChunk> getScriptChunks() {
        return scriptChunks;
    }
}
