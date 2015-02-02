<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${!interestedInKeywords.isEmpty()}">${msg.interestedIn}:</c:if>
<c:forEach var="interestedIn" items="${interestedInKeywords}">
<a rel="tag" id="interestedInKeyword-${interestedIn.id}" title="${msg.higlightingMessagesContaining} '${interestedIn.keywords}'" class="interestedInKeyword" href="javascript:void(0);">${interestedIn.keywords}
<span title="${msg.delete}" onmouseout="$(this).attr('class', 'deleteInterestedInKeyword')" onmouseover="$(this).attr('class', 'deleteInterestedInKeywordHover')" onclick="deleteInterestedInKeyword(${interestedIn.id});" class="deleteInterestedInKeyword">×</span></a>

</c:forEach>