/*
 * Modifications described in the NOTICE.txt file are licensed under the Open BSV Licence.
 * Modifications Copyright 2020 Bitcoin Association
 */
package io.bitcoinj.script;

import java.util.EnumSet;

/**
 * Flags to pass to { Script#correctlySpends(Transaction, long, Script, Coin, Set)}.
 * Note currently only P2SH, DERSIG and NULLDUMMY are actually supported.
 */
public enum ScriptVerifyFlag {
    P2SH, // Enable BIP16-style subscript evaluation.
    STRICTENC, // Passing a non-strict-DER signature or one with undefined hashtype to a checksig operation causes script failure.
    DERSIG, // Passing a non-strict-DER signature to a checksig operation causes script failure (softfork safe, BIP66 rule 1)
    LOW_S, // Passing a non-strict-DER signature or one with S > order/2 to a checksig operation causes script failure
    NULLDUMMY, // Verify dummy stack item consumed by CHECKMULTISIG is of zero-length.
    SIGPUSHONLY, // Using a non-push operator in the scriptSig causes script failure (softfork safe, BIP62 rule 2).
    MINIMALDATA, // Require minimal encodings for all push operations and number encodings
    DISCOURAGE_UPGRADABLE_NOPS, // Discourage use of NOPs reserved for upgrades (NOP1-10)
    CLEANSTACK, // Require that only a single stack element remains after evaluation.
    CHECKLOCKTIMEVERIFY, // Enable CHECKLOCKTIMEVERIFY operation
    ENABLESIGHASHFORKID,
    MONOLITH_OPCODES, // May 15, 2018 Hard fork
    MAGNETIC_OPCODES, //Nov 15 2018 Hard fork
    GENESIS_OPCODES, // Feb 4th, 2020 Hard fork
    CHRONICLE_OPCODES,
    ;
    public static final EnumSet<ScriptVerifyFlag> CHRONICLE_SET = EnumSet.of(MONOLITH_OPCODES, GENESIS_OPCODES, CHRONICLE_OPCODES);
    public static final EnumSet<ScriptVerifyFlag> GENESIS_SET = EnumSet.of(MONOLITH_OPCODES, MAGNETIC_OPCODES, GENESIS_OPCODES);
    public static final EnumSet<ScriptVerifyFlag> MAGNETIC_SET = EnumSet.of(MONOLITH_OPCODES, MAGNETIC_OPCODES);
    public static final EnumSet<ScriptVerifyFlag> MONOLITH_SET = EnumSet.of(MONOLITH_OPCODES);
    public static final EnumSet<ScriptVerifyFlag> ALL_VERIFY_FLAGS_PRE_GENESIS = EnumSet.complementOf(EnumSet.of(GENESIS_OPCODES)); // Future Chronicle hard fork
    public static final EnumSet<ScriptVerifyFlag> ALL_VERIFY_FLAGS = EnumSet.allOf(ScriptVerifyFlag.class);
}
