package org.bitcoinj.msg.bitcoin;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.script.Script;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;

public class InputBean extends BitcoinObjectImpl implements Input {

    // Allows for altering transactions after they were broadcast. Values below NO_SEQUENCE-1 mean it can be altered.
    private long sequenceNumber;
    // Data needed to connect to the output of the transaction we're gathering coins from.
    private OutPoint outpoint;
    // The "script bytes" might not actually be a script. In coinbase transactions where new coins are minted there
    // is no input transaction, so instead the scriptBytes contains some extra stuff (like a rollover nonce) that we
    // don't care about much. The bytes are turned into a Script object (cached below) on demand via a getter.
    private byte[] scriptBytes;
    // The Script object obtained from parsing scriptBytes. Only filled in on demand and if the transaction is not
    // coinbase.
    private Script scriptSig;
    /** Value of the output connected to the input, if known. This field does not participate in equals()/hashCode(). */
    @Nullable
    private Coin value;

    public InputBean(TxBean parent, byte[] payload, int offset) {
        super(parent, payload, offset);
    }

    public InputBean(byte[] payload) {
        this(null, payload, 0);
    }


    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public OutPoint getOutpoint() {
        return outpoint;
    }

    @Override
    public void setOutpoint(OutPoint outpoint) {
        this.outpoint = outpoint;
    }

    @Override
    public byte[] getScriptBytes() {
        //never return null, use empty array instead but if scriptSig
        //is available to covert that to bytes
        if (scriptBytes == null || EMPTY_ARRAY == scriptBytes) {
            scriptBytes = scriptSig == null ? EMPTY_ARRAY : scriptSig.getProgram();
        }
        return scriptBytes;
    }

    @Override
    public void setScriptBytes(byte[] scriptBytes) {
        this.scriptBytes = scriptBytes;
        scriptSig = null;
    }

    @Override
    public Script getScriptSig() {
        if (scriptSig == null && scriptBytes != EMPTY_ARRAY)
            scriptSig = new Script(scriptBytes);
        return scriptSig;
    }

    @Override
    public void setScriptSig(Script scriptSig) {
        this.scriptSig = scriptSig;
        scriptBytes = scriptSig.getProgram();
    }

    @Override
    @Nullable
    public Coin getValue() {
        return value;
    }

    @Override
    public void setValue(@Nullable Coin value) {
        this.value = value;
    }

    @Override
    protected void parse() {
        outpoint = new OutPointBean(this, payload, cursor);
        cursor += outpoint.getMessageSize();
        int scriptLen = (int) readVarInt();
        scriptBytes = readBytes(scriptLen);
        sequenceNumber = readUint32();
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        outpoint.serializeTo(stream);
        stream.write(new VarInt(getScriptBytes().length).encode());
        stream.write(getScriptBytes());
        Utils.uint32ToByteStreamLE(sequenceNumber, stream);
    }
}
