try {
	if (window.console && window.console.log) {
		console.log("%c安全警告", "color:red;font-weight:bold;font-size:72px;");
		console.log("%c控制台专供开发者使用。请不要在此粘贴执行任何内容，这可能会给您带来损失！",
				"color:orange;font-weight:bold;font-size:20px;");
		console.log("如果你想更了解我们，登录我司官网 %c www.okap.com", "color:red");
	}
} catch (e) {
};

var mca = {
	/**
	 * JSONP错误函数
	 * 
	 * @param fail
	 *            错误回调
	 */
	"onJSONPError" : function(fail) {
		// ie 8+, chrome and some other browsers
		var head = document.head || $('head')[0] || document.documentElement; // code
		// from
		// jquery
		var script = $(head).find('script')[0];
		script.onerror = function(evt) {
			// 关闭提示层
			layer.closeAll();
			if (typeof (eval(fail)) == "function") {
				try {
					fail(evt);
				} catch (e) {
				}
			} else {
				alert("调用JSONP请求失败!");
			}
			// do some clean

			// delete script node
			if (script.parentNode) {
				script.parentNode.removeChild(script);
			}
			// delete jsonCallback global function
			var src = script.src || '';
			var idx = src.indexOf('callback=');
			if (idx != -1) {
				var idx2 = src.indexOf('&');
				if (idx2 == -1) {
					idx2 = src.length;
				}
				var jsonCallback = src.substring(idx + 13, idx2);
				delete window[jsonCallback];
			}
		};
	},

	/**
	 * 获取多CA客户端版本号
	 * 
	 * @param func
	 *            成功后的回调函数,入参是多CA版本号
	 */
	"getVersion" : function(func) {
		layer.msg('正在获取云盾云签多CA客户端版本号,请稍候...', {
			icon : 16,
			shade : 0.3,
			time : false
		});
		$.ajax({
			url : 'http://localhost:9995/version',
			type : 'GET',
			dataType : "jsonp",
			error : function(XMLHttpRequest, textStatus, errorThrown) {
				// 关闭提示层
				layer.closeAll();
				alert("获取云盾云签多CA客户端版本号失败,请检查是否安装并打开云盾云签安全客户端!");
			},
			/*
			 * in ie 8, if service is down (or network occurs an error), the
			 * arguments will be:
			 * 
			 * testStatus: 'parsererror' ex.description: 'xxxx was not called'
			 * (xxxx is the name of jsoncallback function) ex.message: (same as
			 * ex.description) ex.name: 'Error'
			 */
			success : function(data) {
				// 关闭提示层
				layer.closeAll();
				// alert(JSON.stringify(data));
				if (data.success) {
					func(data.result);
					return;
				}
				alert(data.errorCode + ":" + data.errorDesc);
			}
		});
		mca.onJSONPError(function() {
			alert("获取云盾云签多CA客户端版本号失败,请检查是否安装并打开云盾云签安全客户端!");
		});
	},

	/**
	 * 获取证书颁发机构类型
	 * 
	 * @param func
	 *            成功回调函数,入参是颁发机构类型
	 */
	"getCertIssuerType" : function(func) {
		layer.msg('正在获取证书颁发机构类型,请稍候...', {
			icon : 16,
			shade : 0.3,
			time : false
		});
		$.ajax({
			url : 'http://localhost:9995/keytype',
			type : 'GET',
			dataType : "jsonp",
			error : function(XMLHttpRequest, textStatus, errorThrown) {
				// 关闭提示层
				layer.closeAll();
				alert("获取证书颁发机构类型失败,请检查是否安装并打开云盾云签安全客户端!");
			},
			/*
			 * in ie 8, if service is down (or network occurs an error), the
			 * arguments will be:
			 * 
			 * testStatus: 'parsererror' ex.description: 'xxxx was not called'
			 * (xxxx is the name of jsoncallback function) ex.message: (same as
			 * ex.description) ex.name: 'Error'
			 */
			success : function(data) {
				// 关闭提示层
				layer.closeAll();
				// alert(JSON.stringify(data));
				if (data.success) {
					func(data.result);
					return;
				}
				alert(data.errorCode + ":" + data.errorDesc);
			}
		});
		mca.onJSONPError(function() {
			alert("获取证书颁发机构类型失败,请检查是否安装并打开云盾云签安全客户端!");
		});
	},

	/**
	 * 获取证书信息
	 * 
	 * @param func
	 *            回调,参数证书信息
	 */
	"getCertInfo" : function(func) {
		layer.msg('正在获取证书信息,请稍候...', {
			icon : 16,
			shade : 0.3,
			time : false
		});
		$.ajax({
			url : 'http://localhost:9995/keyinfo',
			type : 'GET',
			dataType : "jsonp",
			error : function(XMLHttpRequest, textStatus, errorThrown) {
				// 关闭提示层
				layer.closeAll();
				alert("获取证书信息失败,请检查是否安装并打开云盾云签安全客户端!");
			},
			/*
			 * in ie 8, if service is down (or network occurs an error), the
			 * arguments will be:
			 * 
			 * testStatus: 'parsererror' ex.description: 'xxxx was not called'
			 * (xxxx is the name of jsoncallback function) ex.message: (same as
			 * ex.description) ex.name: 'Error'
			 */
			success : function(data) {
				// 关闭提示层
				layer.closeAll();
				// alert(JSON.stringify(data));
				if (data.success) {
					func(data.result);
					return;
				}
				alert(data.errorCode + ":" + data.errorDesc);
			}
		});
		mca.onJSONPError(function() {
			alert("获取证书信息失败,请检查是否安装并打开云盾云签安全客户端!");
		});
	},

	/**
	 * 获取软证书信息
	 * 
	 * @param func
	 *            回调,证书base64
	 */
	"getSoftCertInfo" : function(func) {
		layer.msg('正在获取证书信息,请稍候...', {
			icon : 16,
			shade : 0.3,
			time : false
		});
		$.ajax({
			url : 'http://localhost:9995/softinfo',
			type : 'GET',
			dataType : "jsonp",
			error : function(XMLHttpRequest, textStatus, errorThrown) {
				// 关闭提示层
				layer.closeAll();
				alert("获取证书信息失败,请检查是否安装并打开云盾云签安全客户端!");
			},
			/*
			 * in ie 8, if service is down (or network occurs an error), the
			 * arguments will be:
			 * 
			 * testStatus: 'parsererror' ex.description: 'xxxx was not called'
			 * (xxxx is the name of jsoncallback function) ex.message: (same as
			 * ex.description) ex.name: 'Error'
			 */
			success : function(data) {
				// 关闭提示层
				layer.closeAll();
				// alert(JSON.stringify(data));
				if (data.success) {
					func(data.result);
					return;
				}
				alert(data.errorCode + ":" + data.errorDesc);
			}
		});
		mca.onJSONPError(function() {
			alert("获取证书信息失败,请检查是否安装并打开云盾云签安全客户端!");
		});
	},

	/**
	 * 签名
	 * 
	 * @param data
	 *            要签名的内容
	 * @param type
	 *            类型0|1
	 * @param func
	 *            回调,签名值
	 * @returns
	 */
	"doSign" : function(data, type, func) {
		layer.msg('正在签名,请稍候...', {
			icon : 16,
			shade : 0.3,
			time : false
		});
		data = encodeURIComponent(data);
		$.ajax({
			url : 'http://localhost:9995/sign?data=' + data + '&type=' + type,
			type : 'GET',
			dataType : "jsonp",
			error : function(XMLHttpRequest, textStatus, errorThrown) {
				// 关闭提示层
				layer.closeAll();
				alert("签名失败,请检查是否安装并打开云盾云签安全客户端!");
			},
			/*
			 * in ie 8, if service is down (or network occurs an error), the
			 * arguments will be:
			 * 
			 * testStatus: 'parsererror' ex.description: 'xxxx was not called'
			 * (xxxx is the name of jsoncallback function) ex.message: (same as
			 * ex.description) ex.name: 'Error'
			 */
			success : function(data) {
				// 关闭提示层
				layer.closeAll();
				// alert(JSON.stringify(data));
				if (data.success) {
					func(data.result);
					return;
				}
				alert(data.errorCode + ":" + data.errorDesc);
			}
		});
		mca.onJSONPError(function() {
			alert("签名失败,请检查是否安装并打开云盾云签安全客户端!");
		});
	},

	/**
	 * 验签
	 * 
	 * @param src
	 *            签名前的值
	 * @param sign
	 *            签名值
	 * @param type
	 *            类型0|1
	 * @param func
	 *            回调
	 */
	"doVerify" : function(src, sign, type, func) {
		layer.msg('正在验签,请稍候...', {
			icon : 16,
			shade : 0.3,
			time : false
		});
		src = encodeURIComponent(src);
		$.ajax({
			url : 'http://localhost:9995/verify?data=' + src + '&sign=' + sign
					+ '&type=' + type,
			type : 'GET',
			dataType : "jsonp",
			error : function(XMLHttpRequest, textStatus, errorThrown) {
				// 关闭提示层
				layer.closeAll();
				alert("验签失败,请检查是否安装并打开云盾云签安全客户端!");
			},
			/*
			 * in ie 8, if service is down (or network occurs an error), the
			 * arguments will be:
			 * 
			 * testStatus: 'parsererror' ex.description: 'xxxx was not called'
			 * (xxxx is the name of jsoncallback function) ex.message: (same as
			 * ex.description) ex.name: 'Error'
			 */
			success : function(data) {
				// 关闭提示层
				layer.closeAll();
				// alert(JSON.stringify(data));
				if (data.success) {
					func(data.result);
					return;
				}
				alert(data.errorCode + ":" + data.errorDesc);
			}
		});
		mca.onJSONPError(function() {
			alert("验签失败,请检查是否安装并打开云盾云签安全客户端!");
		});
	},

	/**
	 * 加密
	 * 
	 * @param txt
	 *            加密前的值
	 * @param type
	 *            类型0|1
	 * @param func
	 *            回调
	 */
	"doEncrypt" : function(txt, type, func) {
		layer.msg('正在加密,请稍候...', {
			icon : 16,
			shade : 0.3,
			time : false
		});
		txt = encodeURIComponent(txt);
		$
				.ajax({
					url : 'http://localhost:9995/encrypt?data=' + txt
							+ '&type=' + type,
					type : 'GET',
					dataType : "jsonp",
					error : function(XMLHttpRequest, textStatus, errorThrown) {
						// 关闭提示层
						layer.closeAll();
						alert("加密失败,请检查是否安装并打开云盾云签安全客户端!");
					},
					/*
					 * in ie 8, if service is down (or network occurs an error),
					 * the arguments will be:
					 * 
					 * testStatus: 'parsererror' ex.description: 'xxxx was not
					 * called' (xxxx is the name of jsoncallback function)
					 * ex.message: (same as ex.description) ex.name: 'Error'
					 */
					success : function(data) {
						// 关闭提示层
						layer.closeAll();
						// alert(JSON.stringify(data));
						if (data.success) {
							func(data.result);
							return;
						}
						alert(data.errorCode + ":" + data.errorDesc);
					}
				});
		mca.onJSONPError(function() {
			alert("加密失败,请检查是否安装并打开云盾云签安全客户端!");
		});
	},

	/**
	 * 解密
	 * 
	 * @param mi
	 *            密文
	 * @param type
	 *            类型0|1
	 * @param func
	 *            回调
	 */
	"doDecrypt" : function(mi, type, func) {
		layer.msg('正在解密,请稍候...', {
			icon : 16,
			shade : 0.3,
			time : false
		});
		$.ajax({
			url : 'http://localhost:9995/decrypt?data=' + mi + '&type=' + type,
			type : 'GET',
			dataType : "jsonp",
			error : function(XMLHttpRequest, textStatus, errorThrown) {
				// 关闭提示层
				layer.closeAll();
				alert("解密失败,请检查是否安装并打开云盾云签安全客户端!");
			},
			/*
			 * in ie 8, if service is down (or network occurs an error), the
			 * arguments will be:
			 * 
			 * testStatus: 'parsererror' ex.description: 'xxxx was not called'
			 * (xxxx is the name of jsoncallback function) ex.message: (same as
			 * ex.description) ex.name: 'Error'
			 */
			success : function(data) {
				// 关闭提示层
				layer.closeAll();
				// alert(JSON.stringify(data));
				if (data.success) {
					func(data.result);
					return;
				}
				alert(data.errorCode + ":" + data.errorDesc);
			}
		});
		mca.onJSONPError(function() {
			alert("解密失败,请检查是否安装并打开云盾云签安全客户端!");
		});
	}
};
