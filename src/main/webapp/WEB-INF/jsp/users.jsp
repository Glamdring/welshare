<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<script type="text/javascript">
<%@ include file="includes/countries.jsp" %>

$(document).ready(function() {
    $("#country").autocomplete({source:countries, minLength: 3});
    <c:if test="${metadataIncluded}">
        $(".thresholdInput").spinner({min: 0, max: 10000});
        $(".likesThresholdInfo").poshytip({className: 'blackTooltip', alignTo: 'target'});
    </c:if>
});

function searchByCountry() {
    window.location.href = root + 'users/top/country/' + $("#country").val();
}
function searchByCity() {
    window.location.href = root + 'users/top/city/' + $("#city").val();
}
</script>

<%@ include file="includes/screenMessages.jsp" %>

<div id="usersList" class="mainContent" style="background-color: white;">
    <c:if test="${topUsers}">
        <h2>${msg.topUsers}
        <c:choose>
            <c:when test="${!empty country}">
                <c:out value=" ${msg.topUsersIn} ${country}" />
            </c:when>
            <c:when test="${!empty city}">
                <c:out value=" ${msg.topUsersIn} ${city}" />
            </c:when>
            <c:otherwise>
                <c:out value=" ${msg.topUsersWorldwide}" />
            </c:otherwise>
        </c:choose>
        </h2>
    </c:if>
    <c:if test="${!topUsers}">
        <h2>${msg[title]}</h2>
    </c:if>

    <%-- links to followers/following on external sites --%>
    <c:if test="${title == 'followers' || title == 'following'}">
        <c:if test="${title == 'followers'}">
            ${msg.followersDescription}:
        </c:if>
        <c:if test="${title == 'following'}">
            ${msg.followingDescription}:
        </c:if>
        <br />
        <c:forEach items="${applicationScope.socialNetworks.values()}" var="sn">
            <c:set var="settingsField" value="${sn.siteName}Settings" />
            <c:set var="currentUserSettings" value="${loggedUser[settingsField]}" />
            <c:if test="${currentUserSettings.fetchMessages == true}">
                <c:if test="${title == 'followers'}">
                    <c:set var="externalHref" value="${sn.followersUrl}" />
                </c:if>
                <c:if test="${title == 'following'}">
                    <c:set var="externalHref" value="${sn.followingUrl}" />
                </c:if>
                <a href="${externalHref}" target="_blank" style="margin-right: 17px;">
                    <img src="${staticRoot}/images/social/${sn.icon}" class="connectIcon" /> ${sn.name}
                </a>
            </c:if>
        </c:forEach>
        <br /><hr />
    </c:if>
    <c:set var="noExternal" value="true" />
    <c:set var="cssClass" value="userPagePicture" />
    <c:set var="pictureType" value="large" />

    <!-- TODO select all text on focus -->
    <c:if test="${topUsers}">
        <div style="float: left;">
            ${msg.country}:<input type="text" name="country" id="country" value="${loggedUser.profile.country.name}" /><input type="button" onclick="searchByCountry();" value="${msg.search}" />
        </div>
        <div style="float: right">
            ${msg.city}:<input type="text" name="city" id="city" value="${loggedUser.profile.city}" /><input type="button" onclick="searchByCity();" value="${msg.search}" />
        </div>
        <div style="clear: both;"></div>
        <hr />
    </c:if>

    <c:if test="${showLimitedExternalFollowing}">
        <div style="float: right;">
            <a href="<c:url value="/users/limited" />">${msg.limitedExternalFollowing}</a>
        </div>
    </c:if>

    <c:forEach items="${users}" var="user">
        <div class="userEntry">
            <%@ include file="includes/profilePicture.jsp" %>
            <a href="${root}${user.username}"><span class="userPageNames">${user.names}</span> <span class="userPageUsername">(@${user.username})</span></a>
            <div>
                <span style="font-size: 0.8em; margin-right: 15px;"><a href="<c:url value="/${user.username}/socialReputation" />">${msg.score}: <fmt:formatNumber value="${user.score + user.externalScore}" groupingUsed="true" /></a></span>
                <span style="font-size: 0.8em">${msg.messageCount}: <fmt:formatNumber value="${user.messages}" groupingUsed="true" /></span><br /><br />
            </div>
            <%@ include file="includes/followingButtons.jsp" %>
        </div>
    </c:forEach>
    <c:if test="${users.isEmpty()}">
        <div>${msg.noResults}</div>
    </c:if>
</div>
<%@ include file="footer.jsp" %>