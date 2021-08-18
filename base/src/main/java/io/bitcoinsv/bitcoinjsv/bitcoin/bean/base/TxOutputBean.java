/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.bean.base;

import io.bitcoinsv.bitcoinjsv.core.Coin;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.core.VarInt;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxOutput;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.BitcoinObjectImpl;
import io.bitcoinsv.bitcoinjsv.script.Script;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class TxOutputBean extends BitcoinObjectImpl<TxOutput> implements TxOutput {

    // The output's value is kept as a native type in order to save class instances.
    private Coin value;

    // A transaction output has a script used for authenticating that the redeemer is allowed to spend
    // this output.
    private byte[] scriptBytes;

    // The script bytes are parsed and turned into a Script on demand.
    private Script scriptPubKey;

    public TxOutputBean(Tx parent, byte[] payload, int offset) {
        super(parent, payload, offset);
    }

    public TxOutputBean(byte[] payload) {
        super(null, payload, 0);
    }

    public TxOutputBean(Tx parent) {
        super(parent);
    }

    public TxOutputBean(TxBean parent, InputStream in) {
        super(parent, in);
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
    protected int parse(InputStream in) throws IOException {
        int read;
        value = Coin.valueOf(Utils.readInt64(in));
        read = 8;
        int scriptLen = (int) new VarInt(in).value;
        read += VarInt.sizeOf(scriptLen);
        scriptBytes = Utils.readBytesStrict(in, scriptLen);
        read += scriptLen;
        return read;
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
    protected int estimateMessageLength() {
        int scriptLen = getScriptBytes().length;
        return 8 + VarInt.sizeOf(scriptLen) + scriptLen;
    }

    @Override
    public TxOutput makeNew(byte[] serialized) {
        return new TxOutputBean(serialized);
    }
}
