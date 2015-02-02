<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.signup}" />
<%@ include file="header.jsp"%>
<%@ page pageEncoding="UTF-8" %>
<script type="text/javascript" src="${staticRoot}/js/detect_timezone.js"></script>
<script type="text/javascript">
$(document).ready(function() {
    $("#passwordHint").poshytip({
        content: function(updateCallback) {
            return "${msg.passwordHint}";
        },
        alignTo: 'target',
        alignX: 'center',
        alignY: 'bottom',
        offsetY: 5,
        allowTipHover: true,
        fade: true,
        slide: false,
        className: 'blackTooltip boxTooltip',
        showTimeout: 0,
        showOn: 'hover'
     });
});
</script>
<%@ include file="includes/registrationValidation.jsp" %>

<c:set var="externalAuthType" value="${msg.externalSignup}" />
<c:set var="login" value="false" />
<%@ include file="includes/externalAuth.jsp" %>
<div style="clear: both; padding-top: 5px;"></div>
<c:url value="/account/register" var="signupUrl" />
<form:form action="${signupUrl}" modelAttribute="user" id="signupForm" class="mainContent">
    <div class="sectionTitle">${msg.signup}</div>
    <table id="signupTable" cellpadding="0" cellspacing="0">
        <tr>
            <td width="80"><label for="username">${msg.username}</label></td>
            <td width="200"><form:input cssClass="textInput" path="username" autocomplete="off" id="username" /></td>
            <td><span class="signupMessage" id="usernameMessage"><form:errors path="username" cssClass="error" /></span></td>
        </tr>
        <tr>
            <td><label for="password">${msg.password}</label></td>
            <td><form:password cssClass="textInput" path="password" autocomplete="off"
                id="password" /></td>
            <td><span class="signupMessage" id="passwordMessage"><form:errors path="password" cssClass="error" /></span>
            <a href="javascript:void(0);" id="passwordHint">[?]</a></td>
        </tr>
        <tr>
            <td><label for="email">${msg.email}</label></td>
            <td><form:input cssClass="textInput" path="email" autocomplete="off" id="email" /></td>
            <td><span class="signupMessage" id="emailMessage"><form:errors path="email" cssClass="error" /></span></td>
        </tr>
        <tr>
            <td><label for="names">${msg.names}</label></td>
            <td><form:input cssClass="textInput" path="names" autocomplete="off" id="names" /></td>
            <td><span class="signupMessage" id="namesMessage"><form:errors path="names" cssClass="error" /></span></td>
        </tr>

        <tr>
            <td></td>
            <td class="smallText"><form:checkbox path="profile.searchableByEmail" id="searchableByEmail" />
            <label for="searchableByEmail">${msg.searchableByEmail}</label><br />
            ${msg.searchableByEmailNote}</td>
            <td style="float: right;"><input type="submit" value="${msg.createAccount}" onclick="return $('#signupForm').valid();" class="bigButton" /></td>
        </tr>

        <tr>
            <td colspan="3" style="padding-top: 12px;" class="smallText">${msg.agreementNotice}</td>
        </tr>
    </table>
    <input type="hidden" name="timezoneId" id="timezoneId" />
</form:form>
<img src="${staticRoot}/images/shade.png" class="panelShade" alt="" />

<script type="text/javascript">
    //ref http://www.onlineaspect.com/2007/06/08/auto-detect-a-time-zone-with-javascript/
    var timezoneId = jzTimezoneDetector.determine_timezone().timezone.olson_tz;
    $("#timezoneId").val(timezoneId);
    $("#externalTimezoneId").val(timezoneId);
    $(".externalProvider").each(function(idx, item) {
        item = $(item);
        item.attr("href", item.attr("href") + "&timezoneId=" + timezoneId);
    });
</script>
<%@ include file="footer.jsp"%>