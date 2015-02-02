<%@ include file="../header.jsp"%>
<%@ page pageEncoding="UTF-8" %>

<%@ include file="passwordValidation.jsp"%>
<%@ include file="../includes/screenMessages.jsp" %>

<div class="mainContent">
<div class="sectionTitle">${msg.changePassword}</div>
<form action="<c:url value="/account/doChangePassword" />" method="post" id="passwordForm">

    <table>
        <c:if test="${!loggedUser.allowUnverifiedPasswordReset || sessionScope.loggedWithRememberMe}">
        <tr>
            <td><label for="currentPassword">${msg.currentPassword}</label></td>
            <td><input type="password" class="textInput" name="currentPassword" id="currentPassword" /></td>
            <td></td>
        </tr>
        </c:if>
        <tr>
            <td><label for="newPassword">${msg.newPassword}</label></td>
            <td><input type="password" class="textInput" name="newPassword" id="newPassword" /></td>
            <td><span class="signupMessage" id="newPasswordMessage"></span></td>
        </tr>
        <tr>
            <td><label for="newPasswordRepeat">${msg.newPasswordRepeat}</label></td>
            <td><input type="password" class="textInput" name="newPasswordRepeat" id="newPasswordRepeat" /></td>
            <td></td>
        </tr>
        <tr>
            <td></td>
            <td><input type="submit" value="${msg.changePassword}" onclick="return $('#passwordForm').valid();" /></td>
            <td></td>
        </tr>
    </table>
</form>
</div>
<img src="${staticRoot}/images/shade.png" class="panelShade" alt="" />
<%@ include file="../footer.jsp"%>