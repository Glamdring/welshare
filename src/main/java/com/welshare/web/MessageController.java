package com.welshare.web;

import static com.welshare.web.util.WebConstants.MESSAGES_KEY;
import static com.welshare.web.util.WebConstants.MESSAGES_RESULT_VIEW;
import static com.welshare.web.util.WebConstants.REPLIES_KEY;
import static com.welshare.web.util.WebConstants.REPLIES_RESULT_VIEW;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.text.WordUtils;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.feed.AbstractRssFeedView;

import com.google.common.collect.Maps;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Description;
import com.sun.syndication.feed.rss.Item;
import com.welshare.dao.enums.MessageSearchType;
import com.welshare.model.Message;
import com.welshare.model.ScheduledMessage;
import com.welshare.model.User;
import com.welshare.model.enums.AccountType;
import com.welshare.service.FollowingService;
import com.welshare.service.MessageService;
import com.welshare.service.UserService;
import com.welshare.service.impl.GoogleTranslateService;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.qualifiers.LinkedIn;
import com.welshare.service.social.qualifiers.Twitter;
import com.welshare.util.WebUtils;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;
import com.welshare.web.util.StartupListener.SocialNetwork;
import com.welshare.web.util.WebConstants;

@Controller
public class MessageController {

    private static final String WINDOW_ID = "windowId";

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private static final String HOME_PAGE = "home";

    private static final String SELECTED_USER_KEY = "selectedUser";

    @Inject
    private MessageService messageService;

    @Inject
    private UserService userService;

    @Inject
    private FollowingService followingService;

    @Inject
    private List<SocialNetworkService> socialNetworkServices;

    @Inject
    private ServletContext servletContext;

    @Inject
    @Twitter
    private SocialNetworkService twitterService;

    @Inject
    @LinkedIn
    private SocialNetworkService linkedInService;

    @Inject
    private GoogleTranslateService translateService;

    @Value("${common.page.size}")
    private int pageSize;

    @Value("${base.url}")
    private String baseUrl;

    private final Random random = new Random();

    @RequestMapping({ "/", "/home" })
    public String home(Model model, @SessionAttribute String userId, HttpSession session) {
        if (userId != null) {
            return home(userId, false, model, session);
        } else {
            return WebConstants.UNREGISTERED_HOME;
        }
    }

    @RequestMapping("/important")
    public String important(Model model, @SessionAttribute String userId, HttpSession session) {
        if (userId != null) {
            return home(userId, true, model, session);
        } else {
            return WebConstants.UNREGISTERED_HOME;
        }
    }

    @RequestMapping("/messages/filtered")
    public String getFilteredMessages(@RequestParam(required = false) String filterNetwork,
            @SessionAttribute String userId, Model model, HttpSession session) {
        Collection<Message> result = null;
        if (filterNetwork != null) {
            result = messageService.getMessages(userId, Collections.<Message> emptyList(), false,
                    filterNetwork).getMessages();
            WebUtils.prepareForOutput(result);
            model.addAttribute("messages", result);
            return MESSAGES_RESULT_VIEW;
        } else {
            return null;
        }
    }

