/**
 * @file 对EasyUI的加强以及扩展，引用路径：&lt;script type="text/javascript" src="${path}/static/js/uitlsEasyUI/SZUtilsDate.js">&lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 */

/**
 * @desc sy 对EasyUI的加强以及扩展
 * @version v1.0.0
 * @namespace sy
 * @author hyc
 * @since 2017-05-02
 * @requires jQuery
 * @requires EasyUI
 * @example sy.progress.show();
 * @example sy.progress.hide();
 * @example sy.dialog(options);
 * @example sy.window(options);
 * @example sy.messagerConfirm(title, msg, fn);
 * @example sy.messagerShow(options);
 * @example sy.messagerAlert(title, msg, icon, fn);
 */
var sy = sy || {};

/**
 * @desc sy.progress 对EasyUI progress进度条的加强<br/>{@link sy}
 * @version v1.0.0
 * @memberof sy
 * @namespace sy.progress
 * @author hyc
 * @since 2017-05-02
 * @example sy.progress.show();
 * @example sy.progress.hide();
 */
sy.progress = {
	/**
	 * @desc 显示加载进度条
	 * @method show
	 * @memberof sy.progress
	 * @param {obejct|null}
	 *            options 可选 对象
	 * @example sy.progress.show();
	 */
	"show" : function(options) {
		var opts = $.extend({}, options);
		var text = '正在执行操作请稍候......';
		if (typeof (opts.text) != 'undefined') {
			text = opts.text;
		}
		top.$.messager.progress({
			text : text,
			interval : 100
		});
	},
	/**
	 * @desc 关闭加载进度条
	 * @method hide
	 * @memberof sy.progress
	 * @param {obejct|null}
	 *            options 可选 对象
	 * @example sy.progress.hide();
	 */
	"hide" : function(options) {
		var opts = $.extend({}, options);
		window.setTimeout(function() {
			top.$.messager.progress('close');
			if (self != parent) {
				window.setTimeout(function() {
					try {
						parent.top.$.messager.progress('close');
					} catch (e) {
					}
				}, 500);
			}
		}, 500);
	}
};

/**
 * @desc
 * <p>
 * 开启EasyUI dialog
 * </p>
 * <br/>
 * <p>
 * modal,onClose默认
 * </p>
 * @method dialog
 * @author 孙宇
 * @memberof sy
 * @param {obejct|null}
 *            options 对象
 * @example sy.dialog(options);
 */
sy.dialog = function(options) {
	var opts = $.extend({
		modal : true,
		onClose : function() {
			$(this).dialog('destroy');
		}
	}, options);
	return $('<div/>').dialog(opts);
};

/**
 * @desc
 * <p>
 * 开启EasyUI window
 * </p>
 * <br/>
 * <p>
 * modal,onClose默认
 * </p>
 * @method window
 * @author zy
 * @memberof sy
 * @param {obejct}
 *            options 对象
 * @example sy.window(options);
 */
sy.window = function(options) {
	var opts = $.extend({
		modal : true,
		onClose : function() {
			$(this).window('destroy');
		}
	}, options);
	return $('<div/>').window(opts);
};

/**
 * @desc
 * <p>
 * 开启EasyUI confirm
 * </p>
 * @method messagerConfirm
 * @author 孙宇
 * @memberof sy
 * @param {string}
 *            title 标题
 * @param {string}
 *            msg 提示信息
 * @param {function}
 *            fun 回调方法
 * @example sy.messagerConfirm(title, msg, fn);
 */
sy.messagerConfirm = function(title, msg, fn) {
	return $.messager.confirm(title, msg, fn);
};

/**
 * @desc
 * <p>
 * 开启EasyUI show
 * </p>
 * @method messagerShow
 * @author 孙宇
 * @memberof sy
 * @param {obejct}
 *            options 可选 JOSN对象
 * @example sy.messagerShow(options);
 */
sy.messagerShow = function(options) {
	return $.messager.show(options);
};

/**
 * @desc
 * <p>
 * 开启EasyUI alert
 * </p>
 * @method messagerAlert
 * @author 孙宇
 * @memberof sy
 * @param {string}
 *            title 在头部面板显示的标题文本
 * @param {string}
 *            msg 显示的消息文本
 * @param {string}
 *            icon 可选 显示的图标图像。可用值有：error,question,info,warning。
 * @param {function|null}
 *            fn 可选 在窗口关闭的时候触发该回调函数
 * @example sy.messagerAlert(title, msg, icon, fn);
 * 
 */
