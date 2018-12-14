/**
 * 包名：com.sozone.eokb.bus.decrypt.xm.service.impl
 * 文件名：FJSZHasCreditScoreImpl.java<br/>
 * 创建时间：2018-5-16 上午10:09:35<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt.xm.service.impl;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.decrypt.service.BidderDocumentServiceAdapter;
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.common.ConstantEOKB.TENDERPROJECT_APP_TYPE;
import com.sozone.eokb.utils.SessionUtils;
import com.ws.client.CreditScoreUtils;

/**
 * 厦门房建市政有信用分的投标文件解析器<br/>
 * <p>
 * 施工的最低投标价中标法A类，综合评估法A类。<br/>
 * </p>
 * Time：2018-5-16 上午10:09:35<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class FJSZHasCreditScoreImpl extends BidderDocumentServiceAdapter
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sozone.eokb.bus.decrypt.service.BidderDocumentServiceAdapter#
	 * buildBidderInfo(com.sozone.aeolus.dao.data.Record, boolean)
	 */
	@Override
	protected Record<String, Object> buildBidderInfo(
			Record<String, Object> bidderDelivery, boolean group)
			throws ServiceException
	{
		// 唱标要素
		Record<String, Object> sings = bidderDelivery.getColumn("SingObj");
		String userID = ApacheShiroUtils.getCurrentUserID();
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("ID", Random.generateUUID());
		record.setColumn("V_TPID", SessionUtils.getTPID());
		record.setColumn("V_BID_SECTION_ID",
				bidderDelivery.getString("V_BID_SECTION_ID"));
		record.setColumn("V_BID_SECTION_CODE",
				bidderDelivery.getString("V_BID_SECTION_CODE"));
		record.setColumn("V_BID_SECTION_GROUP_CODE",
				bidderDelivery.getString("V_BID_SECTION_GROUP_CODE"));
		// 投标人组织机构代码
		record.setColumn("V_BIDDER_ORG_CODE",
				bidderDelivery.getString("V_ORG_CODE"));
		// 投标人名称
		String bidderName = BidderElementParseUtils.getSingObjAttribute(
				"tbrmc", sings);
		// 去前导与尾部空格
		bidderName = StringUtils.trim(bidderName);
		// 不能从投递表中去取，为了防止出现联合体投标情况
		record.setColumn("V_BIDDER_NAME", bidderName);
		// 作废临时状态标识(1正常,0作废)
		record.setColumn("N_VOIDTAGTMP", "1");
		record.setColumn("N_ENVELOPE_0", "1");

		// 投标报价
		String tbbj = BidderElementParseUtils
				.getSingObjAttribute("tbbj", sings);
		if (StringUtils.isEmpty(tbbj))
		{
			tbbj = "0";
			// throw new ServiceException("", "无法获取到投标文件中的投标报价!");
		}
		// 投标报价
		double price = Double.parseDouble(tbbj);
		record.setColumn("N_PRICE", price);
		// 投标人编号
		record.setColumn("V_BIDDER_NO",
				bidderDelivery.getString("V_DELIVER_NO"));
		// 递交文件顺序号
		record.setColumn("N_SORT_FILE_BID",
				getSortFileBid(bidderDelivery.getString("V_DELIVER_NO")));
		// 获取项目类型
		String type = SessionUtils.getTenderProjectType();
		if (StringUtils.equals(TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE, type))
		{
			type = "房建";
		}
		else if (StringUtils.equals(TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE, type))
		{
			type = "市政";
		}
		// 用统一社会信用代码或者组织机构号码匹配 wengdm 2018-11-20
		// 获取投标人信用信息
		Record<String, Object> bcs = getBidderCreditScore(
				bidderDelivery.getString("V_ORG_CODE"),
				bidderDelivery.getString("V_UNIFY_CODE"), type);
		if (CollectionUtils.isEmpty(bcs))
		{
			throw new ServiceException("", "无法获取到投标人信用分!");
		}
		sings.setColumn("cerditScore", bcs);
		// 还要将总分设置到指定位置
		JSONArray objsing = sings.getColumn("objSing");
		if (null != objsing)
		{
			JSONObject jobj = new JSONObject();
			jobj.put("tbRatingsInEvlScore", bcs.getFloat("N_TOTAL_SCORE"));
			jobj.put("施工企业信用分", bcs.getFloat("N_TOTAL_SCORE"));
			objsing.add(jobj);
			sings.setColumn("objSing", objsing);
		}
		// 信用分
		record.setColumn("N_CREDITSCORE", bcs.getFloat("N_TOTAL_SCORE"));

		// JSON信息
		record.setColumn("V_JSON_OBJ", JSON.toJSONString(sings));
		record.setColumn("V_CREATE_USER", userID);
		record.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		record.setColumn("V_UPDATE_USER", userID);
		record.setColumn("N_UPDATE_TIME", System.currentTimeMillis());
		return record;
	}

	/**
	 * 获取投标人信用分<br/>
	 * <p>
	 * </p>
	 * 
	 * @param orgCode
	 *            组织代码
	 * @param unifyCode
	 *            统一社会信用代码
	 * @param type
	 *            类型
	 * @return 信用分
	 * @throws ServiceException
	 *             ServiceException
	 */
	protected Record<String, Object> getBidderCreditScore(String orgCode,
			String unifyCode, String type) throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("type", type).setColumn("orgcode", orgCode)
				.setColumn("unifycode", unifyCode);
		param.putAll(CreditScoreUtils.getYearAndQuarter());
		return getActiveRecordDAO().statement().selectOne(
				"3DecryptV3.getBidderCreditScore", param);
	}
}
