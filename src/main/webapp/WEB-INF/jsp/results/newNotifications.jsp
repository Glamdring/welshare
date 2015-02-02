<%@ include file="../includes.jsp" %>
<%@ page pageEncoding="UTF-8" %>
<% out.clear(); %>
<c:set var="wholeItemLink" value="true" />
<c:forEach items="${notifications}" var="notification">
    <%@ include file="../includes/notification.jsp" %>
</c:forEach>

<div id="notificationsBottom"><a href="<c:url value="/notifications/all" />">${msg.viewNotifications}</a></div>