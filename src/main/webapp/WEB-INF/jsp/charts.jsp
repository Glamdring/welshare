<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.chartsTitle}" />
<%@ include file="header.jsp"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag" %>

<script type="text/javascript" src="${staticRoot}/js/jquery.jqplot.min.js"></script>
<script type="text/javascript" src="${staticRoot}/js/chartPlugins/jqplot.highlighter.min.js"></script>
<link rel="stylesheet" type="text/css" href="${staticRoot}/styles/jquery.jqplot.min.css" />

<%@ page pageEncoding="UTF-8"%>

<c:if test="${(twitterStats == null || twitterStats.maxCount == 0)
    && (facebookStats == null || facebookStats.maxCount == 0)
    && (googlePlusStats == null || googlePlusStats.maxCount == 0)
    && welshareStats.maxCount == 0}">
    ${msg.noCharts}
</c:if>

<tag:socialChart maxCount="${twitterStats.maxCount}"
    likes="${twitterStats.retweets}" messages="${twitterStats.tweets}"
    replies="${twitterStats.mentions}" prefix="twitter"
    title="${msg.statsTwitterTitle}" divId="twitterChart"
    messagesTitle="${msg.statsTweets}" repliesTitle="${msg.statsMentions}"
    likesTitle="${msg.statsRetweets}" />

<c:if test="${twitterStats != null}">
    <div class="statsData">
        <span><c:out value="${msg.averageTweets}: ${twitterStats.averageTweets}" /></span>
        <span><c:out value="${msg.averageRetweets}: ${twitterStats.averageRetweets}" /></span>
        <span><c:out value="${msg.averageMentions}: ${twitterStats.averageMentions}" /></span>
        <br />
        <c:if test="${!loggedUser.twitterSettings.tweetWeeklySummary}">
            <a href="<c:url value="/settings/activateTwitterWeeklySummary" />">${msg.activateTwitterWeeklySummary}</a>
            <br />
        </c:if>
        <a href="<c:url value="/twitter/realFollowers" />">${msg.realFollowersTitle}</a>
    </div>
</c:if>

<tag:socialChart maxCount="${facebookStats.maxCount}"
    likes="${facebookStats.likes}" messages="${facebookStats.posts}"
    replies="${facebookStats.comments}" prefix="facebook"
    title="${msg.statsFacebookTitle}" divId="facebookChart"
    messagesTitle="${msg.statsPosts}" repliesTitle="${msg.statsComments}"
    likesTitle="${msg.statsLikes}" />

<c:if test="${facebookStats != null}">
    <div class="statsData">
        <span><c:out value="${msg.averagePosts}: ${facebookStats.averagePosts}" /></span>
        <span><c:out value="${msg.averageLikes}: ${facebookStats.averageLikes}" /></span>
        <span><c:out value="${msg.averageComments}: ${facebookStats.averageComments}" /></span>
    </div>
</c:if>

<tag:socialChart maxCount="${googlePlusStats.maxCount}"
    likes="${googlePlusStats.plusOnes}" messages="${googlePlusStats.posts}"
    replies="${googlePlusStats.replies}" prefix="googlePlus"
    title="${msg.statsGooglePlusTitle}" divId="googlePlusChart"
    messagesTitle="${msg.statsPosts}" repliesTitle="${msg.statsReplies}"
    likesTitle="${msg.statsPlusOnes}" />

<c:if test="${googlePlusStats != null}">
    <div class="statsData">
        <span><c:out value="${msg.averagePosts}: ${googlePlusStats.averagePosts}" /></span>
        <span><c:out value="${msg.averagePlusOnes}: ${googlePlusStats.averagePlusOnes}" /></span>
        <span><c:out value="${msg.averageReplies}: ${googlePlusStats.averageReplies}" /></span>
    </div>
</c:if>

<tag:socialChart maxCount="${welshareStats.maxCount}"
    likes="${welshareStats.likes}" messages="${welshareStats.messages}"
    replies="${welshareStats.replies}" prefix="welshare"
    title="${msg.statsWelshareTitle}" divId="welshareChart"
    messagesTitle="${msg.statsMessages}" repliesTitle="${msg.statsReplies}"
    likesTitle="${msg.statsLikes}" />

<c:if test="${welshareStats != null}">
    <div class="statsData">
        <span><c:out value="${msg.averageMessages}: ${welshareStats.averageMessages}" /></span>
        <span><c:out value="${msg.averageLikes}: ${welshareStats.averageLikes}" /></span>
        <span><c:out value="${msg.averageComments}: ${welshareStats.averageReplies}" /></span>
    </div>
</c:if>


<%@ include file="footer.jsp"%>

