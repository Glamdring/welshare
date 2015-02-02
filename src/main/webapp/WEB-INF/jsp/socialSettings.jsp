<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.socialSettingsTitle}" />
<%@ include file="header.jsp"%>
<%@ page pageEncoding="UTF-8" %>

<c:set var="page" value="social" />

<div class="mainContent">
<%@ include file="includes/settingsMenu.jsp" %>

<script type="text/javascript">
function submitForms() {
    $.post($("#twitterForm").attr("action"), $("#twitterForm").serialize());
    $.post($("#facebookForm").attr("action"), $("#facebookForm").serialize());
    $.post($("#linkedInForm").attr("action"), $("#linkedInForm").serialize());
    $.post($("#googlePlusForm").attr("action"), $("#googlePlusForm").serialize());
    $.post($("#bitlyForm").attr("action"), $("#bitlyForm").serialize());
    $.post($("#generalForm").attr("action"), $("#generalForm").serialize());
    //TODO show on request completion
    info("${msg.settingsSaveSuccess}");
}
</script>

<!-- Twitter -->
<c:if test="${loggedUser.twitterSettings.fetchMessages == false}">
    <div class="connectBox">
        <a title="Connect with Twitter" href="<c:url value="/twitter/connect?sendInitialMessage=true" />" id="twConnect" class="connectLabel">
            <img src="${staticRoot}/images/social/twitter_connect.png" class="connectIcon" />Connect with Twitter
        </a>
    </div>
</c:if>
<c:if test="${loggedUser.twitterSettings.fetchMessages}">
<div class="connectBox">
    <form action="<c:url value="/settings/social/save/twitter" />" id="twitterForm">
        <a title="Disconnect from Twitter" href="<c:url value="/twitter/disconnect" />">
            <img src="${staticRoot}/images/social/twitter_connect.png" class="connectIcon" />Disconnect from Twitter
        </a>
        <input type="checkbox" id="twitterShareLikes" name="twitterShareLikes" <c:if test="${loggedUser.twitterSettings.shareLikes}">checked="checked"</c:if> />
        <label for="twitterShareLikes" class="smallLabel">${msg.shareLikes}</label><br />

        <input type="checkbox" id="twitterTweetWeeklySummary" name="twitterTweetWeeklySummary" <c:if test="${loggedUser.twitterSettings.tweetWeeklySummary}">checked="checked"</c:if> />
        <label for="twitterTweetWeeklySummary" class="smallLabel">${msg.tweetWeeklySummary}</label><br />

        <input type="checkbox" id="twitterFetchImages" name="twitterFetchImages" <c:if test="${loggedUser.twitterSettings.fetchImages}">checked="checked"</c:if> />
        <label for="twitterFetchImages" class="smallLabel">${msg.fetchImages}</label><br />

        <input type="checkbox" id="twitterShowInProfile" name="twitterShowInProfile" <c:if test="${loggedUser.twitterSettings.showInProfile}">checked="checked"</c:if> />
        <label for="twitterShowInProfile" class="smallLabel">${msg.showInProfile}</label><br />

        <input type="checkbox" id="twitterImportMessages" name="twitterImportMessages" <c:if test="${loggedUser.twitterSettings.importMessages}">checked="checked"</c:if> />
        <label for="twitterImportMessages" class="smallLabel">${msg.importMessages}</label><br />
    </form>
</div>
</c:if>

<!-- Facebook -->
<c:if test="${loggedUser.facebookSettings.fetchMessages == false}">
    <div class="connectBox">
        <a title="Connect with Facebook" href="<c:url value="/facebook/connect?sendInitialMessage=true" />" id="fbConnect" class="connectLabel">
            <img src="${staticRoot}/images/social/facebook_connect.png" class="connectIcon" />Connect with Facebook
        </a>
    </div>
