<%@ page pageEncoding="UTF-8"%>
<%@ include file="../includes.jsp"%>
<% out.clear(); %>
<ul id="friendSuggestions">
    <c:forEach items="${suggestions}" var="user">
        <%@ include file="../includes/profilePictureNoLink.jsp"%>
        <li><img src="${src}" class="mediumPicture" /><c:out value="${user.names} (${user.username})" />
        <input type="hidden" name="selectedFriend" value="${user.id}" />
        </li>
    </c:forEach>
</ul>
<input type="button" value="${msg.followSelected}" onclick="followMultiple()"/>