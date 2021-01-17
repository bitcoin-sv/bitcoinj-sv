package io.bitcoinj.msg.protocol;

import io.bitcoinj.core.*;
import io.bitcoinj.ecc.ECDSASignature;
import io.bitcoinj.ecc.TransactionSignature;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.script.*;
import io.bitcoinj.script.interpreter.ScriptExecutionException;
import io.bitcoinj.temp.KeyBag;
import io.bitcoinj.temp.RedeemData;
import io.bitcoinj.temp.TransactionBag;
import io.bitcoinj.temp.WalletTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

public class TxHelper {

    private static final Logger log = LoggerFactory.getLogger(TxHelper.class);


    /**
     * Adds a new and fully signed input for the given parameters. Note that this method is <b>not</b> thread safe
     * and requires external synchronization. Please refer to general documentation on Bitcoin scripting and contracts
     * to understand the values of sigHash and anyoneCanPay: otherwise you can use the other form of this method
     * that sets them to typical defaults.
     *
     * @throws ScriptExecutionException if the scriptPubKey is not a pay to address or pay to pubkey script.
     */
    public static TransactionInput addSignedInput(Transaction transaction, TransactionOutPoint prevOut, Script scriptPubKey, ECKey sigKey,
                                                  SigHash.Flags sigHash, boolean anyoneCanPay) throws ScriptExecutionException {
        // Verify the API user didn't try to do operations out of order.
        checkState(!transaction.getOutputs().isEmpty(), "Attempting to sign tx without outputs.");
        TransactionInput input = new TransactionInput(transaction.getNet(), transaction, new byte[]{}, prevOut);
        transaction.addInput(input);
        Sha256Hash hash = Transaction.hashForLegacySignature(transaction, transaction.getInputs().size() - 1, scriptPubKey, sigHash, anyoneCanPay);
        ECDSASignature ecSig = sigKey.sign(hash);
        TransactionSignature txSig = new TransactionSignature(ecSig, sigHash, anyoneCanPay, false);
        if (scriptPubKey.isSentToRawPubKey())
            input.setScriptSig(ScriptBuilder.createInputScript(txSig));
        else if (scriptPubKey.isSentToAddress())
            input.setScriptSig(ScriptBuilder.createInputScript(txSig, sigKey));
        else
            throw new ScriptExecutionException("Don't know how to sign for this kind of scriptPubKey: " + scriptPubKey);
        return input;
    }

    /**
     * Same as {addSignedInput(TransactionOutPoint, Script, ECKey, SigHash, boolean)}
     * but defaults to {@link SigHash.Flags#ALL} and "false" for the anyoneCanPay flag. This is normally what you want.
     */
    public static TransactionInput addSignedInput(Transaction transaction, TransactionOutPoint prevOut, Script scriptPubKey, ECKey sigKey) throws ScriptExecutionException {
        return addSignedInput(transaction, prevOut, scriptPubKey, sigKey, SigHash.Flags.ALL, false);
    }

    /**
     * Returns the RedeemData identified in the connected output, for either pay-to-address scripts, pay-to-key
     * or P2SH scripts.
     * If the script forms cannot be understood, throws ScriptException.
     *
     * @return a RedeemData or null if the connected data cannot be found in the wallet.
     */
    @Nullable
    public static RedeemData getConnectedRedeemData(TransactionOutPoint transactionOutPoint, KeyBag keyBag) throws ScriptExecutionException {
        TransactionOutput connectedOutput = transactionOutPoint.getConnectedOutput();
        checkNotNull(connectedOutput, "Input is not connected so cannot retrieve key");
        Script connectedScript = connectedOutput.getScriptPubKey();
        if (connectedScript.isSentToAddress()) {
            byte[] addressBytes = connectedScript.getPubKeyHash();
            return RedeemData.of(keyBag.findKeyFromPubHash(addressBytes), connectedScript);
        } else if (connectedScript.isSentToRawPubKey()) {
            byte[] pubkeyBytes = connectedScript.getPubKey();
            return RedeemData.of(keyBag.findKeyFromPubKey(pubkeyBytes), connectedScript);
        } else if (connectedScript.isPayToScriptHash()) {
            byte[] scriptHash = connectedScript.getPubKeyHash();
            return keyBag.findRedeemDataFromScriptHash(scriptHash);
        } else {
            throw new ScriptExecutionException("Could not understand form of connected output script: " + connectedScript);
        }
    }

