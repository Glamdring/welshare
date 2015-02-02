<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="Twitter - ${user.username}" />
<%@ include file="../../header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<c:set var="cssClass" value="userPagePicture" />
<c:set var="noExternal" value="false" />
<c:set var="pictureType" value="large" />

<c:if test="${user != null}">
    <%@ include file="../../includes/profilePicture.jsp" %>
    <a class="userPageNames" href="${user.externalUrl}">${user.names}</a>
    <a class="userPageUsername" href="${user.externalUrl}">@${user.username}</a>
</c:if>

<c:if test="${user != null}">
    <br />
    <c:if test="${!empty user.city or user.country != null}">
        <c:out value="${msg.location}: ${user.city}" />
        <c:if test="${!empty user.city and user.country != null}">, </c:if>
        <c:out value="${user.country.name}" />
    </c:if>

    <br />
    <c:if test="${!empty user.bio}">
        <c:out value="${msg.bio}: ${user.bio}" />
    </c:if>
    <c:if test="${!empty user.birthDate}">
        ${msg.birthDate}: <fmt:formatDate type="date" value="${user.birthDate.toDate()}" dateStyle="medium" />
    </c:if>
</c:if>
<br />
<c:if test="${followedByCurrent}">
    <input type="button" value="${msg.unfollow}" class="twFollowButton" onclick="unfollowOnTwitter('${user.externalId}')" />
</c:if>
<c:if test="${!followedByCurrent}">
    <input type="button" value="${msg.follow}" class="twFollowButton" onclick="followOnTwitter('${user.externalId}')" />
</c:if>
<div>
${msg.messageCount}: <fmt:formatNumber value="${user.messages}" groupingUsed="true" /><br />
${msg.followers}: <fmt:formatNumber value="${user.followers}" groupingUsed="true" /><br />
${msg.following}: <fmt:formatNumber value="${user.following}" groupingUsed="true" /><br />
</div>

<%@ include file="../externalLikesThreshold.jsp" %>

<div id="timeline">
    <ol id="messagesList">
        <%@ include file="../../includes/messages.jsp" %>
    </ol>
</div>
<%@ include file="../../footer.jsp" %>