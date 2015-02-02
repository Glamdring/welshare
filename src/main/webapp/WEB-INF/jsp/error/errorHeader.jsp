<%@ include file="../includes.jsp" %>
<%@ page pageEncoding="UTF-8" %><% out.clear(); %>

<jsp:useBean id="random" class="java.util.Random" scope="application" />

<html>
    <head>
        <w:addResource resourcePath="${staticRoot}/styles/jquery-ui.css" contentType="text/css" />
        <w:addResource resourcePath="${staticRoot}/styles/main.css" contentType="text/css" />
        <w:addResource resourcePath="${staticRoot}/styles/fileuploader.css" contentType="text/css" />
        <w:addResource resourcePath="${staticRoot}/styles/fcbklistselection.css" contentType="text/css" />

        <w:flushResources contentType="text/css" assetsVersion="${appProperties['assets.version']}" />
    </head>
    <title>Welshare - ${msg.errorTitle}</title>
<body>

<c:set var="currentRandom" value="${random.nextInt()}" />
<div id="wrapper">
        <div id="topPanel">
            <a href="${root}">
                <c:choose>
                    <c:when test="${currentRandom % 2 == 0}">
                        <img src="${staticRoot}/images/logoBroken.png" id="logoBroken"/>
                    </c:when>
                    <c:otherwise>
                        <img src="${staticRoot}/images/logoBrokenFail.png" id="logoBroken"/>
                    </c:otherwise>
                </c:choose>
            </a>
        </div>
        <div id="singleColumn">
            <br />
            <fmt:setBundle basename="messages" />