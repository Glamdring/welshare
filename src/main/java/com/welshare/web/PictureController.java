package com.welshare.web;

import java.io.OutputStream;
import java.util.Collections;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.welshare.model.Message;
import com.welshare.model.Picture;
import com.welshare.service.BaseService;
import com.welshare.service.PictureService;
import com.welshare.util.WebUtils;

@Controller
public class PictureController {

    @Inject
    private PictureService pictureService;

    @Resource(name="baseService")
    private BaseService service;

    @RequestMapping(PictureService.UPLOADS_URI + "/profile/{filename}.{extension}")
    public void getProfilePicture(@PathVariable String filename,
            @PathVariable String extension,
            OutputStream outputStream,
            HttpServletResponse response) {

        // don't cache profile pictures

        String fullFilename = getFullFilename(filename, extension);
        response.setContentType("image/" + extension);
        pictureService.transferProfilePicture(fullFilename, outputStream);
    }

    @RequestMapping(PictureService.UPLOADS_URI + "{filename}.{extension}")
    public void getPicture(@PathVariable String filename,
            @PathVariable String extension, OutputStream outputStream,
            HttpServletResponse response) {

        DateTime cachePeriod = new DateTime();
        cachePeriod = cachePeriod.plusDays(5);
        response.setDateHeader("Expires", cachePeriod.getMillis());

        String fullFilename = getFullFilename(filename, extension);
        response.setContentType("image/" + extension);
        pictureService.transferPicture(fullFilename, outputStream);
    }

    @RequestMapping(PictureService.UPLOADS_TEMP_URI + "{filename}.{extension}")
    public void getTempPicture(@PathVariable String filename,
            @PathVariable String extension, OutputStream outputStream,
            HttpServletResponse response) {

        String fullFilename = getFullFilename(filename, extension);
        response.setContentType("image/" + extension);
        pictureService.transferTempPicture(fullFilename, outputStream);
    }

    @RequestMapping("/picture/{pictureKey}")
    public String getPictureByKey(@PathVariable String pictureKey, Model model) {
        Picture picture = service.get(Picture.class, "shortKey", pictureKey);
        Message message = pictureService.getMessage(picture);
        if (message != null) {
            WebUtils.prepareForOutput(message);
            model.addAttribute("messages", Collections.singletonList(message));
        }
        model.addAttribute("picture", picture);
        return "picture";
    }

    private String getFullFilename(String filename, String extension) {
        String fullFilename = filename + (StringUtils.isNotBlank(extension)
                ? FilenameUtils.EXTENSION_SEPARATOR + extension : "");
        return fullFilename;
    }
}
