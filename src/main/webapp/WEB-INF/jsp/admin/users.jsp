<%@ include file="../includes.jsp" %>
<html>
<head>
    <%@ include file="../head.jsp" %>
</head>
<script type="text/javascript">
$(document).ready(function() {
    var tr = $("#users tr")[0];
    var tds = $(tr).children();
    tds.click(function() {
        var field = $(this).attr("title");
        if (typeof field != 'undefined') {
            window.location.href = root + 'admin/users/registered?orderBy=' + field;
        }
    });
});

function selectAll(){
    $("[name='selected']").attr('checked', "checked");
}
</script>
<body>
<form action="/admin/users/sendEmail" method="POST">

<table border="1" callpadding="0" cellspacing="0" id="users">
    <tr style="cursor: pointer;">
        <td title="username">Username</td>
        <td title="email">Email</td>
        <td title="registrationTimestamp">Registered time</td>
        <td title="lastLogin">Last login</td>
        <td title="score">Score</td>
        <td title="externalScore">External score</td>
        <td title="messages">Messages</td>
        <td></td>
        <td></td>
        <td></td>
        <td><input type="checkbox" onclick="selectAll(); return false;"/></td>
    </tr>
<jsp:useBean id="dateValue" class="java.util.Date" />
<jsp:useBean id="lastLoginValue" class="java.util.Date" />
<c:forEach items="${users}" var="user">

    <jsp:setProperty name="dateValue" property="time" value="${user.registrationTimestamp}" />
    <jsp:setProperty name="lastLoginValue" property="time" value="${user.lastLogin}" />
    <tr>
        <td><a href="<c:url value="/${user.username}" />" target="_blank">${user.username}</a></td>
        <td>${user.email}</td>
        <td><fmt:formatDate value="${dateValue}" pattern="HH:mm dd.MM.yyyy" /></td>
        <td><fmt:formatDate value="${lastLoginValue}" pattern="HH:mm dd.MM.yyyy" /></td>
        <td>${user.score}</td>
        <td>${user.externalScore}</td>
        <td>${user.messages}</td>
        <td><input type="button" onclick="document.location='<c:url value="/admin/users/loginAs/${user.id}" />'" value="Login as" /></td>
        <td><input type="button" onclick="document.location='<c:url value="/admin/users/changeAdminRights/${user.id}" />'" value="${user.admin ? 'Revoke admin rights' : 'Make admin'}" /></td>
        <td><input type="button" onclick="$.post('<c:url value="/admin/users/recalculateSocialReputationScores/${user.id}" />');" value="Recalculate score" /></td>
        <td><input name="selected" type="checkbox" value="${user.id}" /></td>
    </tr>
</c:forEach>
</table>

<input type="text" name="subject" /><br />
<textarea rows="10" columns="40" id="messageText" name="messageText"></textarea>
<input type="submit" value="Send message" />
</form>
</body>
</html>