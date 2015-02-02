package com.welshare.web;

import java.net.MalformedURLException;
import java.util.Locale;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;

import com.welshare.model.ContactMessage;
import com.welshare.service.UserService;
import com.welshare.util.WebUtils;

@Controller
@RequestMapping("/info")
public class InfoController {

    private static final Logger logger = LoggerFactory.getLogger(InfoController.class);

    @Inject
    private UserService userService;

    @Inject
    private ServletContext ctx;

    @RequestMapping("/about")
    public void about() {
        //just forward to view
    }
    @RequestMapping("/contact")
    public void contact() {
        //just forward to view
    }
    @RequestMapping("/license")
    public void license() {
        //just forward to view
    }

    @RequestMapping("/features")
    public String features(Locale locale) {
        return getLocaleSpecificPage("info/features/features_", locale);
    }

    @RequestMapping("/privacy")
    public String privacy(Locale locale) {
        return getLocaleSpecificPage("info/privacy/privacy_", locale);
    }

    @RequestMapping("/tos")
    public String tos(Locale locale) {
        return getLocaleSpecificPage("info/tos/tos_", locale);
    }

    @RequestMapping("/reshare")
    public String reshare(Locale locale) {
        return getLocaleSpecificPage("info/reshare/reshare_", locale);
    }

    private String getLocaleSpecificPage(String viewNamePrefix, Locale locale) {
        try {
            String viewName = viewNamePrefix + locale.getLanguage().toLowerCase();
            if (ctx.getResource("/WEB-INF/jsp/" + viewName + ".jsp") != null) {
                return viewName;
            }
        } catch (MalformedURLException ex) {
            logger.error("Unable to show info page" + viewNamePrefix + " for locale " + locale, ex);
        }

        return viewNamePrefix + "en";
    }

    @RequestMapping("/contact/sendMessage")
    public String sendContactMessage(@Valid ContactMessage contactMessage,
            BindingResult binding, HttpServletRequest request) {
        if (!binding.hasErrors()) {
            contactMessage.setMessage(StringUtils.left(contactMessage.getMessage(), 1000));
            contactMessage.setDateTime(new DateTime());
            userService.save(contactMessage);
            WebUtils.addMessage(request, "contactMessageSent");
            userService.notifyAdminUsers("New feedback received on welshare");
        }
        return "info/contact";
    }
}
