/**
 * @file 设置list的页面datagrid常用方法以及提供页面常用方法，引用路径：&lt;script type="text/javascript" src="${path}/static/js/uitlsEasyUI/SZUtilsExtEasyUIListPage.js">&lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 */

/**
 * @desc 设置list的页面datagrid常用方法以及提供页面常用方法
 * @version v1.0.0
 * @namespace SZUtilsExtEasyUIListPage
 * @author hyc
 * @since 2017-05-05
 * @requires jQuery
 * @requires EasyUI
 *@example 
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8" />
<title></title>
<%includeJSP("/static/include/inc.jsp",{}){}%>
<script type="text/javascript">
	SZUtilsExtEasyUIListPage.datagrid.pagination();
	$(function() {
	});

	function doSearch() {
		SZUtilsExtEasyUIListPage.datagrid.doSearch({
			formObejct : $('#searchForm'),
			datagirdObejct : $('#formGrid')
		});
	}

	function doClear() {

		SZUtilsExtEasyUIListPage.datagrid.doClear({
			formObejct : $('#searchForm'),
			datagirdObejct : $('#formGrid')
		});
	}
</script>
</head>
<body>
	<!-- 表格 -->
	<table id="formGrid" style="width: auto; height: auto"
		class="easyui-datagrid" title="" toolbar="#searchForm-toolbar"
		rownumbers="true" fitcolumns="true" fit="true"
		url="" pagination="true"
		data-options="method:'get'">
		<thead>
			<tr>
				<th data-options="field:'',checkbox:true"></th>
				<th field="V_REMARK" align="left" width="100">描述</th>
			</tr>
		</thead>
	</table>
	<!-- 搜索工具栏 -->
	<div id="searchForm-toolbar" style="padding: 5px; height: auto">
		<div>
			<form id="searchForm"> 
				<label>描述：</label> 
				<input class="easyui-textbox" style="width: 120px" id="V_REMARK" name="V_REMARK" type="text" /> 
				<a href="javascript:void(0)" class="easyui-linkbutton" data-options="iconCls:'icon-search'" onclick="doSearch()">搜索</a> 
				<a href="javascript:void(0)" class="easyui-linkbutton" data-options="iconCls:'icon-clear'" onclick="doClear()">清空搜索条件</a> 
			</form>
		</div>
		<div>
			<a href="javascript:void(0)" class="easyui-linkbutton" data-options="iconCls:'icon-add'" title="新增" onclick="doAdd()">新增</a>
		</div>
	</div>
</body>
</html>
 */
var SZUtilsExtEasyUIListPage = SZUtilsExtEasyUIListPage || {};

/**
 * @desc 设置list的页面datagrid常用方法
 * @version v1.0.0
 * @namespace SZUtilsExtEasyUIListPage.datagrid
 * @author hyc
 * @since 2017-05-05
 * @requires jQuery
 * @requires EasyUI
 */
SZUtilsExtEasyUIListPage.datagrid = SZUtilsExtEasyUIListPage.datagrid || {};

SZUtilsExtEasyUIListPage.datagrid.loadFilter = function(data) {
	data.rows = data.content;
	data.total = data.totalElements;
	// 删除原参数中的content属性
	delete data.content;
	return data;
};

SZUtilsExtEasyUIListPage.datagrid.onBeforeLoad = function(param) {
	// 设置size参数
	param.size = param.rows;
	// 去除IE get缓存
	param._ = new Date().getTime();
	return true;
};

/**
 * @desc
 * <p>
 * 加载信息,兼容Aeolus框架，用于dataGrid的分页列表
 * </p>
 * <p style="color:red;">
 * 设置$.fn.datagrid.defaults.loadFilter <br/>
 * 设置$.fn.datagrid.defaults.onBeforeLoad<br/>
 * dataGrid无需设置通用的loadFilter以及onBeforeLoad<br/>
 * 在页面加载前加SZUtilsExtEasyUIListPage.datagrid.pagination()
 * </p>
 * @author hyc
 * @since 2017-05-04
 */
SZUtilsExtEasyUIListPage.datagrid.pagination = function()
{
	$.fn.datagrid.defaults.loadFilter = SZUtilsExtEasyUIListPage.datagrid.loadFilter;
	$.fn.datagrid.defaults.onBeforeLoad = SZUtilsExtEasyUIListPage.datagrid.onBeforeLoad;
}

/**
 * 解决多值带字段名称带[]问题
 */
$.ajaxSetup({
	traditional : true
});

