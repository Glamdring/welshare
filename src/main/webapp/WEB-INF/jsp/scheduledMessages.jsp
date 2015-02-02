<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.scheduledMessages}" />
<%@ include file="header.jsp" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page pageEncoding="UTF-8" %>

<script type="text/javascript">
function deleteScheduledMessage(id) {
    $.post('${root}scheduledMessages/delete', {id: id}, function() {
        info('${msg.messageDeleted}');
        $("#row-" + id).remove();
    });
}
</script>
<c:if test="${messages.isEmpty()}">
<h3>${msg.noScheduledMessages}</h3>
(${msg.noScheduledMessagesHint})
</c:if>

<c:if test="${!messages.isEmpty()}">
<table border="1" cellspacing="0" style="width: 100%;">
    <thead style="font-weight: bold;">
    <tr>
        <td style="width: 120px;">${msg.scheduledTime}</td>
        <td>${msg.text}</td>
        <td style="width: 120px;">${msg.socialNetworks}</td>
        <td style="width: 60px;"></td>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${messages}" var="message">
        <tr id="row-${message.id}">
            <td><span class="time" style="font-size: 0.9em;" title="<w:outputDateTime dateTime="${message.scheduledTime}" timeZone="${loggedUser.actualTimeZoneId}" />"></span></td>
            <td>${message.text}</td>
            <td>
            <c:set var="externalSites" value="${fn:split(message.externalSites, ',')}" />
            <c:forEach items="${externalSites}" var="site">
                <c:forEach items="${applicationScope.socialNetworks.values()}" var="sn">
                    <c:if test="${sn.prefix == site}">
                        <c:out value="${sn.name} " />
                    </c:if>
                </c:forEach>
            </c:forEach>
            </td>
            <td><a href="javascript:void(0);" onclick="deleteScheduledMessage('${message.id}')">
        <img src="${staticRoot}/images/delete.png" class="actionIcon" />${msg.delete}</a></td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>
<%@ include file="footer.jsp" %>