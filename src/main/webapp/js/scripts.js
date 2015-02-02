var root = '/';
var staticRoot = '/static';
var language = 'en';
var userLogged = false;
var pollForNotificatations = false;
var currentUsername = '';
var socialNetworkSettings = [];
var replyMsg = 'Reply';
var followMsg = 'Follow';
var loadingMsg = 'loading..';
var likeMsg = 'Like';
var reshareMsg = 'Reshare';
var unlikeMsg = 'Unlike';
var favouriteMsg = 'Favourite';
var unfavouriteMsg = 'Unfavourite';
var addedToFavouritesMsg = 'Added to favourites';
var externalUsernames = [];
var externalNetworksActive = false;
var confirmDeleteMsg = 'Are you sure you want to delete this message?';
var confirmDeleteExternalCheckboxMsg = 'Also delete message on external networks?';
var likeSuccessMsg = 'Message successfully liked';
var reshareSuccessMsg = 'Message successfully reshared';
var createMessageFilterSuccessMsg = 'Filter successfully created';
var deleteMessageFilterSuccessMsg = 'Filter successfully deleted';
var editMsg = 'Edit';
var saveMsg = 'Save';
var translateMsg = 'Translate';
var emoticonsEnabled = false;
var inactivityTimeout;
var interestedInKeywords = [];
var twitterEnabled = false;
var currentLikedId;
var reshareDialogInitialized = false;
var MAX_MESSAGE_SIZE = 300;
var NOTIFICATIONS_DEFAULT_COLOR = "#D4D2D2";
var BACKGROUND_QUEUE = "backgroundQueue";
var backgroundQueue = [];
var URL_REGEX = /[-a-zA-Z0-9@:%_\+.~#?&//=]{2,256}\.[a-z]{2,4}\b(\/[-a-zA-Z0-9@:%_\+.~#?&//=]*)?/gi;

function init(config) {
    root = config.root;
    staticRoot = config.staticRoot;
    language = config.language;
    userLogged = config.userLogged;
    currentUsername = config.currentUsername;
    externalUsernames = config.externalUsernames;
    externalNetworksActive = config.externalNetworksActive;
    socialNetworkSettings = config.socialNetworkSettings;
    replyMsg = config.messages.reply;
    followMsg = config.messages.follow;
    loadingMsg = config.messages.loading;
    reshareMsg = config.messages.reshare;
    likeMsg = config.messages.like;
    unlikeMsg = config.messages.unlike;
    favouriteMsg = config.messages.favourite;
    unfavouriteMsg = config.messages.unfavourite;
    addedToFavouritesMsg = config.messages.addedToFavourites;
    confirmDeleteMsg = config.messages.confirmDelete;
    confirmDeleteExternalCheckboxMsg = config.messages.confirmDeleteExternalCheckbox;
    likeSuccessMsg = config.messages.likeSuccess;
    reshareSuccessMsg = config.messages.reshareSuccess;
    createMessageFilterSuccessMsg = config.messages.createMessageFilterSuccess;
    deleteMessageFilterSuccessMsg = config.messages.deleteMessageFilterSuccess;
    editMsg = config.messages.edit;
    saveMsg = config.messages.save;
    translateMsg = config.messages.translate;
    emoticonsEnabled = config.emoticonsEnabled;
    pollForNotificatations = config.pollForNotifications;
    twitterEnabled = isTwitterEnabled();

    $(document).ready(function() {
        $(":button").button();
        $(":submit").button();
        jQuery.timeago.settings.allowFuture = true;
        initMessageOptions();
        //$.ajaxSetup({scriptCharset: "utf-8", contentType: "application/json; charset=utf-8"});

        // generic ajax indicator
        $("body").append('<div id="ajaxBusy"><img src="' + staticRoot + '/images/ajax_loading.gif" />Please wait</div>');
        $('#ajaxBusy').css({
            display: "none",
        });

        $.ajaxSetup({
            // Disable caching of AJAX responses
            cache: false,
            timeout: 20000,
            complete: function(jqxhr, textStatus) {
                if (jqxhr.status == 401) {
                    // redirect to the login page. If there is a permanent cookie
                    // the server will automatically relogin the user
                    window.location.href = root + "login?message=sessionExpired";
                }
            }
        });

        // Ajax activity indicator bound
        // to ajax start/stop document events
        $(document).ajaxSend(function(e, jqxhr, settings){
            if (settings.url.indexOf("background") == -1) {
                var indicator = $('#ajaxBusy');
                indicator.css("top", indicator.offset().top);
                indicator.show();
            }
        }).ajaxComplete(function(e, jqxhr, settings){
            $('#ajaxBusy').hide();
            var externalStatuses = jqxhr.getResponseHeader("External-Network-Statuses");
            if ($.trim(externalStatuses).length > 0
                    && settings.url.indexOf("background") == -1) {
                info(externalStatuses, 20000);
            }
            // handling the background queue - call the next queued request
            // each queued request is a function that makes an ajax request
            // whenever each request completes, the next one is invoked.
            // That way they are sequentially called.
            if (backgroundQueue.length > 0) {
                backgroundQueue[0].call();
            }
        }).ajaxError(function(e, jqxhr, settings, exception) {
            if (settings.url.indexOf("background") == -1) {
                if (exception.length > 0) {
                    info("Request failed", 20000); //TODO i18n?
                } else {
                    info("Connection problem", 20000); //TODO i18n
                }
            }
        });

        $(document).scroll(function() {
            var indicator = $('#ajaxBusy');
            if (indicator.css("display") != "none") {
                indicator.css("top", $(document).scrollTop() + 3);
            }
        });

        // init notifications polling
        if (pollForNotificatations) {
            initNotificationPolling();
        }

        // fetch the missed important messages count
        if (!$("#unreadImportantMessagesCount").is(":visible")) {
            setTimeout(function() {
                pushToBackgroundQueue(function() {
                    $.get(root + "messages/missedCount?background=true", function(data) {
                        if (parseInt(data) > 0) {
                            $("#unreadImportantMessagesCount").text(data);
                            $("#unreadImportantMessagesCount").show();
                        }
                    });
                });
            }, 7000);
        }

        highlightInterestingMessages();
        initTwitterCurrentlyActiveFollowersPolling();

        // activate polling on every page
        initActivityPolling();

        // slow down polling if the page is not active for 6 minutes
        inactivityTimeout = setInterval(function() {
            pollingSlowedDown = true;
        }, 6 * 60 * 1000);

        $(document).mouseenter(function() {
            clearInterval(inactivityTimeout);
            pollingSlowedDown = false;
        });
    });
}

function initMessageOptions() {
    $("span.timeago").timeago(); // contains specific styling
    $("span.time").timeago(); // no styling
    initLikersTooltips();
    initMessageActionsHover();
    initReshareDialog();
    if (emoticonsEnabled) {
        $(".textContent").emoticons(staticRoot + "/images/emoticons/");
        $(".replyText").emoticons(staticRoot + "/images/emoticons/");
    }
}

function share(pictures) {
    var externalSites = [];
    var hideFromUsernames = [];
    var hideFromCloseFriends = false;

    $("#shareButton").attr("disabled", "disabled");

    $(".shareOption input:checked").each(function() {
        externalSites.push($(this).val());
    });

    showNewMessages();

    var params = {text: $("#message").val(),
            pictureFiles: pictures.join(","),
            externalSites: externalSites.join(","),
            hideFromUsernames: hideFromUsernames.join(","),
            hideFromCloseFriends: hideFromCloseFriends};

    var scheduledTime = $("#messageScheduler").datetimepicker("getDate");
    if (scheduledTime != null && $("#scheduleClearButton").is(":visible")) {
        $.extend(params, {scheduledTime: scheduledTime.valueOf()});
    }
    $.post(root + "share", params,
        function(data) {
            var messagesList = $("#messagesList");
            messagesList.prepend(data);
            $(".message").each(function(idx, element) {
                var current = $(this);
                current.hide();
                current.fadeIn();
                handleMessageActionsHover(current);
                return false;
            });
            fillTimeago(1);
            if (scheduledTime != null && $("#shareButton").val() == "Schedule") {
                info("Scheduling successful"); //TODO i18n
                $("#shareButton").val("Share");
            }
        });
    $("#shareButton").removeAttr("disabled");
    $("#message").val("");
    $("#charsRemaining").text(MAX_MESSAGE_SIZE);
    $("#messageScheduler").datetimepicker("setDate", null);
    $("#scheduleClearButton").hide();
    $("#charsRemainingNote").html("");
}

function reply(msgId, inReplyTo, shouldAppendReply) {
    var replyText = $("#replybox-" + msgId).val();
    $("#replybox-" + msgId).parent().remove();
    $.post(root + "message/reply", {text: replyText, originalMessageId: inReplyTo}, function(data) {
        var repliesHolder = $("#message-" + msgId + " .replies");
        if (repliesHolder.children(".reply").length == 0) {
            if (shouldAppendReply) {
                getRepliesAndAppend(msgId, data);
            } else {
                getReplies(msgId);
            }
        } else {
            repliesHolder.append(data);
            handleReplyMessageActionsHover(repliesHolder.children(".reply"));
        }
    });
}

function showNewMessages() {
    //TODO possible race condition here? possible in js?
    if (incomingMessagesHtml != "") {
        incomingMessagesHtml += '<div class="recentMessageSeparator"></div>';
        resetTitle();
        var messagesList = $("#messagesList");
        $(".recentMessageSeparator").removeClass("recentMessageSeparator"); //only one separator
        messagesList.prepend(incomingMessagesHtml);
        $(".message").each(function(idx, element) {
            $(this).hide();
            $(this).fadeIn();

            handleMessageActionsHover($(this));

            if (idx == incomingCount - 1) {
                return false;
            }
        });

        incomingMessagesHtml = "";
        fillTimeago(incomingCount);
        initLikersTooltips();
        initReshareDialog();
        highlightInterestingMessages();
        incomingCount = 0;
        $("#newMessages").hide();
    }
}

function fillTimeago(messageCount) {
    $("span.timeago").each(function(idx, element) {
        $(this).timeago();
        if (idx == messageCount - 1) {
            return false;
        }
    });
}

var showingMore = false;
var noMoreMessagesExist = false;

function showMoreMessages(windowId, important) {
    showMore("messages/more", {windowId: windowId, important: important});
}

function showMoreUserMessages(id) {
    showMore("messages/more/user", {userId: id});
}

function showMore(path, extraParams) {

    // if the previous fetch was empty, i.e. no more messages exist
    if (noMoreMessagesExist) {
        return;
    }

    // if currently fetching more results
    if (showingMore) {
        return;
    }
    $("#loadingMore").show();
    var count = $(".message").length;
    var params = {from: count + 1};
    showingMore = true;
    $.extend(params, extraParams);

    extendParamsWithFilteredNetwork(params);

    $.get(root + path, params , function(data) {
        if ($.trim(data).length == 0) {
            noMoreMessagesExist = true;
        }

        $("#messagesList").append(data);

        //TODO handle emoticons
        $(".message").each(function(idx, element) {
            handleMessageActionsHover($(this));
        });

        $(".timeago").timeago();
        initLikersTooltips();
        initReshareDialog();
        highlightInterestingMessages();
        $("#loadingMore").hide();
        showingMore = false;
    });
}

function handleMessageActions(target) {
    if (userLogged) {
        target.hover(function() {
            $(this).find(".messageActions:first").css("visibility", "visible");
        }, function() {
            $(this).find(".messageActions:first").css("visibility", "hidden");
        });

        target.hover(function() {
            $(this).find(".messageOptionsIcon").css("visibility", "visible");
            $(this).find(".externalIndicator").css("visibility", "hidden");
        }, function() {
            $(this).find(".messageOptionsIcon").css("visibility", "hidden");
            $(this).find(".externalIndicator").css("visibility", "visible");
        });

        target.find(".messageOptionsIcon").hover(function() {
            $(this).attr("src", staticRoot + "/images/options_arrow_hover.png");
        }, function() {
            $(this).attr("src", staticRoot + "/images/options_arrow.png");
        });

        createOptionsMenu(target, "messageOptionsIcon");
    }
}

function createOptionsMenu(target, cssClass) {
    target.each(function() {
        var current = $(this);
        current.find("." + cssClass).click(function() {
            var panel = $("<div/>").addClass("messageOptionsPanel");
            var canEdit = current.find(":input[name='canEdit']").val() == 'true';
            var isOwn = current.find(":input[name='isOwn']").val() == 'true';
            var elementId = current.attr("id");
            var reply = elementId.indexOf("reply") > -1;
            var id = "";
            if (reply) {
                id = elementId.replace("reply-", "");
            } else {
                id = elementId.replace("message-", "");
            }
            //TODO make it possible to edit replies
            if (canEdit && isOwn && !reply) {
                var edit = $("<div/>").text(editMsg).click(function() {
                    panel.hide();
                    startEdit(id);
                });
                panel.append(edit);
            }
            if (!isOwn && false) { //TODO reporting
                var report = $("<div/>").text("msg").click(function() {
                    panel.hide();
                    reportAbuse(id, reply);
                });
                panel.append(report);
            }
            if (!isOwn) {
                var translateItem = $("<div/>").text(translateMsg).click(function() {
                    panel.hide();
                    translate(id, reply);
                });
                panel.append(translateItem);
            }

            panel.find("div").hover(function() {
                $(this).css("background-color", "#F8F8F8");
            }, function() {
                $(this).css("background-color", "white");
            });
            var icon = $(this);
            var top = icon.offset().top + icon.height();
            var left = icon.offset().left - panel.width();
            panel.offset({top: top, left: left});
            if (panel.children().length > 0) {
                $("body").append(panel);
                panel.bind("mousedownoutside", function() {
                    $(this).hide();
                });
            }
        });
    });
}

function handleReplyMessageActionsHover(target) {
    handleReplyMessageActions(target);
    handleMessageHover(target);
}

function handleReplyMessageActions(target) {
    if (userLogged) {
        target.each(function (idx, element) {
            element = $(element);
            element.hover(function() {
                element.find(".messageActions").css("visibility", "visible");
            }, function() {
                element.find(".messageActions").css("visibility", "hidden");
            });
        });

        target.hover(function() {
            $(this).find(".replyOptionsIcon").css("visibility", "visible");
        }, function() {
            $(this).find(".replyOptionsIcon").css("visibility", "hidden");
        });

        createOptionsMenu(target, "replyOptionsIcon");
    }
}

function reshare(msgId, withExtraOptions) {
    var comment = '';
    var editedLikedMessage = '';
    var shareAndLike = true;
    var sites = [];
    if (withExtraOptions) {
        $(".reshareOption input:checked").each(function() {
            sites.push($(this).val());
        });
        comment = $("#reshareComment").val();
        if (!comment) {
            comment = '';
        }
        editedLikedMessage = $("#editedLikedMessage").val();
        // send as empty if it has not been edited
        var elementPrefix = "#message-";
        if (isReply(msgId)) {
            elementPrefix = "#reply-";
        }
        if (!editedLikedMessage || $(elementPrefix + msgId).find(".fullTextContent").first().text().trim() == editedLikedMessage) {
            editedLikedMessage = '';
        }
        shareAndLike = $("#shareAndLike").is(":checked");
    } else {
        if (!isReply(msgId)) {
            for (var i = 0; i < socialNetworkSettings.length; i++) {
                var sn = socialNetworkSettings[i];
                if (sn.enabled && sn.active) {
                    sites.push(sn.prefix);
                }
            }
            sites.push("ws");
        } else {
            shareAndLike = false; // by default don't share if it's a comment
            // like the reply (comment) only on the target network
            for (var i = 0; i < socialNetworkSettings.length; i++) {
                var sn = socialNetworkSettings[i];
                if (msgId.indexOf(sn.prefix) == 0) {
                    sites.push(sn.prefix);
                }
            }
            if (sites.length == 0) {
                sites.push("ws"); //if no external network found, it means it's an internal message
            }
        }
    }

    $.post(root + "reshare/" + msgId, {
        comment: comment,
        editedLikedMessage: editedLikedMessage,
        sites: sites.join(","),
        shareAndLike: shareAndLike},
        function(data) {
            handleLikeButton(msgId);
            $("#messagesList").prepend(data);
            var msg = $("#messagesList .message").eq(0);
            msg.hide();
            msg.fadeIn("slow");
            msg.find(".timeago").timeago();
            handleMessageActionsHover(msg);

            // instead of the old $.scrollTo($("#logo"));, show a message:
            info(reshareSuccessMsg);
        }
    );

    $("#reshareDialog").dialog("close");
}

function simpleLike(msgId) {
    $.post(root + "simpleLike/" + msgId,
        function() {
           handleLikeButton(msgId);
           info(likeSuccessMsg);
        }
    );
}

function handleLikeButton(msgId) {
     var currentValue = $("#message-" + msgId + " span.score").text();
     if (currentValue.length == 0) {
         currentValue = "0";
     }
     $("#message-" + msgId + " span.score").text(parseInt(currentValue) + 1);
     var link = $("#likeLink" + msgId);
     link.attr("id", "unlikeLink" + msgId);
     link.removeClass("likeLink").addClass("unlikeLink");
     link.poshytip("disable");
     link.html(link.html().replace(likeMsg, unlikeMsg));
     link.removeAttr("onclick").click(function () {
         unlike(msgId);
     });
}
function unlike(msgId) {
    $.post(root + "unlike/" + msgId,
        function(data) {
            $("#message-" + msgId + " span.score").text(parseInt($("#message-" + msgId + " span.score").text()) - 1);
            var link =  $("#unlikeLink" + msgId);
            link.html(link.html().replace(unlikeMsg, likeMsg));
            link.attr("id", "likeLink" + msgId);
            link.removeClass("unlikeLink").addClass("likeLink");
            link.removeAttr("onclick").click(function () {
                like(msgId);
            });
            link.poshytip("enable");
            if (data.length > 0) {
                $("#message-" + data).remove();
            }
        }
    );
}

function favourite(msgId) {
    $.post(root + "message/favourite/" + msgId,
        function(data) {
            var link =  $("#favouriteLink" + msgId);
            link.html(link.html().replace(favouriteMsg, unfavouriteMsg));
            link.removeAttr("onclick").click(function () {
                unfavourite(msgId);
            });
            info(addedToFavouritesMsg);
        }
    );
}

function unfavourite(msgId) {
    $.post(root + "message/unfavourite/" + msgId,
        function(data) {
            var link = $("#favouriteLink" + msgId);
            link.html(link.html().replace(unfavouriteMsg, favouriteMsg));
            link.removeAttr("onclick").click(function () {
                favourite(msgId);
            });
        }
    );
}

/**
 * Fetch replies and then (optionally) append a freshly shared message at the end
 *
 * @param msgId
 * @param newMessage
 */
function getRepliesAndAppend(msgId, newMessage) {
    $("#message-" + msgId + " .repliesLink").replaceWith('<span class="loadingReplies">' + loadingMsg + '</span>');

    $.get(root + "replies/" + msgId, function(data) {
        var holder = $("#message-" + msgId + " .replies");
        $("#message-" + msgId + " .loadingReplies").hide();

        var trimmedData = $.trim(data);
        if (trimmedData.length > 0) {
            holder.append(trimmedData);
            $("#message-" + msgId + " .replies .timeago").timeago();
        }
        if (newMessage != null) {
            holder.append(newMessage);
        }
        if (trimmedData.length > 0 || newMessage != null) {
            var replies = holder.children(".reply");
            handleReplyMessageActionsHover(replies);
            if (emoticonsEnabled) {
                replies.emoticons(staticRoot + "/images/emoticons/");
            }
            initReshareDialog();
            initLikersTooltips();
        }
    });
}
function getReplies(msgId) {
    getRepliesAndAppend(msgId, null);
}

function startReply(msgId, inReplyTo, author, mentions, shouldAppendReply) {
    var replybox = $("#replybox-div-" + msgId);
    var textarea;
    if (replybox.length == 0) {
        replybox = $('<div id="replybox-div-' + msgId + '">' +
            '<textarea id="replybox-' + msgId + '" class="replyBox"></textarea>' +
            '<span class="replyCharsRemaining">' + MAX_MESSAGE_SIZE + '</span>' +
            '<input type="button" class="replyButton" onclick="reply(\'' + msgId + '\',\''+ inReplyTo + '\',' + shouldAppendReply + ')" value="' + replyMsg + '"/>' +
            '<button value="" onclick="shortenUrlsInBox(\'replybox-' + msgId + '\');" class="replyButton shortenButton" style="margin-top: 0px; margin-right: 10px;" title="">' +
            '<img src="' + staticRoot + '/images/shortenLinks.png" />' +
            '</button>' +
            '</div>');

        textarea = replybox.find("textarea");
        var button = replybox.find("input");

        textarea.keydown(function (e) {
            if (e.ctrlKey && e.keyCode == 13) {
              button.click();
            }
        });

        var counter = replybox.find("span");
        //textarea.elastic(); TODO is this making firefox block?

        textarea.blur(function() {
            if ($(this).val().length == 0) {
                replybox.remove();
            }
        });

        button.button();

        initCharCounter([button], textarea, counter);

        var repliesHolder = $("#message-" + msgId + " .replies");
        repliesHolder.append(replybox);
    } else {
        textarea = replybox.find("textarea");
    }


    var currentValue = textarea.val();

    if ($.trim(author).length > 0 && currentValue.indexOf("@" + author) == -1) {
        mentions = mentions.replace('@' + author + ' ', '');
        var usernamesPrefix = "@" + author + " " + mentions;
        usernamesPrefix = usernamesPrefix.replace('@' + currentUsername + ' ', '');
        for (var i = 0; i < externalUsernames.length; i ++) {
            usernamesPrefix = usernamesPrefix.replace('@' + externalUsernames[i] + ' ', '');
        }
        textarea.val(usernamesPrefix + textarea.val());
    }

    textarea.focus();
    textarea.triggerHandler("keyup"); //calculate initial values
    textarea.selectRange(textarea.val().length, textarea.val().length);
}

function isReply(msgId) {
    return msgId.indexOf("tw") == -1 && $("#reply-" + msgId).length == 1;
}

function deleteMessage(msgId, reply) {
    var dialogHtml = "<div><h3>" + confirmDeleteMsg + "</h3>";
    if (externalNetworksActive) {
        dialogHtml += "<br /><input type=\"checkbox\" checked=\"checked\" "
                + "id=\"deleteExternal\"><label for=\"deleteExternal\">"
                + confirmDeleteExternalCheckboxMsg +  "</label>";
    }
    dialogHtml += "</div>";
    var dialog = $(dialogHtml);
    dialog.dialog({buttons: [
         {
             text: 'Yes',
             click: function() {
                 var deleteExternal = $("#deleteExternal").is(":checked");
                 dialog.dialog("close");
                 $.post(root + "delete/" + msgId, {deleteExternal: deleteExternal}, function(data) {
                    if (data) {
                        if (reply) {
                            $("#reply-" + msgId).fadeOut("slow", function() {
                                $("#reply-" + msgId).remove();
                            });
                        } else {
                            $("#message-" + msgId).fadeOut("slow", function() {
                                $("#message-" + msgId).remove();
                            });

                        }
                        info("Message deleted");
                    }
                });
             }
         }, {text: 'No', click: function() {$(this).dialog("close");}}
    ],
    close: function() {
        dialog.remove();
    }});
}

function editMessage(messageId) {
    var newText = $("#inplaceEdit-" + messageId).val();
    var parentIdPrefix = "#message-";
    $(parentIdPrefix + messageId + " .messageText").html("").append(newText);
    $.post(root + "edit/" + messageId, {newText: newText}, function(data) {
        $(parentIdPrefix + messageId).replaceWith(data);
        $(parentIdPrefix + messageId + " .timeago").timeago();
        handleMessageActionsHover($(parentIdPrefix + messageId));
    });
}

function startEdit(messageId) {
    var parentIdPrefix = "#message-";
    var text = $(parentIdPrefix + messageId + " .textContent").text().trim();
    var originalHtml = $(parentIdPrefix + messageId + " .messageText").html();
    var holder = $(parentIdPrefix + messageId + " .textContent");
    var inplaceEdit = '<textarea class="inplaceEdit" rows="1" id="inplaceEdit-' + messageId + '">' + text + '</textarea>';
    inplaceEdit += '<input type="button" onclick="editMessage(\'' + messageId + '\');" value="' + saveMsg + '"/>';
    holder.bind("mousedownoutside", function() {
        holder.parent().html(originalHtml);
    });

    holder.html(inplaceEdit);
    holder.find(":button").button();
}

function translate(messageId, reply) {
    var contentSelector = "#message-" + messageId + " .textContent";
    if (reply) {
        contentSelector = "#reply-" + messageId + " .replyText";
    }
    var text = $(contentSelector).html();

    $.post(root + "message/translate", {text:text, language: language}, function(result) {
        $(contentSelector).html(result);
    });
}

var lastLikedId;
var lastEnteredLikeComment;
function initReshareDialog() {
    $(".reshareOptions").on("click", function() {
        var current = $(this);
        currentLikedId = current.attr("id").replace("reshareOptions", '');
        var reply = isReply(currentLikedId);

        var container = $("#reshareDialog");
        container.dialog({width: 290, height: 290, resizable: false, title: reshareMsg});

        for (var i = 0; i < socialNetworkSettings.length; i++) {
            var sn = socialNetworkSettings[i];
            if (sn.enabled && !reply && sn.active) {
                $("#" + sn.siteName + "ReshareOption").parent().toggleCheckboxValue(true);
            } else {
                $("#" + sn.siteName + "ReshareOption").parent().toggleCheckboxValue(false);
            }
            // if the message is external, and the external network supports like+share, show checkbox
            if (currentLikedId.indexOf(sn.prefix) != -1) {
                if (!sn.likeAndReshare) {
                    $("#shareAndLikeHolder").hide();
                } else {
                    $("#shareAndLikeHolder").show();
                }
            }
        }

        if (!reply) {
            $("#internalReshareOption").parent().toggleCheckboxValue(true);
            $("#editedLikedMessage").val($("#message-" + currentLikedId).find(".fullTextContent").first().text().trim());
        } else {
            $("#internalReshareOption").parent().toggleCheckboxValue(false);
            $("#editedLikedMessage").val($("#reply-" + currentLikedId).find(".fullTextContent").first().text().trim());
        }

        $("#shareAndLike").attr("checked", "checked");

        // the parent of the container is the actual dialog wrapper
        container.parent().bind("mousedownoutside", function() {
            container.dialog("close");
        });

        // if focus is lost on the dialog, preserve the entered text
        if (lastLikedId == currentLikedId) {
            container.find("#reshareComment").val(lastEnteredLikeComment);
        } else {
            lastEneteredLikeComment = "";
        }
        lastLikedId = currentLikedId;
    });

    if (!reshareDialogInitialized) {
        // do these only once - on initialization
        var container = $("#reshareDialog");
        container.find("#reshareComment").keyup(function() {
            lastEnteredLikeComment = $(this).val();
        });
        container.find("#reshareComment").labelify({labelledClass: "hint"});

        container.find(".reshareOption").each(function() {
            $(this).toggleCheckbox();
        });
        reshareDialogInitialized = true;
    }
}

function initLikersTooltips() {
    $(".scoreHolder").poshytip({
        content: function(updateCallback) {
            var msgId = $(this).attr("id").replace("score", "");
            $.get(root + "message/likers", {messageId: msgId},
                function(data) {
                   updateCallback(data);
                   $(".likersHolder").bind("mousedownoutside", function(event) {
                       $("#score" + msgId).poshytip("hide");
                   });
                }
            );
            return loadingMsg;
        },
        alignTo: 'target',
        alignX: 'center',
        alignY: 'bottom',
        offsetY: 5,
        allowTipHover: true,
        fade: false,
        slide: false,
        className: 'blackTooltip',
        showTimeout: 0,
        showOn: 'none'
     });

    $(".scoreHolder").click(function() {
        $(this).poshytip("show");
    });
}

function colorToHex(rgbString) {
    if (rgbString == "transparent") {
        return "";
    }
    var parts = rgbString
            .match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/);

    delete(parts[0]);
    for (var i = 1; i <= 3; ++i) {
        parts[i] = parseInt(parts[i]).toString(16);
        if (parts[i].length == 1) parts[i] = '0' + parts[i];
    }
    var hexString = parts.join(''); // "0070ff"
    return "#" + hexString.toUpperCase();
}

