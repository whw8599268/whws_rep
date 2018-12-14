/**
 * @file 提供附件常用方法，引用路径：
 * &lt;link rel="stylesheet" href="${path}/static/res/uploadify/uploadify.css" type="text/css">&lt;/link>
 * &lt;script type="text/javascript" src="${path}/static/res/uploadify/jquery.uploadify.min.js">&lt;/script>
 * &lt;script type="text/javascript" src="${path}${adminPath}/res/static/js/utils/SZUtilsFile.js"> &lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 */

/**
 * @desc
 * <p>
 * 提供附件常用方法
 * </p>
 * @version v1.0.0
 * @namespace SZUtilsFile
 * @author hyc
 * @since 2017-05-02
 * @requires uploadify.css
 * @requires jquery.uploadify.min.js
 * @example 
 * var refid; 
 * function callbackfunction(data){ 
 * //TODO 
 * }
 * SZUtilsFile.uploadfiletype('公共附件', callbackfunction,refid);
 * @example SZUtilsFile.deleteAttachments("ID")
 * @example SZUtilsFile.downfile("attachid","downfile");
 * @example SZUtilsFile.downfile("attachid","lookfile");
 * @example SZUtilsFile.downfilebyself("attachid","downfile");
 * @example SZUtilsFile.downfilebyself("attachid","lookfile");
 */
var SZUtilsFile = SZUtilsFile || {};
/**
 * @desc
 * <p style="color:red;">
 * 用于附件内部使用
 * </p>
 */
SZUtilsFile.filedata = new Array();
/**
 * @desc
 * <p style="color:red;">
 * 用于附件内部使用
 * </p>
 */
SZUtilsFile._i = 0;
/**
 * @desc
 * <p>
 * 获取工程路径名称
 * </p>
 * <p style="color:red;">
 * 用于附件内部使用
 * </p>
 * @returns {string} 工程路径名称
 */
SZUtilsFile.getRootPath = function() {
	return "${basePath}";
};

/**
 * @desc
 * <p>
 * 获取缺省的资源路径
 * </p>
 * <p style="color:red;">
 * 用于附件内部使用
 * </p>
 * @returns {string} 缺省的资源路径
 */
SZUtilsFile.getDefaultResourcePath = function() {
	return '${basePath}${adminPath}/';
};

/**
 * @desc
 * <p>
 *	通过附件类型名称获取附件类型ID
 * </p>
 * <p style="color:red;">
 * 用于附件内部使用
 * </p>
 * @param {string} typeName
 * @returns {string} typeid
 */
SZUtilsFile.getTypeIdByTypeName = function(typeName) {
	if(!typeName){
		alert("附件类型名称不能为空！");
	}
	var typeid;
	$.ajax({
		url : SZUtilsFile.getDefaultResourcePath()
				+ '/attachType/gettypeid?typename='+typeName,
		type : "get",// 请求方法
		// 设置类型
		// dataType : 'json',
		// 设置请求方法
		async : false,
		cache : false,
		// 成功回调
		success : function(data) {
			typeid = data;
		},
		// 失败回调
		error : function(XMLHttpRequest, textStatus, errorThrown) {
			return;
		}
	});
	return typeid;
	
}
/**
 * @desc
 * <p>
 * 上传附件
 * </p>
 * <p style="color:red;">
 * 用于附件内部使用
 * </p>
 * @param {string}
 *            typeName 附件类型名称
 * @param {function}
 *            callback 回调函数
 * @param {string}
 *            refid 组ID
 * @example var refid; 
 * function callbackfunction(data){ 
 * //TODO 
 * }
 * SZUtilsFile.uploadfiletype('公共附件', callbackfunction,refid);
 */
SZUtilsFile.uploadfiletype = function(typeName, callback, refid) {
	var typeid = SZUtilsFile.getTypeIdByTypeName(typeName);
	if (!typeid) {
		top.$.messager.alert("提示","找不到对应附件类型");
		return;
	}
	if (refid) {
		SZUtilsFile.uploadfile(typeid, callback, refid);
	} else {
		SZUtilsFile.uploadfile(typeid, callback);
	}
};

