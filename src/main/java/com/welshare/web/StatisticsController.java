package com.welshare.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.welshare.model.ActiveReadersEntry;
import com.welshare.model.User;
import com.welshare.service.MessageService;
import com.welshare.service.MessageService.BestTimesToShare;
import com.welshare.service.social.FollowersTrackingService;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.qualifiers.Facebook;
import com.welshare.service.social.qualifiers.GooglePlus;
import com.welshare.service.social.qualifiers.Twitter;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;

@Controller
@RequestMapping("/stats")
public class StatisticsController {

    private static final Comparator<ActiveReadersEntry> BEST_TIME_COMPARATOR = new BestTimeToShareComparator();

    @Inject @Twitter
    private SocialNetworkService twitterService;

    @Inject @Facebook
    private SocialNetworkService facebookService;

    @Inject @GooglePlus
    private SocialNetworkService googlePlusService;

    @Inject
    private MessageService messageService;

    @Inject
    private FollowersTrackingService followersTrackingService;

    @RequestMapping("/charts")
    public String charts(@RequestAttribute User loggedUser, Model model, HttpServletResponse response) {
        if (loggedUser != null) {
            model.addAttribute("twitterStats", twitterService.getStats(loggedUser));
            model.addAttribute("facebookStats", facebookService.getStats(loggedUser));
            model.addAttribute("googlePlusStats", googlePlusService.getStats(loggedUser));
            model.addAttribute("welshareStats", messageService.getStats(loggedUser));
        }
        return "charts";
    }

    @RequestMapping("/activeFollowers")
    public String activityStats(@SessionAttribute String userId, Model model) {
        if (userId != null) {
            Map<String, BestTimesToShare> bestTimes = messageService.getBestTimesToShare(userId);
            model.addAttribute("bestTimes", bestTimes);
        }
        return "bestTimeToShare";
    }

    @RequestMapping("/lostFollowers")
    public String getLostFollowers(@SessionAttribute String userId, Model model) {
        model.addAttribute("lostFollowers", followersTrackingService.getLostFollowers(userId));
        return "lostFollowers";
    }

    @RequestMapping("/recommendedTimeToShare")
    @ResponseBody
    public List<String> getRecommendedTimeToShare(@RequestAttribute User loggedUser, Model model) {
        Map<Integer, Double> bestTimes = Maps.newHashMap();
        if (loggedUser != null) {
            DateTime now = new DateTime(DateTimeZone.forID(loggedUser.getActualTimeZoneId()));
            Map<String, BestTimesToShare> stats = messageService.getBestTimesToShare(loggedUser.getId());
            // we don't care about which social network the value is for. We will display the top 4 times regardless of the network
            for (BestTimesToShare bt : stats.values()) {
                List<ActiveReadersEntry> list = null;
                if (now.getDayOfWeek() == DateTimeConstants.SATURDAY || now.getDayOfWeek() == DateTimeConstants.SUNDAY) {
                    list = bt.getWeekends();
                } else {
                    list = bt.getWeekdays();
                }

                if (list.size() >= 48) {
                    Collections.sort(list, BEST_TIME_COMPARATOR);
                    for (int i = 0; i < 4; i++) {
                        ActiveReadersEntry entry = list.get(i);
                        Double existingValue = bestTimes.get(entry.getMinutes());
                        // don't allow replacing the existing entry with one that has lower percentage from another network
                        if (existingValue == null || existingValue < entry.getActivePercentage()) {
                            bestTimes.put(entry.getMinutes(), entry.getActivePercentage());
                        }
                    }
                }
            }
        }
        List<Integer> minutesList = new ArrayList<Integer>(bestTimes.keySet());
        Collections.sort(minutesList);
        Collections.reverse(minutesList);
        if (minutesList.size() > 4) {
            minutesList = minutesList.subList(0, 4);
        }
        List<String> resultList = Lists.newArrayListWithCapacity(minutesList.size());
        for (int minutes : minutesList) {
            String hour = String.valueOf(minutes / 60);
            String minute = String.valueOf(minutes % 60);
            resultList.add(StringUtils.leftPad(hour, 2, '0') + ":" + StringUtils.leftPad(minute, 2, '0'));
        }
        return resultList;
    }

    private static final class BestTimeToShareComparator implements Comparator<ActiveReadersEntry> {

        @Override
        public int compare(ActiveReadersEntry o1, ActiveReadersEntry o2) {
            return -1 * Doubles.compare(o1.getActivePercentage(), o2.getActivePercentage());
        }

    }
}
