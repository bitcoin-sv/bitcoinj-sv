/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.blockstore;

import io.bitcoinsv.bitcoinjsv.bitcoin.Genesis;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.FullBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.LiteBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxBean;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.extended.LiteBlockBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.UnsafeByteArrayOutputStream;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.core.VarInt;
import io.bitcoinsv.bitcoinjsv.exception.BlockStoreException;
import io.bitcoinsv.bitcoinjsv.params.Net;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Similar to an SPVBlockstore except it maintains the full block header history.  It also loads and stores
 * all blocks headers in memory on startup.
 * <p>
 * optionally can also store all coinbase transactions
 * FIXME coinbase functionality hasn't been ported into LiteBlock yet so this remains TODO
 *
 * @author Steve Shadders
 */
public class FullHeadersBlockStore implements BlockStore {

    private static final Logger log = LoggerFactory.getLogger(FullHeadersBlockStore.class);

    public static final int FLAG_HAS_COINBASE = 1;
    public static final int FLAG_HAS_COINBASE_PROOF = 2;
    public static final int FLAG_HAS_TX_COUNT_PROOF = 4;
    public static final int FLAG_HAS_TX_IDS = 8;

    public static final String HEADER_MAGIC = "TNP2PHEADERSBLOCKSTORE";
    public static final byte[] HEADER_MAGIC_BYTES = HEADER_MAGIC.getBytes();

    //public static final int CHAIN_WORK_BYTES = ChainInfo.CHAIN_WORK_BYTES;
    //public static final byte[] EMPTY_BYTES = new byte[ChainInfo.CHAIN_WORK_BYTES];

    public static final int MAX_TXID_FILE_SIZE = 1024 * 1024 * 128;

    private static final int BASE_COMPACT_SERIALIZED_SIZE =
            // header, chainwork, height, txCount, blockSize
            LiteBlock.FIXED_MESSAGE_SIZE;

    private static final int BASE_METADATA_BUFFER_SIZE =
            // headerMagic, lastHeightPruned, flags, lastTxidFileNum, chainHeadBlockHeader
            HEADER_MAGIC_BYTES.length + 1 + 4 + 4;

    //private int METADATA_BUFFER_SIZE;

    protected Net net;

    protected ReentrantLock fileLock = new ReentrantLock();

    private LiteBlock chainHead;

    private final File dir;
    private final File txIdDir;

    //The main headers file, we read the entire file on startup then
    //it is append only so FileOutputStream is the most efficient way of writing it.
    private File headersFile;
    private FileOutputStream headerOutputStream;

    //Metadata is a short file that is constantly being overwritten
    //so we use a memory mapping and let the OS take care of flushing.
    //This is safe unless the JVM is killed in the middle of write operation
    //in which case we will possibly have some headers written to the main
    //file a second time next time we start up as the chainhead will not be updated
    //to reflect the highest chain head.
    private RandomAccessFile metadataRaf;
    private volatile MappedByteBuffer metadataBuffer;

    //The coinbases and possible other block meta data, this is currently
    //append only but may be read at any time, it isn't expected to be
    //read heavy but if it ever is it will likely be sequential so we
    //could make this a buffered flags in the future if read
    //performance ever becomes an issue.
    private RandomAccessFile cbDataRaf;

    //txids are written in 128mb files similar to blkxxxx.dat files in bitcoins
    //not implemented in this flags of the class.
    private int txidFileNum = 0;
    private File writeableTxidsFile;
    private OutputStream writeableTxidsOutputStream;
    private int writeableTxidsOutputStreamBytesWritten = 0;

    private int lastHeightPruned;

    private boolean spvMode;
    private boolean hasCoinbase = false;
    private boolean hasCoinbaseProof = false;
    private boolean hasTxCountProof = false;
    private boolean hasAnyCoinbaseData;
    private boolean hasTxids = false;

    private Map<Sha256Hash, LiteBlock> cache = new HashMap<Sha256Hash, LiteBlock>();

    public FullHeadersBlockStore(Net net, File dir) throws BlockStoreException {
        this(net, dir, false);
    }

    public FullHeadersBlockStore(Net net, File dir, boolean storeCoinbase) throws BlockStoreException {
        this(net, dir, storeCoinbase, false, false);
    }

