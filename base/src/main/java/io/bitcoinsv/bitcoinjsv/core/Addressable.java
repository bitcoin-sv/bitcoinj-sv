/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.core;

import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;

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
