package org.bitcoinj.script;

import org.bitcoinj.core.ScriptException;

import java.util.Iterator;
import java.util.List;

import static org.bitcoinj.script.ScriptOpCodes.*;

public class SimpleScriptStream implements ScriptStream {

    private final List<ScriptChunk> chunks;
    private final int programSize;
    private int index = 0; //no point using a long since it's backed by an arraylist
    private long bytePos = 0;


    public SimpleScriptStream(Script script) {
        this.chunks = script.getChunks();
        programSize = script.getProgram().length;
    }

    @Override
    public boolean hasNext() {
        return index < chunks.size();
    }

    @Override
    public ScriptChunk next() {
        ScriptChunk chunk = chunks.get(index++);
        bytePos += chunkSize(chunk);
        return chunk;
    }

    @Override
    public long sizeOfNext() {
        ScriptChunk chunk = chunks.get(index + 1);
        return chunkSize(chunk);
    }

    @Override
    public long count() {
        return chunks.size();
    }

    @Override
    public long sizeInBytes() {
        return programSize;
    }

    @Override
    public long posInBytes() {
        return bytePos;
    }

    @Override
    public long posInElements() {
        return index;
    }

    private static long chunkSize(ScriptChunk chunk) {
        if (chunk.isPushData()) {
            int opcode = chunk.opcode;
            if (opcode >= 0 && opcode < OP_PUSHDATA1) {
                // Read some bytes of data, where how many is the opcode value itself.
                return 1 + opcode;
            } else if (opcode == OP_PUSHDATA1) {
                return 1 + 1 + chunk.data.length;
            } else if (opcode == OP_PUSHDATA2) {
                return 1 + 2 + chunk.data.length;
            } else if (opcode == OP_PUSHDATA4) {
                return 1 + 4 + chunk.data.length;
            }
        }
        //chunk is an op code
        return 1;
    }
}
