<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.scheduledMessages}" />
<%@ include file="header.jsp" %>
<%@ taglib prefix="w" uri="http://welshare.com/tags" %>
<%@ page pageEncoding="UTF-8" %>

<c:set var="selectedUser" value="${picture.uploader}" scope="request" />

<img src="${w:addSuffix(picture.path, '_large')}" alt="" />

<c:set var="hideMedia" value="true" />
<div id="timeline">
<ol>
<%@ include file="includes/messages.jsp" %>
</ol>
</div>

<%@ include file="footer.jsp" %>