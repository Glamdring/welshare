<%@ page pageEncoding="UTF-8"%>
<div id="reshareDialog">
    <span style="float: left; margin-top: 7px; margin-right: 5px;">${msg.spreadTo}:</span>
    <%-- preconfigured in ContextListener --%>
    <c:forEach items="${applicationScope.socialNetworks.values()}" var="sn">
        <c:if test="${sn.sharingEnabled}">
            <c:set var="settingsField" value="${sn.siteName}Settings" />
            <c:set var="currentUserSettings" value="${loggedUser[settingsField]}" />
            <c:if test="${currentUserSettings.fetchMessages == true}">
                <c:set var="optionTooltip" value="sendTo${sn.siteName}Tooltip"/>
                <div class="reshareOption" title="${msg[optionTooltip]}">
                    <input type="checkbox" <c:if test="${shareOptionsOn && currentUserSettings.active}">checked="checked" </c:if>name="externalSites" value="${sn.prefix}" id="${sn.siteName}ReshareOption" />
                    <img src="${staticRoot}/images/social/${sn.icon}" alt="${sn.siteName}" />
                </div>
            </c:if>
        </c:if>
    </c:forEach>
    <div class="reshareOption"><input type="checkbox" name="sitesToShareLikeTo" value="ws" id="internalReshareOption" checked="checked" /> <label for="internalOptionLike"><img src="${staticRoot}/images/auxiliary/icon_big.png" /></label></div>

    <div id="shareAndLikeHolder" style="clear: both;">
        <input type="checkbox" name="shareAndLike" id="shareAndLike" />
        <label title="${msg.shareAndLike}" for="shareAndLike" style="font-size: 0.8em;">${msg.shareAndLike}</label>
    </div>

    <div style="clear: both; margin-top: 4px;"></div>
    <div>${msg.addComment}:</div><input type="text" id="reshareComment" name="reshareComment" style="width: 250px;"/><br />
    <div>${msg.editOriginalMessage}:</div><textarea id="editedLikedMessage" name="editedLikedMessage" style="width: 250px;" rows="5"></textarea><br />
    <input type="button" value="${msg.send}" onclick="reshare(currentLikedId, true);" style="float: right;"/>
</div>