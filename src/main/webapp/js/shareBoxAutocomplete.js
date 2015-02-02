(function($){
    $.fn.shareBoxAutocomplete = function(url, imageRoot, idSuffix, startChar, param, valueField, stringRepresentationFunction) {
        $(this).data("targetUrl" + idSuffix, url);
        $(this).data("startChar" + idSuffix, startChar);
        $(this).data("param" + idSuffix, param);
        $(this).data("currentIdx" + idSuffix, -1);
        $(this).data("valueField" + idSuffix, valueField);
        $(this).data("autocompleting" + idSuffix, false);
        $(this).data("stringRepresentationFunction" + idSuffix, stringRepresentationFunction);
        var targetId = $(this).attr("id");
        var suggestionsDiv = '<div id="suggestions' + idSuffix + '" class="suggestions"><img src="'
            + imageRoot + '/images/upArrow.png" style="position: relative;'
            + 'top: -12px; left: 30px" /><ul class="suggestionList" id="suggestionList'
            + idSuffix + '"></ul></div>';

        $("body").append(suggestionsDiv);
        var suggestions = $("#suggestions" + idSuffix);
        var target = $('#' + targetId);

        var offset = {top: target.offset().top + target.height() + 3,
                      left: target.offset().left};

        suggestions.offset(offset);

        $(this).bind("keyup", function(event) {lookup(event, targetId, idSuffix);});
        $(this).bind("keydown", function(event){return handleArrows(event, targetId, idSuffix);});

        $(this).bind("blur", function() {
            suggestions.hide();
            target.data("autocompleting" + idSuffix, false);
        });
    };
})(jQuery);

function handleArrows(event, targetId, idSuffix) {
    var target = $("#" + targetId);
    if (target.data("autocompleting" + idSuffix) == false) {
        return true;
    }

    var code = (event.keyCode ? event.keyCode : event.which);

    var currentIdx = target.data("currentIdx" + idSuffix);

    var list = target.data("list" + idSuffix);

    if (code == 40) { //down arrow
        currentIdx++;
        if (currentIdx == list.length) {
            currentIdx = 0;
        }
        changeCurrentSelection(currentIdx, targetId, idSuffix);
        return false;
    }
    if (code == 38) { //up arrow
        currentIdx--;
        if (currentIdx == -1) {
            currentIdx = list.length - 1;
        }
        changeCurrentSelection(currentIdx, targetId, idSuffix);
        return false;
    }

    if (code == 13) {
        var valueField = target.data("valueField" + idSuffix);
        add(list[currentIdx][valueField], $("#" + targetId).caret(), targetId, idSuffix);
        return false;
    }
}

function lookup(event, targetId, idSuffix) {
    var code = (event.keyCode ? event.keyCode : event.which);
    if (code == 38 || code == 40 || code == 13) {
        return;
    }

    var target = $("#" + targetId);

    var currentPosition = target.caret();
    var text = target.val();

    var should = shouldAutocomplete(text, currentPosition, target, idSuffix);
    if (!should) {
        $("#suggestions" + idSuffix).hide();
        target.data("autocompleting" + idSuffix, false);
        return;
    }

    if(text.length == 0) {
        // Hide the suggestion box.
        $("#suggestions" + idSuffix).hide();
    } else {
        var part = "";
        for (var i = currentPosition; i > 0; i --) {
            if (text.charAt(i - 1) == target.data("startChar" + idSuffix)) {
                 part = text.substring(i, currentPosition);
                 break;
            }
        }
        var params = new Object();
        params[target.data("param" + idSuffix)] = part;

        var stringRepresentationFunction = target.data("stringRepresentationFunction" + idSuffix);
        var valueField = target.data("valueField" + idSuffix);
        $.getJSON(target.data("targetUrl" + idSuffix), params, function(data){
            if(data != null && data.length > 0) {
                var html = '';
                for (var i = 0; i < data.length; i++) {
                    html += '<li class="suggestionListItem" onmousedown="add(\'' + data[i][valueField] + '\', ' + currentPosition + ',\''
                        + targetId + '\',\'' + idSuffix + '\')">' + stringRepresentationFunction(data, i) +'</li>';
                }

                if (target.data("autocompleting" + idSuffix) == false) {
                    $("#suggestions" + idSuffix).show();
                    target.data("currentIdx" + idSuffix, 0);
                }

                $("#suggestionList" + idSuffix).html(html);

                $("#suggestionList" + idSuffix + " .suggestionListItem").bind("mouseover", function(event) {
                    $("#suggestionList" + idSuffix).children().css("background-color", "#212427");
                    $(event.target).css("background-color", "#659CD8");
                });
                $("#suggestionList" + idSuffix + " .suggestionListItem").bind("mouseout", function(event) {
                    $(event.target).css("background-color", "#212427");
                });

                changeCurrentSelection(target.data("currentIdx" + idSuffix), targetId, idSuffix);

                target.data("list" + idSuffix, data);
                target.data("autocompleting" + idSuffix, true);
            } else {
                $("#suggestions" + idSuffix).hide();
                target.data("autocompleting" + idSuffix, false);
            }
        });
    }
}

function changeCurrentSelection(idx, targetId, idSuffix) {
    $("#suggestionList" + idSuffix).children().css("background-color", "#212427");
    var item = $("#suggestionList" + idSuffix).children().eq(idx);
    item.css("background-color", "#659CD8");
    $("#" + targetId).data("currentIdx" + idSuffix, idx);
}

function add(autocompletedValue, currentPosition, targetId, idSuffix) {
   target = $("#" + targetId);
   var text = target.val();
   for (var i = currentPosition; i > 0; i --) {
       if (text.charAt(i - 1) == target.data("startChar" + idSuffix)) {
            text = text.substring(0, i) + autocompletedValue + ' ' + text.substring(currentPosition);
            target.caret(currentPosition);
            break;
       }
   }
   target.val(text);
   $("#suggestions" + idSuffix).hide();
   target.data("autocompleting" + idSuffix, false);
   target.data("currentIdx" + idSuffix, -1);
   target.data("list" + idSuffix, null);
   target.focus();
}

function shouldAutocomplete(text, currentPosition, target, idSuffix) {

    // if there is no startChar, skip autocomplete
    var startChar = target.data("startChar" + idSuffix);
    if (text.indexOf(startChar) < 0) {
        return false;
    }

    // find the previous symbol - space or the startChar
    for (var i = currentPosition - 1; i >=0; i--) {
        // if space is found, (= no startChar)
        if (text.charAt(i) == ' ') {
            return false;
        }
        // if there are only no-space chars between the current one
        // and the startChar, return true - this has to be autocompleted
        if (text.charAt(i) == startChar) {
            return true;
        }
    }
    return false;
}