/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.core;

import io.bitcoinj.params.NetworkParameters;

import java.io.Serializable;

// todo: type of comparable is different, see https://errorprone.info/bugpattern/ComparableType
@SuppressWarnings("ComparableType")
public interface Addressable extends Serializable, Cloneable, Comparable<VersionedChecksummedBytes> {

    String toBase58();

    int getVersion();

    NetworkParameters getParams();

    byte[] getHash160();

    boolean isP2SHAddress();

}
