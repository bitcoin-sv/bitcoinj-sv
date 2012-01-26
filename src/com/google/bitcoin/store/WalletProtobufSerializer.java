/**
 * Copyright 2012 Google Inc.
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

package com.google.bitcoin.store;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WalletTransaction;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;

import org.bitcoinj.wallet.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Serialize and de-serialize a wallet to a protobuf stream.
 * 
 * @author Miron Cuperman
 */
public class WalletProtobufSerializer {
    private static final Logger log = LoggerFactory.getLogger(WalletProtobufSerializer.class);

    // Used for de-serialization
    private Map<ByteString, Transaction> txMap;
    
    public WalletProtobufSerializer() {
        txMap = new HashMap<ByteString, Transaction>();
    }
    
    public static void writeWallet(Wallet wallet, OutputStream output) throws IOException {
        Protos.Wallet walletProto = walletToProto(wallet);
        
        walletProto.writeTo(output);
    }

    public static String walletToText(Wallet wallet) {
        Protos.Wallet walletProto = walletToProto(wallet);
        
        return TextFormat.printToString(walletProto);
    }

    public static Protos.Wallet walletToProto(Wallet wallet) {
        Protos.Wallet.Builder walletBuilder = Protos.Wallet.newBuilder();
        walletBuilder
            .setNetworkIdentifier(wallet.getNetworkParameters().getId())
            //.setLastSeenBlockHash(null)  // TODO
            ;
        for (WalletTransaction wtx : wallet.getWalletTransactions()) {
            Protos.Transaction txProto = makeTxProto(wtx);
            walletBuilder.addTransaction(txProto);
        }
        
        for (ECKey key : wallet.getKeys()) {
            walletBuilder.addKey(
                    Protos.Key.newBuilder()
                        // .setCreationTimestamp() TODO
                        // .setLabel() TODO
                        .setType(Protos.Key.Type.ORIGINAL)
                        .setPrivateKey(ByteString.copyFrom(key.getPrivKeyBytes()))
                        .setPublicKey(ByteString.copyFrom(key.getPubKey()))
                        );
        }
        return walletBuilder.build();
    }
    
    private static Protos.Transaction makeTxProto(WalletTransaction wtx) {
        Transaction tx = wtx.getTransaction();
        Protos.Transaction.Builder txBuilder = Protos.Transaction.newBuilder();
        
        txBuilder
            .setPool(Protos.Transaction.Pool.valueOf(wtx.getPool().getValue()))
            .setHash(ByteString.copyFrom(tx.getHash().getBytes()))
            .setVersion((int)tx.getVersion())
            ;

        if (tx.getUpdateTime() != null) {
            txBuilder.setUpdatedAt(tx.getUpdateTime().getTime());
        }
        
        if (tx.getLockTime() > 0) {
            txBuilder.setLockTime((int)tx.getLockTime());
        }
        
        // Handle inputs
        for (TransactionInput input : tx.getInputs()) {
            Protos.TransactionInput.Builder inputBuilder = Protos.TransactionInput.newBuilder()
                .setScriptBytes(ByteString.copyFrom(input.getScriptBytes()))
                .setTransactionOutPointHash(ByteString.copyFrom(
                    input.getOutpoint().getHash().getBytes()))
                .setTransactionOutPointIndex((int)input.getOutpoint().getIndex()); // FIXME
            if (input.hasSequence()) {
                inputBuilder.setSequence((int)input.getSequence());
            }
            txBuilder.addTransactionInput(inputBuilder);
        }
        
        // Handle outputs
        for (TransactionOutput output : tx.getOutputs()) {
            Protos.TransactionOutput.Builder outputBuilder =
                    Protos.TransactionOutput.newBuilder()
                        .setScriptBytes(ByteString.copyFrom(output.getScriptBytes()))
                        .setValue(output.getValue().longValue());
            final TransactionInput spentBy = output.getSpentBy();
            if (spentBy != null) {
                Sha256Hash spendingHash = spentBy.getParentTransaction().getHash();
                outputBuilder
                    .setSpentByTransactionHash(hashToByteString(spendingHash))
                    .setSpentByTransactionIndex(
                            spentBy.getParentTransaction().getInputs().indexOf(spentBy));
            }
            txBuilder.addTransactionOutput(outputBuilder);
        }
        
        // Handle which blocks tx was seen in
        if (tx.getAppearsInHashes() != null) {
            for (Sha256Hash hash : tx.getAppearsInHashes()) {
                txBuilder.addBlockHash(hashToByteString(hash));
            }
        }
        
        if (tx.hasConfidence()) {
            TransactionConfidence confidence = tx.getConfidence();
            Protos.TransactionConfidence.Builder confidenceBuilder =
                Protos.TransactionConfidence.newBuilder();
            
            writeConfidence(txBuilder, confidence, confidenceBuilder);
        }
        
        return txBuilder.build();
    }