</c:if>
<c:if test="${loggedUser.facebookSettings.fetchMessages}">
<div class="connectBox">
    <form action="<c:url value="/settings/social/save/facebook" />" id="facebookForm">
        <a title="Disconnect from Facebook" href="<c:url value="/facebook/disconnect" />">
            <img src="${staticRoot}/images/social/facebook_connect.png" class="connectIcon" />Disconnect from Facebook
        </a>

        <input type="checkbox" id="facebookShareLikes" name="facebookShareLikes" <c:if test="${loggedUser.facebookSettings.shareLikes}">checked="checked"</c:if> />
        <label for="facebookShareLikes" class="smallLabel">${msg.shareLikes}</label><br />

        <input type="checkbox" id="facebookFetchImages" name="facebookFetchImages" <c:if test="${loggedUser.facebookSettings.fetchImages}">checked="checked"</c:if> />
        <label for="facebookFetchImages" class="smallLabel">${msg.fetchImages}</label><br />

        <input type="checkbox" id="facebookShowInProfile" name="facebookShowInProfile" <c:if test="${loggedUser.facebookSettings.showInProfile}">checked="checked"</c:if> />
        <label for="facebookShowInProfile" class="smallLabel">${msg.showInProfile}</label><br />

        <input type="checkbox" id="facebookImportMessages" name="facebookImportMessages" <c:if test="${loggedUser.facebookSettings.importMessages}">checked="checked"</c:if> />
        <label for="facebookImportMessages" class="smallLabel">${msg.importMessages}</label><br />

        <input type="checkbox" style="visibility: hidden;" />
        <span class="smallLabel" style="visibility: hidden;">placeholder</span><br />
    </form>
</div>
</c:if>

<!-- Google Plus -->
<c:if test="${loggedUser.googlePlusSettings.fetchMessages == false}">
    <div class="connectBox">
        <a title="Connect with Google+" href="<c:url value="/googleplus/connect?sendInitialMessage=true" />" id="gpConnect" class="connectLabel">
            <img src="${staticRoot}/images/social/googleplus_connect.png" class="connectIcon" />Connect with Google+
        </a>
    </div>
</c:if>

<c:if test="${loggedUser.googlePlusSettings.fetchMessages}">
<div class="connectBox">
    <form action="<c:url value="/settings/social/save/googlePlus" />" id="googlePlusForm">
        <a title="Disconnect from Google+" href="<c:url value="/googleplus/disconnect" />">
            <img src="${staticRoot}/images/social/googleplus_connect.png" class="connectIcon" />Disconnect from Google+
        </a>

        <input type="checkbox" id="googlePlusActive" name="googlePlusActive" <c:if test="${loggedUser.googlePlusSettings.active}">checked="checked"</c:if> />
        <label for="googlePlusActive" class="smallLabel">${msg.socialNetworkActive}</label><br />

        <input type="checkbox" id="googlePlusShareLikes" name="googlePlusShareLikes" <c:if test="${loggedUser.googlePlusSettings.shareLikes}">checked="checked"</c:if> />
        <label for="googlePlusShareLikes" class="smallLabel">${msg.shareLikes}</label><br />

        <input type="checkbox" id="googlePlusFetchImages" name="googlePlusFetchImages" <c:if test="${loggedUser.googlePlusSettings.fetchImages}">checked="checked"</c:if> />
        <label for="googlePlusFetchImages" class="smallLabel">${msg.fetchImages}</label><br />

        <input type="checkbox" id="googlePlusShowInProfile" name="googlePlusShowInProfile" <c:if test="${loggedUser.googlePlusSettings.showInProfile}">checked="checked"</c:if> />
        <label for="googlePlusShowInProfile" class="smallLabel">${msg.showInProfile}</label><br />

        <input type="checkbox" id="googlePlusImportMessages" name="googlePlusImportMessages" <c:if test="${loggedUser.googlePlusSettings.importMessages}">checked="checked"</c:if> />
        <label for="googlePlusImportMessages" class="smallLabel">${msg.importMessages}</label><br />
    </form>
</div>
</c:if>

<!-- LinkedIn -->
<c:if test="${loggedUser.linkedInSettings.fetchMessages == false}">
    <div class="connectBox">
        <a title="Connect with LinkedIn" href="<c:url value="/linkedIn/connect?sendInitialMessage=true" />" id="liConnect" class="connectLabel">
            <img src="${staticRoot}/images/social/linkedin_connect.png" class="connectIcon" />Connect with LinkedIn
        </a>
    </div>
</c:if>

