<%@ page language="java" pageEncoding="utf-8"%>
<!-- easyui 主题扩展 -->
<link href="${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/easyui.css" rel="stylesheet" type="text/css">
<link href="${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/icon.css" rel="stylesheet" type="text/css">
<link href="${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/easyui_animation.css" rel="stylesheet" type="text/css">
<link href="${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/easyui_plus.css" rel="stylesheet" type="text/css">
<link id="AeolusUITheme" href="${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/insdep_theme_default.css" rel="stylesheet" type="text/css">
<link href="${pageContext.request.contextPath}/static/res/font-awesome-4.7.0/css/font-awesome.min.css" rel="stylesheet" type="text/css">
<script type="text/javascript" src="${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/jquery.insdep-extend.min.js"></script>
<script type="text/javascript">
var AppStyleUtils = window.AppStyleUtils || {};
AppStyleUtils = {
   removeStyleSheet : function(id){
       var existing = document.getElementById(id);
       if(existing){
           existing.parentNode.removeChild(existing);
       }
   },

   swapStyleSheet : function(id, url){
       this.removeStyleSheet(id);
       var ss = document.createElement("link");
       ss.setAttribute("rel", "stylesheet");
       ss.setAttribute("type", "text/css");
       ss.setAttribute("id", id);
       ss.setAttribute("href", url);
       document.getElementsByTagName("head")[0].appendChild(ss);
   }
}

function setCookie(b, d, a, f, c, e) {
	document.cookie = b + "=" + escape(d) + ((a) ? "; expires=" + a.toGMTString() : "") + ((f) ? "; path=" + f : "") + ((c) ? "; domain=" + c : "") + ((e) ? "; secure" : "");
}
function getCookie(b) {
	var d = b + "=";
	var e = document.cookie.indexOf(d);
	if (e == -1) {
		return null;
	}
	var a = document.cookie.indexOf(";", e + d.length);
	if (a == -1) {
		a = document.cookie.length;
	}
	var c = document.cookie.substring(e + d.length, a);
	return unescape(c);
}
function deleteCookie(a, c, b) {
	if (getCookie(a)) {
		document.cookie = a + "=" + ((c) ? "; path=" + c : "") + ((b) ? "; domain=" + b : "") + "; expires=Thu, 01-Jan-70 00:00:01 GMT";
	}
}
/**
 * 设置全局字体大小
 */
function setGlobalFontSize(fontsize){
	if (fontsize==null || fontsize=='') return;
    var a = new Date();
	a.setDate(a.getDate() + 100*24*3600*1000);
	setCookie("GlobalFontSize", fontsize, a, "");
	AppStyleUtils.swapStyleSheet('GlobalFontSize', '${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/insdep_theme_font_'+fontsize+'.css');
	if ($("#content").find('iframe').length>0){
		$("#content").find('iframe').each(function(){
			var thisobj = $(this);
			if(thisobj[0].contentWindow.AppStyleUtils) {
				thisobj[0].contentWindow.AppStyleUtils.swapStyleSheet('GlobalFontSize', '${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/insdep_theme_font_'+fontsize+'.css');
			}
		});
	}
}

/**
 * 设置全局主题
 */
function setGlobalTheme(theme){
	if (theme==null || theme=='') return;
    var a = new Date();
	a.setDate(a.getDate() + 100*24*3600*1000);
	setCookie("AeolusUITheme", theme, a, "");
	AppStyleUtils.swapStyleSheet('AeolusUITheme', '${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/insdep_theme_'+theme+'.css');
}

$(function(){
	var aeolusUITheme=top.getCookie('AeolusUITheme');
	if(aeolusUITheme==null || aeolusUITheme==''){ aeolusUITheme = "default"; }
	AppStyleUtils.swapStyleSheet('AeolusUITheme', '${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/insdep_theme_'+aeolusUITheme+'.css');
	
	var globalFontSize=top.getCookie('GlobalFontSize');
	if(globalFontSize==null || globalFontSize==''){ globalFontSize = "14"; }
	AppStyleUtils.swapStyleSheet('GlobalFontSize', '${pageContext.request.contextPath}/static/res/jquery-easyui/themes/insdep/insdep_theme_font_'+globalFontSize+'.css');
});
</script>