    @RequestMapping("/{username}")
    public String userPage(@PathVariable String username, Model model, @RequestAttribute User loggedUser,
            HttpServletResponse response, @RequestParam(required = false, defaultValue = "0") int page)
            throws IOException {

        int start = page * pageSize;
        Collection<Message> messages = messageService.getUserMessages(username, loggedUser, start);
        WebUtils.prepareForOutput(messages);
        model.addAttribute(MESSAGES_KEY, messages);

        UserDetails details = userService.getUserDetails(username);

        if (details != null) {
            details.setBio(WebUtils.prepareForOutput(details.getBio()).getText());
        }

        // fill in model metadata about the relation between the currently
        // logged user and the selected user
        if (loggedUser != null) {
            List<UserDetails> tempList = Collections.singletonList(details);
            followingService.updateCurrentUserFollowings(loggedUser.getId(), tempList, true);
            if (details == null) {
                String id = null;
                // redirecting to external users only if there's a logged-in
                // user, because otherwise no valid token(s) are available. Of
                // course we assume the user is connected to the particular
                // network, as he has clicked a username from the stream, rather
                // than typed it
                for (SocialNetworkService sns : socialNetworkServices) {
                    id = sns.getUserId(username, loggedUser);
                    if (id != null) {
                        return "redirect:user/external/" + id;
                    }
                }
            }
        }

        if (details != null) {
            model.addAttribute("profileUrls", getExternalNetworkProfiles(details));
            model.addAttribute(SELECTED_USER_KEY, details);
        }

        model.addAttribute("metadataIncluded", Boolean.TRUE);
        model.addAttribute("selectedUsername", username);

        return "userPage";
    }

    @RequestMapping("/{username}/socialReputation")
    public String getSocialReputation(@PathVariable String username, Model model) {
        User selectedUser = userService.getByUsername(username);
        if (selectedUser == null) {
            return "userPage"; // user page for non-existent user
        }

        model.addAttribute(SELECTED_USER_KEY, selectedUser);
        model.addAttribute("reputationScores", userService.getReputationScores(selectedUser.getId()));
        return "socialReputation";
    }

    private Map<String, String> getExternalNetworkProfiles(UserDetails details) {
        @SuppressWarnings("unchecked")
        Map<String, SocialNetwork> networks = (Map<String, SocialNetwork>) servletContext
                .getAttribute("socialNetworks");

        Map<String, String> profileUrls = Maps.newHashMap();
        User user = userService.get(User.class, details.getId());
        for (SocialNetwork network : networks.values()) {
            if (network.getPrefix().equals("fb") && user.getFacebookSettings().isFetchMessages()
                    && user.getFacebookSettings().isShowInProfile()) {
                profileUrls.put(network.getSiteName(), "http://facebook.com/profile.php?id="
                        + user.getFacebookSettings().getUserId());
            }
            if (network.getPrefix().equals("tw") &&user.getTwitterSettings().isFetchMessages()
                    && user.getTwitterSettings().isShowInProfile()) {
                profileUrls.put(network.getSiteName(),
                        "http://twitter.com/" + twitterService.getExternalUsername(user));
            }
            if (network.getPrefix().equals("gp") && user.getGooglePlusSettings().isFetchMessages()
                    && user.getGooglePlusSettings().isShowInProfile()) {
                profileUrls.put(network.getSiteName(), "https://plus.google.com/"
                        + user.getGooglePlusSettings().getUserId());
            }
            if (network.getPrefix().equals("li") && user.getLinkedInSettings().isFetchMessages()
                    && user.getLinkedInSettings().isShowInProfile()) {
                UserDetails linkedInDetails = linkedInService.getUserDetails(user.getLinkedInSettings()
                        .getUserId(), user);
                if (linkedInDetails != null) {
                    profileUrls.put(network.getSiteName(), linkedInDetails.getExternalUrl());
                }
            }
        }

        return profileUrls;
    }

    @RequestMapping("/message/{id}")
    public String getIndividualMessage(@PathVariable String id, @RequestAttribute User loggedUser, Model model) {
        Message msg = messageService.getMessage(id, loggedUser);
        if (msg != null) {
            WebUtils.prepareForOutput(msg);
        } else {
            logger.debug("No message found with ID " + id);
            return WebConstants.REDIRECT_HOME;
        }
        getReplies(id, loggedUser, model);

        model.addAttribute("message", msg);
        model.addAttribute(SELECTED_USER_KEY, msg.getAuthor());

        return "message";
    }