    /**
     * Verifies that this input can spend the given output. Note that this input must be a part of a transaction.
     * Also note that the consistency of the outpoint will be checked, even if this input has not been connected.
     *
     * @param transactionInput
     * @param output the output that this input is supposed to spend.
     * @throws ScriptExecutionException If the script doesn't verify.
     * @throws VerificationException If the outpoint doesn't match the given output.
     */
    public static void verify(TransactionInput transactionInput, TransactionOutput output, Set<ScriptVerifyFlag> verifyFlags) throws VerificationException {
        if (output.getParent() != null) {
            if (!transactionInput.getOutpoint().getHash().equals(output.getParentTransaction().getHash()))
                throw new VerificationException("This input does not refer to the tx containing the output.");
            if (transactionInput.getOutpoint().getIndex() != output.getIndex())
                throw new VerificationException("This input refers to a different output on the given tx.");
        }
        Script pubKey = output.getScriptPubKey();
        int myIndex = transactionInput.getParentTransaction().getInputs().indexOf(transactionInput);
        //this is used in tests for CLTV so we have to be more liberal about disabled opcodes than usual.
        ScriptUtils_legacy.correctlySpends(transactionInput.getScriptSig(), transactionInput.getParentTransaction(), myIndex, pubKey, verifyFlags);
    }

    /**
     * Verifies that this input can spend the given output. Note that this input must be a part of a transaction.
     * Also note that the consistency of the outpoint will be checked, even if this input has not been connected.
     *
     * @param transactionInput
     * @param output the output that this input is supposed to spend.
     * @throws ScriptExecutionException If the script doesn't verify.
     * @throws VerificationException If the outpoint doesn't match the given output.
     */
    public static void verify(TransactionInput transactionInput, TransactionOutput output) throws VerificationException {
        verify(transactionInput, output, ScriptVerifyFlag.ALL_VERIFY_FLAGS);
    }

    /**
     * Alias for getOutpoint().getConnectedRedeemData(keyBag)
     */
    @Nullable
    public static RedeemData getConnectedRedeemData(TransactionInput transactionInput, KeyBag keyBag) throws ScriptExecutionException {
        return getConnectedRedeemData(transactionInput.getOutpoint(), keyBag);
    }

    /**
     * Connects this input to the relevant output of the referenced transaction if it's in the given map.
     * Connecting means updating the internal pointers and spent flags. If the mode is to ABORT_ON_CONFLICT then
     * the spent output won't be changed, but the outpoint.fromTx pointer will still be updated.
     *
     * @param transactionInput
     * @param transactions Map of txhash->transaction.
     * @param mode   Whether to abort if there's a pre-existing connection or not.
     * @return NO_SUCH_TX if the prevtx wasn't found, ALREADY_SPENT if there was a conflict, SUCCESS if not.
     */
    public static TransactionInput.ConnectionResult connect(TransactionInput transactionInput, Map<Sha256Hash, Transaction> transactions, TransactionInput.ConnectMode mode) {
        Transaction tx = transactions.get(transactionInput.getOutpoint().getHash());
        if (tx == null) {
            return TransactionInput.ConnectionResult.NO_SUCH_TX;
        }
        return connect(transactionInput, tx, mode);
    }

