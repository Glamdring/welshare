<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.contactUsTitle}" />
<%@ include file="../header.jsp" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<%@ include file="../includes/screenMessages.jsp" %>
<br />
<c:url value="/info/contact/sendMessage" var="submitUrl" />
<form:form action="${submitUrl}" method="post" modelAttribute="contactMessage">

    ${msg.contactName}:<br />
    <input type="text" name="names" value="${loggedUser.names}" />
    <form:errors path="names" cssClass="error" /><br />

    ${msg.contactEmail}:<br />
    <input type="text" name="email" value="${loggedUser.email}" />
    <form:errors path="email" cssClass="error" /><br />

    ${msg.contactMessageType}<br />
    <select name="messageType">
        <option value="any">Any</option>
        <option value="problem">Problem</option>
        <option value="suggestion">Suggestion</option>
    </select><br />

    ${msg.contactMessage} (max 1000):<br />
    <textarea name="message" cols="40" rows="10"></textarea>
    <form:errors path="message" cssClass="error" /><br />

    <input type="submit" value="${msg.send}" />
</form:form>
<%@ include file="../footer.jsp" %>