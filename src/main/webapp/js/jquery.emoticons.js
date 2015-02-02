/*
 * jQuery emoticons plug-in 0.5
 *
 * Copyright (c) 2009 Sebastian Kreft
 *
 * Licensed under the GPL license:
 * http://www.gnu.org/licenses/gpl.html
 *
 * Replaces occurrences of emoticons with the corresponding image
 * images are of class emoticonimg so they can be styled
 */
jQuery.fn.emoticons = function(icon_folder) {
    /* emoticons is the folder where the emoticons are stored*/
    var icon_folder = icon_folder || "emoticons";
    //var settings = jQuery.extend({emoticons: "emoticons"}, options);
    /* keys are the emoticons
     * values are the ways of writing the emoticon
     *
     * for each emoticons should be an image with filename
     * 'face-emoticon.png'
     * so for example, if we want to add a cow emoticon
     * we add "cow" : Array("(C)") to emotes
     * and an image called 'face-cow.png' under the emoticons folder
     */
    var emotes = {"smile": Array(":-)",":)","=]","=)", ":&gt;"),
                  "sad": Array(":-(",":(","=(",":[",":&lt;"),
                  "cry": Array(":'(",":'-(", ";("),
                  "grin": Array(":D","=D","xD"),
                  "wink": Array(";-)",";)",";]","*)"),
                  "tongue": Array(":P", ":-P"),
                  "eek": Array(":shock:"),
                  "blish": Array(":oops:", "(blush)"),
                  "confused": Array(":?", ":-?"),
                  "surprised": Array(":O",":-O",":o", ":-o"),
                  "twisted": Array("]:)"),
                  "cool": Array("8-)"),
                  "idea": Array(":idea:"),
                  "lol": Array(":lol:", "(rofl)"),
                  "mad": Array(":mad:"),
                  "rolleyes": Array("(think)", ":rolleyes:"),
                  "neutral": Array(":|", ":-|")};

    /* Replaces all ocurrences of emoticons in the given html with images
     */
    function emoticons(html){
        for(var emoticon in emotes){
            for(var i = 0; i < emotes[emoticon].length; i++){
                /* css class of images is emoticonimg for styling them*/
                html = html.replace(" " + emotes[emoticon][i], " <img src=\""+icon_folder+"/"+emoticon+".gif\" class=\"emoticonimg\" alt=\""+emotes[emoticon][i]+"\"/>","g");
            }
        }
        return html;
    }
    return this.each(function(){
        $(this).html(emoticons($(this).html()));
    });
};