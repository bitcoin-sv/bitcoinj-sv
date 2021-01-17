///*
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package io.bitcoinj.core.listeners;
//
//import io.bitcoinj.chain_legacy.SPVBlockChain_legacy;
//import io.bitcoinj.chain_legacy.StoredBlock_legacy;
//import io.bitcoinj.core.*;
//import io.bitcoinj.exception.VerificationException;
//import io.bitcoinj.bitcoin.api.extended.ChainInfoReadOnly;
//import io.bitcoinj.msg.protocol.Transaction;
//
//import java.util.*;
//
///**
// * For backwards compatibility only. Implements the block chain listener interfaces. Use the more specific interfaces
// * instead.
// */
//@Deprecated
//public class AbstractBlockChainListener implements BlockChainListener {
//    @Override
//    public void notifyNewBestBlock(ChainInfoReadOnly block) throws VerificationException {
//    }
//
//    @Override
//    public void reorganize(ChainInfoReadOnly splitPoint, List<ChainInfoReadOnly> oldBlocks, List<ChainInfoReadOnly> newBlocks) throws VerificationException {
//    }
//
//    @Override
//    public void receiveFromBlock(Transaction tx, StoredBlock_legacy block, SPVBlockChain_legacy.NewBlockType blockType, int relativityOffset) throws VerificationException {
//    }
//
//    @Override
//    public boolean notifyTransactionIsInBlock(Sha256Hash txHash, StoredBlock_legacy block, SPVBlockChain_legacy.NewBlockType blockType, int relativityOffset) throws VerificationException {
//        return false;
//    }
//}
