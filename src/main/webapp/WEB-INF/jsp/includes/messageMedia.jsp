<%@ page pageEncoding="UTF-8"%>
<%@ taglib prefix="w" uri="http://welshare.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:if test="${!hideMedia && message.data.videoData == null && message.pictureCount > 0}">
    <c:forEach items="${message.pictures}" var="pic">
        <c:choose>
            <c:when test="${!pic.external}">
                <a href="${pic.publicUrl}" target="_blank"> <img itemprop="image"
                    src="${w:addSuffix(pic.path, '_small')}" alt="" class="linkedImage streamPicture" />
                </a>
            </c:when>
            <c:otherwise>
                <a href="${pic.externalUrl}" target="_blank"> <img itemprop="image"
                    src="${pic.path}" alt="" class="linkedImage streamPicture" />
                </a>
            </c:otherwise>
        </c:choose>
    </c:forEach>
</c:if>

<c:if test="${message.data.videoData != null}">
    <div class="videoData">
        <img src="${staticRoot}/images/play.png" style="position: absolute; top: 25px; left: 48px;" onclick="$(this).next().click()">
        <img src="${message.data.videoData.picture}"
            style="width: 130px; height: 97px;" alt=""
            onclick="$(this).parent().html('${fn:replace(message.data.videoData.embedCode, '"', '&quot;')}')"/>
    </div>
</c:if>