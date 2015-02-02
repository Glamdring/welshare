<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.analytics}" />
<%@ include file="includes.jsp" %>
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<h3>${msg.analyticsTitle}</h3>
<div id="timeline">
    <c:if test="${messages.isEmpty()}">
        <h3>${msg.noAnalyticsData}</h3>
    </c:if>
    <ol id="messagesList">
        <%@ include file="includes/messages.jsp" %>
    </ol>
    <div id="loadingMore"><img src="${staticRoot}/images/ajax_loading.gif" alt="" title="" /></div>
</div>
<%@ include file="footer.jsp" %>