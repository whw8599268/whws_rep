<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<%
	String path = request.getContextPath();
	request.setAttribute("path", path);
%>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<title>福建CA老版介质证书登录页</title>
<%@include file="static/include/inc.jsp"%>
<style type="text/css">
<!--
.user-mod {
	background-color: #FFF;
	height: 380px;
	width: 380px;
	margin: 0 auto;
}

.user-login {
	margin: 40px;
	width: 300px;
}

.role-zb {
	float: left;
	height: 116px;
	width: 298px;
	margin-bottom: 20px;
	margin-top: 10px;
	cursor: pointer;
}

.role-tb {
	float: left;
	height: 116px;
	width: 298px;
	margin-top: 20px;
	cursor: pointer;
}
-->
</style>
<script type="text/javascript">
	function login(type) {
		if (0 == type) {
			//招标人
			location.href = "${path}/authorize/o2c/auth?user_type=0&key=0";
		}
		if (1 == type) {
			//投标人
			location.href = "${path}/authorize/o2c/auth?user_type=1&key=0";
		}
	}
</script>
</head>

<body style="text-align: center;">
    <div class="user-mod">
        <div class="user-login">
            <span class="role-zb" onclick="login(0)"><img src="${path}/authorize/view/login/images/role_zb.png" width="298" height="116" /></span> <span class="role-tb" onclick="login(1)"><img src="${path}/authorize/view/login/images/role_tb.png" width="298" height="116" /></span>
        </div>
    </div>
</body>
</html>