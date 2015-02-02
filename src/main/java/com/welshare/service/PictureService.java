package com.welshare.service;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.List;

import com.welshare.model.Message;
import com.welshare.model.Picture;

public interface PictureService {

    String UPLOADS_URI = "/pictures/";
    String UPLOADS_TEMP_URI = "/pictures/temp/";

    /**
     * Upload a picture file to disk
     *
     * @param data
     * @param originalFilename
     * @param user currently logged user
     *
     * @return the URL to the picture
     */
    String uploadPicture(byte[] data, String originalFilename, String userId);

    /**
     * Upload a picture file to a temporary storage on disk.
     * The file is deleted after a short period of time
     *
     * @param data
     * @param originalFilename
     * @param user currently logged user
     *
     * @return the URL to the picture
     */
    String uploadTempPicture(byte[] data, String originalFilename, String userId);


    String uploadProfilePicture(byte[] data, String originalFilename, String userId);

    /**
     * Transfers the file identified by the given filename to the
     * given output stream
     *
     * @param filename
     * @param outputStream target stream
     */
    void transferPicture(String filename, OutputStream outputStream);

    /**
     * Transfers the temp picture file identified by the given filename to the
     * given output stream
     *
     * @param filename
     * @param outputStream target stream
     */
    void transferTempPicture(String filename, OutputStream outputStream);

    /**
     * Moves the list of temporary picture files to a permanent location
     *
     * @param pictureUrls list of absolute file urls
     * @return a list of relative file names
     */
    List<String> makePermanent(List<String> pictureUrls);

    /**
     * Transfers a profile picture to the given output stream
     * @param filename
     * @param outputStream
     */
    void transferProfilePicture(String filename, OutputStream outputStream);

    void deleteFiles(Picture picture);

    String storedCroppedProfilePicture(BufferedImage image, String suffix, String filename, String format);

    Message getMessage(Picture picture);
}
