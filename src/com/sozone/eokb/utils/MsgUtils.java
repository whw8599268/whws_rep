/**
 * 包名：com.sozone.eokb.utils
 * 文件名：MsgUtils.java<br/>
 * 创建时间：2017-12-20 下午1:40:22<br/>
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

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.base.utils.sms.SZUtilSMS;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;

/**
 * 短信工具类<br/>
 * <p>
 * 短信工具类<br/>
 * </p>
 * Time：2017-12-20 下午1:40:22<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MsgUtils
{

	/**
	 * 发送短息给运维人员<br/>
	 * <p>
	 * 发送短息给运维人员
	 * </p>
	 * 
	 * @param msg
	 *            短信内容
	 * @return 返回结果
	 */
	public static JSONObject send(String msg)
	{
		// SZUtilSMS sms = new SZUtilSMS();
		String flag = SystemParamUtils.getString(SysParamKey.MSG_SWITCH_KEY,
				"false");
		// 如果要发送
		if (Boolean.valueOf(flag))
		{
			return SZUtilSMS.sendRsJson(
					SystemParamUtils.getString(SysParamKey.PERSON_PHONES_KEY),
					msg);
		}
		JSONObject result = new JSONObject();
		result.put("success", false);
		return result;
	}

	/**
	 * 发送短息给运维人员<br/>
	 * <p>
	 * 发送短息给运维人员
	 * </p>
	 * 
	 * @param tempCode
	 *            短信模板ID
	 * @param params
	 *            参数
	 * @return 返回结果
	 */
	public static JSONObject send(String tempCode, Map<String, Object> params)
	{
		String flag = SystemParamUtils.getString(SysParamKey.MSG_SWITCH_KEY,
				"false");
		// 如果要发送
		if (Boolean.valueOf(flag))
		{
			return SZUtilSMS.sendRsJson(
					SystemParamUtils.getString(SysParamKey.PERSON_PHONES_KEY),
					tempCode, params);
		}
		JSONObject result = new JSONObject();
		result.put("success", false);
		return result;
	}

}
