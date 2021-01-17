/*
 * Copyright 2013 Google Inc.
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

package org.bitcoinj.moved.jni;

import io.bitcoinj.blockchain.AbstractBlockChain;
import io.bitcoinj.chain_legacy.StoredBlock_legacy;
import io.bitcoinj.core.*;

import java.util.List;
import io.bitcoinj.core.listeners.NewBestBlockListener;
import io.bitcoinj.core.listeners.ReorganizeListener;
import io.bitcoinj.core.listeners.TransactionReceivedInBlockListener;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.bitcoin.api.extended.ChainInfoReadOnly;
import io.bitcoinj.msg.protocol.Transaction;

/**
 * An event listener that relays events to a native C++ object. A pointer to that object is stored in
 * this class using JNI on the native side, thus several instances of this can point to different actual
 * native implementations.
 */
public class NativeBlockChainListener implements NewBestBlockListener, ReorganizeListener, TransactionReceivedInBlockListener {
    public long ptr;

    @Override
    public native void notifyNewBestBlock(ChainInfoReadOnly block) throws VerificationException;

    @Override
    public native void reorganize(ChainInfoReadOnly splitPoint, List<ChainInfoReadOnly> oldBlocks, List<ChainInfoReadOnly> newBlocks) throws VerificationException;

    @Override
    public native void receiveFromBlock(Transaction tx, StoredBlock_legacy block, AbstractBlockChain.NewBlockType blockType,
                                        int relativityOffset) throws VerificationException;

    @Override
    public native boolean notifyTransactionIsInBlock(Sha256Hash txHash, StoredBlock_legacy block, AbstractBlockChain.NewBlockType blockType,
                                                     int relativityOffset) throws VerificationException;
}
