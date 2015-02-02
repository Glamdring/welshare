<%@ page pageEncoding="UTF-8" %>
<%@ include file="../includes.jsp" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<% out.clear(); %>

<c:set var="cssClass" value="userPagePicture" />
<c:set var="noExternal" value="true" />
<c:set var="pictureType" value="large" />
<%@ include file="../includes/profilePicture.jsp" %>

<div class="hovercardContent">
    <div class="userPageNames">${user.names}</div>
    <c:if test="${!empty user.username}"><div class="userPageUsername">@${user.username}</div></c:if>

    <c:if test="${user.id != null}">${msg.score}: ${user.score + user.externalScore}<br /></c:if>
    <c:if test="${user.id != null || fn:startsWith(user.externalId, 'tw')}">${msg.messageCount}: <fmt:formatNumber value="${user.messages}" groupingUsed="true" /><br /></c:if>
    <c:if test="${user.id != null || fn:startsWith(user.externalId, 'tw')}">${msg.followers}: <fmt:formatNumber value="${user.followers}" groupingUsed="true" /><br /></c:if>
    <c:if test="${user.id != null || fn:startsWith(user.externalId, 'tw')}">${msg.following}: <fmt:formatNumber value="${user.following}" groupingUsed="true" /><br /></c:if>

    <c:if test="${loggedUser != null && user.externalId == null}">
        <%@ include file="../includes/followingButtons.jsp" %>
    </c:if>

    <c:if test="${loggedUser != null && fn:startsWith(user.externalId, 'tw')}">
        <c:if test="${followedByCurrent}">
            <input type="button" value="${msg.unfollow}" class="twFollowButton" onclick="unfollowOnTwitter('${user.externalId}')" />
        </c:if>
        <c:if test="${!followedByCurrent}">
            <input type="button" value="${msg.follow}" class="twFollowButton" onclick="followOnTwitter('${user.externalId}')" />
        </c:if>
    </c:if>

    <c:if test="${loggedUser != null && fn:startsWith(user.externalId, 'fb')}">
        <c:if test="${!friendOfCurrent}">
            <input type="button" value="${msg.addAsFriend}" onclick="window.location.href='http://www.facebook.com/addfriend.php?id=${fn:replace(user.externalId, 'fb', '')}';" />
        </c:if>
    </c:if>
</div>