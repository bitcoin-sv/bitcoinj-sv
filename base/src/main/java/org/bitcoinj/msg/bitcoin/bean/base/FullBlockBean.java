package org.bitcoinj.msg.bitcoin.bean.base;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.msg.bitcoin.api.BitcoinObject;
import org.bitcoinj.msg.bitcoin.bean.extended.BlockMetaDataBean;
import org.bitcoinj.msg.bitcoin.api.base.FullBlock;
import org.bitcoinj.msg.bitcoin.api.base.Header;
import org.bitcoinj.msg.bitcoin.api.base.MerkleRootProvider;
import org.bitcoinj.msg.bitcoin.api.base.Tx;
import org.bitcoinj.msg.bitcoin.bean.BitcoinObjectImpl;
import org.bitcoinj.utils.Threading;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static org.bitcoinj.core.Sha256Hash.hashTwice;
import static org.bitcoinj.core.Sha256Hash.of;

public class FullBlockBean extends BitcoinObjectImpl<FullBlock> implements FullBlock, MerkleRootProvider {

    private HeaderBean header;

    private BlockMetaDataBean metaData;

    private List<Tx> transactions;

    public FullBlockBean(byte[] payload, int offset) {
        super(null, payload, offset);
    }

    public FullBlockBean(byte[] payload) {
        super(null, payload, 0);
    }

    public FullBlockBean(InputStream in) {
        super(null, in);
    }

    @Override
    public HeaderBean getHeader() {
        return header;
    }

    @Override
    public void setHeader(HeaderBean header) {
        checkMutable();
        this.header = header;
    }

    @Override
    public List<Tx> getTransactions() {
        return isMutable() ? transactions : Collections.unmodifiableList(transactions);
    }

    @Override
    public void setTransactions(List<Tx> transactions) {
        checkMutable();
        this.transactions = transactions;
    }

    public void calculateTxHashes() {
        CountDownLatch hashLatch = new CountDownLatch(getTransactions().size());
        for (Tx tx: getTransactions()) {
            //we are doing batches of hashes so we can take advantage of multi threading
            Threading.THREAD_POOL.execute(new Runnable() {
                @Override
                public void run() {
                    //calculates hash and caches it, this is 99% of the runtime
                    tx.getHash();
                    hashLatch.countDown();
                }
            });

            try {
                ExecutorService exec = Threading.THREAD_POOL;
                long awaiting = hashLatch.getCount();
                hashLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void parse() {

        header = new HeaderBean(this, payload, offset);
        cursor += header.getMessageSize();

        int numTransactions = (int) readVarInt();
        transactions = new ArrayList<>(numTransactions);
        CountDownLatch hashLatch = new CountDownLatch(numTransactions);

        for (int i = 0; i < numTransactions; i++) {
            TxBean tx = new TxBean(this, payload, cursor);
            transactions.add(tx);
            cursor += tx.getMessageSize();
        }

        header.afterBlockParse();

        //fill in the meta data
        buildMetaData();
    }

    @Override
    protected int parse(InputStream in) throws IOException {
        int read = 0;
        header = new HeaderBean(this, in);
        read += header.getMessageSize();

        int numTransactions = (int) new VarInt(in).value;
        read += VarInt.sizeOf(numTransactions);
        transactions = new ArrayList<>(numTransactions);

        for (int i = 0; i < numTransactions; i++) {
            TxBean tx = new TxBean(this, in);
            transactions.add(tx);
            read += tx.getMessageSize();
        }

        header.afterBlockParse();

        //fill in the meta data
        buildMetaData();

        return read;
    }

    private void buildMetaData() {
        //have to pass null so calling the setters doesn't clear parent payload.
        //it isn't part of the same serialized payload though so it doesn't need a parent
        metaData = new BlockMetaDataBean();
        metaData.setTxCount(transactions.size());
        metaData.setBlockSize(cursor - offset);
        metaData.makeImmutable();
    }

    @Override
    public void serializeTo(OutputStream stream) throws IOException {
        header.serializeTo(stream);
        stream.write(new VarInt(transactions.size()).encode());
        for (Tx tx : transactions) {
            tx.serializeTo(stream);
        }
    }

    @Override
    protected int estimateMessageLength() {
        int len = Header.FIXED_MESSAGE_SIZE;
        for (Tx tx: getTransactions())
            len += tx instanceof TxBean ? ((TxBean) tx).estimateMessageLength() : 500;
        return len;
    }

    @Override
    public FullBlock makeNew(byte[] serialized) {
        return new FullBlockBean(serialized);
    }

    @Override
    public void makeSelfMutable() {
        super.makeSelfMutable();
        header.makeSelfMutable(); //also nulls block hash
        header.setMerkleRoot(null); //needs to be nulled in case txs change.
        for (Tx tx: getTransactions())
            tx.makeSelfMutable();
    }

    /**
     * This is here for early testing, we'll replace it with something more efficient.
     * @return
     */

    @Override
    public Sha256Hash calculateMerkleRoot() {
        List<byte[]> tree = buildMerkleTree();
        return Sha256Hash.wrap(tree.get(tree.size() - 1));
    }

    private List<byte[]> buildMerkleTree() {
        // The Merkle root is based on a tree of hashes calculated from the transactions:
        //
        //     root
        //      / \
        //   A      B
        //  / \    / \
        // t1 t2 t3 t4
        //
        // The tree is represented as a list: t1,t2,t3,t4,A,B,root where each
        // entry is a hash.
        //
        // The hashing algorithm is double SHA-256. The leaves are a hash of the serialized contents of the transaction.
        // The interior nodes are hashes of the concenation of the two child hashes.
        //
        // This structure allows the creation of proof that a transaction was included into a block without having to
        // provide the full block contents. Instead, you can provide only a Merkle branch. For example to prove tx2 was
        // in a block you can just provide tx2, the hash(tx1) and B. Now the other party has everything they need to
        // derive the root, which can be checked against the block header. These proofs aren't used right now but
        // will be helpful later when we want to download partial block contents.
        //
        // Note that if the number of transactions is not even the last tx is repeated to make it so (see
        // tx3 above). A tree with 5 transactions would look like this:
        //
        //         root
        //        /     \
        //       1        5
        //     /   \     / \
        //    2     3    4  4
        //  / \   / \   / \
        // t1 t2 t3 t4 t5 t5
        ArrayList<byte[]> tree = new ArrayList<byte[]>();
        // Start by adding all the hashes of the transactions as leaves of the tree.
        for (Tx t : transactions) {
            tree.add(t.calculateHash().getBytes());
        }
        int levelOffset = 0; // Offset in the list where the currently processed level starts.
        // Step through each level, stopping when we reach the root (levelSize == 1).
        for (int levelSize = transactions.size(); levelSize > 1; levelSize = (levelSize + 1) / 2) {
            // For each pair of nodes on that level:
            for (int left = 0; left < levelSize; left += 2) {
                // The right hand node can be the same as the left hand, in the case where we don't have enough
                // transactions.
                int right = Math.min(left + 1, levelSize - 1);
                byte[] leftBytes = Utils.reverseBytes(tree.get(levelOffset + left));
                byte[] rightBytes = Utils.reverseBytes(tree.get(levelOffset + right));
                tree.add(Utils.reverseBytes(hashTwice(leftBytes, 0, 32, rightBytes, 0, 32)));
            }
            // Move to the next level.
            levelOffset += levelSize;
        }
        return tree;
    }
}
