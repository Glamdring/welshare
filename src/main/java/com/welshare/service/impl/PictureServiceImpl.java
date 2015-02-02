package com.welshare.service.impl;

import static com.welshare.util.WebUtils.addSuffix;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.ResampleOp;
import com.welshare.dao.MessageDao;
import com.welshare.model.Message;
import com.welshare.model.Picture;
import com.welshare.service.FileStorageService;
import com.welshare.service.PictureService;
import com.welshare.service.enums.PictureSize;
import com.welshare.util.Constants;

@Service
public class PictureServiceImpl implements PictureService {

    private static final Logger logger = LoggerFactory.getLogger(PictureServiceImpl.class);

    @Resource(name="${filesystem.implementation}")
    private FileStorageService fileStorageService;

    @Inject
    private MessageDao messageDao;

    @Value("${uploads.dir}")
    private String uploadsDir;

    @Value("${uploads.temp.dir}")
    private String uploadsTempDir;

    @Value("${profile.pictures.dir}")
    private String profilePicturesDir;

    @Value("${base.url.any}" +  PictureService.UPLOADS_URI)
    private String urlPrefix;

    @Value("${base.url.any}" + PictureService.UPLOADS_TEMP_URI)
    private String tempUrlPrefix;

    @Value("${base.url.any}" + PictureService.UPLOADS_URI + "profile/")
    private String profileUrlPrefix;

    @Value("${rescale.small.width}")
    private int rescaleSmallWidth;
    @Value("${rescale.small.height}")
    private int rescaleSmallHeight;

    @Value("${rescale.large.width}")
    private int rescaleLargeWidth;
    @Value("${rescale.large.height}")
    private int rescaleLargeHeight;

    @PostConstruct
    public void init() {
        if (!uploadsDir.endsWith(Constants.FORWARD_SLASH)) {
            throw new IllegalArgumentException("uploads.dir property must end with a slash");
        }
        if (!uploadsTempDir.endsWith(Constants.FORWARD_SLASH)) {
            throw new IllegalArgumentException("uploads.temp.dir property must end with a slash");
        }
        if (!profilePicturesDir.endsWith(Constants.FORWARD_SLASH)) {
            throw new IllegalArgumentException("profile.pictures.dir property must end with a slash");
        }

        // create the needed directories
        new File(uploadsDir).mkdirs();
        new File(uploadsTempDir).mkdirs();
        new File(profilePicturesDir).mkdirs();
    }

    @Override
    public String uploadPicture(byte[] data, String originalFilename, String userId) {
        return uploadPicture(data, originalFilename, userId, uploadsDir, urlPrefix, true, false);
    }

    @Override
    public String uploadTempPicture(byte[] data, String originalFilename, String userId) {
        return uploadPicture(data, originalFilename, userId, uploadsTempDir, tempUrlPrefix, true, false);
    }

    @Override
    public String uploadProfilePicture(byte[] data, String originalFilename, String userId) {
        return uploadPicture(data, originalFilename, userId, profilePicturesDir, profileUrlPrefix, false, true);
    }

    private String uploadPicture(byte[] data, String originalFilename, String userId,
            String targetDir, String urlPrefix, boolean storeSmall, boolean simpleFilename) {

        String extension = StringUtils.trimToEmpty(
                    FilenameUtils.getExtension(originalFilename)).toLowerCase();

        String filename;
        if (simpleFilename) {
            filename = userId + FilenameUtils.EXTENSION_SEPARATOR_STR + extension;
        } else {
            filename = userId + "_" + UUID.randomUUID().toString()
                + FilenameUtils.EXTENSION_SEPARATOR_STR + extension;
        }


        try {
            if (storeSmall) {
                String smallFilename;
                if (simpleFilename) {
                    smallFilename = targetDir + filename;
                } else {
                    smallFilename = addSuffix(targetDir + filename, PictureSize.SMALL.getSuffix());
                }
                fileStorageService.storeFile(smallFilename, rescaleSmall(data, extension));
            }

            String largeFilename;
            if (simpleFilename) {
                largeFilename = targetDir + filename;
            } else {
                largeFilename = addSuffix(targetDir + filename, PictureSize.LARGE.getSuffix());
            }
            fileStorageService.storeFile(largeFilename, rescaleLarge(data, extension));

            return urlPrefix + filename;
        } catch (IOException ex) {
            logger.error("Failed to store file in temp folder", ex);
            return null;
        }

    }

