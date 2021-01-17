/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.core;

import io.bitcoinj.ecc.ECKeyBytes;
import org.spongycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.Arrays;

public class BasicECKeyBytes implements ECKeyBytes {

    private byte[] pubkey;

    public BasicECKeyBytes(byte[] pubkey) {
        this.pubkey = pubkey;
    }

    @Override
    public boolean isPubKeyOnly() {
        return true;
    }

    @Override
    public boolean hasPrivKey() {
        return false;
    }

    @Override
    public byte[] getPubKeyHash() {
        return Utils.sha256hash160(pubkey);
    }

    @Override
    public byte[] getPubKey() {
        return pubkey;
    }

    @Override
    public ECPoint getPubKeyPoint() {
        return null;
    }

    @Override
    public BigInteger getPrivKey() {
        return null;
    }

    @Override
    public boolean isCompressed() {
        return false;
    }

    @Override
    public byte[] getPrivKeyBytes() {
        return null;
    }

    @Override
    public String getPrivateKeyAsHex() {
        return null;
    }

    @Override
    public String getPublicKeyAsHex() {
        return Utils.HEX.encode(pubkey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicECKeyBytes)) return false;
        BasicECKeyBytes that = (BasicECKeyBytes) o;
        return Arrays.equals(pubkey, that.pubkey);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(pubkey);
    }
}
