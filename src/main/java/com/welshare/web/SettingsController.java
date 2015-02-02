package com.welshare.web;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.welshare.model.User;
import com.welshare.model.enums.Country;
import com.welshare.model.enums.Language;
import com.welshare.model.social.BitlySettings;
import com.welshare.service.UrlShorteningService;
import com.welshare.service.UserAccountService;
import com.welshare.service.UserService;
import com.welshare.service.impl.shortening.BitlyUrlShorteningService.BitlyUrlShortener;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.qualifiers.Facebook;
import com.welshare.service.social.qualifiers.GooglePlus;
import com.welshare.service.social.qualifiers.LinkedIn;
import com.welshare.service.social.qualifiers.Twitter;
import com.welshare.util.WebUtils;
import com.welshare.web.model.ProfileDetails;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;
import com.welshare.web.util.WebConstants;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @Inject
    private UserService userService;

    @Inject
    private UserAccountService userAccountService;

    @Inject @Twitter
    private SocialNetworkService twitterService;

    @Inject @Facebook
    private SocialNetworkService facebookService;

    @Inject @LinkedIn
    private SocialNetworkService linkedInService;

    @Inject @GooglePlus
    private SocialNetworkService googlePlusService;

    @Inject @BitlyUrlShortener
    private UrlShorteningService bitlyShorteningService;

    @RequestMapping("/account")
    public String viewAccountSettings(@RequestAttribute User loggedUser, Model model) {
        if (loggedUser == null) {
            return "redirect:/login";
        }
        ProfileDetails pd = new ProfileDetails();
        BeanUtils.copyProperties(loggedUser.getProfile(), pd);
        pd.setUsername(loggedUser.getUsername());
        pd.setEmail(loggedUser.getEmail());
        pd.setNames(loggedUser.getNames());
        pd.setProfilePictureURI(loggedUser.getProfilePictureURI());
        pd.setSmallProfilePictureURI(loggedUser.getSmallProfilePictureURI());
        pd.setLargeProfilePictureURI(loggedUser.getLargeProfilePictureURI());
        pd.setGravatarHash(loggedUser.getGravatarHash());

        model.addAttribute("user", pd);
        model.addAttribute("countryList", Country.values());
        model.addAttribute("languageList", Language.values());

        return "accountSettings";
    }

    @RequestMapping("/social")
    public String viewSocialSettings(Model model) {
        return "socialSettings";
    }

    @RequestMapping("/account/save")
    public String saveSettings(
            @Valid @ModelAttribute("user") ProfileDetails profileDetails,
            BindingResult binding, Model model, @RequestAttribute User loggedUser,
            HttpServletRequest request, HttpSession session) {

        if (binding.hasErrors()) {
             model.addAttribute("countryList", Country.values());
             model.addAttribute("languageList", Language.values());

             return "accountSettings";
        }

        try {
            if (!profileDetails.getEmail().equals(loggedUser.getEmail())
                    && !userService.checkEmail(profileDetails.getEmail())) {
                WebUtils.addError(request, "emailAlreadyExists");
            } else if (!profileDetails.getUsername().equals(loggedUser.getUsername())
                    && !userService.checkUsername(profileDetails.getUsername())) {
                WebUtils.addError(request, "usernameTaken");
            } else if (profileDetails.getCountry() == null && StringUtils.isNotBlank(request.getParameter("country"))) {
                WebUtils.addError(request, "invalidCountry");
            } else if (Boolean.TRUE.equals(session.getAttribute(WebConstants.REMEMBER_ME_LOGIN))
                    && (!profileDetails.getEmail().equals(loggedUser.getEmail())
                    || !profileDetails.getUsername().equals(loggedUser.getUsername()))) {
                // disallow email and username change when not logged in with password
                WebUtils.addError(request, "mustBeLoggedInManually");
            } else {
                loggedUser = userAccountService.saveSettings(profileDetails, loggedUser.getId());
                WebUtils.addMessage(request, "settingsSaveSuccess");
            }
        } catch (Exception ex) {
            // TODO handle validation errors
            logger.warn("Problem when saving settings", ex);
        }

        return viewAccountSettings(loggedUser, model);
    }

    @RequestMapping("/social/save/facebook")
    @ResponseBody
    public void saveFacebookSocialSettings(
            @RequestParam(required=false, defaultValue="false") boolean facebookShareLikes,
            @RequestParam(required=false, defaultValue="false") boolean facebookFetchImages,
            @RequestParam(required=false, defaultValue="false") boolean facebookShowInProfile,
            @RequestParam(required=false, defaultValue="false") boolean facebookImportMessages,
            @RequestAttribute User loggedUser) {

        loggedUser.getFacebookSettings().setShareLikes(facebookShareLikes);
        loggedUser.getFacebookSettings().setFetchImages(facebookFetchImages);
        loggedUser.getFacebookSettings().setShowInProfile(facebookShowInProfile);
        loggedUser.getFacebookSettings().setImportMessages(facebookImportMessages);

        facebookService.storeSettings(loggedUser.getFacebookSettings(), loggedUser.getId());
    }

    @RequestMapping("/social/save/twitter")
    @ResponseBody
    public void saveTwitterSocialSettings(
            @RequestParam(required=false, defaultValue="false") boolean twitterShareLikes,
            @RequestParam(required=false, defaultValue="false") boolean twitterFetchImages,
            @RequestParam(required=false, defaultValue="false") boolean twitterShowInProfile,
            @RequestParam(required=false, defaultValue="false") boolean twitterImportMessages,
            @RequestParam(required=false, defaultValue="false") boolean twitterTweetWeeklySummary,
            @RequestAttribute User loggedUser) {

        loggedUser.getTwitterSettings().setShareLikes(twitterShareLikes);
        loggedUser.getTwitterSettings().setFetchImages(twitterFetchImages);
        loggedUser.getTwitterSettings().setShowInProfile(twitterShowInProfile);
        loggedUser.getTwitterSettings().setImportMessages(twitterImportMessages);
        loggedUser.getTwitterSettings().setTweetWeeklySummary(twitterTweetWeeklySummary);

        twitterService.storeSettings(loggedUser.getTwitterSettings(), loggedUser.getId());
    }

    @RequestMapping("/activateTwitterWeeklySummary")
    public String activateTwitterWeeklySummary(@RequestAttribute User loggedUser) {
        loggedUser.getTwitterSettings().setTweetWeeklySummary(true);
        twitterService.storeSettings(loggedUser.getTwitterSettings(), loggedUser.getId());
        return "redirect:/stats/charts";
    }

    @RequestMapping("/social/save/linkedIn")
    @ResponseBody
    public void saveLinkedInSocialSettings(
            @RequestParam(required=false, defaultValue="false") boolean linkedInShareLikes,
            @RequestParam(required=false, defaultValue="false") boolean linkedInFetchImages,
            @RequestParam(required=false, defaultValue="false") boolean linkedInActive,
            @RequestParam(required=false, defaultValue="false") boolean linkedInShowInProfile,
            @RequestAttribute User loggedUser) {

        loggedUser.getLinkedInSettings().setShareLikes(linkedInShareLikes);
        loggedUser.getLinkedInSettings().setFetchImages(linkedInFetchImages);
        loggedUser.getLinkedInSettings().setActive(linkedInActive);
        loggedUser.getLinkedInSettings().setShowInProfile(linkedInShowInProfile);

        linkedInService.storeSettings(loggedUser.getLinkedInSettings(), loggedUser.getId());
    }

    @RequestMapping("/social/save/googlePlus")
    @ResponseBody
    public void saveGooglePlusSocialSettings(
            @RequestParam(required=false, defaultValue="false") boolean googlePlusShareLikes,
            @RequestParam(required=false, defaultValue="false") boolean googlePlusFetchImages,
            @RequestParam(required=false, defaultValue="false") boolean googlePlusActive,
            @RequestParam(required=false, defaultValue="false") boolean googlePlusShowInProfile,
            @RequestParam(required=false, defaultValue="false") boolean googlePlusImportMessages,
            @RequestAttribute User loggedUser) {

        loggedUser.getGooglePlusSettings().setShareLikes(googlePlusShareLikes);
        loggedUser.getGooglePlusSettings().setFetchImages(googlePlusFetchImages);
        loggedUser.getGooglePlusSettings().setActive(googlePlusActive);
        loggedUser.getGooglePlusSettings().setShowInProfile(googlePlusShowInProfile);
        loggedUser.getGooglePlusSettings().setImportMessages(googlePlusImportMessages);

        googlePlusService.storeSettings(loggedUser.getGooglePlusSettings(), loggedUser.getId());
    }


    @RequestMapping("/saveBitlySettings")
    @ResponseBody
    public void saveBitlySettings(@RequestParam String bitlyUser,
            @RequestParam String bitlyApiKey, @RequestAttribute User loggedUser,
            Model model) {

        BitlySettings bitlySettings = new BitlySettings();
        bitlySettings.setUser(bitlyUser);
        bitlySettings.setApiKey(bitlyApiKey);
        bitlyShorteningService.storeShorteningSettings(bitlySettings, loggedUser.getId());
    }

    @RequestMapping("/account/uploadPicture")
    public String uploadProfilePicture(@RequestParam MultipartFile pictureFile,
            @RequestAttribute User loggedUser, HttpServletRequest request, Model model) {

        try {
            if (pictureFile.isEmpty()) {
                throw new IOException("File is empty");
            }
            byte[] bytes = pictureFile.getBytes();

            userAccountService.saveProfilePicture(loggedUser.getId(), pictureFile.getOriginalFilename(), bytes);
            return "redirect:/settings/account?crop=true";

        } catch (IOException ex) {
            WebUtils.addError(request, "noFileSelected");
            return viewAccountSettings(loggedUser, model);
        }
    }

    @RequestMapping("/account/picture/crop")
    @ResponseBody
    public void setCropCoordinates(int x, int y, int size, @SessionAttribute String userId) {
        if (size <= 0) {
            return;
        }
        userAccountService.storeCroppedProfilePictures(x, y, size, userId);
    }

    @RequestMapping("/setImportantMessageThresholds")
    @ResponseBody
    public void setImportantMessageThresholds(@RequestParam int threshold,
            @RequestParam int thresholdRatio, @SessionAttribute String userId) {
        userAccountService.setImportantMessageThresholds(threshold, thresholdRatio, userId);
    }

    @RequestMapping("/social/saveSettings")
    @ResponseBody
    public void saveSocialSettings(
            @RequestParam String likeFormat,
            @SessionAttribute String userId) {

            userAccountService.saveSocialSettings(likeFormat, userId);
    }

    @RequestMapping("/dailyEmailUnsubscribe/{userId}")
    public String unsubscribeFromDailyEmail(@PathVariable String userId, HttpServletRequest request) {
        userAccountService.unsubscribeFromDailyEmail(userId);
        WebUtils.addMessage(request, "successfullyUnsubscribed");
        return "unregisteredHome";
    }
}