    private byte[] rescaleLarge(byte[] data, String format) throws IOException {
        return rescale(data, rescaleLargeWidth, rescaleLargeHeight, format);
    }

    private byte[] rescaleSmall(byte[] data, String format) throws IOException {
        return rescale(data, rescaleSmallWidth, rescaleSmallHeight, format);
    }

    private String stripStandardSuffixes(String path) {
        return path.replace(PictureSize.SMALL.getSuffix(), "").replace(PictureSize.LARGE.getSuffix(), "");
    }

    @Override
    public void transferPicture(String filename, OutputStream outputStream) {
        transferFile(filename, outputStream, uploadsDir);
    }

    @Override
    public void transferTempPicture(String filename, OutputStream outputStream) {
        transferFile(filename, outputStream, uploadsTempDir);
    }

    @Override
    public void transferProfilePicture(String filename,
            OutputStream outputStream) {
        transferFile(filename, outputStream, profilePicturesDir);
    }

    private void transferFile(String filename, OutputStream outputStream,
            String sourceDir) {
        InputStream is = null;
        try {
            is = fileStorageService.getFile(sourceDir + filename);
            IOUtils.copy(is, outputStream);
        } catch (SocketException ex) {
            logger.info("Problem (most likely broken pipe) obtaining image");
        } catch (IOException ex) {
            if (ExceptionUtils.getRootCause(ex) instanceof SocketException) {
                logger.info("Problem (most likely broken pipe) obtaining image");
            } else {
                logger.warn("Problem obtaining image", ex);
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Override
    public List<String> makePermanent(List<String> pictureUrls) {
        List<String> permanent = new ArrayList<String>(pictureUrls.size());

        for (String url : pictureUrls) {
            String baseFilename = stripStandardSuffixes(url.replace(tempUrlPrefix, ""));
            String src = uploadsTempDir + baseFilename;
            src = stripStandardSuffixes(src);

            String dest = uploadsDir + baseFilename;
            try {

                fileStorageService.moveFile(
                        addSuffix(src, PictureSize.SMALL.getSuffix()),
                        addSuffix(dest, PictureSize.SMALL.getSuffix()));

                fileStorageService.moveFile(
                        addSuffix(src, PictureSize.LARGE.getSuffix()),
                        addSuffix(dest, PictureSize.LARGE.getSuffix()));

                permanent.add(urlPrefix + baseFilename);
            } catch (IOException e) {
                logger.error("Failed to move file " + src + " to destination "
                        + dest, e);
            }
        }

        return permanent;
    }

    private byte[] rescale(byte[] data, int width, int height, String format) throws IOException {

        BufferedImage original = ImageIO.read(new ByteArrayInputStream(data));

        if (original == null) {
            throw new IOException("Unsupported file format!");
        }

        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // don't up-scale
        if (originalWidth <= width && originalHeight <= height) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(original, format, baos);
            return baos.toByteArray();
        }

        float factorX = (float) originalWidth / width;
        float factorY = (float) originalHeight / height;

        // keep aspect
        factorX = Math.max(factorX, factorY);
        factorY = factorX;

        ResampleOp op = new ResampleOp(Math.round(originalWidth / factorX),
                Math.round(originalHeight / factorY));

        op.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
        BufferedImage result = op.filter(original, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(result, format, baos);
        return baos.toByteArray();
    }

    @Override
    public void deleteFiles(Picture picture) {
        try {
            String path = uploadsDir + picture.getPath().replace(urlPrefix, "");
            fileStorageService.delete(addSuffix(path, PictureSize.SMALL.getSuffix()));
            fileStorageService.delete(addSuffix(path, PictureSize.LARGE.getSuffix()));
        } catch (IOException e) {
            logger.error("Error deleting picture with id " + picture.getId(), e);
        }
    }

    @Override
    public String storedCroppedProfilePicture(BufferedImage image,
            String suffix, String filename, String format) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, format, baos);

            String fullFilename = filename + suffix + "." + format;
            fileStorageService.storeFile(profilePicturesDir + fullFilename, baos.toByteArray());
            return profileUrlPrefix + fullFilename;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    @Transactional
    public Message getMessage(Picture picture) {
        return messageDao.getMessages(picture);
    }
}
