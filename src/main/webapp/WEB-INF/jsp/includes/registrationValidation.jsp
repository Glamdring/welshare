<%@ page pageEncoding="UTF-8" %>
<script type="text/javascript">

var RED = "#FFCFCF";
var GREEN = "#F0FEE9";
var GREY = "#D3D3D3";
var verifying = false;


$("#username").live("blur", function() {
    var username = $("#username").val();
    if (username.length > 0 && $("#username").valid()) {
        $("#usernameMessage").text("Verifying username...").css("background-color", GREY);
        $("#usernameMessage").prepend('<img src="${staticRoot}/images/ajax_loading.gif" style="margin-right: 5px;" />');
        verifying = true;
        $.getJSON("${root}account/checkUsername/" + username,
            function(response) {
                verifying = false;
                if (response == false) {
                    $("#usernameMessage").text("Username taken").css("background-color", RED).show();
                } else {
                    $("#usernameMessage").text("Username free").css("background-color", GREEN).show();
                }
            }
        );
    }
});

$("#email").live("blur", function() {
    var email = $("#email").val();
    if (email.length > 0 && $("#email").valid()) {
        $("#emailMessage").text("Verifying email...").css("background-color", GREY);
        $("#emailMessage").prepend('<img src="${staticRoot}/images/ajax_loading.gif" style="margin-right: 5px;" />');
        verifying = true;
        $.getJSON("${root}account/checkEmail", {email:email},
            function(response) {
                verifying = false;
                if (response == false) {
                    $("#emailMessage").text("Email taken").css("background-color", RED).show();
                } else {
                    $("#emailMessage").text("Email OK").css("background-color", GREEN).show();
                }
            }
        );
    }
});

    var signupInfos = {
            username: "${msg.usernameInfo}",
            password: "${msg.passwordInfo}",
            names: "${msg.namesInfo}",
            email: "${msg.emailInfo}"
    }

    $(document).ready(function(){
       $("#signupForm").validate({
          rules: {
              username: {
                  required: true,
                  minlength: 4,
                  maxlength: 30
              },
              password: {
                  required: true,
                  minlength: 6,
                  maxlength: 60
              },
              email: {
                  required: true,
                  email: true
              },
              names: {
                  required: true
              }
          },
          messages : {
              username: "${msg.usernameValidationMessage}",
              password: "${msg.passwordValidationMessage}",
              email: "${msg.emailValidationMessage}",
              names: "${msg.namesValidationMessage}"
          },
          errorPlacement: function(error, element) {
              var inputId = $(element).attr("id");
              var target = $("#" + inputId + "Message");
              target.text(error.text());
              target.css("background-color", RED);
              target.show();
          }
       });

       $(".signupMessage").corner();
       $(".textInput").live("click", function(event) {
           hideSignupMessages();

           if (verifying) {
               $("#usernameMessage").show();
           }
           var inputId = $(event.target).attr("id");
           var target = $("#" + inputId + "Message");
           target.text(signupInfos[inputId]).css("background-color", GREY);
           target.show();
       });
       $(".textInput").live("blur", function(event) {
          if ($(event.target).valid() && !verifying) {
              $("#" + $(event.target).attr("id") + "Message").hide();
          }
          hideSignupMessages();
       });
     });

    function hideSignupMessages() {
       $(".signupMessage").each(function() {
           if (colorToHex($(this).css("background-color")) == GREY) {
               $(this).hide();
           }
       });
    }
</script>