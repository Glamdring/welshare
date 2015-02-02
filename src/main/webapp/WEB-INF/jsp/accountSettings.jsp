<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.settingsTitle}" />
<%@ include file="header.jsp"%>
<%@ page pageEncoding="UTF-8"%>

<script type="text/javascript" src="${staticRoot}/js/jquery.jcrop.min.js"></script>
<link rel="stylesheet" href="${staticRoot}/styles/jquery.jcrop.css" type="text/css" />

<script type="text/javascript">
<%@ include file="includes/countries.jsp" %>

$(document).ready(function() {
    $("#country").autocomplete({source:countries, minLength: 3, search: function(event, ui) {
        // should be here, empty, so that user-entered value is sent on submit (wtf)
    }});
    $("#settingsForm").submit(function() {
        var selectedCode = countryCodes[$("#country").val()];
        $("#countryCode").val(selectedCode);
        return true;
    });
    var selected = $("#translateLanguageHidden").val();
    $("#val").append(selected);

    $("#birthDate").datepicker({
        changeMonth: true,
        changeYear: true,
        yearRange: '1920:2010',
        dateFormat: 'dd.mm.yy',
        showButtonPanel: true,
        defaultDate: '<fmt:formatDate value="${user.birthDate.toDate()}" pattern="dd.MM.yyyy" />'
    });
});
$(window).load(function() {
    if (window.location.href.indexOf("crop=true") != -1) {
        openPictureCropPanel();
    }
});
function openPictureCropPanel() {
    $("#pictureCropPanel").dialog({width: 700, height: 525});
    initJcrop();
    //$("#pictureCropPanel img").show();
}
</script>

<c:set var="page" value="account" />

<div class="mainContent">

<%@ include file="includes/settingsMenu.jsp" %>

<c:url value="/settings/account/save" var="submitUrl" />

<%@ include file="includes/screenMessages.jsp" %>

<form action="<c:url value="/settings/account/uploadPicture" />"
    enctype="multipart/form-data" method="POST">
    <c:set var="pictureType" value="large" />
    <c:set var="cssClass" value="userPagePicture" />
    <%@ include file="includes/profilePictureNoLink.jsp" %>
    <img src="${src}" class="linkedImage ${cssClass}" />

    <input type="file" id="pictureFile" name="pictureFile" />
    <input type="submit" value="${msg.upload}" />
    <c:if test="${!empty user.profilePictureURI || !empty user.largeProfilePictureURI || !empty user.smallProfilePictureURI}">
        <input type="button" value="${msg.edit}" onclick="openPictureCropPanel();" />
    </c:if>
</form>
<%@ include file="includes/cropPicture.jsp" %>

