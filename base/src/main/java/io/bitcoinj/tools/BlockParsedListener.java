/**
 * Copyright (c) 2014 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.tools;

import io.bitcoinj.bitcoin.api.base.FullBlock;

import java.io.File;
import java.io.IOException;

/**
 * @author Steve Shadders
 */
public interface BlockParsedListener {
    void onBlockParsed(FullBlock block, int numParsed, File currentFile, long start, long len);
    default void onNewFile(File currentFile, int fileNum) {}
    void onComplete(long timeToProcess) throws IOException;
}