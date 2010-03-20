/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;

import org.appwork.utils.formatter.HexFormatter;

public class Files {
    /**
     * Returns the fileextension for a file with the given name
     * 
     * @param name
     * @return
     */
    public static String getExtension(String name) {

        final int index = name.lastIndexOf(".");
        if (index < 0) return null;
        return name.substring(index + 1).toLowerCase();

    }

    /**
     * Returns the mikmetype of the file. If unknown, it returns
     * Unknown/extension
     * 
     * @param name
     * @return
     */
    public static String getMimeType(String name) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String ret = fileNameMap.getContentTypeFor(name);
        if (ret == null) {
            ret = "unknown/" + getExtension(name);
        }
        return ret;
    }

    /**
     * 
     * Returns the hash checksum for the given file.
     * 
     * @param arg
     * @param type
     *            e.g. md5 or sha1
     * @return
     */
    public static String getHash(File arg, String type) {
        if (arg == null || !arg.exists() || arg.isDirectory()) return null;
        try {
            MessageDigest md = MessageDigest.getInstance(type);
            byte[] b = new byte[4096];
            InputStream in = new FileInputStream(arg);
            for (int n = 0; (n = in.read(b)) > -1;) {
                md.update(b, 0, n);
            }
            in.close();
            byte[] digest = md.digest();
            return HexFormatter.byteArrayToHex(digest);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the MD5 Hashsum for the file arg
     * 
     * @param arg
     * @return
     */
    public static String getMD5(File arg) {
        return getHash(arg, "md5");
    }

    /**
     * Returns all files which were find in a recursive search through all files
     * 
     * @param files
     * @return
     */
    public static ArrayList<File> getFiles(File... files) {
        ArrayList<File> ret = new ArrayList<File>();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    ret.addAll(getFiles(f.listFiles()));
                } else {
                    ret.add(f);
                }
            }
        }
        return ret;
    }

}
