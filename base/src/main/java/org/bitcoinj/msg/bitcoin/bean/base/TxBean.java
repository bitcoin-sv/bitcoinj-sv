package org.bitcoinj.msg.bitcoin.bean.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;
import org.bitcoinj.msg.bitcoin.api.base.Input;
import org.bitcoinj.msg.bitcoin.api.base.Output;
import org.bitcoinj.msg.bitcoin.api.base.Tx;
import org.bitcoinj.msg.bitcoin.bean.BitcoinObjectImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.bitcoinj.core.Utils.uint32ToByteStreamLE;

public class TxBean extends BitcoinObjectImpl<Tx> implements Tx {

    private Sha256Hash hash;

    // These are bitcoin serialized.
    private long version;

    private List<Input> inputs;

    private List<Output> outputs;

    private long lockTime;

    public TxBean(BitcoinObject parent, byte[] payload, int offset) {
        super(parent, payload, offset);
    }

    public TxBean(byte[] payload, int offset) {
        this(null, payload, offset);
    }

    public TxBean(BitcoinObject parent, byte[] payload) {
        this(parent, payload, 0);
    }

    public TxBean(byte[] payload) {
        this(null, payload, 0);
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
    public List<Input> getInputs() {
        return isMutable() ? inputs : Collections.unmodifiableList(inputs);
    }

    @Override
    public void setInputs(List<Input> inputs) {
        checkMutable();
        this.inputs = inputs;
    }

    @Override
    public List<Output> getOutputs() {
        return isMutable() ? outputs : Collections.unmodifiableList(outputs);
    }

    @Override
    public void setOutputs(List<Output> outputs) {
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
            Input input = new InputBean(this, payload, cursor);
            cursor += input.getMessageSize();
            inputs.add(input);
        }
        // Now the outputs
        long numOutputs = readVarInt();
        outputs = new ArrayList<>((int) numOutputs);
        for (long i = 0; i < numOutputs; i++) {
            Output output = new OutputBean(this, payload, cursor);
            cursor += output.getMessageSize();
            outputs.add(output);
        }
        lockTime = readUint32();

        hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(payload, offset, cursor - offset));
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
            Input input = new InputBean(this, in);
            read += input.getMessageSize();
            inputs.add(input);
        }

        // Now the outputs
        long numOutputs = new VarInt(in).value;
        outputs = new ArrayList<>((int) numOutputs);
        for (long i = 0; i < numOutputs; i++) {
            Output output = new OutputBean(this, in);
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
        for (Input in : inputs)
            in.serializeTo(stream);
        stream.write(new VarInt(outputs.size()).encode());
        for (Output out : outputs)
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
        for (Input in: getInputs()) {
            in.makeSelfMutable();
        }
        for (Output out: getOutputs())
            out.makeSelfMutable();
    }
}
