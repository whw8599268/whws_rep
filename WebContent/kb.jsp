<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<%
	String path = request.getContextPath();
	request.setAttribute("path", path);
%>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<title>首页</title>
<%@include file="static/include/inc.jsp"%>

<!--[if lte IE 9 ]>
<script type="text/javascript">
location.href = "${path}/unsupport.jsp";
</script>
<![endif]-->

<script type="text/javascript">
	$(function() {
		location.href = "${path}/authorize/view/login/login.html";
	});
</script>
</head>

<body>

</body>
</html>