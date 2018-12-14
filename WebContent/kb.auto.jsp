<%@page import="com.sozone.aeolus.common.config.Global"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<%
	String path = request.getContextPath();
	request.setAttribute("path", path);
	String adminPath = Global.getAdminPath();
%>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	
	<title>首页</title>

	<%@include file="static/include/inc.jsp"%>
	<link href="${path}/authorize/view/login/css/xz.all.css" rel="stylesheet" />
	
	<script type="text/javascript">
		$(function(){
			var themeCode = '${param.code}';
			var user_type = '${param.user_type}';
			
			var theme = '';
			if(themeCode=='10'){
				theme = "/fjs_gsgl";
			}
			if(themeCode=='20'){
				theme = "/fjs_ptgl";
			}
			if(themeCode=='30'){
				theme = "/fjs_sygc";
			}
			if(themeCode=='A01'){
				theme = "/xms_fwjz";
			}
			if(themeCode=='A02'){
				theme = "/xms_szgc";
			}
			var postUrl = "${path}/authorize/setTheme?theme="+theme;
			$.post(postUrl, {}, function(resData){
				//alert(resData);
				location.href="${path}/authorize/view/login/login-auto.html?user_type="+user_type;
			}, "text");
		})
	</script>
</head>

<body>
<div class="bg">
</div>
</body>
</html>