    @RequestMapping("/message/external/{id}")
    public String getIndividualExternalMessage(@PathVariable String id, @RequestAttribute User loggedUser,
            Model model) {

        Message msg = null;
        for (SocialNetworkService sns : socialNetworkServices) {
            if (sns.shouldHandle(id)) {
                msg = sns.getMessageByExternalId(id, loggedUser);
                break;
            }
        }
        if (msg != null) {
            WebUtils.prepareForOutput(msg);
        } else {
            logger.debug("No message found for ID " + id);
            return WebConstants.REDIRECT_HOME;
        }
        getReplies(id, loggedUser, model);

        model.addAttribute("message", msg);

        return "message";
    }

    @RequestMapping("/message/favourite/{id}")
    @ResponseBody
    public void favourite(@PathVariable String id, @RequestAttribute User loggedUser) {
        messageService.favourite(id, loggedUser);
    }

    @RequestMapping("/message/unfavourite/{id}")
    @ResponseBody
    public void unfavourite(@PathVariable String id, @RequestAttribute User loggedUser) {
        messageService.unfavourite(id, loggedUser);
    }

    private String home(String userId, boolean important, Model model, HttpSession session) {
        MessageService.MessagesResult result = messageService.getMessages(userId,
                Collections.<Message> emptyList(), important);

        Collection<Message> currentMessages = result.getMessages();
        WebUtils.prepareForOutput(currentMessages);

        // windowId is used to differentiate multiple tabs within the same
        // session
        // new windowIds are generated on each refresh as well, thus filling the
        // session
        // with unused collections, but since they point to the same Message
        // objects,
        // it is not too much of memory overhead
        String windowId = String.valueOf(random.nextInt(10000));
        addMessagesToSession(currentMessages, session, WebConstants.OLDEST_MESSAGES_RETRIEVED, windowId);
        addMessagesToSession(result.getNewestMessages(), session, WebConstants.NEWEST_MESSAGES_RETRIEVED,
                windowId);

        model.addAttribute(MESSAGES_KEY, currentMessages);
        model.addAttribute(WINDOW_ID, windowId);
        model.addAttribute("important", important);
        if (!important) {
            List<Message> oldMessages = messageService.getOldMessages(userId);
            WebUtils.prepareForOutput(oldMessages);
            model.addAttribute("oldMessages", oldMessages);
        }
        return HOME_PAGE;
    }

    @RequestMapping("/home/page/{page}")
    public String pagedHome(@RequestAttribute User loggedUser, Model model, @PathVariable int page) {
        Collection<Message> messages = messageService.getPagedMessages(loggedUser, page);
        WebUtils.prepareForOutput(messages);
        model.addAttribute(MESSAGES_KEY, messages);
        model.addAttribute("paged", true);

        return HOME_PAGE;
    }

    @RequestMapping("/messages/missed")
    public String getMissedImportantMessages(HttpSession session) {
        // remove the unread count = mark as read
        session.removeAttribute(WebConstants.MISSED_IMPORTANT_MESSAGES_UNREAD_COUNT);
        return "missedImportantMessages";
    }

    @RequestMapping("/messages/missedCount")
    @ResponseBody
    public int getMissedImportantMessagesCount(HttpSession session) {
        Integer count = (Integer) session.getAttribute(WebConstants.MISSED_IMPORTANT_MESSAGES_UNREAD_COUNT);
        return count != null ? count.intValue() : 0;
    }

    @RequestMapping("{username}/messages/topRecent")
    public String getTopRecentMessages(Model model, @PathVariable String username,
            HttpServletResponse response) {
        User user = userService.getByUsername(username);
        if (user != null) {
            List<Message> messages = messageService.getTopRecentMessages(user);
            WebUtils.prepareForOutput(messages);
            model.addAttribute(MESSAGES_KEY, messages);
        }
        model.addAttribute("username", username);

        response.setDateHeader("Expires", DateTimeUtils.currentTimeMillis()
                + DateTimeConstants.MILLIS_PER_DAY);
        response.setHeader("Cache-Control", "max-age=" + DateTimeConstants.SECONDS_PER_DAY);

        return "topRecentMessages";
    }