    public FullHeadersBlockStore(Net net, File dir, boolean storeCoinbase, boolean deleteExisting, boolean storeTxids) throws BlockStoreException {
        this.net = net;
        this.dir = dir;
        this.txIdDir = new File(dir, "txids");
        String prefix = net.name();
        spvMode = true;
        this.hasCoinbase = storeCoinbase;
        this.hasTxids = storeTxids;
        hasAnyCoinbaseData = hasCoinbase | hasCoinbaseProof | hasTxCountProof;
        if (deleteExisting && dir.exists()) {
            FileUtil.deleteDir(dir);
        }
        boolean isExistingStore = dir.exists();
        dir.mkdirs();
        if (storeTxids) {
            txIdDir.mkdirs();
        }
        try {
            this.headersFile = new File(dir, "header-store-" + prefix + ".dat");

            this.headerOutputStream = new FileOutputStream(headersFile, true);
        } catch (FileNotFoundException e) {
            throw new BlockStoreException(e);
        }
        try {
            metadataRaf = new RandomAccessFile(new File(dir, "header-store-" + prefix + "-meta.dat"), "rw");
            if (metadataRaf.getChannel().tryLock() == null)
                throw new ChainFileLockedException("Store metadata file is already locked by another process");

            long metaSize = isExistingStore ? metadataRaf.length() : METADATA_BUFFER_SIZE();
            metadataBuffer = metadataRaf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, metaSize);
        } catch (Exception e) {
            throw new BlockStoreException(e);
        }
        if (hasAnyCoinbaseData) {
            try {
                cbDataRaf = new RandomAccessFile(new File(dir, "header-store-" + prefix + "-cb-data.dat"), "rw");
                if (cbDataRaf.getChannel().tryLock() == null)
                    throw new ChainFileLockedException("Store coinbase data file is already locked by another process");

            } catch (Exception e) {
                throw new BlockStoreException(e);
            }
        }

        if (isExistingStore) {
            initOldStore();
        } else {
            initNewStore();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
    }

    private int COMPACT_SERIALIZED_SIZE() {
        int size = BASE_COMPACT_SERIALIZED_SIZE;
        if (hasAnyCoinbaseData)
            size += 8;
        if (hasTxids)
            size += 8;
        return size;
    }

    private int METADATA_BUFFER_SIZE() {
        return BASE_METADATA_BUFFER_SIZE + COMPACT_SERIALIZED_SIZE();
    }

