package org.bitcoinj.msg.bitcoin;

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.OutputStream;

public interface BitcoinObject<I extends BitcoinObject> {

    byte[] EMPTY_ARRAY = new byte[0];

    int getMessageSize();

    byte[] serialize();

    void serializeTo(OutputStream stream) throws IOException;

    boolean isMutable();

    /**
     * Should should only be called by copyMutable implementations to avoid traversing up the tree on every call.
     *
     * Should make any child object mutable as well.
     */
    @VisibleForTesting
    void makeSelfMutable();

    BitcoinObject rootObject();

    I makeMutable();

    I makeNew(byte[] serialized);

    /**
     * makes an immutable copy of this object
     * @return
     */
    default public I copy() {
        return makeNew(serialize());
    }

    /**
     * makes a copy of the object and sets it's state to mutable
     * @return
     */
    default public I mutableCopy() {
        I copy = makeNew(serialize());
        copy.makeSelfMutable();
        return copy;
    }

}
