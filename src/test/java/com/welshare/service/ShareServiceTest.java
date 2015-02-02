package com.welshare.service;

import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import com.welshare.model.Message;
import com.welshare.model.User;
import com.welshare.model.enums.Language;
import com.welshare.service.ShareService.ResharingDetails;
import com.welshare.service.exception.UserException;
import com.welshare.test.BaseSpringTest;
import com.welshare.util.Constants;

public class ShareServiceTest extends BaseSpringTest {

    @Inject
    private ShareService shareService;

    @Inject
    private MessageService messageService;

    @Inject
    private UserService userService;

    private User user;
    private User user2;

    @PostConstruct
    public void init() throws UserException {
        user = new User();
        user.setUsername("fooo");
        user.setPassword("barr");
        user.setNames("foo bar");
        user.setEmail("foo@bar.com");
        user.getProfile().setLanguage(Language.EN);
        user = userService.register(user);

        user2 = new User();
        user2.setUsername("fooo2");
        user2.setPassword("barr2");
        user2.setNames("foo2 bar2");
        user2.setEmail("foo2@bar2.com");
        user2.getProfile().setLanguage(Language.EN);
        user2 = userService.register(user2);
    }

    @After
    public void destroy() {
        userService.delete(user);
        userService.delete(user2);
    }

    @Test
    public void sharingTest() throws UserException {
        // first share
        String text = "test";

        Message msg = shareService.share(text, user.getId(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                false);

        Message dbMsg = messageService.get(Message.class, msg.getId());

        Assert.assertEquals(msg, dbMsg);

        Assert.assertFalse(dbMsg.isExternalReply());
        Assert.assertFalse(dbMsg.isLiking());
        Assert.assertFalse(dbMsg.isDeleted());
        Assert.assertFalse(dbMsg.isReply());

        Assert.assertEquals(text, dbMsg.getText());
        Assert.assertEquals(user, dbMsg.getAuthor());

        // then reply
        String replyText = "@" + user.getUsername() + " reply text";
        Message replyMsg = shareService.reply(replyText, dbMsg.getId(), user.getId());
        Message dbReplyMsg = messageService.get(Message.class, replyMsg.getId());

        Assert.assertEquals(replyMsg, dbReplyMsg);

        Assert.assertTrue(dbReplyMsg.isReply());
        Assert.assertEquals(dbMsg, dbReplyMsg.getOriginalMessage());
        Assert.assertEquals(replyText, dbReplyMsg.getText());

        // then like
        ResharingDetails details = new ResharingDetails();
        details.setComment("");
        details.setExternalSites(Collections.<String>emptyList());
        details.setReshareInternally(true);
        LikeResult likeResult = shareService.reshare(dbMsg.getId(), details, user2.getId());
        Assert.assertEquals(1, likeResult.getNewLikes());
        Assert.assertTrue(likeResult.getMessage().isLiking());
        Assert.assertEquals(dbMsg, likeResult.getMessage().getOriginalMessage());
        Assert.assertEquals(Constants.LIKE_SCORE, userService.get(User.class, user.getId()).getScore());

        messageService.delete(dbReplyMsg.getId(), true, user);
        shareService.unlike(dbMsg.getId(), user2.getId());
        messageService.delete(dbMsg.getId(), true, user);
    }
}
