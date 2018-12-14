/**
 * @file 提供时间常用方法以及Map类，引用路径：&lt;script type="text/javascript" src="${path}/static/js/utils/SZUtilsDate.js">&lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 */

/**
 * @desc SZUtilsDate 提供时间常用方法
 * @version v1.0.0
 * @namespace SZUtilsDate
 * @author hyc
 * @since 2017-05-02
 * @example SZUtilsDate.format(new Date('2017-05-02'),'yyyy-MM-dd');
 * @example SZUtilsDate.format('2017-05-02','yyyy-MM-dd');
 * @example SZUtilsDate.format(1488330036996);
 */
var SZUtilsDate = SZUtilsDate || {};

/**
 * @desc 时间格式化
 * @param date
 *            {number|string|Date} 毫秒数、时间字符串、Date对象
 * @param format
 *            {string} 格式化字符串
 * @return {string} 格式化的时间字符串
 * @example SZUtilsDate.format(new Date('2017-05-02'),'yyyy-MM-dd');
 * @example SZUtilsDate.format('2017-05-02','yyyy-MM-dd');
 * @example SZUtilsDate.format(1488330036996);
 */
SZUtilsDate.format = function(date, format) {
	if (!format) {
		format = "yyyy-MM-dd hh:mm:ss";
	}
	if (typeof (date) === 'object') {
		try {
			return date.format(format);
		} catch (e) {
			return '';
		}
	}
	if (typeof (date) === 'string' || typeof (date) === 'number') {
		try {
			return new Date(date).format(format);
		} catch (e) {
			return '';
		}
	}
	return '';
};

/**
 * @classdesc JavaScript对象Date
 * @class Date
 * @requires SZUtilsDate.js
 */
/**
 * @method format
 * @memberof Date
 * @desc 时间格式化
 * @param format
 *            {string} 可选 格式化字符串(默认"yyyy-MM-dd hh:mm:ss")
 * @return {string} 格式化的时间字符串
 * @example date.format("yyyy-MM-dd");
 * @example date.format();
 */
Date.prototype.format = function(format) {
	if (isNaN(this.getMonth())) {
		return '';
	}
	if (!format) {
		format = "yyyy-MM-dd hh:mm:ss";
	}
	var o = {
		/* month */
		"M+" : this.getMonth() + 1,
		/* day */
		"d+" : this.getDate(),
		/* hour */
		"h+" : this.getHours(),
		/* minute */
		"m+" : this.getMinutes(),
		/* second */
		"s+" : this.getSeconds(),
		/* quarter */
		"q+" : Math.floor((this.getMonth() + 3) / 3),
		/* millisecond */
		"S" : this.getMilliseconds()
	};
	if (/(y+)/.test(format)) {
		format = format.replace(RegExp.$1, (this.getFullYear() + "")
				.substr(4 - RegExp.$1.length));
	}
	for ( var k in o) {
		if (new RegExp("(" + k + ")").test(format)) {
			format = format.replace(RegExp.$1, RegExp.$1.length == 1 ? o[k]
					: ("00" + o[k]).substr(("" + o[k]).length));
		}
	}
	return format;
};
function DateMinus(sDate){
	　　var sdate = new Date(sDate.replace(/-/g, "/"));
	　　var now = new Date();
	　　var days = now.getTime() - sdate.getTime();
	　　var day = parseInt(days / (1000 * 60 * 60 * 24));
	　　return day;
	}