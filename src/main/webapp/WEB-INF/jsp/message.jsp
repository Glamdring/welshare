<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.messageBy} ${message.author.names}" />
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>
<script type="text/javascript">
    $(document).ready(function() {
        var replies = $("#message-${message.publicId} .replies .reply");
        handleReplyMessageActionsHover(replies);
    });
</script>
<div id="timeline">
    <div class="message singleMessage" id="message-${message.publicId}" itemscope itemtype="http://schema.org/CreativeWork">
         <c:if test="${loggedUser.profile.showExternalSiteIndicator && message.data.externalId != null}">
            <c:set var="sn" value="${applicationScope.socialNetworks[message.data.externalSiteName]}" />
            <c:if test="${sn != null}">
                    <img class="externalIndicator" src="<c:url value="${staticRoot}/images/social/${sn.icon}" />" title="${msg.sourceIndicatorVia} ${sn.name}" alt="${msg.sourceIndicatorVia} ${sn.name}" />
            </c:if>
        </c:if>
        <c:if test="${message.imported}">
            <c:set var="sn" value="${applicationScope.socialNetworksByPrefix[message.importSource]}" />
            <c:if test="${sn != null}">
                <img class="externalIndicator" src="<c:url value="${staticRoot}/images/social/${sn.indicatorIcon}" />" title="${msg.sourceIndicatorVia} ${sn.name}" alt="${msg.sourceIndicatorVia} ${sn.name}" />
            </c:if>
        </c:if>


        <img class="messageOptionsIcon" src="<c:url value="${staticRoot}/images/options_arrow.png" />" title="${msg.messageOptions}" alt="${msg.messageOptions}" />

        <c:set var="user" value="${message.author}" scope="request" />
        <c:set var="size" value="48" />
        <c:set var="pictureType" value="small" />
        <%@ include file="includes/profilePictureNoLink.jsp" %>
        <img src="${src}" class="profilePicture" />

        <c:if test="${message.author.external}">
            <c:set var="targetUrl" value="${message.author.externalUrl}" />
            <c:set var="target" value="_blank" />
        </c:if>
        <c:if test="${!message.author.external}">
            <c:url var="targetUrl" value="/${message.author.username}" />
            <c:set var="target" value="_self" />
        </c:if>

        <div class="singleMessageAuthor">
            <a href="${targetUrl}" target="${target}">${message.author.names}</a>
        </div>
        <div class="singleMessageUsername">
            <c:if test="${!empty message.author.username}">
                @${message.author.username}
            </c:if>&nbsp;
        </div>
        <input type="hidden" name="userId" value="${message.author.publicId}" />

        <c:choose>
        <c:when test="${!message.liking || message.data.externalId != null}">
            <div class="singleMessageText"><span class="textContent">${message.data.formattedText}</span>
                <!-- show "in reply to (original text)" for replies to external messages -->
                <c:if test="${message.id != null and message.externalOriginalMessageId != null and !message.externalLike}">
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
            <%@ include file="includes/likeMessage.jsp" %>
        </c:otherwise>
        </c:choose>
        <meta itemprop="name" content="Welshare post" />
        <meta itemprop="description" content="${message.text}" />
        <%-- Used by the like dialog, to display the full actual message (with full links). TODO consider fetching with ajax --%>
        <span class="fullTextContent" itemprop="text"><c:out value="${message.text}" /></span>
        <c:if test="${message.reply}">
            <%-- placeholder so that replies are working even if a message is shown outside of conversation context --%>
            <div id="message-${message.originalMessage.id}">
                <div class="replies"></div>
            </div>
        </c:if>

        <div id="messagePictures" class="messageMeta">
            <%@ include file="includes/messageMedia.jsp" %>
        </div>

        <div class="messageMeta">
            <span class="timeago" title="<w:outputDateTime dateTime="${message.dateTime}" timeZone="${loggedUser.actualTimeZoneId}" />"></span>

            <c:if test="${message.score > 0}">
                <span class="scoreHolder" id="score${message.publicId}">${msg.messageLikes}: <span class="score">${message.score}</span></span>
            </c:if>

            <%@ include file="includes/messageActions.jsp" %>
        </div>

        <input type="hidden" name="canEdit" value="${message.replies == 0 && message.score == 0 ? 'true' : 'false'}" />
        <input type="hidden" name="isOwn" value="${message.author.id == loggedUser.id ? 'true' : 'false'}" />

        <div class="messageContent">
            <ol class="replies"><%@ include file="includes/replies.jsp" %></ol>
        </div>
    </div>
</div>
<%@ include file="includes/reshareDialog.jsp" %>
<%@ include file="footer.jsp" %>