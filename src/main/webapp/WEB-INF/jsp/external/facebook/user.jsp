<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="title" value="Facebook - ${user.names}" />
<%@ include file="../../header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<c:set var="cssClass" value="userPagePicture" />
<c:set var="noExternal" value="false" />
<c:set var="pictureType" value="large" />

<c:if test="${user != null}">
    <%@ include file="../../includes/profilePicture.jsp" %>
    <a class="userPageNames" href="${user.externalUrl}">${user.names}</a>
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
<c:if test="${!friendOfCurrent}">
    <input type="button" value="${msg.addAsFriend}" onclick="window.location.href='http://www.facebook.com/addfriend.php?id=${fn:replace(user.externalId, 'fb', '')}';" />
</c:if>
<c:if test="${friendOfCurrent}">
    <%@ include file="../externalLikesThreshold.jsp" %>
</c:if>

<div id="timeline">
    <ol id="messagesList">
        <%@ include file="../../includes/messages.jsp" %>
    </ol>
</div>
<%@ include file="../../footer.jsp" %>