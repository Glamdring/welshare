<%@ include file="../header.jsp"%>
<%@ page pageEncoding="UTF-8" %>

<%@ include file="../includes/screenMessages.jsp" %>
<form action="<c:url value="/account/remindPassword" />" method="post" class="mainContent">
    <div class="sectionTitle">${msg.forgottenPasswordTitle}</div>
    <label for="username">${msg.usernameOrEmail}:</label>
    <input type="text" class="textInput" name="username" id="username" />
    <input type="submit" value="${msg.sendPasswordRecoveryInstructions}" class="bigButton" style="width: 200px;"/>
</form>
<img src="${staticRoot}/images/shade.png" class="panelShade" alt="" />
<br />
<%@ include file="../footer.jsp"%>