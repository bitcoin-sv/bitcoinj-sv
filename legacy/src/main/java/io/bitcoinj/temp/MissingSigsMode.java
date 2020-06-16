package io.bitcoinj.temp;

/**
 * Enumerates possible resolutions for missing signatures.
 */
public enum MissingSigsMode {
    /** Input script will have OP_0 instead of missing signatures */
    USE_OP_ZERO,
    /**
     * Missing signatures will be replaced by dummy sigs. This is useful when you'd like to know the fee for
     * a transaction without knowing the user's password, as fee depends on size.
     */
    USE_DUMMY_SIG,
    /**
     * If signature is missing, {@link io.bitcoinj.signers.TransactionSigner.MissingSignatureException}
     * will be thrown for P2SH and {@link io.bitcoinj.core.ECKey.MissingPrivateKeyException} for other tx types.
     */
    THROW
}