/**
 * @desc
 * <p>
 * 获取guid
 * </p>
 * <p style="color:red;">
 * 用于附件内部使用
 * </p>
 * @returns {string} guid
 */
SZUtilsFile.guid = function guid() {
	return 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
		var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
		return v.toString(16);
	});
};

/**
 * @desc
 * <p>
 * 上传附件
 * </p>
 * <p style="color:red;">
 * 用于附件内部使用
 * </p>
 * @param {string}
 *            typeName 附件类型名称
 * @param {function}
 *            callback 回调函数
 * @param {string}
 *            refid 组ID
 * @example var refid; function callbackfunction(data){ //TODO }
 *          SZUtilsFile.uploadfile('NOTICE', callbackfunction,refid);
 */
SZUtilsFile.uploadfile = function(typeid, callback, refid) {
	if (!refid) {
		refid = SZUtilsFile.guid();
	}
	var N_MAXCOUNT = 999;
	var fileTypeExts = '*.*';
	var fileSizeLimit = "100MB";
	$.ajax({
		url : SZUtilsFile.getDefaultResourcePath()
				+ '/attach/gettypeinfo?attachid=' + typeid,
		type : "post",// 请求方法
		// 设置类型
		// dataType : 'json',
		// 设置请求方法
		async : false,
		// 成功回调
		success : function(data) {
			var obj = jQuery.parseJSON(data);
			if (obj.result.N_MAXCOUNT != -1) {
				N_MAXCOUNT = obj.result.N_MAXCOUNT;
			}
			var fileExt = obj.result.V_FILETYPE_EXTS;
			if(fileExt && fileExt!='*'){
				var fileExts = fileExt.split('|');
				//*.gif;*.jpg;*.jpeg;*.png
				fileTypeExts = "";
				for(var i=0;i<fileExts.length;i++){
					if(i!=0){
						fileTypeExts+= ";";
					}
					fileTypeExts+= "*."+fileExts[i];
				}
			}
			var maxsize = obj.result.N_MAXSIZE;
			if(maxsize && maxsize>=0){
				fileSizeLimit = maxsize + "KB";
			}
		},
		// 失败回调
		error : function(XMLHttpRequest, textStatus, errorThrown) {
			var result = jQuery.parseJSON(XMLHttpRequest.responseText);
			top.$.messager.alert('操作失败', "操作失败[" + result.errorDesc + "]");
			return;
		}
	});
	var p = $(
			'<div align="center"><input type="file"  style="text-align:center;" name="uploadify" id="'
					+ typeid + '" /></div>').dialog({
		title : "上传文件",
		height : 400,
		width : 430,
		modal :true,
		onClose : function() {
			try {
				$('#' + typeid).uploadify('destroy');
				p.dialog('destroy');
			} catch (e) {
				p.each(function() {
					try {
						$(this).prop('outerHTML', '');
					} catch (e) {
					}
				});
			}
		},
		buttons : [ {
			text : '开始上传',
			iconCls : "icon-save",
			handler : function() {
				$('#' + typeid).uploadify('upload', '*');
			}
		}, {
			text : '取消所有上传',
			iconCls : "icon-remove",
			handler : function() {
				$('#' + typeid).uploadify('cancel', '*');
			}
		} ]

	});
	try {
		$('#' + typeid).uploadify('destroy');
	} catch (e) {
	}
	var url = SZUtilsFile.getDefaultResourcePath() + '/attach/upfile';
	url += ";jsessionid=${authz.getSessionID()}?refid=" + refid + "&attachid="
			+ typeid;
	$("#" + typeid).uploadify(
			{
				'swf' : SZUtilsFile.getRootPath()
						+ '/static/res/uploadify/uploadify.swf',
				'uploader' : url,
				'auto' : false, // ture手动上传 false选择上传
				'method' : 'post',// 请求方法
				'buttonImage' : SZUtilsFile.getRootPath()
						+ '/static/res/uploadify/uploadify-browse.png', // 浏览按钮背景图片
				'height' : 30,
				'width' : 120,
				'multi' : true, // 多文件上传
				// 'simUploadLimit':10,
				// fileTypeExts: '*.gif;*.jpg;*.jpeg;*.png',
				'fileTypeExts' : fileTypeExts,// 上传文件类型
				'fileSizeLimit' : fileSizeLimit, // 上传文件的大小限制，单位为B, KB, MB, 或 GB
				'queueSizeLimit' : N_MAXCOUNT,// 选择文件个数
				// 'fileObjName' : 'uploadifys',
				// 'simUploadLimit' : 5,同时上传数量 收费
				// 'wmode' : transparent,//设置该项为transparent
				// 可以使浏览按钮的flash背景文件透明，并且flash文件会被置为页面的最高层。 默认值：opaque 。
				// 'queueID' : 'fileQueue',//与下面的id对应
				'onUploadStart' : function(file) {
					// $("#file_upload").uploadify("settings", "qq", );
				},
				'onSelectError' : SZUtilsFile.onSelectError,// 选择失败时触发一次
				'onUploadError' : SZUtilsFile.onUploadError,// 当上传返回错误时触发
				'onUploadSuccess' : SZUtilsFile.onUploadSuccess,// 每成功完成一次文件上传时触发一次
				'onQueueComplete' : function(data) {// 当队列中的所有文件全部完成上传时触发
					if (SZUtilsFile.verFile == true) {
						top.$.messager.alert("提示", "上传成功的文件数量"
								+ data.uploadsSuccessful);
						p.dialog('close');
						if(callback && typeof(callback)==='function')
							callback(SZUtilsFile.filedata);
						SZUtilsFile._i = 0;
						SZUtilsFile.filedata = [];
					}
				}

			});
	var userAgent = navigator.userAgent; // 取得浏览器的userAgent字符串
	var isOpera = userAgent.indexOf("Opera") > -1;
	// 判断是否Firefox浏览器
	if (userAgent.indexOf("Firefox") > -1) {
		$(".swfupload").css({
			"margin-left" : "-58px"
		});
	}
};

