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

package io.bitcoinsv.bitcoinjsv.msg.protocol;

import com.google.common.base.Objects;
import io.bitcoinsv.bitcoinjsv.msg.ChildMessage;
import io.bitcoinsv.bitcoinjsv.params.SerializeMode;
import io.bitcoinsv.bitcoinjsv.params.Net;
import io.bitcoinsv.bitcoinjsv.script.Script;
import io.bitcoinsv.bitcoinjsv.script.ScriptBuilder;
import io.bitcoinsv.bitcoinjsv.script.ScriptUtils;
import io.bitcoinsv.bitcoinjsv.script.interpreter.ScriptExecutionException;
import io.bitcoinsv.bitcoinjsv.core.*;
import org.slf4j.*;

import javax.annotation.*;
import java.io.*;
import java.util.*;

import static com.google.common.base.Preconditions.*;

/**
 * <p>A TransactionOutput message contains a scriptPubKey that controls who is able to spend its value. It is a sub-part
 * of the Transaction message.</p>
 * 
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class TransactionOutput extends ChildMessage {
    private static final Logger log = LoggerFactory.getLogger(TransactionOutput.class);

    // The output's value is kept as a native type in order to save class instances.
    private long value;

    // A transaction output has a script used for authenticating that the redeemer is allowed to spend
    // this output.
    private byte[] scriptBytes;

    // The script bytes are parsed and turned into a Script on demand.
    private Script scriptPubKey;

    // These fields are not Bitcoin serialized. They are used for tracking purposes in our wallet
    // only. If set to true, this output is counted towards our balance. If false and spentBy is null the tx output
    // was owned by us and was sent to somebody else. If false and spentBy is set it means this output was owned by
    // us and used in one of our own transactions (eg, because it is a change output).
    private boolean availableForSpending;
    @Nullable private TransactionInput spentBy;

    private int scriptLen;

    /**
     * Deserializes a transaction output message. This is usually part of a transaction message.
     */
    public TransactionOutput(Net net, @Nullable Transaction parent, byte[] payload,
                             int offset) throws ProtocolException {
        super(net, payload, offset);
        setParent(parent);
        availableForSpending = true;
    }

    /**
     * Deserializes a transaction output message. This is usually part of a transaction message.
     *
     * @param net NetworkParameters object.
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param offset The location of the first payload byte within the array.
     * @param serializeMode the serializeMode to use for this message.
     * @throws ProtocolException
     */
    public TransactionOutput(Net net, @Nullable Transaction parent, byte[] payload, int offset, SerializeMode serializeMode) throws ProtocolException {
        super(net, payload, offset, parent, serializeMode, UNKNOWN_LENGTH);
        availableForSpending = true;
    }

    /**
     * Creates an output that sends 'value' to the given address (public key hash). The amount should be created with
     * something like {@link Coin#valueOf(int, int)}. Typically you would use
     * {@link Transaction#addOutput(Coin, Address)} instead of creating a TransactionOutput directly.
     */
    public TransactionOutput(Net net, @Nullable Transaction parent, Coin value, Address to) {
        this(net, parent, value, ScriptBuilder.createOutputScript(to).getProgram());
    }

    /**
     * Creates an output that sends 'value' to the given public key using a simple CHECKSIG script (no addresses). The
     * amount should be created with something like {@link Coin#valueOf(int, int)}. Typically you would use
     * {@link Transaction#addOutput(Coin, ECKey)} instead of creating an output directly.
     */
    public TransactionOutput(Net net, @Nullable Transaction parent, Coin value, ECKey to) {
        this(net, parent, value, ScriptBuilder.createOutputScript(to).getProgram());
    }

    public TransactionOutput(Net net, @Nullable Transaction parent, Coin value, byte[] scriptBytes) {
        super(net);
        // Negative values obviously make no sense, except for -1 which is used as a sentinel value when calculating
        // SIGHASH_SINGLE signatures, so unfortunately we have to allow that here.
        checkArgument(value.signum() >= 0 || value.equals(Coin.NEGATIVE_SATOSHI), "Negative values not allowed");
        checkArgument(!net.params().hasMaxMoney() || value.compareTo(net.params().getMaxMoney()) <= 0, "Values larger than MAX_MONEY not allowed");
        this.value = value.value;
        this.scriptBytes = scriptBytes;
        setParent(parent);
        availableForSpending = true;
        setLength(8 + VarInt.sizeOf(scriptBytes.length) + scriptBytes.length);
    }

    public Script getScriptPubKey() throws ScriptExecutionException {
        if (scriptPubKey == null) {
            maybeParse();
            scriptPubKey = new Script(scriptBytes);
        }
        return scriptPubKey;
    }

    @Override
    protected void parseLite() throws ProtocolException {
        value = readInt64();
        scriptLen = (int) readVarInt();
        setLength(cursor - offset + scriptLen);
    }

    @Override
    protected void parse() throws ProtocolException {
        scriptBytes = readBytes(scriptLen);
    }

    @Override
    public void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        checkNotNull(scriptBytes);
        maybeParse();
        Utils.int64ToByteStreamLE(value, stream);
        // TODO: Move script serialization into the Script class, where it belongs.
        stream.write(new VarInt(scriptBytes.length).encode());
        stream.write(scriptBytes);
    }

    /**
     * Returns the value of this output. This is the amount of currency that the destination address
     * receives.
     */
    public Coin getValue() {
        maybeParse();
        try {
            return Coin.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Sets the value of this output.
     */
    public void setValue(Coin value) {
        checkNotNull(value);
        unCache();
        this.value = value.value;
    }

    /**
     * Gets the index of this output in the parent transaction, or throws if this output is free standing. Iterates
     * over the parents list to discover this.
     */
    public int getIndex() {
        List<TransactionOutput> outputs = getParentTransaction().getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            if (outputs.get(i) == this)
                return i;
        }
        throw new IllegalStateException("Output linked to wrong parent transaction?");
    }

    /**
     * Will this transaction be relayable and mined by default miners?
     */
    public boolean isDust() {
        // Transactions that are OP_RETURN can't be dust regardless of their value.
        if (getScriptPubKey().isOpReturnBeforeGenesis())
            return false;
        return getValue().isLessThan(getMinNonDustValue());
    }

    /**
     * <p>Gets the minimum value for a txout of this size to be considered non-dust by Bitcoin Core
     * (and thus relayed). See: CTxOut::IsDust() in Bitcoin Core. The assumption is that any output that would
     * consume more than a third of its value in fees is not something the Bitcoin system wants to deal with right now,
     * so we call them "dust outputs" and they're made non standard. The choice of one third is somewhat arbitrary and
     * may change in future.</p>
     *
     * <p>You probably should use {@link TransactionOutput#getMinNonDustValue()} which uses
     * a safe fee-per-kb by default.</p>
     *
     * @param feePerKb The fee required per kilobyte. Note that this is the same as Bitcoin Core's -minrelaytxfee * 3
     */
    public Coin getMinNonDustValue(Coin feePerKb) {
        // A typical output is 33 bytes (pubkey hash + opcodes) and requires an input of 148 bytes to spend so we add
        // that together to find out the total amount of data used to transfer this amount of value. Note that this
        // formula is wrong for anything that's not a pay-to-address output, unfortunately, we must follow Bitcoin Core's
        // wrongness in order to ensure we're considered standard. A better formula would either estimate the
        // size of data needed to satisfy all different script types, or just hard code 33 below.
        final long size = this.unsafeBitcoinSerialize().length + 148;
        return feePerKb.multiply(size).divide(1000);
    }

    /**
     * Returns the minimum value for this output to be considered "not dust", i.e. the transaction will be relayable
     * and mined by default miners. For normal pay to address outputs, this is 2730 satoshis, the same as
     * {@link Coin#MIN_NONDUST_OUTPUT}.
     */
    public Coin getMinNonDustValue() {
        return getMinNonDustValue(BitcoinJ.REFERENCE_DEFAULT_MIN_TX_FEE.multiply(3));
    }

    /**
     * Sets this objects availableForSpending flag to false and the spentBy pointer to the given input.
     * If the input is null, it means this output was signed over to somebody else rather than one of our own keys.
     * @throws IllegalStateException if the transaction was already marked as spent.
     */
    public void markAsSpent(TransactionInput input) {
        checkState(availableForSpending);
        availableForSpending = false;
        spentBy = input;
        if (parent != null)
            if (log.isDebugEnabled()) log.debug("Marked {}:{} as spent by {}", getParentTransactionHash(), getIndex(), input);
        else
            if (log.isDebugEnabled()) log.debug("Marked floating output as spent by {}", input);
    }

    /**
     * Resets the spent pointer / availableForSpending flag to null.
     */
    public void markAsUnspent() {
        if (parent != null)
            if (log.isDebugEnabled()) log.debug("Un-marked {}:{} as spent by {}", getParentTransactionHash(), getIndex(), spentBy);
        else
            if (log.isDebugEnabled()) log.debug("Un-marked floating output as spent by {}", spentBy);
        availableForSpending = true;
        spentBy = null;
    }

    /**
     * Returns whether {@link TransactionOutput#markAsSpent(TransactionInput)} has been called on this class. A
     * {Wallet} will mark a transaction output as spent once it sees a transaction input that is connected to it.
     * Note that this flag can be false when an output has in fact been spent according to the rest of the network if
     * the spending transaction wasn't downloaded yet, and it can be marked as spent when in reality the rest of the
     * network believes it to be unspent if the signature or script connecting to it was not actually valid.
     */
    public boolean isAvailableForSpending() {
        return availableForSpending;
    }

    /**
     * The backing script bytes which can be turned into a Script object.
     * @return the scriptBytes
    */
    public byte[] getScriptBytes() {
        maybeParse();
        return scriptBytes;
    }

    /**
     * Returns a human readable debug string.
     */
    @Override
    public String toString() {
        try {
            Script script = getScriptPubKey();
            StringBuilder buf = new StringBuilder("TxOut of ");
            buf.append(Coin.valueOf(value).toFriendlyString());
            if (script.isSentToAddress() || script.isPayToScriptHash())
                buf.append(" to ").append(ScriptUtils.getToAddress(script, net.params()));
            else if (script.isSentToRawPubKey())
                buf.append(" to pubkey ").append(Utils.HEX.encode(script.getPubKey()));
            else if (script.isSentToMultiSig())
                buf.append(" to multisig");
            else
                buf.append(" (unknown type)");
            buf.append(" script:").append(script);
            return buf.toString();
        } catch (ScriptExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the connected input.
     */
    @Nullable
    public TransactionInput getSpentBy() {
        return spentBy;
    }

    /**
     * Returns the transaction that owns this output.
     */
    @Nullable
    public Transaction getParentTransaction() {
        return (Transaction)parent;
    }

    /**
     * Returns the transaction hash that owns this output.
     */
    @Nullable
    public Sha256Hash getParentTransactionHash() {
        return parent == null ? null : parent.getHash();
    }

    /**
     * Returns a new {@link TransactionOutPoint}, which is essentially a structure pointing to this output.
     * Requires that this output is not detached.
     */
    public TransactionOutPoint getOutPointFor() {
        return new TransactionOutPoint(net, getIndex(), getParentTransaction());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionOutput other = (TransactionOutput) o;
        return value == other.value && (parent == null || (parent == other.parent && getIndex() == other.getIndex()))
                && Arrays.equals(scriptBytes, other.scriptBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value, parent, Arrays.hashCode(scriptBytes));
    }
}