function initCharCounter(buttons, field, counter) {
    for (var i = 0; i < buttons.length; i++) {
        buttons[i].attr("disabled", "disabled");
        buttons[i].css("opacity", "0.5");
    }

    field.on("keyup", function() {
        var msgText = $(this).val();
        var remaining = MAX_MESSAGE_SIZE - msgText.length;
        counter.text(remaining);
        if (remaining >= 0) {
            counter.css("color", "#CCCCCC");
        } else {
            counter.css("color", "#D40D12");
        }

        for (var i = 0; i < buttons.length; i++) {
            if (remaining == MAX_MESSAGE_SIZE || remaining < 0) {
                buttons[i].attr("disabled", "disabled");
                buttons[i].css("opacity", "0.5");
            } else {
                buttons[i].removeAttr("disabled");
                buttons[i].css("opacity", "1");
            }
        }

        if (twitterEnabled) {
            //find links and count them as 20 characters (they will be shortened on twitter)
            var urls = msgText.match(URL_REGEX);
            if (urls != null) {
                for (var i = 0; i < urls.length; i ++) {
                    remaining = remaining + urls[i].length - 20;
                }
            }
            if (remaining < MAX_MESSAGE_SIZE - 140) {
                $("#charsRemainingNote").html("Twitter limit reached. The tweet will contain a link to welshare");
            } else {
                $("#charsRemainingNote").html("");
            }
        }
    });

    field.on("paste", function() {
        setTimeout(function() {
            field.trigger("keyup");
        }, 100);
    });
}

