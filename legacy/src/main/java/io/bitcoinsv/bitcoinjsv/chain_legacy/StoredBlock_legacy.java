/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.bitcoinsv.bitcoinjsv.chain_legacy;

import io.bitcoinsv.bitcoinjsv.core.CheckpointManager;
import io.bitcoinsv.bitcoinjsv.core.ProtocolException;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.msg.Serializer;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfoReadOnly;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Block;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Transaction;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.store_legacy.BlockStore_legacy;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import com.google.common.base.Objects;
import io.bitcoinsv.bitcoinjsv.utils.ObjectGetter;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkState;

/**
 * Wraps a {@link Block} object with extra data that can be derived from the block chain but is slow or inconvenient to
 * calculate. By storing it alongside the block header we reduce the amount of work required significantly.
 * Recalculation is slow because the fields are cumulative - to find the chainWork you have to iterate over every
 * block in the chain back to the genesis block, which involves lots of seeking/loading etc. So we just keep a
 * running total: it's a disk space vs cpu/io tradeoff.<p>
 *
 * StoredBlocks are put inside a {@link BlockStore_legacy} which saves them to memory or disk.
 */
@Deprecated
@SuppressWarnings("MutablePublicArray")     // we're not going to fix this legacy class
public class StoredBlock_legacy implements ChainInfoReadOnly {

    // A BigInteger representing the total amount of work done so far on this chain. As of May 2011 it takes 8
    // bytes to represent this field, so 12 bytes should be plenty for now.
    public static final int CHAIN_WORK_BYTES = 12;
    public static final byte[] EMPTY_BYTES = new byte[CHAIN_WORK_BYTES];
    public static final int COMPACT_SERIALIZED_SIZE = Block.HEADER_SIZE + CHAIN_WORK_BYTES + 4 + 4 + 8;  // for height, txCount, blockSize

    private Block header;
    private BigInteger chainWork;
    private int height;
    private int txCount = -1;
    private long blockSize = -1;

    private long coinbaseOffsetInFile = -1;
    private ObjectGetter<Transaction> coinbase;
    //TODO implement this
//    private Object coinbaseSPVProof;
    //TODO implement this
//    private Object txCountProof;

    private int txidFileNum = -1;
    private long txidFileOffset = -1;
    private ObjectGetter<List<Sha256Hash>> txids;


    public StoredBlock_legacy(Block header, BigInteger chainWork, int height) {
        this.header = header;
        this.chainWork = chainWork;
        this.height = height;
        if (header.getTransactions() != null && !header.getTransactions().isEmpty()) {
            txCount = header.getTransactions().size();
            blockSize = header.getSerializedLength();
            coinbase = ObjectGetter.direct(header.getTransactions().get(0));
            if (!coinbase.get().isCoinBase()) {
                throw new RuntimeException("first transaction is not a valid coinbase");
            }
        }
        txids = ObjectGetter.direct(header.getTxids());
    }

    public void checkIsHeaderOnly() {
//        if (header.getTransactions() == null || header.getTransactions().isEmpty())
//            return;
//        throw new RuntimeException("Stored block is contains full block isntance");
    }

    /**
     * The block header this object wraps. The referenced block object must not have any transactions in it.
     */
    public Block getHeader() {
        return header;
    }

    /**
     * The total sum of work done in this block, and all the blocks below it in the chain. Work is a measure of how
     * many tries are needed to solve a block. If the target is set to cover 10% of the total hash value space,
     * then the work represented by a block is 10.
     */
    public BigInteger getChainWork() {
        return chainWork;
    }

    /**
     * Position in the chain for this block. The genesis block has a height of zero.
     */
    public int getHeight() {
        return height;
    }

    @Override
    public long getTotalChainTxs() {
        return -1;
    }

    public int getTxCount() { return txCount; }

    public long getBlockSize() { return blockSize; }

    public long getCoinbaseOffsetInFile() {
        return coinbaseOffsetInFile;
    }

    public Transaction getCoinbase() {
        return coinbase == null ? null : coinbase.get();
    }

    public void setCoinbase(ObjectGetter<Transaction> coinbase) {
        this.coinbase = coinbase;
    }

    public int getTxidFileNum() {
        return txidFileNum;
    }

    public void setTxidFileNum(int txidFileNum) {
        this.txidFileNum = txidFileNum;
    }

    public long getTxidFileOffset() {
        return txidFileOffset;
    }

    public void setTxidFileOffset(long txidFileOffset) {
        this.txidFileOffset = txidFileOffset;
    }

