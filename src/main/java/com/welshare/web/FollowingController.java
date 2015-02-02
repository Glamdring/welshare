package com.welshare.web;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.welshare.model.User;
import com.welshare.service.FollowingService;
import com.welshare.service.UserService;
import com.welshare.service.model.UserDetails;
import com.welshare.util.WebUtils;
import com.welshare.web.util.WebConstants;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;

@Controller
public class FollowingController {

    private static final String USERS = "users";

    private static final String TITLE = "title";

    private static final String SELECTED_USER = "selectedUser";

    private static final String METADATA_INCLUDED_KEY = "metadataIncluded";

    @Inject
    private FollowingService service;

    @Inject
    private UserService userService;

    @RequestMapping("/follow/{targetUserId}")
    @ResponseBody
    public void follow(@PathVariable String targetUserId, @SessionAttribute String userId) {
        if (userId != null) {
            service.follow(userId, targetUserId);
        }
    }

    @RequestMapping("/unfollow/{targetUserId}")
    @ResponseBody
    public void unfollow(@PathVariable String targetUserId, @SessionAttribute String userId) {
        if (userId != null) {
            service.unfollow(userId, targetUserId);
        }
    }

    @RequestMapping("{username}/followers")
    public String followers(@PathVariable String username, @RequestAttribute User loggedUser, Model model) {
        if (username == null && loggedUser == null) {
            return WebConstants.REDIRECT_LOGIN;
        }

        if (username == null) {
            username = loggedUser.getUsername();
        }

        User selectedUser = userService.getByUsername(username);
        if (selectedUser == null) {
            return "redirect:/" + username;
        }

        List<UserDetails> result = service.getFollowersDetails(selectedUser.getId());
        if (loggedUser != null) {
            service.updateCurrentUserFollowings(loggedUser.getId(), result, false);
        }

        model.addAttribute(USERS, result);
        model.addAttribute(SELECTED_USER, selectedUser);
        model.addAttribute("title", "followers");

        return USERS;
    }

    @RequestMapping("{username}/following")
    public String following(@PathVariable String username, @RequestAttribute User loggedUser, Model model) {
        if (username == null && loggedUser == null) {
            return WebConstants.REDIRECT_LOGIN;
        }
        if (username == null) {
            username = loggedUser.getUsername();
        }

        User selectedUser = userService.getByUsername(username);
        if (selectedUser == null) {
            return "redirect:/" + username;
        }

        List<UserDetails> result = service.getFollowingDetails(selectedUser.getId());

        // fill metadata only if this is the user's own following list
        boolean includeMetadata = loggedUser != null && loggedUser.getUsername().equals(username);
        if (loggedUser != null) {
            service.updateCurrentUserFollowings(loggedUser.getId(), result, includeMetadata);
        }
        model.addAttribute(METADATA_INCLUDED_KEY, includeMetadata);
        model.addAttribute(SELECTED_USER, selectedUser);
        // only visible to logged users viewing their own followings
        model.addAttribute("showLimitedExternalFollowing", (loggedUser != null && loggedUser.equals(selectedUser)));
        model.addAttribute(TITLE, "following");
        model.addAttribute(USERS, result);

        return USERS;
    }

    @RequestMapping("{username}/closeFriends")
    public String friends(@PathVariable String username, @RequestAttribute User loggedUser, Model model, HttpServletRequest request) {
        if (username == null && loggedUser == null) {
            return WebConstants.REDIRECT_LOGIN;
        }
        if (username == null) {
            username = loggedUser.getUsername();
        }
        User selectedUser = userService.getByUsername(username);
        if (selectedUser == null) {
            WebUtils.addError(request, "inexistentUser");
            return USERS;
        }

        List<UserDetails> result = service.getCloseFriendsDetails(selectedUser.getId());
        // fill metadata only if this is the user's own friends list
        boolean includeMetadata = loggedUser != null && loggedUser.getUsername().equals(username);
        if (loggedUser != null) {
            service.updateCurrentUserFollowings(loggedUser.getId(), result, includeMetadata);
        }
        model.addAttribute(METADATA_INCLUDED_KEY, includeMetadata);
        model.addAttribute(SELECTED_USER, selectedUser);
        model.addAttribute(TITLE, "closeFriends");

        model.addAttribute(USERS, result);

        return USERS;
    }

    @RequestMapping("/friends/suggest")
    public String suggestFriends(@SessionAttribute String userId, Model model) {
        if (userId != null) {
            Set<UserDetails> suggestions = service.getFriendSuggestions(userId);
            model.addAttribute("suggestions", suggestions);
        }
        return "results/friendSuggestions";
    }

    @RequestMapping("/following/userActionButtons")
    public String getUserActionButtons(@RequestParam String targetUserId, @SessionAttribute String userId, Model model) {
        User targetUser = userService.get(User.class, targetUserId);
        if (targetUser != null) {
            UserDetails details = new UserDetails(targetUser);
            service.updateCurrentUserFollowings(userId, Collections.singletonList(details), false);

            model.addAttribute("user", details);
            model.addAttribute("wrap", Boolean.FALSE);
            model.addAttribute("ajaxRequest", Boolean.TRUE);
        }
        return "includes/followingButtons";
    }

    @RequestMapping("/following/setTreshold")
    @ResponseBody
    public void setTreshold(@SessionAttribute String userId,
            @RequestParam String targetUserId, @RequestParam int value, @RequestParam boolean hideReplies) {
        service.setTreshold(userId, targetUserId, value, hideReplies);
    }

    @RequestMapping("/toggleCloseFriend/{targetUserId}")
    @ResponseBody
    public boolean toggleCloseFriend(@SessionAttribute String userId,
            @PathVariable String targetUserId) {
        return service.toggleCloseFriend(userId, targetUserId);
    }
}
