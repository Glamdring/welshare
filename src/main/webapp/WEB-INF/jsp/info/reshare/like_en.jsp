<%@ include file="../../header.jsp" %>
<%@ page pageEncoding="UTF-8" %>
<style>
li {
    padding-bottom: 12px;
}
ul {
    padding-top: 12px;
}
</style>

<h1>The unified "reshare" button</h1>
<p>Welshare has a unified "reshare" button which means it needs a consistent behaviour across external networks (facebook, twitter). This page explains what to expect</p>

<ul>
<li>if the message was originally shared on welshare, when a user ("current user") reshares it:
    <ul>
        <li>a new message is posted on welshare that consits of "UserX liked a message UserY: " followed by an optional comment and the liked message</li>
        <li>if the author of the liked message has spread it to other networks (facebook, twitter), and the current user has associated their account with any of these services:
            <ul>
                <li>on twitter the message is retweeted - via a normal retweet, if no comment is specified when liking, or with "comment RT @user: message" if a comment is specified (the format is configurable)
                <li>on facebook the message is liked if no comment is specified, or is posted on the current user's wall, in the form: "comment: message via original user", if a comment is specified (the format is configurable)
            </ul>
        <li>if the user has configured to share his likes on other networks, and the message did not meet the conditions in the previous point, the message is shared as a new message there, ending with "via original user"
    </ul>
<li>if the message comes from twitter (it is shown in the current user's stream), when the current user reshares it:
    <ul>
        <li>the message is retweeted - via a normal retweet, if no comment is specified when liking, or with "comment RT @user: message" if a comment is specified</li>
        <li>the message is posted on welshare, ending with "via original user"</li>
        <li>if the user has configured to share his likes on other networks, the message is shared as a new message there, ending with "via original user"
    </ul>
</li>
<li>if the message comes from facebook (it is shown in the current user's stream), when the current user reshares it:
    <ul>
        <li>the message is liked on facebook if no comment is specified, or is posted on the current user's wall, in the form: "comment: message via original user", if a comment is specified</li>
        <li>the message is posted on welshare, ending with "via original user"</li>
        <li>if the user has configured to share his likes on other networks, the message is shared as a new message there, ending with "via original user"</li>
    </ul>
</li>
</ul>
<%@ include file="../../footer.jsp" %>