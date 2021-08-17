/**
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.bean.extended;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.core.VarInt;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.AbstractBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Header;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.TxIdBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.BitcoinObjectImpl;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.MerkleBuilder;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HeaderBean;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean that stores txids along with block header for convenience.
 */
public class TxIdBlockBean extends BitcoinObjectImpl<TxIdBlock> implements TxIdBlock<TxIdBlock> {

    private Header header;
    private List<Sha256Hash> txids;

    public TxIdBlockBean(byte[] payload, int offset) {
        super(null, payload, offset);
    }

    public TxIdBlockBean(InputStream in) {
        super(null, in);
    }

    public TxIdBlockBean() {
        super(null);
    }

    @Override
    public Header getHeader() {
        return header;
    }

    @Override
    public void setHeader(Header header) {
        this.header = header;
    }

    @Override
    public Sha256Hash calculateMerkleRoot() {
        return MerkleBuilder.calculateMerkleRoot(txids);
    }

    @Override
    public List<Sha256Hash> getTxids() {
        return txids;
    }

    @Override
    public void setTxids(List<Sha256Hash> txids) {
        this.txids = txids;
    }

    @Override
    protected void parse() {
        header = new HeaderBean(this, payload, cursor);
        cursor += header.getMessageSize();

        VarInt numTxs = new VarInt(payload, cursor);
        cursor += numTxs.getOriginalSizeInBytes();
        txids = new ArrayList<>((int) numTxs.value);
        for (int i = 0; i < numTxs.value; i++) {
            byte[] bytes = readBytes(32);
            Sha256Hash hash = Sha256Hash.wrap(bytes);
            txids.add(hash);
        }
    }

    @Override
    protected int parse(InputStream in) throws IOException {
        //have to parse the super class fields as well
        int read = 0;
        header = new HeaderBean(this, in);
        read = header.fixedSize();

        VarInt numTxs = new VarInt(in);
        read += numTxs.getOriginalSizeInBytes();
        txids = new ArrayList<>((int) numTxs.value);
        for (int i = 0; i < numTxs.value; i++) {
            byte[] bytes = Utils.readBytesStrict(in, 32);
            read += 32;
            Sha256Hash hash = Sha256Hash.wrap(bytes);
            txids.add(hash);
        }
        return read;
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        header.serializeTo(stream);
        stream.write(new VarInt(txids.size()).encode());
        for (Sha256Hash hash: txids)
            stream.write(hash.getBytes());
    }

    @Override
    public TxIdBlock makeNew(byte[] serialized) {
        return new TxIdBlockBean(serialized, 0);
    }

    @Override
    public AbstractBlock getBlock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearHash() {
        getHeader().clearHash();
    }
}
