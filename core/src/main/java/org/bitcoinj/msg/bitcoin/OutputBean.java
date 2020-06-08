package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.script.Script;

import java.io.IOException;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class OutputBean extends BitcoinObjectImpl<Output> implements Output {

    // The output's value is kept as a native type in order to save class instances.
    private Coin value;

    // A transaction output has a script used for authenticating that the redeemer is allowed to spend
    // this output.
    private byte[] scriptBytes;

    // The script bytes are parsed and turned into a Script on demand.
    private Script scriptPubKey;

    public OutputBean(TxBean parent, byte[] payload, int offset) {
        super(parent, payload, offset);
    }

    public OutputBean(byte[] payload) {
        super(null, payload, 0);
    }

    public OutputBean(BitcoinObject parent) {
        super(parent);
    }

    @Override
    public Coin getValue() {
        return value;
    }

    @Override
    public void setValue(Coin value) {
        checkMutable();
        this.value = value;
    }

    @Override
    public byte[] getScriptBytes() {
        //never return null, use empty array instead but if scriptPubKey
        //is available to covert that to bytes
        if (scriptBytes == null || EMPTY_ARRAY == scriptBytes) {
            scriptBytes = scriptPubKey == null ? EMPTY_ARRAY : scriptPubKey.getProgram();
        }
        return scriptBytes;
    }

    @Override
    public void setScriptBytes(byte[] scriptBytes) {
        checkMutable();
        this.scriptBytes = scriptBytes;
        scriptPubKey = null;
    }

    @Override
    public Script getScriptPubKey() {
        if (scriptPubKey == null)
            scriptPubKey = new Script(getScriptBytes());
        return scriptPubKey;
    }

    @Override
    public void setScriptPubKey(Script scriptPubKey) {
        checkMutable();
        this.scriptPubKey = scriptPubKey;
        scriptBytes = scriptPubKey.getProgram();
    }

    @Override
    protected void parse() {
        value = Coin.valueOf(readInt64());
        int scriptLen = (int) readVarInt();
        scriptBytes = readBytes(scriptLen);
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        checkNotNull(scriptBytes);
        Utils.int64ToByteStreamLE(value.value, stream);
        // TODO: Move script serialization into the Script class, where it belongs.
        stream.write(new VarInt(getScriptBytes().length).encode());
        stream.write(getScriptBytes());
    }

    @Override
    public Output makeNew(byte[] serialized) {
        return new OutputBean(serialized);
    }
}
