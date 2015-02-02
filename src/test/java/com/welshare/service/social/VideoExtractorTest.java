package com.welshare.service.social;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.welshare.model.social.VideoData;
import com.welshare.service.social.video.VimeoVideoExtractor;
import com.welshare.service.social.video.YoutubeVideoExtractor;


public class VideoExtractorTest {

    @Test
    public void vimeoExtractTest() {
        VimeoVideoExtractor extractor = new VimeoVideoExtractor();
        VideoData data = extractor.getVideoData("http://vimeo.com/22428395");
        Assert.assertNotSame(VimeoVideoExtractor.DEFAULT_PICTURE, data.getPicture());
        Assert.assertEquals("Incorrectly extracted key", "22428395", data.getKey());
    }

    @Test
    public void youtubeExtractTest() {
        YoutubeVideoExtractor extractor = new YoutubeVideoExtractor();
        VideoData data = extractor.getVideoData("http://www.youtube.com/watch?v=abcdefg&feature=autoplay&list=AV4oVf-d_");
        Assert.assertEquals("Incorrectly extracted video key", "abcdefg", data.getKey());
        Assert.assertTrue(StringUtils.isNotBlank(data.getEmbedCode()));
        Assert.assertTrue(StringUtils.isNotBlank(data.getPicture()));
        Assert.assertTrue(StringUtils.isNotBlank(data.getPlayerUrl()));

        data = extractor.getVideoData("http://youtu.be/asdfasdf#t=7m50s");
        Assert.assertEquals("Incorrectly extracted video key before hash", "asdfasdf", data.getKey());
        Assert.assertTrue(StringUtils.isNotBlank(data.getEmbedCode()));
        Assert.assertTrue(StringUtils.isNotBlank(data.getPicture()));
        Assert.assertTrue(StringUtils.isNotBlank(data.getPlayerUrl()));
        Assert.assertTrue(data.getPlayerUrl().contains("#t=7m50s"));

    }
}
