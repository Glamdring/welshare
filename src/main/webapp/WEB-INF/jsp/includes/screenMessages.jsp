<%@ page pageEncoding="UTF-8" %>
<div style="clear:both;">
<c:forEach items="${screenMessages}" var="screenMsg">
    <c:choose>
        <c:when test="${screenMsg.error}">
           <div class="error">${msg[screenMsg.key]}</div>
        </c:when>
        <c:otherwise>
           <div class="screenMessage">${msg[screenMsg.key]}</div>
        </c:otherwise>
    </c:choose>
</c:forEach>
</div>