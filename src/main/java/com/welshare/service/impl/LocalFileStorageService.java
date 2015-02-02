package com.welshare.service.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import com.welshare.service.FileStorageService;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Override
    public void storeFile(String path, byte[] content) throws IOException {
         FileUtils.writeByteArrayToFile(new File(path), content);
    }

    @Override
    public void storeFile(String path, InputStream inputStream, long size)
            throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(path));
        IOUtils.copy(inputStream, os);
        os.close();
    }

    @Override
    public InputStream getFile(String path) throws IOException {
        return new FileInputStream(path);
    }

    @Override
    public void moveFile(String sourcePath, String targetPath)
            throws IOException {
        FileUtils.moveFile(new File(sourcePath), new File(targetPath));

    }

    @Override
    public void delete(String path) throws IOException {
        boolean result = new File(path).delete();
        if (!result) {
            throw new IOException("Failed to delete file " + path);
        }
    }
}
