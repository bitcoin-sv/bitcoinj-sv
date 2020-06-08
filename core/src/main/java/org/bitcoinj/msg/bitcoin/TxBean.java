package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.VarInt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.bitcoinj.core.Utils.uint32ToByteStreamLE;

public class TxBean extends BitcoinObjectImpl implements Tx {

    private Sha256Hash hash;

    // These are bitcoin serialized.
    private long version;

    private List<Input> inputs;

    private List<Output> outputs;

    private long lockTime;

    public TxBean(BitcoinObjectImpl parent, byte[] payload, int offset) {
        super(parent, payload, offset);
    }

    public TxBean(byte[] payload, int offset) {
        this(null, payload, offset);
    }

    public TxBean(BitcoinObjectImpl parent, byte[] payload) {
        this(parent, payload, 0);
    }

    public TxBean(byte[] payload) {
        this(null, payload, 0);
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
        this.hash = hash;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public List<Input> getInputs() {
        return inputs;
    }

    @Override
    public void setInputs(List<Input> inputs) {
        this.inputs = inputs;
    }

    @Override
    public List<Output> getOutputs() {
        return outputs;
    }

    @Override
    public void setOutputs(List<Output> outputs) {
        this.outputs = outputs;
    }

    @Override
    public long getLockTime() {
        return lockTime;
    }

    @Override
    public void setLockTime(long lockTime) {
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
}
