package com.welshare.service.social.pictures;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstagramPictureProvider implements PictureProvider {

    private static final Logger logger = LoggerFactory.getLogger(InstagramPictureProvider.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getImageURL(String link) {
        if (link.contains("//instagr.am")) {
            if (link.endsWith("/")) {
                link = link.substring(0, link.length() - 1);
            }
            String key = link.substring(link.lastIndexOf('/') + 1);
            try {
                URL url = new URL("http://api.instagram.com/oembed?url=http://instagr.am/p/" + key + "/&maxwidth=160&maxheight=160");
                InstagramThumbnail thumb = mapper.readValue(url, InstagramThumbnail.class);

                return thumb.getUrl();
            } catch (FileNotFoundException ex) {
                // do nothing, the image has been removed
            } catch (IOException ex) {
                logger.warn("Problem getting instagram preview image", ex.getMessage());
            }
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class InstagramThumbnail {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