sy.messagerAlert = function(title, msg, icon, fn) {
	return $.messager.alert(title, msg, icon, fn);
};

/**
 * @desc
 * <p>
 * 创建一个模式化的dialog
 * </p>
 * @method modalDialog
 * @author 孙宇
 * @memberof sy
 * @param {object}
 *            options 对象
 * @example sy.modalDialog(options);
 */
sy.modalDialog = function(options) {
	var opts = $.extend({
		title : '&nbsp;',
		width : 640,
		height : 480,
		modal : true,
		onClose : function() {
			$(this).dialog('destroy');
		}
	}, options);
	opts.modal = true;// 强制此dialog为模式化，无视传递过来的modal参数
	if (options.url) {
		opts.content = '<iframe id="" src="'
				+ options.url
				+ '" allowTransparency="true" scrolling="auto" width="100%" height="98%" frameBorder="0" name=""></iframe>';
	}
	return $('<div/>').dialog(opts);
};

/**
 * @desc
 * <p>
 * 1、在页面开启前关闭easyui默认开启的parser
 * </p>
 * <p>
 * 2、在页面加载之前，先开启一个进度条
 * </p>
 * <p>
 * 3、然后在页面所有easyui组件渲染完毕后，关闭进度条
 * </p>
 * 
 * <pre>
 * $.parser.auto = false;
 * $(function() {
 * 	sy.documentParse();
 * });
 * </pre>;
 * @method documentParse
 * @author 孙宇
 * @memberof sy
 * @example 不需要手动调用;
 */
$.parser.auto = false;
sy.documentParse = function() {
	$.messager.progress({
		text : '页面加载中....',
		interval : 100
	});
	$.parser.parse(window.document);
	window.setTimeout(function() {
		$.messager.progress('close');
		if (self != parent) {
			window.setTimeout(function() {
				try {
					parent.$.messager.progress('close');
				} catch (e) {
				}
			}, 500);
		}
	}, 1);
	$.parser.auto = true;
};
$(function() {
	sy.documentParse();
});

/**
 * @author 孙宇
 * @method removeEasyuiTipFunction
 * @memberof sy
 * @desc
 *           <p>
 *           避免验证tip屏幕跑偏
 *           </p>
 *           <p>
 *           用于panel/window/dialog onClose关闭
 *           </p>
 */
sy.removeEasyuiTipFunction = function() {
	window.setTimeout(function() {
		$('div.validatebox-tip').remove();
	}, 0);
};
$.fn.panel.defaults.onClose = sy.removeEasyuiTipFunction;
$.fn.window.defaults.onClose = sy.removeEasyuiTipFunction;
$.fn.dialog.defaults.onClose = sy.removeEasyuiTipFunction;

/**
 * @author 孙宇
 * @method easyuiErrorFunction
 * @memberof sy
 * @desc
 *           <p>
 *           通用错误提示
 *           </p>
 *           <p>
 *           用于datagrid/treegrid/tree/combogrid/combobox/form onLoadError加载数据出错
 *           </p>
 */
sy.easyuiErrorFunction = function(XMLHttpRequest) {
	$.messager.progress('close');
	$.messager.alert('错误', XMLHttpRequest.responseText);
};
$.fn.datagrid.defaults.onLoadError = sy.easyuiErrorFunction;
$.fn.treegrid.defaults.onLoadError = sy.easyuiErrorFunction;
$.fn.tree.defaults.onLoadError = sy.easyuiErrorFunction;
$.fn.combogrid.defaults.onLoadError = sy.easyuiErrorFunction;
$.fn.combobox.defaults.onLoadError = sy.easyuiErrorFunction;
$.fn.form.defaults.onLoadError = sy.easyuiErrorFunction;

/**
 * @author 孙宇
 * @method userSessionErrorFunction
 * @memberof sy
 * @desc
 *           <p>
 *           错误跳转
 *           </p>
 *           <p>
 *           用于用户Session出错
 *           </p>
 */
sy.userSessionErrorFunction = function(XMLHttpRequest) {
	$.messager.progress('close');
	document.location.href = sy.pn() + '/auth/logout';
};

