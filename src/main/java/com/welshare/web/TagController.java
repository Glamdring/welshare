package com.welshare.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.welshare.model.Message;
import com.welshare.model.Tag;
import com.welshare.model.User;
import com.welshare.service.MessageService;
import com.welshare.util.WebUtils;
import com.welshare.web.util.WebConstants;
import com.welshare.web.util.RequestAttribute;

@Controller
@RequestMapping("/tags")
public class TagController {

    @Autowired
    private MessageService service;

    @RequestMapping("/autocomplete")
    @ResponseBody
    public List<Tag> getTagSuggestions(@RequestParam String tagPart) {
        return service.getTagSuggestions(tagPart);
    }

    @RequestMapping("/{tag}")
    public String getTaggedMessages(@RequestAttribute User loggedUser,
            @PathVariable String tag, Model model) {
        List<Message> messages = service.getTaggedMessages(tag, 0, loggedUser);
        WebUtils.prepareForOutput(messages);
        model.addAttribute(WebConstants.MESSAGES_KEY, messages);
        model.addAttribute("tag", tag);
        return "tagged";
    }
}
