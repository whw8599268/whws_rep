<%@ page language="java" contentType="text/html; charset=utf-8"
	pageEncoding="utf-8"%>
<%
	response.setContentType("application/json");
	String inputRandomNum = request.getParameter("randomNum");
	String randomNum = (String) session.getAttribute("random");
	if (null != randomNum && randomNum.equals(inputRandomNum))
	{
		response.getWriter().print("{\"success\":true}");
		return;
	}
	response.getWriter().print("{\"success\":false}");
%>