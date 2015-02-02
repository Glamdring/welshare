<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ attribute name="bestTimesToShare" required="true" rtexprvalue="true" type="com.welshare.service.MessageService.BestTimesToShare" %>
<%@ attribute name="prefix" required="true" rtexprvalue="true" %>
<%@ attribute name="max" required="true" rtexprvalue="true" %>
<%@ attribute name="divId" required="true" rtexprvalue="true" %>
<%@ attribute name="weekdayTitle" required="true" rtexprvalue="true" %>
<%@ attribute name="weekendTitle" required="true" rtexprvalue="true" %>
<%@ attribute name="title" required="true" rtexprvalue="true" %>

<c:set var="weekdayEntries" value="${bestTimesToShare.weekdays}" />
<c:set var="weekendEntries" value="${bestTimesToShare.weekends}" />

<c:set var="weekdaysSize" value="${weekdayEntries.size()}"/>
<c:if test="${weekdaysSize < 48 && weekendEntries.size() < 48}">
    <fmt:formatNumber var="hoursRemaining" value="${(48 - weekdaysSize) / 2}" maxFractionDigits="0" />
    <c:out value="${msg.stillCollectingData} ${applicationScope.socialNetworksByPrefix[prefix].name}. ${msg.stillCollectingDataSuffix} (${msg.around} ${hoursRemaining} ${msg.hours})" /><br /><br />
</c:if>
<c:if test="${weekdaysSize == 48 || weekendEntries.size() == 48}">

<script type="text/javascript">
    var ${prefix}weekdayEntries = new Array();
    var ${prefix}weekendEntries = new Array();
    var ${prefix}XTicks = new Array();
    var ${prefix}YTicks = new Array();

    <c:forEach items="${weekdayEntries}" var="entry">
        ${prefix}weekdayEntries.push([${entry.minutes}, ${entry.activePercentage}]);
    </c:forEach>
    <c:forEach items="${weekendEntries}" var="entry">
        ${prefix}weekendEntries.push([${entry.minutes}, ${entry.activePercentage}]);
    </c:forEach>

    <c:forEach items="${weekdayEntries}" var="entry" varStatus="status">
        <c:if test="${status.index % 2 == 0}">
            ${prefix}XTicks.push([${entry.minutes}, <fmt:formatNumber value="${entry.minutes / 60}" maxFractionDigits="0" />]);
        </c:if>
    </c:forEach>

    <c:forEach begin="0" end="${max}" step="1" varStatus="status">
    ${prefix}YTicks.push(${status.index}, '${status.index}');</c:forEach>
    ${prefix}YTicks.push(${max + 1}, '${max + 1}');

    $(document).ready(function() {
        $.jqplot("${divId}", [${prefix}weekdayEntries, ${prefix}weekendEntries],
            {
              legend:{show:true},
              title: "${title}",
              series: [
                  {label:'${weekdayTitle}'},
                  {label:'${weekendTitle}'}
              ],
              axes: {
                  xaxis:{ticks: ${prefix}XTicks, tickOptions:{formatString:'%d'}, label: '${msg.bestTimeToShareXLabel}'},
                  yaxis:{ticks: ${prefix}YTicks, showTicks: false}
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

<div id="${divId}" style="margin-bottom: 7px; border-style: solid; border-width: 1px;"></div>

</c:if>