/**
 * @author 孙宇
 * @method userSessionErrorFunction
 * @memberof sy
 * @desc
 *           <p>
 *           增加表头菜单，用于显示或隐藏列，注意：冻结列不在此菜单中
 *           </p>
 *           <p>
 *           用于datagrid、treegrid onHeaderContextMenu
 *           </p>
 */
sy.createGridHeaderContextMenu = function(e, field) {
	e.preventDefault();
	var grid = $(this);/* grid本身 */
	var headerContextMenu = this.headerContextMenu;/* grid上的列头菜单对象 */
	if (!headerContextMenu) {
		var tmenu = $('<div style="width:100px;"></div>').appendTo('body');
		var fields = grid.datagrid('getColumnFields');
		for ( var i = 0; i < fields.length; i++) {
			var fildOption = grid.datagrid('getColumnOption', fields[i]);
			if (!fildOption.hidden) {
				$('<div iconCls="icon-ok" field="' + fields[i] + '"/>').html(
						fildOption.title).appendTo(tmenu);
			} else {
				$('<div iconCls="icon-empty" field="' + fields[i] + '"/>')
						.html(fildOption.title).appendTo(tmenu);
			}
		}
		headerContextMenu = this.headerContextMenu = tmenu.menu({
			onClick : function(item) {
				var field = $(item.target).attr('field');
				if (item.iconCls == 'icon-ok') {
					grid.datagrid('hideColumn', field);
					$(this).menu('setIcon', {
						target : item.target,
						iconCls : 'icon-empty'
					});
				} else {
					grid.datagrid('showColumn', field);
					$(this).menu('setIcon', {
						target : item.target,
						iconCls : 'icon-ok'
					});
				}
			}
		});
	}
	headerContextMenu.menu('show', {
		left : e.pageX,
		top : e.pageY
	});
};
$.fn.datagrid.defaults.onHeaderContextMenu = sy.createGridHeaderContextMenu;
$.fn.treegrid.defaults.onHeaderContextMenu = sy.createGridHeaderContextMenu;

/**
 * @author 孙宇
 * @method panelBeforeDestroy
 * @memberof sy
 * @desc
 *           <p>
 *           panel关闭时回收内存,主要用于layout使用iframe嵌入网页时的内存泄漏问题
 *           </p>
 *           <p>
 *           用于panel onBeforeDestroy
 *           </p>
 */
sy.panelBeforeDestroy = function() {
	var frame = $('iframe', this);
	try {
		if (frame.length > 0) {
			for ( var i = 0; i < frame.length; i++) {
				frame[i].contentWindow.document.write('');
				frame[i].contentWindow.close();
			}
			frame.remove();
			if ($.browser.msie || navigator.userAgent.indexOf("MSIE") > 0) {
				try {
					CollectGarbage();
				} catch (e) {
				}
			}

		}
	} catch (e) {
	}
};
$.fn.panel.defaults.onBeforeDestroy = sy.panelBeforeDestroy;

/**
 * 修改panel和datagrid在加载时提示
 * 
 * @author 孙宇
 */

$.fn.panel.defaults.loadingMessage = '加载中....';
$.fn.datagrid.defaults.loadMsg = '加载中....';

/**
 * @author 孙宇
 * 
 * @requires jQuery,EasyUI
 * 
 * 扩展datagrid，添加动态增加或删除Editor的方法
 * 
 * 例子如下，第二个参数可以是数组
 * 
 * datagrid.datagrid('removeEditor', 'cpwd');
 * 
 * datagrid.datagrid('addEditor', [ { field : 'ccreatedatetime', editor : { type :
 * 'datetimebox', options : { editable : false } } }, { field :
 * 'cmodifydatetime', editor : { type : 'datetimebox', options : { editable :
 * false } } } ]);
 * 
 */
$.extend($.fn.datagrid.methods, {
	addEditor : function(jq, param) {
		if (param instanceof Array) {
			$.each(param, function(index, item) {
				var e = $(jq).datagrid('getColumnOption', item.field);
				e.editor = item.editor;
			});
		} else {
			var e = $(jq).datagrid('getColumnOption', param.field);
			e.editor = param.editor;
		}
	},
	removeEditor : function(jq, param) {
		if (param instanceof Array) {
			$.each(param, function(index, item) {
				var e = $(jq).datagrid('getColumnOption', item);
				e.editor = {};
			});
		} else {
			var e = $(jq).datagrid('getColumnOption', param);
			e.editor = {};
		}
	}
});

