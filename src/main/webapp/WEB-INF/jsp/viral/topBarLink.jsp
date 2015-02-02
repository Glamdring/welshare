<%@ include file="../includes.jsp" %>
<%@ page pageEncoding="UTF-8" %><% out.clear(); %>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
    <head>
        <w:addResource resourcePath="${staticRoot}/js/jquery-1.4.4.min.js" contentType="text/javascript" />
        <w:addResource resourcePath="${staticRoot}/js/simpledateformat.js" contentType="text/javascript" />
        <w:addResource resourcePath="${staticRoot}/js/jquery.timeago.js" contentType="text/javascript" />
        <w:addResource resourcePath="${staticRoot}/js/jquery.poshytip.min.js" contentType="text/javascript" />
        <w:flushResources contentType="text/javascript" assetsVersion="${appProperties['assets.version']}"/>

        <link rel="stylesheet" type="text/css" href="${staticRoot}/styles/topBarLink.css" />

        <title>${url.longUrl}</title>
        <meta name="title" content="${url.longUrl}">
    </head>
<body>
    <div id="topBar">
        <img src="${staticRoot}/images/logo.png" style="width: 123px; height: 25px; float: left;" />

        <c:if test="${url.trackViral}">
            <div id="viralInfo">
                ${msg.viralPoints}: ${url.viralPoints} <img src="${staticRoot}/images/info.png" class="infoIcon" id="viralPointsIcon" title="${msg.viralPointsInfo}" /> <!-- span onclick="viewGraph('${url.key}')">[view graph]</span-->
                ${msg.viralPointsPercentage}: <fmt:formatNumber value="${url.viralPointsPercentage}" pattern="#.##" /><img src="${staticRoot}/images/info.png" class="infoIcon" id="percentageIcon" title="${msg.viralPointsPercentageInfo}" />

                ${msg.viralKeyLabel}: <a href="${appProperties['base.url']}/viral/info/${url.key}" style="font-size: 1.2em;" target="_blank">${url.key}</a></span>
            </div>
        </c:if>

        <div id="closeTopBar">
            <a href="${url.longUrl}"><img
                src="${staticRoot}/images/close.png" style="border: 0"
                title="${msg.closeTopBar}" alt="${msg.closeTopBar}" />
            </a>
        </div>

        <div id="sharePanel">
            <div>${msg.shareOn}:</div>
            <a href="http://twitter.com/?status=${shortUrl}" target="_blank"><img src="${staticRoot}/images/social/twitter_connect.png" style="width: 24px; height: 24px;" /></a>
            <a href="http://www.facebook.com/sharer.php?u=${shortUrl}" target="_blank"><img src="${staticRoot}/images/social/facebook_connect.png"/></a>
            <a href="${appProperties['base.url']}/?message=${shortUrl}" target="_blank"><img src="${staticRoot}/images/auxiliary/icon_big.png" style="width: 24px; height: 24px;"/></a>
        </div>
    </div>
    <script type="text/javascript">
        $(document).ready(function() {
            $("#viralPointsIcon").poshytip({className: 'blackTooltip', alignTo: 'cursor', showTimeout: 0, slide: false, fade: false});
            $("#percentageIcon").poshytip({className: 'blackTooltip', alignTo: 'curosr', showTimeout: 0, slide: false, fade: false});
        });
    </script>
    <iframe frameborder="0"  src="${url.longUrl}" name="wsMain" id="wsMain" noresize="noresize"></iframe>
</body>
</html>