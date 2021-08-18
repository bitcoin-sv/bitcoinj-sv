/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.ecc;

import com.google.common.primitives.UnsignedBytes;
import org.spongycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.Comparator;

public interface ECKeyBytes {

    /** Compares pub key bytes using {@link com.google.common.primitives.UnsignedBytes#lexicographicalComparator()} */
    public static final Comparator<ECKeyBytes> PUBKEY_COMPARATOR = new Comparator<ECKeyBytes>() {
        private Comparator<byte[]> comparator = UnsignedBytes.lexicographicalComparator();

        @Override
        public int compare(ECKeyBytes k1, ECKeyBytes k2) {
            return comparator.compare(k1.getPubKey(), k2.getPubKey());
        }
    };

    boolean isPubKeyOnly();

    boolean hasPrivKey();

    byte[] getPubKeyHash();

    byte[] getPubKey();

    ECPoint getPubKeyPoint();

    BigInteger getPrivKey();

    boolean isCompressed();

    byte[] getPrivKeyBytes();

    String getPrivateKeyAsHex();

    String getPublicKeyAsHex();
}
