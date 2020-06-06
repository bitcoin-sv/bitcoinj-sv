package org.bitcoinj.core;

import org.bitcoinj.params.NetworkParameters;

import java.io.Serializable;

public interface Addressable extends Serializable, Cloneable, Comparable<VersionedChecksummedBytes> {

    String toBase58();

    int getVersion();

    NetworkParameters getParams();

    byte[] getHash160();

    boolean isP2SHAddress();

}
