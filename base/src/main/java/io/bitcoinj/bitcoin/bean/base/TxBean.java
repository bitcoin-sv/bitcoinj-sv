/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.bitcoin.bean.base;

import io.bitcoinj.bitcoin.api.base.*;
import io.bitcoinj.core.Coin;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.Utils;
import io.bitcoinj.core.VarInt;
import io.bitcoinj.bitcoin.api.BitcoinObject;
import io.bitcoinj.exception.VerificationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static io.bitcoinj.core.Utils.uint32ToByteStreamLE;

public class TxBean extends HashableImpl<Tx> implements Tx {

    //private Sha256Hash hash;

    // These are bitcoin serialized.
    private long version;

    private List<TxInput> inputs;

    private List<TxOutput> outputs;

    private long lockTime;

    /**
     * Only use this constructor if you're sure this is the correct hash.  It can avoid recalculating
     * when you already know it.
     * @param parent
     * @param payload
     * @param offset
     * @param hash
     */
    public TxBean(BitcoinObject parent, byte[] payload, int offset, Sha256Hash hash) {
        super(parent, payload, offset);
        this.hash = hash;
    }

    public TxBean(BitcoinObject parent, byte[] payload, int offset) {
        super(parent, payload, offset);
    }

    /**
     * Only use this constructor if you're sure this is the correct hash.  It can avoid recalculating
     * when you already know it.
     * @param payload
     * @param hash
     */
    public TxBean(byte[] payload, Sha256Hash hash) {
        this(null, payload, 0, hash);
    }

    public TxBean(byte[] payload) {
        this(null, payload, 0);
    }

    public TxBean(FullBlock parent, byte[] payload) {
        this(parent, payload, 0);
    }

    /**
     * Only use this constructor if you're sure this is the correct hash.  It can avoid recalculating
     * when you already know it.
     * @param in
     * @param hash
     */
    public TxBean(InputStream in, Sha256Hash hash) {
        super(null, in);
        this.hash = hash;
    }

    public TxBean(InputStream in) {
        this(in, null);
    }

    /**
     * Constructor for building manually
     * @param parent
     */
    public TxBean(BitcoinObject parent) {
        super(parent);
        //assigned defaults here instead of on fields as they may overwrite parsed values if other
        //constructors are called.
        version = 1;
        inputs = new ArrayList<>(4);
        outputs = new ArrayList<>(2);
        lockTime = 0; //this is default for java but we are being explicit
    }

    public TxBean(BitcoinObject parent, InputStream in) {
        super(parent, in);
    }

    @Override
    public Sha256Hash calculateHash() {
        Sha256Hash hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(serialize()));
        return hash;
    }

    @Override
    public Sha256Hash getHash() {
        if (hash == null) {
            hash = calculateHash();
        }
        return hash;
    }

    @Override
    public void setHash(Sha256Hash hash) {
        checkMutable();
        this.hash = hash;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        checkMutable();
        this.version = version;
    }

    @Override
    public List<TxInput> getInputs() {
        return isMutable() ? inputs : Collections.unmodifiableList(inputs);
    }

    @Override
    public void setInputs(List<TxInput> inputs) {
        checkMutable();
        this.inputs = inputs;
    }

    @Override
    public List<TxOutput> getOutputs() {
        return isMutable() ? outputs : Collections.unmodifiableList(outputs);
    }

    @Override
    public void setOutputs(List<TxOutput> outputs) {
        checkMutable();
        this.outputs = outputs;
    }

    @Override
    public long getLockTime() {
        return lockTime;
    }

    @Override
    public void setLockTime(long lockTime) {
        checkMutable();
        this.lockTime = lockTime;
    }

    @Override
    protected void parse() {
        cursor = offset;
        version = readUint32();

        // First come the inputs.
        long numInputs = readVarInt();
        inputs = new ArrayList<>((int) numInputs);
        for (long i = 0; i < numInputs; i++) {
            TxInput input = new TxInputBean(this, payload, cursor);
            cursor += input.getMessageSize();
            inputs.add(input);
        }
        // Now the outputs
        long numOutputs = readVarInt();
        outputs = new ArrayList<>((int) numOutputs);
        for (long i = 0; i < numOutputs; i++) {
            TxOutput output = new TxOutputBean(this, payload, cursor);
            cursor += output.getMessageSize();
            outputs.add(output);
        }
        lockTime = readUint32();

    }

    @Override
    protected int parse(InputStream in) throws IOException {
        int read = 0;
        version = Utils.readUint32(in);
        read += 4;

        long numInputs = new VarInt(in).value;
        read += VarInt.sizeOf(numInputs);
        inputs = new ArrayList<>((int) numInputs);
        for (long i = 0; i < numInputs; i++) {
            TxInput input = new TxInputBean(this, in);
            read += input.getMessageSize();
            inputs.add(input);
        }

        // Now the outputs
        long numOutputs = new VarInt(in).value;
        outputs = new ArrayList<>((int) numOutputs);
        for (long i = 0; i < numOutputs; i++) {
            TxOutput output = new TxOutputBean(this, in);
            read += output.getMessageSize();
            outputs.add(output);
        }
        lockTime = Utils.readUint32(in);
        read += 4;

        return read;
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        uint32ToByteStreamLE(version, stream);
        stream.write(new VarInt(inputs.size()).encode());
        for (TxInput in : inputs)
            in.serializeTo(stream);
        stream.write(new VarInt(outputs.size()).encode());
        for (TxOutput out : outputs)
            out.serializeTo(stream);
        uint32ToByteStreamLE(lockTime, stream);
    }

    @Override
    protected int estimateMessageLength() {
        int len = 4 + 4 + VarInt.sizeOf(getInputs().size()) + VarInt.sizeOf(getOutputs().size());
        len += getInputs().size() * (73 + 33); // sig + pubkey;
        len += getOutputs().size() + (4 + 1 + 20); // p2pkh - 4 opcodes, 1 pushdata, 20 byte hash
        len *= 2; //add some extra padding
        return len;
    }

    public Tx makeNew(byte[] serialized) {
        return new TxBean(null, serialize());
    }

    @Override
    public void makeSelfMutable() {
        super.makeSelfMutable();
        if (inputs != null) {
            for (TxInput in : getInputs()) {
                in.makeSelfMutable();
            }
        }
        if (outputs != null) {
            for (TxOutput out : getOutputs())
                out.makeSelfMutable();
        }
    }

}
