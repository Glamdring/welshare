<%@ page pageEncoding="UTF-8" %>

<div class="settingsMenu">
    <a href="<c:url value="/settings/account" />">
        <span class="settingsTab${page == 'account' ? 'Selected' : ''}">
            ${msg.accountSettings}
        </span>
    </a>

    <a href="<c:url value="/settings/social" />">
        <span class="settingsTab${page == 'social' ? 'Selected' : ''}">
            ${msg.socialSettings}
        </span>
    </a>
</div>