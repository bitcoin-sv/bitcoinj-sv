package org.bitcoinj.script;

import org.bitcoinj.core.*;
import org.bitcoinj.msg.Translate;
import org.bitcoinj.msg.bitcoin.api.base.Tx;
import org.bitcoinj.msg.protocol.Transaction;
import org.bitcoinj.script.interpreter.ScriptExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ScriptUtils_legacy {

    static final Logger log = LoggerFactory.getLogger(ScriptUtils_legacy.class);

    /**
     * Verifies that this script (interpreted as a scriptSig) correctly spends the given scriptPubKey, enabling all
     * validation rules.
     *
     * @param script
     * @param txContainingThis The transaction in which this input scriptSig resides.
     *                         Accessing txContainingThis from another thread while this method runs results in undefined behavior.
     * @param scriptSigIndex   The index in txContainingThis of the scriptSig (note: NOT the index of the scriptPubKey).
     * @param scriptPubKey     The connected scriptPubKey containing the conditions needed to claim the value.
     * @deprecated Use {@link #correctlySpends(Script, Transaction, long, Script, java.util.Set)}
     * instead so that verification flags do not change as new verification options
     * are added.
     */
    @Deprecated
    public static void correctlySpends(Script script, Transaction txContainingThis, long scriptSigIndex, Script scriptPubKey)
            throws ScriptExecutionException {
        correctlySpends(script, txContainingThis, scriptSigIndex, scriptPubKey, Coin.ZERO, ScriptVerifyFlag.ALL_VERIFY_FLAGS);
    }

    @Deprecated
    public static void correctlySpends(Script script, Transaction txContainingThis, long scriptSigIndex, Script scriptPubKey,
                                       Set<ScriptVerifyFlag> verifyFlags)
            throws ScriptExecutionException {
        correctlySpends(script, txContainingThis, scriptSigIndex, scriptPubKey, Coin.ZERO, verifyFlags);
    }

    /**
     * Verifies that this script (interpreted as a scriptSig) correctly spends the given scriptPubKey.
     *
     * @param script
     * @param txContainingThis The transaction in which this input scriptSig resides.
     *                         Accessing txContainingThis from another thread while this method runs results in undefined behavior.
     * @param scriptSigIndex   The index in txContainingThis of the scriptSig (note: NOT the index of the scriptPubKey).
     * @param scriptPubKey     The connected scriptPubKey containing the conditions needed to claim the value.
     * @param verifyFlags      Each flag enables one validation rule. If in doubt, use { #correctlySpends(Tx, long, Script)}
     *                         which sets all flags.
     */
    public static void correctlySpends(Script script, Transaction txContainingThis, long scriptSigIndex, Script scriptPubKey, Coin value,
                                       Set<ScriptVerifyFlag> verifyFlags) throws ScriptExecutionException {
        Tx translatedParentTx = Translate.toTx(txContainingThis);
        ScriptUtils.correctlySpends(script, translatedParentTx, scriptSigIndex, scriptPubKey, value, verifyFlags);
    }

}