<form:form action="${submitUrl}" modelAttribute="user" id="settingsForm">
    <table id="settings" cellpadding="0" cellspacing="1" style="font-size: 0.9em;">
        <tr>
            <td width="110"></td>
            <td width="200"><input type="button" onclick="document.location.href='<c:url value="/account/changePassword" />';" value="${msg.changePassword}" /></td>
            <td></td>
        </tr>
        <tr>
            <td width="110"><label for="username">${msg.username}</label></td>
            <td width="200"><form:input cssClass="textInput${sessionScope.loggedWithRememberMe ? ' readonly' : ''}" path="username"
                autocomplete="off" id="username" readonly="${sessionScope.loggedWithRememberMe}" /></td>
            <td><span class="signupMessage" id="usernameMessage"><form:errors
                path="username" cssClass="error" /></span></td>
        </tr>
        <tr>
            <td><label for="email">${msg.email}</label></td>
            <td><form:input cssClass="textInput${sessionScope.loggedWithRememberMe ? ' readonly' : ''}" path="email"
                autocomplete="off" id="email" readonly="${sessionScope.loggedWithRememberMe}" /></td>
            <td><span class="signupMessage" id="emailMessage"><form:errors
                path="email" cssClass="error" /></span></td>
        </tr>
        <c:if test="${sessionScope.loggedWithRememberMe}">
        <tr>
            <td colspan="2">${msg.mustBeLoggedInManually}</td>
        </tr>
        </c:if>
        <tr>
            <td></td>
            <td colspan="2" style="font-size: 0.8em;"><form:checkbox path="searchableByEmail"
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
            <td><label for="country">${msg.country}</label></td>
            <td><input type="text" class="textInput" id="country"
                value="${user.country.name}" /> <form:hidden path="country"
                id="countryCode" /></td>
            <td><span class="signupMessage" id="countryMessage"><form:errors
                path="country" cssClass="error" /></span></td>
        </tr>

        <tr>
            <td><label for="city">${msg.city}</label></td>
            <td><form:input cssClass="textInput" path="city"
                autocomplete="off" id="city" /></td>
            <td><span class="signupMessage" id="cityMessage"><form:errors
                path="city" cssClass="error" /></span></td>
        </tr>

        <tr>
            <td><label for="originallyFrom">${msg.originallyFrom}</label></td>
            <td><form:input cssClass="textInput" path="originallyFrom"
                autocomplete="off" id="originallyFrom" /></td>
            <td><span class="signupMessage" id="originallyFromMessage"><form:errors
                path="originallyFrom" cssClass="error" /></span></td>
        </tr>

        <tr>
            <td><label for="birthDate">${msg.birthDate}</label></td>
            <td><form:input cssClass="textInput" path="birthDate"
                autocomplete="off" id="birthDate" /></td>
            <td><span class="signupMessage" id="birthDateMessage"><form:errors
                path="birthDate" cssClass="error" /></span></td>
        </tr>

        <tr>
            <td><label for="language">${msg.language}</label></td>
            <td><form:select cssClass="textInput dropdown" path="language"
                items="${languageList}" id="language" itemLabel="name" /></td>
            <td><span class="signupMessage" id="languageMessage"><form:errors
                path="language" cssClass="error" /></span></td>
        </tr>

        <tr>
            <td><label for="bio">${msg.bio}</label></td>
            <td><form:textarea cssClass="textInput" path="bio"
                autocomplete="off" id="bio" style="height: 45px;" /></td>
            <td><span class="signupMessage" id="namesMessage"><form:errors
                path="bio" cssClass="error" /></span></td>
        </tr>

        <tr>
            <td><label for="interests">${msg.interests}</label></td>
            <td><form:textarea cssClass="textInput" path="interests"
                autocomplete="off" id="interests" style="height: 45px;" /></td>
            <td><span class="signupMessage" id="interestsMessage"><form:errors
                path="interests" cssClass="error" /></span></td>
        </tr>

        <tr>
            <td><label for="translateLanguage">${msg.translateLanguage}</label></td>
            <td><select class="textInput dropdown" name="translateLanguage"
                id="translateLanguage">
                    <option value="af">Afrikaans</option>
                    <option value="sq">Albanian</option>
                    <option value="ar">Arabic</option>
                    <option value="az">Azerbaijani</option>
                    <option value="eu">Basque</option>
                    <option value="bn">Bengali</option>
                    <option value="be">Belarusian</option>
                    <option value="bg">Bulgarian</option>
                    <option value="ca">Catalan</option>
                    <option value="zh-CN">Chinese Simplified</option>
                    <option value="zh-TW">Chinese Traditional </option>
                    <option value="hr">Croatian</option>
                    <option value="cs">Czech</option>
                    <option value="da">Danish</option>
                    <option value="nl">Dutch</option>
                    <option value="en">English</option>
                    <option value="eo">Esperanto</option>
                    <option value="et">Estonian</option>
                    <option value="tl">Filipino</option>
                    <option value="fi">Finnish</option>
                    <option value="fr">French</option>
                    <option value="gl">Galician</option>
                    <option value="ka">Georgian</option>
                    <option value="de">German</option>
                    <option value="el">Greek</option>
                    <option value="gu">Gujarati</option>
                    <option value="ht">Haitian Creole</option>
                    <option value="iw">Hebrew</option>
                    <option value="hi">Hindi</option>
                    <option value="hu">Hungarian</option>
                    <option value="is">Icelandic</option>
                    <option value="id">Indonesian</option>
                    <option value="ga">Irish</option>
                    <option value="it">Italian</option>
                    <option value="ja">Japanese</option>
                    <option value="kn">Kannada</option>
                    <option value="ko">Korean</option>
                    <option value="la">Latin</option>
                    <option value="lv">Latvian</option>
                    <option value="lt">Lithuanian</option>
                    <option value="mk">Macedonian</option>
                    <option value="ms">Malay</option>
                    <option value="mt">Maltese</option>
                    <option value="no">Norwegian</option>
                    <option value="fa">Persian</option>
                    <option value="pl">Polish</option>
                    <option value="pt">Portuguese</option>
                    <option value="ro">Romanian</option>
                    <option value="ru">Russian</option>
                    <option value="sr">Serbian</option>
                    <option value="sk">Slovak</option>
                    <option value="sl">Slovenian</option>
                    <option value="es">Spanish</option>
                    <option value="sw">Swahili</option>
                    <option value="sv">Swedish</option>
                    <option value="ta">Tamil</option>
                    <option value="te">Telugu</option>
                    <option value="th">Thai</option>
                    <option value="tr">Turkish</option>
                    <option value="uk">Ukrainian</option>
                    <option value="ur">Urdu</option>
                    <option value="vi">Vietnamese</option>
                    <option value="cy">Welsh</option>
                    <option value="yi">Yiddish</option>
                </select>
                <input type="hidden" value="${user.translateLanguage}" id="translateLanguageHidden" />
            </td>
            <td></td>
        </tr>

         <tr>
            <td><label for="minutesOnlinePerDay">${msg.minutesOnlinePerDayLabel}</label></td>
            <td>
                <form:checkbox path="warnOnMinutesPerDayLimit" id="warnOnMinutesPerDayLimit" onclick="$('#minutesOnlinePerDay').attr('disabled', !$('#minutesOnlinePerDay').attr('disabled'));"/>
                <form:input class="textInput" type="text" name="minutesOnlinePerDay" path="minutesOnlinePerDay" id="minutesOnlinePerDay" disabled="${user.warnOnMinutesPerDayLimit ? 'false' : 'true'}" style="width: 100px;"/>
                ${msg.minutes}
            </td>
            <td></td>
        </tr>

        <tr>
            <td></td>
            <td colspan="2" style="font-size: 0.8em;"><form:checkbox path="showExternalSiteIndicator"
                id="showExternalSiteIndicator" /> <label for="showExternalSiteIndicator">${msg.showExternalSiteIndicator}</label><br />
            </td>
        </tr>
         <tr>
            <td></td>
            <td colspan="2" style="font-size: 0.8em;"><form:checkbox path="emoticonsEnabled"
                id="emoticonsEnabled" /> <label for="emoticonsEnabled">${msg.emoticonsEnabled}</label><br />
            </td>
        </tr>
        <tr>
            <td></td>
            <td colspan="2" style="font-size: 0.8em;"><form:checkbox path="getFollowNotificationsByEmail"
                id="getFollowNotificationsByEmail" /> <label for="getFollowNotificationsByEmail">${msg.getFollowNotificationsByEmail}</label><br />
            </td>
        </tr>
        <tr>
            <td></td>
            <td colspan="2" style="font-size: 0.8em;"><form:checkbox path="receiveMailForReplies"
                id="receiveMailForReplies" /> <label for="receiveMailForReplies">${msg.receiveMailForReplies}</label><br />
            </td>
        </tr>
        <tr>
            <td></td>
            <td colspan="2" style="font-size: 0.8em;"><form:checkbox path="receiveMailForLikes"
                id="receiveMailForLikes" /> <label for="receiveMailForLikes">${msg.receiveMailForLikes}</label><br />
            </td>
        </tr>
        <tr>
            <td></td>
            <td colspan="2" style="font-size: 0.8em;"><form:checkbox path="receiveDailyTopMessagesMail"
                id="receiveDailyTopMessagesMail" /> <label for="receiveDailyTopMessagesMail">${msg.receiveDailyTopMessagesMail}</label><br />
            </td>
        </tr>
        <tr>
            <td></td>
            <td><input type="submit" value="${msg.saveSettings}" /></td>
            <td></td>
        </tr>
    </table>
</form:form>
</div>
<img src="${staticRoot}/images/shade.png" class="panelShade" alt="" />
<%@ include file="footer.jsp"%>

