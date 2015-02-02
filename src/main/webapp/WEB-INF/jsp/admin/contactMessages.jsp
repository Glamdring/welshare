<%@ include file="../includes.jsp" %>
<html>
<head>
    <%@ include file="../head.jsp" %>
</head>
<body>

<table border="1" callpadding="0" cellspacing="0">
    <tr>
        <td>Email</td>
        <td>Names</td>
        <td>Type</td>
        <td>Date sent</td>
        <td>Message</td>
    </tr>
<jsp:useBean id="dateValue" class="java.util.Date" />
<c:forEach items="${messages}" var="message">
    <tr>
        <td>${message.email}</td>
        <td>${message.names}</td>
        <td>${message.messageType}</td>
        <td><fmt:formatDate value="${message.dateTime.toDate()}" pattern="HH:mm dd.MM.yyyy" /></td>
        <td>${message.message}</td>
    </tr>
</c:forEach>
</table>

</body>
</html>