/**
 * @desc
 * <p >
 * 用于设置datagrid的form搜索
 * </p>
 * @author hyc
 * @since 2017-05-04
 * @param {object} 
 *            option 对象
 * @example    
 * SZUtilsExtEasyUIListPage.datagrid.doSearch({
 * 		//form对象
 * 		formObejct: $('#searchForm'),
 * 		//datagird对象
 * 		datagirdObejct:$('#formGrid'),
 *		//用于from数据处理，数据加载前的调用对表单数据进行特殊处理
 * 		beforeload:function(){
 * 
 * 		}
 * }); 
 */
SZUtilsExtEasyUIListPage.datagrid.doSearch = function(option) {
	if (option.formObejct && option.formObejct.length<1) {
		option.formObejct = $('#searchForm');
		if (option.formObejct.length<1) {
			return;
		}
	}
	if (option.datagirdObejct && option.datagirdObejct.length<1) {
		option.datagirdObejct = $('#formGrid');
		if (option.datagirdObejct.length<1) {
			return;
		}
	}
	// 使用Form2Json插件处理,同时过滤掉空数据
	var searchParam = option.formObejct.form2json({
		allowEmptySingleVal : false
	});
	if(option.beforeload && typeof(option.beforeload) === 'function'){
		option.beforeload(searchParam);
	}
	option.datagirdObejct.datagrid('load', searchParam);
}

/**
 * @desc
 * <p >
 * 用于设置datagrid的form清除
 * </p>
 * @author hyc
 * @since 2017-05-04
 * @param {object} 
 *            option 对象
 * @example    
 * SZUtilsExtEasyUIListPage.datagrid.doClear({
 * 		//form对象
 * 		formObejct: $('#searchForm'),
 * 		//datagird对象
 * 		datagirdObejct:$('#formGrid'),
 *		//用于from清除后，数据加载前的调用
 * 		beforeload:function(){
 * 
 * 		}
 * });      
 */
SZUtilsExtEasyUIListPage.datagrid.doClear = function(option) {
	if (option.formObejct && option.formObejct.length<1) {
		option.formObejct = $('#searchForm');
		if (option.formObejct.length<1) {
			return;
		}
	}
	if (option.datagirdObejct && option.datagirdObejct.length<1) {
		option.datagirdObejct = $('#formGrid');
		if (option.datagirdObejct.length<1) {
			return;
		}
	}
	option.formObejct.form('reset');
	
	if(option.beforeload && typeof(option.beforeload) === 'function'){
		option.beforeload();
	}
	option.datagirdObejct.datagrid('load', {});
}

/**
 * @desc 设置list的页面tree常用方法
 * @version v1.0.0
 * @namespace SZUtilsExtEasyUIListPage.tree
 * @author hyc
 * @since 2017-05-09
 * @requires jQuery
 * @requires EasyUI
 */
SZUtilsExtEasyUIListPage.tree = SZUtilsExtEasyUIListPage.tree || {};

/**
 * @desc
 * <p >
 * 扩展tree和combotree，使其支持平滑数据格式
 * </p>
  * <p >
 * 可用与$.fn.combotree.defaults.loadFilter与$.fn.tree.defaults.loadFilter
 * </p>
 * @author 孙宇
 * @since 2017-05-09
 * @param {object} 
 *            data 加载的原始数据
 * @param {object} parent
 *            DOM对象，代表父节点
 * @example    
 *	<input class="easyui-combotree" method="get" data-options="url:'${path}${adminPath}/bus/org/tree/my',loadFilter:SZUtilsExtEasyUIListPage.tree.loadFilter,idField:'ID',textField:'V_NAME',parentField:'V_PID'" style="width: 150px;" /> 
 */

SZUtilsExtEasyUIListPage.tree.loadFilter = function(data, parent) {
	var opt = $(this).data().tree.options;
	var idField, textField, parentField;
	if (opt.parentField) {
		idField = opt.idField || 'id';
		textField = opt.textField || 'text';
		parentField = opt.parentField || 'pid';
		var i, l, treeData = [], tmpMap = [];
		for (i = 0, l = data.length; i < l; i++) {
			tmpMap[data[i][idField]] = data[i];
		}
		for (i = 0, l = data.length; i < l; i++) {
			if (tmpMap[data[i][parentField]]
					&& data[i][idField] != data[i][parentField]) {
				if (!tmpMap[data[i][parentField]]['children'])
					tmpMap[data[i][parentField]]['children'] = [];
				data[i]['text'] = data[i][textField];
				tmpMap[data[i][parentField]]['children'].push(data[i]);
			} else {
				data[i]['text'] = data[i][textField];
				treeData.push(data[i]);
			}
		}
		return treeData;
	}
	return data;
}

