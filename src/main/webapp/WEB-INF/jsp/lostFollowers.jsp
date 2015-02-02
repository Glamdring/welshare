<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.favourites}" />
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<script type="text/javascript">
$(document).ready(function() {
    $("#lostFollowers .linkedImage").poshytip({
        className: 'blackTooltip',
        showTimeout: 0,
        alignTo: 'target',
        alignX: 'center',
        alignY: 'bottom',
        offsetY: 5,
        allowTipHover: true,
        fade: false,
        slide: false
    });
});
</script>
<h2>${msg.lostFollowersTitle}</h2>
<c:if test="${lostFollowers.isEmpty()}">
    <h3>${msg.noLostFollowersData}</h3>
</c:if>
<div id="lostFollowers">
<c:forEach items="${lostFollowers}" var="entry">
    <c:if test="${!entry.value.isEmpty()}">
        <c:out value="${msg.lostFollowers} ${applicationScope.socialNetworksByPrefix[entry.key].name}:" /><br />
        <c:forEach items="${entry.value}" var="user">
            <%@ include file="includes/profilePicture.jsp" %>
        </c:forEach>
    </c:if>
    <c:if test="${entry.value.isEmpty()}">
        <c:out value="${msg.noLostFollowers} ${applicationScope.socialNetworks[entry.key].name}" />
    </c:if>
</c:forEach>
</div>
<%@ include file="footer.jsp" %>