    public List<Sha256Hash> getTxids() {
        return txids == null ? null : txids.get();
    }

    public void setTxids(ObjectGetter<List<Sha256Hash>> txids) {
        this.txids = txids;
    }

    public void setCoinbase(Transaction coinbase) {
        this.coinbase = ObjectGetter.direct(coinbase);
        //we don't wan't to set the coinbase only as parent as it won't change
        //and this will trigger uncaching the merkle root which we can't recalculate
        //without the rest of the transactions in block.  We'll just set parent to null
        //to release any block that's attached to the coinbase so it can be garbage collected.
        coinbase.setParent(null);
    }

    public void setTxCount(int txCount) {
        this.txCount = txCount;
    }

    public void setBlockSize(long blockSize) {
        this.blockSize = blockSize;
    }

    public void setCoinbaseOffsetInFile(long coinbaseOffsetInFile) {
        this.coinbaseOffsetInFile = coinbaseOffsetInFile;
    }

    /** Returns true if this objects chainWork is higher than the others. */
    public boolean moreWorkThan(StoredBlock_legacy other) {
        return chainWork.compareTo(other.chainWork) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredBlock_legacy other = (StoredBlock_legacy) o;
        return header.equals(other.header) && chainWork.equals(other.chainWork) && height == other.height;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(header, chainWork, height);
    }

    /**
     * Creates a new StoredBlock, calculating the additional fields by adding to the values in this block.
     */
    public StoredBlock_legacy build(Block block) throws VerificationException {
        // Stored blocks track total work done in this chain, because the canonical chain is the one that represents
        // the largest amount of work done not the tallest.
        BigInteger chainWork = this.chainWork.add(block.getWork());
        int height = this.height + 1;
        return new StoredBlock_legacy(block, chainWork, height);
    }

    /**
     * Given a block store, looks up the previous block in this chain. Convenience method for doing
     * <tt>store.get(this.getHeader().getPrevBlockHash())</tt>.
     *
     * @return the previous block in the chain or null if it was not found in the store.
     */
    public StoredBlock_legacy getPrev(BlockStore_legacy store) throws BlockStoreException {
        return store.get(getHeader().getPrevBlockHash());
    }

    /** Serializes the stored block to a custom packed format. Used by {@link CheckpointManager}. */
    public void serializeCompact(ByteBuffer buffer) {
        byte[] chainWorkBytes = getChainWork().toByteArray();
        checkState(chainWorkBytes.length <= CHAIN_WORK_BYTES, "Ran out of space to store chain work!");
        if (chainWorkBytes.length < CHAIN_WORK_BYTES) {
            // Pad to the right size.
            buffer.put(EMPTY_BYTES, 0, CHAIN_WORK_BYTES - chainWorkBytes.length);
        }
        buffer.put(chainWorkBytes);
        buffer.putInt(getHeight());
        buffer.putInt(getTxCount());
        buffer.putLong(getBlockSize());
        // Using unsafeBitcoinSerialize here can give us direct access to the same bytes we read off the wire,
        // avoiding serialization round-trips.
        //byte[] bytes = getHeader().unsafeBitcoinSerialize();
        byte[] bytes = header.serialize();
        buffer.put(bytes, 0, Block.HEADER_SIZE);  // Trim the trailing 00 byte (zero transactions).
    }

    /** De-serializes the stored block from a custom packed format. Used by {@link CheckpointManager}. */
    public static StoredBlock_legacy deserializeCompact(NetworkParameters params, ByteBuffer buffer) throws ProtocolException {
        byte[] chainWorkBytes = new byte[StoredBlock_legacy.CHAIN_WORK_BYTES];
        buffer.get(chainWorkBytes);
        BigInteger chainWork = new BigInteger(1, chainWorkBytes);
        int height = buffer.getInt();  // +4 bytes
        int txCount = buffer.getInt(); // + 4 bytes
        long blockSize = buffer.getLong(); // + 8 bytes
        byte[] header = new byte[Block.HEADER_SIZE + 1];    // Extra byte for the 00 transactions length.
        buffer.get(header, 0, Block.HEADER_SIZE);
        StoredBlock_legacy block = new StoredBlock_legacy(Serializer.defaultFor(params).makeBlock(header), chainWork, height);
        block.setTxCount(txCount);
        block.setBlockSize(blockSize);
        return block;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Block %s at height %d: %s",
                getHeader().getHashAsString(), getHeight(), getHeader().toString());
    }
}
