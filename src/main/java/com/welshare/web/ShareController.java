package com.welshare.web;

import static com.welshare.web.util.WebConstants.MESSAGES_KEY;
import static com.welshare.web.util.WebConstants.MESSAGES_RESULT_VIEW;
import static com.welshare.web.util.WebConstants.REPLIES_KEY;
import static com.welshare.web.util.WebConstants.REPLIES_RESULT_VIEW;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.welshare.model.Message;
import com.welshare.model.User;
import com.welshare.service.MessageService;
import com.welshare.service.PictureService;
import com.welshare.service.ShareService;
import com.welshare.service.ShareService.ResharingDetails;
import com.welshare.service.enums.PictureSize;
import com.welshare.util.WebUtils;
import com.welshare.web.util.WebConstants;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;

@Controller
public class ShareController {

    @Inject
    private MessageService messageService;

    @Inject
    private ShareService shareService;

    @Inject
    private PictureService pictureService;

    @RequestMapping("/share")
    public String share(@RequestParam String text,
            @RequestParam List<String> pictureFiles,
            @RequestParam List<String> externalSites,
            @RequestParam List<String> hideFromUsernames,
            @RequestParam boolean hideFromCloseFriends,
            @RequestParam(required=false, defaultValue="-1") long scheduledTime,
            @SessionAttribute String userId, Model model, HttpSession session) {

        if (userId == null || (StringUtils.isBlank(text) && CollectionUtils
                        .isEmpty(pictureFiles)) || text.length() > ShareService.MAX_MESSAGE_SIZE) {
            return MESSAGES_RESULT_VIEW;
        }
        // TODO protect from cross-site request forgery and XSS

        // instant share
        if (scheduledTime == -1) {
            Message msg = shareService.share(text, userId, pictureFiles, externalSites, hideFromUsernames,
                    hideFromCloseFriends);
            WebUtils.prepareForOutput(msg);

            addMessageToLastRetrievedList(msg, session);
            model.addAttribute(MESSAGES_KEY, Collections.singletonList(msg));
        } else {
            // the scheduledTime millis are coming from the browser in UTC, so no conversion
            shareService.schedule(text, userId, pictureFiles, externalSites, hideFromUsernames,
                    hideFromCloseFriends, new DateTime(scheduledTime));
        }
        return MESSAGES_RESULT_VIEW;
    }

    @RequestMapping("/share/upload")
    @ResponseBody
    public String uploadPicture(HttpEntity<byte[]> entity,
            @RequestParam("qqfile") String originalFilename,
            @SessionAttribute String userId) {

        // TODO protect from cross-site request forgery and XSS
        byte[] data = entity.getBody();
        String filename = pictureService.uploadTempPicture(data, originalFilename, userId);
        filename = WebUtils.addSuffix(filename, PictureSize.SMALL.getSuffix());

        return filename;
    }

    @RequestMapping("/message/reply")
    public String reply(@RequestParam String text,
            @RequestParam String originalMessageId, Model model,
            @SessionAttribute String userId) {

        if (userId == null || StringUtils.isBlank(text)) {
            return MESSAGES_RESULT_VIEW;
        }

        Message msg = shareService.reply(text, originalMessageId, userId);
        WebUtils.prepareForOutput(msg);
        model.addAttribute(REPLIES_KEY, Collections.singletonList(msg));

        return REPLIES_RESULT_VIEW;
    }

    @RequestMapping("/reshare/{messageId}")
    public String reshare(@PathVariable String messageId,
            @RequestParam(required=false, defaultValue="") String comment,
            @RequestParam(required=false) String editedLikedMessage,
            @RequestParam(required=false) boolean shareAndLike,
            @RequestParam List<String> sites, Model model,
            @SessionAttribute String userId,
            HttpSession session, HttpServletResponse response) {

        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        ResharingDetails details = new ResharingDetails();
        details.setComment(comment);
        details.setEditedResharedMessage(editedLikedMessage);
        details.setExternalSites(sites);
        details.setShareAndLike(shareAndLike);
        details.setReshareInternally(sites.contains("ws"));
        Message msg = shareService.reshare(messageId, details, userId).getMessage();

        if (msg != null) {
            WebUtils.prepareForOutput(msg);
            addMessageToLastRetrievedList(msg, session);
            model.addAttribute(MESSAGES_KEY, Collections.singletonList(msg));
        }
        return MESSAGES_RESULT_VIEW;
    }

    @RequestMapping("/simpleLike/{messageId}")
    public String reshare(@PathVariable String messageId, Model model,
            @SessionAttribute String userId, HttpSession session, HttpServletResponse response) {

        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }
        shareService.simpleLike(messageId, userId);
        return MESSAGES_RESULT_VIEW;
    }

    @RequestMapping("/unlike/{messageId}")
    @ResponseBody
    public String unlike(@PathVariable String messageId, @SessionAttribute String userId) {
        String deletedMessageId = shareService.unlike(messageId, userId);
        return deletedMessageId;
    }

    @RequestMapping("/delete/{messageId}")
    @ResponseBody
    public boolean delete(@PathVariable String messageId,
            @RequestParam boolean deleteExternal,
            @RequestAttribute User loggedUser) {

        Message msg = messageService.delete(messageId, deleteExternal, loggedUser);
        if (msg == null) {
            return false;
        }
        return true;
    }

    @RequestMapping("/edit/{messageId}")
    public String edit(@PathVariable String messageId,
            @RequestParam String newText,
            @RequestAttribute User loggedUser,
            Model model) {

        if (loggedUser == null || StringUtils.isBlank(newText) || newText.length() > ShareService.MAX_MESSAGE_SIZE) {
            return MESSAGES_RESULT_VIEW;
        }

        Message msg = messageService.edit(messageId, newText, loggedUser);
        if (msg == null) {
            return null;
        }
        WebUtils.prepareForOutput(msg);
        model.addAttribute(MESSAGES_KEY, Collections.singletonList(msg));

        return MESSAGES_RESULT_VIEW;
    }

    @RequestMapping("/message/shortenUrls")
    @ResponseBody
    public String shortenUrls(@RequestParam String message,
            @RequestParam boolean showTopBar, @RequestParam boolean trackViral,
            @SessionAttribute String userId) {
        if (userId != null) {
            return shareService.shortenUrls(message, userId, showTopBar, trackViral);
        } else {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private void addMessageToLastRetrievedList(Message message, HttpSession session) {
        Map<String, Collection<Message>> windows = (Map<String, Collection<Message>>) session
                .getAttribute(WebConstants.OLDEST_MESSAGES_RETRIEVED);
        if (windows == null) {
            return;
        }

        for (Collection<Message> messages : windows.values()) {
            if (messages instanceof List) {
                ((List<Message>) messages).add(0, message);
            }
        }
    }
}
