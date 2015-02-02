package com.welshare.web.admin;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.welshare.model.User;
import com.welshare.model.WaitingUser;
import com.welshare.service.SocialReputationService;
import com.welshare.service.admin.UserAdminService;
import com.welshare.web.UserSession;

@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

    @Inject
    private UserAdminService userService;

    @Inject
    private SocialReputationService reputationService;

    @RequestMapping("/waiting")
    public String getWaitingUsers(Model model) {
        List<WaitingUser> waitingUsers = userService.listOrdered(
                WaitingUser.class, "registered", false, "registrationTimestamp");
        model.addAttribute("waitingUsers", waitingUsers);

        return "admin/waitingUsers";
    }

    @RequestMapping("/sendInvitationEmail")
    public String sendInvitationEmail(
            @RequestParam("selected") List<String> selectedIds,
            @RequestParam String invitationText) {

        userService.sendInvitationEmail(selectedIds, invitationText);

        return "redirect:/admin/users/waiting";
    }

    @RequestMapping("/sendEmail")
    public String sendEmail(
            @RequestParam("selected") List<String> selectedIds,
            @RequestParam String messageText, @RequestParam String subject) {

        userService.sendEmail(selectedIds, messageText, subject);

        return "redirect:/admin/users/registered";
    }

    @RequestMapping("/registered")
    public String getRegisteredUsers(Model model,
            @RequestParam(value = "orderBy", required = false) String orderBy) {
        if (StringUtils.isBlank(orderBy)) {
            orderBy = "registrationTimestamp";
        }
        List<User> users = userService.listOrdered(User.class, orderBy + " DESC");
        model.addAttribute("users", users);

        return "admin/users";
    }

    @RequestMapping("/loginAs/{userId}")
    public String loginAs(@PathVariable String userId, HttpSession session) {
        UserSession.setUser(session, userId);
        return "redirect:/";
    }

    @RequestMapping("/changeAdminRights/{userId}")
    public String changeAdminRights(@PathVariable String userId, Model model) {
        User user = userService.get(User.class, userId);
        user.setAdmin(!user.isAdmin());
        userService.save(user);
        return getRegisteredUsers(model, null);
    }

    @RequestMapping("/recalculateSocialReputationScores/{userId}")
    @ResponseBody
    public void recalculateSocialReputationScores(@PathVariable String userId) {
        User user = userService.get(User.class, userId);
        reputationService.calculateSocialReputation(user);
    }
}
