/**
 * @file 提供FJCA常用方法，引用路径：&lt;script type="text/javascript" src="${path}/static/js/utils/SZUtilsFJCA.js">&lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 */

/**
 * @desc SZUtilsFJCA 提供FJCA常用方法
 * @version v1.0.0
 * @namespace SZUtilsFJCA
 * @author hyc
 * @since 2017-05-02
 * @example SZUtilsFJCA.loadFJCA('id');
 */

var SZUtilsFJCA = SZUtilsFJCA || {};

/**
 * @desc 获取FJCA证书信息
 * @param id
 *            {string} ID
 * @return {Map} map Map对象，读取不到为空
 * @example SZUtilsFJCA.loadFJCA('id');
 */
SZUtilsFJCA.loadFJCA = function(id) {
	if (!id || typeof (id) !== 'string') {
		return null;
	}
	try {
		var fjca = document.getElementById(id);
		var caTagOpen = fjca.OpenFJCAUSBKey();
		if (caTagOpen == false) {
			fjca.CloseUSBKey();
			return null;
		}
		var openReg = fjca.ReadCertNameFromKey();
		if (openReg == false) {
			fjca.CloseUSBKey();
			return null;
		}
		fjca.ReadCertFromKey();
		var certName = fjca.GetCertName();
		if (certName == null || "" == certName) {
			fjca.CloseUSBKey();
			return null;
		}
		var certData = fjca.GetCertData();
		// var jsonStr =
		// '[{"certName":"'+certName+'","certData":"'+certData+'"}]';
		fjca.CloseUSBKey();
		var map = new Map();
		map.put("certName", certName);
		map.put("certData", certData);
		// alert("fjca"+jsonStr);
		return map;
	} catch (ex) {
		try {
			fjca.CloseUSBKey();
		} catch (ex2) {
			alert("请先安装福建CA驱动,再插入CA USB-KEY,然后再进行身份验证!");
		}
		return null;
	}
};