package com.welshare.web;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.keyvalue.DefaultKeyValue;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.welshare.model.User;
import com.welshare.model.enums.Country;
import com.welshare.service.FollowingService;
import com.welshare.service.UserAccountService;
import com.welshare.service.UserService;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.helper.FacebookHelper;
import com.welshare.service.social.helper.TwitterHelper;
import com.welshare.util.WebUtils;
import com.welshare.web.social.FacebookController;
import com.welshare.web.social.TwitterController;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;

@Controller
@RequestMapping("/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private static final String USERS = "users";

    @Value("${profile.pictures.dir}")
    private String profilePicturesDir;

    @Inject
    private UserService userService;

    @Inject
    private UserAccountService userAccountService;

    @Inject
    private FollowingService followingService;

    @Inject
    private TwitterController twitterController;

    @Inject
    private FacebookController facebookController;

    @RequestMapping("/find/{keywords}")
    public String findUsers(@PathVariable String keywords, Model model) {
        List<UserDetails> users = userService.findUsers(keywords);
        model.addAttribute(USERS, users);
        return USERS;
    }

    @RequestMapping("/suggest")
    public String suggestUsers(@RequestParam String keywords, Model model) {
        List<UserDetails> details = userService.suggestUsers(keywords);

        model.addAttribute(USERS, details);

        return "results/userSuggestions";
    }

    @RequestMapping("/autocomplete")
    public String suggestUsers(@RequestParam String start, @SessionAttribute User currentUser, Model model) {

        List<UserDetails> details = userService.suggestUsers(start, currentUser);

        model.addAttribute(USERS, details);

        return "results/userSuggestions";
    }

    /**
     * Method to handle the fcbkcomplete library, which has "tag" hardcoded as
     * param. Changing it is not preferable, because of possible upgrades
     *
     * @param tag
     * @param currentUser
     * @param model
     * @return the view with results
     */
    @RequestMapping("/autocompleteList")
    @ResponseBody
    public List<DefaultKeyValue> suggestUsersList(@RequestParam String tag, @SessionAttribute User currentUser) {

        List<UserDetails> details = userService.suggestUsers(tag, currentUser);
        List<DefaultKeyValue> result = new ArrayList<DefaultKeyValue>(details.size());
        for (UserDetails detail : details) {
            // doing HTML stuff here, which is not particularly nice, but no
            // other way to do it
            String pic = detail.getSmallProfilePictureURI();
            if (pic == null) {
                pic = "http://www.gravatar.com/avatar/" + detail.getGravatarHash() + "?s=16&d=identicon&r=PG";
            }

            String display = "<img class=\"userAutocompletePicture\" " + "src=\"" + pic + "\"/>"
                    + detail.getNames() + " (" + detail.getUsername() + ")";

            result.add(new DefaultKeyValue(display, detail.getId()));
        }

        return result;
    }

    @RequestMapping("/autocompleteBox")
    @ResponseBody
    public List<UserDetails> suggestUsers(@RequestParam String start, @SessionAttribute User currentUser) {

        List<UserDetails> details = userService.suggestUsers(start, currentUser);

        return details;
    }

    @RequestMapping("/picture/{filename}")
    public void getProfilePicture(@PathVariable String filename, OutputStream outputStream) {
        // TODO serve statically (/move to service)
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(profilePicturesDir + "/" + filename);
            IOUtils.copy(fis, outputStream);
        } catch (IOException ex) {
            logger.warn("Problem obtaining image", ex);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    @RequestMapping("/top/{page}")
    public String getTopUsers(@PathVariable int page, Model model, @SessionAttribute String userId) {
        List<UserDetails> users = userService.getTopUsers(page);
        followingService.updateCurrentUserFollowings(userId, users, false);
        model.addAttribute(USERS, users);
        model.addAttribute("topUsers", Boolean.TRUE);
        return USERS;
    }

    @RequestMapping("/top")
    public String getTopUsers(Model model, @SessionAttribute String userId) {
        return getTopUsers(0, model, userId);
    }

    @RequestMapping("/top/country/{country}/{page}")
    public String getTopUsersByCountry(@PathVariable String country, @PathVariable int page, Model model,
            @SessionAttribute String userId, HttpServletRequest request) {

        Country foundCountry = Country.getByName(country);
        if (foundCountry == null) {
            WebUtils.addError(request, "noResultsForCountry");
            return getTopUsers(model, userId);
        }
        List<UserDetails> users = userService.getTopUsers(foundCountry, page);
        followingService.updateCurrentUserFollowings(userId, users, false);
        model.addAttribute(USERS, users);
        model.addAttribute("topUsers", Boolean.TRUE);
        model.addAttribute("country", country);
        return USERS;
    }

    @RequestMapping("/top/city/{city}/{page}")
    public String getTopUsersByCity(@PathVariable String city, @PathVariable int page, Model model,
            @SessionAttribute String userId) {
        List<UserDetails> users = userService.getTopUsers(city, page);
        // TODO check if cloning of the users is required (used to be here)
        followingService.updateCurrentUserFollowings(userId, users, false);
        model.addAttribute(USERS, users);
        model.addAttribute("topUsers", Boolean.TRUE);
        model.addAttribute("country", city);
        return USERS;
    }

    @RequestMapping("/top/country/{country}")
    public String getTopUsersByCountry(@PathVariable String country, Model model,
            @SessionAttribute String userId, HttpServletRequest request) {
        return getTopUsersByCountry(country, 0, model, userId, request);
    }

    @RequestMapping("/top/city/{city}")
    public String getTopUsersByCity(@PathVariable String city, Model model, @SessionAttribute String userId) {
        return getTopUsersByCity(city, 0, model, userId);
    }

    @RequestMapping("/limited")
    public String getLimitedUsers(@SessionAttribute String userId, Model model) {
        List<UserDetails> users = userAccountService.getLimitedUsers(userId);
        followingService.updateCurrentUserFollowings(userId, users, false);
        model.addAttribute(USERS, users);
        model.addAttribute("title", "limitedExternalUsers");
        return "externalUsers";
    }

    @RequestMapping("/info/{id}")
    public String getUserInfo(@PathVariable String id, @RequestAttribute User loggedUser, Model model) {
        // TODO show something for unregistered users with an option to register
        if (loggedUser == null) {
            return null;
        }

        UserDetails details = userService.getByPublicId(id, loggedUser.getId());
        model.addAttribute("user", details);

        if (details == null || (details.getId() == null && details.getExternalId() == null)) {
            return null; // no user found
        }

        // external-service specific code
        if (id.startsWith(TwitterHelper.PUBLIC_ID_PREFIX)) {
            twitterController.getUser(id, loggedUser, model);
        } else if (id.startsWith(FacebookHelper.PUBLIC_ID_PREFIX)) {
            facebookController.getUser(id, loggedUser, model);
        }

        return "results/userInfo";
    }

    @ModelAttribute("countryList")
    public Country[] getCountryList() {
        return Country.values();
    }

    @RequestMapping("/deleteCurrent")
    public String deleteUser(@SessionAttribute String userId) {
        userService.deleteUser(userId);
        return "redirect:/logout";
    }

    @RequestMapping("/clearDisconnectReasons")
    public void clearDisconnectReasons(@SessionAttribute String userId) {
        userService.clearDisconnectReasons(userId);
    }
}
