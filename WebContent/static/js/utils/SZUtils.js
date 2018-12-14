/**
 * @file 提供常用方法，引用路径：&lt;script type="text/javascript" src="${path}/static/js/utils/SZUtils.js">&lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 */

/**
 * @version v1.0.0
 * @author hyc
 * @since 2017-05-02
 * @desc SZUtils 提供常用方法
 * @version v1.0.0
 * @namespace SZUtils
 * @example SZUtils.getProjectPath()
 * @example SZUtils.getProjectName();
 * @example SZUtils.getJsUrlParam(jsId, paramName);
 * @example SZUtils.formatString('字符串{0}字符串{1}字符串','第一个变量','第二个变量');
 * @example SZUtils.getUrlParam(name)
 * @example SZUtils.getList(str)
 * @example SZUtils.isLessThanIe8();
 * @example SZUtils.addNameSpace ('jQuery.bbb.ccc','jQuery.eee.fff');
 */
var SZUtils = SZUtils || {};

/**
 * @desc 获取项目路径 http://IP:端口/项目名称
 * @example SZUtils.getProjectPath()
 * @returns {string}项目路径
 */
SZUtils.getProjectPath = function() {
	var curWwwPath = window.document.location.href;
	var pathName = window.document.location.pathname;
	var pos = curWwwPath.indexOf(pathName);
	var localhostPath = curWwwPath.substring(0, pos);
	var projectName = pathName
			.substring(0, pathName.substr(1).indexOf('/') + 1);
	return (localhostPath + projectName);
};

/**
 * @desc 获取项目名称
 * 
 * @example SZUtils.getProjectName();
 * @returns {string}项目名称
 */
SZUtils.getProjectName = function() {
	var path = window.document.location.pathname;
	if (path && path.indexOf('\/', 1) > 0) {
		return path.substring(1, path.indexOf('\/', 1));
	}
	return '';
};

/**
 * @desc 获取跟在JS地址后的参数
 * 
 * @param {string}jsId
 *            JS的ID
 * @param {string}paramName
 *            参数名称
 * @example SZUtils.getJsUrlParam(jsId, paramName);
 * @returns {string}参数值，无则返回空
 */
SZUtils.getJsUrlParam = function(jsId, paramName) {
	var reg = new RegExp("(^|\\?|&)" + paramName + "=([^&]*)(\\s|&|$)", "i");
	if (reg.test($("#" + jsId).attr("src"))) {
		return RegExp.$2;
	} else {
		return '';
	}
};

/**
 * @desc 增加formatString功能
 * 
 * @param {string}
 *            str 字符串
 * @example SZUtils.formatString('字符串{0}字符串{1}字符串','第一个变量','第二个变量');
 * @returns {string} 格式化后的字符串
 */
SZUtils.formatString = function(str) {
	if (!str || typeof (str) !== "string") {
		return '';
	}
	for ( var i = 0; i < arguments.length - 1; i++) {
		str = str.replace("{" + i + "}", arguments[i + 1]);
	}
	return str;
};

/**
 * @desc 获得URL参数
 * 
 * @param {string}name
 *            参数名称
 * @example SZUtils.getUrlParam(name)
 * @return {String}对应名称的值
 */
SZUtils.getUrlParam = function(name) {
	var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
	var r = window.location.search.substr(1).match(reg);
	if (r != null)
		return unescape(r[2]);
	return '';
};

/**
 * @desc 接收一个以逗号分割的字符串， 返回List，list里每一项都是一个字符串
 * @param {string}str
 *            以逗号分割的字符串
 * @example SZUtils.getList(str)
 * @returns {Array}数组
 */
SZUtils.getList = function(str) {
	if (!str || typeof (str) !== 'string') {
		return [];
	}
	var values = [];
	var t = str.split(',');
	for ( var i = 0; i < t.length; i++) {
		/* 避免他将ID当成数字 */
		values.push('' + t[i]);
	}
	return values;
};

/**
 * @desc 判断浏览器是否是IE并且版本小于8
 * 
 * @requires jQuery
 * @example SZUtils.isLessThanIe8();
 * @returns {boolean}true/false
 */
SZUtils.isLessThanIe8 = function() {
	return ($.browser.msie && $.browser.version < 8);
};

/**
 * @desc 增加命名空间功能
 * 
 * @example SZUtils.addNameSpace ('jQuery.bbb.ccc','jQuery.eee.fff');
 * @returns {object}最后一个命名空间的根对象
 */
SZUtils.addNameSpace = function() {
	var o = {}, d;
	for ( var i = 0; i < arguments.length; i++) {
		d = arguments[i].split(".");
		o = window[d[0]] = window[d[0]] || {};
		for ( var k = 0; k < d.slice(1).length; k++) {
			o = o[d[k + 1]] = o[d[k + 1]] || {};
		}
	}
	return o;
};

/**
 * @desc 解决IE input框 
 * @param {jQueryObject} obejct 对象
 * @example SZUtils.inputFocus()
 */
SZUtils.inputFocus = function(obejct) {
	if(obejct.length<1){
		var $text = $("input:text:first");
		if ($text.length > 0) {
			$text.focus();
		}
		return;
	}
	var $text = obejct.find("input:text:first");
	if ($text.length > 0) {
		$text.focus();
	}
};

(function(){
	$(document).keydown(function(e){  
		if(e && e.keyCode==8){  
			var keyEvent = true;  
			var d=e.srcElement||e.target;  
	        if(d.tagName.toUpperCase()=='INPUT'||d.tagName.toUpperCase()=='TEXTAREA'){  
	            keyEvent=d.readOnly||d.disabled;
	        } 
			if(keyEvent){  
			    e.preventDefault();  
			}  
		} 
	});
})();

/**
 * @desc 引入js和css文件
 * 
 * @param id
 * @param path
 * @param file
 */
// SZUtils.include = function(id, path, file) {
// if (document.getElementById(id) == null) {
// var files = typeof (file) == "string" ? [ file ] : file;
// for ( var i = 0; i < files.length; i++) {
// var name = files[i].replace(/^\s|\s$/g, "");
// var att = name.split('.');
// var ext = att[att.length - 1].toLowerCase();
// var isCSS = ext == "css";
// var tag = isCSS ? "link" : "script";
// var attr = isCSS ? " type='text/css' rel='stylesheet' "
// : " type='text/javascript' ";
// var link = (isCSS ? "href" : "src") + "='" + path + name + "'";
// document.write("<" + tag + (i == 0 ? " id=" + id : "") + attr
// + link + "></" + tag + ">");
// }
// }
// };
/**
 * @desc 打开一个窗体
 * 
 * @param url
 * @param name
 * @param width
 * @param height
 */
// SZUtils.windowOpen = function(url, name, width, height) {
// var top = parseInt((window.screen.height - height) / 2, 10), left = parseInt(
// (window.screen.width - width) / 2, 10), options =
// "location=no,menubar=no,toolbar=no,dependent=yes,minimizable=no,modal=yes,alwaysRaised=yes,"
// + "resizable=yes,scrollbars=yes,"
// + "width="
// + width
// + ",height="
// + height + ",top=" + top + ",left=" + left;
// window.open(url, name, options);
// };
