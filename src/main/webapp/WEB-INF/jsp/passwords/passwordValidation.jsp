<script type="text/javascript">
var RED = "#FFCFCF";
var GREEN = "#F0FEE9";
var GREY = "#D3D3D3";

    $(document).ready(function(){
       $("#passwordForm").validate({
          rules: {
              newPassword: {
                  required: true,
                  minlength: 4,
                  maxlength: 40
              }
          },
          messages : {
              newPassword: "${msg.passwordValidationMessage}"
          },
          errorPlacement: function(error, element) {
              var inputId = $(element).attr("id");
              var target = $("#" + inputId + "Message");
              target.text(error.text());
              target.css("background-color", RED);
              target.show();
          }
       });

       $(".textInput").live("blur", function(event) {
           $(event.target).valid();
           $(".textInput").each(function() {
               if (colorToHex($(this).css("background-color")) == GREY) {
                   $(this).hide();
               }
           });
        });
    });
</script>