function follow(id, name) {
    $.post(root + "follow/" + id, function() {
        refreshFollowButtons(id);
        info("Following " + name + " successful"); //TODO i18n
    });
}

function toggleCloseFriend(id, name) {
    $.post(root + "toggleCloseFriend/" + id, function(data) {
        refreshFollowButtons(id);
        if (data) {
            info(name + " added to close friends"); //TODO i18n
        } else {
            info(name + " removed from close friends"); //TODO i18n
        }
    });
}

function unfollow(id, name) {
    $.post(root + "unfollow/" + id, function() {
        refreshFollowButtons(id);
        info("Unfollowing " + name + " successful"); //TODO i18n
    });

}
function followMultiple() {
    var idFields = $("#friendSuggestions .itemselected [name='selectedFriend']");
    $.each(idFields, function(idx, idField) {
        $.post(root + "follow/" + $(idField).val());
    });
    info("Following multiple people successful"); //TODO i18n
}

function refreshFollowButtons(userId) {
    var holder = $("#userActionButtons-" + userId);
    holder.empty();
    $.get(root + "following/userActionButtons", {targetUserId: userId}, function(data){
        holder.html(data);
        holder.find(":button").button();
    });
}

function performUserSearch() {
    window.location.href = root + "users/find/" + $("#userSearch").val();
}

