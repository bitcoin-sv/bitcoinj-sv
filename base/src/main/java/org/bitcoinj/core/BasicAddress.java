package org.bitcoinj.core;

import org.bitcoinj.exception.WrongNetworkException;
import org.bitcoinj.params.NetworkParameters;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class BasicAddress extends VersionedChecksummedBytes implements Addressable {

    private transient NetworkParameters params;

    /**
     * Construct an address from parameters, the address version, and the hash160 form. Example:<p>
     *
     * <pre>new Address(MainNetParams.get(), NetworkParameters.getAddressHeader(), Hex.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));</pre>
     */
    public BasicAddress(NetworkParameters params, int version, byte[] hash160) throws WrongNetworkException {
        super(version, hash160);
        checkArgument(hash160.length == 20, "Addresses are 160-bit hashes, so you must provide 20 bytes");
        this.params = params;
    }

    public BasicAddress(NetworkParameters params, byte[] hash160) {
        super(params.getAddressHeader(), hash160);
        checkArgument(hash160.length == 20, "Addresses are 160-bit hashes, so you must provide 20 bytes");
        this.params = params;
    }

    @Override
    public NetworkParameters getParams() {
        return params;
    }

    @Override
    public byte[] getHash160() {
        return super.bytes;
    }

    @Override
    public boolean isP2SHAddress() {
        return params != null && this.version == params.getP2SHHeader();
    }

}