    /**
     * Connects this input to the relevant output of the referenced transaction.
     * Connecting means updating the internal pointers and spent flags. If the mode is to ABORT_ON_CONFLICT then
     * the spent output won't be changed, but the outpoint.fromTx pointer will still be updated.
     *
     * @param transactionInput
     * @param transaction The transaction to try.
     * @param mode   Whether to abort if there's a pre-existing connection or not.
     * @return NO_SUCH_TX if transaction is not the prevtx, ALREADY_SPENT if there was a conflict, SUCCESS if not.
     */
    public static TransactionInput.ConnectionResult connect(TransactionInput transactionInput, Transaction transaction, TransactionInput.ConnectMode mode) {
        if (!transaction.getHash().equals(transactionInput.getOutpoint().getHash()))
            return TransactionInput.ConnectionResult.NO_SUCH_TX;
        checkElementIndex((int) transactionInput.getOutpoint().getIndex(), transaction.getOutputs().size(), "Corrupt transaction");
        TransactionOutput out = transaction.getOutput((int) transactionInput.getOutpoint().getIndex());
        if (!out.isAvailableForSpending()) {
            if (transactionInput.getParentTransaction().equals(transactionInput.getOutpoint().fromTx)) {
                // Already connected.
                return TransactionInput.ConnectionResult.SUCCESS;
            } else if (mode == TransactionInput.ConnectMode.DISCONNECT_ON_CONFLICT) {
                out.markAsUnspent();
            } else if (mode == TransactionInput.ConnectMode.ABORT_ON_CONFLICT) {
                transactionInput.getOutpoint().fromTx = out.getParentTransaction();
                return TransactionInput.ConnectionResult.ALREADY_SPENT;
            }
        }
        transactionInput.connect(out);
        return TransactionInput.ConnectionResult.SUCCESS;
    }

    /**
     * For a connected transaction, runs the script against the connected pubkey and verifies they are correct.
     * @throws ScriptExecutionException if the script did not verify.
     * @throws VerificationException If the outpoint doesn't match the given output.
     * @param transactionInput
     */
    public static void verify(TransactionInput transactionInput) throws VerificationException {
        final Transaction fromTx = transactionInput.getOutpoint().fromTx;
        long spendingIndex = transactionInput.getOutpoint().getIndex();
        checkNotNull(fromTx, "Not connected");
        final TransactionOutput output = fromTx.getOutput((int) spendingIndex);
        verify(transactionInput, output);
    }

    /**
     * <p>If the output script pays to an address as in <a href="https://bitcoin.org/en/developer-guide#term-p2pkh">
     * P2PKH</a>, return the address of the receiver, i.e., a base58 encoded hash of the public key in the script. </p>
     *
     * @param transactionOutput
     * @param networkParameters needed to specify an address
     * @return null, if the output script is not the form <i>OP_DUP OP_HASH160 <PubkeyHash> OP_EQUALVERIFY OP_CHECKSIG</i>,
     * i.e., not P2PKH
     * @return an address made out of the public key hash
     */
    @Nullable
    public static Addressable getAddressFromP2PKHScript(TransactionOutput transactionOutput, NetworkParameters networkParameters) throws ScriptExecutionException {
        if (transactionOutput.getScriptPubKey().isSentToAddress())
            return ScriptUtils.getToAddress(transactionOutput.getScriptPubKey(), networkParameters);

        return null;
    }

    /**
     * <p>If the output script pays to a redeem script, return the address of the redeem script as described by,
     * i.e., a base58 encoding of [one-byte version][20-byte hash][4-byte checksum], where the 20-byte hash refers to
     * the redeem script.</p>
     *
     * <p>P2SH is described by <a href="https://github.com/bitcoin/bips/blob/master/bip-0016.mediawiki">BIP 16</a> and
     * <a href="https://bitcoin.org/en/developer-guide#p2sh-scripts">documented in the Bitcoin Developer Guide</a>.</p>
     *
     * @param transactionOutput
     * @param networkParameters needed to specify an address
     * @return null if the output script does not pay to a script hash
     * @return an address that belongs to the redeem script
     */
    @Nullable
    public static Addressable getAddressFromP2SH(TransactionOutput transactionOutput, NetworkParameters networkParameters) throws ScriptExecutionException {
        if (transactionOutput.getScriptPubKey().isPayToScriptHash())
            return ScriptUtils.getToAddress(transactionOutput.getScriptPubKey(), networkParameters);

        return null;
    }

