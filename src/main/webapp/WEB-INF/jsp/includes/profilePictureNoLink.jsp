<%@ page pageEncoding="UTF-8" %>

<c:choose>
    <c:when test="${pictureType == 'small' and !empty user.smallProfilePictureURI}">
        <c:set var="src" value="${user.smallProfilePictureURI}" />
    </c:when>
    <c:when test="${pictureType == 'large' and !empty user.largeProfilePictureURI}">
        <c:set var="src" value="${user.largeProfilePictureURI}" />
    </c:when>
    <c:when test="${!empty user.profilePictureURI}">
        <c:set var="src" value="${user.profilePictureURI}" />
    </c:when>
    <c:otherwise>
        <c:set var="src" value="http://www.gravatar.com/avatar/${user.gravatarHash}?s=${size}&d=monsterid&r=PG" />
    </c:otherwise>
</c:choose>