<%@ page pageEncoding="UTF-8" %>
<%@ include file="../includes.jsp" %>

<% out.clear(); %>
<div class="likersHolder">
    <c:forEach items="${likers}" var="liker">
        <c:choose>
            <c:when test="${!liker.external}">
                <c:url value="${root}${liker.username}" var="userUrl" />
            </c:when>
            <c:otherwise>
                <c:url value="${root}user/external/${liker.externalId}" var="userUrl" />
            </c:otherwise>
        </c:choose>

        <a href="${userUrl}" target="_blank">
        <c:if test="${liker.username != null}">
            ${liker.username} -
        </c:if>
        ${liker.names}</a><br />
    </c:forEach>
</div>
<c:if test="${fn:length(likers) == 0}">${msg.noLikers}</c:if>