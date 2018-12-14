/**
 * 包名：com.sozone.ebidpb.utils
 * 文件名：SessionUtils.java<br/>
 * 创建时间：2017-8-3 下午4:51:34<br/>
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

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.common.config.Global;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.eokb.common.ConstantEOKB;

/**
 * Session会话工具类<br/>
 * <p>
 * Session会话工具类<br/>
 * </p>
 * Time：2017-8-3 下午4:51:34<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SessionUtils
{
	private SessionUtils()
	{
	}

	/**
	 * 设置属性<br/>
	 * <p>
	 * </p>
	 * 
	 * @param key
	 *            键
	 * @param value
	 *            值
	 */
	public static void setAttribute(Object key, Object value)
	{
		ApacheShiroUtils.getSession().setAttribute(key, value);
	}

	/**
	 * 获取属性<br/>
	 * <p>
	 * </p>
	 * 
	 * @param key
	 *            键
	 * @return 值
	 * @param <T>
	 *            参数值泛型
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAttribute(Object key)
	{
		return (T) ApacheShiroUtils.getSession().getAttribute(key);
	}

	/**
	 * 获取存放在SESSION中的招标项目信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @return SESSION中的招标项目信息
	 */
	public static Record<String, Object> getTenderProjectInfo()
	{
		return getAttribute(ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY);
	}

	/**
	 * 
	 * 获取招标项目信息表ID<br/>
	 * 
	 * @return 项目信息表ID
	 */
	public static String getTPID()
	{
		return getTenderProjectInfo().getString("ID");
	}

	/**
	 * 
	 * 获取项目名称<br/>
	 * 
	 * @return 开标项目名称
	 */
	public static String getBidProjectName()
	{
		return getTenderProjectInfo().getString("V_TENDER_PROJECT_NAME");
	}

	/**
	 * 
	 * 获取开标时间<br/>
	 * 
	 * @return 开标时间
	 */
	public static String getBidOpenTime()
	{
		return getTenderProjectInfo().getString("V_BIDOPEN_TIME");
	}

	/**
	 * 
	 * 获取是否采用标段组<br/>
	 * 
	 * @return boolean
	 */
	public static boolean isSectionGroup()
	{
		Integer flag = getTenderProjectInfo().getInteger("N_IS_SECTION_GROUP");
		if (null == flag)
		{
			return false;
		}
		return 1 == flag;
	}

	/**
	 * 
	 * 获取招标项目编号<br/>
	 * 
	 * @return 招标项目编号
	 */
	public static String getTenderProjectCode()
	{
		return getTenderProjectInfo().getString("V_TENDER_PROJECT_CODE");
	}

	/**
	 * 
	 * 获取评标办法JSON 中的评标办法类型<br/>
	 * 
	 * @return 获取评标办法JSON 中的评标办法类型
	 */
	public static String getTenderProjectTypeCode()
	{
		String bemJson = getTenderProjectInfo().getString("V_BEM_INFO_JSON");
		if (StringUtils.isEmpty(bemJson))
		{
			return null;
		}
		JSONObject bem = JSON.parseObject(bemJson);
		return bem.getString("V_CODE");
	}

	/**
	 * 
	 * 获取评标办法JSON 中的评标办法类型(中文)<br/>
	 * 
	 * @return 获取评标办法JSON 中的评标办法类型(中文)
	 */
	public static String getTenderProjectTypeCodeCn()
	{
		String bemJson = getTenderProjectInfo().getString("V_BEM_INFO_JSON");
		if (StringUtils.isEmpty(bemJson))
		{
			return null;
		}
		JSONObject bem = JSON.parseObject(bemJson);
		return bem.getString("V_BID_EVALUATION_METHOD_NAME");
	}

	/**
	 * 
	 * 获取招标项目ID<br/>
	 * 
	 * @return 招标项目ID
	 */
	// public static String getTenderProjectID()
	// {
	// return getTenderProjectInfo().getString("V_TENDER_PROJECT_ID");
	// }

	/**
	 * 
	 * 获取招标项目类型<br/>
	 * 
	 * @return 招标项目ID
	 */
	public static String getTenderProjectType()
	{
		return getTenderProjectInfo().getString("V_TENDERPROJECT_APP_TYPE");
	}

	/**
	 * 获取当前登陆用户组织机构代码
	 * 
	 * @return 当前登陆用户组织机构代码
	 * @throws ValidateException
	 *             ValidateException
	 */
	public static String getCompanyCode() throws ValidateException
	{
		// ApacheShiroUtils.getCurrentUserID();
		return ApacheShiroUtils.getCurrentUser().getColumn("V_COMPANY_CODE");
	}

	/**
	 * 获取当前登陆用户统一社会代码
	 * 
	 * @return 当前登陆用户统一社会代码
	 * @throws ValidateException
	 *             ValidateException
	 */
	public static String getSocialcreditNO() throws ValidateException
	{
		return ApacheShiroUtils.getCurrentUser().getColumn("V_SOCIALCREDIT_NO");
	}

	/**
	 * 获取当前登陆用户证书名称
	 * 
	 * @return 当前登陆用户证书名称
	 * @throws ValidateException
	 *             ValidateException
	 */
	public static String getLoginName() throws ValidateException
	{
		return ApacheShiroUtils.getCurrentUser().getColumn("V_LOGIN_NAME");
	}

	/**
	 * 获取当前登陆用户企业名称名称
	 * 
	 * @return 当前登陆用户企业名称名称
	 * @throws ValidateException
	 *             ValidateException
	 */
	public static String getCompanyName() throws ValidateException
	{
		return ApacheShiroUtils.getCurrentUser().getColumn("V_NAME");
	}

	/**
	 * 获取招标项目对应的流程信息<br/>
	 * 
	 * @return 招标项目对应的流程信息
	 * @throws ValidateException
	 *             ValidateException
	 */
	public static Record<String, Object> getTenderProjectFlow()
			throws ValidateException
	{
		return getAttribute(ConstantEOKB.TENDER_PROJECT_FLOW_INFO_SESSION_KEY);
	}

	/**
	 * 获取招标项目对应的流程ID<br/>
	 * 
	 * @return 招标项目对应的流程ID
	 * @throws ValidateException
	 *             ValidateException
	 */
	public static String getTenderProjectFlowID() throws ValidateException
	{
		return getTenderProjectFlow().getString("ID");
	}

	/**
	 * 获取招标项目对应的评标办法<br/>
	 * 
	 * @return 获取招标项目对应的评标办法
	 * @throws ValidateException
	 *             ValidateException
	 */
	public static int getEvaluationMethodType() throws ValidateException
	{
		return getTenderProjectInfo()
				.getInteger("V_BID_EVALUATION_METHOD_TYPE");
	}

	/**
	 * 判断当前用户是否为投标人<br/>
	 * <p>
	 * <font color="red">注意:是投标人,是投标人,不是招标人,不要眼瘸看错了</font>
	 * </p>
	 * 
	 * @return boolean true/false
	 * @throws ValidateException
	 *             ValidateException
	 */
	public static boolean isBidder() throws ValidateException
	{
		String flag = getAttribute("roleCode");
		return StringUtils.equals("1", flag);
	}

	/**
	 * 获取当前用户组织结构代码或者统一社会信用代码<br/>
	 * <p>
	 * </p>
	 * 
	 * @return 组织结构代码或者统一社会信用代码
	 * @throws ValidateException
	 *             校验异常
	 */
	public static String getEntUniqueCode() throws ValidateException
	{
		// 组织结构代码
		String zzjgdm = getCompanyCode();
		// 统一社会信用代码 (志忠那边现在没有统一社会信用代码占时放空)
		// String tyshxhdm = getSocialcreditNO();
		String tyshxhdm = "";
		String code = StringUtils.defaultIfEmpty(tyshxhdm, zzjgdm);
		if (StringUtils.isEmpty(code))
		{
			throw new ValidateException("E-2005");
		}
		return code;
	}

	/**
	 * 
	 * 根据主题切换招标项目的类型 <br/>
	 * <p>
	 * 招标项目的类型 （10 高速公路 20 普通公路）
	 * </p>
	 * 
	 * @param request
	 *            HttpServletRequest
	 * @return 招标项目的类型
	 */
	public static String getTenderProjectAppType(HttpServletRequest request)
	{
		String theme = Global.getTheme(request);
		// 高速公路
		if (StringUtils.equals(theme, "/fjs_gsgl"))
		{
			return "10";
		}
		// 普通公路
		else if (StringUtils.equals(theme, "/fjs_ptgl"))
		{
			return "20";
		}
		// 水运工程
		else if (StringUtils.equals(theme, "/fjs_sygc"))
		{
			return "30";
		}
		// 房屋建筑
		else if (StringUtils.equals(theme, "/xms_fwjz"))
		{
			return "A01";
		}
		// 市政工程
		else if (StringUtils.equals(theme, "/xms_szgc"))
		{
			return "A02";
		}
		return null;
	}

	/**
	 * 
	 * 获取摇球标识<br/>
	 * <p>
	 * 获取摇球标识
	 * </p>
	 * 
	 * @return 摇球标识
	 */
	public static boolean getSieveFlag()
	{
		return null == getAttribute("SIEVE") ? false
				: (Boolean) getAttribute("SIEVE");
	}

	/**
	 * 
	 * 是否是国道养护<br/>
	 * <p>
	 * 是否是国道养护
	 * </p>
	 * 
	 * @return 是否是国道养护
	 */
	public static boolean isPtGdyh()
	{
		String bemJson = getTenderProjectInfo().getString("V_BEM_INFO_JSON");
		if (StringUtils.isEmpty(bemJson))
		{
			return false;
		}
		JSONObject bem = JSON.parseObject(bemJson);
		return StringUtils.contains(bem.getString("V_CODE"), "fjs_ptgl_gdyh");
	}

	/**
	 * 
	 * 获取主题名称 <br/>
	 * 
	 * @param request
	 *            HttpServletRequest
	 * @return 主题名称
	 */
	public static String getThemeName(HttpServletRequest request)
	{
		String theme = Global.getTheme(request);
		// 高速公路
		if (StringUtils.equals(theme, "/fjs_gsgl"))
		{
			return "高速公路电子招投标";
		}
		// 普通公路
		else if (StringUtils.equals(theme, "/fjs_ptgl"))
		{
			return "普通公路电子招投标";
		}
		// 水运工程
		else if (StringUtils.equals(theme, "/fjs_sygc"))
		{
			return "水运工程电子招投标";
		}
		// 房屋建筑
		else if (StringUtils.equals(theme, "/xms_fwjz"))
		{
			return "厦门市建设工程电子招投标交易平台(房屋建筑)";
		}
		// 市政工程
		else if (StringUtils.equals(theme, "/xms_szgc"))
		{
			return "厦门市建设工程电子招投标交易平台(市政工程)";
		}
		return "";
	}

	/**
	 * 是否使用两次解密模式<br/>
	 * <p>
	 * 1、厦门监理、勘察、设计<br/>
	 * 2、因为材料设备的项目不一定会请公证人员，所以在解密时，第二层公证解密要改成公证/交易中心解密。<br/>
	 * 3、所有小项目<br/>
	 * 4、招标文件制作时主动选中要求两层<br/>
	 * </p>
	 * 
	 * @return boolean true/false
	 */
	public static boolean isTowLayerDecrypt()
	{
		// 先判断小项目
		JSONObject jobj = getTenderProjectInfo().getJSONObject("V_JSON_OBJ");
		if (null != jobj)
		{
			// 如果是小项目
			Boolean isSTP = jobj.getBoolean("IS_SMALL_TP");
			// 如果是小项目，第二次使用交易中心解密
			if (null != isSTP && isSTP)
			{
				return true;
			}
		}
		// 设置评标办法
		JSONObject pbMethod = getAttribute(ConstantEOKB.PB_METHOD_JSON_INFO_SESSION_KEY);
		if (null != pbMethod)
		{
			String flag = pbMethod.getString("BID_OPEN_DECRYPTION_METHOD");
			// 如果在招标文件制作时已经选中了第二层公证解密要交易中心解密。
			if (StringUtils.equals("1", flag))
			{
				return true;
			}
		}

		Pattern p = Pattern.compile("^xms\\_\\w{4}\\_(jl|kc|sj|cl|sb)\\_.*");
		String code = getTenderProjectTypeCode();
		return p.matcher(code).matches();
	}

	// public static void main(String[] args)
	// {
	// String[] codes = new String[] {
	// // 厦门市_房屋建筑_监理_综合评估法_v1
	// "xms_fwjz_jl_zhpgf_v1",
	// // 厦门市_房屋建筑_监理_经评审的选取法_v1
	// "xms_fwjz_jl_jpsdxqf_v1",
	//
	// // 厦门市_房屋建筑_勘察_简易评估法_资格后审_v1
	// "xms_fwjz_kc_jypgf_zghs_v1",
	// // 厦门市_房屋建筑_勘察_简易评估法_资格预审_前_v1
	// "xms_fwjz_kc_jypgf_zghs_v1_1",
	// // 厦门市_房屋建筑_勘察_简易评估法_资格预审_后_v1
	// "xms_fwjz_kc_jypgf_zghs_v1_2",
	//
	// // 厦门市_房屋建筑_勘察_综合评估法_资格后审_v1
	// "xms_fwjz_kc_zhpgf_zghs_v1",
	// // 厦门市_房屋建筑_勘察_综合评估法_资格后审_前_v1
	// "xms_fwjz_kc_zhpgf_zghs_v1_1",
	// // 厦门市_房屋建筑_勘察_综合评估法_资格后审_后_v1
	// "xms_fwjz_kc_zhpgf_zghs_v1_2",
	//
	// // 厦门市_房屋建筑_设计13版_记名投票法_v1
	// "xms_fwjz_sj_jmtpf_v1",
	// // 厦门市_房屋建筑_设计13版_记名投票法_资格预审_前_v1
	// "xms_fwjz_sj_jmtpf_v1_1",
	// // 厦门市_房屋建筑_设计13版_记名投票法_资格预审_后_v1
	// "xms_fwjz_sj_jmtpf_v1_2",
	//
	// // 厦门市_房屋建筑_设计_排序法_资格后审_v1
	// "xms_fwjz_sj_pxf_v1",
	// // 厦门市_房屋建筑_设计_排序法_资格预审_前_v1
	// "xms_fwjz_sj_pxf_v1_1",
	// // 厦门市_房屋建筑_设计_排序法_资格预审_后_v1
	// "xms_fwjz_sj_pxf_v1_2",
	//
	// // 厦门市_市政工程_监理_综合评估法_v1
	// "xms_szgc_jl_zhpgf_v1",
	// // 厦门市_市政工程_监理_经评审的选取法_v1
	// "xms_szgc_jl_jpsdxqf_v1",
	//
	// // 厦门市_市政工程_勘察_简易评估法_资格后审_v1
	// "xms_szgc_kc_jypgf_zghs_v1",
	// // 厦门市_市政工程_勘察_简易评估法_资格预审_前_v1
	// "xms_szgc_kc_jypgf_zghs_v1_1",
	// // 厦门市_市政工程_勘察_简易评估法_资格预审_后_v1
	// "xms_szgc_kc_jypgf_zghs_v1_2",
	//
	// // 厦门市_市政工程_勘察_综合评估法-资格后审_v1
	// "xms_szgc_kc_zhpgf_zghs_v1",
	// // 厦门市_市政工程_勘察_综合评估法-资格预审_v1
	// "xms_szgc_kc_zhpgf_zghs_v1_1",
	// // 厦门市_市政工程_勘察_综合评估法-资格预审_v1
	// "xms_szgc_kc_zhpgf_zghs_v1_2",
	//
	// // 厦门市_市政工程_设计15版_记名投票法_v1
	// "xms_szgc_sj_jmtpf_v1",
	// // 厦门市_市政工程_设计15版_记名投票法_资格预审_前_v1
	// "xms_szgc_sj_jmtpf_v1_1",
	// // 厦门市_市政工程_设计15版_记名投票法_资格预审_后_v1
	// "xms_szgc_sj_jmtpf_v1_2",
	//
	// "fjs_gsgl_sj_jmtpf_v1_2", "xms_fjsz_sg_jmtpf_v1_2",
	// "xms_fwjz_sg_jpsdzdtbjzbf_A_v1"
	//
	// };
	//
	// Pattern p = Pattern.compile("^xms\\_\\w{4}\\_(jl|kc|sj)\\_.*");
	//
	// for (String code : codes)
	// {
	// System.err.println(p.matcher(code).matches());
	// }
	// }

	/**
	 * 判断当前会话是否为移动端会话<br/>
	 * <p>
	 * </p>
	 * 
	 * @return boolean true/false
	 */
	public static boolean isMobileClient()
	{
		String clientType = getAttribute("EOKB_CLIENT_TYPE");
		return StringUtils.equals("EOKB-MOBILE", clientType);
	}
}
