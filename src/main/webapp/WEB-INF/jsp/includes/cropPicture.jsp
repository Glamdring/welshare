<%@ include file="../includes.jsp"%>
<%@ page pageEncoding="UTF-8"%>

<script type="text/javascript">
var imageWidth;
var imageHeight;
var api;
var initialized = false;

function initJcrop() {
    if (!initialized) {
        imageWidth = $("#cropbox").width();
        imageHeight = $("#cropbox").height();

        var initialSize;
        var initialX;
        var initialY;
        if (imageWidth > imageHeight) {
            initialSize = imageHeight;
            initialX = (imageWidth - initialSize) / 2;
            initialY = 0;
        } else {
            initialSize = imageWidth;
            initialX = 0;
            initialY = (imageHeight - initialSize) / 2;
        }

        api = $.Jcrop('#cropbox', {
            onChange: showPreview,
            onSelect: showPreview,
            aspectRatio: 1,
            setSelect: [ initialX, initialY, initialSize, initialSize ]
        });
        initialized = true;
    }
}
function showPreview(coords) {
    if (parseInt(coords.w) > 0) {
        var rx = 72 / coords.w;
        var ry = 72 / coords.h;

        $('#preview').css({
            width: Math.round(rx * imageWidth) + 'px',
            height: Math.round(ry * imageHeight) + 'px',
            marginLeft: '-' + Math.round(rx * coords.x) + 'px',
            marginTop: '-' + Math.round(ry * coords.y) + 'px'
        });
    }
}

function saveCropCoordinates() {
    var coords = api.tellSelect();
    $.post(root + 'settings/account/picture/crop', {x : coords.x, y : coords.y, size: coords.w}, function(data) {
        window.location.href = root + 'settings/account';
    });
}
</script>

<div id="pictureCropPanel" style="display: none;" align="center">
    <div style="width: 72px; height: 72px; overflow: hidden; margin-bottom: 5px;">
        <img src="${user.profilePictureURI}" id="preview" />
    </div>
    <img src="${user.profilePictureURI}" id="cropbox" />
    <br />
    <input type="button" onclick="saveCropCoordinates()" value="${msg.save}" />
</div>