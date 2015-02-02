<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="${msg.importantMessagesPageTitle}" />
<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" %>

<script type="text/javascript">
var initialized;
function showSettings() {
    $("#importantMessagesSettings").slideToggle(350, function() {
        if (!initialized) {
            $("#importantMessageScoreThreshold").spinner({min: 1, max: 10000});
            $("#importantMessageScoreThresholdRatio").spinner({min: 0, max: 10000});
            initialized = true;
        }
    });
}

function saveThresholdSettings() {
    var threshold = $("#importantMessageScoreThreshold").val();
    var thresholdRatio = $("#importantMessageScoreThresholdRatio").val();
    $.post(root + "settings/setImportantMessageThresholds",
        {threshold: threshold, thresholdRatio: thresholdRatio},
        function() {
          info("${msg.importantMessagesThresholdSet}");
        });
}
</script>
<c:set var="messages" value="${sessionScope.missedImportantMessages}" />

<c:choose>
    <c:when test="${!empty messages}">
        <c:out value="${msg.importantMessagesPageTitle} ${loggedUser.profile.importantMessageScoreThreshold}" />
    </c:when>
    <c:otherwise>
        ${msg.noMissedImportantMessages}
    </c:otherwise>
</c:choose>
[<a href="javascript:void(0);" onclick="showSettings()">${msg.configure}</a>]
<div id="importantMessagesSettings">
    <div class="thresholdHolder">
        <input type="text" size="4" id="importantMessageScoreThreshold" name="importantMessageScoreThreshold" class="thresholdInput" value="${loggedUser.profile.importantMessageScoreThreshold}" />
        <label for="importantMessageScoreThreshold">${msg.importantMessageScoreThreshold}</label>
    </div>
    <div class="thresholdHolder">
        <input type="text" size="4" id="importantMessageScoreThresholdRatio" name="importantMessageScoreThresholdRatio" class="thresholdInput" value="${loggedUser.profile.importantMessageScoreThresholdRatio}" />
        <label for="importantMessageScoreThresholdRatio">${msg.importantMessageScoreThresholdRatio}</label>
    </div>
    <input type="button" onclick="saveThresholdSettings()" value="${msg.save}" />
</div>

<div id="timeline">
    <ol id="messagesList">
        <%@ include file="includes/messages.jsp" %>
    </ol>
    <div id="loadingMore"><img src="${staticRoot}/images/ajax_loading.gif" alt="" title="" /></div>
</div>

<%@ include file="footer.jsp" %>