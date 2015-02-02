<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="w" uri="http://welshare.com/tags" %>

<%-- Whether this is the currently logged user or it is a userpage --%>
<c:choose>
    <c:when test="${selectedUser != null && !(selectedUser.id eq loggedUser.id)}">
        <c:set var="user" value="${selectedUser}" scope="request" />
    </c:when>
    <c:otherwise>
       <c:set var="user" value="${loggedUser}" scope="request" />
       <c:set var="selectedUser" value="${null}" />
    </c:otherwise>
</c:choose>

<c:set var="size" value="48" />
<c:set var="cssClass" value="profilePicture" />
<c:set var="pictureType" value="small" />
<%@ include file="includes/profilePicture.jsp" %>
<a href="<c:url value="/${user.username}" />">
    <span class="profileUserName">${user.username}</span>
</a>

<%-- no notifications shown for users other than the current --%>
<c:if test="${selectedUser == null}">
    <span id="notifications" title="${msg.notifications}">0</span>
    <div id="notificationEventsPanel"></div>
    <a href="${root}messages/missed" style="float: right; text-decoration: none;">
        <c:if test="${empty sessionScope.missedImportantMessagesUnreadCount or sessionScope.missedImportantMessagesUnreadCount == 0}">
            <c:set var="unreadImportantMessagesDivStyle" value=" style='display: none;'" />
        </c:if>
        <div id="unreadImportantMessagesCount"${unreadImportantMessagesDivStyle} title="${msg.importantMessagesTitle}">${sessionScope.missedImportantMessagesUnreadCount}</div>
        <img src="${staticRoot}/images/timeline.png"
        alt="${msg.importantMessagesTitle}"
        title="${msg.importantMessagesTitle}" class="linkedImage" id="missedImportantMessagesLink" /> </a>
</c:if>

<div style="font-size: 0.8em;">
<a href="<c:url value="/${user.username}/socialReputation" />">${msg.score}: <fmt:formatNumber value="${user.score + user.externalScore}" groupingUsed="true" /></a>
<br />
<a href="<c:url value="/${user.username}" />">${msg.messageCount}: <fmt:formatNumber value="${user.messages}" groupingUsed="true" /></a><br />
<a href="<c:url value="/${user.username}/messages/topRecent" />" onclick="timedInfo('${msg.pleaseWait}', 30000); return true;">[${msg.topRecent}]</a>
</div>

<hr class="thick" />
<div>
<div style="float: right; font-size: 0.8em;">
    <div style="float: right;">
        <a href="<c:url value="/${user.username}/followers" />">${msg.followers}: <fmt:formatNumber value="${user.followers}" groupingUsed="true" /></a>
    </div><br />
    <div style="float: right;">
        <a href="<c:url value="/${user.username}/following" />">${msg.following}: <fmt:formatNumber value="${user.following}" groupingUsed="true" /></a>
    </div>
    <c:if test="${selectedUser == null}">
        <br /><div style="float: right;">
            <a href="<c:url value="/${user.username}/closeFriends" />">${msg.closeFriends}: <fmt:formatNumber value="${user.closeFriends}" groupingUsed="true" /></a>
        </div>
    </c:if>
</div>

<c:if test="${selectedUser == null}">
    <div style="float: left; font-size: 0.8em">
        <a href="<c:url value="/stats/charts" />" onclick="timedInfo('${msg.pleaseWait}', 30000); return true;"><img src="${staticRoot}/images/side/charts.png" title="${msg.charts}" class="sideMenuIcon" />${msg.charts}</a><br />
        <!-- a href="<c:url value="/messages/analytics" />">${msg.analytics}</a><br /-->
        <a href="<c:url value="/stats/activeFollowers" />"><img src="${staticRoot}/images/side/timeToShare.png" title="${msg.bestTimeToShareTitle}" class="sideMenuIcon" />${msg.bestTimeToShareTitle}</a><br />
        <a href="<c:url value="/stats/lostFollowers" />"><img src="${staticRoot}/images/side/lostFollowers.png" title="${msg.lostFollowers}" class="sideMenuIcon" />${msg.lostFollowersTitle}</a><br />
        <a href="<c:url value="/messages/favourites" />"><img src="${staticRoot}/images/side/favourites.png" title="${msg.favourites}" class="sideMenuIcon" />${msg.favourites}</a><br />
        <a href="<c:url value="/directMessage/list" />"><img src="${staticRoot}/images/side/directMessages.png" title="${msg.directMessages}" class="sideMenuIcon" />${msg.directMessages}</a><br />
        <a href="<c:url value="/scheduledMessages/list" />"><img src="${staticRoot}/images/side/scheduled.png" title="${msg.scheduledMessages}" class="sideMenuIcon" />${msg.scheduledMessages}</a><br />
        <a href="javascript:void(0);" onclick="$('#messageFiltersPanel').slideToggle(350);"><img src="${staticRoot}/images/side/filters.png" title="${msg.bestTimeToShareTitle}" class="sideMenuIcon" />${msg.filters}</a><br />
        <a href="javascript:void(0);" onclick="$('#interestedInKeywordsPanel').slideToggle(350);"><img src="${staticRoot}/images/side/interested.png" title="${msg.interestedIn}" class="sideMenuIcon" />${msg.interestedInKeywords}</a>
        <c:if test="${oldMessages != null && !oldMessages.isEmpty()}">
            <br /><a href="javascript:void(0);" onclick="$('#oldMessages').slideToggle(350);"><img src="${staticRoot}/images/side/suggestions.png" title="${msg.sharingSuggestions}" class="sideMenuIcon" />${msg.sharingSuggestions} (${oldMessages.size()})</a>
        </c:if>
    </div>

