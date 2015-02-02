<%@ page pageEncoding="UTF-8" %>
<link href="${staticRoot}/styles/openid.css" type="text/css" media="screen" rel="stylesheet">

<script type="text/javascript" src="${staticRoot}/js/jquery.openid.js"></script>
<script type="text/javascript">
    $(document).ready(function() {
        $(".openid").openid();
        $(".openid").show();
        //openid.setDemoMode(true); //Stops form submission for client javascript-only test purposes
    });
</script>
<div style="clear: both;">
<c:out value="${externalAuthType} ${msg.externalWith}:" />
</div>

<form class="openid" method="post" action="<c:url value="/externalAuth/openid/connect" />?login=${login}" style="display: none;">
  <div>
  <a class="externalProvider" id="twitterAuthLink" title="Twitter" href="<c:url value="/externalAuth/twitter/connect" />?login=${login}">
      <img src="${staticRoot}/images/openid/twitterW.png" alt="Twitter" class="linkedImage" /></a>

  <a class="externalProvider" id="facebookAuthLink" title="Facebook" href="<c:url value="/externalAuth/facebook/connect" />?login=${login}">
      <img src="${staticRoot}/images/openid/facebookW.png" alt="Facebook" class="linkedImage" /></a>

    <a class="externalProvider" id="googlePlusAuthLink" title="Google+" href="<c:url value="/externalAuth/googlePlus/connect" />?login=${login}">
      <img src="${staticRoot}/images/openid/googlePlusW.png" alt="Google+" class="linkedImage" /></a>

  <a class="externalProvider" id="linkedInAuthLink" title="LinkedIn" href="<c:url value="/externalAuth/linkedIn/connect" />?login=${login}">
      <img src="${staticRoot}/images/openid/linkedInW.png" alt="LinkedIn" class="linkedImage" /></a>

  <ul class="providers">
  <li class="direct" title="Google">
        <img src="${staticRoot}/images/openid/googleW.png" alt="Google" /><span>https://www.google.com/accounts/o8/id</span></li>
  <li class="direct" title="Yahoo">
        <img src="${staticRoot}/images/openid/yahooW.png" alt="Yahoo" /><span>http://yahoo.com/</span></li>
  <li class="openid" title="OpenID">
        <img src="${staticRoot}/images/openid/openidW.png" alt="OpenID" /><span><strong>http://{your-openid-url}</strong></span></li>
  <li class="username" title="MyOpenID user name">
        <img src="${staticRoot}/images/openid/myopenid.png" alt="MyOpenID" /><span>http://<strong>username</strong>.myopenid.com/</span></li>
  <li class="username" title="Flickr user name">
        <img src="${staticRoot}/images/openid/flickr.png" alt="Flickr" /><span>http://flickr.com/<strong>username</strong>/</span></li>
  <li class="username" title="Wordpress blog name">
        <img src="${staticRoot}/images/openid/wordpress.png" alt="Wordpress" /><span>http://<strong>username</strong>.wordpress.com</span></li>
  <li class="username" title="Blogger blog name">
        <img src="${staticRoot}/images/openid/blogger.png" alt="Blogger" /><span>http://<strong>username</strong>.blogspot.com/</span></li>
  <li class="username" title="LiveJournal blog name">
        <img src="${staticRoot}/images/openid/livejournal.png" alt="LiveJournal" /><span>http://<strong>username</strong>.livejournal.com</span></li>
  </ul></div>
  <input type="hidden" name="timezoneId" id="externalTimezoneId" />
  <fieldset style="display: none;">
  <label for="openid_username">Enter your <span>Provider user name</span></label>
  <div><span></span><input type="text" name="openid_username" id="openid_username" /><span></span>
  <input type="submit" value="${externalAuthType}" /></div>
  </fieldset>
  <fieldset style="display: none;">
  <label for="openid_identifier">Enter your <a class="openid_logo" href="http://openid.net">OpenID</a></label>
  <div><input type="text" name="openid_identifier" id="openid_identifier" />
  <input type="submit" value="Login" /></div>
  </fieldset>
</form>