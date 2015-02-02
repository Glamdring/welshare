<%@ page pageEncoding="UTF-8" %>

<span class="likePrefix">
<c:out value="${msg.likes} " /> <a href="<c:url value="/message/${message.originalMessage.id}" />"><c:out value="${msg.messageOf}" /></a> <a href="<c:url value="/${message.originalMessage.author.username}" />">@${message.originalMessage.author.username}</a>
</span>

<c:if test="${!empty message.data.formattedText}"><br />${message.data.formattedText}</c:if>
<div class="messageText likedText">
<span>
    <c:set var="user" value="${message.originalMessage.author}" scope="request" />
    <c:set var="size" value="32" />
    <c:set var="cssClass" value="replyPicture" />
    <%@ include file="profilePicture.jsp" %>
</span>
${message.originalMessage.data.formattedText} <br />
</div>
<%@ include file="messageMedia.jsp" %>

