function info(message) {
    $.pnotify({pnotify_text: message, pnotify_history: false, pnotify_delay: message.length / 30 * 1600});
}

function timedInfo(message, timeInMillis) {
    $.pnotify({pnotify_text: message, pnotify_history: false, pnotify_delay: timeInMillis});
}