package org.bitcoinj.msg.bitcoin;

import java.io.IOException;
import java.io.OutputStream;

public interface BitcoinObject {

    byte[] EMPTY_ARRAY = new byte[0];

    int getMessageSize();

    byte[] serialize();

    void serializeTo(OutputStream stream) throws IOException;

    boolean isMutable();

    BitcoinObject rootObject();
}
