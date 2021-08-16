/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinj.tools;

import io.bitcoinj.core.Utils;
import io.bitcoinj.bitcoin.api.base.FullBlock;
import io.bitcoinj.bitcoin.bean.base.FullBlockBean;
import io.bitcoinj.params.Net;
import io.bitcoinj.params.NetworkParameters;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * A very quick and dirty block.dat parser.
 *
 * Specify where your bitcoind data directory is (typically /home/user/.bitcoin/blocks)
 * and provide a BlockParsedListener to recieve the FullBlocks as they are parsed.
 *
 * @author Steve Shadders
 */

public class BlkDatParser {

    private String dbLocation = "/home/shadders/.bitcoin/blocks";

    //NetworkParameters params;
    Net net;
    File folder;
    File f;
    RandomAccessFile file;
    byte[] buf = new byte[0];
    PosInputStream pos;
    long offset;
    private int blkNum = 0;
    private int fileBlocks = 0;
    private int blocksParsed = 0;

    private boolean buffered = true;

    private byte[] lenBytes = new byte[4];
    private int len;
    private byte[] bytes = new byte[4096];

    private long timeReading, timeParsing, startTime;

    int height = 0;
    private int stopHeight = -1;

    private List<BlockParsedListener> listeners;

    private boolean shutdown = false;



    public BlkDatParser(NetworkParameters params, String dbLocation, boolean buffered) throws FileNotFoundException {
        this(params, dbLocation, buffered, 0, null);
    }

    public BlkDatParser(NetworkParameters params, String dbLocation, boolean buffered, BlockParsedListener listener) throws FileNotFoundException {
        this(params, dbLocation, buffered, 0, listener);
    }

    public BlkDatParser(NetworkParameters params, String dbLocation, boolean buffered, int blknum, BlockParsedListener listener) throws FileNotFoundException {
        super();
        if (dbLocation == null) {
            File home = new File(System.getProperty("user.home"));
            File f = new File(home, ".bitcoin/blocks/");
            dbLocation = f.getAbsolutePath();
        }
        this.dbLocation = dbLocation;
        this.buffered = buffered;
        this.blkNum = blknum;
        if (listener == null)
            listener = new StdOutBlockParsedListener();
        if (listener != null)
            registerBlockParsedListener(listener);
        net = params.getNet();
        init();
    }

    public void registerStdOutListener() {
        registerBlockParsedListener(new StdOutBlockParsedListener());
    }

    public void registerBlockParsedListener(BlockParsedListener listener) {
        if (listeners == null) {
            listeners = new LinkedList<BlockParsedListener>();
        }
        listeners.add(listener);
    }

