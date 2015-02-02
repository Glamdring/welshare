<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.notifications}" />
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>
<c:set var="wholeItemLink" value="false" />
<c:forEach items="${notifications}" var="notification">
   <%@ include file="includes/notification.jsp" %>
</c:forEach>

<%@ include file="footer.jsp" %>