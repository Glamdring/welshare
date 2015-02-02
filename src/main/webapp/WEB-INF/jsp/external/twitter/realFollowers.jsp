<%@ include file="../../header.jsp" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page pageEncoding="UTF-8" %>

<h3>${msg.realFollowersTitle}</h3>

<c:choose>
    <c:when test="${sessionScope.twitterSettings != null || loggedUser != null}">
        <c:set var="url" value="${appProperties['base.url']}/twitter/realFollowers" />
        <c:if test="${loggedUser != null && loggedUser.twitterSettings.realFollowers > 0}">
            <div id="currentResult">
                <fmt:formatNumber var="percentage" value="${100 * loggedUser.twitterSettings.realFollowers / userDetails.followers}" maxFractionDigits="0" />
                <c:set var="realFollowersResult" value="${loggedUser.twitterSettings.realFollowers} (${percentage}%)" />
                ${msg.realFollowers}: <strong>${realFollowersResult}</strong>
                <a href="https://twitter.com/intent/tweet?text=<c:out value="${msg.realFollowersTweetStart} ${fn:replace(realFollowersResult, '%', '%25')}. ${msg.realFollowersTweetEnd} ${url}" />&via=welshare" target="_blank">
                    <img src="<c:url value="${staticRoot}/images/tweet_button.png" />" style="margin-left: 10px; vertical-align: middle;" class="linkedImage" />
                </a>
            </div>
        </c:if>
        <input type="button" onclick="$('#currentResult').hide(); calculateRealTwitterFollowers(
            'resultHolder', this, '${msg.calculatingRealFollowersWait}',
            '${msg.realFollowersResult}', '${msg.realFollowersError}',
            '${msg.realFollowersTweetStart}', '${msg.realFollowersTweetEnd}',
            '${msg.excludedFollowers}', '${url}');" value="${msg.calculateRealFollowers}" />
        <span id="resultHolder"></span>
    </c:when>
    <c:otherwise>
        <a title="Connect with Twitter" href="<c:url value="/twitter/simpleConnect?featureUri=/twitter/realFollowers" />" id="twConnect" class="connectLabel">
            <img src="${staticRoot}/images/social/twitter_connect.png" class="connectIcon" />Connect with Twitter
        </a>

        <div style="margin-bottom: 40px;"></div><hr />
        <a href="<c:url value="/signup?featureUri=/twitter/realFollowers" />">${msg.realFollowersRegister}</a>
    </c:otherwise>
</c:choose>

<%@ include file="../../footer.jsp" %>