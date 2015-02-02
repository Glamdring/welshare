package com.welshare.web.admin;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.welshare.model.ContactMessage;
import com.welshare.service.BaseService;

@Controller
@RequestMapping("/admin/common")
public class CommonController {

    @Resource(name="baseService")
    private BaseService service;

    @RequestMapping("/contactMessages")
    public String getContactMessages(Model model) {
        model.addAttribute("messages", service.listOrdered(ContactMessage.class, "dateTime"));
        return "admin/contactMessages";
    }
}