<c:if test="${loggedUser.linkedInSettings.fetchMessages}">
<div class="connectBox">
    <form action="<c:url value="/settings/social/save/linkedIn" />" id="linkedInForm">
        <a title="Disconnect from LinkedIn" href="<c:url value="/linkedIn/disconnect" />">
            <img src="${staticRoot}/images/social/linkedin_connect.png" class="connectIcon" />Disconnect from LinkedIn
        </a>

        <input type="checkbox" id="linkedInActive" name="linkedInActive" <c:if test="${loggedUser.linkedInSettings.active}">checked="checked"</c:if> />
        <label for="linkedInActive" class="smallLabel">${msg.socialNetworkActive}</label><br />

        <input type="checkbox" id="linkedInShareLikes" name="linkedInShareLikes" <c:if test="${loggedUser.linkedInSettings.shareLikes}">checked="checked"</c:if> />
        <label for="linkedInShareLikes" class="smallLabel">${msg.shareLikes}</label><br />

        <input type="checkbox" id="linkedInFetchImages" name="linkedInFetchImages" <c:if test="${loggedUser.linkedInSettings.fetchImages}">checked="checked"</c:if> />
        <label for="linkedInFetchImages" class="smallLabel">${msg.fetchImages}</label><br />

        <input type="checkbox" id="linkedInShowInProfile" name="linkedInShowInProfile" <c:if test="${loggedUser.linkedInSettings.showInProfile}">checked="checked"</c:if> />
        <label for="linkedInShowInProfile" class="smallLabel">${msg.showInProfile}</label><br />
    </form>
</div>
</c:if>

<div style="clear: both; width: 100%">&nbsp;</div>
<div style="font-size: 0.8em;"><input type="checkbox" id="sendInitialMessage" onclick="changeLinks(this);"/><label for="sendInitialMessage">${msg.sendInitialMessageToExternalSite}</label></div>

<hr />
<div style="margin-top: 10px;">
    <form id="generalForm" method="post" action="/settings/social/saveSettings">
        <label for="likeFormat">${msg.likeFormat}: </label>
        <input type="text" id="likeformat" name="likeFormat" class="textInput" value="${loggedUser.profile.externalLikeFormat}" />
        <br /><span style="font-size: 0.8em;">${msg.likeFormatDescription}</span>
    </form>
</div>

<script type="text/javascript">
    function changeLinks(checkbox) {
        if ($(checkbox).is(':checked')) {
            $("#twConnect").attr("href", root + "twitter/connect?sendInitialMessage=true")
            $("#fbConnect").attr("href", root + "facebook/connect?sendInitialMessage=true")
            $("#liConnect").attr("href", root + "linkedIn/connect?sendInitialMessage=true")
            $("#gpConnect").attr("href", root + "googleplus/connect?sendInitialMessage=true")
        } else {
            $("#twConnect").attr("href", root + "twitter/connect?sendInitialMessage=false")
            $("#fbConnect").attr("href", root + "facebook/connect?sendInitialMessage=false")
            $("#liConnect").attr("href", root + "linkedIn/connect?sendInitialMessage=false")
            $("#gpConnect").attr("href", root + "googleplus/connect?sendInitialMessage=false")
        }
    }
</script>

<hr />
<div style="margin-top: 10px;">
    ${msg.changeWshrWithBitly}:
    <form action="<c:url value="/settings/saveBitlySettings" />" id="bitlyForm">
        <table>
        <tr>
            <td>Bit.ly user:</td>
            <td><input type="text" name="bitlyUser" id="bitlyUser" class="textInput" value="${loggedUser.bitlySettings.user}"/></td>
        </tr>
        <tr>
            <td>Bit.ly api key:</td>
            <td><input type="text" name="bitlyApiKey" id="bitlyApiKey" class="textInput" value="${loggedUser.bitlySettings.apiKey}" /></td>
        </tr>
        </table>
        <c:if test="${!empty loggedUser.bitlySettings.user}">
            <input type="button" value="${msg.clear}" onclick="$('#bitlyUser').val(''); $('#bitlyApiKey').val('');"/>
        </c:if>
    </form>
</div>

<hr />
<br />
<input type="button" value="${msg.save}" onclick="submitForms();" class="bigButton" style="width: 120px; float: right;" />
<div style="clear: both;"></div>
</div>
<img src="${staticRoot}/images/shade.png" class="panelShade" alt="" />
<%@ include file="footer.jsp"%>