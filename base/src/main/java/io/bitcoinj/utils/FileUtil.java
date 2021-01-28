/**
 * Copyright (c) 2009 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.utils;

import io.bitcoinj.core.UnsafeByteArrayOutputStream;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUtil {

    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;

    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;

    /**
     * The number of bytes in a gigabyte.
     */
    public static final long ONE_GB = ONE_KB * ONE_MB;

    public static String getInputStreamAsString(InputStream is, boolean silentFail) {
        StringBuilder sb = null;
        Reader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuilder();
            char[] chars = new char[1 << 16];
            int length;
            while ((length = in.read(chars)) > 0) {
                sb.append(chars, 0, length);
            }
        } catch (Exception e) {
            if (!silentFail)
                e.printStackTrace();
            sb = null;
            try {
                in.close();
            } catch (Exception e1) {
                if (!silentFail)
                    e1.printStackTrace();
            }
        }
        return sb == null ? null : sb.toString();
    }

    public static byte[] getInputStreamAsBytes(InputStream is, boolean silentFail) {
        return getInputStreamAsBytes(is, silentFail, -1);
    }

    public static byte[] getInputStreamAsBytes(InputStream is, boolean silentFail, int sizeHint) {
        try {
            if (sizeHint == -1) {
                return is.readAllBytes();
            }
            UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(sizeHint);
            bos.writeFrom(is);
            return bos.toByteArray();
        } catch (IOException e) {
            if (!silentFail)
                e.printStackTrace();
            return null;
        }
    }

    public static String getFileAsString(File file) {
        try {
            return getInputStreamAsString(new FileInputStream(file), false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getFileAsString(String file) {
        return getFileAsString(new File(file));
    }

    public static boolean appendStringToFile(String string, File outFile) {
        return saveStringAsFile(string, outFile, true);
    }

    public static boolean appendStringToFile(String string, String outFile) {
        return saveStringAsFile(string, outFile, true);
    }

    public static boolean saveStringAsFile(String string, File outFile) {
        return saveStringAsFile(string, outFile, false);
    }

    public static byte[] getFileAsBytes(String file) {
        File f = new File(file);
        return getFileAsBytes(f);
    }


    public static byte[] getFileAsBytes(File f) {
        return getFileAsBytes(f, false);
    }

    public static byte[] getFileAsBytes(File f, boolean quiet) {
        FileInputStream fis = null;
        byte[] bytes = null;
        try {
            fis = new FileInputStream(f);
            bytes = getInputStreamAsBytes(fis, false, fis.available());
            fis.close();

        } catch (FileNotFoundException e) {
            if (!quiet)
                e.printStackTrace();
        } catch (IOException e) {
            if (!quiet)
                e.printStackTrace();
        }
        return bytes;
    }

    public static boolean saveBytesAsFile(byte[] bytes, File outFile, boolean append) {
        FileOutputStream writer = null;
        boolean result = false;
        // System.out.println("writing file");
        try {
            if (outFile.getParentFile() != null)
                outFile.getParentFile().mkdirs();
            writer = new FileOutputStream(outFile, append);
            writer.write(bytes);
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
                Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }

    public static boolean saveBytesAsFile(byte[] bytes, String outFile, boolean append) {
        return saveBytesAsFile(bytes, new File(outFile), append);
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
                Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }

    public static boolean saveStringAsFile(String string, String outFile) {
        return saveStringAsFile(string, new File(outFile));
    }

    public static boolean saveStringAsFile(String string, String outFile, boolean append) {
        return saveStringAsFile(string, new File(outFile), append);
    }

    /**
     * return all files in all subdirs of specified file(folder).
     *
     * @param file
     * @param filter null for no filter
     * @return
     */
    public static List<File> getSubFiles(File file, FilenameFilter filter) {
        List<File> fileList = new ArrayList();

        File[] files = file.listFiles(filter);
        for (File f : files) {
            if (f.isDirectory()) {
                fileList.addAll(getSubFiles(f, filter));
            } else {
                fileList.add(f);
            }
        }
        return fileList;
    }

    /**
     * return all directories in all subdirs of specified file(folder).
     *
     * @param file
     * @param filter null for no filter
     * @return
     */
    public static List<File> getSubDirs(File file, FilenameFilter filter) {
        List<File> fileList = new ArrayList();

        File[] files = file.listFiles(filter);
        for (File f : files) {
            if (f.isDirectory()) {
                fileList.add(f);
                fileList.addAll(getSubDirs(f, filter));
            }
        }
        return fileList;
    }

    public static boolean copyToDir(File src, File destDir, boolean overwrite) {
        return copyToFile(src, new File(destDir, src.getName()), overwrite);
    }

    public static boolean copyToDir(String src, String destDir, boolean overwrite) {
        return copyToDir(new File(src), new File(destDir), overwrite);
    }

    public static boolean copyToDir(File src, File destDir) {
        return copyToFile(src, new File(destDir, src.getName()));
    }

    public static boolean copyToDir(String src, String destDir) {
        return copyToDir(new File(src), new File(destDir));
    }

    public static boolean copyToFile(File src, File dest) {
        return copyToFile(src, dest, true);
    }

    public static boolean copyToFile(String src, String dest) {
        return copyToFile(new File(src), new File(dest));
    }

    public static boolean copyToFile(String src, String dest, boolean overwrite) {
        return copyToFile(new File(src), new File(dest), overwrite);
    }

    public static boolean copyToFile(File src, File dest, boolean overwrite) {
        if (src == null || !src.exists() || !src.isFile() || dest == null)
            return false;
        if (!overwrite && dest.exists())
            return false;
        File parent = dest.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs()))
            return false;
        try {
            if (src.getCanonicalPath().equals(dest.getCanonicalPath()))
                return false;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            if (out != null) ;
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if files are equal. To be equal they should have the same last
     * modified date and the same length
     *
     * @param file1
     * @param file2
     * @param checkName  also check if names match
     * @param checkBytes if all other checks pass check that the file is equal byte for
     *                   byte
     * @return
     */
    public static boolean equals(String file1, String file2, boolean checkName, boolean checkBytes) {
        return equals(new File(file1), new File(file2), checkName, checkBytes);
    }

    /**
     * Checks if files are equal. To be equal they should have the same last
     * modified date and the same length
     *
     * @param file1
     * @param file2
     * @param checkName  also check if names match
     * @param checkBytes if all other checks pass check that the file is equal byte for
     *                   byte
     * @return
     */
    public static boolean equals(File file1, File file2, boolean checkName, boolean checkBytes) {
        if (file1 == null)
            return file2 == null;
        if (file2 == null)
            return false;
        if (!(file1.exists() && file2.exists()))
            return false;
        if (checkName && !file1.getName().equals(file2.getName()))
            return false;
        if (file1.lastModified() != file2.lastModified())
            return false;
        if (file1.length() != file2.length())
            return false;
        if (checkBytes) {
            try {
                InputStream s1 = new FileInputStream(file1);
                InputStream s2 = new FileInputStream(file2);
                byte[] buf1 = new byte[1000];
                byte[] buf2 = new byte[1000];
                boolean finished = false;
                while (!finished) {
                    int s1len = s1.read(buf1);
                    int s2len = s2.read(buf2);
                    if (s1len != s2len)
                        return false;
                    if (s1len == -1)
                        finished = true;
                    for (int i = 0; i < s1len; i++) {
                        if (buf1[i] != buf2[i])
                            return false;
                    }
                }
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a human-readable flags of the file size, where the input
     * represents a specific number of bytes.
     *
     * @param size the number of bytes
     * @return a human-readable display value (includes units)
     */
    public static String byteCountToDisplaySize(long size, int fractionDigits) {
        String displaySize;

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(fractionDigits);

        if (size / ONE_GB > 0) {
            displaySize = nf.format((double) size / ONE_GB) + " GB";
        } else if (size / ONE_MB > 0) {
            displaySize = nf.format((double) size / ONE_MB) + " MB";
        } else if (size / ONE_KB > 0) {
            displaySize = nf.format((double) size / ONE_KB) + " KB";
        } else {
            displaySize = nf.format((double) size) + " bytes";
        }
        return displaySize;
    }

    public static File findDirInParents(String dirName) {
        try {
            File pwd = new File(".").getCanonicalFile();
            return findDirInParents(pwd, dirName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find a directory matching dirName in the parent heirarchy.  Starting in startDir and checking each parent directory sucessively until
     * either the dir is found or until we hit hte root directory.
     *
     * @param dirName
     * @return
     */
    public static File findDirInParents(File startDir, String dirName) {
        boolean found = false;
        boolean hitRoot = false;
        File pwd = startDir;

        while (!found && !hitRoot) {
            File target = new File(pwd, dirName);
            if (target.exists() && target.isDirectory()) {
                return target;
            }
            pwd = pwd.getParentFile();
            hitRoot = pwd == null;
        }
        return null;

    }

    /**
     * Taken from: https://www.baeldung.com/java-delete-directory
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        File[] contents = dir.listFiles();
        //will be null if it's file.
        if (contents != null) {
            for (File file : contents) {
                deleteDir(file);
            }
        }
        return dir.delete();
    }

}
