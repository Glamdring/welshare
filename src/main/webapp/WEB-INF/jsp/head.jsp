<%@ taglib prefix="w" uri="http://welshare.com/tags" %>
<%@ page errorPage="/WEB-INF/jsp/error/redirector.jsp" %>

<w:addResource resourcePath="${staticRoot}/js/jquery-1.7.1.min.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/simpledateformat.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.timeago.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.caret.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.corner.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.scrollTo-min.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/shareBoxAutocomplete.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/userAutocomplete.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.poshytip.min.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.elastic.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.ba-outside-events.min.js" contentType="text/javascript" />
<w:addResource resourcePath="${pageContext.request.secure ? 'https' : 'http'}://www.google.com/jsapi" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/info.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/scripts.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/fcbklistselection.min.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.validate.min.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery-fonteffect-1.0.0.min.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/fileuploader.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery-ui-1.8.16.min.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.labelify.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.fcbkcomplete.min.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/ui.spinner.min.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/toggleCheckbox.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.pnotify.min.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery.emoticons.js" contentType="text/javascript" />
<w:addResource resourcePath="${staticRoot}/js/jquery-ui-timepicker.js" contentType="text/javascript" />

<c:if test="${!loggedUser.viewedStartingHints}">
    <w:addResource resourcePath="${staticRoot}/js/jquery.callout.min.js" contentType="text/javascript" />
</c:if>

<w:flushResources contentType="text/javascript" assetsVersion="${appProperties['assets.version']}"/>

<w:addResource resourcePath="${staticRoot}/styles/jquery-ui.css" contentType="text/css" />
<w:addResource resourcePath="${staticRoot}/styles/main.css" contentType="text/css" />
<w:addResource resourcePath="${staticRoot}/styles/fileuploader.css" contentType="text/css" />
<w:addResource resourcePath="${staticRoot}/styles/fcbklistselection.css" contentType="text/css" />
<w:addResource resourcePath="${staticRoot}/styles/fcbkcomplete.css" contentType="text/css" />
<w:addResource resourcePath="${staticRoot}/styles/jquery.pnotify.default.css" contentType="text/css" />

<w:flushResources contentType="text/css" assetsVersion="${appProperties['assets.version']}" />

<script type="text/javascript">
var socialNetworkSettings = new Array();
<c:if test="${loggedUser != null}">
<c:set var="socialNetworksCount" value="0" />
    <%-- preconfigured in ContextListener --%>
    <c:forEach items="${applicationScope.socialNetworks.values()}" var="sn">
        <c:if test="${sn.sharingEnabled}">
            <c:set var="settingsField" value="${sn.siteName}Settings" />
            <c:set var="currentUserSettings" value="${loggedUser[settingsField]}" />
            var ${sn.siteName} = {
                enabled: ${currentUserSettings.shareLikes},
                active: ${currentUserSettings.active},
                name: '${sn.name}',
                siteName: '${sn.siteName}',
                prefix: '${sn.prefix}',
                icon: '${sn.icon}',
                likeAndReshare: ${sn.likeAndReshare}
            };
            socialNetworkSettings.push(${sn.siteName});
            <c:if test="${currentUserSettings.fetchMessages}">
                <c:set var="socialNetworksCount" value="${socialNetworksCount + 1}" />
            </c:if>
        </c:if>
    </c:forEach>
</c:if>

var config = {
    root : "${root}",
    staticRoot: "${staticRoot}",
    language: "${loggedUser != null ? loggedUser.profile.translateLanguage != null ? loggedUser.profile.translateLanguage : loggedUser.profile.language.code : 'en'}",
    userLogged: ${loggedUser != null},
    currentUsername: "${loggedUser != null ? loggedUser.username : ''}",
    externalUsernames: ["${fn:join(sessionScope.externalUsernames, ',')}"],
    externalNetworksActive: ${loggedUser != null
        && (loggedUser.twitterSettings.fetchMessages
               || loggedUser.facebookSettings.fetchMessages) ? 'true' : 'false'},
    socialNetworkSettings: socialNetworkSettings,
    emoticonsEnabled: ${loggedUser.profile.emoticonsEnabled ? 'true' : 'false'},
    pollForNotifications: ${loggedUser != null && selectedUSer == null},
    messages : {
        reply : "${msg.reply}",
        follow : "${msg.follow}",
        loading : "${msg.loading}",
        like : "${msg.like}",
        reshare : "${msg.reshare}",
        unlike : "${msg.unlike}",
        favourite : "${msg.favourite}",
        unfavourite : "${msg.unfavourite}",
        addedToFavourites: "${msg.addedToFavourites}",
        confirmDeleteExternalCheckbox: "${msg.confirmDeleteExternalCheckbox}",
        confirmDelete : "${msg.confirmDelete}",
        likeSuccess : "${msg.likeSuccess}",
        reshareSuccess : "${msg.reshareSuccess}",
        createMessageFilterSuccess : "${msg.createMessageFilterSuccess}",
        deleteMessageFilterSuccess : "${msg.deleteMessageFilterSuccess}",
        edit : "${msg.editMessage}",
        save : "${msg.save}",
        translate: "${msg.translate}"
    }
};
init(config);
</script>

<link rel="shortcut icon" href="${staticRoot}/images/favicon.png" />
${extraHeaders}
<meta http-equiv="X-UA-Compatible" content="edge" />
<meta name="description" content="Share to Twitter, Facebook, LinkedIn Google+ with a single click. Schedule messages for the future. Social reputation. Aggregates social streams. Social interaction statistics." />
<meta name="keywords" content="social,twitter,facebook,linkedin,share,like,retweet,schedule,reputation" />
<title>Welshare <c:if test="${!empty title}"> - ${title}</c:if></title>