package com.welshare.web;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.welshare.model.DirectMessage;
import com.welshare.model.DirectMessageRecipient;
import com.welshare.model.User;
import com.welshare.service.DirectMessageService;
import com.welshare.util.WebUtils;
import com.welshare.web.util.RequestAttribute;

@Controller
@RequestMapping("/directMessage")
public class DirectMessageController {

    @Inject
    private DirectMessageService service;

    @RequestMapping("/send")
    @ResponseBody
    public void sendDirectMessage(@RequestParam("text") String text,
            @RequestParam("recipients") String recipients,
            @RequestParam(value="originalId", required=false) String originalId,
            @RequestAttribute User loggedUser) {

        service.sendDirectMessage(text, originalId, loggedUser,
                Lists.newArrayList(Splitter.on(",").split(recipients)));
    }

    @RequestMapping("/list")
    public String listDirectMessages(@RequestAttribute User loggedUser, Model model) {
        List<DirectMessage> messages = service.getIncomingDirectMessages(loggedUser, 0);

        for (DirectMessage dm : messages) {
            dm.setFormattedText(WebUtils.prepareForOutput(dm.getText()).getText());
        }
        // mark all as read immediately after fetching
        service.markAllAsRead(loggedUser);
        model.addAttribute("directMessages", messages);

        return "directMessages";
    }

    @RequestMapping("/reply/{id}")
    public String reply(@PathVariable String id, @RequestAttribute User loggedUser, Model model) {
        DirectMessage original = service.get(DirectMessage.class, id);

        model.addAttribute("original", original);

        List<User> recipients = new ArrayList<User>(original.getRecipients().size());
        recipients.add(original.getSender());
        for (DirectMessageRecipient recipient : original.getRecipients()) {
            if (!recipient.getRecipient().equals(loggedUser)) {
                recipients.add(recipient.getRecipient());
            }
        }
        model.addAttribute("recipients", recipients);
        return listDirectMessages(loggedUser, model);
    }

    @RequestMapping("/delete/{messageId}")
    @ResponseBody
    public boolean delete(@PathVariable String messageId,
            @RequestAttribute User loggedUser) {
        return service.delete(messageId, loggedUser);
    }
}
