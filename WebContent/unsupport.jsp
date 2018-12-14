<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
	pageContext.setAttribute("basePath", basePath);
%>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>不被支持的浏览器</title>
<style type="text/css">
body {
	margin: 0px;
	padding: 0px;
	font-family: "微软雅黑", Arial, "Trebuchet MS", Verdana, Georgia,
		Baskerville, Palatino, Times;
	font-size: 16px;
}

div {
	margin-left: auto;
	margin-right: auto;
}

a {
	text-decoration: none;
	color: #1064A0;
}

a:hover {
	color: #0078D2;
}

img {
	border: none;
}

h1,h2,h3,h4 { /*    display:block;*/
	margin: 0;
	font-weight: normal;
	font-family: "微软雅黑", Arial, "Trebuchet MS", Helvetica, Verdana;
}

h1 {
	font-size: 44px;
	color: #0188DE;
	padding: 20px 0px 10px 0px;
}

h2 {
	color: #0188DE;
	font-size: 16px;
	padding: 10px 0px 40px 0px;
}

#page {
	padding: 5px 20px 5px 20px;
	font-size: 26px;
	width: 1000px;
}

p {
	line-height: 26px;
	text-indent: 2em;
}
</style>
</head>
<body>
    <div id="page" style="line-height: 30px;">
        <h1>抱歉!您的浏览器不支持开标辅助系统的相关功能!</h1>
        <hr>
        <p>尊敬的用户:</p>
        <p>您好!</p>
        <p>您当前的IE浏览器版本过低，无法支持开标辅助系统的相关功能。为了更稳定及更好的体验开标辅助系统，建议您使用谷歌、火狐、360极速模式、QQ极速模式、IE10(不要开启兼容模式)、IE11(不要开启兼容模式)这些浏览器。</p>
        <p>
            切换成极速模式了点这里：<a href="${basePath}">进入系统</a>
        </p>
    </div>
</body>
</html>