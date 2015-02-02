<%@ page pageEncoding="UTF-8" %>

<script type="text/javascript">
$(document).ready(function() {
    $(".thresholdInput").spinner({min: 0, max: 10000});
    $(".likesThresholdInfo").poshytip({className: 'blackTooltip', alignTo: 'target'});
});
</script>
<div class="likesThresholdHolder">
    <label for="likesThreshold-${user.externalId}" class="likesThresholdInfo"
        title="${msg.likesThresholdInfo}">${msg.likesThreshold}:</label>
    <input type="text" size="4" id="likesThreshold-${user.externalId}" name="likesThreshold" class="thresholdInput" value="${user.likesThreshold}" />

    <input type="checkbox" value="true"<c:if test="${user.hideReplies}"> checked="checked"</c:if> id="hideReplies-${user.externalId}" name="hideReplies" />
    <label for="hideReplies-${user.externalId}">${msg.hideReplies}</label>

    <input type="button" onclick="setLikesThreshold('${user.externalId}');" value="${msg.set}" />
</div>