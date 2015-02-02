<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<div style="text-align: right;">
    <a href="http://twitter.com/#!/search?q=%23${tag}" target="_blank">${msg.seeTagResultsFromTwitter}</a>
</div>
<div id="timeline">
    <div id="newMessages" onclick="showNewMessages()"></div>
    <ol id="messagesList">
        <%@ include file="includes/messages.jsp" %>
    </ol>
</div>
<%@ include file="footer.jsp" %>