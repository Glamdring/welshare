<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.socialReputationScore}" />
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<h2>${msg.socialReputationScore}: <fmt:formatNumber value="${selectedUser.score + selectedUser.externalScore}" groupingUsed="true" /></h2>

<img src="${staticRoot}/images/auxiliary/icon_big.png" class="connectIcon" /><fmt:formatNumber value="${selectedUser.score}" groupingUsed="true" /><br />
${msg.totalExternalScore}: <span style="font-weight: bold;"><fmt:formatNumber value="${selectedUser.externalScore}" groupingUsed="true" /></span><br />
<c:if test="${selectedUser != null}">
    <%-- preconfigured in ContextListener --%>
    <c:forEach items="${applicationScope.socialNetworks.values()}" var="sn">
        <c:set var="settingsField" value="${sn.siteName}Settings" />
        <c:set var="currentUserSettings" value="${selectedUser[settingsField]}" />
        <c:if test="${currentUserSettings.fetchMessages}">
            <img src="${staticRoot}/images/social/${sn.icon}" class="connectIcon" />
            <fmt:formatNumber value="${reputationScores[sn.prefix].score}" groupingUsed="true" /><br />
        </c:if>
    </c:forEach>
</c:if>

<%@ include file="footer.jsp" %>