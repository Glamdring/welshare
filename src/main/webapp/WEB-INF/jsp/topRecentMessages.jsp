<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.topRecentTitle}" />
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<h2><c:out value="${msg.topRecentMessages} ${username}" /></h2>
<div id="timeline">
    <ol id="messagesList">
        <%@ include file="includes/messages.jsp" %>
    </ol>
</div>

<c:if test="${messages.isEmpty()}">
    ${msg.noTopRecentMessages}
</c:if>

<%@ include file="footer.jsp" %>