/**
 * @desc
 * <p>
 * 上传成功时调用的方法
 * </p>
 * <p style="color:red">
 * 用于附件内部使用
 * </p>
 */
SZUtilsFile.onUploadSuccess = function(file, data, response) {
	SZUtilsFile.verFile = true;
	var obj = jQuery.parseJSON(data);
	if (obj.flag == false) {
		SZUtilsFile.verFile = false;
		top.$.messager.alert("提示", obj.message);
		return;
	}
	SZUtilsFile.filedata[SZUtilsFile._i] = obj;
	SZUtilsFile._i++;
}

/**
 * @desc
 * <p>
 * 选择失败时触发
 * </p>
 * <p style="color:red">
 * 用于附件内部使用
 * </p>
 */
SZUtilsFile.onSelectError = function(file, errorCode, errorMsg) {
	if (errorCode) {
		var msgText = "上传失败\n";
		switch (errorCode) {
		case SWFUpload.QUEUE_ERROR.QUEUE_LIMIT_EXCEEDED:
			msgText += "每次最多上传 " + this.settings.queueSizeLimit + "个文件";
			break;
		case SWFUpload.QUEUE_ERROR.FILE_EXCEEDS_SIZE_LIMIT:
			msgText += "文件大小超过限制( " + this.settings.fileSizeLimit + " )";
			break;
		case SWFUpload.QUEUE_ERROR.ZERO_BYTE_FILE:
			msgText += "文件大小为0";
			break;
		case SWFUpload.QUEUE_ERROR.INVALID_FILETYPE:
			msgText += "文件格式不正确，仅限 " + this.settings.fileTypeExts;
			break;
		default:
			msgText += "错误代码：" + errorCode + "\n" + errorMsg;
		}
		top.$.messager.alert("提示", msgText);
	}
};

/**
 * @desc
 * <p>
 * 上传失败时触发
 * </p>
 * <p style="color:red">
 * 用于附件内部使用
 * </p>
 */
