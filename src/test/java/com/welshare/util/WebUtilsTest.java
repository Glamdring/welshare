package com.welshare.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.welshare.model.Message;
import com.welshare.util.WebUtils.OutputDetails;

public class WebUtilsTest {

    @Test
    public void parseTagsTest() {
        String msg = "#tag0 message #tag1. foo.#tag2 foo#nontag0 #tag1part2 http://abv.bg/#nontag1";

        List<String> tags = WebUtils.extractTags(msg);

        Assert.assertTrue(tags.contains("tag0"));
        Assert.assertTrue(tags.contains("tag1"));
        Assert.assertTrue(tags.contains("tag2"));
        Assert.assertTrue(tags.contains("tag1part2"));

        Assert.assertFalse(tags.contains("nontag0"));
        Assert.assertFalse(tags.contains("nontag1"));

        OutputDetails details = WebUtils.prepareForOutput(msg);
        Assert.assertEquals(tags, details.getTags());
        Assert.assertTrue(details.getText().contains("tags/tag0"));
        Assert.assertTrue(details.getText().contains("tags/tag1"));
        Assert.assertTrue(details.getText().contains("tags/tag2"));
        Assert.assertTrue(details.getText().contains("tags/tag1part2"));

        Assert.assertFalse(details.getText().contains("tags/nontag0"));
        Assert.assertFalse(details.getText().contains("tags/nontag1"));
    }

    @Test
    public void parseUsernamesTest() {
        String msg = "@user0 message @user1. foo.@user2 foo@nonuser0 http://abv.bg/@nonuser1";

        List<String> usernames = WebUtils.extractMentionedUsernames(msg);

        Assert.assertTrue(usernames.contains("user0"));
        Assert.assertTrue(usernames.contains("user1"));
        Assert.assertTrue(usernames.contains("user2"));

        Assert.assertFalse(usernames.contains("nonuser0"));
        Assert.assertFalse(usernames.contains("nonuser1"));

        OutputDetails details = WebUtils.prepareForOutput(msg);
        Assert.assertEquals(usernames, details.getUsernames());
        Assert.assertTrue(details.getText().contains("href=\"/user0"));
        Assert.assertTrue(details.getText().contains("href=\"/user1"));
        Assert.assertTrue(details.getText().contains("href=\"/user2"));

        Assert.assertFalse(details.getText().contains("href=\"/nonuser0"));
        Assert.assertFalse(details.getText().contains("href=\"/nonuser1"));
    }

    @Test
    public void parseUrlsTest() {
        String msg = "http://asd.com. ,http://www.asd.com/~foo/#bar https://asd.com/@urlpart/";

        List<String> urls = WebUtils.extractUrls(msg);

        Assert.assertTrue(urls.contains("http://asd.com"));
        Assert.assertTrue(urls.contains("http://www.asd.com/~foo/#bar"));
        Assert.assertTrue(urls.contains("https://asd.com/@urlpart/"));

        OutputDetails details = WebUtils.prepareForOutput(msg);
        Assert.assertEquals(urls, details.getUrls());
    }

    @Test
    public void trimTrailingUrlTest() {
        List<TestSample> samples = new ArrayList<TestSample>();
        samples.add(new TestSample("adsfa1 dfsa fdas http://a", "adsfa1 dfsa fdas"));
        samples.add(new TestSample("adsfa dfsa fdas https://aasd.com", "adsfa dfsa fdas"));
        samples.add(new TestSample("adsfa dfsafdas http://", "adsfa dfsafdas"));
        samples.add(new TestSample("http://abv.bg", ""));
        samples.add(new TestSample("http://abv.bg foo bar", "http://abv.bg foo bar"));
        samples.add(new TestSample("http://abv.bg foo bar https://a", "http://abv.bg foo bar"));

        for (TestSample sample : samples) {
            Assert.assertEquals(sample.getExpected(), WebUtils.trimTrailingUrl(sample.getOriginal()));
        }
    }

    @Test
    public void multipleNewLinesTest() {
        String msg = "foo\r\n\r\n\r\nbar\n\n\n\n\nbaz";
        OutputDetails details = WebUtils.prepareForOutput(msg);
        Assert.assertEquals("foo<br />bar<br />baz", details.getText());
    }

    @Test
    public void likeFormatTest() {
        Message msg = new Message();
        msg.setShortText("foo bar");
        String format = "$c: $m (via $a)";

        String result = WebUtils.formatLike(msg.getShortText(), "comment", "author", format);
        Assert.assertEquals("comment: foo bar (via author)", result);

        result = WebUtils.formatLike(msg.getShortText(), "", "author2", format);
        Assert.assertEquals("foo bar (via author2)", result);

        format = "$c- RT @$a: $m";
        result = WebUtils.formatLike(msg.getShortText(), "", "author3", format);
        Assert.assertEquals("RT @author3: foo bar", result);
    }

    @Test
    public void trimUsernamesTest() {
        String text = "RT @foo asdf";
        String result = WebUtils.trimUsernames(text);
        Assert.assertFalse(result.contains("RT"));
        Assert.assertFalse(result.contains("@foo"));
    }

    private static class TestSample {
        private String original;
        private String expected;
        public TestSample(String original, String expected) {
            super();
            this.original = original;
            this.expected = expected;
        }
        public String getOriginal() {
            return original;
        }
        public String getExpected() {
            return expected;
        }
    }
}
