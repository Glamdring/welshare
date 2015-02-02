<%@ include file="../includes.jsp" %>
<html>
<head>
    <%@ include file="../head.jsp" %>
</head>
<body>
<form action="/admin/users/sendInvitationEmail" method="POST">

<table border="1" callpadding="0" cellspacing="0">
    <tr>
        <td>Email</td>
        <td>Request time</td>
        <td>Invited</td>
        <td></td>
    </tr>
<jsp:useBean id="dateValue" class="java.util.Date" />
<c:forEach items="${waitingUsers}" var="wuser">

    <jsp:setProperty name="dateValue" property="time" value="${wuser.registrationTimestamp}" />
    <tr>
        <td>${wuser.email}</td>
        <td><fmt:formatDate value="${dateValue}" pattern="HH:mm dd.MM.yyyy" /></td>
        <td><c:if test="${wuser.invitationSent}">yes</c:if></td>
        <td><input name="selected" type="checkbox" value="${wuser.waitingUserId}" /></td>
    </tr>
</c:forEach>
</table>

<textarea rows="10" columns="40" id="invitationText" name="invitationText"></textarea>
<input type="submit" value="Send invitation" />
</form>
</body>
</html>