    /**
     * Returns true if this output is to a key in the wallet or to an address/script we are watching.
     */
    public static boolean isMineOrWatched(TransactionOutput transactionOutput, TransactionBag transactionBag) {
        return isMine(transactionOutput, transactionBag) || isWatched(transactionOutput, transactionBag);
    }

    /**
     * Returns true if this output is to a key, or an address we have the keys for, in the wallet.
     */
    public static boolean isWatched(TransactionOutput transactionOutput, TransactionBag transactionBag) {
        try {
            Script script = transactionOutput.getScriptPubKey();
            return transactionBag.isWatchedScript(script);
        } catch (ScriptExecutionException e) {
            // Just means we didn't understand the output of this transaction: ignore it.
            log.debug("Could not parse tx output script: {}", e.toString());
            return false;
        }
    }

    /**
     * Returns true if this output is to a key, or an address we have the keys for, in the wallet.
     */
    public static boolean isMine(TransactionOutput transactionOutput, TransactionBag transactionBag) {
        try {
            Script script = transactionOutput.getScriptPubKey();
            if (script.isSentToRawPubKey()) {
                byte[] pubkey = script.getPubKey();
                return transactionBag.isPubKeyMine(pubkey);
            } if (script.isPayToScriptHash()) {
                return transactionBag.isPayToScriptHashMine(script.getPubKeyHash());
            } else {
                byte[] pubkeyHash = script.getPubKeyHash();
                return transactionBag.isPubKeyHashMine(pubkeyHash);
            }
        } catch (ScriptParseException e) {
            // Just means we didn't understand the output of this transaction: ignore it.
            log.debug("Could not parse tx {} output script: {}", transactionOutput.getParent() != null ? transactionOutput.getParent().getHash() : "(no parent)", e.toString());
            return false;
        }
    }

    /**
     * Returns the depth in blocks of the parent tx.
     *
     * <p>If the transaction appears in the top block, the depth is one. If it's anything else (pending, dead, unknown)
     * then -1.</p>
     * @return The tx depth or -1.
     * @param transactionOutput
     */
    public static int getParentTransactionDepthInBlocks(TransactionOutput transactionOutput) {
        if (transactionOutput.getParentTransaction() != null) {
            TransactionConfidence confidence = getConfidence(transactionOutput.getParentTransaction());
            if (confidence.getConfidenceType() == TransactionConfidence.ConfidenceType.BUILDING) {
                return confidence.getDepthInBlocks();
            }
        }
        return -1;
    }

    /**
     * Calculates the sum of the inputs that are spending coins with keys in the wallet. This requires the
     * transactions sending coins to those keys to be in the wallet. This method will not attempt to download the
     * blocks containing the input transactions if the key is in the wallet but the transactions are not.
     *
     * @return sum of the inputs that are spending coins with keys in the wallet
     */
    public static Coin getValueSentFromMe(Transaction transaction, TransactionBag wallet) throws ScriptExecutionException {
        // This is tested in WalletTest.
        Coin v = Coin.ZERO;
        for (TransactionInput input : transaction.getInputs()) {
            // This input is taking value from a transaction in our wallet. To discover the value,
            // we must find the connected transaction.
            TransactionOutput connected = input.getConnectedOutput(wallet.getTransactionPool(WalletTransaction.Pool.UNSPENT));
            if (connected == null)
                connected = input.getConnectedOutput(wallet.getTransactionPool(WalletTransaction.Pool.SPENT));
            if (connected == null)
                connected = input.getConnectedOutput(wallet.getTransactionPool(WalletTransaction.Pool.PENDING));
            if (connected == null)
                continue;
            // The connected output may be the change to the sender of a previous input sent to this wallet. In this
            // case we ignore it.
            if (!isMineOrWatched(connected, wallet))
                continue;
            v = v.add(connected.getValue());
        }
        return v;
    }