    private static void writeConfidence(
            Protos.Transaction.Builder txBuilder,
            TransactionConfidence confidence,
            Protos.TransactionConfidence.Builder confidenceBuilder) {
        confidenceBuilder.setType(
                Protos.TransactionConfidence.Type.valueOf(confidence.getConfidenceType().getValue()));
        if (confidence.getConfidenceType() == ConfidenceType.BUILDING) {
            confidenceBuilder.setAppearedAtHeight(confidence.getAppearedAtChainHeight());
        }
        if (confidence.getConfidenceType() == ConfidenceType.OVERRIDDEN_BY_DOUBLE_SPEND) {
            Sha256Hash overridingHash = confidence.getOverridingTransaction().getHash();
            confidenceBuilder.setOverridingTransaction(hashToByteString(overridingHash));
        }
        
        txBuilder.setConfidence(confidenceBuilder);
    }

    private static ByteString hashToByteString(Sha256Hash hash) {
        return ByteString.copyFrom(hash.getBytes());
    }

    public static Wallet readWallet(InputStream input, NetworkParameters params)
            throws IOException {
        WalletProtobufSerializer serializer = new WalletProtobufSerializer();
        Protos.Wallet walletProto = Protos.Wallet.parseFrom(input);
        if (!params.getId().equals(walletProto.getNetworkIdentifier()))
            throw new IllegalArgumentException(
                    "Trying to read a wallet with a different network id " +
                    walletProto.getNetworkIdentifier());
        
        Wallet wallet = new Wallet(params);
        
        // Read all keys
        for (Protos.Key keyProto : walletProto.getKeyList()) {
            if (keyProto.getType() != Protos.Key.Type.ORIGINAL) {
                throw new IllegalArgumentException("Unknown key type in wallet");
            }
            if (!keyProto.hasPrivateKey()) {
                throw new IllegalArgumentException("Don't know how to handle pubkey-only keys");
            }
            
            byte[] pubKey = keyProto.hasPublicKey() ? keyProto.getPublicKey().toByteArray() : null;
            wallet.addKey(new ECKey(keyProto.getPrivateKey().toByteArray(), pubKey));
        }
        
        // Read all transactions and create outputs
        for (Protos.Transaction txProto : walletProto.getTransactionList()) {
            serializer.readTransaction(txProto, params);
        }

        // Create transactions inputs pointing to transactions
        for (Protos.Transaction txProto : walletProto.getTransactionList()) {
            serializer.connectTransactionInputs(txProto, params);
        }
        
        // Update transaction outputs to point to inputs that spend them
        for (Protos.Transaction txProto : walletProto.getTransactionList()) {
            WalletTransaction wtx = serializer.connectTransactionOutputs(txProto, params);
            wallet.addWalletTransaction(wtx);
        }
        
        for (Protos.Extension extProto : walletProto.getExtensionList()) {
            if (extProto.getMandatory()) {
                throw new IllegalArgumentException("Did not understand a mandatory extension in the wallet");
            }
        }
        
        return wallet;
    }


