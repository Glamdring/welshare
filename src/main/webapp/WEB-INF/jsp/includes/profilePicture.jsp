<%@ page pageEncoding="UTF-8" %>

<%@ include file="profilePictureNoLink.jsp" %>

<c:choose>
    <c:when test="${noExternal or !user.external}">
        <c:url value="${root}${user.username}" var="userUrl" />
    </c:when>
    <c:otherwise>
        <c:url value="${user.externalUrl}" var="userUrl" />
    </c:otherwise>
</c:choose>

<a href="${userUrl}">
    <img src="${src}" class="linkedImage ${cssClass}" alt="${user.names}" title="${user.names}" />
</a>