    /**
     * Gets the sum of the inputs, regardless of who owns them.
     */
    public static Coin getValueSentToMe(Transaction transaction, TransactionBag transactionBag) {
        // This is tested in WalletTest.
        Coin v = Coin.ZERO;
        for (TransactionOutput o : transaction.getOutputs()) {
            if (!isMineOrWatched(o, transactionBag)) continue;
            v = v.add(o.getValue());
        }
        return v;
    }

    /**
     * Returns the difference of {@link TxHelper#getValueSentToMe(Transaction, TransactionBag)} and {@link TxHelper#getValueSentFromMe(Transaction, TransactionBag)}.
     */
    public static Coin getValue(Transaction transaction, TransactionBag wallet) throws ScriptExecutionException {
        // FIXME: TEMP PERF HACK FOR ANDROID - this crap can go away once we have a real payments API.
        boolean isAndroid = Utils.isAndroidRuntime();
        Coin result = getValueSentToMe(transaction, wallet).subtract(getValueSentFromMe(transaction, wallet));
        return result;
    }

    /**
     * Returns false if this transaction has at least one output that is owned by the given wallet and unspent, true
     * otherwise.
     */
    public static boolean isEveryOwnedOutputSpent(Transaction transaction, TransactionBag transactionBag) {
        for (TransactionOutput output : transaction.getOutputs()) {
            if (output.isAvailableForSpending() && isMineOrWatched(output, transactionBag))
                return false;
        }
        return true;
    }

    /**
     * <p>Returns the list of transacion outputs, whether spent or unspent, that match a wallet by address or that are
     * watched by a wallet, i.e., transaction outputs whose script's address is controlled by the wallet and transaction
     * outputs whose script is watched by the wallet.</p>
     *
     * @param transaction
     * @param transactionBag The wallet that controls addresses and watches scripts.
     * @return linked list of outputs relevant to the wallet in this transaction
     */
    public static List<TransactionOutput> getWalletOutputs(Transaction transaction, TransactionBag transactionBag){
        List<TransactionOutput> walletOutputs = new LinkedList<TransactionOutput>();
        for (TransactionOutput o : transaction.getOutputs()) {
            if (!isMineOrWatched(o, transactionBag)) continue;
            walletOutputs.add(o);
        }

        return walletOutputs;
    }

    /**
     * Returns the confidence object for this transaction from the {@link TxConfidenceTable}
     * referenced by the implicit {@link Context}.
     * @param transaction
     */
    public static TransactionConfidence getConfidence(Transaction transaction) {
        return getConfidence(transaction, Context.get());
    }

    /**
     * Returns the confidence object for this transaction from the {@link TxConfidenceTable}
     * referenced by the given {@link Context}.
     */
    public static TransactionConfidence getConfidence(Transaction transaction, Context context) {
        return getConfidence(transaction, context.getConfidenceTable());
    }

    /**
     * Returns the confidence object for this transaction from the {@link TxConfidenceTable}
     */
    public static TransactionConfidence getConfidence(Transaction transaction, TxConfidenceTable table) {
        if (transaction.confidence == null)
            transaction.confidence = table.getOrCreate(transaction.getHash()) ;
        return transaction.confidence;
    }

    /** Check if the transaction has a known confidence
     * @param transaction*/
    public static boolean hasConfidence(Transaction transaction) {
        return getConfidence(transaction).getConfidenceType() != TransactionConfidence.ConfidenceType.UNKNOWN;
    }
}
