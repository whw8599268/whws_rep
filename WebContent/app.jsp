<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<%
	String path = request.getContextPath();
	request.setAttribute("path", path);
%>
<head>
<meta charset="UTF-8">
<title>E共享电子开标辅助系统</title>
<script type="text/javascript">
	location.href = "${path}/mobile-app/index.html";
</script>
</head>
<body>
</body>
</html>