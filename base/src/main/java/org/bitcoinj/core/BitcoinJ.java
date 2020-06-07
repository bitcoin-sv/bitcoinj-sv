package org.bitcoinj.core;

/**
 * Placeholder for constants that are commonly referenced but don't necessarilly need to require a dependency on the original class
 */
public class BitcoinJ {

    /** The version of this library release, as a string. */
    public static final String BITCOINJ_VERSION = "0.15-MUTANT";

    /** The value that is prepended to the subVer field of this application. */
    public static final String LIBRARY_SUBVER = "/bitcoinj.cash:" + BITCOINJ_VERSION + "/";

    /**
     * If using this feePerKb, transactions will get confirmed within the next couple of blocks.
     * This should be adjusted from time to time. Last adjustment: March 2016.
     */
    public static final Coin DEFAULT_TX_FEE = Coin.valueOf(5000); // 0.5 mBTC

    /**
     * If feePerKb is lower than this, Bitcoin Core will treat it as if there were no fee.
     */
    public static final Coin REFERENCE_DEFAULT_MIN_TX_FEE = Coin.valueOf(1000); // 0.01 mBTC
}
