<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="pictureType" value="small" />
<c:forEach items="${messages}" var="message">
    <c:if test="${message.reply}">
        <c:set var="message" value="${message.originalMessage}" />
    </c:if>
    <li class="message" id="message-${message.publicId}">

        <c:if test="${message.data.externalId != null && loggedUser.profile.showExternalSiteIndicator}">
            <c:set var="sn" value="${applicationScope.socialNetworks[message.data.externalSiteName]}" />
            <c:if test="${sn != null}">
                <img class="externalIndicator" src="<c:url value="${staticRoot}/images/social/${sn.indicatorIcon}" />" title="${msg.sourceIndicatorVia} ${sn.name}" alt="${msg.sourceIndicatorVia} ${sn.name}" />
            </c:if>
        </c:if>
        <c:if test="${message.imported}">
            <c:set var="sn" value="${applicationScope.socialNetworksByPrefix[message.importSource]}" />
            <c:if test="${sn != null}">
                <img class="externalIndicator" src="<c:url value="${staticRoot}/images/social/${sn.indicatorIcon}" />" title="${msg.sourceIndicatorVia} ${sn.name}" alt="${msg.sourceIndicatorVia} ${sn.name}" />
            </c:if>
        </c:if>

        <img class="messageOptionsIcon" src="<c:url value="${staticRoot}/images/options_arrow.png" />" title="${msg.messageOptions}" alt="${msg.messageOptions}" />

        <span>
            <c:set var="user" value="${message.author}" scope="request" />
            <c:set var="size" value="48" />
            <c:set var="cssClass" value="picture" />
            <%@ include file="profilePicture.jsp" %>
        </span>

        <div class="messageContent">
            <c:if test="${message.author.external}">
                <c:url var="targetUrl" value="/user/external/${message.author.externalId}" />
                <c:set var="target" value="_self" />
            </c:if>
            <c:if test="${!message.author.external}">
                <c:url var="targetUrl" value="/${message.author.username}" />
                <c:set var="target" value="_self" />
            </c:if>
            <a class="username" href="${targetUrl}" target="${target}">
                <c:out value="${message.author.names}" />
                <c:if test="${!empty message.author.username}">
                    (<c:out value="${message.author.username}" />)
                </c:if>
            </a>
            <input type="hidden" name="userId" value="${message.author.publicId}" />
            <c:choose>
                <c:when test="${!message.liking || message.data.externalId != null}">
                    <div class="messageText">
                        <span class="textContent">
                            ${message.data.formattedText}
                        </span>

                        <%-- Used by the like dialog, to display the full actual message (with full links). TODO consider fetching with ajax --%>
                        <span class="fullTextContent">
                            <c:out value="${message.text}" />
                        </span>

                        <!-- show "in reply to (original text)" for replies to external messages -->
                        <c:if test="${message.id != null and message.externalOriginalMessageId != null and !message.externalLike and message.text != null and message.text != ''}">
                            <div style="padding-left: 5px;">${msg.inReplyTo}</div>
                            <div class="inReplyTo">
                                <c:if test="${message.data.externalOriginalMessage.data.openMessageInternally}">
                                    <c:url var="messageUrl" value="/message/external/${message.externalOriginalMessageId}" />
                                </c:if>
                                <c:if test="${!message.data.externalOriginalMessage.data.openMessageInternally}">
                                    <c:set var="messageUrl" value="${message.data.externalOriginalMessage.data.externalUrl}" />
                                </c:if>
                                <a href="${messageUrl}" target="_blank">
                                    <c:out value="${message.data.externalOriginalMessage.text}" />
                                </a>
                            </div>
                        </c:if>
                    </div>
                </c:when>
                <c:otherwise>
                    <%@ include file="likeMessage.jsp" %>
                </c:otherwise>
            </c:choose>
            <%@ include file="messageMedia.jsp" %>
            <div>
                <div class="messageMeta">
                    <c:if test="${showAnalytics}">
                        <c:if test="${message.data.clicks > 0}">
                            <strong>${msg.clicks}: ${message.data.clicks}</strong><br />
                        </c:if>
                        <c:forEach items="${message.data.scores}" var="entry">
                            <c:set var="messageKey" value="${entry.key}Score" />
                            <strong>${msg[messageKey]}: ${entry.value}</strong><br />
                        </c:forEach>
                    </c:if>
                    <span class="timeago" title="<w:outputDateTime dateTime="${message.dateTime}" timeZone="${loggedUser.actualTimeZoneId}" />"></span>

                    <c:if test="${message.score > 0}">
                        <span class="scoreHolder" id="score${message.publicId}">${msg.messageLikes}:<span class="score">${message.score}</span></span>
                    </c:if>
                    <%@ include file="messageActions.jsp" %>
                </div>
                <div class="repliesLink">
                    <c:if test="${message.replies != 0}">
                        <a href="javascript:void(0);"
                            onclick="getReplies('${message.publicId}'); disableLink(this);">
                            <c:choose>
                                <c:when test="${message.replies != -1}">
                                    <img src="${staticRoot}/images/comments.png" class="actionIcon" />
                                    ${msg.viewReplies} (${message.replies})
                                </c:when>
                                <c:when test="${message.replies == -1 and !message.liking and fn:startsWith(message.text, '@')}">
                                    <img src="${staticRoot}/images/comments.png" class="actionIcon" />
                                    ${msg.viewConversation}
                                </c:when>
                            </c:choose>
                        </a>
                     </c:if>
                </div>
            </div>

            <ol class="replies"></ol>
        </div>
        <input type="hidden" name="canEdit" value="${message.replies == 0 && message.score == 0 ? 'true' : 'false'}" />
        <input type="hidden" name="isOwn" value="${message.author.id == loggedUser.id ? 'true' : 'false'}" />
        <div style="clear:both;"></div>
    </li>
</c:forEach>
<c:if test="${!result}">
    <%@ include file="reshareDialog.jsp" %>
</c:if>