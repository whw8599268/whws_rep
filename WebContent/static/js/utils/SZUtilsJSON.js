/**
 * @file 提供JSON常用方法，引用路径：&lt;script type="text/javascript" src="${path}/static/js/utils/SZUtilsJSON.js">&lt;/script>&lt;script type="text/javascript" src="${path}/static/js/others/JSON.js">&lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 */

/**
 * @desc SZUtilsJSON 提供JSON常用方法
 * @version v1.0.0
 * @requires ../others/JSON.js
 * @namespace SZUtilsJSON
 * @author hyc
 * @since 2017-05-02
 * @example SZUtilsJSON.stringify({name:123});
 * @example SZUtilsJSON.parse("{name:123}");
 */
var SZUtilsJSON = SZUtilsJSON || {};

/**
 * @borrows JSON.stringify as stringify
 * @method stringify
 * @memberof SZUtilsJSON
 * @desc 对象转JSON字符串
 * @param {object}
 *            value 将要序列化成 一个JSON 字符串的值。
 * @param {function|null}replacer
 *            可选 如果该参数是一个函数，则在序列化过程中，被序列化的值的每个属性都会经过该函数的转换和处理；
 *            如果该参数是一个数组，则只有包含在这个数组中的属性名才会被序列化到最终的 JSON
 *            字符串中；如果该参数为null或者未提供，则对象所有的属性都会被序列化；
 * @param {string|number|null}space
 *            可选 指定缩进用的空白字符串，用于美化输出（pretty-print）；如果参数是个数字，它代表有多少的空格；上限为10。
 *            改值若小于1，则意味着没有空格；如果该参数为字符串(字符串的前十个字母)，该字符串将被作为空格；
 *            如果该参数没有提供（或者为null）将没有空格。
 * @return {String} JSON字符串
 * @example SZUtilsJSON.stringify({name:123});
 */
SZUtilsJSON.stringify = JSON.stringify;

/**
 * @borrows JSON.parse as parse
 * @method parse
 * @memberof SZUtilsJSON
 * @desc JSON字符串转对象
 * @param {string}text
 *            要被解析成JavaScript值的字符串
 * @param {function|null}
 *            reviver 可选 如果是一个函数，则规定了原始值如何被解析改造，在被返回之前。
 * @return {object} 对象
 * @example SZUtilsJSON.parse("{name:123}");
 */
SZUtilsJSON.parse = JSON.parse;