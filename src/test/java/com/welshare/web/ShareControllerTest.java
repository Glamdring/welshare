package com.welshare.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.welshare.dao.MessageDao;
import com.welshare.model.Message;
import com.welshare.model.User;
import com.welshare.service.FollowingService;
import com.welshare.service.MessageService;
import com.welshare.service.NotificationEventService;
import com.welshare.service.ShareService;
import com.welshare.service.impl.ShareServiceImpl;
import com.welshare.service.model.UserDetails;
@Ignore
public class ShareControllerTest {

    private static final String ORIGIANL_MSG_TEXT = "Origianl msg text";
    private static final String ORIGIANL_MESSAGE_ID = "200";
    private static final String DEFAULT_MESSAGE_ID = "1";
    private ShareController shareController = new ShareController();
    private MessageController messageController = new MessageController();


    private User user;
    private User receivingUser;

    @Before
    public void init() {

        MessageService msgService = mock(MessageService.class);
        when(msgService.getIncomingMessages(Mockito.anyString(), Mockito.<Collection<Message>>any(), Mockito.anyBoolean())).thenReturn(Collections.<Message>emptyList());

        ShareService shareService = new ShareServiceImpl();

        ReflectionTestUtils.setField(shareController, "shareService",
                shareService);
        ReflectionTestUtils.setField(shareController, "messageService",
                msgService);

        ReflectionTestUtils.setField(messageController, "messageService",
                msgService);

        user = new User();
        user.setUsername("Username");
        user.setId(UUID.randomUUID().toString());

        receivingUser = new User();
        receivingUser.setUsername("Different user");
        receivingUser.setId(UUID.randomUUID().toString());

        UserDetails receivingUserDetails = new UserDetails(receivingUser);
        UserDetails userDetails = new UserDetails(user);

        FollowingService followingServiceMock = mock(FollowingService.class);
        when(followingServiceMock.getFollowersDetails(user.getId())).thenReturn(
                Collections.singletonList(receivingUserDetails));
        when(followingServiceMock.getFollowersDetails(receivingUser.getId())).thenReturn(
                Collections.singletonList(userDetails));

        ReflectionTestUtils.setField(shareService, "socialNetworkServices",
                Collections.emptyList());

        ReflectionTestUtils.setField(shareService, "notificationEventService",
                mock(NotificationEventService.class));

        MessageDao daoMock = mock(MessageDao.class);
        Message message = new Message();
        message.setId("100");
        message.setAuthor(receivingUser);

        message.setText(ORIGIANL_MSG_TEXT);
        when(daoMock.getById(Message.class, ORIGIANL_MESSAGE_ID)).thenReturn(
                message);

        when(daoMock.persist(Mockito.<Object> any())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation)
                            throws Throwable {
                        Object[] args = invocation.getArguments();
                        return args[0];
                    }
                });

        ReflectionTestUtils.setField(shareService, "dao", daoMock);
        ReflectionTestUtils.setField(shareService, "messageDao", daoMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shareTest() {
        // Using the receivingController as a sending controller - i.e. the same
        // user

        HttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        Model model = new ExtendedModelMap();
        String messageText = "Text";
        shareController.share(messageText, Collections.EMPTY_LIST,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, false, -1,
                receivingUser.getId(), model, session);

        List<Message> messages = (List<Message>) model.asMap().get("messages");

        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(messageText, messages.iterator().next().getText());

        Collection<Message> incomingMessages = messageController
                .getIncomingMessages(receivingUser, session, DEFAULT_MESSAGE_ID, false);

        // Own messages aren't considered "incoming"
        Assert.assertEquals(0, incomingMessages.size());

//        Collection<Message> allMessages = receivingController
//                .getCurrentMessages(receivingUser);
//
//        Assert.assertEquals(1, allMessages.size());
//        Assert.assertEquals(messageText, allMessages.iterator().next()
//                .getText());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void receiveTest() {

        HttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        Model model = new ExtendedModelMap();
        String messageText = "Text";
        shareController.share(messageText, Collections.EMPTY_LIST,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, false, -1, user.getId(), model, session);

        List<Message> messages = (List<Message>) model.asMap().get("messages");

        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(messageText, messages.iterator().next().getText());

        Collection<Message> incomingMessages = messageController
                .getIncomingMessages(receivingUser, session, DEFAULT_MESSAGE_ID, false);

        // Own messages aren't considered "incoming"
        Assert.assertEquals(1, incomingMessages.size());
        Assert.assertEquals(messageText, incomingMessages.iterator().next()
                .getText());

//        Collection<Message> allMessages = receivingController
//                .getMessages(receivingUser);
//
//        Assert.assertEquals(0, allMessages.size());

        Collection<Message> recentMessages = messageController.getIncomingMessages(receivingUser, session, DEFAULT_MESSAGE_ID, false);

        Assert.assertEquals(1, recentMessages.size());
        Assert.assertEquals(messageText, recentMessages.iterator().next()
                .getText());

        incomingMessages = messageController
                .getIncomingMessages(receivingUser, session, DEFAULT_MESSAGE_ID, false);

        // The queue should be empty after the messages are retrieved
        Assert.assertEquals(0, incomingMessages.size());
    }

    @Test
    public void likeTest() {

        HttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        shareController.reshare(ORIGIANL_MESSAGE_ID, "Comment", "", false, Collections.<String>emptyList(), new ExtendedModelMap(),
                user.getId(), session, new MockHttpServletResponse());

        Collection<Message> incomingMessages = messageController
                .getIncomingMessages(receivingUser, session, DEFAULT_MESSAGE_ID, false);

        Assert.assertEquals(1, incomingMessages.size());
        Message msg = incomingMessages.iterator().next();
        Assert.assertEquals(ORIGIANL_MSG_TEXT, msg.getOriginalMessage()
                .getText());

        Assert.assertEquals(1, msg.getOriginalMessage().getAuthor().getScore());
    }
}

