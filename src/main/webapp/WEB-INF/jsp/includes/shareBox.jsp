<%@ page pageEncoding="UTF-8"%>

<script type="text/javascript">
var currentPictures = new Array();
$(document).ready(function() {
    var shareBox = $("#message");

    initTagAutocomplete();
    //initShareboxUserAutocomplete(); disabled for now
    initCharCounter([$("#shareButton")], shareBox, $("#charsRemaining"));
    $(".shareOption").each(function() {
        $(this).toggleCheckbox();
    });

    // ugly..
    var shorteningExtrasHtml =
'<div id="shorteningExtras">' +
    '<button value="" onclick="shortenUrls();" class="shortenButton extraShortenButton" style="width: 180px;" title="${msg.shortenUrls}">' +
        '<img src="${staticRoot}/images/shortenLinks.png" alt="${msg.shortenUrls}" />${msg.shortenUrls}' +
    '</button><br />' +
    '<button value="" onclick="shortenUrlsWithTopBar();" class="shortenButton extraShortenButton" style="width: 180px;" title="${msg.withTopBar}">' +
        '<img src="${staticRoot}/images/shortenLinks.png" alt="${msg.withTopBar}" />${msg.withTopBar}' +
    '</button><br />' +
    '<button value="" onclick="shortenViralUrls();" class="shortenButton extraShortenButton" style="width: 180px;" title="${msg.viralLinks}">' +
        '<img src="${staticRoot}/images/shortenLinks.png" alt="${msg.viralLinks}"/>${msg.viralLinks}' +
    '</button><br />' +
'</div>';

    $("#shortenButton").next().button({text : false, icons: {primary:'ui-icon-carat-1-s'}})
    .mouseout(function() {$(this).addClass("ui-state-hover");}).parent().buttonset();

    $("#shortenButtonExtras").poshytip({
        content: function(updateCallback) {
            return shorteningExtrasHtml;
        },
        alignTo: 'target',
        alignX: 'center',
        alignY: 'bottom',
        offsetY: 1,
        allowTipHover: true,
        fade: false,
        slide: false,
        className: 'blackTooltip'
   });

    var uploader = new qq.FileUploader({
        // pass the dom node (ex. $(selector)[0] for jQuery users)
        element: $('#uploadPicture')[0],
        // path to server-side upload script
        action: '${root}share/upload',
        label: '<img src="${staticRoot}/images/camera_add.png" style="width: 24px; height: 24px;" title="${msg.pictureUpload}" alt="" />',
        title: '${msg.pictureUpload}',
        allowedExtensions: ['png', 'jpg', 'jpeg', 'gif'],
        onComplete: function(id, fileName, responseJSON){
            currentPictures.push(responseJSON);
            $("#uploadedImages").append('<img src="' + responseJSON + '" alt="${msg.upload}"/>');
            $("#shareButton").removeAttr("disabled");
            $("#shareButton").css("opacity", "1");
        }
    });
    //$("#uploadPicture .qq-upload-button").button();

    $("#messageScheduler").datetimepicker({
        stepMinute: 5,
        format: "yyyy-MM-dd'T'HH:mm:ss.SSSZZ",
        minDate: new Date(),
        onSelect: function() {
            $("#scheduleClearButton").show();
            $("#shareButton").val("Schedule");
        },
        showOn: "button",
        buttonImage: "${staticRoot}/images/calendar.png",
        buttonText: "${msg.scheduleMessage}",
        buttonImageOnly: "true",
        firstDay: "${w:getFirstDayOfWeek()}"
    });


    shareBox.focus();
    shareBox.selectRange(shareBox.val().length, shareBox.val().length);

    shareBox.keydown(function (e) {
         if (e.ctrlKey && e.keyCode == 13) {
           $("#shareButton").click();
         }
     });
});
</script>


<textarea tabindex="1" autocomplete="off" rows="2" cols="38"
    id="message" name="message"><c:out value="${initialText}" /></textarea>

<span id="charsRemaining" title="${msg.charsRemaining}">300</span>
<span id="charsRemainingNote"></span>

<input type="button" value="${msg.share}" onclick="share(currentPictures); clearPictures();" id="shareButton" />

<c:if test="${loggedUser != null}">
    <div style="float: right;">
    <%-- preconfigured in ContextListener --%>
    <c:forEach items="${applicationScope.socialNetworks.values()}" var="sn">
        <c:if test="${sn.sharingEnabled}">
            <c:set var="settingsField" value="${sn.siteName}Settings" />
            <c:set var="currentUserSettings" value="${loggedUser[settingsField]}" />

            <c:if test="${currentUserSettings.fetchMessages == true}">
                <c:set var="optionTooltip" value="sendTo${sn.siteName}Tooltip"/>
                <div class="shareOption" title="${msg[optionTooltip]}">
                    <input type="checkbox" <c:if test="${shareOptionsOn && currentUserSettings.active}">checked="checked" </c:if>name="externalSites" value="${sn.prefix}" id="${sn.siteName}Option" />
                    <img src="${staticRoot}/images/social/${sn.icon}" alt="${sn.siteName}" />
                </div>
            </c:if>
        </c:if>
    </c:forEach>
    </div>
</c:if>

<div style="float: right; width: 55px;">
    <button value="" onclick="shortenUrls();" style="float: left; margin-top: 0px; padding-top: 4px;" class="shortenButton" id="shortenButton" title="${msg.shortenUrls}">
        <img src="${staticRoot}/images/shortenLinks.png" />
    </button>
    <button id="shortenButtonExtras" class="shortenButton ui-state-hover" style="float:left; width: 12px; margin: 0px; padding: 0px;">
    </button>
</div>

<div style="float: right; margin-right: 12px;" class="shareBoxButton">
   <input type="hidden" style="width: 24px; height: 24px;" id="messageScheduler" onclick="$(this).datetimepicker('setDate', new Date());" />
   <a href="javascript:void(0);" onclick="suggestTimeToShare();" title="${msg.bestTimeToShareTitle}">?</a>
</div>
<span class="closeLink" style="float: right; display: none; margin-right: 1px;" id="scheduleClearButton" onclick="$('#messageScheduler').datetimepicker('setDate', null); $('#shareButton').val('${msg.share}'); $(this).hide();" title="${msg.clearMessageSchedule}">×</span>

<!-- img src="${staticRoot}/images/eye.png" alt="${msg.visibilityOptions}" title="${msg.visibilityOptions}" /-->
<div id="uploadPicture">
    <noscript>
        <input type="file" name="pictureUpload" />
    </noscript>
</div>
<ul class="qq-upload-list"></ul>

<div style="clear: both;" id="uploadedImages"></div>