<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${selectedUser.username}" />
<%@ include file="includes.jsp" %>
<c:set var="extraHeaders">
    <link href="<c:url value="/${selectedUser.username}/rss" />" rel="alternate" type="application/rss+xml" title="">
</c:set>
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<script type="text/javascript">
$(document).ready(function() {
    $(window).scroll(function(){
        if ($(window).scrollTop() > $(document).height() - $(window).height() - 110){
            showMoreUserMessages("${selectedUser.id}");
        }
     });
});
</script>
<c:set var="user" value="${selectedUser}" scope="request" />
<c:set var="cssClass" value="userPagePicture" />
<c:set var="noExternal" value="true" />
<c:set var="pictureType" value="large" />

<c:if test="${selectedUser.id == loggedUser.id}">
    <div class="userPageTitle">${msg.userPageHint}</div>
</c:if>

<c:if test="${selectedUser != null}">
    <%@ include file="includes/profilePicture.jsp" %>
    <span class="userPageNames">${selectedUser.names}</span>
    <span class="userPageUsername">@${selectedUser.username}</span>

    <c:choose>
        <c:when test="${loggedUser != null}">
            <%@ include file="includes/followingButtons.jsp" %>
        </c:when>
        <c:otherwise>
            <!-- TODO suggest to register -->
        </c:otherwise>
    </c:choose>
</c:if>

<c:if test="${selectedUser == null}">
    <c:out value="${selectedUsername} ${msg.userNotRegisteredInvite}" />
</c:if>

<c:if test="${selectedUser != null}">
    <br />
    <c:if test="${!empty selectedUser.city or selectedUser.country != null}">
        <c:out value="${msg.location}: ${selectedUser.city}" />
        <c:if test="${!empty selectedUser.city and selectedUser.country != null}">, </c:if>
        <c:out value="${selectedUser.country.name}" />
    </c:if>

    <c:if test="${!empty selectedUser.originallyFrom and selectedUser.originallyFrom != selectedUser.city}">
        <c:out value=" (${msg.originallyFrom} ${selectedUser.originallyFrom})" />
    </c:if>
    <br />
    <c:if test="${!empty selectedUser.bio}">
        ${selectedUser.bio}
    </c:if>
    <c:if test="${!empty selectedUser.birthDate}">
        ${msg.birthDate}: <fmt:formatDate type="date" value="${selectedUser.birthDate.toDate()}" dateStyle="medium" />
    </c:if>

    <div style="clear: both; padding: 5px 0px;">
        <c:forEach items="${applicationScope.socialNetworks.values()}" var="sn">
            <c:set var="profileUrl" value="${profileUrls[sn.siteName]}" />
            <c:if test="${profileUrl != null}">
                <a href="${profileUrl}" target="_blank" style="margin-right: 17px;">
                    <img src="${staticRoot}/images/social/${sn.icon}" class="connectIcon" /> <c:out value="${sn.name} ${msg.profileLower}"/>
                </a>
            </c:if>
        </c:forEach>
    </div>
</c:if>

<c:if test="${selectedUser.id != loggedUser.id}">
    <c:set var="initialText" value="@${selectedUser.username} " />
</c:if>
<c:set var="shareOptionsOn" value="false" />

<c:if test="${loggedUser != null}">
    <%@ include file="includes/shareBox.jsp" %>
</c:if>

<div id="timeline">
    <ol id="messagesList">
        <%@ include file="includes/messages.jsp" %>
    </ol>
    <div id="loadingMore"><img src="${staticRoot}/images/ajax_loading.gif" alt="" title="" /></div>
</div>
<%@ include file="footer.jsp" %>