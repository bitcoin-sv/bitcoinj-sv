/**
 * Copyright (c) 2014 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.tools;

import org.bitcoinj.msg.bitcoin.api.base.FullBlock;

import java.io.File;

/**
 * @author Steve Shadders
 */
public interface BlockParsedListener {
    void onBlockParsed(FullBlock block, int numParsed, File currentFile, long start, long len);
    default void onNewFile(File currentFile, int fileNum) {}
    void onComplete(long timeToProcess);
}
