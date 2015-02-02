<%@ include file="../includes.jsp" %>
<%@ page pageEncoding="UTF-8" %>
<c:if test="${notification.targetMessage != null}">
    <c:set var="targetUrl" value="${root}message/${notification.targetMessage.publicId}" />
</c:if>
<c:if test="${notification.notificationType == 'FOLLOW'}">
    <c:set var="targetUrl" value="${root}${notification.sender.username}" />
</c:if>
<c:if test="${notification.notificationType == 'DIRECT_MESSAGE'}">
    <c:set var="targetUrl" value="${root}directMessage/list" />
</c:if>
<c:if test="${notification.externalMessageId != null && !notification.showInternally}">
    <c:set var="targetUrl" value="${notification.href}" />
</c:if>
<c:if test="${notification.externalMessageId != null && notification.showInternally}">
    <c:set var="targetUrl" value="${root}message/external/${notification.externalMessageId}" />
</c:if>

<div class="notificationListItem unselected<c:if test="${unreadNotifications}"> unreadNotification</c:if>">

    <c:if test="${notification.externalMessageId != null && loggedUser.profile.showExternalSiteIndicator}">
        <c:set var="sn" value="${applicationScope.socialNetworks[notification.externalSiteName]}" />
        <c:if test="${sn != null}">
            <img class="notificationExternalIndicator" src="<c:url value="${staticRoot}/images/social/${sn.icon}" />" title="${msg.sourceIndicatorVia} ${sn.name}" alt="${msg.sourceIndicatorVia} ${sn.name}" />
        </c:if>
    </c:if>

    <c:if test="${wholeItemLink == true}">
        <a href="${targetUrl}"<c:if test="${notification.externalMessageId != null}"> target="_blank"</c:if> class="sideNotificationLink">
    </c:if>
    <c:set var="user" value="${notification.sender}" scope="request" />
    <c:set var="size" value="36" />
    <c:set var="pictureType" value="small" />
    <%@ include file="../includes/profilePictureNoLink.jsp" %>
    <img src="${src}" class="profilePictureSmall linkedImage" style="padding-left: 3px;"/>
    <c:if test="${notification.notificationType != null}">
        <c:if test="${wholeItemLink == false}">
            <a href="${root}${notification.sender.username}">
        </c:if>
        <c:out value="@${notification.sender.username} (${notification.sender.names})" />
        <c:if test="${wholeItemLink == false}">
            </a>
            <a href="${targetUrl}"<c:if test="${notification.externalMessageId != null}"> target="_blank"</c:if>>
        </c:if>
        <c:out value=" ${msg[notification.notificationType.name]}" />
        <c:if test="${wholeItemLink == false}">
            </a>
        </c:if>
    </c:if>

    <c:if test="${notification.externalMessageId != null}">
        <c:if test="${wholeItemLink == false and notification.notificationType == null}">
            <a href="${targetUrl}"<c:if test="${notification.externalMessageId != null}"> target="_blank"</c:if>>
        </c:if>
        <c:out value=" ${notification.textMessage}" />
        <c:if test="${wholeItemLink == false and notification.notificationType == null}">
            </a>
        </c:if>
    </c:if>
    <c:if test="${wholeItemLink == true}">
        </a>
    </c:if>
    <span class="timeago" title="<w:outputDateTime dateTime="${notification.dateTime}" timeZone="${loggedUser.actualTimeZoneId}" />"></span>
    <c:if test="${notification.externalMessageId != null}">
        <input type="hidden" name="externalMessageId" value="${notification.externalMessageId}" />
    </c:if>
</div>