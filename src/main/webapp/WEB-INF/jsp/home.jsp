<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jsp" %>

<script type="text/javascript">
$(document).ready(function() {
    initPolling('${windowId}', ${important});

    <c:if test="${!paged}">
        $(window).scroll(function(){
            if ($(window).scrollTop() > $(document).height() - $(window).height() - 110){
                showMoreMessages('${windowId}', ${important});
            }
         });
    </c:if>

    <c:if test="${!loggedUser.viewedStartingHints}">
        if ($("#twConnect").length > 0) {
            $("#twConnect").callout({ css: "blue", position: "bottom", msg: "${msg.socialSettingsHint}"});
        } else {
            $("#fbConnect").callout({ css: "blue", position: "bottom", msg: "${msg.socialSettingsHint}"});
        }

        //$("#topUsersLink").callout({ css: "blue small", position: "right", pointer: "top", align: "bottom", msg: "${msg.topUsersHint}"});
        //$("#side").callout({ css: "blue", position: "bottom", msg: "${msg.findingFriendsHint}"});
        $("#reshareHint").callout({ css: "blue", position: "bottom", msg: '${msg.reshareHint}'});
        $(document).click(function() {
            $("#twConnect").callout("destroy");
            $("#topUsersLink").callout("destroy");
            $("#side").callout("destroy");
            $("#reshareHint").callout("destroy");
            $("#reshareHint").hide();
        });
        $.post(root + "account/markViewedStartingHints");
    </c:if>

    <c:forEach items="${socialNetworkStatuses}" var="s">
        info("${msg[s]}");
    </c:forEach>

    <c:if test="${!empty param.socialNetworkStatuses}">
        info("${msg[param.socialNetworkStatuses]}");
    </c:if>

    <%-- if the some account was forcibly disconnected, alert the user --%>
    var disconnectReasons = "";
    <c:forEach items="${applicationScope.socialNetworks.values()}" var="sn">
        <c:set var="settingsField" value="${sn.siteName}Settings" />
        <c:set var="currentUserSettings" value="${loggedUser[settingsField]}" />
        <c:if test="${currentUserSettings.disconnectReason != null}">
            disconnectReasons += "${sn.name}: ${msg.yourAccountWasDisconnected}: ${currentUserSettings.disconnectReason}\n";
        </c:if>
    </c:forEach>
    showDisconnectReasons(disconnectReasons);
});
</script>

<c:set var="shareOptionsOn" value="true" />
<c:if test="${param.message != null}">
    <c:set var="initialText" value="${param.message}" />
</c:if>
<%@ include file="includes/shareBox.jsp" %>

<c:if test="${!loggedUser.viewedStartingHints}">
    <h2>${msg.welcomeMessage}</h2>
    <a href="<c:url value="/settings/social" />">${msg.configureSocialSettings}</a>
</c:if>

<c:if test="${!loggedUser.closedHomepageConnectLinks && (loggedUser.twitterSettings.fetchMessages == false ||
    loggedUser.facebookSettings.fetchMessages == false)}">
    <div class="homeConnectBox" style="width: 98%; margin-top: 5px;" id="homeConnectPanel">
        <c:if test="${loggedUser.twitterSettings.fetchMessages == false}">
            <a title="Connect with Twitter" href="<c:url value="/twitter/connect?sendInitialMessage=false" />" id="twConnect" class="connectLabel" style="margin-right: 40px;">
               <img src="${staticRoot}/images/social/twitter_connect.png" class="connectIcon" />Connect with Twitter
            </a>
        </c:if>
        <c:if test="${loggedUser.facebookSettings.fetchMessages == false}">
            <a title="Connect with Facebook" href="<c:url value="/facebook/connect?sendInitialMessage=false" />" id="fbConnect" class="connectLabel" style="margin-right: 40px;">
               <img src="${staticRoot}/images/social/facebook_connect.png" class="connectIcon" />Connect with Facebook
            </a>
        </c:if>
        <c:if test="${loggedUser.googlePlusSettings.fetchMessages == false}">
            <a title="Connect with Google+" href="<c:url value="/googleplus/connect?sendInitialMessage=false" />" id="gpConnect" class="connectLabel">
               <img src="${staticRoot}/images/social/googleplus_connect.png" class="connectIcon" />Connect with Google+
            </a>
        </c:if>
        <span class="closeLink" onclick="closeHomeConnectPanel()">×</span>
    </div>
</c:if>

<%@ include file="includes/screenMessages.jsp" %>

<c:if test="${!loggedUser.viewedStartingHints}">
    <div align="center" style="margin-top: 7px;"><img src="${staticRoot}/images/home_reshare.png" id="reshareHint" /></div>
</c:if>

<div id="timeline">
    <div id="newMessages" onclick="showNewMessages()"></div>
    <ol id="messagesList">
        <c:if test="${socialNetworksCount == 0 && messages.isEmpty()}">${msg.noMessagesHomeText}. <a href="<c:url value="/messages/refresh" />">${msg.refresh}?</a></c:if>
        <%@ include file="includes/messages.jsp" %>
    </ol>

    <!-- input type="button" id="moreButton" onclick="showMoreMessages(); " value="More messages" /-->
    <div id="loadingMore"><img src="${staticRoot}/images/ajax_loading.gif" alt="" title="" style="width: 16px; height: 16px;" /></div>
</div>
<%-- Set a cookie in the url shortener --%>
<img src="${appProperties['url.shortener.domain']}/shortener/userId/${loggedUser.id}" style="display: none;" alt="" />
<%@ include file="footer.jsp" %>