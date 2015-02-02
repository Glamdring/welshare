package com.welshare.dao;

import java.util.List;
import java.util.Map;

import com.welshare.dao.enums.SearchType;
import com.welshare.model.Login;
import com.welshare.model.SocialNetworkScore;
import com.welshare.model.User;
import com.welshare.model.enums.Country;

public interface UserDao extends Dao {

    User login(String username, String password);

    User getUserWithCode(String code);

    int cleanNonActiveUsers(long treshold);

    User getByEmail(String email);

    User getByUsername(String username);

    Login getLoginFromAuthToken(String authToken, String series);

    List<User> findByKeywords(String keywords, SearchType searchType);

    List<User> getTopUsers(int page);

    List<User> getTopUsers(Country country, int page);

    List<User> getTopUsers(String city, int page);

    int calculateScore(User user);

    void deleteLogins(String userId);

    List<User> getUsers(List<String> userIds);

    Map<String, SocialNetworkScore> getReputationScores(String userId);

    void deleteReputationScores();

    int getMessageCount(User user);

    void deleteUser(User user);
}
