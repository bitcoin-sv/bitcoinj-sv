/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.tools;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.FullBlock;

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
