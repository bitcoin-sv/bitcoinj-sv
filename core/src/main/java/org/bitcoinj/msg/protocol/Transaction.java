/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
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

package org.bitcoinj.msg.protocol;

//import org.bitcoinj.chain.AbstractBlockChain;
import org.bitcoinj.chain.StoredBlock;
import org.bitcoinj.core.*;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.ecc.TransactionSignature;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.ChildMessage;
import org.bitcoinj.msg.Message;
import org.bitcoinj.msg.Translate;
import org.bitcoinj.params.SerializeMode;
import org.bitcoinj.params.Net;
import org.bitcoinj.script.*;
import org.bitcoinj.script.interpreter.ScriptExecutionException;
import org.bitcoinj.signers.TransactionSigner;
import org.bitcoinj.utils.ExchangeRate;
//import org.bitcoinj.moved.wallet.Wallet;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

import static org.bitcoinj.core.Utils.*;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>A transaction represents the movement of coins from some addresses to some other addresses. It can also represent
 * the minting of new coins. A Transaction object corresponds to the equivalent in the Bitcoin C++ implementation.</p>
 *
 * <p>Transactions are the fundamental atoms of Bitcoin and have many powerful features. Read
 * <a href="https://bitcoinj.github.io/working-with-transactions">"Working with transactions"</a> in the
 * documentation to learn more about how to use this class.</p>
 *
 * <p>All Bitcoin transactions are at risk of being reversed, though the risk is much less than with traditional payment
 * systems. Transactions have <i>confidence levels</i>, which help you decide whether to trust a transaction or not.
 * Whether to trust a transaction is something that needs to be decided on a case by case basis - a rule that makes
 * sense for selling MP3s might not make sense for selling cars, or accepting payments from a family member. If you
 * are building a wallet, how to present confidence to your users is something to consider carefully.</p>
 *
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class Transaction extends ChildMessage {
    /**
     * A comparator that can be used to sort transactions by their updateTime field. The ordering goes from most recent
     * into the past.
     */
    public static final Comparator<Transaction> SORT_TX_BY_UPDATE_TIME = new Comparator<Transaction>() {
        @Override
        public int compare(final Transaction tx1, final Transaction tx2) {
            final long time1 = tx1.getUpdateTime().getTime();
            final long time2 = tx2.getUpdateTime().getTime();
            final int updateTimeComparison = -(Longs.compare(time1, time2));
            //If time1==time2, compare by tx hash to make comparator consistent with equals
            return updateTimeComparison != 0 ? updateTimeComparison : tx1.getHash().compareTo(tx2.getHash());
        }
    };
    /** A comparator that can be used to sort transactions by their chain height. */
    public static final Comparator<Transaction> SORT_TX_BY_HEIGHT = new Comparator<Transaction>() {
        @Override
        public int compare(final Transaction tx1, final Transaction tx2) {
            final TransactionConfidence confidence1 = TxHelper.getConfidence(tx1);
            final int height1 = confidence1.getConfidenceType() == ConfidenceType.BUILDING
                    ? confidence1.getAppearedAtChainHeight() : BitcoinJ.BLOCK_HEIGHT_UNKNOWN;
            final TransactionConfidence confidence2 = TxHelper.getConfidence(tx2);
            final int height2 = confidence2.getConfidenceType() == ConfidenceType.BUILDING
                    ? confidence2.getAppearedAtChainHeight() : BitcoinJ.BLOCK_HEIGHT_UNKNOWN;
            final int heightComparison = -(Ints.compare(height1, height2));
            //If height1==height2, compare by tx hash to make comparator consistent with equals
            return heightComparison != 0 ? heightComparison : tx1.getHash().compareTo(tx2.getHash());
        }
    };
    private static final Logger log = LoggerFactory.getLogger(Transaction.class);

    /** Maximum transaction size imposed in Genesis upgrade */
    public static final int MAX_TRANSACTION_SIZE = 1000 * 1000 * 1000;

    // smaller size so tests don't take forever
    public static final int MAX_TRANSACTION_SIZE_FOR_TESTS = 100 * 1000 * 1000;

    /** How many bytes a transaction can be before it won't be relayed anymore. Currently 100kb. */
    public static final int MAX_STANDARD_TX_SIZE = 100000;


    public static final int CURRENT_VERSION = 2;
    public static final int MAX_STANDARD_VERSION = 2;
    public static final int FORKID_VERSION = 2; //Version 2 and above will require the new signature hash

    // These are bitcoin serialized.
    private long version;
    private List<TransactionInput> inputs;
    private List<TransactionOutput> outputs;

    private long lockTime;

    // This is either the time the transaction was broadcast as measured from the local clock, or the time from the
    // block in which it was included. Note that this can be changed by re-orgs so the wallet may update this field.
    // Old serialized transactions don't have this field, thus null is valid. It is used for returning an ordered
    // list of transactions from a wallet, which is helpful for presenting to users.
    private Date updatedAt;

    // This is an in memory helper only.
    private Sha256Hash hash;

    // Data about how confirmed this tx is. Serialized, may be null.
    @Nullable
    TransactionConfidence confidence;

    // Records a map of which blocks the transaction has appeared in (keys) to an index within that block (values).
    // The "index" is not a real index, instead the values are only meaningful relative to each other. For example,
    // consider two transactions that appear in the same block, t1 and t2, where t2 spends an output of t1. Both
    // will have the same block hash as a key in their appearsInHashes, but the counter would be 1 and 2 respectively
    // regardless of where they actually appeared in the block.
    //
    // If this transaction is not stored in the wallet, appearsInHashes is null.
    private Map<Sha256Hash, Integer> appearsInHashes;

    // Transactions can be encoded in a way that will use more bytes than is optimal
    // (due to VarInts having multiple encodings)
    // MAX_BLOCK_SIZE must be compared to the optimal encoding, not the actual encoding, so when parsing, we keep track
    // of the size of the ideal encoding in addition to the actual message size (which Message needs) so that Blocks
    // can properly keep track of optimal encoded size
    private int optimalEncodingMessageSize;

    /**
     * This enum describes the underlying reason the transaction was created. It's useful for rendering wallet GUIs
     * more appropriately.
     */
    public enum Purpose {
        /** Used when the purpose of a transaction is genuinely unknown. */
        UNKNOWN,
        /** Transaction created to satisfy a user payment request. */
        USER_PAYMENT,
        /** Transaction automatically created and broadcast in order to reallocate money from old to new keys. */
        KEY_ROTATION,
        /** Transaction that uses up pledges to an assurance contract */
        ASSURANCE_CONTRACT_CLAIM,
        /** Transaction that makes a pledge to an assurance contract. */
        ASSURANCE_CONTRACT_PLEDGE,
        /** Send-to-self transaction that exists just to create an output of the right size we can pledge. */
        ASSURANCE_CONTRACT_STUB,
        /** Raise fee, e.g. child-pays-for-parent. */
        RAISE_FEE,
        // In future: de/refragmentation, privacy boosting/mixing, etc.
        // When adding a value, it also needs to be added to wallet.proto, WalletProtobufSerialize.makeTxProto()
        // and WalletProtobufSerializer.readTransaction()!
    }

    private Purpose purpose = Purpose.UNKNOWN;

    /**
     * This field can be used by applications to record the exchange rate that was valid when the transaction happened.
     * It's optional.
     */
    @Nullable
    private ExchangeRate exchangeRate;

    /**
     * This field can be used to record the memo of the payment request that initiated the transaction. It's optional.
     */
    @Nullable
    private String memo;

    public Transaction(Net net) {
        super(net);
        version = 1;
        inputs = new ArrayList<TransactionInput>();
        outputs = new ArrayList<TransactionOutput>();
        // We don't initialize appearsIn deliberately as it's only useful for transactions stored in the wallet.
        setLength(8); // 8 for std fields
    }

    /**
     * Creates a transaction from the given serialized bytes, eg, from a block or a tx network message.
     */
    public Transaction(Net net, byte[] payloadBytes) throws ProtocolException {
        super(net, payloadBytes, 0);
    }

    /**
     * Creates a transaction by reading payload starting from offset bytes in. Length of a transaction is fixed.
     */
    public Transaction(Net net, byte[] payload, int offset) throws ProtocolException {
        super(net, payload, offset);
        // inputs/outputs will be created in parse()
    }

    /**
     * Creates a transaction by reading payload starting from offset bytes in. Length of a transaction is fixed.
     * @param net NetworkParameters object.
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param offset The location of the first payload byte within the array.
     * @param length The length of message if known.  Usually this is provided when deserializing of the wire
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    public Transaction(Net net, byte[] payload, int offset, @Nullable Message parent, SerializeMode serializeMode, int length)
            throws ProtocolException {
        super(net, payload, offset, parent, serializeMode, length);
    }

    /**
     * Creates a transaction by reading payload. Length of a transaction is fixed.
     */
    public Transaction(Net net, byte[] payload, @Nullable Message parent, SerializeMode serializeMode, int length)
            throws ProtocolException {
        super(net, payload, 0, parent, serializeMode, length);
    }

    /**
     * Returns the transaction hash as you see them in the block explorer.
     */
    @Override
    public Sha256Hash getHash() {
        if (hash == null) {
            hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(unsafeBitcoinSerialize()));
        }
        return hash;
    }

    /**
     * Used by BitcoinSerializer.  The serializeMode has to calculate a hash for checksumming so to
     * avoid wasting the considerable effort a set method is provided so the serializeMode can set it.
     *
     * No verification is performed on this hash.
     */
    public void setHash(Sha256Hash hash) {
        this.hash = hash;
    }

    public String getHashAsString() {
        return getHash().toString();
    }
    /**
     * Gets the sum of the inputs, regardless of who owns them.
     */
    public Coin getInputSum() {
        Coin inputTotal = Coin.ZERO;

        for (TransactionInput input: inputs) {
            Coin inputValue = input.getValue();
            if (inputValue != null) {
                inputTotal = inputTotal.add(inputValue);
            }
        }

        return inputTotal;
    }

    /**
     * Returns a map of block [hashes] which contain the transaction mapped to relativity counters, or null if this
     * transaction doesn't have that data because it's not stored in the wallet or because it has never appeared in a
     * block.
     */
    @Nullable
    public Map<Sha256Hash, Integer> getAppearsInHashes() {
        return appearsInHashes != null ? ImmutableMap.copyOf(appearsInHashes) : null;
    }

    /**
     * Convenience wrapper around getConfidence().getConfidenceType()
     * @return true if this transaction hasn't been seen in any block yet.
     */
    public boolean isPending() {
        return TxHelper.getConfidence(this).getConfidenceType() == TransactionConfidence.ConfidenceType.PENDING;
    }

    /**
     * <p>Puts the given block in the internal set of blocks in which this transaction appears. This is
     * used by the wallet to ensure transactions that appear on side chains are recorded properly even though the
     * block stores do not save the transaction data at all.</p>
     *
     * <p>If there is a re-org this will be called once for each block that was previously seen, to update which block
     * is the best chain. The best chain block is guaranteed to be called last. So this must be idempotent.</p>
     *
     * <p>Sets updatedAt to be the earliest valid block time where this tx was seen.</p>
     *
     * @param block     The {@link StoredBlock} in which the transaction has appeared.
     * @param bestChain whether to set the updatedAt timestamp from the block header (only if not already set)
     * @param relativityOffset A number that disambiguates the order of transactions within a block.
     */
    public void setBlockAppearance(StoredBlock block, boolean bestChain, int relativityOffset) {
        long blockTime = block.getHeader().getTime() * 1000;
        if (bestChain && (updatedAt == null || updatedAt.getTime() == 0 || updatedAt.getTime() > blockTime)) {
            updatedAt = new Date(blockTime);
        }

        addBlockAppearance(block.getHeader().getHash(), relativityOffset);

        if (bestChain) {
            TransactionConfidence transactionConfidence = TxHelper.getConfidence(this);
            // This sets type to BUILDING and depth to one.
            transactionConfidence.setAppearedAtChainHeight(block.getHeight());
        }
    }

    public void addBlockAppearance(final Sha256Hash blockHash, int relativityOffset) {
        if (appearsInHashes == null) {
            // TODO: This could be a lot more memory efficient as we'll typically only store one element.
            appearsInHashes = new TreeMap<Sha256Hash, Integer>();
        }
        appearsInHashes.put(blockHash, relativityOffset);
    }

    /**
     * Gets the sum of the outputs of the transaction. If the outputs are less than the inputs, it does not count the fee.
     * @return the sum of the outputs regardless of who owns them.
     */
    public Coin getOutputSum() {
        Coin totalOut = Coin.ZERO;

        for (TransactionOutput output: outputs) {
            totalOut = totalOut.add(output.getValue());
        }

        return totalOut;
    }

    /**
     * The transaction fee is the difference of the value of all inputs and the value of all outputs. Currently, the fee
     * can only be determined for transactions created by us.
     *
     * @return fee, or null if it cannot be determined
     */
    public Coin getFee() {
        Coin fee = Coin.ZERO;
        for (TransactionInput input : inputs) {
            if (input.getValue() == null)
                return null;
            fee = fee.add(input.getValue());
        }
        for (TransactionOutput output : outputs) {
            fee = fee.subtract(output.getValue());
        }
        return fee;
    }

    /**
     * Returns true if any of the outputs is marked as spent.
     */
    public boolean isAnyOutputSpent() {
        maybeParse();
        for (TransactionOutput output : outputs) {
            if (!output.isAvailableForSpending())
                return true;
        }
        return false;
    }

    /**
     * Returns the earliest time at which the transaction was seen (broadcast or included into the chain),
     * or the epoch if that information isn't available.
     */
    public Date getUpdateTime() {
        if (updatedAt == null) {
            // Older wallets did not store this field. Set to the epoch.
            updatedAt = new Date(0);
        }
        return updatedAt;
    }

    public void setUpdateTime(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * @deprecated Instead use SigHash.ANYONECANPAY.value or SigHash.ANYONECANPAY.byteValue() as appropriate.
     */
    public static final byte SIGHASH_ANYONECANPAY_VALUE = (byte) 0x80;

    @Override
    protected void unCache() {
        super.unCache();
        hash = null;
    }

    @Override
    protected void compactPayload() {
        if (length() != UNKNOWN_LENGTH && payload.length > length() * 2) {
            byte[] compactPayload = new byte[length()];
            System.arraycopy(payload, offset, compactPayload, 0, length());
            payload = compactPayload;
            offset = 0;
        }
    }

    @Override
    protected void parseLite() throws ProtocolException {

        //skip this if the length has been provided i.e. the tx is not part of a block
        if (serializeMode.isParseLazyMode() && length() == UNKNOWN_LENGTH) {
            //If length hasn't been provided this tx is probably contained within a block.
            //In parseRetain mode the block needs to know how long the transaction is
            //unfortunately this requires a fairly deep (though not total) parse.
            //This is due to the fact that transactions in the block's list do not include a
            //size header and inputs/outputs are also variable length due the contained
            //script so each must be instantiated so the scriptlength varint can be read
            //to calculate total length of the transaction.
            //We will still persist will this semi-light parsing because getting the lengths
            //of the various components gains us the ability to cache the backing bytearrays
            //so that only those subcomponents that have changed will need to be reserialized.

            //parse();
            //parsed = true;
            setLength(calcLength(payload, offset));
            cursor = offset + length();
        }
    }

    protected static int calcLength(byte[] buf, int offset) {
        VarInt varint;
        // jump past version (uint32)
        int cursor = offset + 4;

        int i;
        long scriptLen;

        varint = new VarInt(buf, cursor);
        long txInCount = varint.value;
        cursor += varint.getOriginalSizeInBytes();

        for (i = 0; i < txInCount; i++) {
            // 36 = length of previous_outpoint
            cursor += 36;
            varint = new VarInt(buf, cursor);
            scriptLen = varint.value;
            // 4 = length of sequence field (unint32)
            cursor += scriptLen + 4 + varint.getOriginalSizeInBytes();
        }

        varint = new VarInt(buf, cursor);
        long txOutCount = varint.value;
        cursor += varint.getOriginalSizeInBytes();

        for (i = 0; i < txOutCount; i++) {
            // 8 = length of tx value field (uint64)
            cursor += 8;
            varint = new VarInt(buf, cursor);
            scriptLen = varint.value;
            cursor += scriptLen + varint.getOriginalSizeInBytes();
        }
        // 4 = length of lock_time field (uint32)
        return cursor - offset + 4;
    }

    @Override
    protected void parse() throws ProtocolException {

        if (parsed)
            return;

        cursor = offset;
        version = readUint32();
        optimalEncodingMessageSize = 4;

        // First come the inputs.
        long numInputs = readVarInt();
        optimalEncodingMessageSize += VarInt.sizeOf(numInputs);
        inputs = new ArrayList<TransactionInput>((int) numInputs);
        for (long i = 0; i < numInputs; i++) {
            TransactionInput input = new TransactionInput(net, this, payload, cursor, serializeMode);
            inputs.add(input);
            long scriptLen = readVarInt(TransactionOutPoint.MESSAGE_LENGTH);
            optimalEncodingMessageSize += TransactionOutPoint.MESSAGE_LENGTH + VarInt.sizeOf(scriptLen) + scriptLen + 4;
            cursor += scriptLen + 4;
        }
        // Now the outputs
        long numOutputs = readVarInt();
        optimalEncodingMessageSize += VarInt.sizeOf(numOutputs);
        outputs = new ArrayList<>((int) numOutputs);
        for (long i = 0; i < numOutputs; i++) {
            TransactionOutput output = new TransactionOutput(net, this, payload, cursor, serializeMode);
            outputs.add(output);
            long scriptLen = readVarInt(8);
            optimalEncodingMessageSize += 8 + VarInt.sizeOf(scriptLen) + scriptLen;
            cursor += scriptLen;
        }
        lockTime = readUint32();
        optimalEncodingMessageSize += 4;
        setLength(cursor - offset);
    }

    public int getOptimalEncodingMessageSize() {
        if (optimalEncodingMessageSize != 0)
            return optimalEncodingMessageSize;
        maybeParse();
        if (optimalEncodingMessageSize != 0)
            return optimalEncodingMessageSize;
        optimalEncodingMessageSize = getMessageSize();
        return optimalEncodingMessageSize;
    }

    /**
     * The priority (coin age) calculation doesn't use the regular message size, but rather one adjusted downwards
     * for the number of inputs. The goal is to incentivise cleaning up the UTXO set with free transactions, if one
     * can do so.
     */
    public int getMessageSizeForPriorityCalc() {
        int size = getMessageSize();
        for (TransactionInput input : inputs) {
            // 41: min size of an input
            // 110: enough to cover a compressed pubkey p2sh redemption (somewhat arbitrary).
            int benefit = 41 + Math.min(110, input.getScriptSig().getProgram().length);
            if (size > benefit)
                size -= benefit;
        }
        return size;
    }

    /**
     * A coinbase transaction is one that creates a new coin. They are the first transaction in each block and their
     * value is determined by a formula that all implementations of Bitcoin share. In 2011 the value of a coinbase
     * transaction is 50 coins, but in future it will be less. A coinbase transaction is defined not only by its
     * position in a block but by the data in the inputs.
     */
    public boolean isCoinBase() {
        maybeParse();
        return inputs.size() == 1 && inputs.get(0).isCoinBase();
    }

    /**
     * A transaction is mature if it is either a building coinbase tx that is as deep or deeper than the required coinbase depth, or a non-coinbase tx.
     */
    public boolean isMature() {
        if (!isCoinBase())
            return true;

        if (TxHelper.getConfidence(this).getConfidenceType() != ConfidenceType.BUILDING)
            return false;

        return TxHelper.getConfidence(this).getDepthInBlocks() >= net.params().getSpendableCoinbaseDepth();
    }

//    @Override
//    public String toString() {
//        return toString(null);
//    }

    /**
     * A human readable version of the transaction useful for debugging. The format is not guaranteed to be stable.
     */
    public String toString() {
        if (!parsed)
            return "Unparsed transaction";
        StringBuilder s = new StringBuilder();
        s.append("  ").append(getHashAsString()).append('\n');
        if (updatedAt != null)
            s.append("  updated: ").append(Utils.dateTimeFormat(updatedAt)).append('\n');
        if (version != 1)
            s.append("  version ").append(version).append('\n');
        if (isTimeLocked()) {
            s.append("  time locked until ");
            if (lockTime < BitcoinJ.LOCKTIME_THRESHOLD) {
                s.append("block ").append(lockTime);
//                if (chain != null) {
//                    s.append(" (estimated to be reached at ")
//                            .append(Utils.dateTimeFormat(chain.estimateBlockTime((int) lockTime))).append(')');
//                }
            } else {
                s.append(Utils.dateTimeFormat(lockTime * 1000));
            }
            s.append('\n');
        }
        if (isOptInFullRBF()) {
            s.append("  opts into full replace-by-fee\n");
        }
        if (inputs.size() == 0) {
            s.append("  INCOMPLETE: No inputs!\n");
            return s.toString();
        }
        if (isCoinBase()) {
            String script;
            String script2;
            try {
                script = inputs.get(0).getScriptSig().toString();
                script2 = outputs.get(0).getScriptPubKey().toString();
            } catch (ScriptExecutionException e) {
                script = "???";
                script2 = "???";
            }
            s.append("     == COINBASE TXN (scriptSig ").append(script)
                .append(")  (scriptPubKey ").append(script2).append(")\n");
            return s.toString();
        }
        for (TransactionInput in : inputs) {
            s.append("     ");
            s.append("in   ");

            try {
                Script scriptSig = in.getScriptSig();
                s.append(scriptSig);
                if (in.getValue() != null)
                    s.append(" ").append(in.getValue().toFriendlyString());
                s.append("\n          ");
                s.append("outpoint:");
                final TransactionOutPoint outpoint = in.getOutpoint();
                s.append(outpoint.toString());
                final TransactionOutput connectedOutput = outpoint.getConnectedOutput();
                if (connectedOutput != null) {
                    Script scriptPubKey = connectedOutput.getScriptPubKey();
                    if (scriptPubKey.isSentToAddress() || scriptPubKey.isPayToScriptHash()) {
                        s.append(" hash160:");
                        s.append(Utils.HEX.encode(scriptPubKey.getPubKeyHash()));
                    }
                }
                if (in.hasSequence()) {
                    s.append("\n          sequence:").append(Long.toHexString(in.getSequenceNumber()));
                    if (in.isOptInFullRBF())
                        s.append(", opts into full RBF");
                }
            } catch (Exception e) {
                s.append("[exception: ").append(e.getMessage()).append("]");
            }
            s.append('\n');
        }
        for (TransactionOutput out : outputs) {
            s.append("     ");
            s.append("out  ");
            try {
                Script scriptPubKey = out.getScriptPubKey();
                s.append(scriptPubKey);
                s.append(" ");
                s.append(out.getValue().toFriendlyString());
                if (!out.isAvailableForSpending()) {
                    s.append(" Spent");
                }
                if (out.getSpentBy() != null) {
                    s.append(" by ");
                    s.append(out.getSpentBy().getParentTransaction().getHashAsString());
                }
            } catch (Exception e) {
                s.append("[exception: ").append(e.getMessage()).append("]");
            }
            s.append('\n');
        }
        final Coin fee = getFee();
        if (fee != null) {
            final int size = unsafeBitcoinSerialize().length;
            s.append("     fee  ").append(fee.multiply(1000).divide(size).toFriendlyString()).append("/kB, ")
                    .append(fee.toFriendlyString()).append(" for ").append(size).append(" bytes\n");
        }
        if (purpose != null)
            s.append("     prps ").append(purpose).append('\n');
        return s.toString();
    }

    /**
     * Removes all the inputs from this transaction.
     * Note that this also invalidates the length attribute
     */
    public void clearInputs() {
        unCache();
        for (TransactionInput input : inputs) {
            input.setParent(null);
        }
        inputs.clear();
        // You wanted to reserialize, right?
        this.setLength(this.unsafeBitcoinSerialize().length);
    }

    /**
     * Adds an input to this transaction that imports value from the given output. Note that this input is <i>not</i>
     * complete and after every input is added with {@code addInput()} and every output is added with
     * {@code addOutput()}, a {@link TransactionSigner} must be used to finalize the transaction and finish the inputs
     * off. Otherwise it won't be accepted by the network.
     * @return the newly created input.
     */
    public TransactionInput addInput(TransactionOutput from) {
        return addInput(new TransactionInput(net, this, from));
    }

    /**
     * Adds an input directly, with no checking that it's valid.
     * @return the new input.
     */
    public TransactionInput addInput(TransactionInput input) {
        unCache();
        input.setParent(this);
        inputs.add(input);
        adjustLength(inputs.size(), input.length());
        return input;
    }

    /**
     * Creates and adds an input to this transaction, with no checking that it's valid.
     * @return the newly created input.
     */
    public TransactionInput addInput(Sha256Hash spendTxHash, long outputIndex, Script script) {
        return addInput(new TransactionInput(net, this, script.getProgram(), new TransactionOutPoint(net, outputIndex, spendTxHash)));
    }

    /**
     * Removes all the outputs from this transaction.
     * Note that this also invalidates the length attribute
     */
    public void clearOutputs() {
        unCache();
        for (TransactionOutput output : outputs) {
            output.setParent(null);
        }
        outputs.clear();
        // You wanted to reserialize, right?
        this.setLength(this.unsafeBitcoinSerialize().length);
    }

    /**
     * Adds the given output to this transaction. The output must be completely initialized. Returns the given output.
     */
    public TransactionOutput addOutput(TransactionOutput to) {
        unCache();
        to.setParent(this);
        outputs.add(to);
        adjustLength(outputs.size(), to.length());
        return to;
    }

    /**
     * Creates an output based on the given address and value, adds it to this transaction, and returns the new output.
     */
    public TransactionOutput addOutput(Coin value, Address address) {
        return addOutput(new TransactionOutput(net, this, value, address));
    }

    /**
     * Creates an output that pays to the given pubkey directly (no address) with the given value, adds it to this
     * transaction, and returns the new output.
     */
    public TransactionOutput addOutput(Coin value, ECKey pubkey) {
        return addOutput(new TransactionOutput(net, this, value, pubkey));
    }

    /**
     * Creates an output that pays to the given script. The address and key forms are specialisations of this method,
     * you won't normally need to use it unless you're doing unusual things.
     */
    public TransactionOutput addOutput(Coin value, Script script) {
        return addOutput(new TransactionOutput(net, this, value, script.getProgram()));
    }


    /**
     * Calculates a signature that is valid for being inserted into the input at the given position. This is simply
     * a wrapper around calling {@link Transaction#hashForLegacySignature(Transaction, int, byte[], SigHash.Flags, boolean)}
     * followed by {@link ECKey#sign(Sha256Hash)} and then returning a new {@link TransactionSignature}. The key
     * must be usable for signing as-is: if the key is encrypted it must be decrypted first external to this method.
     *
     * @param inputIndex Which input to calculate the signature for, as an index.
     * @param key The private key used to calculate the signature.
     * @param redeemScript Byte-exact contents of the scriptPubKey that is being satisified, or the P2SH redeem script.
     * @param hashType Signing mode, see the enum for documentation.
     * @param anyoneCanPay Signing mode, see the SigHash enum for documentation.
     * @return A newly calculated signature object that wraps the r, s and sighash components.
     */
    public TransactionSignature calculateLegacySignature(int inputIndex, ECKey key,
                                                         byte[] redeemScript,
                                                         SigHash.Flags hashType, boolean anyoneCanPay) {
        Sha256Hash hash = hashForLegacySignature(this, inputIndex, redeemScript, hashType, anyoneCanPay);
        return new TransactionSignature(key.sign(hash), hashType, anyoneCanPay);
    }
    public TransactionSignature calculateForkIdSignature(
            int inputIndex,
            ECKey key,
            byte[] redeemScript,
            Coin value,
            SigHash.Flags hashType,
            boolean anyoneCanPay)
    {
        Sha256Hash hash = SigHash.hashForForkIdSignature(Translate.toTx(this), inputIndex, redeemScript, value, hashType, anyoneCanPay);
        return new TransactionSignature(key.sign(hash), hashType, anyoneCanPay, true);
    }
    /**
     * Calculates a signature that is valid for being inserted into the input at the given position. This is simply
     * a wrapper around calling {@link Transaction#hashForLegacySignature(Transaction, int, byte[], SigHash.Flags, boolean)}
     * followed by {@link ECKey#sign(Sha256Hash)} and then returning a new {@link TransactionSignature}.
     *
     * @param inputIndex Which input to calculate the signature for, as an index.
     * @param key The private key used to calculate the signature.
     * @param redeemScript The scriptPubKey that is being satisified, or the P2SH redeem script.
     * @param hashType Signing mode, see the enum for documentation.
     * @param anyoneCanPay Signing mode, see the SigHash enum for documentation.
     * @return A newly calculated signature object that wraps the r, s and sighash components.
     */
    public TransactionSignature calculateLegacySignature(int inputIndex, ECKey key,
                                                         Script redeemScript,
                                                         SigHash.Flags hashType, boolean anyoneCanPay) {
        Sha256Hash hash = hashForLegacySignature(this, inputIndex, redeemScript.getProgram(), hashType, anyoneCanPay);
        return new TransactionSignature(key.sign(hash), hashType, anyoneCanPay);
    }
    public TransactionSignature calculateForkIdSignature(
            int inputIndex,
            ECKey key,
            Script redeemScript,
            Coin value,
            SigHash.Flags hashType,
            boolean anyoneCanPay)
    {
        Sha256Hash hash = SigHash.hashForForkIdSignature(Translate.toTx(this), inputIndex, redeemScript.getProgram(), value, hashType, anyoneCanPay);
        return new TransactionSignature(key.sign(hash), hashType, anyoneCanPay, true);
    }

    /**
     * <p>Calculates a signature hash, that is, a hash of a simplified form of the transaction. How exactly the transaction
     * is simplified is specified by the type and anyoneCanPay parameters.</p>
     *
     * <p>This is a low level API and when using the regular { Wallet} class you don't have to call this yourself.
     * When working with more complex transaction types and contracts, it can be necessary. When signing a P2SH output
     * the redeemScript should be the script encoded into the scriptSig field, for normal transactions, it's the
     * scriptPubKey of the output you're signing for.</p>
     *
     * @param transaction
     * @param inputIndex input the signature is being calculated for. Tx signatures are always relative to an input.
     * @param redeemScript the bytes that should be in the given input during signing.
     * @param type Should be SigHash.ALL
     * @param anyoneCanPay should be false.
     */
    public static Sha256Hash hashForLegacySignature(Transaction transaction, int inputIndex, byte[] redeemScript,
                                                    SigHash.Flags type, boolean anyoneCanPay) {
        byte sigHashType = (byte) TransactionSignature.calcSigHashValue(type, anyoneCanPay);
        return SigHash.hashForLegacySignature(Translate.toTx(transaction), inputIndex, redeemScript, sigHashType);
    }

    /**
     * <p>Calculates a signature hash, that is, a hash of a simplified form of the transaction. How exactly the transaction
     * is simplified is specified by the type and anyoneCanPay parameters.</p>
     *
     * <p>This is a low level API and when using the regular {Wallet} class you don't have to call this yourself.
     * When working with more complex transaction types and contracts, it can be necessary. When signing a P2SH output
     * the redeemScript should be the script encoded into the scriptSig field, for normal transactions, it's the
     * scriptPubKey of the output you're signing for.</p>
     *
     * @param transaction
     * @param inputIndex input the signature is being calculated for. Tx signatures are always relative to an input.
     * @param redeemScript the script that should be in the given input during signing.
     * @param type Should be SigHash.ALL
     * @param anyoneCanPay should be false.
     */
    public static Sha256Hash hashForLegacySignature(Transaction transaction, int inputIndex, Script redeemScript,
                                                    SigHash.Flags type, boolean anyoneCanPay) {
        int sigHash = TransactionSignature.calcSigHashValue(type, anyoneCanPay);
        return SigHash.hashForLegacySignature(Translate.toTx(transaction), inputIndex, redeemScript.getProgram(), (byte) sigHash);
    }

    /**
     * <p>Calculates a signature hash, that is, a hash of a simplified form of the transaction. How exactly the transaction
     * is simplified is specified by the type and anyoneCanPay parameters.</p>
     *
     * <p>This is a low level API and when using the regular {Wallet} class you don't have to call this yourself.
     * When working with more complex transaction types and contracts, it can be necessary. When signing a Witness output
     * the scriptCode should be the script encoded into the scriptSig field, for normal transactions, it's the
     * scriptPubKey of the output you're signing for. (See BIP143: https://github.com/bitcoin/bips/blob/master/bip-0143.mediawiki)</p>
     *
     * @param inputIndex input the signature is being calculated for. Tx signatures are always relative to an input.
     * @param scriptCode the script that should be in the given input during signing.
     * @param prevValue the value of the coin being spent
     * @param type Should be SigHash.ALL
     * @param anyoneCanPay should be false.
     */
    public synchronized Sha256Hash hashForForkIdSignature(
            int inputIndex,
            Script scriptCode,
            Coin prevValue,
            SigHash.Flags type,
            boolean anyoneCanPay)
    {
        byte[] connectedScript = scriptCode.getProgram();
        return SigHash.hashForForkIdSignature(Translate.toTx(this), inputIndex, connectedScript, prevValue, type, anyoneCanPay);
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        uint32ToByteStreamLE(version, stream);
        stream.write(new VarInt(inputs.size()).encode());
        for (TransactionInput in : inputs)
            in.bitcoinSerialize(stream);
        stream.write(new VarInt(outputs.size()).encode());
        for (TransactionOutput out : outputs)
            out.bitcoinSerialize(stream);
        uint32ToByteStreamLE(lockTime, stream);
    }


    /**
     * Transactions can have an associated lock time, specified either as a block height or in seconds since the
     * UNIX epoch. A transaction is not allowed to be confirmed by miners until the lock time is reached, and
     * since Bitcoin 0.8+ a transaction that did not end its lock period (non final) is considered to be non
     * standard and won't be relayed or included in the memory pool either.
     */
    public long getLockTime() {
        maybeParse();
        return lockTime;
    }

    /**
     * Transactions can have an associated lock time, specified either as a block height or in seconds since the
     * UNIX epoch. A transaction is not allowed to be confirmed by miners until the lock time is reached, and
     * since Bitcoin 0.8+ a transaction that did not end its lock period (non final) is considered to be non
     * standard and won't be relayed or included in the memory pool either.
     */
    public void setLockTime(long lockTime) {
        unCache();
        boolean seqNumSet = false;
        for (TransactionInput input : inputs) {
            if (input.getSequenceNumber() != TransactionInput.NO_SEQUENCE) {
                seqNumSet = true;
                break;
            }
        }
        if (lockTime != 0 && (!seqNumSet || inputs.isEmpty())) {
            // At least one input must have a non-default sequence number for lock times to have any effect.
            // For instance one of them can be set to zero to make this feature work.
            log.warn("You are setting the lock time on a transaction but none of the inputs have non-default sequence numbers. This will not do what you expect!");
        }
        this.lockTime = lockTime;
    }

    public long getVersion() {
        maybeParse();
        return version;
    }

    public void setVersion(int version) {
        unCache();
        this.version = version;
    }

    public void setInputs(List<TransactionInput> inputs) {
        unCache();
        this.inputs = (List<TransactionInput>) inputs;
    }

    public void setOutputs(List<TransactionOutput> outputs) {
        unCache();
        this.outputs = outputs;
    }

    /** Returns an unmodifiable view of all inputs. */
    public List<TransactionInput> getInputs() {
        maybeParse();
        return Collections.unmodifiableList(inputs);
    }

    /** Returns an unmodifiable view of all outputs. */
    public List<TransactionOutput> getOutputs() {
        maybeParse();
        return Collections.unmodifiableList(outputs);
    }

    /** Randomly re-orders the transaction outputs: good for privacy */
    public void shuffleOutputs() {
        maybeParse();
        Collections.shuffle(outputs);
    }

    /** Same as getInputs().get(index). */
    public TransactionInput getInput(long index) {
        maybeParse();
        return inputs.get((int)index);
    }

    /** Same as getOutputs().get(index) */
    public TransactionOutput getOutput(long index) {
        maybeParse();
        return outputs.get((int)index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return getHash().equals(((Transaction)o).getHash());
    }

    @Override
    public int hashCode() {
        return getHash().hashCode();
    }

    /**
     * Gets the count of regular SigOps in this transactions
     */
    public int getSigOpCount() throws ScriptExecutionException {
        maybeParse();
        int sigOps = 0;
        for (TransactionInput input : inputs)
            sigOps += ScriptUtils.getSigOpCount(input.getScriptBytes());
        for (TransactionOutput output : outputs)
            sigOps += ScriptUtils.getSigOpCount(output.getScriptBytes());
        return sigOps;
    }

    /**
     * Check block height is in coinbase input script, for use after BIP 34
     * enforcement is enabled.
     */
    public void checkCoinBaseHeight(final int height)
            throws VerificationException {
        checkArgument(height >= BitcoinJ.BLOCK_HEIGHT_GENESIS);
        checkState(isCoinBase());

        // Check block height is in coinbase input script
        final TransactionInput in = this.getInputs().get(0);
        final ScriptBuilder builder = new ScriptBuilder();
        builder.number(height);
        final byte[] expected = builder.build().getProgram();
        final byte[] actual = in.getScriptBytes();
        if (actual.length < expected.length) {
            throw new VerificationException.CoinbaseHeightMismatch("Block height mismatch in coinbase.");
        }
        for (int scriptIdx = 0; scriptIdx < expected.length; scriptIdx++) {
            if (actual[scriptIdx] != expected[scriptIdx]) {
                throw new VerificationException.CoinbaseHeightMismatch("Block height mismatch in coinbase.");
            }
        }
    }

    /**
     * <p>Checks the transaction contents for sanity, in ways that can be done in a standalone manner.
     * Does <b>not</b> perform all checks on a transaction such as whether the inputs are already spent.
     * Specifically this method verifies:</p>
     *
     * <ul>
     *     <li>That there is at least one input and output.</li>
     *     <li>That the serialized size is not larger than the max block size.</li>
     *     <li>That no outputs have negative value.</li>
     *     <li>That the outputs do not sum to larger than the max allowed quantity of coin in the system.</li>
     *     <li>If the tx is a coinbase tx, the coinbase scriptSig size is within range. Otherwise that there are no
     *     coinbase inputs in the tx.</li>
     * </ul>
     *
     * @throws VerificationException
     */
    public void verify() throws VerificationException {
        maybeParse();
        if (inputs.size() == 0 || outputs.size() == 0)
            throw new VerificationException.EmptyInputsOrOutputs();
        if (this.getMessageSize() > Transaction.MAX_TRANSACTION_SIZE)
            throw new VerificationException.LargerThanMaxTransactionSize();

        Coin valueOut = Coin.ZERO;
        HashSet<TransactionOutPoint> outpoints = new HashSet<TransactionOutPoint>();
        for (TransactionInput input : inputs) {
            if (outpoints.contains(input.getOutpoint()))
                throw new VerificationException.DuplicatedOutPoint();
            outpoints.add(input.getOutpoint());
        }
        try {
            for (TransactionOutput output : outputs) {
                if (output.getValue().signum() < 0)    // getValue() can throw IllegalStateException
                    throw new VerificationException.NegativeValueOutput();
                valueOut = valueOut.add(output.getValue());
                if (net.params().hasMaxMoney() && valueOut.compareTo(net.params().getMaxMoney()) > 0)
                    throw new IllegalArgumentException();
            }
        } catch (IllegalStateException e) {
            throw new VerificationException.ExcessiveValue();
        } catch (IllegalArgumentException e) {
            throw new VerificationException.ExcessiveValue();
        }

        if (isCoinBase()) {
            if (inputs.get(0).getScriptBytes().length < 2 || inputs.get(0).getScriptBytes().length > 100)
                throw new VerificationException.CoinbaseScriptSizeOutOfRange();
        } else {
            for (TransactionInput input : inputs)
                if (input.isCoinBase())
                    throw new VerificationException.UnexpectedCoinbaseInput();
        }
    }

    /**
     * <p>A transaction is time locked if at least one of its inputs is non-final and it has a lock time</p>
     *
     * <p>To check if this transaction is final at a given height and time, see {@link Transaction#isFinal(int, long)}
     * </p>
     */
    public boolean isTimeLocked() {
        if (getLockTime() == 0)
            return false;
        for (TransactionInput input : getInputs())
            if (input.hasSequence())
                return true;
        return false;
    }

    /**
     * Returns whether this transaction will opt into the
     * <a href="https://github.com/bitcoin/bips/blob/master/bip-0125.mediawiki">full replace-by-fee </a> semantics.
     */
    public boolean isOptInFullRBF() {
        for (TransactionInput input : getInputs())
            if (input.isOptInFullRBF())
                return true;
        return false;
    }

    /**
     * <p>Returns true if this transaction is considered finalized and can be placed in a block. Non-finalized
     * transactions won't be included by miners and can be replaced with newer versions using sequence numbers.
     * This is useful in certain types of <a href="http://en.bitcoin.it/wiki/Contracts">contracts</a>, such as
     * micropayment channels.</p>
     *
     * <p>Note that currently the replacement feature is disabled in Bitcoin Core and will need to be
     * re-activated before this functionality is useful.</p>
     */
    public boolean isFinal(int height, long blockTimeSeconds) {
        long time = getLockTime();
        return time < (time < BitcoinJ.LOCKTIME_THRESHOLD ? height : blockTimeSeconds) || !isTimeLocked();
    }

//    /**
//     * Returns either the lock time as a date, if it was specified in seconds, or an estimate based on the time in
//     * the current head block if it was specified as a block time.
//     */
//    public Date estimateLockTime(AbstractBlockChain chain) {
//        if (lockTime < BitcoinJ.LOCKTIME_THRESHOLD)
//            return chain.estimateBlockTime((int)getLockTime());
//        else
//            return new Date(getLockTime()*1000);
//    }

    /**
     * Returns the purpose for which this transaction was created. See the javadoc for {@link Purpose} for more
     * information on the point of this field and what it can be.
     */
    public Purpose getPurpose() {
        return purpose;
    }

    /**
     * Marks the transaction as being created for the given purpose. See the javadoc for {@link Purpose} for more
     * information on the point of this field and what it can be.
     */
    public void setPurpose(Purpose purpose) {
        this.purpose = purpose;
    }

    /**
     * Getter for {@link #exchangeRate}.
     */
    @Nullable
    public ExchangeRate getExchangeRate() {
        return exchangeRate;
    }

    /**
     * Setter for {@link #exchangeRate}.
     */
    public void setExchangeRate(ExchangeRate exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    /**
     * Returns the transaction {@link #memo}.
     */
    public String getMemo() {
        return memo;
    }

    /**
     * Set the transaction {@link #memo}. It can be used to record the memo of the payment request that initiated the
     * transaction.
     */
    public void setMemo(String memo) {
        this.memo = memo;
    }

    /**
     * Needed by Block constructor but we don't want to expose it publicly
     * @param length
     */
    void forceSetLength(int length) {
        super.setLength(length);
    }
}
