<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.bestTimeToShareTitle}" />
<%@ include file="includes.jsp" %>
<%@ include file="header.jsp" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag" %>
<%@ page pageEncoding="UTF-8" %>

<script type="text/javascript" src="${staticRoot}/js/jquery.jqplot.min.js"></script>
<script type="text/javascript" src="${staticRoot}/js/chartPlugins/jqplot.highlighter.min.js"></script>
<link rel="stylesheet" type="text/css" href="${staticRoot}/styles/jquery.jqplot.min.css" />

<h3>${msg.bestTimesToShare}</h3>

<c:if test="${bestTimes.isEmpty()}">
    ${msg.noSocialNetworksForBestTimeToShare}
</c:if>

<c:if test="${bestTimes['tw'] != null}">
    <tag:bestTimeToShareChart divId="twitterChart" prefix="tw"
        bestTimesToShare="${bestTimes['tw']}" max="${bestTimes['tw'].maxValue}"
        weekdayTitle="${msg.weekdays}" weekendTitle="${msg.weekends}"
        title="${msg.bestTimesToTweet} " />
</c:if>

<c:if test="${bestTimes['fb'] != null}">
    <tag:bestTimeToShareChart divId="facebookChart" prefix="fb"
        bestTimesToShare="${bestTimes['fb']}" max="${bestTimes['fb'].maxValue}"
        weekdayTitle="${msg.weekdays}" weekendTitle="${msg.weekends}"
        title="${msg.bestTimesToPost} " />
</c:if>

<%@ include file="footer.jsp" %>