    private void readTransaction(Protos.Transaction txProto, NetworkParameters params) {
        Transaction tx = 
            new Transaction(params, txProto.getVersion(),
                    new Sha256Hash(txProto.getHash().toByteArray()));
        if (txProto.hasUpdatedAt())
            tx.setUpdateTime(new Date(txProto.getUpdatedAt()));
        
        for (Protos.TransactionOutput outputProto :
            txProto.getTransactionOutputList()) {
            TransactionOutput output = new TransactionOutput(params, tx,
                    BigInteger.valueOf(outputProto.getValue()),
                    outputProto.getScriptBytes().toByteArray());
            tx.addOutput(output);
        }

        if (txMap.containsKey(ByteString.copyFrom(tx.getHash().getBytes()))) {
            throw new RuntimeException("Transaction " + tx.getHashAsString() + " appears twice");
        }
        
        for (ByteString blockHash : txProto.getBlockHashList()) {
            tx.addBlockAppearance(new Sha256Hash(blockHash.toByteArray()));
        }

        if (txProto.hasLockTime()) {
            tx.setLockTime(txProto.getLockTime());
        }
        
        txMap.put(txProto.getHash(), tx);
    }

    private void connectTransactionInputs(Protos.Transaction txProto, NetworkParameters params) {
        Transaction tx = txMap.get(txProto.getHash());
        for (Protos.TransactionInput transactionInput : txProto.getTransactionInputList()) {
            TransactionInput input =
                    new TransactionInput(params, tx,
                            transactionInput.getScriptBytes().toByteArray(),
                            new TransactionOutPoint(params,
                                    transactionInput.getTransactionOutPointIndex(),
                                    new Sha256Hash(transactionInput.getTransactionOutPointHash().toByteArray())
                                    )
                            );
            if (transactionInput.hasSequence()) {
                input.setSequence(transactionInput.getSequence());
            }
            tx.addInput(input);
        }
    }

    private WalletTransaction connectTransactionOutputs(
            org.bitcoinj.wallet.Protos.Transaction txProto, NetworkParameters params) {
        Transaction tx = txMap.get(txProto.getHash());
        WalletTransaction.Pool pool =
                WalletTransaction.Pool.valueOf(txProto.getPool().getNumber());
        for (int i = 0 ; i < tx.getOutputs().size() ; i++) {
            TransactionOutput output = tx.getOutputs().get(i);
            final Protos.TransactionOutput transactionOutput =
                    txProto.getTransactionOutput(i);
            if (transactionOutput.hasSpentByTransactionHash()) {
                Transaction spendingTx =
                    txMap.get(transactionOutput.getSpentByTransactionHash());
                final int spendingIndex = transactionOutput.getSpentByTransactionIndex();
                output.markAsSpent(spendingTx.getInputs().get(spendingIndex));
            }
        }
        
        if(txProto.hasConfidence()) {
            Protos.TransactionConfidence confidenceProto = txProto.getConfidence();
            TransactionConfidence confidence = tx.getConfidence();
            readConfidence(tx, confidenceProto, confidence);
        }

        return new WalletTransaction(pool, tx);
    }

    private void readConfidence(
            Transaction tx, Protos.TransactionConfidence confidenceProto,
            TransactionConfidence confidence) {
        // We are lenient here because tx confidence is not an essential part of the wallet.
        // If the tx has an unknown type of confidence, ignore.
        if (!confidenceProto.hasType()) {
            log.warn("Unknown confidence type for tx {}", tx.getHashAsString());
            return;
        }
        ConfidenceType confidenceType =
            TransactionConfidence.ConfidenceType.valueOf(confidenceProto.getType().getNumber());
        confidence.setConfidenceType(confidenceType);
        if (confidenceProto.hasAppearedAtHeight()) {
            if (confidence.getConfidenceType() != ConfidenceType.BUILDING) {
                log.warn("Have appearedAtHeight but not BUILDING for tx {}", tx.getHashAsString());
                return;
            }
            confidence.setAppearedAtChainHeight(confidenceProto.getAppearedAtHeight());
        }
        if (confidenceProto.hasOverridingTransaction()) {
            if (confidence.getConfidenceType() != ConfidenceType.OVERRIDDEN_BY_DOUBLE_SPEND) {
                log.warn("Have overridingTransaction but not OVERRIDDEN for tx {}", tx.getHashAsString());
                return;
            }
            Transaction overridingTransaction =
                txMap.get(confidenceProto.getOverridingTransaction());
            if (overridingTransaction == null) {
                log.warn("Have overridingTransaction that is not in wallet for tx {}", tx.getHashAsString());
                return;
            }
            confidence.setOverridingTransaction(overridingTransaction);
        }
    }
}