/**
 * @author 孙宇
 * 
 * @requires jQuery,EasyUI
 * 
 * 扩展datagrid的editor
 * 
 * 增加带复选框的下拉树
 * 
 * 增加日期时间组件editor
 * 
 * 增加多选combobox组件
 */
$.extend($.fn.datagrid.defaults.editors, {
	combocheckboxtree : {
		init : function(container, options) {
			var editor = $('<input />').appendTo(container);
			options.multiple = true;
			editor.combotree(options);
			return editor;
		},
		destroy : function(target) {
			$(target).combotree('destroy');
		},
		getValue : function(target) {
			return $(target).combotree('getValues').join(',');
		},
		setValue : function(target, value) {
			$(target).combotree('setValues', sy.getList(value));
		},
		resize : function(target, width) {
			$(target).combotree('resize', width);
		}
	},
	datetimebox : {
		init : function(container, options) {
			var editor = $('<input />').appendTo(container);
			editor.datetimebox(options);
			return editor;
		},
		destroy : function(target) {
			$(target).datetimebox('destroy');
		},
		getValue : function(target) {
			return $(target).datetimebox('getValue');
		},
		setValue : function(target, value) {
			$(target).datetimebox('setValue', value);
		},
		resize : function(target, width) {
			$(target).datetimebox('resize', width);
		}
	},
	multiplecombobox : {
		init : function(container, options) {
			var editor = $('<input />').appendTo(container);
			options.multiple = true;
			editor.combobox(options);
			return editor;
		},
		destroy : function(target) {
			$(target).combobox('destroy');
		},
		getValue : function(target) {
			return $(target).combobox('getValues').join(',');
		},
		setValue : function(target, value) {
			$(target).combobox('setValues', sy.getList(value));
		},
		resize : function(target, width) {
			$(target).combobox('resize', width);
		}
	}
});

/**
 * @author 孙宇
 * @method changeTheme
 * @memberof sy
 * @param {string}themeName
 *            主题字符串
 * @desc
 *            <p>
 *            更换主题
 *            </p>
 */
sy.changeTheme = function(themeName) {
	var $easyuiTheme = $('#easyuiTheme');
	var url = $easyuiTheme.attr('href');
	var href = url.substring(0, url.indexOf('themes')) + 'themes/' + themeName
			+ '/easyui.css';
	$easyuiTheme.attr('href', href);

	var $iframe = $('iframe');
	if ($iframe.length > 0) {
		for ( var i = 0; i < $iframe.length; i++) {
			var ifr = $iframe[i];
			try {
				$(ifr).contents().find('#easyuiTheme').attr('href', href);
			} catch (e) {
				try {
					ifr.contentWindow.document.getElementById('easyuiTheme').href = href;
				} catch (e) {
				}
			}
		}
	}
	sy.cookie('easyuiTheme', themeName, {
		expires : 7
	});
};

/**
 * 改变jQuery的AJAX默认属性和方法
 */
$.ajaxSetup({
	type : 'POST',
	error : function(XMLHttpRequest, textStatus, errorThrown) {
		$.messager.progress('close');
		$.messager.alert('错误', XMLHttpRequest.responseText);
	}
});

