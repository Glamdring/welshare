<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.directMessages}" />
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<script type="text/javascript">
function sendDirectMessage() {
    var text = $("#message").val();
    var recipients = $("#recipients :selected");
    var recipientsText = "";
    var delim = "";
    recipients.each(function(idx, elem) {
        if (idx == 1) {
            delim = ",";
        }
        recipientsText += delim + $(elem).val();
    });

    var params = {text: text, recipients: recipientsText};

    var original = $("#original");
    if (original) {
        $.extend(params, {originalId: original.val()});
    }
    $.post(root + "directMessage/send", params, function(data) {
        info("Message sent");
        $("#message").val("");
        $("#recipients").val("");
    });
}

function deleteDirectMessage(msgId) {
    $.post(root + "directMessage/delete/" + msgId, function(data) {
        if (data) {
            $("#directMessage-" + msgId).fadeOut("slow", function() {
                $("#directMessage-" + msgId).remove();
            });
            info("Message deleted"); //TODO i18n
        }
    });
}

$(document).ready(function() {
    $("#recipients").fcbkcomplete({
        json_url: root + "users/autocompleteList",
        cache: false,
        height: 10,
        filter_case: false,
        filter_hide: true,
        newel: false,
        maxitems: 5
    });
    var options = [];
    <c:forEach items="${recipients}" var="recipient">
        $("#recipients").trigger("addItem", {title:'${recipient.names} (${recipient.username})', value: '${recipient.id}'});
    </c:forEach>
});
</script>
<c:if test="${original != null}">
    <input type="hidden" id="original" value="${original.id}" />
</c:if>

<label for="recipients">${msg.recipientsLabel}:</label>
<input type="text" name="recipients" id="recipients" />
<br />

<textarea tabindex="1" autocomplete="off" rows="2" cols="40"
    id="message" name="message"></textarea>
<input type="button" value="${msg.send}" onclick="sendDirectMessage();"/>

<div id="timeline">
    <ol id="messagesList">
        <c:forEach items="${directMessages}" var="directMessage">
            <!-- get the "recipient" object for the current user, in order to know whether the message is read/unread -->
            <c:forEach items="${directMessage.recipients}" var="recipient">
                <c:if test="${recipient.recipient eq loggedUser}">
                    <c:set var="currentRecipient" value="${recipient}" />
                </c:if>
            </c:forEach>

            <li class="message" id="directMessage-${directMessage.id}" <c:if test="${!currentRecipient.read}">style="background-color: #EDEDED; margin-bottom: 2px;"</c:if>>
                <span>
                    <c:set var="user" value="${directMessage.sender}" scope="request" />
                    <c:set var="size" value="48" />
                    <c:set var="pictureType" value="small" />
                    <c:set var="cssClass" value="picture" />
                    <%@ include file="includes/profilePicture.jsp" %>
                </span>

                <div class="messageContent">
                    <c:url var="targetUrl" value="/${directMessage.sender.username}" />
                    <a class="username" href="${targetUrl}">
                        <c:out value="${directMessage.sender.names}" />
                        (<c:out value="${directMessage.sender.username}" />)
                    </a>
                    &nbsp;&#9658;&nbsp;
                    <c:set var="delimiter" value="" />
                    <c:forEach items="${directMessage.recipients}" var="recipient">
                        ${delimiter}
                        <c:url var="targetUrl" value="/${recipient.recipient.username}" />
                        <a class="username" href="${targetUrl}">
                            <c:out value="${recipient.recipient.names}" />
                            (<c:out value="${recipient.recipient.username}" />)
                        </a>
                        <c:set var="delimiter" value=", " />
                    </c:forEach>
                    <div class="messageText">
                        ${directMessage.formattedText}
                    </div>
                    <div>
                        <div class="messageMeta">
                            <span class="timeago" title="<w:outputDateTime dateTime="${message.dateTime}" timeZone="${loggedUser.actualTimeZoneId}" />"></span>

                            <%@ include file="includes/directMessageActions.jsp" %>
                        </div>
                    </div>
                </div>

                <div style="clear:both;"></div>
            </li>
        </c:forEach>
    </ol>
</div>
<%@ include file="footer.jsp" %>