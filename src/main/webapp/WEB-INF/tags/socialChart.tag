<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ attribute name="messages" required="true" rtexprvalue="true" type="java.util.Map" %>
<%@ attribute name="likes" required="true" rtexprvalue="true" type="java.util.Map" %>
<%@ attribute name="replies" required="true" rtexprvalue="true" type="java.util.Map" %>
<%@ attribute name="prefix" required="true" rtexprvalue="true" %>
<%@ attribute name="maxCount" required="true" rtexprvalue="true" %>
<%@ attribute name="divId" required="true" rtexprvalue="true" %>
<%@ attribute name="title" required="true" rtexprvalue="true" %>
<%@ attribute name="messagesTitle" required="true" rtexprvalue="true" %>
<%@ attribute name="repliesTitle" required="true" rtexprvalue="true" %>
<%@ attribute name="likesTitle" required="true" rtexprvalue="true" %>

<c:if test="${messages.size() > 0}">

<script type="text/javascript">
    var ${prefix}messages = new Array();
    var ${prefix}likes = new Array();
    var ${prefix}replies = new Array();
    var ${prefix}XTicks = new Array();
    var ${prefix}YTicks = new Array();

    <c:forEach items="${messages}" var="entry">
    ${prefix}messages.push([${entry.key.millis}, ${entry.value}]);</c:forEach>
    <c:forEach items="${likes}" var="entry">
    ${prefix}likes.push([${entry.key.millis}, ${entry.value}]);</c:forEach>
    <c:forEach items="${replies}" var="entry">
    ${prefix}replies.push([${entry.key.millis}, ${entry.value}]);</c:forEach>

    <c:forEach items="${messages}" var="entry" step="${messages.size() / 10 + 1}">
    ${prefix}XTicks.push([${entry.key.millis}, '<fmt:formatDate value="${entry.key.toDate()}" pattern="dd.MM" />']);</c:forEach>

    <c:forEach begin="0" end="${maxCount}" step="${maxCount / 5 >= 1 ? maxCount / 5 : 1}" varStatus="status">
    ${prefix}YTicks.push(${status.index}, '${status.index}');</c:forEach>
    ${prefix}YTicks.push(${maxCount + 1}, '${maxCount + 1}');

    $(document).ready(function() {
        $.jqplot("${divId}", [${prefix}messages, ${prefix}replies, ${prefix}likes],
            {
              legend:{show:true},
              title: '${title}',
              series: [
                  {label:'${messagesTitle}'},
                  {label:'${repliesTitle}'},
                  {label:'${likesTitle}'}
              ],
              axes: {
                  xaxis:{ticks: ${prefix}XTicks},
                  yaxis:{ticks: ${prefix}YTicks, tickOptions:{formatString:'%d'}}
              },
              highlighter: {
                  show:true,
                  sizeAdjust: 7.5,
                  tooltipAxes: 'y',
                  tooltipSeparator: ''
              }
            }
        );
    });
</script>

<div id="${divId}" style="margin-bottom: 7px;"></div>

</c:if>