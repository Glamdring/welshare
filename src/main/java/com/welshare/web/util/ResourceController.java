package com.welshare.web.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.welshare.util.collection.CollectionUtils;

@Controller
@RequestMapping("/static")
public class ResourceController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);

    @Value("${static.resources.cache.period}")
    private long cachePeriod;

    /**
     * Method for serving a merged asset file (merging all javascripts or all css)
     * Note that the CSS contain relative paths to images, in the form
     * ../images/img.png. This means that in order for images to be located properly
     * /static/version/merge/type has to be the URL for the merged assets. Thus
     * ../ will evaluate /static/version.
     *
     * Also note that only images are handled via the spring mvc:resource mechanism
     * javascript and css files are handled through this controller.
     *
     * TODO Consider http://jawr.java.net/integration/spring.html
     *
     * @param resources
     * @param assetsVersion
     * @param type
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("/{assetsVersion}/merge/{type}")
    public void merge(@RequestParam(required=false) List<String> resources,
            @PathVariable String assetsVersion,
            @PathVariable String type, HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        if (CollectionUtils.isEmpty(resources)) {
            return;
        }

        long lastAccessed = request.getDateHeader("If-Modified-Since");
        // if within one week - send NOT_MODIFIED
        if (cachePeriod > 0 && new DateTime().minusWeeks(1).isBefore(new DateTime(lastAccessed))) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        String contentType = "text/" + type;

        response.setContentType(contentType);

        response.setDateHeader("Expires", DateTimeUtils.currentTimeMillis()
                + cachePeriod * DateTimeConstants.MILLIS_PER_SECOND);
        response.setHeader("Cache-Control", "max-age=" + cachePeriod);

        OutputStream out = response.getOutputStream();
        for (String resource : resources) {
            try {
                if (resource.startsWith("http")) {
                    InputStream is = new BufferedInputStream(new URL(resource).openStream());
                    IOUtils.copy(is, out);
                    IOUtils.closeQuietly(is);
                } else {
                    InputStream in = request.getServletContext().getResourceAsStream(
                            resource.replace("/static/" + assetsVersion, ""));
                    if (in != null) {
                        IOUtils.copy(in, out);
                    }
                }
            } catch (SocketException ex) {
                logger.info("Socket exception (likley a broken pipe) when getting resources: " + ex.getMessage());
            } catch (IOException ex) {
                if (ExceptionUtils.getRootCause(ex) instanceof SocketException) {
                    logger.info("Socket exception (likley a broken pipe) when getting resources: (" + resource + ") " + ex.getMessage());
                    return; //the pipe is broken, no need to continue
                } else {
                    logger.warn("Problem processing resources. Current resource is: " + resource, ex);
                }
            } catch (Exception ex) {
                logger.warn("Problem processing resources. Current resource is: " + resource, ex);
            }
        }
    }
}
