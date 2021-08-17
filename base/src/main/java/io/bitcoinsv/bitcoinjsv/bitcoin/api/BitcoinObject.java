/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api;

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

public interface BitcoinObject<I extends BitcoinObject> extends Serializable {

    byte[] EMPTY_ARRAY = new byte[0];
    public static final int UNKNOWN_MESSAGE_LENGTH = Integer.MIN_VALUE;

    default int getMessageSize() {
        if (isFixedSize()) {
            return fixedSize();
        }
        throw new UnsupportedOperationException();
    }

    byte[] serialize();

    void serializeTo(OutputStream stream) throws IOException;

    boolean isMutable();

    /**
     * Mark object as immutable.
     */
    void makeImmutable();

    /**
     * Should should only be called by copyMutable implementations to avoid traversing up the tree on every call.
     *
     * Should make any child object mutable as well.
     */
    @VisibleForTesting
    void makeSelfMutable();

    BitcoinObject rootObject();

    BitcoinObject parent();

    I makeMutable();

    I makeNew(byte[] serialized);

    /**
     * makes an immutable copy of this object
     * @return
     */
    default I copy() {
        return makeNew(serialize());
    }

    /**
     * makes a copy of the object and sets it's state to mutable
     * @return
     */
    default I mutableCopy() {
        I copy = makeNew(serialize());
        copy.makeSelfMutable();
        return copy;
    }

    default int fixedSize() {
        return UNKNOWN_MESSAGE_LENGTH;
    }

    default boolean isFixedSize() {
        return fixedSize() != UNKNOWN_MESSAGE_LENGTH;
    }



}
