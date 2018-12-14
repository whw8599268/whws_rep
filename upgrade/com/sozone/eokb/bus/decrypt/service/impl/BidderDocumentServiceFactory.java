/**
 * 包名：com.sozone.eokb.bus.decrypt.service
 * 文件名：BidderDocumentServiceFactory.java<br/>
 * 创建时间：2018-4-11 下午4:46:11<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.aop.Duang;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.decrypt.service.BidderDocumentService;
import com.sozone.eokb.bus.decrypt.service.BidderDocumentServiceAdapter;
import com.sozone.eokb.bus.decrypt.xm.service.impl.FJSZHasCreditScoreImpl;
import com.sozone.eokb.bus.decrypt.xm.service.impl.FJSZNoneBidPriceServiceImpl;
import com.sozone.eokb.bus.decrypt.xm.service.impl.FJSZNoneCreditScoreServiceImpl;
import com.sozone.eokb.bus.decrypt.xm.service.impl.GCJLCreditScoreImpl;
import com.sozone.eokb.common.ConstantEOKB.TENDERPROJECT_APP_TYPE;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 投标文件服务工厂类<br/>
 * <p>
 * </p>
 * Time：2018-4-11 下午4:46:11<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class BidderDocumentServiceFactory
{

	/**
	 * 评标办法code与服务实例图
	 */
	private static Map<String, BidderDocumentService> instances = new HashMap<String, BidderDocumentService>();

	/**
	 * 获取评标办法对应的投标文件解析服务实例<br/>
	 * <p>
	 * </p>
	 * 
	 * @param bemCode
	 *            招标项目评标办法编码
	 * @param tpType
	 *            招标项目类型
	 * @return 投标文件解析服务实例
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static BidderDocumentService getServiceInstance(String bemCode,
			String tpType) throws ServiceException
	{
		if (StringUtils.isEmpty(bemCode))
		{
			throw new ServiceException("", "无效的评标办法编码!");
		}
		// 先从缓存里面拿
		BidderDocumentService instance = instances.get(bemCode);
		if (null != instance)
		{
			return instance;
		}
		Class<? extends BidderDocumentService> clazz = getBidderDocumentServiceImplClass(
				bemCode, tpType);
		// 获取实例
		instance = Duang.duang(clazz);
		instances.put(bemCode, instance);
		return instance;
	}

	/**
	 * 获取解析器实现类<br/>
	 * <p>
	 * </p>
	 * 
	 * @param bemCode
	 * @param tpType
	 * @return
	 * @throws ServiceException
	 *             ServiceException
	 */
	private static Class<? extends BidderDocumentService> getBidderDocumentServiceImplClass(
			String bemCode, String tpType) throws ServiceException
	{
		// 如果是高速公路
		if (StringUtils.equals(TENDERPROJECT_APP_TYPE.FJS_GSGL_TYPE, tpType))
		{
			// 勘察设计、勘察监理|设计审查 、交竣工
			if (StringUtils.equals("fjs_gsgl_kcsj_zhpgf1_v1", bemCode)
					|| StringUtils.equals("fjs_gsgl_kcjl_sjsc_zhpgf1_v1",
							bemCode)
					|| StringUtils.equals("fjs_gsgl_jgjg_zhpff_v1", bemCode)
					|| StringUtils.equals("fjs_gsgl_jgjg_ysqzljc_zhpff_v1",
							bemCode)
					|| StringUtils.equals("fjs_gsgl_jgjg_ysqzljc_zhpff_v2",
							bemCode)
					|| StringUtils.equals("fjs_gsgl_kcjl_sjsc_zhpff_v2",
							bemCode)
					|| StringUtils.equals("fjs_gsgl_kcsj_zhpgf1_v2", bemCode))
			{
				// 不含信用分的情况
				return GSNoneCreditScoreServiceImpl.class;
			}
			return BidderDocumentServiceAdapter.class;
		}
		// 普通公路
		if (StringUtils.equals(TENDERPROJECT_APP_TYPE.FJS_PTGL_TYPE, tpType))
		{
			// 福建省普通公路工程勘察设计-综合评估法Ⅱ
			if (StringUtils.equals("fjs_ptgl_kcsj_zhpgf2_v1", bemCode)
					|| StringUtils.equals("fjs_ptgl_kcsj_zhpgf2_v2", bemCode))
			{
				// 没有投标报价的情况
				return PTGLNoneBidPriceServiceImpl.class;
			}
			// 有投标报价
			return PTGLServiceImpl.class;
		}
		// 港航水运
		if (StringUtils.equals(TENDERPROJECT_APP_TYPE.FJS_SYGC_TYPE, tpType))
		{
			return SYGCServiceImpl.class;
		}

		// 如果是厦门房建市政
		if (StringUtils.equals(TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE, tpType)
				|| StringUtils.equals(TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE,
						tpType))
		{
			return getXMServiceClass(bemCode, tpType);
		}
		throw new ServiceException("", "不支持的项目类型及评标办法编码!");
	}

	/**
	 * 获取厦门房建市政解析器<br/>
	 * <p>
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private static Class<? extends BidderDocumentService> getXMServiceClass(
			String bemCode, String tpType) throws ServiceException
	{
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
			return FJSZHasCreditScoreImpl.class;
		}
		// 监理新范本
		if (StringUtils.equals("xms_fwjz_jl_jypbf_v2", bemCode)
				|| StringUtils.equals("xms_fwjz_jl_zhpgf_v2", bemCode)
				|| StringUtils.equals("xms_szgc_jl_jypbf_v2", bemCode)
				|| StringUtils.equals("xms_szgc_jl_zhpgf_v2", bemCode))
		{
			// 这个信用分比较特殊
			return GCJLCreditScoreImpl.class;
		}
		// --------------没有信用分的情况---------
		Record<String, Object> tpinfo = SessionUtils.getTenderProjectInfo();
		if (CollectionUtils.isEmpty(tpinfo))
		{
			throw new ServiceException("", "初始化投标文件解析服务实例发生异常,无法获取招标项目信息!");
		}
		// 如果有扩展信息
		String json = tpinfo.getString("V_JSON_OBJ");
		if (StringUtils.isNotEmpty(json))
		{
			JSONObject ext = JSON.parseObject(json);
			// 如果是预审项目且是预审
			if (ext.getBooleanValue("IS_PRETRIAL")
					&& 1 == ext.getIntValue("N_BID_ORDER"))
			{
				return FJSZNoneBidPriceServiceImpl.class;
			}
		}
		return FJSZNoneCreditScoreServiceImpl.class;
	}

}
