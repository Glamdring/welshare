<%@ page pageEncoding="UTF-8" %>
<c:set var="pictureType" value="small" />
<c:forEach items="${replies}" var="reply">
    <li class="reply" id="reply-${reply.publicId}">
        <img class="replyOptionsIcon" src="<c:url value="${staticRoot}/images/options_arrow.png" />" title="${msg.messageOptions}" alt="${msg.messageOptions}" />
        <span>
            <c:set var="user" value="${reply.author}" scope="request" />
            <c:set var="size" value="32" />
            <c:set var="cssClass" value="replyPicture" />
            <%@ include file="profilePicture.jsp" %>
        </span>

        <div class="replyContent">
            <c:if test="${empty reply.author.username}">
                <c:set var="targetUrl" value="${reply.author.externalUrl}" />
            </c:if>
            <c:if test="${!empty reply.author.username}">
                <c:url var="targetUrl" value="/${reply.author.username}" />
            </c:if>
            <a class="username" href="${targetUrl}">
            <c:if test="${reply.author.username != null}">
                ${reply.author.username} -
            </c:if>
            ${reply.author.names}</a>
            <div class="replyText">${reply.data.formattedText}</div>
            <%-- Used by the like dialog, to display the full actual message (with full links). TODO consider fetching with ajax --%>
            <span class="fullTextContent">
                ${reply.text}
            </span>
        </div>
        <c:set var="message" value="${reply}" />
        <div style="font-size: 1.1em;">
            <span class="timeago" title="${reply.dateTime}"></span>
            <c:if test="${reply.score > 0}">
                <span class="scoreHolder" id="score${reply.publicId}">${msg.messageLikes}: <span class="score">${reply.score}</span></span>
            </c:if>
            <%@ include file="messageActions.jsp" %>
        </div>
        <input type="hidden" name="canEdit" value="${reply.replies == 0 && reply.score == 0 ? 'true' : 'false'}" />
        <input type="hidden" name="isOwn" value="${reply.author.id == loggedUser.id ? 'true' : 'false'}" />
        <input type="hidden" name="userId" value="${reply.author.publicId}" />
        <div style="clear:both;"></div>
    </li>
</c:forEach>