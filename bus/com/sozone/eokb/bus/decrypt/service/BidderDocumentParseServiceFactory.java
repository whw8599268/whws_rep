/**
 * 包名：com.sozone.eokb.bus.service
 * 文件名：BidderDocumentParseServiceFactory.java<br/>
 * 创建时间：2017-9-4 下午2:34:37<br/>
 * 创建者：sozone<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.decrypt.service.impl.CommonHighwayNoPriceParseServiceImpl;
import com.sozone.eokb.bus.decrypt.service.impl.CommonHighwayParseServiceImpl;
import com.sozone.eokb.bus.decrypt.service.impl.ExpresswayParseServiceImpl;
import com.sozone.eokb.bus.decrypt.service.impl.GSGLJJGParseServiceImpl;
import com.sozone.eokb.bus.decrypt.service.impl.HousingConstructionParseServiceImpl;
import com.sozone.eokb.bus.decrypt.service.impl.NoCreditScoreHCParseServiceImpl;
import com.sozone.eokb.bus.decrypt.service.impl.NormalExpresswayParseServiceImpl;
import com.sozone.eokb.bus.decrypt.service.impl.PortShippingParseServiceImpl;
import com.sozone.eokb.bus.decrypt.service.impl.XMGCJLParseServiceImpl;
import com.sozone.eokb.bus.decrypt.service.impl.XMPretrialParseServiceImpl;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 投标文件解析服务工厂类<br/>
 * <p>
 * </p>
 * Time：2017-9-4 下午2:34:37<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
public class BidderDocumentParseServiceFactory
{

	/**
	 * 评标办法code与服务实例图
	 */
	private static Map<String, BidderDocumentParseService> instances = new HashMap<String, BidderDocumentParseService>();

	/**
	 * 获取投标文件解析服务实例<br/>
	 * <p>
	 * </p>
	 * 
	 * @return 实例
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static BidderDocumentParseService getBidderDocumentParseServiceInstance()
			throws ServiceException
	{
		// 根据招标文件中的评标办法编码获取具体的解析类
		String bemCode = SessionUtils.getTenderProjectTypeCode();
		if (StringUtils.isEmpty(bemCode))
		{
			throw new ServiceException("", "无法获取评标办法编码");
		}
		String tpType = SessionUtils.getTenderProjectType();
		// 先从缓存里面拿
		BidderDocumentParseService instance = instances.get(bemCode);
		if (null != instance)
		{
			return instance;
		}
		Class<? extends BidderDocumentParseService> clazz = null;
		// 如果是高速公路
		if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_GSGL_TYPE, tpType))
		{
			// 勘察设计、勘察监理|设计审查 、交竣工
			if (StringUtils.equals("fjs_gsgl_kcsj_zhpgf1_v1", bemCode)
					|| StringUtils.equals("fjs_gsgl_kcjl_sjsc_zhpgf1_v1",
							bemCode)
					|| StringUtils.equals("fjs_gsgl_jgjg_zhpff_v1", bemCode))
			{
				clazz = NormalExpresswayParseServiceImpl.class;
			}
			// 福建省_高速公路_交工竣工_验收前质量检测_综合评分法
			else if (StringUtils.equals("fjs_gsgl_jgjg_ysqzljc_zhpff_v1",
					bemCode)
					|| StringUtils.equals("fjs_gsgl_jgjg_ysqzljc_zhpff_v2",
							bemCode))
			{
				clazz = GSGLJJGParseServiceImpl.class;
			}
			else
			{
				clazz = ExpresswayParseServiceImpl.class;
			}
		}
		// 普通公路
		else if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_PTGL_TYPE, tpType))
		{
			// 福建省普通公路工程勘察设计-综合评估法Ⅱ
			if (StringUtils.equals("fjs_ptgl_kcsj_zhpgf2_v1", bemCode))
			{
				clazz = CommonHighwayNoPriceParseServiceImpl.class;
			}
			else
			{
				clazz = CommonHighwayParseServiceImpl.class;
			}
		}
		// 港航水运
		else if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_SYGC_TYPE, tpType))
		{
			clazz = PortShippingParseServiceImpl.class;
		}

		// 如果是厦门房建市政
		else if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE, tpType)
				|| StringUtils.equals(
						ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE,
						tpType))
		{
			clazz = getXMFJSZParseServiceClass();
		}

		try
		{
			instance = clazz.newInstance();
			instances.put(bemCode, instance);
			return instance;
		}
		catch (InstantiationException e)
		{
			throw new ServiceException("", "初始化投标文件解析服务实例发生异常", e);
		}
		catch (IllegalAccessException e)
		{
			throw new ServiceException("", "初始化投标文件解析服务实例发生异常", e);
		}

	}

	/**
	 * 获取厦门房建市政解析器<br/>
	 * <p>
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private static Class<? extends BidderDocumentParseService> getXMFJSZParseServiceClass()
			throws ServiceException
	{
		// 根据招标文件中的评标办法编码获取具体的解析类
		String bemCode = SessionUtils.getTenderProjectTypeCode();

		// 如果是施工的最低投标价中标法A类，综合评估法A类。
		if (StringUtils.equals("xms_fwjz_sg_jpsdzdtbjzbf_A_v1", bemCode)
				|| StringUtils.equals("xms_fwjz_sg_zhpgf_A_v1", bemCode)
				|| StringUtils.equals("xms_szgc_sg_jpsdzdtbjzbf_A_v1", bemCode)
				|| StringUtils.equals("xms_szgc_sg_zhpgf_A_v1", bemCode)
				|| StringUtils.equals("xms_fwjz_sg_jpsdzdtbjzbf_A_v2", bemCode)
				|| StringUtils.equals("xms_fwjz_sg_zhpgf_A_v2", bemCode)
				|| StringUtils.equals("xms_szgc_sg_jpsdzdtbjzbf_A_v2", bemCode)
				|| StringUtils.equals("xms_szgc_sg_zhpgf_A_v2", bemCode))
		{
			// 有信用分的情况
			return HousingConstructionParseServiceImpl.class;
		}
		// 监理新范本
		if (StringUtils.equals("xms_fwjz_jl_jypbf_v2", bemCode)
				|| StringUtils.equals("xms_fwjz_jl_zhpgf_v2", bemCode)
				|| StringUtils.equals("xms_szgc_jl_jypbf_v2", bemCode)
				|| StringUtils.equals("xms_szgc_jl_zhpgf_v2", bemCode))
		{
			// 这个信用分比较特殊
			return XMGCJLParseServiceImpl.class;
		}
		// --------------没有信用分的情况---------
		Record<String, Object> tpinfo = SessionUtils.getTenderProjectInfo();
		if (CollectionUtils.isEmpty(tpinfo))
		{
			throw new ServiceException("", "初始化投标文件解析服务实例发生异常,无法获取招标项目信息!");
		}
		// 如果有扩展信息
		String json = tpinfo.getString("V_JSON_OBJ");
		if (StringUtils.isEmpty(json))
		{
			return NoCreditScoreHCParseServiceImpl.class;
		}
		JSONObject ext = JSON.parseObject(json);
		// 如果是预审项目且是预审
		if (ext.getBooleanValue("IS_PRETRIAL")
				&& 1 == ext.getIntValue("N_BID_ORDER"))
		{
			return XMPretrialParseServiceImpl.class;
		}
		return NoCreditScoreHCParseServiceImpl.class;
	}

}
