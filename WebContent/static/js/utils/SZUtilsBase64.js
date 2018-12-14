/**
 * @file 提供BASE64常用方法，引用路径：&lt;script type="text/javascript" src="${path}/static/js/utils/SZUtilsBase64.js">&lt;/script>
 * @version v1.0.0
 * @since 2017-05-02
 * @author hyc
 */

/**
 * @desc SZUtilsBase64 提供BASE64常用方法
 * @version v1.0.0
 * @namespace SZUtilsBase64
 * @author hyc
 * @since 2017-05-02
 * @example SZUtilsBase64.encoder(str)
 * @example SZUtilsBase64.decoder(_base64Str)
 */
var SZUtilsBase64 = SZUtilsBase64 || {};

/**
 * create by 2012-08-25 pm 17:48
 * 本类库目前基于UTF-8编码实现的Base64编码和解码（支持utf-8能表示的所有字符的base64编码和解码，如汉字等），可以和java的BASE64编码对应。目前测试通过，但是其他编码的汉字如GBK编码，未测试，可能会出现无法解码的现象。
 * 
 * @author hexinglun@gmail.com BASE64 Encode and Decode By UTF-8 unicode
 *         可以和java的BASE64编码和解码互相转化
 */
(function() {
	var BASE64_MAPPING = [ 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
			'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
			'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
			'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'+', '/' ];

	/**
	 * ascii convert to binary
	 */
	var _toBinary = function(ascii) {
		var binary = new Array();
		while (ascii > 0) {
			var b = ascii % 2;
			ascii = Math.floor(ascii / 2);
			binary.push(b);
		}
		/*
		 * var len = binary.length; if(6-len > 0){ for(var i = 6-len ; i > 0 ;
		 * --i){ binary.push(0); } }
		 */
		binary.reverse();
		return binary;
	};

	/**
	 * binary convert to decimal
	 */
	var _toDecimal = function(binary) {
		var dec = 0;
		var p = 0;
		for ( var i = binary.length - 1; i >= 0; --i) {
			var b = binary[i];
			if (b == 1) {
				dec += Math.pow(2, p);
			}
			++p;
		}
		return dec;
	};

	/**
	 * unicode convert to utf-8
	 */
	var _toUTF8Binary = function(c, binaryArray) {
		var mustLen = (8 - (c + 1)) + ((c - 1) * 6);
		var fatLen = binaryArray.length;
		var diff = mustLen - fatLen;
		while (--diff >= 0) {
			binaryArray.unshift(0);
		}
		var binary = [];
		var _c = c;
		while (--_c >= 0) {
			binary.push(1);
		}
		binary.push(0);
		var i = 0, len = 8 - (c + 1);
		for (; i < len; ++i) {
			binary.push(binaryArray[i]);
		}

		for ( var j = 0; j < c - 1; ++j) {
			binary.push(1);
			binary.push(0);
			var sum = 6;
			while (--sum >= 0) {
				binary.push(binaryArray[i++]);
			}
		}
		return binary;
	};

	var __BASE64 = {
		/**
		 * @memberof SZUtilsBase64
		 * @desc 字符串转BASE64字符串
		 * @example SZUtilsBase64.encoder("")
		 * @param {string}
		 *            str 字符串
		 * @returns {string}BASE64字符串
		 */
		encoder : function(str) {
			var base64_Index = [];
			var binaryArray = [];
			for ( var i = 0, len = str.length; i < len; ++i) {
				var unicode = str.charCodeAt(i);
				var _tmpBinary = _toBinary(unicode);
				if (unicode < 0x80) {
					var _tmpdiff = 8 - _tmpBinary.length;
					while (--_tmpdiff >= 0) {
						_tmpBinary.unshift(0);
					}
					binaryArray = binaryArray.concat(_tmpBinary);
				} else if (unicode >= 0x80 && unicode <= 0x7FF) {
					binaryArray = binaryArray.concat(_toUTF8Binary(2,
							_tmpBinary));
				} else if (unicode >= 0x800 && unicode <= 0xFFFF) {// UTF-8
					// 3byte
					binaryArray = binaryArray.concat(_toUTF8Binary(3,
							_tmpBinary));
				} else if (unicode >= 0x10000 && unicode <= 0x1FFFFF) {// UTF-8
					// 4byte
					binaryArray = binaryArray.concat(_toUTF8Binary(4,
							_tmpBinary));
				} else if (unicode >= 0x200000 && unicode <= 0x3FFFFFF) {// UTF-8
					// 5byte
					binaryArray = binaryArray.concat(_toUTF8Binary(5,
							_tmpBinary));
				} else if (unicode >= 4000000 && unicode <= 0x7FFFFFFF) {// UTF-8
					// 6byte
					binaryArray = binaryArray.concat(_toUTF8Binary(6,
							_tmpBinary));
				}
			}

			var extra_Zero_Count = 0;
			for ( var i = 0, len = binaryArray.length; i < len; i += 6) {
				var diff = (i + 6) - len;
				if (diff == 2) {
					extra_Zero_Count = 2;
				} else if (diff == 4) {
					extra_Zero_Count = 4;
				}
				// if(extra_Zero_Count > 0){
				// len += extra_Zero_Count+1;
				// }
				var _tmpExtra_Zero_Count = extra_Zero_Count;
				while (--_tmpExtra_Zero_Count >= 0) {
					binaryArray.push(0);
				}
				base64_Index.push(_toDecimal(binaryArray.slice(i, i + 6)));
			}

			var base64 = '';
			for ( var i = 0, len = base64_Index.length; i < len; ++i) {
				base64 += BASE64_MAPPING[base64_Index[i]];
			}

			for ( var i = 0, len = extra_Zero_Count / 2; i < len; ++i) {
				base64 += '=';
			}
			return base64;
		},
		/**
		 * @memberof SZUtilsBase64
		 * @desc BASE64字符串转字符串(UTF-8)
		 * @example SZUtilsBase64.decoder("")
		 * @param {string}
		 *            _base64Str BASE64字符串
		 * @returns {string}字符串(UTF-8)
		 */
		decoder : function(_base64Str) {
			var _len = _base64Str.length;
			var extra_Zero_Count = 0;
			/**
			 * 计算在进行BASE64编码的时候，补了几个0
			 */
			if (_base64Str.charAt(_len - 1) == '=') {
				// alert(_base64Str.charAt(_len-1));
				// alert(_base64Str.charAt(_len-2));
				if (_base64Str.charAt(_len - 2) == '=') {// 两个等号说明补了4个0
					extra_Zero_Count = 4;
					_base64Str = _base64Str.substring(0, _len - 2);
				} else {// 一个等号说明补了2个0
					extra_Zero_Count = 2;
					_base64Str = _base64Str.substring(0, _len - 1);
				}
			}

			var binaryArray = [];
			for ( var i = 0, len = _base64Str.length; i < len; ++i) {
				var c = _base64Str.charAt(i);
				for ( var j = 0, size = BASE64_MAPPING.length; j < size; ++j) {
					if (c == BASE64_MAPPING[j]) {
						var _tmp = _toBinary(j);
						/* 不足6位的补0 */
						var _tmpLen = _tmp.length;
						if (6 - _tmpLen > 0) {
							for ( var k = 6 - _tmpLen; k > 0; --k) {
								_tmp.unshift(0);
							}
						}
						binaryArray = binaryArray.concat(_tmp);
						break;
					}
				}
			}

			if (extra_Zero_Count > 0) {
				binaryArray = binaryArray.slice(0, binaryArray.length
						- extra_Zero_Count);
			}

			var unicode = [];
			var unicodeBinary = [];
			for ( var i = 0, len = binaryArray.length; i < len;) {
				if (binaryArray[i] == 0) {
					unicode = unicode.concat(_toDecimal(binaryArray.slice(i,
							i + 8)));
					i += 8;
				} else {
					var sum = 0;
					while (i < len) {
						if (binaryArray[i] == 1) {
							++sum;
						} else {
							break;
						}
						++i;
					}
					unicodeBinary = unicodeBinary.concat(binaryArray.slice(
							i + 1, i + 8 - sum));
					i += 8 - sum;
					while (sum > 1) {
						unicodeBinary = unicodeBinary.concat(binaryArray.slice(
								i + 2, i + 8));
						i += 8;
						--sum;
					}
					unicode = unicode.concat(_toDecimal(unicodeBinary));
					unicodeBinary = [];
				}
			}
			return unicode;
		}
	};

	SZUtilsBase64 = __BASE64;
})();