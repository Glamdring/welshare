<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="title" value="${msg.signup}" />
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8"%>
<script type="text/javascript" src="${staticRoot}/js/detect_timezone.js"></script>

<%@ include file="includes/registrationValidation.jsp" %>

<c:url value="/externalAuth/register" var="submitUrl" />

<c:if test="${!sessionScope.isRegistration}">
    ${msg.externalLoginNotExisting}
</c:if>
<c:if test="${sessionScope.isRegistration}">
    ${msg.externalRegistrationPageCaption}
</c:if>

<form:form action="${submitUrl}" modelAttribute="user" id="signupForm" cssClass="mainContent">
    <table id="settings" cellpadding="0" cellspacing="0">
        <tr>
            <td width="80"><label for="username">${msg.username}</label></td>
            <td width="200"><form:input cssClass="textInput" path="username"
                autocomplete="off" id="username" /></td>
            <td><span class="signupMessage" id="usernameMessage"><form:errors
                path="username" cssClass="error" /></span></td>
        </tr>
        <tr>
            <td><label for="email">${msg.email}</label></td>
            <td><form:input cssClass="textInput" path="email"
                autocomplete="off" id="email" /></td>
            <td><span class="signupMessage" id="emailMessage"><form:errors
                path="email" cssClass="error" /></span></td>
        </tr>
        <tr>
            <td></td>
            <td colspan="2"><form:checkbox path="profile.searchableByEmail"
                id="searchableByEmail" /> <label for="searchableByEmail">${msg.searchableByEmail}</label><br />
            ${msg.searchableByEmailNote}</td>
        </tr>

        <tr>
            <td><label for="names">${msg.names}</label></td>
            <td><form:input cssClass="textInput" path="names"
                autocomplete="off" id="names" /></td>
            <td><span class="signupMessage" id="namesMessage"><form:errors
                path="names" cssClass="error" /></span></td>
        </tr>

        <tr>
            <td colspan="3">${msg.agreementNotice}</td>
        </tr>
        <tr>
            <td></td>
            <td><input type="submit" class="bigButton" value="${msg.createAccount}" onclick="return $('#signupForm').valid()" /></td>
            <td></td>
        </tr>
    </table>
    <input type="hidden" name="timezoneId" id="timezoneId" />
</form:form>
<img src="${staticRoot}/images/shade.png" class="panelShade" alt="" />

<script type="text/javascript">
    // ref http://www.onlineaspect.com/2007/06/08/auto-detect-a-time-zone-with-javascript/
    var timezoneId = jzTimezoneDetector.determine_timezone().timezone.olson_tz;
    $("#timezoneId").val(timezoneId);
</script>
<%@ include file="footer.jsp"%>