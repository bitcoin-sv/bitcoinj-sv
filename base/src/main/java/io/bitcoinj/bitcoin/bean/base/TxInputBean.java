/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.bitcoin.bean.base;

import io.bitcoinj.core.Coin;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.Utils;
import io.bitcoinj.core.VarInt;
import io.bitcoinj.bitcoin.api.base.TxInput;
import io.bitcoinj.bitcoin.api.base.TxOutPoint;
import io.bitcoinj.bitcoin.api.base.Tx;
import io.bitcoinj.bitcoin.bean.BitcoinObjectImpl;
import io.bitcoinj.script.Script;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TxInputBean extends BitcoinObjectImpl<TxInput> implements TxInput {

    // Allows for altering transactions after they were broadcast. Values below NO_SEQUENCE-1 mean it can be altered.
    private long sequenceNumber;
    // Data needed to connect to the output of the transaction we're gathering coins from.
    private TxOutPoint outpoint;
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

    public TxInputBean(Tx parent, byte[] payload, int offset) {
        super(parent, payload, offset);
    }

    public TxInputBean(byte[] payload) {
        this(null, payload, 0);
    }

    public TxInputBean(Tx txBean, InputStream in) {
        super(txBean, in);
    }

    /**
     * Constructor for building manually
     * @param parent
     */
    public TxInputBean(Tx parent) {
        super(parent);
        setOutpoint(new TxOutPointBean(this));
    }


    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public void setSequenceNumber(long sequenceNumber) {
        checkMutable();
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public TxOutPoint getOutpoint() {
        return outpoint;
    }

    @Override
    public void setOutpoint(TxOutPoint outpoint) {
        checkMutable();
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
        checkMutable();
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
        checkMutable();
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
        checkMutable();
        this.value = value;
    }

    @Override
    protected void parse() {
        outpoint = new TxOutPointBean(this, payload, cursor);
        cursor += outpoint.getMessageSize();
        int scriptLen = (int) readVarInt();
        scriptBytes = readBytes(scriptLen);
        sequenceNumber = readUint32();
    }

    @Override
    protected int parse(InputStream in) throws IOException {
        int read;
        outpoint = new TxOutPointBean(this, in);
        read = outpoint.getMessageSize();
        int scriptLen = (int) new VarInt(in).value;
        read += VarInt.sizeOf(scriptLen);
        scriptBytes = Utils.readBytesStrict(in, scriptLen);
        read += scriptLen;
        sequenceNumber = Utils.readUint32(in);
        read += 4;
        return read;
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        outpoint.serializeTo(stream);
        stream.write(new VarInt(getScriptBytes().length).encode());
        stream.write(getScriptBytes());
        Utils.uint32ToByteStreamLE(sequenceNumber, stream);
    }

    @Override
    protected int estimateMessageLength() {
        int scriptLen = getScriptBytes().length;
        return TxOutPoint.FIXED_MESSAGE_SIZE + 4 + VarInt.sizeOf(scriptLen) + scriptLen;
    }

    @Override
    public TxInput makeNew(byte[] serialized) {
        return new TxInputBean(serialized);
    }

    @Override
    public void makeSelfMutable() {
        super.makeSelfMutable();
        if (outpoint != null)
            outpoint.makeSelfMutable();
    }
}
