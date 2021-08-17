/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.script.interpreter;

import io.bitcoinsv.bitcoinjsv.script.ScriptChunk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * An interface that acts as an iterator over script element.  This allows the script engine to see a simple view
 * that might be backed by a fetch-on-demand implementation of script retriever.
 */
public interface ScriptStream extends Iterator<ScriptChunk>, Iterable<ScriptChunk>, Cloneable {

    public ScriptStream clone();

    /**
     * @return true if there are more elements in the stream
     */
    public boolean hasNext();

    /**
     * @return the next ScriptChunk in the stream
     */
    public ScriptChunk next();

    /**
     * @return chunk index of the next element
     */
    public int chunkIndex();

    /**
     * @return byte index of the next element
     */
    public long byteIndex();

    /**
     * @return byte index of the last encountered OP_CODESEPERATOR
     */
    public long getLastCodeSepBytePos();

    /**
     * @return chunk index of the last encountered OP_CODESEPERATOR
     */
    public int getLastCodeSepIndex();

    /**
     * Advances the internal pointer to the given ScriptChunk index
     * @param chunkIndex
     */
    default void advanceTo(int chunkIndex) {
        while (chunkIndex() < chunkIndex) {
            next();
        }
    }

    /**
     * Get the program from a specified point. Usually used to get connectedBytes from last CODESEPERATOR for signature verification.
     * @param chunkIndex
     * @return
     */
    default byte[] getProgramFrom(int chunkIndex) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ScriptStream clone = clone();
        clone.advanceTo(chunkIndex);
        while (clone.hasNext()) {
            ScriptChunk chunk = clone.next();
            try {
                chunk.write(bos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return bos.toByteArray();
    }

    /**
     * @return size (in bytes) of the next element including op_code + data if present.
     */
    public long sizeOfNext();

    /**
     * Number of elements in the fully expanded script
     * @return
     */
    public long count();

    /**
     * @return total size of the fully expanded script in bytes
     */
    public long sizeInBytes();

    /**
     * @return byte index of the next called ScriptChunk (will return zero before next() has been called)
     */
    public long posInBytes();

    /**
     * @return positional index of the next called ScriptChunk (will return zero before next() has been called)
     */
    public long posInElements();

    @Override
    default Iterator<ScriptChunk> iterator() {
        return this;
    }

    @Override
    default void forEach(Consumer<? super ScriptChunk> action) {
        for (ScriptChunk chunk: clone())
            action.accept(chunk);
    }

    @Override
    default Spliterator<ScriptChunk> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }
}
