package org.bitcoinj.script;

import java.util.Iterator;

/**
 * An interface that acts as an iterator over script element.  This allows the script engine to see a simple view
 * that might be backed by a fetch-on-demand implementation of script retriever.
 */
public interface ScriptStream extends Iterator<ScriptChunk> {

    /**
     * @return true if there are more elements in the stream
     */
    public boolean hasNext();

    /**
     * @return the next ScriptChunk in the stream
     */
    public ScriptChunk next();

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
}