function disableLink(link) {
    var lnk = $(link);
    if (lnk.attr("disabled") == undefined) {
        lnk.attr("disabled", "disabled");
        lnk.attr("onclick", "");
    }
}

function fetchNewEvents() {
    $.get(root + "notifications/unread?background=true", function(data) {
        // if there are no incoming messages, do (almost) nothing
        // this stops the script from overriding existing notifications
        // after they are being marked as read
        if (data == null || data.indexOf("notificationListItem") == -1) {
            // if the box is empty, show the notifications footer
            if ($.trim($("#notificationEventsPanel").html()).length == 0) {
                showLastRead(0);
                handleInternalNotificationTargets();
                //$("#notificationEventsPanel").html(data);
            }
            $("#notificationEventsPanel").html(data);
            $("#notificationEventsPanel .timeago").timeago();
            return;
        }
        $("#notificationEventsPanel").html(data);
        $("#notificationEventsPanel .timeago").timeago();

        var count = $("#notificationEventsPanel .notificationListItem").size();
        if (count > 0) {
            $("#notifications").text(count);
            $("#notifications").css("background-color", "red");

            $("#notificationEventsPanel:button").button();
            handleInternalNotificationTargets();
        } else {
            $("#notifications").text("");
            $("#notifications").css("background-color", NOTIFICATIONS_DEFAULT_COLOR);
            $("#notifications").hide();
        }

        showLastRead(count);
    });
}

