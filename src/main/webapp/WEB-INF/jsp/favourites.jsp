<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.favourites}" />
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<div id="timeline">
    <div id="newMessages" onclick="showNewMessages()"></div>
    <ol id="messagesList">
        <%@ include file="includes/messages.jsp" %>
    </ol>
</div>
<%@ include file="footer.jsp" %>