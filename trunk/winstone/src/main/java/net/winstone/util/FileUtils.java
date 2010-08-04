package net.winstone.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

/**
 * FileUtils class group some utilities methods around file management.
 * 
 * @author Jerome Guibert
 */
public class FileUtils {
    /**
     * Copy source file to destination. If destination is a path then source file name is appended. If destination file exists then:
     * overwrite=true - destination file is replaced; overwrite=false - exception is thrown. For larger files (20Mb) we use streams copy,
     * and for smaller files we use channels.
     * 
     * @param src source file
     * @param dst destination file or path
     * @param overwrite overwrite destination file
     * @exception IOException I/O problem
     * @exception IllegalArgumentException illegal argument
     */
    
    public static void copy(final File src, File dst, final boolean overwrite) throws IOException, IllegalArgumentException {
        // checks
        if (!src.isFile() || !src.exists())
            throw new IllegalArgumentException("Source file '" + src.getAbsolutePath() + "' not found!");
        
        if (dst.exists())
            if (dst.isDirectory()) // Directory? -> use source file name
                dst = new File(dst, src.getName());
            else if (dst.isFile()) {
                if (!overwrite)
                    throw new IllegalArgumentException("Destination file '" + dst.getAbsolutePath() + "' already exists!");
            } else
                throw new IllegalArgumentException("Invalid destination object '" + dst.getAbsolutePath() + "'!");
        
        File dstParent = dst.getParentFile();
        if (!dstParent.exists())
            if (!dstParent.mkdirs())
                throw new IOException("Failed to create directory " + dstParent.getAbsolutePath());
        
        long fileSize = src.length();
        if (fileSize > 20971520l) { // for larger files (20Mb) use streams
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dst);
            try {
                int doneCnt = -1, bufSize = 32768;
                byte buf[] = new byte[bufSize];
                while ((doneCnt = in.read(buf, 0, bufSize)) >= 0)
                    if (doneCnt == 0)
                        Thread.yield();
                    else
                        out.write(buf, 0, doneCnt);
                out.flush();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                }
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        } else { // smaller files, use channels
            FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dst);
            FileChannel in = fis.getChannel(), out = fos.getChannel();
            try {
                long offs = 0, doneCnt = 0, copyCnt = Math.min(65536, fileSize);
                do {
                    doneCnt = in.transferTo(offs, copyCnt, out);
                    offs += doneCnt;
                    fileSize -= doneCnt;
                } while (fileSize > 0);
            } finally { // cleanup
                try {
                    in.close();
                } catch (IOException e) {
                }
                try {
                    out.close();
                } catch (IOException e) {
                }
                try {
                    fis.close();
                } catch (IOException e) {
                }
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        // http://www.ibm.com/developerworks/java/library/j-jtp09275.html?ca=dgr-jw22JavaUrbanLegends
        // System.out.println(">>> " + String.valueOf(src.length() / 1024) + " Kb, " + String.valueOf(System.currentTimeMillis() - q));
    }
    
    /**
     * Copy stream utility.
     * 
     * @param in input stream
     * @param out output stream
     * @throws IOException
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        copyStream(in, out, -1);
    }
    
    /**
     * Copy stream utility.
     * 
     * @param in input stream
     * @param out output stream
     * @param maxLen maximum length to copy (-1 unlimited).
     * @throws IOException
     */
    public static void copyStream(final InputStream in, final OutputStream out, final long maxLen) throws IOException {
        byte[] buf = new byte[4096 * 2];
        int len;
        if (maxLen <= 0) {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } else {
            long max = maxLen;
            while ((len = in.read(buf)) > 0) {
                if (len <= max) {
                    out.write(buf, 0, len);
                    max -= len;
                } else {
                    out.write(buf, 0, (int)max);
                    break;
                }
            }
        }
    }
}