function handleInternalNotificationTargets() {
    // if the target messages are currently visible in the stream,
    // make clicking on notifications scroll to messages within welshare
    // rather than going to the external site
    $("#notificationEventsPanel .notificationListItem").each(function(idx, element) {
        var hidden = $(this).find("input[name='externalMessageId']");
        if (hidden.length > 0) {
            $(this).find("a").click(function() {
                var targetId = hidden.val();
                var target = $("#message-" + targetId);
                if (target.length > 0) {
                    $.scrollTo(target);
                    return false;
                }
                return true;
            });
        }
    });
}

function showLastRead(newNotificationsCount) {
    // if count of new notifications is less than 5, fill the panel with read notifications
    if (newNotificationsCount < 5) {
        // hid indicator only if it was not visible before the operation
        var hideAjaxIndicator = !$('#ajaxBusy').is(":visible");
        pushToBackgroundQueue(function() {
            $.get(root + "notifications/lastRead?background=true", {count: 5 - newNotificationsCount}, function(data) {
                if (newNotificationsCount == 0) {
                    $("#notificationEventsPanel").empty();
                }
                $("#notificationEventsPanel").append(data);
                $("#notificationEventsPanel .timeago").timeago();
                handleInternalNotificationTargets();
            });
        });
        if (hideAjaxIndicator) {
            $('#ajaxBusy').hide(); //this is background operation, so no need to show activity
        }
    }
}

