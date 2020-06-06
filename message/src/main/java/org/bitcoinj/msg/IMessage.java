package org.bitcoinj.msg;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.params.Net;
import org.bitcoinj.params.NetworkParameters;
import org.bitcoinj.params.SerializeMode;

import java.io.IOException;
import java.io.OutputStream;

public interface IMessage {

    byte[] bitcoinSerialize();

    byte[] unsafeBitcoinSerialize();

    long getSerializedLength();

    SerializeMode getSerializeMode();

    void bitcoinSerialize(OutputStream stream) throws IOException;

    Sha256Hash getHash();

    int getMessageSize();

    NetworkParameters getParams();

    Net getNet();

    int length();

    void ensureParsed();
}
