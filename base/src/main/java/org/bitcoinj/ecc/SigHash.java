package org.bitcoinj.ecc;

/**
 * These constants are a part of a scriptSig signature on the inputs. They define the details of how a
 * transaction can be redeemed, specifically, they control how the hash of the transaction is calculated.
 */
public enum SigHash {
    ALL(1),
    NONE(2),
    SINGLE(3),
    FORKID(0x40),
    ANYONECANPAY(0x80), // Caution: Using this type in isolation is non-standard. Treated similar to ANYONECANPAY_ALL.
    ANYONECANPAY_ALL(0x81),
    ANYONECANPAY_NONE(0x82),
    ANYONECANPAY_SINGLE(0x83),
    UNSET(0); // Caution: Using this type in isolation is non-standard. Treated similar to ALL.

    public final int value;

    /**
     * @param value
     */
    private SigHash(final int value) {
        this.value = value;
    }

    /**
     * @return the value as a byte
     */
    public byte byteValue() {
        return (byte) this.value;
    }
}
