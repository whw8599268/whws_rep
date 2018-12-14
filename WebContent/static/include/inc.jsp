<link href="${pageContext.request.contextPath}/static/css/default.css" rel="stylesheet" />
<!--  -->
<link href="${pageContext.request.contextPath}/static/css/table.css" rel="stylesheet" />
<link href="${pageContext.request.contextPath}/static/css/kb.css" rel="stylesheet" />
<link href="${pageContext.request.contextPath}/static/res/jquery-easyui/themes/default/easyui.css" rel="stylesheet" />
<link href="${pageContext.request.contextPath}/static/res/jquery-easyui/themes/icon.css" rel="stylesheet" />
<link href="${pageContext.request.contextPath}/static/res/jquery-easyui/themes/myiconCss.css" rel="stylesheet" />
<script src="${pageContext.request.contextPath}/static/res/jquery/jquery.min.js" type="text/javascript"></script>
<script src="${pageContext.request.contextPath}/static/res/jquery-easyui/jquery.easyui.min.js" type="text/javascript"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/res/jquery-easyui/locale/easyui-lang-zh_CN.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/res/jquery/jquery.form.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/res/jquery/jquery.form2json.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/js/uitlsEasyUI/SZUtilsExtEasyUI.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/js/uitlsEasyUI/SZUtilsExtEasyUIListPage.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/js/utils/SZUtilsDate.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/js/utils/SZUtilsArray.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/js/utils/SZUtils.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/js/utils/SZUtilsStringMD5.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/js/utils/SZUtilsString.js"></script>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/static/res/ztree/style/zTreeStyle.css">
<script src="${pageContext.request.contextPath}/static/res/ztree/jquery.ztree.all-3.5.min.js" type="text/javascript"></script>
<!--[if lte IE 8 ]><script type="text/javascript" src="${pageContext.request.contextPath}/static/js/json2.js"></script><![endif]-->
<script>
	function onBeforeList(param) {
		param.size = param.rows;
		param._ = new Date().getTime();
		return true;
	}
	function resultFilter(data) {
		if (data.totalElements == null) {
			return resultFilterByNoPage(data);
		} else {
			return resultFilterByPage(data);
		}

	}
	function resultFilterByPage(data) {
		data.rows = data.content;
		data.total = data.totalElements;
		delete data.content;
		return data;
	}
	function resultFilterByNoPage(data) {
		var result = {};
		result.rows = data;
		result.total = data.size;
		return result;
	}
	$.ajaxSetup({
		 cache:false
		});

</script>