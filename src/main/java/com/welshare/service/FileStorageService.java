package com.welshare.service;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {
    /**
     * Stores a file on a given path
     * @param path
     * @param content
     */
    void storeFile(String path, byte[] content) throws IOException;

    void moveFile(String sourcePath, String targetPath) throws IOException;

    InputStream getFile(String path) throws IOException;

    void delete(String path) throws IOException;

    void storeFile(String path, InputStream inputStream, long size) throws IOException;
}
