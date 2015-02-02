<%@ page pageEncoding="UTF-8" %>
</div> <!-- end of main -->
<div id="side">
<c:if test="${loggedUser != null || selectedUser != null}">
    <%@ include file="side.jsp" %>
</c:if>

<c:if test="${loggedUser == null && selectedUser == null}">
    <%@ include file="unregisteredSide.jsp" %>
</c:if>

<br /><hr class="thin" />
<span style="font-size: 0.8em;">${msg.likeWelshare}</span>
<a href="https://twitter.com/intent/tweet?text=${msg.likeWelshareMessage}&amp;via=welshare" target="_blank"><img src="${staticRoot}/images/social/twitter_connect.png" style="width: 16px; height: 16px;" class="linkedImage" alt="Twitter" /></a>
<a href="http://www.facebook.com/sharer.php?u=http://welshare.com" target="_blank"><img src="${staticRoot}/images/social/facebook_connect.png" style="width: 16px; height: 16px;" class="linkedImage" alt="facebook" /></a>

<div id="aboutPanelHover">
    <div id="aboutPanelHoverIcon">&nbsp;</div>
    <div id="aboutPanel">
        <a href="<c:url value="http://blog.welshare.com" />" target="_blank">${msg.footerBlog}</a> |
        <a href="<c:url value="/info/about" />">${msg.footerAboutUs }</a> |
        <a href="<c:url value="/info/contact" />">${msg.footerContactUs}</a><br />
        <a href="<c:url value="/info/license" />">${msg.footerLicense}</a> |
        <a href="<c:url value="/info/tos" />">${msg.footerTos}</a> |
        <a href="<c:url value="/info/privacy" />">${msg.footerPrivacy}</a><br />
        <a href="<c:url value="/info/reshare" />">${msg.footerReshareButton}</a> |
        <a href="<c:url value="/info/features" />">${msg.footerFeatures}</a>
    </div>
</div>

</div> <!-- end of side -->
</div> <!-- end of main wrapper -->

</div> <!-- end of wrapper -->

<%@ include file="includes/analytics.jsp" %>

<noscript>
    <div id="noscript-warning">
        ${msg.noscriptWarning}
    </div>
</noscript>
</body>
</html>