<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="Features" />
<%@ include file="../../header.jsp" %>
<%@ page pageEncoding="UTF-8" %>
<h2 style="margin-top: 5px;">Features</h2>

<div class="featurePanel">
<h2>Unified stream</h2>
<img src="${staticRoot}/images/screenshots/stream.jpg" /><br />
The messages from all networks that you are connected to are displayed in a single stream (twitter, facebook, linkedIn)
</div>


<div class="featurePanel">
<h2>Share</h2>
<img src="${staticRoot}/images/screenshots/share.jpg" /><br />
You can share a message on all networks that you are connected to. You can deselect some of them for each message you share
</div>

<div class="featurePanel">
<h2>Reshare</h2>
If you click "reshare", the message is liked and reshared on all networks that you are connected to. The exact behaviour is explained in many details <a href="<c:url value="/info/reshare" />">here</a>
</div>

<div class="featurePanel">
<h2>Like/Retweet/...</h2>
The native button for each social network is preserved here - if you click "like" on a facebook message, it is simply liked. If you click "retweet" on a tweet, it is simply retweeted. This is a basic functionality if you don't need to use the more complex reshare button.
</div>

<div class="featurePanel">
<h2>Reshare dialog</h2>
<img src="${staticRoot}/images/screenshots/reshare.jpg" /><br />
This dialog appears when you click the cog icon next to a "reshare" button and gives you extra options:
<ul>
<li>which networks to reshare the message to. You can exclude the original network;</li>
<li>whether to share in addition to liking the message (if the origin network supports it). This lets you both like and share a facebook link, for example;</li>
<li>add a comment. In twitter this would be "nice one RT @hackernewsbot Why ...", and on facebook it will be "nice one: Why ... ";</li>
<li>edit the original message before resharing/retweeting it if it contains typos, or information that you want to trim.</li>
</ul>
</div>


<div class="featurePanel">
<h2>Social reputation</h2>
<img src="${staticRoot}/images/screenshots/reputation.jpg" /><br />
Each like and reply of a welshare message gives you points, and each external network that you connect to (twitter, facebook, etc.) gives you points. Thus a common social reputation score is obtained for each user. Then there are reputation rankings per country and per city.
</div>

<div class="featurePanel">
<h2>Schedule messages</h2>
<img src="${staticRoot}/images/screenshots/schedule.jpg" /><br />
Each message can be scheduled for a future time. You can schedule multiple messages for the future.
</div>

<div class="featurePanel">
<h2>Notifications</h2>
<img src="${staticRoot}/images/screenshots/notifications.jpg" /><br />
When someone replies to you, likes your message, retweets, +1s, tags, etc. on any network, you see these events in a unified notifications panel.
</div>

<div class="featurePanel">
<h2>Missed important messages</h2>
<img src="${staticRoot}/images/screenshots/missedimportant.jpg" /><br />
Get a list of the most popular messages that appeared in your stream while you were online. <a href="http://blog.welshare.com/?p=26">Read more here</a>.
</div>

<div class="featurePanel">
<h2>Filters</h2>
<img src="${staticRoot}/images/screenshots/filters.jpg" /><br />
You can configure keywords that you don't want to see, and whenever a message containing them is received, it is not displayed in the stream. That way you can filter annoying twitter hashtags, foursquare checkins, etc.
</div>

<div class="featurePanel">
<h2>Interested in</h2>
<img src="${staticRoot}/images/screenshots/interestedin.jpg" /><br />
Messages that contain these keywords are highlighted in the stream. Useful for finding important things while just scanning the stream.
</div>

<div class="featurePanel">
<h2>Thresholds</h2>
<img src="${staticRoot}/images/screenshots/threshold.jpg" /><br />
You can set a threshold for each user, regardless of the network. And you will see only messages that have a score above the configured threshold. That way you can filter out some "spammy" people, but still see their best messages
</div>

<div class="featurePanel">
<h2>Charts</h2>
<img src="${staticRoot}/images/screenshots/charts.jpg" /><br />
Simple charts showing how many messages you've shared on each network, how many likes/retweets/+1s/etc. and replies they received
</div>


<div class="featurePanel">
<h2>Limit online presence</h2>
<img src="${staticRoot}/images/screenshots/limit.jpg" /><br />
You can optionally limit your daily online presence. Social networks are addictive and this feature is useful if you want to focus on more productive things
</div>

<div class="featurePanel">
<h2>Search</h2>
<img src="${staticRoot}/images/screenshots/search.jpg" /><br />
You can search your own messages, your stream and the whole welshare. The search only returns results from messages shared on welshare.
</div>

<div class="featurePanel">
More features:
<ul>
<li><strong>twitter followers currently active</strong> - see how many of your followers are currently active (tweeted recently). That way you can know if it's a good time to tweet something that you would like to have greater exposure</li>
<li><strong>old messages for sharing again</strong> - welshare picks some of your old messages and suggest them for sharing again. It's sometimes a good idea to share a good thought twice</li>
<li><strong>top recent messages</strong> - this is a list of your own top recent messages (having most likes/retweets/etc.)</li>
<li><strong>shorten URLs</strong> - when you share a message you can choose to shorten the links in the message. By default wshr.eu is used, but you can configure bit.ly</li>
<li><strong>viral graph</strong> - when shortening urls you have two additional options - to have a special bar at the top, which allows easier resharing, and to track the viral graph of the link (again through that top bar). You can read more about the viral graph <a href="http://blog.welshare.com/?p=17">here</a></li>
<li><strong>upload pictures</strong> - you can attach multiple pictures to each message. They are shared on external networks as well (if possible)</li>
<li><strong>translate</strong> - you can translate each message to your preferred language. This allows you to follow people that usually tweet in a language you don't understand</li>
<li><strong>delete everywhere</strong> - if a messages was shared through welshare, when you delete it, you have an option to delete from all networks it was shared to</li>
<li><strong>edit</strong> - you can edit your messages, including the messages that were sent to other networks. If the other networks don't support editing, the messages is deleted and reposted (unless someone has already interacted with it)</li>
<li><strong>daily email</strong> - you can optionally receive a daily email containing the most popular messages in your stream during the previous day</li>
</ul>
</div>
<%@ include file="../../footer.jsp" %>