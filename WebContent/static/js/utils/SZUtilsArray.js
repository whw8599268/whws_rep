/**
 * @file 提供数组常用方法，引用路径：&lt;script type="text/javascript" src="${path}/static/js/utils/SZUtilsArray.js">&lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 * 
 */

/**
 * @desc SZUtilsArray 提供数组常用方法
 * @version v1.0.0
 * @namespace SZUtilsArray
 * @author hyc
 * @since 2017-05-02
 * @example SZUtilsArray.arrayToJson(array)
 */
var SZUtilsArray = SZUtilsArray || {};

/**
 * 数组转JSON字符串
 * 
 * @example SZUtilsArray.arrayToJson(array)
 * 
 * @param {array}
 *            array 数组
 * @returns {string} JSON 字符串
 * @author hym
 * @since 2011-12-30
 */
SZUtilsArray.arrayToJson = function(array) {
	var r = [];
	if (typeof (array) == "string")
		return "\""
				+ array.replace(/([\'\"\\])/g, "\\$1").replace(/(\n)/g, "\\n")
						.replace(/(\r)/g, "\\r").replace(/(\t)/g, "\\t") + "\"";
	if (typeof (array) == "object") {
		if (!array.sort) {
			for ( var i in array)
				r.push(i + ":" + arrayToJson(array[i]));
			if (!!document.all
					&& !/^\n?function\s*toString\(\)\s*\{\n?\s*\[native code\]\n?\s*\}\n?\s*$/
							.test(array.toString)) {
				r.push("toString:" + array.toString.toString());
			}
			r = "{" + r.join() + "}";
		} else {
			for ( var i = 0; i < array.length; i++) {
				r.push(arrayToJson(array[i]));
			}
			r = "[" + r.join() + "]";
		}
		return r;
	}
	return array.toString();
};

/**
 * @classdesc JavaScript对象Array
 * @class Array
 * @example var array=new Array(); var array = [];
 * @requires SZUtilsArray.js
 */

/**
 * @desc 去数组中重复的元素
 * @method unique
 * @memberof Array
 * @returns {Array} 去重复元素后的数组
 * @example var array=[1,1,2];array.unique();
 */
Array.prototype.unique = function() {
	this.sort();
	var re = [ this[0] ];
	for ( var i = 1; i < this.length; i++) {
		if (this[i] !== re[re.length - 1]) {
			re.push(this[i]);
		}
	}
	return re;
};

/**
 * @desc 并集去重复后的数组
 * @method union
 * @memberof Array
 * @array {Array} array 数组
 * @returns {Array} 并集去重复后的数组
 * @example var array=[1,1,2];array.union([3,4,5]);
 */
Array.prototype.union = function(array) {
	return this.concat(array).unique();
};

/**
 * @desc 差集 this-input 去重
 * @method minus
 * @memberof Array
 * @array {Array} array 数组
 * @returns {Array} 差集去重复后的数组
 * @example var array=[1,1,2];array.minus([2,3]);
 */
Array.prototype.minus = function(array) {
	var result = [];
	var clone = this;
	for ( var i = 0; i < clone.length; i++) {
		var flag = true;
		for ( var j = 0; j < array.length; j++) {
			if (clone[i] == array[j])
				flag = false;
		}
		if (flag)
			result.push(clone[i]);
	}
	return result.unique();
};

/**
 * @desc 交集 this 并 input 去重
 * @method intersect
 * @memberof Array
 * @array {Array} array 数组
 * @returns {Array} 交集去重复后的数组
 * @example var array=[1,1,2];array.intersect([2,3]);
 */
Array.prototype.intersect = function(array) {
	var result = [];
	var a = this;
	for ( var i = 0; i < array.length; i++) {
		var temp = array[i];
		for ( var j = 0; j < a.length; j++) {
			if (temp === a[j]) {
				result.push(temp);
				break;
			}
		}
	}
	return result.unique();
};

/**
 * @classdesc 自定义类、Map类和java的Map一样
 * @class Map
 * @constructor
 * @version v1.0.0
 * @author hym
 * @since 2012-02-01
 * @requires SZUtilsArray.js
 * @example var map=new Map(); map.put("KEY","VALUE");
 * @example var map=new Map(); map.get("KEY");
 * @example var map=new Map(); map.size();
 * @example var map=new Map(); map.isEmpty();
 */
var Map = function() {

	var entry = function(key, value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * @desc Map添加
	 * @Method put
	 * @memberof Map
	 * @param {var}
	 *            key 键
	 * @param {var}
	 *            value 值
	 * @example map.put("KEY","VALUE");
	 */
	var put = function(key, value) {
		for ( var i = 0; i < this.arr.length; i++) {
			if (this.arr[i].key === key) {
				this.arr[i].value = value;
				return;
			}
		}
		this.arr[this.arr.length] = new entry(key, value);
	}

	/**
	 * @desc Map 获取
	 * @Method get
	 * @memberof Map
	 * @param {var}
	 *            key 键
	 * @return {var} value 值
	 * @example map.get("KEY");
	 */
	var get = function(key) {
		for ( var i = 0; i < this.arr.length; i++) {
			if (this.arr[i].key === key) {
				return this.arr[i].value;
			}
		}
		return null;
	}

	/**
	 * @desc Map删除
	 * @Method remove
	 * @memberof Map
	 * @param {var}
	 *            key 键
	 * @example map.remove("KEY");
	 */
	var remove = function(key) {
		var v;
		for ( var i = 0; i < this.arr.length; i++) {
			v = this.arr.pop();
			if (v.key === key) {
				continue;
			}
			this.arr.unshift(v);
		}
	}

	/**
	 * @desc 获取Map的size
	 * @Method size
	 * @memberof Map
	 * @returns {number} size Map大小
	 * @example map.size();
	 */
	var size = function() {
		return this.arr.length;
	}
	/**
	 * @desc 判断Map是否为空
	 * @Method isEmpty
	 * @memberof Map
	 * @returns {boolean} true/false
	 * @example map.isEmpty();
	 */
	var isEmpty = function() {
		return this.arr.length <= 0;
	}
	this.arr = new Array();
	this.get = get;
	this.put = put;
	this.remove = remove;
	this.size = size;
	this.isEmpty = isEmpty;
};
