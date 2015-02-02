<%@ include file="../includes.jsp" %>
<%@ page pageEncoding="UTF-8" %>
<% out.clear(); %>
<hr class="thin" />
<a href="javascript:void(0);" onclick="getMoreOldMessages();">${msg.showMoreOldMessages}</a>
${msg.oldMessages}:
<br />
<br />
<c:forEach var="message" items="${oldMessages}">
    <c:set var="shareMessage"
        value="${message.text} ${msg.fromTheArchives}" />
            ${message.data.formattedText}&nbsp;<a
        href="javascript:void(0);"
        onclick="$('#message').val('${w:escape(shareMessage)}');$('#message').trigger('keyup');">share</a>
    <br />
    <br />
</c:forEach>