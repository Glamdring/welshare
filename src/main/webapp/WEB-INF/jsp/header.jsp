<%@ include file="includes.jsp" %>
<%@ page pageEncoding="UTF-8" %><% out.clear(); %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <%@ include file="head.jsp" %>
        <script type="text/javascript">
            $(document).ready(function() {
                   $("#searchKeywords").labelify({labelledClass: "labelInside"});

                    <c:if test="${loggedUser != null}">
                    $("#searchExtraButtons").css("left",
                            $("#searchDropdown").position().left);
                    $("#searchExtraButtons").mouseleave(function() {
                        $("#searchExtraButtons").slideUp('fast');
                    });
                    $("#searchExtraButtons").parent().mouseleave(function() {
                        $("#searchExtraButtons").slideUp('fast');
                    });

                    $("#searchExtraButtons div").hover(function() {
                        $(this).css("background-color", "darkgrey");
                    }, function() {
                        $(this).css("background-color", "#D8E6EF");
                    });
                    $("#searchDropdown").mouseover(function() {
                        $("#searchExtraButtons").slideDown('fast').show();
                    });
                    </c:if>
                    $("#keywordsSearchButton").click(function() {
                        search('all');
                    })

                    $("#aboutPanelHover").mouseover(function() {
                        $("#aboutPanel").slideDown("slow");
                    });

                    $("#aboutPanelHover").click(function() {
                        $("#aboutPanel").slideUp("slow");
                    });

                    $("#searchKeywords").keydown(function (e) {
                        if (e.keyCode == 13) {
                          search('all');
                        }
                    });
                });
        </script>
    </head>
<body>
<div id="wrapper">
    <noscript>
        <div style="padding-top: 5px;">&nbsp;</div>
    </noscript>
    <div id="topPanel">
        <a href="${root}">
            <img src="${staticRoot}/images/logo.png" id="logo" alt="welshare" title="welshare" />
        </a>

        <div id="topMenu">
            <div id="topSearch">
                <input type="text" id="searchKeywords" title="${msg.search}..." />
                <c:if test="${loggedUser != null}">
                    <img id="searchDropdown" src="${staticRoot}/images/searchArrow.png" alt="${msg.search}" />
                </c:if>
                <img id="keywordsSearchButton" src="${staticRoot}/images/searchButton.png"
                    alt="${msg.search}" title="${msg.search}" <c:if test="${loggedUser == null}">style="left: 195px;" </c:if>/>
                <div id="searchExtraButtons">
                    <div onclick="search('all')">${msg.searchAll}</div>
                    <div onclick="search('stream')">${msg.searchStream}</div>
                    <div onclick="search('own')">${msg.searchOwn}</div>
                </div>
            </div>
            <div id="topMenuItems">
                <c:choose>
                <c:when test="${loggedUser != null}">
                    <a href="<c:url value="/" />">Home</a>
                    <a href="<c:url value="/${loggedUser.username}" />">Your page</a>
                    <a href="<c:url value="/settings/account" />" id="settingsLink">Settings</a>
                    <a href="<c:url value="/logout" />">Logout</a>
                </c:when>
                <c:otherwise>
                    <a href="<c:url value="/login" />">${msg.login}</a>
                    <a href="<c:url value="/signup" />">${msg.signup}</a>
                </c:otherwise>
                </c:choose>

                <a href="<c:url value="/users/top" />" id="topUsersLink">${msg.topUsers}</a>
            </div>
        </div>
    </div>
    <div id="mainWrapper">
        <div id="main">