    private void init() throws FileNotFoundException {
        if (net == null)
            //params = TestNet3Params.get();
            net = Net.MAINNET;
        folder = new File(dbLocation);
        try {
            newFile(blkNum);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * @return the f
     */
    public File getF() {
        return f;
    }

    /**
     * number of the block000x.dat file not the block height.
     * @return the blkNum
     */
    public int getBlkNum() {
        return blkNum;
    }

    private void newFile(int blkNum) throws IOException {
        if (fileBlocks > 0) {
            int size = (int) (file.length() / fileBlocks);
            Date lastBlockTime = lastBlock == null ? null : lastBlock.getTimeAsDate();
            System.out.println("blocks: " + blocksParsed + " - New file, last file contained " + fileBlocks + " blocks" + " - avg length: " + size
                    + " bytes - time: " + lastBlockTime);
            System.gc();
            long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long availableMem = Runtime.getRuntime().maxMemory() - usedMem;
            System.out.println(String.format("totalMem: %s, maxMem: %s, freeMem: %s, usedMem: %s, availableMem: %s",
                    Utils.humanReadableByteCount(Runtime.getRuntime().totalMemory(), false),
                    Utils.humanReadableByteCount(Runtime.getRuntime().maxMemory(), false),
                    Utils.humanReadableByteCount(Runtime.getRuntime().freeMemory(), false),
                    Utils.humanReadableByteCount(usedMem, false),
                    Utils.humanReadableByteCount(availableMem, false)
            ));
        }
        fileBlocks = 0;
        f = new File(folder, buildFileName(blkNum));
        file = new RandomAccessFile(f, "r");
        offset = 0;
        if (buffered) {
            long start = System.currentTimeMillis();
            if (buf.length < file.length()) {
                buf = new byte[(int) (file.length() * 1.2f)];
                System.out.println("Allocating buffer bytes: " + buf.length);
            }
            file.read(buf);
            System.out.println("buffer filled in " + (System.currentTimeMillis() - start) + "ms");
            pos = new PosByteArrayInputStream(buf, 0, (int) file.length());
        } else {
            pos = new RandomAccessFileInputStream(file);
        }
        System.out.println("Opened file: " + f.getAbsolutePath());
    }

    private static String buildFileName(int blkNum) {
        StringBuilder sb = new StringBuilder(12);
        sb.append("blk");
        String blknum = String.valueOf(blkNum);
        for (int i = 0; i < 5 - blknum.length(); i++)
            sb.append('0');
        sb.append(blkNum);
        sb.append(".dat");
        return sb.toString();
    }

    public void run() {
        blocksParsed = 0;
        try {
            init();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        startTime = System.currentTimeMillis();
        FullBlock block = null;
        long start, end, len, cum = 0;
        float mb, rate;
        long startTime = System.currentTimeMillis();
        while (!shutdown) {
            try {
                // start = file.getFilePointer();
                start = pos.getPosition();
                block = nextBlock();

                if (block == null || (stopHeight > 0 && height >= stopHeight)) {
                    // end
                    long secs = (System.currentTimeMillis() - startTime) / 1000;

                    if (listeners != null) {
                        for (BlockParsedListener listener: listeners) {
                            listener.onComplete(secs);
                        }
                    }

                    return;
                }

                end = pos.getPosition();
                len = end - start;

                if (listeners != null) {
                    for (BlockParsedListener listener: listeners) {
                        listener.onBlockParsed(block, height, getF(), start, len);
                    }
                }

                height++;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

    }

    private int reallocs = 0;
    private int foundMarkers = 0;
    //private MovingAverage<Float> ma = new MovingAverage<Float>(30, true);

    private FullBlock lastBlock = null;

    /**
     * returns the next block in the /bitcoin/blocks/ dir.  Each blockxxx.dat file contains many blocks and may contain orphaned blocks.
     * @return The next block or null if no more blocks are available.
     * @throws IOException
     */
    public FullBlock nextBlock() throws IOException {

        long start = System.currentTimeMillis();
        if (!seekPastMagicBytes()) {
            //EOF, move to the next file.
            blkNum++;
            try {
                newFile(blkNum);
            } catch (FileNotFoundException e) {
                //run out of files this is all the blocks we have.
                return null;
            }
            seekPastMagicBytes();
        }

        pos.read(lenBytes, 0, lenBytes.length);
        len = (int) Utils.readUint32(lenBytes, 0);
        bytes = new byte[len];
        pos.read(bytes, 0, len);
        long end = System.currentTimeMillis();
        timeReading += end - start;
        // can't be certain the reported length is accurate.
        //Block block = new Block(params, bytes, false, false, len);
        //Block block = Serializer.get(net, true, false, false).makeBlock(bytes);
        FullBlock block = new FullBlockBean(bytes);
        lastBlock = block;
        blocksParsed++;

        timeParsing += System.currentTimeMillis() - end;
        fileBlocks++;
        return block;

    }

    /**
     * Blocks are stored exactly the way they appear on the wire in the p2p protocol.  All bitcoin wire messages begin with 4 magic bytes as a marker.
     * @return true on successful seek.  false if the byte are not found in the current file.
     * @throws IOException
     */
    public boolean seekPastMagicBytes() throws IOException {

        long packetMagic = net.params().getPacketMagic();
        long oldPacketMagic = net.params().getOldPacketMagic();

        int magicCursor = 3; // Which byte of the magic we're looking for
        // currently.
        int oldMagicCursor = 3;

        byte b;
        while (pos.available() > 0) {
            b = (byte) pos.read();
            // We're looking for a run of bytes that is the same as the packet
            // magic but we want to ignore partial
            // magics that aren't complete. So we keep track of where we're up
            // to with magicCursor.

            //check for packetMagic
            byte expectedByte = (byte) (0xFF & packetMagic >>> (magicCursor * 8));
            if (b == expectedByte) {
                magicCursor--;
                if (magicCursor < 0) {
                    // We found the magic sequence.
                    return true;
                } else {
                    // We still have further to go to find the next message.
                }
            } else {
                magicCursor = 3;
            }

            //check for oldPacketMagic
            byte oldExpectedByte = (byte) (0xFF & oldPacketMagic >>> (oldMagicCursor * 8));
            if (b == oldExpectedByte) {
                oldMagicCursor--;
                if (oldMagicCursor < 0) {
                    // We found the magic sequence.
                    return true;
                } else {
                    // We still have further to go to find the next message.
                }
            } else {
                oldMagicCursor = 3;
            }
        }
        return false;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public int getStopHeight() {
        return stopHeight;
    }

    public void setStopHeight(int stopHeight) {
        this.stopHeight = stopHeight;
    }

    public static boolean saveStringAsFile(String string, File outFile, boolean append) {
        FileWriter writer = null;
        boolean result = false;
        // System.out.println("writing file");
        try {
            if (outFile.getParentFile() != null)
                outFile.getParentFile().mkdirs();
            writer = new FileWriter(outFile, append);
            writer.write(string);
            writer.close();
            result = true;

        } catch (IOException ex) {
            // Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE,
            // null, ex);
            System.out.println(ex.getMessage());
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                //Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }

    public void shutdown() {
        shutdown = true;
    }

    private interface PosInputStream {

        public int getPosition() throws IOException;

        public int available() throws IOException;

        public int read() throws IOException;

        public int read(byte[] buf, int offset, int len) throws IOException;
    }

    private class PosByteArrayInputStream extends ByteArrayInputStream implements PosInputStream {

        public PosByteArrayInputStream(byte[] buf, int offset, int length) {
            super(buf, offset, length);
        }

        public int getPosition() {
            return pos;
        }

    }

    private class RandomAccessFileInputStream implements PosInputStream {

        RandomAccessFile file;

        public RandomAccessFileInputStream(RandomAccessFile file) {
            super();
            this.file = file;
        }

        public int getPosition() throws IOException {
            return (int) file.getFilePointer();
        }

        public int available() throws IOException {
            return (int) (file.length() - file.getFilePointer() - 1);
        }

        public int read() throws IOException {
            return file.read();
        }

        public int read(byte[] buf, int offset, int len) throws IOException {
            return file.read(buf, offset, len);
        }

    }

    private class StdOutBlockParsedListener implements BlockParsedListener {

        long end, cum = 0;
        float mb, rate;
        long startTime = System.currentTimeMillis();
        int total = 0;

        public void onBlockParsed(FullBlock block, int height, File currentFile, long start, long len) {
            total++;
            end = start + len;

            if (len > 0)
                cum += len;

            if (height % 5000 == 0) {
                mb = cum / (1024 * 1024f);
                mb = ((long) (mb * 100)) / 100f;
                long interval = System.currentTimeMillis() - startTime;
                rate = mb / (interval / 1000f);
                rate = ((long) (rate * 100)) / 100f;
                float rp = timeReading / (float) timeParsing;
                rp = ((long) (rp * 100)) / 100f;
                System.out.format(
                        "Read block %s start: %s end: %s - length: %s bytes - total %smb - rate %smb/sec -- File: %s "
                                + "r/p: %s\n", height, start, end, len, mb, rate, buildFileName(blkNum), rp);
                System.out.println("Block hash: " + block.getHashAsString());
            }
        }

        public void onComplete(long secs) {
            System.out.println("Finished processing blockchain in " + secs + " seconds.  Total blocks read: " + total);
        }
    }

}