function search(type) {
    var keywords = $("#searchKeywords").val();
    if (keywords.length > 0) {
        window.location.href = root + "search/" + type + "/" + keywords;
    }
}

function suggestFriends() {
    $.get(root + "friends/suggest", function(data) {
        $("#friendSuggestionsHolder").append(data);
        if ($("#friendSuggestionsHolder [name='selectedFriend']").length == 0) {
            info("No friends are found from other social networks that you have connected your account to"); //TODO i18n;
            $("#friendSuggestionsHolder").empty();
            return;
        }

        $("#friendSuggestionsHolder :button").button();
        $("#friendSuggestionsHolder").dialog({width: 510,
            beforeClose: function(event, ui) {
                $("#friendSuggestionsHolder").empty();
            }});
        $.fcbkListSelection("#friendSuggestions", 480, 50, 3);
    });
}

function markNotificationsAsRead() {
    $("#notificationEventsPanel").show();
    // mark as read only if there are currently unread
    if ($("#notifications").text() != "0") {
        pushToBackgroundQueue(function() {
            $.post(root + "notifications/markAsRead?background=true");
        });
    }
    $("#notifications").text("0");
    $("#notifications").css("background-color", NOTIFICATIONS_DEFAULT_COLOR);
}

function initMessageActionsHover() {
    var all = $(".message");
    handleMessageActionsHover(all);
}

function handleMessageActionsHover(target) {
    handleMessageActions(target);
    handleMessageHover(target);
}

function handleMessageHover(target) {
    target.each(function() {
        var userId = $(this).find(":input[name='userId']").val();
        hoverInfo($(this).find(".username"), userId);
        hoverInfo($(this).find(".picture"), userId);
        hoverInfo($(this).find(".singleMessageAuthor"), userId);
        hoverInfo($(this).find(".singleMessageUsername"), userId);
    });
}