<script type="text/javascript">
$(document).ready(function() {
    $("#userSearch").labelify({labelledClass: "labelInside"});
    $("#newMessageFilter").labelify({labelledClass: "labelInside"});
    $("#newInterestedInKeyword").labelify({labelledClass: "labelInside"});
    $("#userSearch").userAutocomplete("${root}users/suggest");
});
<c:forEach items="${interestedInKeywords}" var="interestedIn">
interestedInKeywords.push("${interestedIn.keywords}");
</c:forEach>
</script>
</div>
<div style="clear: both;"></div>
<div id="messageFiltersPanel">
    <input type="text" name="newMessageFilter" id="newMessageFilter" title="${msg.createMessageFilterLabel}" style="width: 140px;" />
    <input type="button" value="${msg.createMessageFilter}" onclick="createMessageFilter()" style="width: 50px; padding: 0px; " />
    <div id="messageFilters">
        <%@ include file="includes/messageFilters.jsp" %>
    </div>
</div>
<div style="clear: both;"></div>
<div id="interestedInKeywordsPanel">
    <input type="text" name="newInterestedInKeyword" id="newInterestedInKeyword" title="${msg.createInterestedInKeywordLabel}" style="width: 140px;" />
    <input type="button" value="${msg.createInterestedInKeyword}" onclick="createInterestedInKeyword()" style="width: 50px; padding: 0px; " />
    <div id="interestedInKeywords">
        <%@ include file="includes/interestedInKeywords.jsp" %>
    </div>
</div>

<c:if test="${oldMessages != null && !oldMessages.isEmpty()}">
    <div id="oldMessages">
        <hr class="thin" />
        <a href="javascript:void(0);" onclick="getMoreOldMessages();">${msg.showMoreOldMessages}</a><br />
        ${msg.oldMessages}:<br /><br />
        <c:forEach var="message" items="${oldMessages}">
            <c:set var="shareMessage" value="${message.text} ${msg.fromTheArchives}" />
            ${message.data.formattedText}&nbsp;<a href="javascript:void(0);" onclick="$('#message').val('${w:escape(shareMessage)}');$('#message').trigger('keyup');">share</a><br /><br />
        </c:forEach>
    </div>
</c:if>

<hr class="thin" />

<%-- Show the option to filter only if there is more than 1 social network connected --%>
<c:if test="${socialNetworksCount > 1}">
    <div style="font-size: 0.8em;">
        ${msg.showMessagesFrom}: <select name="filterNetwork" id="filterNetwork" onchange="getFilteredMessages();" class="dropdown textInput" style="width: 120px;">
            <option value="all">${msg.allNetworks}</option>
            <c:forEach items="${applicationScope.socialNetworks.values()}" var="sn">
                <c:if test="${sn.sharingEnabled}">
                    <c:set var="settingsField" value="${sn.siteName}Settings" />
                    <c:set var="currentUserSettings" value="${loggedUser[settingsField]}" />
                    <c:if test="${currentUserSettings.fetchMessages}">
                        <option value="${sn.prefix}">${sn.name}</option>
                    </c:if>
                </c:if>
            </c:forEach>
            <option value="ws">Welshare</option>
        </select>
    </div>
    <hr class="thin" />
    <div style="clear: both; margin-bottom: 10px;"></div>
</c:if>


<c:if test="${loggedUser.twitterSettings.fetchMessages}">
    <div style="font-size: 0.8em; margin-top: 5px;">
        ${msg.activeTwitterFollowers}: <span id="twitterFollowersActive" class="readerCount">...</span>
    </div>
</c:if>

<c:if test="${loggedUser.profile.warnOnMinutesPerDayLimit and loggedUser.profile.minutesOnlinePerDay > 0}">
    <div style="margin-top: 10px;">
        <c:set var="percentage" value="${(loggedUser.onlineSecondsToday/60) / loggedUser.profile.minutesOnlinePerDay  * 100}" />
        <fmt:formatNumber var="displayPercentage" value="${percentage}" maxFractionDigits="0" />
        <span style="font-size: 0.8em;">${msg.minutesOnlinePerDayPercentage} (${displayPercentage}%)</span>
        <%-- can't display a progress bar with more than 100% --%>
        <c:if test="${percentage > 100}">
            <c:set var="percentage" value="100" />
        </c:if>
        <div style="width: 100%; background-color: #64C7E9; margin-top: 5px;">
            <div style="background-color: darkgreen; min-height: 2px; width: ${percentage}%"></div>
        </div>
    </div>
</c:if>

<c:if test="${loggedUser.twitterSettings.fetchMessages or (loggedUser.profile.warnOnMinutesPerDayLimit and loggedUser.profile.minutesOnlinePerDay > 0)}">
    <hr class="thin" />
</c:if>

<div style="position: relative;">
    <input type="text" class="textInput" name="userSearch" id="userSearch" title="${msg.findPeople}" />
    <img id="userSearchButton" title="${msg.search}" alt="${msg.search}" src="${staticRoot}/images/searchButton.png" onclick="performUserSearch()" />
</div>

<div id="friendSuggestionsHolder"></div>
<input type="button" value="${msg.findFriends}" id="friendFindingButton" onclick="suggestFriends()" style="width: 100%;"/>
</c:if>
<c:if test="${selectedUser != null}">
    <br />
</c:if>