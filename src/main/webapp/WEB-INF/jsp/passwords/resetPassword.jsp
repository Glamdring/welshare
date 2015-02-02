<%@ include file="../header.jsp"%>
<%@ page pageEncoding="UTF-8" %>

<%@ include file="passwordValidation.jsp"%>
<%@ include file="../includes/screenMessages.jsp" %>

<form action="<c:url value="/account/doResetPassword" />" method="post" id="passwordForm" class="mainContent">

    <table>
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
            <td><input type="hidden" name="username" value="${user.username}" />
            <input type="hidden" name="token" value="${param.token}" /></td>
            <td><input type="submit" value="${msg.changePassword}" onclick="return $('#passwordForm').valid();" class="bigButton" /></td>
            <td></td>
        </tr>
    </table>
</form>
<img src="${staticRoot}/images/shade.png" class="panelShade" alt="" />
<br />
<%@ include file="../footer.jsp"%>