$
		.extend(
				$.fn.validatebox.defaults.rules,
				{
					CHS : {
						validator : function(value, param) {
							return /^[\u0391-\uFFE5]+$/.test(value);
						},
						message : '请输入汉字'
					},
					ZIP: {
				        validator: function (value, param) {
				            return /^[0-9]\d{5}$/.test(value);
				        },
				        message: '邮政编码不存在' 
				    },
					QQ : {
						validator : function(value, param) {
							return /^[1-9]\d{4,16}$/.test(value);
						},
						message : 'QQ号码不正确'
					},
					TEL : {
						validator : function(value, param) {
							return /^(\d{3}-|\d{4}-)?(\d{8}|\d{7})$/
									.test(value);
						},
						message : '固定号码不正确，例如：0591-86481010'
					},
					DATATIME : {
						validator : function(value, param) {
							return /^([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8])))$/
									.test(value);
						},
						message : '日期格式不正常'
					},
					mobile : {
						validator : function(value, param) {
							return /^[0-9]{11}$/.test(value);
						},
						message : '手机号码不正确'
					},
					loginName : {
						validator : function(value, param) {
							return /^[\u0391-\uFFE5\w]+$/.test(value);
						},
						message : '登录名称只允许汉字、英文字母、数字及下划线'
					},
					safepass : {
						validator : function(value, param) {
							function safePassword(value) {
								return !(/^(([A-Z]*|[a-z]*|\d*|[-_\~!@#\$%\^&\*\.\(\)\[\]\{\}<>\?\\\/\'\"]*)|.{0,5})$|\s/
										.test(value));
							}
							return safePassword(value);
						},
						message : '密码由字母和数字组成，至少6位'
					},
					equalTo : {
						validator : function(value, param) {
							return value == $(param[0]).val();
						},
						message : '两次输入的字符不一致'
					},
					eqPwd : {/* 验证两次密码是否一致功能 */
						validator : function(value, param) {
							return value == $(param[0]).val();
						},
						message : '密码不一致！'
					},
					eqPassword : {
						validator : function(value, param) {
							return value == $(param[0]).val();
						},
						message : '密码不一致！'
					},
					maxTo : {
						validator : function(value, param) {
							return parseInt(value) >= parseInt($(param[0])
									.val());
						},
						message : '初始值不能大于末值'
					},
					number : {
						validator : function(value, param) {
							return /^\d+$/.test(value);
						},
						message : '请输入数字'
					},
					maxLength : {
						validator : function(value, param) {
							return value.length <= param[0];
						},
						message : '最多只能输入{0}个字符'
					},
					equalLength : {
						validator : function(value, param) {
							var rules = $.fn.validatebox.defaults.rules;
							rules.equalLength.messager = '请输入{0}个数字';
							if (!rules.number.validator(value)) {
								rules.equalLength.messager = rules.number.messager;
								return false;
							}
							return value.length == param[0];
						},
						message : '请输入{0}个数字'
					},
					remoteEdit : {
						validator : function(value, param) {
							// 参数为空
							if (param == null || param.length < 2) {
								return false;
							}

							// 判断值有没有修改
							if (value == $(param[2]).val()) {
								return true;
							}

							// 拼接请求对象
							var _2e = {};
							_2e[param[1]] = value;

							var _res = $.ajax({
								url : param[0],
								dataType : "json",
								data : _2e,
								async : false,
								cache : false,
								type : "post"
							}).responseText;

							return _res == "true";
						},
						message : '已存在该数据'
					},
					idcard : {
						validator : function(value, param) {
							return sy.rules.idCard(value);
						},
						message : '请输入正确的身份证号码'
					}
				});

sy.rules = sy.rules || {};
sy.rules.idCard = function(value) {
	if (value.length == 18 && 18 != value.length)
		return false;
	var number = value.toLowerCase();
	var d, sum = 0, v = '10x98765432', w = [ 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7,
			9, 10, 5, 8, 4, 2 ], a = '11,12,13,14,15,21,22,23,31,32,33,34,35,36,37,41,42,43,44,45,46,50,51,52,53,54,61,62,63,64,65,71,81,82,91';
	var re = number
			.match(/^(\d{2})\d{4}(((\d{2})(\d{2})(\d{2})(\d{3}))|((\d{4})(\d{2})(\d{2})(\d{3}[x\d])))$/);
	if (re == null || a.indexOf(re[1]) < 0)
		return false;
	if (re[2].length == 9) {
		number = number.substr(0, 6) + '19' + number.substr(6);
		d = [ '19' + re[4], re[5], re[6] ].join('-');
	} else
		d = [ re[9], re[10], re[11] ].join('-');
	if (!isDateTime.call(d, 'yyyy-MM-dd'))
		return false;
	for ( var i = 0; i < 17; i++)
		sum += number.charAt(i) * w[i];
	return (re[2].length == 9 || number.charAt(17) == v.charAt(sum % 11));
	function isDateTime(format, reObj) {
		format = format || 'yyyy-MM-dd';
		var input = this, o = {}, d = new Date();
		var f1 = format.split(/[^a-z]+/gi), f2 = input.split(/\D+/g), f3 = format
				.split(/[a-z]+/gi), f4 = input.split(/\d+/g);
		var len = f1.length, len1 = f3.length;
		if (len != f2.length || len1 != f4.length)
			return false;
		for ( var i = 0; i < len1; i++)
			if (f3[i] != f4[i])
				return false;
		for ( var i = 0; i < len; i++)
			o[f1[i]] = f2[i];
		o.yyyy = s(o.yyyy, o.yy, d.getFullYear(), 9999, 4);
		o.MM = s(o.MM, o.M, d.getMonth() + 1, 12);
		o.dd = s(o.dd, o.d, d.getDate(), 31);
		o.hh = s(o.hh, o.h, d.getHours(), 24);
		o.mm = s(o.mm, o.m, d.getMinutes());
		o.ss = s(o.ss, o.s, d.getSeconds());
		o.ms = s(o.ms, o.ms, d.getMilliseconds(), 999, 3);
		if (o.yyyy + o.MM + o.dd + o.hh + o.mm + o.ss + o.ms < 0)
			return false;
		if (o.yyyy < 100)
			o.yyyy += (o.yyyy > 30 ? 1900 : 2000);
		d = new Date(o.yyyy, o.MM - 1, o.dd, o.hh, o.mm, o.ss, o.ms);
		var reVal = d.getFullYear() == o.yyyy && d.getMonth() + 1 == o.MM
				&& d.getDate() == o.dd && d.getHours() == o.hh
				&& d.getMinutes() == o.mm && d.getSeconds() == o.ss
				&& d.getMilliseconds() == o.ms;
		return reVal && reObj ? d : reVal;
		function s(s1, s2, s3, s4, s5) {
			s4 = s4 || 60, s5 = s5 || 2;
			var reVal = s3;
			if (s1 != undefined && s1 != '' || !isNaN(s1))
				reVal = s1 * 1;
			if (s2 != undefined && s2 != '' && !isNaN(s2))
				reVal = s2 * 1;
			return (reVal == s1 && s1.length != s5 || reVal > s4) ? -10000
					: reVal;
		}
	}
	;
};

(function($) {
	Number.prototype.floor = function (s) {  //在Number的原型上重写toFixed方法
		var changenum = (parseInt(this * Math.pow(10, s)) / Math.pow(10,s)).toString();
		var index = changenum.indexOf(".");
		if (index < 0 && s > 0) {
		changenum = changenum + ".";
		for (i = 0; i < s; i++) {
		changenum = changenum + "0";
		}
		} else {
		index = changenum.length - index;
		for (i = 0; i < (s - index) + 1; i++) {
		changenum = changenum + "0";
		}
		}
		return changenum;
		} 
	$.fn.numberbox.defaults.filter = function(e) {
	    var opts = $(this).numberbox('options');
	    var s = $(this).numberbox('getText');
	    if (e.which == 45) { //-
	        return (s.indexOf('-') == -1 ? true: false);
	    }
	    var c = String.fromCharCode(e.which);
	    if (c == opts.decimalSeparator) {
	        return (s.indexOf(c) == -1 ? true: false);
	    } else if (c == opts.groupSeparator) {
	        return true;
	    } else if ((e.which >= 48 && e.which <= 57 && e.ctrlKey == false && e.shiftKey == false) || e.which == 0 || e.which == 8) {
	        return true;
	    } else if (e.ctrlKey == true && (e.which == 99 || e.which == 118)) {
	        return true;
	    } else {
	        return false;
	    }
	};
	$.fn.numberbox.defaults.parser=function(s) {
	    s = s + "";
	    var opts = $(this).numberbox("options");
	    if (parseFloat(s) != s) {
	        if (opts.prefix) {
	            s = $.trim(s.replace(new RegExp("\\" + $.trim(opts.prefix), "g"), ""));
	        }
	        if (opts.suffix) {
	            s = $.trim(s.replace(new RegExp("\\" + $.trim(opts.suffix), "g"), ""));
	        }
	        if (opts.groupSeparator) {
	            s = $.trim(s.replace(new RegExp("\\" + opts.groupSeparator, "g"), ""));
	        }
	        if (opts.decimalSeparator) {
	            s = $.trim(s.replace(new RegExp("\\" + opts.decimalSeparator, "g"), "."));
	        }
	        s = s.replace(/\s/g, "");
	    }
	    var val = parseFloat(s).floor(opts.precision);
	    if (isNaN(val)) {
	        val = "";
	    } else {
	        if (typeof(opts.min) == "number" && val < opts.min) {
	            val = opts.min.floor(opts.precision);
	        } else {
	            if (typeof(opts.max) == "number" && val > opts.max) {
	                val = opts.max.floor(opts.precision);
	            }
	        }
	    }
	    return val;
	};
})(jQuery);


/**
 * 解决多值带字段名称带[]问题 
 */
$.ajaxSetup({traditional: true});