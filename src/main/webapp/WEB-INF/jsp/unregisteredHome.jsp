<%@ include file="includes.jsp" %>
<%@ page pageEncoding="UTF-8" %><% out.clear(); %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <%@ include file="head.jsp"%>
    <link rel="stylesheet" type="text/css"  href="${staticRoot}/styles/home.css" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <script type="text/javascript">
        $(document).ready(
            function() {
                $(":button").button();
                $(":submit").button();
                $("#signupLink").button();
                $("#loginLink").button();
            }
        );
    </script>
</head>
<body>
    <div id="alert"></div>
    <div id="homeWrapper">
        <div id="topPanel">
            <a href="${root}">
                <img src="${staticRoot}/images/logo_home.png" id="logoHome" alt="welshare" title="welshare" />
            </a>
        </div>
        <div id="unregisteredMain">
            <div id="introText"><span class="left">A great tool for</span> <span class="right"> social network power users</span></div>
            <div id="introTextSmall">
                Share smartly. Share easily. Share well.
            </div>

            <div id="loginSignupContainer">
                <div style="margin: auto; width: 384px;">
                <c:if test="${appProperties['signup.invite.only'] == false}">
                    <a href="signup" id="signupLink" class="homeLink">${msg.signup}</a>
                </c:if>
                <div id="loginLinkHolder">
                    <a href="login" id="loginLink" class="homeLink">${msg.login}</a>
                </div>
                </div>
            </div>
            <img src="${staticRoot}/images/shade.png" class="underShade" alt="" />
            <c:if test="${appProperties['signup.invite.only']}">
                <h2>Private alpha. There are still a lot of bugs and missing features</h2>
                ${msg.invitationIntroText }
                <form action="${root}account/requestInvitationCode" method="get">
                    <label for="requestInvitationEmail">${msg.email}:</label>
                    <input type="text" name="email" id="requestInvitationEmail" />
                    <input type="submit" value="${msg.requestInvitation}" />
                </form>
            </c:if>

            <%@ include file="includes/screenMessages.jsp" %>
            <img src="${staticRoot}/images/curved_arrow.png" style="float: left; margin: 0px; margin-top: 12px; margin-left: -5px; padding: 0px; padding-right: -5px;"/>
            <iframe width="560" height="315" src="http://www.youtube.com/embed/3_THRjfj9x8" frameborder="0" allowfullscreen style="margin-top: 9px; float: left"></iframe>

            <div class="homeItems">
                <div class="item">
                    <img class="itemIcon" src="${staticRoot}/images/home_lighthouse.png" alt="&raquo;" />
                    <h2>Spread your messages</h2>
                    <p>Share your thoughts and links on Welshare, Twitter and Facebook through a simple interface</p>
                </div>

                <div class="item">
                    <img class="itemIcon" src="${staticRoot}/images/home_reshare.png" alt="&raquo;" />
                    <h2>Unified reshare</h2>
                    <p>One "reshare" button for all messages, no matter where they come from. Pressing it means "I like this, and I want my friends to know about that"</p>
                </div>

                <div class="item">
                    <img class="itemIcon" src="${staticRoot}/images/home_timeline.png" alt="&raquo;" />
                    <h2>Don't miss a thing</h2>
                    <p>See which are the most important messages that you missed while being offline</p>
                </div>

                <div class="item">
                    <img class="itemIcon" src="${staticRoot}/images/home_notifications.png" alt="&raquo;" />
                    <h2>Get notifications</h2>
                    <p>See notifications from all social services you use - whether your messages were liked, retweeted or replied to</p>
                </div>

                <div class="item" style="border-right-style: none;">
                    <img class="itemIcon" src="${staticRoot}/images/home_medal.png" alt="&raquo;" />
                    <h2>Social reputation</h2>
                    <p>Measuring your reputation based on your activity on all supported social networks. Providing rankings based on city and country</p>
                </div>
            </div>

            <a id="featureTourLink" href="<c:url value="/info/features" />" class="homeExtraLink">Take a quick tour to learn more</a><br/><br />
            <a href="javascript:void(0);" onclick="$('#comparisonPanel').toggle();  $('html, body').animate({scrollTop: $('#comparisonPanel').offset().top}, 1300);" class="homeExtraLink">See how Welshare compares to similar services</a>
        </div>
    </div>
    <div id="comparisonPanel"><img src="${staticRoot}/images/auxiliary/feature_comparison.png" /></div>
    <%@ include file="includes/analytics.jsp" %>
</body>
</html>