package com.welshare.web;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.welshare.model.User;
import com.welshare.service.MessageFilterService;
import com.welshare.web.util.RequestAttribute;

@Controller
public class MessageFilterController {

    @Inject
    private MessageFilterService service;

    @RequestMapping("/filters/create")
    @ResponseBody
    public void createMessageFilter(@RequestParam String text, @RequestAttribute User loggedUser) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        service.createMessageFilter(text, loggedUser);
    }

    @RequestMapping("/filters/delete")
    @ResponseBody
    public void deleteMessageFilter(@RequestParam long filterId, @RequestAttribute User loggedUser) {
       service.deleteMessageFilter(filterId, loggedUser);
    }

    @RequestMapping("/filters/list")
    public String listMessageFilters() {
        // list set in the request by an interceptor
        return "includes/messageFilters";
    }

    @RequestMapping("/interestedIn/create")
    @ResponseBody
    public void createInterestedInKeyword(@RequestParam String keywords, @RequestAttribute User loggedUser) {
        if (StringUtils.isBlank(keywords)) {
            return;
        }
        service.createInterestedInKeyword(keywords, loggedUser);
    }

    @RequestMapping("/interestedIn/delete")
    @ResponseBody
    public void deleteInterestedInKeyword(@RequestParam long id, @RequestAttribute User loggedUser) {
       service.deleteInterestedInKeyword(id, loggedUser);
    }

    @RequestMapping("/interestedIn/list")
    public String listInterestedInKeywords() {
        // list set in the request by an interceptor
        return "includes/interestedInKeywords";
    }
}
