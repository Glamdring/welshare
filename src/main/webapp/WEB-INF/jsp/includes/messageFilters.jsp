<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${!messageFilters.isEmpty()}">${msg.filtered}:</c:if>
<c:forEach var="filter" items="${messageFilters}">
<a rel="tag" id="messageFilter-${filter.id}" title="${msg.notShowingMessagesContaining} '${filter.filterText}'" class="messageFilter" href="javascript:void(0);">${filter.filterText}
<span title="${msg.delete}" onmouseout="$(this).attr('class', 'deleteFilter')" onmouseover="$(this).attr('class', 'deleteFilterHover')" onclick="deleteMessageFilter(${filter.id});" class="deleteFilter">×</span></a>

</c:forEach>