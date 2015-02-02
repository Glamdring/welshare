<%@ page pageEncoding="UTF-8" %>
<span class="messageActions">

    <a href="<c:url value="/directMessage/reply/${directMessage.id}" />">
    <img src="${staticRoot}/images/reply.png" class="actionIcon" />${msg.reply}</a>

    <a href="javascript:void(0);" onclick="deleteDirectMessage('${directMessage.id}');">
    <img src="${staticRoot}/images/delete.png" class="actionIcon" />${msg.delete}</a>

</span>