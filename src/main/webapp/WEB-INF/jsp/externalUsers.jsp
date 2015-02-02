<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<%@ include file="includes/screenMessages.jsp" %>

<div id="searchResults">
    <c:set var="noExternal" value="false" />
    <c:set var="cssClass" value="userPagePicture" />
    <c:set var="pictureType" value="small" />
    <h3>${msg[title]}</h3>
    <c:forEach items="${users}" var="user">
        <div class="userEntry">
            <%@ include file="includes/profilePicture.jsp" %>
            <a class="username" href="<c:url value="/user/external/${user.externalId}" />">${user.username}</a> -
            ${user.names}<br />
        </div>
    </c:forEach>
    <c:if test="${users.isEmpty()}">
        <div>${msg.noResults}</div>
    </c:if>
</div>
<%@ include file="footer.jsp" %>