    /**
     * Used for fetching the incoming messages by the UI (to show at the top of
     * the stream)
     *
     * @param model
     * @param loggedUser
     * @param windowId
     * @param important
     * @param session
     * @param response
     * @return
     */
    @RequestMapping("/messages/recent")
    public String getRecentMessages(Model model, @RequestAttribute User loggedUser,
            @RequestParam(required = false, defaultValue = "") String windowId,
            @RequestParam(required = false) String filterNetwork,
            @RequestParam(required = false) boolean important, HttpSession session,
            HttpServletResponse response) {

        if (loggedUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return MESSAGES_RESULT_VIEW;
        }

        Collection<Message> incomingMessages = getIncomingMessages(loggedUser, session, windowId, important);

        List<Message> newMessages = new ArrayList<Message>(incomingMessages.size());
        for (Message incoming : incomingMessages) {
            // not returning updates in comments
            // and own messages (they appear automatically)
            if (!incoming.isReply() && !incoming.getAuthor().equals(loggedUser)) {
                // filter messages by network here (rather than in the service
                // layer)
                // no need for multiple caches and fetching messages multiple
                // times
                // (this differs from regular message fetching, because all new
                // messages are returned, rather than limiting them to 20)
                if (isFromDesiredNetwork(filterNetwork, incoming)) {
                    newMessages.add(incoming);
                }
            }
        }
        WebUtils.prepareForOutput(newMessages);

        model.addAttribute(MESSAGES_KEY, newMessages);

        if (!newMessages.isEmpty()) {
            addMessagesToSession(newMessages, session, WebConstants.NEWEST_MESSAGES_RETRIEVED, windowId);
        }

        return MESSAGES_RESULT_VIEW;
    }

    private boolean isFromDesiredNetwork(String filterNetwork, Message incoming) {
        return filterNetwork == null || incoming.getPublicId().startsWith(filterNetwork)
                || (filterNetwork.equals("ws") && incoming.getId() != null);
    }

    @RequestMapping("/messages/more")
    public String getMoreMessages(Model model, @SessionAttribute String userId, HttpSession session,
            @RequestParam(required = false, defaultValue = "") String windowId,
            @RequestParam(required = false) String filterNetwork,
            @RequestParam(required = false) boolean important) {

        if (userId == null) {
            return MESSAGES_RESULT_VIEW;
        }
        Collection<Message> lastRetrieved = getMessagesFromSession(session,
                WebConstants.OLDEST_MESSAGES_RETRIEVED, windowId, userId, important);

        Collection<Message> messages = null;
        if (filterNetwork == null) {
            messages = messageService.getMessages(userId, lastRetrieved, important).getMessages();
        } else {
            messages = messageService.getMessages(userId, lastRetrieved, important, filterNetwork)
                    .getMessages();
        }
        WebUtils.prepareForOutput(messages);
        lastRetrieved.addAll(messages);
        addMessagesToSession(lastRetrieved, session, WebConstants.OLDEST_MESSAGES_RETRIEVED, windowId);
        model.addAttribute(MESSAGES_KEY, messages);

        return MESSAGES_RESULT_VIEW;
    }

    @RequestMapping("/messages/more/user")
    public String getMoreUserMessages(@RequestAttribute User loggedUser, @RequestParam String userId,
            @RequestParam int from, Model model) {
        User targetUser = userService.get(User.class, userId);
        if (targetUser != null) {
            Collection<Message> messages = messageService.getUserMessages(targetUser, loggedUser, from);
            WebUtils.prepareForOutput(messages);
            model.addAttribute(MESSAGES_KEY, messages);
        }

        return MESSAGES_RESULT_VIEW;
    }

    @RequestMapping("/replies/{originalMessageId}")
    public String getReplies(@PathVariable String originalMessageId, @RequestAttribute User loggedUser,
            Model model) {
        Collection<Message> replies = messageService.getReplies(originalMessageId, loggedUser);

        WebUtils.prepareForOutput(replies);

        model.addAttribute(REPLIES_KEY, replies);

        return REPLIES_RESULT_VIEW;
    }