function initTagAutocomplete() {
    $("#message").shareBoxAutocomplete("tags/autocomplete", staticRoot,
            "tag", "#", "tagPart", "name", function(data, idx) {
           return data[idx].name + ' (' + data[idx].occurrences + ')';
    });
}

function initShareboxUserAutocomplete() {
    $("#message").shareBoxAutocomplete("users/autocompleteBox", staticRoot,
            "shareBoxUser", "@", "start", "username", function(data, idx) {
        return '@' + data[idx].username + ' (' + data[idx].names + ')';
    });
}

var incomingMessagesHtml = "";
var incomingCount = 0;

var pollingCounter = 0;
var pollingSlowedDown = false;
function initPolling(windowId, important) {
    window.setInterval(function() {
        pollingCounter++;
        if (pollingSlowedDown && pollingCounter % 3 != 0) {
            return;
        }
        var params = {windowId: windowId, important: important};
        extendParamsWithFilteredNetwork(params);
        $.get(root + "messages/recent?background=true", params, function(data) {
            data = $.trim(data);
            if (incomingCount > 60) {
                incomingCount = 0;
                incomingMessagesHtml = "";
            }
            incomingMessagesHtml = data + incomingMessagesHtml;
            if (data.length > 0) {
                incomingCount += countMessages(data);
                var msg = incomingCount + "" + (incomingCount == 1 ? " new message" : " new messages");
                $("#newMessages").text(msg).show();
                resetTitle();
                document.title = '(' + incomingCount + ') ' + document.title;
            }
        });
    }, 240000);
}

function initNotificationPolling() {
    fetchNewEvents();
    window.setInterval(fetchNewEvents, 240000);
    $("#notifications").click(function() {
        markNotificationsAsRead();
    });
    $("#notificationEventsPanel").bind("mousedownoutside", function(event) {
        $("#notificationEventsPanel").hide();
    });
}

function handleActivityPollingResponse(data) {
    if ($.trim(data) == 'true') {
        $("#limitDialog").remove();
        var dialog = $('<div id="limitDialog">You have exceeded your configured daily time limit for looking at social networks. Now feel free to do something more productive.</div>'); //TODO i18n
        dialog.dialog({buttons: [
            {
                text: 'Ok, fine, just a little more',
                click: function() {
                    dialog.remove();
                }
            }],
            close: function() {
                dialog.remove();
            }
        });
    }
}

function initActivityPolling() {
    pushToBackgroundQueue(function() {
        $.post(root + "userActivity/poll?background=true", handleActivityPollingResponse);
    });
    window.setInterval(function() {
         // if polling is slowed down this means the screen is inactive => not continuing the activity session
         if (!pollingSlowedDown) {
            pushToBackgroundQueue(function() {
                $.post(root + "userActivity/poll?background=true", handleActivityPollingResponse);
            });
         }
    }, 30000);
}

function resetTitle() {
    if (document.title.charAt(0) == '(') {
        document.title = document.title.replace(document.title.substring(0, document.title.indexOf(')') + 1), '');
    }
}
function countMessages(data) {
    var pattern = new RegExp("<li", "g");
    var m = data.match(pattern);
    if (m != null) {
       return m.length;
    }
    return 0;
}

function clearPictures() {
    currentPictures = [];
    $("#uploadedImages").empty();
    $(".qq-upload-list").empty();
}

function shortenUrls(){
    shortenUrlsInBox("message", false, false);
}

function shortenUrlsWithTopBar(){
    shortenUrlsInBox("message", true, false);
}

function shortenViralUrls(){
    shortenUrlsInBox("message", true, true);
}

function shortenUrlsInBox(id, showTopBar, trackViral){
    if (showTopBar == undefined) {
        showTopBar = false;
    }
    if (trackViral == undefined) {
        trackViral = false;
    }
    var text = $("#" + id).val();
    if (text.indexOf("http") != -1) {
        $.post(root + "message/shortenUrls",
                {message: text, showTopBar: showTopBar, trackViral: trackViral}, function(data) {
            $("#" + id).val(data);
            $("#" + id).triggerHandler("keyup");
        });
    }
}

function closeHomeConnectPanel() {
    $("#homeConnectPanel").slideUp("slow");
    $.post(root + "account/markClosedHomepageConnectLinks");
}

function setLikesThreshold(userId) {
    var newThreshold = $("#likesThreshold-" + userId).val();
    var hideReplies = $("#hideReplies-" + userId).is(":checked");
    $.post(root + "following/setTreshold", {targetUserId: userId, value: newThreshold, hideReplies: hideReplies}, function(data) {
        info("Threshold set");
    });

}

$.fn.selectRange = function(start, end) {
    return this.each(function() {
        if (this.setSelectionRange) {
            this.focus();
            this.setSelectionRange(start, end);
        } else if (this.createTextRange) {
            var range = this.createTextRange();
            range.collapse(true);
            range.moveEnd('character', end);
            range.moveStart('character', start);
            range.select();
        }
    });
};

function createMessageFilter() {
    var text = $("#newMessageFilter").val();
    $("#newMessageFilter").val("");
    $.post(root + "filters/create", {text: text}, function() {
        info(createMessageFilterSuccessMsg);
        $.get(root + "filters/list", function(data) {
            $("#messageFilters").html(data);
            appendRefreshIcon("#messageFilters");
        });
    });
}

function deleteMessageFilter(id) {
    $.post(root + "filters/delete", {filterId: id}, function() {
        info(deleteMessageFilterSuccessMsg);
        $("#messageFilter-" + id).remove();
        appendRefreshIcon("#messageFilters");
    });
}

function createInterestedInKeyword() {
    var keywords = $("#newInterestedInKeyword").val();
    interestedInKeywords.push(keywords);
    $("#newInterestedInKeyword").val("");
    $.post(root + "interestedIn/create", {keywords: keywords}, function() {
        $.get(root + "interestedIn/list", function(data) {
            $("#interestedInKeywords").html(data);
        });
    });
    highlightInterestingMessages();
}

function deleteInterestedInKeyword(id) {
    $.post(root + "interestedIn/delete", {id: id}, function() {
        $("#interestedInKeyword-" + id).remove();
    });
}

