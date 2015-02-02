(function($){
    $.fn.toggleCheckbox = function(backgroundColor, borderStyle) {
        var check = $(this).find("input");
        var image = $(this).find("img");
        check.hide();
        $(this).css("border-right-style", borderStyle != undefined? borderStyle : "inset");
        $(this).css("border-bottom-style", borderStyle != undefined? borderStyle : "inset");
        $(this).css("border-width", "1px");
        $(this).css("border-collapse", "collapse");
        $(this).css("padding", "0px");
        var width = image.width();
        var height = image.height();
        if (width == 0) {
            width = 20; //default TODO make configurable
        }
        if (height == 0) {
            height = 20;
        }

        $(this).css("width", width + "px");
        $(this).css("background-color", backgroundColor != undefined ? backgroundColor : "#EDEDED");
        $(this).css("cursor", "pointer");
        $(this).css("height", (height + 5) + "px");
        image.css("cursor", "pointer");
        image.css("margin-top", check.attr("checked") ? "5px" : "0px");
        if (!check.is(":checked")) {
            image.fadeTo("fast", 0.5);
        }

        $(this).click(function() {
            actualToggle(image, check, !check.is(":checked"));
        });
    };

    $.fn.toggleCheckboxValue = function(on) {
        var check = $(this).find("input");
        var image = $(this).find("img");
        actualToggle(image, check, on);
    };

    function actualToggle(image, check, on) {
        if (on) {
            check.attr("checked", "checked");
            image.fadeTo("fast", 1);
            image.css("margin-top", "5px");
        } else {
            check.removeAttr("checked");
            image.css("margin-top", "0px");
            image.fadeTo("fast", 0.5);
        }
    }
})(jQuery);