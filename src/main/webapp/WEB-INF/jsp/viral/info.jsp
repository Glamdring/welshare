<%@ include file="../header.jsp"%>
<%@ page pageEncoding="UTF-8" %>
<h4><c:out value="${msg.viralInfoAbout} " /><a href="${shortUrl}">${url.key}</a></h4>
<div>
    <table>
    <tr>
        <td>${msg.originalUrl}</td>
        <td><strong><a href="${url.longUrl}">${url.longUrl}</a></strong></td>
    </tr>
    <tr>
        <td>${msg.viralPoints}</td>
        <td><strong>${url.viralPoints}</strong></td>
    </tr>
    <tr>
        <td>${msg.viralPointsPercentage}</td>
        <td><strong><fmt:formatNumber value="${url.viralPointsPercentage}" pattern="#.##" /></strong></td>
    </tr>
    <tr>
        <td>${msg.nodesFromBeginning}</td>
        <td><strong>${url.nodesFromBeginning}</strong></td>
    </tr>
    <tr>
        <td>${msg.averageSubgraphDepth}</td>
        <td><strong>${url.averageSubgraphDepth}</strong></td>
    </tr>
    </table>
</div>

<%@ include file="../footer.jsp"%>
