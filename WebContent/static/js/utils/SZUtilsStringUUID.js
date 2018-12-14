/**
 * @file 提供获取UUID常用方法，引用路径：&lt;script type="text/javascript" src="${path}/static/js/utils/SZUtilsStringUUID.js">&lt;/script>&lt;script type="text/javascript" src="${path}/static/js/others/UUID.js">&lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 */

/**
 * @desc SZUtilsStringUUID 提供获取UUID常用方法
 * @version v1.0.0
 * @namespace SZUtilsStringUUID
 * @author hyc
 * @since 2017-05-02
 * @requires ../others/JSON.js
 * @example SZUtilsStringUUID.get36UUID();
 * @example SZUtilsStringUUID.get32UUID();
 */
var SZUtilsStringUUID = SZUtilsStringUUID || {};

/**
 * @desc 获取36位UUID（默认大写）
 * @returns {string} 36位UUID
 * @example SZUtilsStringUUID.get36UUID();
 */
SZUtilsStringUUID.get36UUID = function() {
	return new UUID().toString();
};

/**
 * @desc 获取32位UUID（默认大写）
 * @returns {string} 32位UUID
 * @example SZUtilsStringUUID.get32UUID();
 */
SZUtilsStringUUID.get32UUID = function() {
	var uuid36 = SZUtilsStringUUID.get36UUID();
	if (uuid36 && typeof (uuid36) === 'string') {
		return uuid36.replace(/-/g, '');
	}
	return '';
};