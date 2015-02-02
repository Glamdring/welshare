(function($){
    $.fn.userAutocomplete = function(url) {
        userTargetUrl = url;
        userTargetId = $(this).attr("id");
        var suggestionsDiv = '<div id="' + HOLDER_DIV_ID + '"></div>';
        $("body").append(suggestionsDiv);
        var suggestions = $("#" + HOLDER_DIV_ID);
        var target = $(this);

        var offset = {top: target.offset().top + target.height() + 5,
                      left: target.offset().left};

        suggestions.offset(offset);

        // blurring causes the div to hide even before the click is dispatched
        // to the links inside, hence it's needed to disable the hiding in that
        // case
        suggestions.bind("mousedown", function() {
            shouldHide = false;
        });

        $(this).bind("keyup", function() {userLookup();});
        $(this).bind("focus", function() {
            userLookup();
            shouldHide = true;
        });
        $(this).bind("keydown", function(event){return handleUserArrows(event);});

        $(this).bind("blur", function(event) {
            if (shouldHide) {
                suggestions.hide();
            }
            userAutocompleting = false;
        });
    };
})(jQuery);

var shouldHide = true;
var HOLDER_DIV_ID = "divAutoCompleteSearch";
var userAutocompleting = false;
var userCurrentIdx = -1;
var dataSize;
var urlList;
var userTargetUrl;
var userTargetId;

function handleUserArrows(event) {
    if (!userAutocompleting) {
        return true;
    }

    var code = (event.keyCode ? event.keyCode : event.which);

    if (code == 40) { //down arrow
        userCurrentIdx++;
        if (userCurrentIdx == dataSize) {
            userCurrentIdx = 0;
        }
        changeCurrentUserSelection(userCurrentIdx);
        return false;
    }
    if (code == 38) { //up arrow
        userCurrentIdx--;
        if (userCurrentIdx == -1) {
            userCurrentIdx = dataSize - 1;
        }
        changeCurrentUserSelection(userCurrentIdx);
        return false;
    }

    if (code == 13) {
        window.location.href = urlList.eq(userCurrentIdx).val();
        return false;
    }
}

function userLookup() {
    var target = $("#" + userTargetId);

    var currentPosition = target.caret();
    var text = target.val();

    if(text.length < 2) {
        // Hide the suggestion box.
        $('#' + HOLDER_DIV_ID).hide();
        userAutocompleting = false;
        return;
    } else {

        $.get(userTargetUrl, {keywords: text}, function(data){
            if(data != null && data.length > 0) {
                var html = data;

                if (!userAutocompleting) {
                    $('#' + HOLDER_DIV_ID).show();
                    userCurrentIdx = 0;
                }

                $('#' + HOLDER_DIV_ID).html(html);

                $("#" + HOLDER_DIV_ID + " .userListItem").bind("mouseover", function(event) {
                    var all = $("#" + HOLDER_DIV_ID).children("div");
                    all.addClass("unselected");
                    all.removeClass("selected");

                    $(event.target).removeClass("unselected");
                    $(event.target).addClass("selected");
                });
                $("#" + HOLDER_DIV_ID + " .userListItem").bind("mouseout", function(event) {
                    $(event.target).addClass("unselected");
                });

                changeCurrentUserSelection(userCurrentIdx);
                urlList = $("#" + HOLDER_DIV_ID + " div .hdnUrl");
                dataSize = urlList.size();
                userAutocompleting = true;
            }
        });
    }
}
function addUser(autocompletedValue, currentPosition, target) {
       var text = $("#" + target).val();
       for (var i = currentPosition; i > 0; i --) {
           if (text.charAt(i - 1) == '#') {
                text = text.substring(0, i) + autocompletedValue + ' ' + text.substring(currentPosition);
                $("#" + target).caret(currentPosition);
                break;
           }
       }
       $('#' + targetId).val(text);
       $('#suggestions').hide();
       userAutocompleting = false;
       userCurrentIdx = -1;
       userList = null;
       $("#" + target).focus();
    }

function changeCurrentUserSelection(idx) {
    var all = $("#" + HOLDER_DIV_ID + " .userListItem");
    all.addClass("unselected");
    all.removeClass("selected");
    var item = $("#" + HOLDER_DIV_ID + " .userListItem").eq(idx);
    item.removeClass("unselected");
    item.addClass("selected");
}