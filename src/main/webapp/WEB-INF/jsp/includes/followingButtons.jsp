<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:if test="${loggedUser != null and user.id != loggedUser.id}"> <!-- no buttons for the current user or if no user is logged-->
    <c:if test="${!wrap}">
        <div id="userActionButtons-${user.id}" class="userActions">
    </c:if>

    <!-- Output the relationship with the current user -->
    <c:if test="${user.followingCurrentUser && !user.followedByCurrentUser}">
        <c:out value="${user.names} ${msg.followsYou}" />
    </c:if>
    <c:if test="${user.followedByCurrentUser && !user.followingCurrentUser}">
        <c:out value="${msg.youFollow} ${user.names} " />
    </c:if>
    <c:if test="${user.followedByCurrentUser && user.followingCurrentUser}">
        <c:out value="${msg.youFollowEachOther}" />
    </c:if>
    &nbsp;
    <c:if test="${!user.followedByCurrentUser}">
        <input type="button" class="followButton" value="${msg.follow}" onclick="follow('${user.id}', '${user.username}'); $(this).hide();"/>
    </c:if>
    <c:if test="${user.followedByCurrentUser}">
        <c:if test="${selectedUser != null or ajaxRequest}">
            <input type="button" value="${user.closeFriendOfCurrentUser ? msg.removeFromCloseFriends : msg.addToCloseFriends}" onclick="toggleCloseFriend('${user.id}', '${user.username}'); $(this).hide();" />
        </c:if>
        <input type="button" class="followButton" value="${msg.unfollow}" onclick="unfollow('${user.id}', '${user.username}'); $(this).hide();"/>
    </c:if>

    <c:if test="${metadataIncluded}">
        <div class="likesThresholdHolder">
            <label for="likesThreshold-${user.id}" class="likesThresholdInfo"
                title="${msg.likesThresholdInfo}">${msg.likesThreshold}:</label>
            <input type="text" size="4" id="likesThreshold-${user.id}" name="likesThreshold" class="thresholdInput" value="${user.likesThreshold}" />

            <input type="checkbox" value="true"<c:if test="${user.hideReplies}"> checked="checked"</c:if> id="hideReplies-${user.id}" name="hideReplies" />
            <label for="hideReplies-${user.id}">${msg.hideReplies}</label>

            <input type="button" onclick="setLikesThreshold('${user.id}');" value="${msg.set}" />
        </div>
    </c:if>
    <c:if test="${!wrap}">
        </div>
    </c:if>
</c:if>