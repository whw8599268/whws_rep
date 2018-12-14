/**
 * 字符串以什么开头
 */
String.prototype.startsWith = function(prefix) {
    return this.slice(0, prefix.length) === prefix;
};

/**
 * 字符串以什么结尾
 */
String.prototype.endsWith = function(prefix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};