function highlightInterestingMessages() {
    $(".message:not(.singleMessage)").each(function() {
        for (var i = 0; i < interestedInKeywords.length; i++) {
            if ($(this).text().match(new RegExp(interestedInKeywords[i], "i"))) {
                $(this).css("background-color", "#FFFDDE");
                break;
            }
        }
    });
}
function appendRefreshIcon(id) {
    if ($(id).html().indexOf('/refresh.png') == -1) {
        $(id).append('<img src="' + staticRoot + '/images/refresh.png"' +
            ' onclick="location.reload();" style="vertical-align: middle; cursor: pointer;" />');
    }
}

function hoverInfo(target, userId) {
    target.poshytip({
        content: function(updateCallback) {
            $.ajax({url: root + "users/info/" + userId,
                success: function(data, textStatus) {
                    updateCallback(data);
                    $(".hovercard").find(":button").button();
                },
                error: function() {
                    $(this).posytip("hide");
                }
            });
            return loadingMsg;
        },
        alignTo: 'cursor',
        alignX: 'right',
        alignY: 'bottom',
        allowTipHover: true,
        fade: false,
        slide: false,
        className: 'hovercard',
        showTimeout: 600,
        hideTimeout: 800
    });
}

function followOnTwitter(externalId) {
    $.post(root + "twitter/follow/" + externalId);
    $(".twFollowButton").remove();
}
function unfollowOnTwitter(externalId) {
    $.post(root + "twitter/unfollow/" + externalId);
    $(".twFollowButton").remove();
}

function calculateRealTwitterFollowers(targetFieldId, button, waitingMsg, resultMsg, errorMsg, tweetMsgStart, tweetMsgEnd, excludedFollowersMsg, url) {
    $("#" + targetFieldId).append('<img src="' + staticRoot + '/images/ajax_loading.gif" />');
    var waitingDiv = $("<div>" + waitingMsg + "</div>");
    $(button).replaceWith(waitingDiv);
    $.post(root + "twitter/realFollowers/calculate");
    var id = window.setInterval(function() {
        $.get(root + "twitter/realFollowersResult", function(data) {
            if (data.error == true) {
                $("#" + targetFieldId).html(errorMsg);
                waitingDiv.hide();
                window.clearInterval(id);
            } else if (data.error == false && data.realFollowers != undefined) {
                var result = data.realFollowers + " (" + (100 * data.realFollowers / data.totalFollowers).toFixed(0) + "%)";
                var html = resultMsg + " <strong>" + result + "</strong>";
                html += '<a href="https://twitter.com/intent/tweet?text=' + tweetMsgStart + ' ' + result.replace("%", "%25") + '. '
                    + tweetMsgEnd + ' ' + url + '&via=welshare" target="_blank"><img src="' + staticRoot
                    + '/images/tweet_button.png" style="margin-left: 10px; vertical-align: middle;" class="linkedImage" /></a><br />';
                html += excludedFollowersMsg + ': @' + data.excludedFollowersUsernames.join(', @');

                $("#" + targetFieldId).html(html);
                waitingDiv.hide();
                window.clearInterval(id);
            }
        });
    }, 10000);
}

function initTwitterCurrentlyActiveFollowersPolling() {
    if (twitterEnabled) {
        fetchCurrentlyActiveFollowersCount();
        window.setInterval(fetchCurrentlyActiveFollowersCount, 600000);
    }
}

function fetchCurrentlyActiveFollowersCount() {
    var hideAjaxIndicator = !$('#ajaxBusy').is(":visible");
    pushToBackgroundQueue(function() {
        $.get(root + "twitter/currentlyActiveFollowers?background=true", function(count) {
            $("#twitterFollowersActive").text(count);
        });
    });
    if (hideAjaxIndicator) {
        $('#ajaxBusy').hide(); //this is background operation, so no need to show activity
    }
}

function getFilteredMessages() {
    var params = {};
    extendParamsWithFilteredNetwork(params);
    if (params.filterNetwork) {
        $.get(root + "messages/filtered", params, function(data) {
            $("#messagesList").html(data);
            initMessageOptions();
        });
    } else {
        window.location.href = window.location.href;
    }
}

function extendParamsWithFilteredNetwork(params) {
    var filtered = $("#filterNetwork").val();
    if (filtered != null && filtered != 'all') {
        $.extend(params, {filterNetwork: filtered});
    }
}

function isTwitterEnabled() {
    var twitterEnabled = false;
    for (var i = 0; i < socialNetworkSettings.length; i++) {
        var sn = socialNetworkSettings[i];
        if (sn.prefix == 'tw') {
            if (sn.enabled && sn.active) {
                twitterEnabled = true;
            }
            break;
        }
    }
    return twitterEnabled;
}

function getMoreOldMessages() {
    $.get(root + 'messages/old/more', function(data) {
        $('#oldMessages').html(data);
    });
}

function pushToBackgroundQueue(f) {
    if (backgroundQueue.length == 0) {
        f.call();
    } else {
        backgroundQueue.push(f);
    }
}

function suggestTimeToShare() {
    $.get(root + "stats/recommendedTimeToShare", function(data) {
        var times = "";
        for (var i = 0; i < data.length; i++) {
            times += "<strong>" + data[i] + "</strong><br />"; //TODO add schedule button
        }
        if (times != '') {
            var dialog = $('<div id="limitDialog">Below are the suggested times to share your messages for maximum exposure<br /><br />' + times + '</div>'); //TODO i18n
            dialog.dialog({buttons: [
                {
                    text: 'Ok',
                    click: function() {
                        dialog.remove();
                    }
                }],
                close: function() {
                    dialog.remove();
                }
            });
        } else {
            info("There is no data about the best times to share yet");
        }
    });
}

function showDisconnectReasons(reasons) {
    if (reasons == '') {
        return;
    }

    var dialog = $('<div id="disconnectReasonsDialog">' + reasons + '</div>');
    dialog.dialog({buttons: [
        {
            text: 'Ok',
            click: function() {
                dialog.remove();
            }
        }],
        close: function() {
            dialog.remove();
        }
    });

    $.post(root + "users/clearDisconnectReasons");
}