/**
 * @file 提供form Serialize常用方法，引用路径：&lt;script type="text/javascript" src="${path}/static/js/utils/SZUtilsSerialize.js">&lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 */

/**
 * @desc SZUtilsSerialize 提供form Serialize常用方法
 * @version v1.0.0
 * @namespace SZUtilsSerialize
 * @author hyc
 * @since 2017-05-02
 * @example SZUtilsSerialize.serializeObject(form)
 * @example SZUtilsSerialize.serializeObjectWithComma(form)
 * @example SZUtilsSerialize.form_serializeObject(form)
 */
var SZUtilsSerialize = SZUtilsSerialize || {};

/**
 * @desc 将form表单元素的值序列化成对象，多值则取第一个
 * @param {jQueryObject}
 *            form 表单
 * @returns {object} 对象
 * @past FxzUtils_serializeObject(form)
 * @example SZUtilsSerialize.serializeObject(form)
 * @requires jQuery
 */
SZUtilsSerialize.serializeObject = function(form) {
	var _obj = {}; // 定义对象
	// 把表单序列化成数组
	$.each(form.serializeArray(), function(index) {
		var _name = this['name'];
		var _value = this['value'];
		if (!_obj[_name]) {
			_obj[_name] = _value;
		}
	});
	return _obj;
};

/**
 * @desc 将form表单元素的值序列化成对象，多值用逗号隔开
 * @param {jQueryObject}
 *            form 表单
 * @returns {object} 对象
 * @past Fsy.serializeObject(form)
 * @example SZUtilsSerialize.serializeObjectWithComma(form)
 * @requires jQuery
 */
SZUtilsSerialize.serializeObjectWithComma = function(form) {
	var o = {};
	$.each(form.serializeArray(), function(index) {
		if (o[this['name']]) {
			o[this['name']] = o[this['name']] + "," + this['value'];
		} else {
			o[this['name']] = this['value'];
		}
	});
	return o;
};

/**
 * @desc 将form表单元素的值序列化成对象，表单使用
 * @param {jQueryObject}
 *            form 表单
 * @returns {object} 对象
 * @example SZUtilsSerialize.form_serializeObject(form)
 * @requires jQuery
 */

SZUtilsSerialize.form_serializeObject = function(form) {
	var _obj = {}; // 定义对象
	var repeat = "";
	// 把表单序列化成数组
	$.each(form.serializeArray(), function(index) {
		var _name = this['name'];
		var _value = this['value'];

		if (!_obj[_name]) {
			if (/^AFCOL\d+CFCOL\d+$/.test(_name)) {
				repeat += repeat == "" ? _name : "," + _name;
				var param = [];
				$.each(form.find("[name='" + _name + "']").serializeArray(),
						function(i) {
							param.push(this['value']);
						});
				_obj[_name] = param;
			} else {
				_obj[_name] = _value;
			}
		} else {
			if (/^AFCOL\d+$/.test(_name)) {
				_obj[_name] = _obj[_name] + "," + _value;
			}
		}
	});
	_obj["SZFORMREPEAT"] = repeat;
	return _obj;
};
