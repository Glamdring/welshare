package com.welshare.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.collect.Maps;
import com.welshare.model.Message;
import com.welshare.model.Picture;
import com.welshare.model.social.VideoData;
import com.welshare.service.UrlShorteningService;
import com.welshare.service.social.pictures.PictureProvider;
import com.welshare.service.social.video.VideoExtractor;
import com.welshare.web.util.WebConstants;

public final class WebUtils {

    // video extractors and picture providers are registered in the static list on startup
    public static final List<VideoExtractor> videoExtractors = new ArrayList<VideoExtractor>();
    public static final List<PictureProvider> pictureProviders = new ArrayList<PictureProvider>();
    public static final List<UrlShorteningService> urlShorteners = new ArrayList<UrlShorteningService>();
    public static final WebUtils INSTANCE = new WebUtils();


    private static final String BR = "<br />";

    private static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

    private static final String RETWEET_REGEX = "\\bRT[: ]";
    private static final String USERNAME_REGEX = "(?:^|\\s|[\\p{Punct}&&[^/]])(@[\\p{L}0-9_\\.]*[\\p{L}0-9_]{1})";
    private static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEX);
    private static final String URL_REGEX = "http(s)?://([\\w+?\\.\\w+])+([\\p{L}0-9\\p{Punct}]*)?[\\p{L}0-9/]";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX, Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE);;
    private static final Pattern TAG_PATTERN = Pattern.compile("(?:^|\\s|[\\p{Punct}&&[^/]])(#[\\p{L}0-9-_]+)");
    private static final Pattern URL_START_PATTERN = Pattern.compile("http(s)?://");

    private static final Map<Locale, Integer> firstDaysOfWeek = Maps.newHashMap();

    private WebUtils() {
    }
    /**
     * Append "<a href", and escape javascript
     * @param text
     * @return the result text
     */
    public static OutputDetails prepareForOutput(String text) {

        OutputDetails details = new OutputDetails();

        if (StringUtils.isEmpty(text)) {
            return details;
        }

        String unescapedText = text;
        text = StringEscapeUtils.escapeHtml4(text);

        Matcher matcher = URL_PATTERN.matcher(unescapedText);
        while (matcher.find()) {
            String link = matcher.group();
            if (details.getUrls().contains(link)) {
                continue;
            }
            details.getUrls().add(link);
            String shortLink = link;
            link = StringEscapeUtils.escapeHtml4(link);
            for (UrlShorteningService shortener : urlShorteners) {
                if (shortener.isShortened(link)) {
                    shortLink = shortener.expand(link);
                }
            }
            shortLink = StringUtils.left(shortLink, 50);
            if (shortLink.length() < link.length()) {
                shortLink += "...";
            }

            text = text.replace(link, "<a href=\"" + link + "\" target=\"_blank\">" + shortLink + "</a>");
        }

        matcher = USERNAME_PATTERN.matcher(unescapedText);
        while (matcher.find()) {
            String username = matcher.group(1);
            if (details.getUsernames().contains(username.substring(1))) {
                continue;
            }
            details.getUsernames().add(username.substring(1));
            text = text.replaceFirst(username, "<a href=\"/" + username.substring(1)
                    + "\">" + username + "</a>");
        }

        matcher = TAG_PATTERN.matcher(unescapedText);
        while (matcher.find()) {
            String tag = matcher.group(1);
            if (details.getTags().contains(tag.substring(1))) {
                continue;
            }
            details.getTags().add(tag.substring(1));
            text = text.replaceFirst(tag, "<a href=\"/tags/" + tag.substring(1)
                    + "\">" + tag + "</a>");
        }

        text = text.replaceAll("(\r\n)+", BR).replaceAll("\n+", BR);
        details.setText(text);
        return details;
    }

    public static List<String> extractUrls(String text) {
        if (StringUtils.isEmpty(text)) {
            return Collections.emptyList();
        }
        Matcher matcher = URL_PATTERN.matcher(text);
        List<String> list = new ArrayList<String>();
        while (matcher.find()) {
            list.add(matcher.group());
        }

        return list;
    }

    public static List<String> extractMentionedUsernames(String text) {
        List<String> usernames = new ArrayList<String>();
        Matcher m = USERNAME_PATTERN.matcher(text);
        while (m.find()) {
            usernames.add(m.group(1).replace("@", "").trim());
        }

        return usernames;
    }

    public static List<String> extractTags(String text) {
        List<String> tags = new ArrayList<String>();
        Matcher m = TAG_PATTERN.matcher(text);
        while (m.find()) {
            tags.add(m.group(1).replace("#", "").trim());
        }

        return tags;
    }

    public static void prepareForOutput(Collection<Message> messages) {
        for (Message msg : messages) {
            prepareForOutput(msg);
        }
    }

    public static void prepareForOutput(Message msg) {
        try {
            if (msg.getData().getFormattedText() == null) {
                OutputDetails details = WebUtils.prepareForOutput(msg.getText());
                msg.getData().setFormattedText(details.getText());
                for (VideoExtractor extractor : videoExtractors) {
                    for (String url : details.getUrls()) {
                        VideoData vd = extractor.getVideoData(url);
                        if (vd != null) {
                            msg.getData().setVideoData(vd);
                            break;
                        }
                    }
                }
                // do this only for internal messages. External ones are handled in their separate services
                if (msg.getId() != null) {
                    for (PictureProvider provider : pictureProviders) {
                        for (String url : details.getUrls()) {
                            String thumb = provider.getImageURL(url);
                            if (thumb != null) {
                                Picture pic = new Picture();
                                pic.setExternal(true);
                                pic.setPath(thumb);
                                pic.setExternalUrl(url);
                                // this check is needed because the collection may not be initialized
                                // if it's not (pictureCount=0), re-set it to a new collection
                                if (msg.getPictureCount() > 0) {
                                    msg.getPictures().add(pic);
                                } else {
                                    msg.setPictures(new ArrayList<Picture>());
                                    msg.getPictures().add(pic);
                                }
                                msg.setPictureCount(msg.getPictureCount() + 1);
                            }
                        }
                    }
                }

                if (msg.getOriginalMessage() != null && StringUtils.isEmpty(msg.getOriginalMessage().getData().getFormattedText())) {
                    msg.getOriginalMessage().getData().setFormattedText(WebUtils.prepareForOutput(msg.getOriginalMessage().getText()).getText());
                }

                if (msg.getData().getMentionedUsernames().isEmpty()) {
                    msg.getData().setMentionedUsernames(details.getUsernames());
                }
            }
        } catch (Exception ex) {
            logger.warn("Problem preparing message for output: " + msg.getText(), ex);
        }
    }

    public static void addMessage(HttpServletRequest request, String key, Object... args) {
        List<InfoMessage> msgs = getScreenMessages(request);

        InfoMessage msg = new InfoMessage();
        msg.setKey(key);
        msg.setArgs(args);
        msgs.add(msg);
        request.setAttribute(WebConstants.SCREEN_MESSAGES_KEY, msgs);
    }

    public static void addError(HttpServletRequest request, String key, Object... args) {
        List<InfoMessage> msgs = getScreenMessages(request);

        InfoMessage msg = new InfoMessage();
        msg.setKey(key);
        msg.setArgs(args);
        msg.setError(true);
        msgs.add(msg);
        request.setAttribute(WebConstants.SCREEN_MESSAGES_KEY, msgs);
    }

    @SuppressWarnings("unchecked")
    private static List<InfoMessage> getScreenMessages(
            HttpServletRequest request) {
        List<InfoMessage> msgs = (List<InfoMessage>) request.getAttribute(WebConstants.SCREEN_MESSAGES_KEY);
        if (msgs == null) {
            msgs = new LinkedList<InfoMessage>();
        }
        return msgs;
    }

    /**
     * Adds the desired suffix right before the extension of the file.
     * For example, for path=foo/bar.jpg, and suffix=_small, the result
     * would be foo/bar_small.jpg
     *
     * @param path
     * @param suffix
     * @return the new filename with the suffix.
     */
    public static String addSuffix(String path, String suffix) {
        String extension = FilenameUtils.getExtension(path).toLowerCase();
        String name = FilenameUtils.removeExtension(path);
        return name + suffix + FilenameUtils.EXTENSION_SEPARATOR_STR + extension;
    }

    public static Map<String, List<String>> getQueryParams(String url) {
        try {
            Map<String, List<String>> params = new HashMap<String, List<String>>();
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String query = urlParts[1];
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = "";
                    if (pair.length > 1) {
                        value = URLDecoder.decode(pair[1], "UTF-8");
                    }

                    List<String> values = params.get(key);
                    if (values == null) {
                        values = new ArrayList<String>();
                        params.put(key, values);
                    }
                    values.add(value);
                }
            }

            return params;
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
    }

    public static String trimTrailingUrl(String message) {
        int lastSpace = 0;
        for (int i = message.length() - 1; i >= 0; i--) {
            if (Character.isWhitespace(message.charAt(i))) {
                lastSpace = i;
                break;
            }
        }
        if (URL_START_PATTERN.matcher(message.substring(lastSpace)).find()) {
            message = message.substring(0, lastSpace);
        }

        return message;
    }

    public static String formatDateTime(DateTime dateTime, String format, String timeZone) {
        return DateTimeFormat.forPattern(format).withZone(DateTimeZone.forID(timeZone)).print(dateTime);
    }

    public static class InfoMessage {
        private String key;
        private Object[] args;
        private boolean error;

        public String getKey() {
            return key;
        }
        public void setKey(String key) {
            this.key = key;
        }
        public Object[] getArgs() {
            return args;
        }
        public void setArgs(Object[] args) {
            this.args = args;
        }
        public boolean isError() {
            return error;
        }
        public void setError(boolean error) {
            this.error = error;
        }
    }

    public static class OutputDetails {
        private String text = "";
        private List<String> usernames = new ArrayList<String>();
        private List<String> tags = new ArrayList<String>();
        private List<String> urls = new ArrayList<String>();

        public String getText() {
            return text;
        }
        public void setText(String message) {
            this.text = message;
        }
        public List<String> getUsernames() {
            return usernames;
        }
        public void setUsernames(List<String> usernames) {
            this.usernames = usernames;
        }
        public List<String> getTags() {
            return tags;
        }
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        public List<String> getUrls() {
            return urls;
        }
        public void setUrls(List<String> urls) {
            this.urls = urls;
        }
    }

    public static String formatLike(String messageText, String comment, String author, String format) {
        String result = format;
        if (comment.isEmpty()) {
            int commentStart = format.indexOf("$c");
            String commentPart = result.substring(commentStart, format.indexOf(' ', commentStart));
            result = result.replace(commentPart, "");
        } else {
            result = result.replace("$c", comment);
        }

        result = result.replace("$a", author);
        result = result.replace("$m", messageText);

        return result.trim();
    }

    public static String redirectAfterAuthentication(String redirectBaseUrl, HttpSession session) {
       String featureUri = (String) session.getAttribute(WebConstants.FEATURE_URI);
       if (featureUri != null) {
           session.removeAttribute(WebConstants.FEATURE_URI);
           return redirectBaseUrl + featureUri;
       } else {
           return redirectBaseUrl;
       }
    }

    public static String trimUsernames(String shortText) {
        return shortText.replaceAll(USERNAME_REGEX, "").replaceAll(RETWEET_REGEX, "").trim();
    }

    public static String escape(String text) {
        text = StringEscapeUtils.escapeHtml4(text);
        text = StringEscapeUtils.escapeEcmaScript(text);
        return text;
    }

    //no need of synchronization, a couple of extra instance won't hurt
    public static int getFirstDayOfWeek() {
        Locale locale = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getLocale();
        Integer result = firstDaysOfWeek.get(locale);
        if (result == null) {
            result = Calendar.getInstance(locale).getFirstDayOfWeek() - 1;
            firstDaysOfWeek.put(locale, result);
        }
        return result;
    }
}