/*
 * Copyright (c) 2017 Steve Shadders
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.blockstore;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.FullBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.FullBlockBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.utils.FileUtil;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple file store for blocks that seperates into dirs based on first 12 bits to keep the file count reasonable.
 * Also contains an in memory cache limited to 200 entries or 130mb total bytes. This is based on the serialized bytes
 * however so real memory usage will be more than this.
 *
 * @author Steve Shadders
 */
public class FullBlockStore {

    private final static boolean OVERWRITE_FILES = true;
    private final static long MAX_CACHE_BYTES = 130 * 1000 * 1000;
    private final static long MAX_CACHE_ENTRIES = 1200;

    private static FullBlockStore instance = null;

    private final File baseDir;
    private final NetworkParameters params;

    private final long[] cacheSize = new long[1];

    private final boolean memoryOnly;

    private Map<Sha256Hash, FullBlock> cache = new LinkedHashMap<Sha256Hash, FullBlock>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Sha256Hash, FullBlock> eldest) {

            if (size() > MAX_CACHE_ENTRIES || cacheSize[0] > MAX_CACHE_BYTES) {
                long len = eldest.getValue().getMessageSize();
                cacheSize[0] -= len;
                return true;
            }
            return false;
        }
    };

    public static FullBlockStore get() {
        return instance;
    }

    public static void initMemoryOnly(NetworkParameters params) {
        new FullBlockStore(null, params, true);
    }

    public FullBlockStore(File baseDir, NetworkParameters params, boolean memoryOnly) {
        if (instance != null) {
            throw new RuntimeException("Cannot create more than one instance of " + getClass());
        }

        this.baseDir = baseDir == null ? null : addNetSuffix(baseDir, params);
        this.params = params;
        this.memoryOnly = memoryOnly;
        instance = this;
    }

    public FullBlockStore(File baseDir, NetworkParameters params) {
        this(baseDir, params, false);

    }

    public synchronized long deleteBlock(Sha256Hash hash) {
        FullBlock removed = cache.get(hash);
        long reclaimed = -1;
        if (removed != null) {
            cacheSize[0] -= removed.getMessageSize();
            cache.remove(hash);
        }
        if (memoryOnly) {
            return -1;
        }

        File f = getFile(hash);
        if (f.exists()) {
            reclaimed = f.length();
            f.delete();
            File parent = f.getParentFile();
            try {
                parent.delete();
            } catch (Exception e) {
                //swallow it, this is expected if the dir isn't empty.
            }
            return reclaimed;
        }
        return reclaimed;
    }

    /**
     * The block if found or null
     * @param hash
     * @return The block if found or null
     */
    public synchronized FullBlock loadBlock(Sha256Hash hash) {
        FullBlock block = cache.get(hash);
        if (block == null && !memoryOnly) {
            File f = getFile(hash);
            if (f.exists()) {
                byte[] bytes = FileUtil.getFileAsBytes(f);
                //block = Serializer.get(params,true, true).makeBlock(bytes);
                block = new FullBlockBean(bytes);
                cacheSize[0] += bytes.length;
                cache.put(hash, block);
            }
        }
        return block;
    }

    public synchronized boolean hasBlock(Sha256Hash hash) {
        if (cache.containsKey(hash))
            return true;
        if (memoryOnly)
            return false;
        File f = getFile(hash);
        return f.exists();
    }

    /**
     * @param block
     * @return true if the file was written as a new block, false if it already exists
     */
    public synchronized boolean putBlock(FullBlock block) {
        if (!isFullBlock(block))
            throw new RuntimeException("Cannot save full block with no transactions");
        Sha256Hash hash = block.getHash();

        File f = null;
        if (!memoryOnly) {
            f = getFile(hash);
            if (!OVERWRITE_FILES && f.exists()) {
                return false;
            }
        }

        byte[] bytes = block.serialize();
        cacheSize[0] += bytes.length;
        cache.put(hash, block);
        if (!memoryOnly)
            FileUtil.saveBytesAsFile(bytes, f, false);
        return true;
    }

    private File getFile(Sha256Hash hash) {
        String hex = hash.toString();
        String subDir1 = hex.substring(hex.length() - 3, hex.length() - 1);
        String subDir2 = hex.substring(hex.length() - 1);
        String subDir = hex.substring(hex.length() - 3);
        File dir = new File(baseDir, subDir);
        dir.mkdirs();
        File f = new File(dir, hex);
        return f;
    }

    public static boolean isFullBlock(FullBlock block) {
        return block != null && block.getTransactions() != null && !block.getTransactions().isEmpty();
    }

    private static File addNetSuffix(File f, NetworkParameters params) {
        String name = f.getName();
        String baseName = name;
        String extension = null;
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex >= 0) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex + 1);
        }
        String suffix = params.getNet().name();
        name = baseName + "_" + suffix;
        if (extension != null)
            name += "." + extension;
        File newFile = new File(f.getParent(), name);
        return newFile;
    }

}
