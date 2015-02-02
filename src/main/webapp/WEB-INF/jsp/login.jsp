<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.login}" />
<%@ include file="header.jsp"%>
<%@ page pageEncoding="UTF-8" %>
<c:url value="/account/login" var="loginUrl" />
<script type="text/javascript" src="${staticRoot}/js/detect_timezone.js"></script>

<c:if test="${!empty error}">
    <span class="error">${msg[error]}</span>
</c:if>
<%@ include file="includes/screenMessages.jsp" %>

<c:set var="externalAuthType" value="${msg.externalLogin}" />
<c:set var="login" value="true" />
<%@ include file="includes/externalAuth.jsp" %>
<div style="clear: both;"></div>
<form:form action="${loginUrl}" class="mainContent">
    <div class="sectionTitle">${msg.login}</div>
    <table>
        <tr>
            <td><label for="username">${msg.usernameOrEmail}</label></td>
            <td><input type="text" class="textInput" name="username" id="username" /></td>
        </tr>
        <tr>
            <td><label for="password">${msg.password}</label></td>
            <td><input type="password" class="textInput" name="password" id="password" /></td>
            <td><a href="/account/forgottenPassword" style="font-size: 0.8em;">${msg.forgottenPassword}</a></td>
        </tr>
        <tr>
            <td><input type="checkbox" name="remember" id="remember" value="1" /><label for="remember" style="font-size: 0.8em;">${msg.rememberMe}</label></td>
            <td><input type="submit" value="${msg.login}" class="bigButton" style="margin-top: 8px;"/></td>
            <td></td>
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