/**
 * 包名：com.sozone.eokb.utils
 * 文件名：TenderProjectParamUtils.java<br/>
 * 创建时间：2017-12-8 上午11:39:51<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;

import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.eokb.common.ConstantEOKB;

/**
 * 招标项目参数工具类<br/>
 * <p>
 * 招标项目参数工具类<br/>
 * </p>
 * Time：2017-12-8 上午11:39:51<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class TenderProjectParamUtils
{

	/**
	 * 获取系统参数值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param key
	 *            键
	 * @param type
	 *            类型
	 * @return 值
	 */
	public static String getSystemParamValue(String key, String type)
	{
		String keyPrefix = "";
		// 如果是高速、普通、港航水运
		if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_GSGL_TYPE, type)
				|| StringUtils
						.equals(ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_PTGL_TYPE,
								type)
				|| StringUtils
						.equals(ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_SYGC_TYPE,
								type))
		{
			keyPrefix = "";
		}
		// 如果是厦门房建市政
		else if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE, type)
				|| StringUtils
						.equals(ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE,
								type))
		{
			keyPrefix = "xm.fjsz.";
		}
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("type", keyPrefix);
		// 表达式解析器
		StrSubstitutor strs = new StrSubstitutor(params);
		key = strs.replace(key);
		return SystemParamUtils.getString(key);
	}

	/**
	 * 获取系统参数值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param key
	 *            键
	 * @return 值
	 */
	public static String getSystemParamValue(String key)
	{
		// 获取招标项目类
		String type = SessionUtils.getTenderProjectType();
		return getSystemParamValue(key, type);
	}

	/**
	 * 获取系统参数值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param param
	 *            Record
	 * @return 值
	 */
	public static String getSystemParamValue(Record<String, Object> param)
	{
		String key = param.getString("KEY");
		String type = param.getString("TYPE");
		// 获取招标项目类
		return getSystemParamValue(key, type);
	}
}