SZUtilsFile.onUploadError = function(file, errorCode, errorMsg, errorString) {
	top.$.messager.progress('close');
	if (errorCode) {
		// 手工取消不弹出提示
		if (errorCode == SWFUpload.UPLOAD_ERROR.FILE_CANCELLED
				|| errorCode == SWFUpload.UPLOAD_ERROR.UPLOAD_STOPPED) {
			return;
		}
		var msgText = "上传失败\n";
		switch (errorCode) {
		case SWFUpload.UPLOAD_ERROR.HTTP_ERROR:
			msgText += "HTTP 错误\n" + errorMsg;
			break;
		case SWFUpload.UPLOAD_ERROR.MISSING_UPLOAD_URL:
			msgText += "上传文件丢失，请重新上传";
			break;
		case SWFUpload.UPLOAD_ERROR.IO_ERROR:
			msgText += "IO错误";
			break;
		case SWFUpload.UPLOAD_ERROR.SECURITY_ERROR:
			msgText += "安全性错误\n" + errorMsg;
			break;
		case SWFUpload.UPLOAD_ERROR.UPLOAD_LIMIT_EXCEEDED:
			msgText += "每次最多上传 " + this.settings.uploadLimit + "个";
			break;
		case SWFUpload.UPLOAD_ERROR.UPLOAD_FAILED:
			msgText += errorMsg;
			break;
		case SWFUpload.UPLOAD_ERROR.SPECIFIED_FILE_ID_NOT_FOUND:
			msgText += "找不到指定文件，请重新操作";
			break;
		case SWFUpload.UPLOAD_ERROR.FILE_VALIDATION_FAILED:
			msgText += "参数错误";
			break;
		default:
			msgText += "文件:" + file.name + "\n错误码:" + errorCode + "\n"
					+ errorMsg + "\n" + errorString;
		}
		top.$.messager.alert("提示", msgText);
	}
};

/**
 * @desc
 * <p>
 * 删除文件
 * </p>
 * @param {string}
 *            ids ID字符串
 * @example SZUtilsFile.deleteAttachments("ID")
 */
SZUtilsFile.deleteAttachments = function(ids) {
	var url = SZUtilsFile.getDefaultResourcePath() + '/attach/';
	$.ajax({
		url : url + ids,
		// 设置类型
		// dataType : 'json',
		// 设置请求方法
		type : "DELETE",
		// 成功回调
		success : function(result) {
			parent.sy.messagerShow({
				msg : "操作成功!",
				title : '提示'
			});

		},
		// 失败回调
		error : function(XMLHttpRequest, textStatus, errorThrown) {
			var result = jQuery.parseJSON(XMLHttpRequest.responseText);
			top.$.messager.alert('操作失败', "操作失败[" + result.errorDesc + "]");
		}
	});
};

/**
 * @desc
 * <p>
 * 下载/在线预览(在线PDF打开)
 * </p>
 * @param {string}
 *            attachid 附件ID
 * @param {string}
 *            fileStatus downfile/lookfile 下载/在线预览(在线PDF打开)
 * @example SZUtilsFile.downfile("attachid","downfile");
 * @example SZUtilsFile.downfile("attachid","lookfile");
 */
SZUtilsFile.downfile = function(attachid, fileStatus)
{
	var path = SZUtilsFile.getDefaultResourcePath() + "/attach/downfile/"
			+ attachid + "/" + fileStatus;
	pwin = window.open(path);
	pwin.document.charset = "UTF-8";
};

/**
 * @desc
 * <p>
 * 下载/在线预览(用本机自带的软件打开)
 * </p>
 * @param {string}
 *            attachid 附件ID
 * @param {string}
 *            fileStatus downfile/lookfile 下载/在线预览(用本机自带的软件打开)
 * @example SZUtilsFile.downfilebyself("attachid","downfile");
 * @example SZUtilsFile.downfilebyself("attachid","lookfile");
 */
SZUtilsFile.downfilebyself = function(attachid, fileStatus)
{
	var path = SZUtilsFile.getDefaultResourcePath() + "/attach/downfilebyself/"
			+ attachid + "/" + fileStatus;
	pwin = window.open(path);
	pwin.document.charset = "UTF-8";
};