    @RequestMapping("/message/likers")
    public String getLikers(@RequestParam String messageId, @RequestAttribute User loggedUser, Model model) {
        List<User> likers = messageService.getLikers(messageId, loggedUser);
        model.addAttribute("likers", likers);

        return "results/likers";
    }

    Collection<Message> getIncomingMessages(User user, HttpSession session, String windowId, boolean important) {
        if (user == null) {
            return Collections.emptyList();
        }

        return messageService.getIncomingMessages(
                user.getId(),
                getMessagesFromSession(session, WebConstants.NEWEST_MESSAGES_RETRIEVED, windowId,
                        user.getId(), important), important);
    }

    @RequestMapping("/search/{type}/{keywords}/{page}")
    public String search(@PathVariable String keywords, @PathVariable MessageSearchType type,
            @RequestAttribute User loggedUser, @PathVariable int page, Model model) {

        Collection<Message> result = messageService.search(keywords, type, loggedUser, page);
        WebUtils.prepareForOutput(result);
        model.addAttribute(MESSAGES_KEY, result);
        model.addAttribute("type", type.name());
        model.addAttribute("keywords", keywords);
        return "searchResults";
    }

    @RequestMapping("/search/{type}/{keywords}")
    public String searchAll(@PathVariable String keywords, @PathVariable MessageSearchType type,
            @RequestAttribute User loggedUser, Model model) {
        return search(keywords, type, loggedUser, 0, model);
    }

    @RequestMapping("/messages/favourites")
    public String getFavourites(@RequestAttribute User loggedUser, Model model) {
        List<Message> favourites = messageService.getFavourites(loggedUser);
        WebUtils.prepareForOutput(favourites);
        model.addAttribute(MESSAGES_KEY, favourites);
        return "favourites";
    }

    @RequestMapping("/scheduledMessages/list")
    public String getScheduledMessages(@SessionAttribute String userId, Model model) {
        List<ScheduledMessage> scheduledMessage = messageService.getScheduledMessages(userId);
        model.addAttribute(MESSAGES_KEY, scheduledMessage);
        return "scheduledMessages";
    }

    @RequestMapping("/scheduledMessages/delete")
    @ResponseBody
    public void deleteScheduledMessage(@RequestParam long id) {
        messageService.deleteScheduledMessage(id);
    }

    @RequestMapping("/externalOriginalMessage/{id}")
    public String getExternalOriginalMessage(@PathVariable String id) {
        String url = messageService.getOriginalExternalUrl(id);
        if (url != null) {
            return "redirect:" + url;
        } else {
            return WebConstants.REDIRECT_HOME;
        }
    }

    @RequestMapping("/messages/refresh")
    public String refreshMessageStream(@SessionAttribute String userId) {
        messageService.invalidateStreamCache(userId);
        return WebConstants.REDIRECT_HOME;
    }

    @RequestMapping("/message/translate")
    @ResponseBody
    public String translate(@RequestParam String text, @RequestParam String language,
            @SessionAttribute String userId) {
        if (userId == null) {
            return "not available"; // only logged-in users can translate,
                                    // otherwise the service can be abused
        }
        return translateService.translate(text.trim(), language);
    }

    @RequestMapping("{username}/rss")
    public View userRss(@PathVariable String username, Model model) {
        // TODO cache
        Collection<Message> messages = messageService.getUserMessages(username, null, 0);
        WebUtils.prepareForOutput(messages);
        model.addAttribute(MESSAGES_KEY, messages);
        MessageRssView view = new MessageRssView(username);
        view.setContentType("application/rss+xml;charset=UTF-8");
        return view;
    }

    @RequestMapping("/error/500")
    public String error500() {
        return "error/500";
    }

