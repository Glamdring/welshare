<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="titleKey" value="searchResults${type}" />
<c:set var="title" value="${msg[titleKey]}" />
<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jsp" %>

<h3><c:out value="${title} \"${keywords}\"" /></h3>
<div id="timeline">
    <div id="newMessages" onclick="showNewMessages()"></div>
    <ol id="messagesList">
    <%@ include file="includes/messages.jsp" %>
    </ol>
    <c:if test="${messages.isEmpty()}">
        ${msg.noResults}
    </c:if>
</div>

<%@ include file="footer.jsp" %>