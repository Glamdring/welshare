<%@ include file="../includes.jsp" %>
<%-- Relevant discussion here: http://stackoverflow.com/questions/7503765/how-to-send-the-browser-to-an-error-page-if-part-of-the-response-has-been-sent-c --%>

<!--
    this is needed, otherwise the exception is not visible anywhere
    getting the exception manually, because of the page is declared isErrorPage="true",
    then the engine sets error status, which in turn breaks the GZIP filter
-->
<%
java.lang.Throwable exception = (java.lang.Throwable) request.getAttribute("javax.servlet.error.exception");
if (exception == null) {
    exception = (java.lang.Throwable) request.getAttribute("javax.servlet.jsp.jspException");
}
exception.printStackTrace();
%>
"</script> <!-- closing any potentially open <script> tag before this -->
<script type="text/javascript">
window.location.href = "<c:url value="/error/500" />";
</script>