    @RequestMapping("/messages/old/more")
    public String getMoreOldMessages(@SessionAttribute String userId, Model model) {

        List<Message> oldMessages = messageService.getMoreOldMessages(userId);
        WebUtils.prepareForOutput(oldMessages);
        model.addAttribute("oldMessages", oldMessages);
        return "results/oldMessages";
    }

    @RequestMapping("/messages/analytics")
    public String getAnalytics(@RequestAttribute User loggedUser, Model model, HttpSession session) {
        if (loggedUser.getAccountType() != AccountType.PAID) {
            return home(model, loggedUser.getId(), session);
        }
        List<Message> messages = messageService.getAnalytics(loggedUser.getId());
        WebUtils.prepareForOutput(messages);
        model.addAttribute("messages", messages);
        model.addAttribute("showAnalytics", Boolean.TRUE);
        return "analytics";
    }

    private void addMessagesToSession(Collection<Message> newMessages, HttpSession session, String key,
            String windowId) {
        Map<String, Collection<Message>> map = getSessionMessagesMap(session, key);
        map.put(windowId, newMessages);
        session.setAttribute(key, map);
    }

    private Collection<Message> getMessagesFromSession(HttpSession session, String key, String windowId,
            String userId, boolean important) {

        Map<String, Collection<Message>> map = getSessionMessagesMap(session, key);
        Collection<Message> messages = map.get(windowId);

        if (messages == null) {
            // if there is nothing for that windowId, the session may have
            // expired
            // fetch the home messages to populate the session, with the same
            // windowId
            MessageService.MessagesResult result = messageService.getMessages(userId,
                    Collections.<Message> emptyList(), important);
            Collection<Message> currentMessages = result.getMessages();
            WebUtils.prepareForOutput(currentMessages);

            addMessagesToSession(currentMessages, session, WebConstants.OLDEST_MESSAGES_RETRIEVED, windowId);
            addMessagesToSession(result.getNewestMessages(), session, WebConstants.NEWEST_MESSAGES_RETRIEVED,
                    windowId);

            return currentMessages;
        }

        return messages;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Collection<Message>> getSessionMessagesMap(HttpSession session, String key) {
        Map<String, Collection<Message>> map = (Map<String, Collection<Message>>) session.getAttribute(key);
        if (map == null) {
            map = new HashMap<String, Collection<Message>>();
        }
        return map;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(MessageSearchType.class, new MessageSearchTypePropertyEditor());
    }

    private static class MessageSearchTypePropertyEditor extends PropertyEditorSupport {
        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            setValue(MessageSearchType.valueOf(text.toUpperCase()));
        }

        @Override
        public String getAsText() {
            return getValue().toString().toLowerCase();
        }
    }

    private class MessageRssView extends AbstractRssFeedView {
        private String username;

        public MessageRssView(String username) {
            this.username = username;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected List<Item> buildFeedItems(Map<String, Object> model, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            Collection<Message> messages = (Collection<Message>) model.get("messages");
            List<Item> items = new ArrayList<Item>(messages.size());

            for (Message message : messages) {
                Item item = new Item();
                item.setAuthor(message.getAuthor().getNames() + " (@" + message.getAuthor().getUsername()
                        + ")");
                item.setPubDate(message.getDateTime().toDate());
                item.setLink(baseUrl + "/message/" + message.getId());
                String[] lines = WordUtils.wrap(message.getText(), 60).split(
                        System.getProperty("line.separator"));
                item.setTitle(lines[0] + (lines.length == 1 ? "" : "..."));
                Description desc = new Description();
                desc.setValue(message.getData().getFormattedText());
                item.setDescription(desc);
                items.add(item);
            }

            return items;
        }

        @Override
        protected Channel newFeed() {
            Channel channel = super.newFeed();
            channel.setTitle(username);
            channel.setDescription(baseUrl);
            channel.setLink(baseUrl + "/" + username + "/rss");
            channel.setEncoding("utf-8");
            return channel;
        }
    }
}
