<%@ include file="../includes.jsp" %>
<%@ page pageEncoding="UTF-8" %>
var countries = new Array(${fn:length(countryList)});
var countryCodes = new Array(${fn:length(countryList)});
<c:forEach var="country" items="${countryList}" varStatus="i">
countries[${i.index}] = "${country.name}";
countryCodes["${country.name}"] = "${country}";
</c:forEach>