    public void start() {

        if (!spvMode) {
            Thread blockPruner = new Thread("block-pruner") {

                public void run() {
                    try {
                        Thread.sleep(30*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    boolean neverFalse = true;
                    while (neverFalse) { //suppress IDE warning
                        try {
                            pruneFullBlockStore(500);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            Thread.sleep(180*1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            blockPruner.setDaemon(true);
            blockPruner.start();
        }
        System.out.println("Initialized blockstore at: " + this.dir);
    }

    private void shutdown() {
        fileLock.lock();
        try {
            headerOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            metadataBuffer.force();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                log.info("Windows mmap hack: Forcing buffer cleaning");
                WindowsMMapHack.forceRelease(metadataBuffer);
            }
            metadataBuffer = null;  // Allow it to be GCd and the underlying file mapping to go away.
            metadataRaf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (cbDataRaf != null) {
                cbDataRaf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (writeableTxidsOutputStream != null) {
            try {
                writeableTxidsOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //don't unlock as we want to block any waiting headerOutputStream writes.
    }

    private void initNewStore() throws BlockStoreException {

        byte[] header;
        header = HEADER_MAGIC_BYTES;
        // Insert the genesis block.
        fileLock.lock();
        try {
            //FileUtil.saveBytesAsFile(header, headerOutputStream, false);
            try {
                headerOutputStream.write(header);
            } catch (IOException e) {
                throw new BlockStoreException(e);
            }

            FullBlock genesisFull = Genesis.getFor(net);
            if (FullBlockStore.get() != null)
                FullBlockStore.get().putBlock(genesisFull);

            LiteBlock storedGenesis = Genesis.getHeaderFor(net);
            put(storedGenesis);
            setChainHead(storedGenesis);

        } finally {
            fileLock.unlock();
        }
    }

    private void initOldStore() throws BlockStoreException {

        fileLock.lock();

        try {
            readMeta();

            byte[] bytes = FileUtil.getFileAsBytes(headersFile);
            int offset = 0;
            byte[] header = checkHeader(bytes, 0);
            offset += header.length;

            while (offset <= bytes.length - COMPACT_SERIALIZED_SIZE()) {

                final LiteBlock block = new LiteBlockBean(bytes, offset);
                offset += block.getMessageSize();
                cache.put(block.getHeader().getHash(), block);

            }

        } finally {
            fileLock.unlock();
        }

    }

    private void writeMeta() throws IOException {
        fileLock.lock();

        try {
            UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(METADATA_BUFFER_SIZE());
            bos.write(HEADER_MAGIC_BYTES);
            Utils.uint32ToByteStreamLE(lastHeightPruned, bos);
            int flags = (hasCoinbase ? FLAG_HAS_COINBASE : 0)
                    + (hasCoinbaseProof ? FLAG_HAS_COINBASE_PROOF : 0)
                    + (hasTxCountProof ? FLAG_HAS_TX_COUNT_PROOF : 0)
                    + (hasTxids ? FLAG_HAS_TX_IDS : 0);
            bos.write(flags);
            Utils.uint32ToByteStreamLE(txidFileNum, bos);

            while (bos.size() < METADATA_BUFFER_SIZE())
                bos.write(0);
            metadataBuffer.position(0);
            bos.writeTo(metadataBuffer);
        } finally {
            fileLock.unlock();
        }
    }

    private void readMeta() throws BlockStoreException {
        fileLock.lock();

        byte[] bytes = null;
        try {
            metadataBuffer.position(0);
            bytes = new byte[metadataBuffer.limit()];
            metadataBuffer.get(bytes);

            byte[] header = checkHeader(bytes, 0);
            int offset = header.length;

            lastHeightPruned = (int) Utils.readUint32(bytes, offset);
            offset += 4;

            int flags = bytes[offset];
            offset++;

            hasCoinbase = (flags & FLAG_HAS_COINBASE) == FLAG_HAS_COINBASE;
            hasCoinbaseProof = (flags & FLAG_HAS_COINBASE_PROOF) == FLAG_HAS_COINBASE_PROOF;
            hasTxCountProof = (flags & FLAG_HAS_TX_COUNT_PROOF) == FLAG_HAS_TX_COUNT_PROOF;
            hasAnyCoinbaseData = hasCoinbase | hasCoinbaseProof | hasTxCountProof;

            hasTxids = (flags & FLAG_HAS_TX_IDS) == FLAG_HAS_TX_IDS;

            txidFileNum = (int) Utils.readUint32(bytes, offset);
            offset += 4;

            chainHead = new LiteBlockBean(bytes, offset);
            offset += COMPACT_SERIALIZED_SIZE();
            cache.put(chainHead.getHeader().getHash(), chainHead);

        } finally {
            fileLock.unlock();
        }
    }

    private byte[] checkHeader(byte[] bytes, int offset) throws BlockStoreException {
        byte[] header = new byte[HEADER_MAGIC_BYTES.length];

        System.arraycopy(bytes, offset, header, 0, header.length);
        if (!new String(header).equals(HEADER_MAGIC))
            throw new BlockStoreException("Header bytes do not equal " + HEADER_MAGIC);

        return header;
    }

    public int getLastHeightPruned() {
        return lastHeightPruned;
    }

    public void setLastHeightPruned(int lastHeightPruned) throws IOException {
        this.lastHeightPruned = lastHeightPruned;
        writeMeta();
    }

    public void put(LiteBlock block) throws BlockStoreException {
        fileLock.lock();
        try {

            LiteBlock cached = cache.put(block.getHeader().getHash(), block);
            if (cached != null) {
                return;
            }

            //write it to disk
            //byte[] bytes = serializeValue(block, writeCBFile(block), writeTxidFile(block));
            //headerOutputStream.write(bytes);
            block.serializeTo(headerOutputStream);

        } catch (IOException e) {
            throw new BlockStoreException("Failed to write block: " + block ,e);
        } finally {
            fileLock.unlock();
        }
    }

//    private List<Sha256Hash> readTxIdFile(int fileNum, long offset, Sha256Hash expectedHash) throws BlockStoreException {
//        File f = new File(txIdDir, buildFileName(fileNum));
//        try {
//            RandomAccessFile raf = new RandomAccessFile(f, "r");
//            raf.seek(offset);
//
//
//            byte[] buf = new byte[80];
//            raf.read(buf);
//            Block header = Serializer.defaultFor(net).makeBlock(buf);
//            Sha256Hash blockHash = header.getHash();
//            if (!blockHash.equals(expectedHash))
//                throw new BlockStoreException("block hash for txids did not match expected");
//            //read a varint but we need to make sure we don't overrun the file
//            long pointer = raf.getFilePointer();
//            long remaining = raf.length() - pointer;
//            int lenToRead = remaining > buf.length ? buf.length : (int) remaining;
//            raf.read(buf, 0, lenToRead);
//            VarInt txCount = new VarInt(buf, 0);
//
//            raf.seek(pointer + txCount.getOriginalSizeInBytes());
//
//            List<Sha256Hash> txids = new ArrayList<>((int) txCount.value);
//
//            for (int i = 0; i < txCount.value; i++) {
//                raf.read(buf);
//                Sha256Hash hash = Sha256Hash.of(Arrays.copyOf(buf, buf.length));
//                txids.add(hash);
//            }
//
//            return txids;
//
//        } catch (Exception e) {
//            throw new BlockStoreException(e);
//        }
//    }
//
//    private TxidFileRef writeTxidFile(StoredBlock block) throws BlockStoreException {
//        if (!hasTxids)
//            return null;
//        try {
//            int written = 0;
//            OutputStream out = getTxidOutFileStream();
//            TxidFileRef ref = new TxidFileRef(txidFileNum, writeableTxidsFile.length());
//            byte[] headerBytes = block.getHeader().unsafeBitcoinSerialize();
//            if (headerBytes.length > Block.HEADER_SIZE)
//                throw new BlockStoreException("block was not header only");
//            out.write(headerBytes);
//            written += Block.HEADER_SIZE;
//            VarInt count = new VarInt(block.getTxids().size());
//            out.write(count.encode());
//            written += count.getOriginalSizeInBytes();
//            for (Sha256Hash txid: block.getTxids()) {
//                out.write(txid.getBytes());
//                written += 32;
//            }
//            writeableTxidsOutputStreamBytesWritten += written;
//            return ref;
//        } catch (Exception e) {
//            throw new BlockStoreException(e);
//        }
//    }

    private OutputStream getTxidOutFileStream() throws IOException {
        File f = null;
        if (writeableTxidsFile == null) {
            txIdDir.mkdirs();
            writeableTxidsFile = new File(txIdDir, buildFileName(txidFileNum));
            FileOutputStream fos = new FileOutputStream(writeableTxidsFile, true);
            writeableTxidsOutputStream = new BufferedOutputStream(fos, MAX_TXID_FILE_SIZE);
            writeableTxidsOutputStreamBytesWritten = 0;
        } else if (writeableTxidsOutputStreamBytesWritten > MAX_TXID_FILE_SIZE) {
            writeableTxidsOutputStream.close();
            txidFileNum++;
            writeableTxidsFile = new File(txIdDir, buildFileName(txidFileNum));
            writeableTxidsOutputStream = new FileOutputStream(writeableTxidsFile, true);
            writeableTxidsOutputStreamBytesWritten = 0;
        }
        return writeableTxidsOutputStream;
    }

//    /**
//     * returns the start index of the record in the coinbase file.
//     * @param block
//     * @return
//     * @throws IOException
//     */
//    private long writeCBFile(LiteBlock block) throws IOException {
//        if (!hasAnyCoinbaseData)
//            return -1;
//        long startIndex = cbDataRaf.length();
//        UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(2000);
//        if (hasCoinbase && block.getCoinbase() != null) {
//            byte[] cbBytes = block.getCoinbase().unsafeBitcoinSerialize();
//            VarInt cbLen = new VarInt(cbBytes.length);
//            bos.write(cbLen.encode());
//            bos.write(block.getCoinbase().unsafeBitcoinSerialize());
//        }
//        if (hasCoinbaseProof) {
//            //TODO implement serializing CB proof
//        }
//        if (hasTxCountProof) {
//            //TODO implement serialize txCount proof
//        }
//        if (bos.size() == 0)
//            return -1;
//        cbDataRaf.write(new VarInt(bos.size()).encode());
//        bos.writeTo(cbDataRaf);
//        return startIndex;
//    }

    public Tx readCBFile(LiteBlock block, long fileOffset) throws IOException {
        long available = cbDataRaf.length() - fileOffset;
        int lenToRead = (int) Math.min(available, 9);
        cbDataRaf.seek(fileOffset);
        byte[] lenBytes = new byte[lenToRead];
        cbDataRaf.readFully(lenBytes);
        VarInt len = new VarInt(lenBytes, 0);
        cbDataRaf.seek(fileOffset + len.getOriginalSizeInBytes());
        byte[] bytes = new byte[(int) len.value];
        cbDataRaf.readFully(bytes);
        int offset = 0;

        Tx coinbase = null;
        if (hasCoinbase) {
            VarInt cbLen = new VarInt(bytes,0);
            offset += cbLen.getOriginalSizeInBytes();
            //coinbase = Serializer.defaultFor(net).makeTransaction(bytes, offset, (int) cbLen.value, null);
            coinbase = new TxBean(null, bytes, offset);
        }

        if (hasCoinbaseProof) {
            //TODO implement this
        }

        if (hasTxCountProof) {
            //TODO implement this
        }
        return coinbase;
    }

    public LiteBlock get(Sha256Hash hash) throws BlockStoreException {
        return cache.get(hash);
    }

    public LiteBlock getChainHead() throws BlockStoreException {
        return chainHead;
    }

    public void setChainHead(LiteBlock newChainHead) throws BlockStoreException {
        fileLock.lock();
        try {
            if (newChainHead.equals(chainHead))
                return;
            chainHead = newChainHead;
            writeMeta();
        } catch (IOException e) {
            throw new BlockStoreException("Error writing metadata", e);
        } finally {
            fileLock.unlock();
        }
    }

    public void close() throws BlockStoreException {
        try {
            metadataRaf.close();
            cbDataRaf.close();
        } catch (IOException ex){
            throw new BlockStoreException(ex);
        }
    }

    @Override
    public NetworkParameters getParams() {
        return net.params();
    }

    public Net getNet() {
        return net;
    }

//    public StoredBlock deserializeValue(byte[] bytes, int offset) {
//
//        byte[] chainWorkBytes = new byte[CHAIN_WORK_BYTES];
//        System.arraycopy(bytes, offset, chainWorkBytes, 0, CHAIN_WORK_BYTES);
//        BigInteger chainWork = new BigInteger(1, chainWorkBytes);
//        offset += CHAIN_WORK_BYTES;
//
//        int height = (int) Utils.readUint32(bytes, offset);  // +4 bytes
//        offset += 4;
//
//        int txCount = (int) Utils.readUint32(bytes, offset); // + 4 bytes
//        offset += 4;
//
//        long blockSize = Utils.readInt64(bytes, offset); // + 8 bytes
//        offset += 8;
//
//        byte[] header = new byte[Block.HEADER_SIZE];    // Extra byte for the 00 transactions length.
//        System.arraycopy(bytes, offset, header, 0, Block.HEADER_SIZE);
//
//        StoredBlock block = new StoredBlock(Serializer.defaultFor(params).makeBlock(header), chainWork, height);
//        offset += Block.HEADER_SIZE;
//        block.setTxCount(txCount);
//        block.setBlockSize(blockSize);
//
//        if (block.getHeight() < 0) {
//            throw new RuntimeException("block height not set");
//        }
//
//        long coinbaseEntryIndex = -1;
//        if (hasAnyCoinbaseData) {
//            coinbaseEntryIndex = Utils.readInt64(bytes, offset); // + 8 bytes
//            offset += 8;
//        }
//        block.setCoinbaseOffsetInFile(coinbaseEntryIndex);
//
//        if (hasTxids) {
//            int fileNum = (int) Utils.readUint32(bytes, offset);
//            offset += 4;
//            long fileOffset = Utils.readUint32(bytes, offset);
//            offset += 4;
//            TxidFileRef ref = new TxidFileRef(fileNum, fileOffset);
//        }
//
//        return block;
//    }
//
//    public byte[] serializeValue(LiteBlock block, long cbIndexToWrite, TxidFileRef fileRef) {
//
//        int size = CHAIN_WORK_BYTES + 4 + 4 + 8 + 80 + 8;
//
//        ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(size);
//
//        byte[] chainWorkBytes = block.getChainWork().toByteArray();
//        checkState(chainWorkBytes.length <= CHAIN_WORK_BYTES, "Ran out of space to store chain work!");
//        try {
//            if (chainWorkBytes.length < CHAIN_WORK_BYTES) {
//                bos.write(EMPTY_BYTES, 0, CHAIN_WORK_BYTES - chainWorkBytes.length);
//            }
//            bos.write(chainWorkBytes);
//
//            Utils.uint32ToByteStreamLE(block.getHeight(), bos);
//            Utils.uint32ToByteStreamLE(block.getBlockMetaData().getTxCount(), bos);
//            Utils.int64ToByteStreamLE(block.getBlockMetaData().getBlockSize(), bos);
//            byte[] header = block.getHeader().unsafeBitcoinSerialize();
//            if (header.length > Block.HEADER_SIZE) {
//                byte[] h = new byte[Block.HEADER_SIZE];
//                System.arraycopy(header, 0, h, 0, Block.HEADER_SIZE);
//                header = h;
//            }
//            bos.write(header);
//
//            if (hasAnyCoinbaseData)
//                Utils.int64ToByteStreamLE(cbIndexToWrite,bos);
//
//            if (hasTxids) {
//                if (fileRef == null)
//                    throw new RuntimeException("no txidFileRef provided");
//                Utils.uint32ToByteStreamLE(fileRef.fileNum, bos);
//                Utils.uint32ToByteStreamLE(fileRef.offset, bos);
//            }
//
////            if (cbIndexToWrite > 0) {
////
////                VarInt cbLen = new VarInt(block.getCoinbase().getSerializedLength());
////                bos.write(cbLen.encode());
////                bos.write(block.getCoinbase().unsafeBitcoinSerialize());
////            }
//
//            byte[] bytes = bos.toByteArray();
//
//            return bytes;
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//    }

    public List<Sha256Hash> pruneFullBlockStore(int maxReorgDepth) throws BlockStoreException, IOException {

        List<Sha256Hash> pruned;

        int highestToKeep;
        long startPruneTime;
        long startTime;
        long trimBytes;
        int deleted;

        startTime = System.currentTimeMillis();
        trimBytes = 0;

        //walk back from best commited block to find the oldest block we want to keep
        //StoredBlock currentBlock = getChainHead();
        LiteBlock currentBlock = getChainHead();

        if (currentBlock == null) {
            //this can happen at startup
            return Collections.emptyList();
        }

        int tipHeight = currentBlock.getHeight();
        int oldOldestFullBlockHeight = getLastHeightPruned();
        highestToKeep = tipHeight - maxReorgDepth;
        if (oldOldestFullBlockHeight >= highestToKeep)
            return Collections.emptyList();

        int currentHeight = tipHeight;
        int counter = 0;

        pruned = new LinkedList<Sha256Hash>();
        LiteBlock lastCurrentBlock = null;

        while (currentBlock.getHeight() >= lastHeightPruned
                && !Genesis.getFor(net).equals(currentBlock.getHeader())) {

            if (currentBlock.getHeight() < highestToKeep) {
                pruned.add(currentBlock.getHeader().getHash());
            }
            lastCurrentBlock = currentBlock;
            currentBlock = get(currentBlock.getHeader().getPrevBlockHash());
        }

        deleted = 0;
        startPruneTime = System.currentTimeMillis();

        for (Sha256Hash deletable : pruned) {
            try {
                long reclaimed = FullBlockStore.get().deleteBlock(deletable);
                if (reclaimed != -1) {
                    deleted++;
                    trimBytes += reclaimed;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        setLastHeightPruned(highestToKeep);

        long completeTime = System.currentTimeMillis();
        log.info("Full block prune deleted {} entries and took {}ms. Walkback took {}ms. File prune took {}ms and reclaimed {} bytes",
                deleted,
                completeTime - startTime,
                startPruneTime - startTime,
                completeTime - startPruneTime,
                Utils.humanReadableByteCount(trimBytes)
        );
        return pruned;

    }

    private static String buildFileName(int filenum) { return buildFileName(filenum, 6); }

    private static String buildFileName(int filenum, int digits) {
        StringBuilder sb = new StringBuilder(12);
        sb.append("liteblk");
        String blknum = String.valueOf(filenum);
        for (int i = 0; i < digits - blknum.length(); i++)
            sb.append('0');
        sb.append(filenum);
        sb.append(".dat");
        return sb.toString();
    }

    private class TxidFileRef {
        int fileNum;
        long offset;

//        public TxidFileRef(StoredBlock_legacy block) {
//            fileNum = block.getTxidFileNum();
//            offset = block.getTxidFileOffset();
//        }

        public TxidFileRef(int fileNum, long offset) {
            this.fileNum = fileNum;
            this.offset = offset;
        }
    }

}
