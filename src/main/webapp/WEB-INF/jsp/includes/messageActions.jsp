<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<span class="messageActions">

    <c:set var="isOwn" value="${loggedUser.id == message.author.id}" />
    <c:set var="isAddressee" value="${loggedUser.id == message.addressee.id}" />

    <c:if test="${message.data.externalId == null}">
        <span class="messageLink"><a href="<c:url value="/message/${message.id}" />" target="_blank">[${msg.directLink}]</a></span>
    </c:if>
    <c:if test="${message.data.externalUrl != null}">
        <span class="messageLink"><a href="${message.data.externalUrl}" target="_blank">[${msg.directLink}]</a></span>
    </c:if>

    <%-- replies and likes to external messages have a link to the external site so that conversations can be tracked --%>
    <c:if test="${message.id != null and message.externalOriginalMessageId != null}">
       <span class="messageLink"><a href="<c:url value="/externalOriginalMessage/${message.id}" />" target="_blank">[${msg.viewOriginal}]</a></span>
    </c:if>

    <c:if test="${!isOwn}">
        <!-- Reshare button with options -->
        <!-- div class="reshareSideButton"-->
            <a href="javascript:void(0);" onclick="reshare('${message.publicId}', false)" class="reshareLink" id="reshareLink${message.publicId}">
            <img src="${staticRoot}/images/reshare.png" class="actionIcon" alt="" />${msg.reshare}</a>
            <img src="${staticRoot}/images/cog.png" class="reshareOptions likeLink" id="reshareOptions${message.publicId}" title="${msg.reshareOptions}" alt="" />
        <!--/div-->
        <%-- TODO different caption and icon per external social network --%>
        <c:set var="likeCaption" value="${msg.like}" />
        <c:set var="unlikeCaption" value="${msg.like}" />
        <c:set var="likeIcon" value="thumb_up.png" />

        <c:if test="${message.data.externalId.startsWith('tw')}">
            <c:set var="likeCaption" value="Retweet" />
            <c:set var="unlikeCaption" value="Undo retweet" />
            <c:set var="likeIcon" value="retweet.png" />
        </c:if>

        <c:if test="${!message.data.likedByCurrentUser}">
            <a href="javascript:void(0);" onclick="simpleLike('${message.publicId}')" class="likeLink" id="likeLink${message.publicId}">
            <img src="${staticRoot}/images/${likeIcon}" class="actionIcon" alt="" />${likeCaption}</a>
        </c:if>
        <c:if test="${message.data.likedByCurrentUser}">
            <a href="javascript:void(0);" onclick="unlike('${message.publicId}')" class="unlikeLink" id="unlikeLink${message.publicId}">
            <img src="${staticRoot}/images/thumb_up.png" class="actionIcon" alt="" />${unlikeCaption}</a>
        </c:if>
    </c:if>

    <c:set var="mentions" value="" />
    <c:forEach items="${message.data.mentionedUsernames}" var="mention">
        <c:set var="mentions" value="${mentions}@${mention} " />
    </c:forEach>
    <c:set var="originalMessageId" value="${!message.reply && !message.externalReply ? message.publicId : message.publicOriginalMessageId}" />
    <c:set var="inReplyTo" value="${!message.externalReply ? message.publicId : message.publicOriginalMessageId}" />

    <%-- Reply available for internal messages and external messages whose network has sharing enabled --%>
    <c:if test="${sn == null || sn.sharingEnabled}">
        <a href="javascript:void(0);" onclick="startReply('${originalMessageId}', '${inReplyTo}', '${message.author.username}', '${mentions}', ${message.data.externalId != null  and fn:contains(message.data.externalId, 'tw')});">
        <img src="${staticRoot}/images/reply.png" class="actionIcon" alt="" />${msg.reply}</a>
    </c:if>

    <c:if test="${!isOwn}">
        <c:if test="${!message.data.favouritedByCurrentUser}">
            <a href="javascript:void(0);" onclick="favourite('${message.publicId}', ${message.reply});" id="favouriteLink${message.publicId}">
            <img src="${staticRoot}/images/favourite.png" class="actionIcon" alt="" />${msg.favourite}</a>
        </c:if>
        <c:if test="${message.data.favouritedByCurrentUser}">
            <a href="javascript:void(0);" onclick="unfavourite('${message.publicId}', ${message.reply});" id="favouriteLink${message.publicId}">
            <img src="${staticRoot}/images/favourite.png" class="actionIcon" alt="" />${msg.unfavourite}</a>
        </c:if>
    </c:if>

    <c:if test="${isOwn || isAddressee}">
        <a href="javascript:void(0);" onclick="deleteMessage('${message.publicId}', ${message.reply or (message.externalOriginalMessageId != null and !message.externalLike)});">
        <img src="${staticRoot}/images/delete.png" class="actionIcon" alt="" />${msg.delete}</a>
    </c:if>

</span>