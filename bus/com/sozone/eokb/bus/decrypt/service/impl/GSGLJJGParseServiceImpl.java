/**
 * 包名：com.sozone.eokb.bus.decrypt.service.impl
 * 文件名：GSGLJJGParseServiceImpl.java<br/>
 * 创建时间：2018-6-1 下午5:53:23<br/>
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

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 高速公路交竣工投标文件解析器<br/>
 * <p>
 * 高速公路交竣工投标文件解析器<br/>
 * </p>
 * Time：2018-6-1 下午5:53:23<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
public class GSGLJJGParseServiceImpl extends ExpresswayParseServiceImpl
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sozone.eokb.bus.decrypt.service.BidderDocumentParseServiceAdapter
	 * #buildBidderInfo(com.sozone.aeolus.dao.data.Record, boolean)
	 */
	@Override
	protected Record<String, Object> buildBidderInfo(
			Record<String, Object> bidderDelivery, boolean group)
			throws ServiceException
	{
		// 唱标要素
		Record<String, Object> sings = bidderDelivery.getColumn("SingObj");
		String userID = ApacheShiroUtils.getCurrentUserID();
		String tpid = SessionUtils.getTPID();
		String unitfy = SessionUtils.getSocialcreditNO();
		String orgcode = SessionUtils.getCompanyCode();
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
		// record.setColumn("V_BIDDER_NAME",
		// bidderDelivery.getString("V_BIDDER_NAME"));
		record.setColumn("V_BIDDER_NAME", bidderName);
		// 作废临时状态标识(1正常,0作废)
		record.setColumn("N_VOIDTAGTMP", "1");
		record.setColumn("N_ENVELOPE_0", "1");

		// 投标报价
		String tbbj = BidderElementParseUtils
				.getSingObjAttribute("tbbj", sings);
		if (StringUtils.isEmpty(tbbj))
		{
			throw new ServiceException("", "无法获取到投标文件中的投标报价!");
		}
		// 投标报价
		double price = Double.parseDouble(tbbj);
		record.setColumn("N_PRICE", price);
		// // 投标人信用等级
		// String tbRatingsInEvl = BidderElementParseUtils.getSingObjAttribute(
		// "tbRatingsInEvl", sings);
		// // 投标人编号
		// String bidNo = generateBidderNo(tbRatingsInEvl,
		// bidderDelivery.getString("V_DELIVER_NO"));
		record.setColumn("V_BIDDER_NO",
				bidderDelivery.getString("V_DELIVER_NO"));
		// 递交文件顺序号
		record.setColumn("N_SORT_FILE_BID",
				getSortFileBid(bidderDelivery.getString("V_DELIVER_NO")));
		// 关联企业编号
		record.setColumn("V_CORRELATE_CODE",
				getCorrelate(unitfy, orgcode, tpid));

		// JSON信息
		record.setColumn("V_JSON_OBJ", JSON.toJSONString(sings));
		record.setColumn("V_CREATE_USER", userID);
		record.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		record.setColumn("V_UPDATE_USER", userID);
		record.setColumn("N_UPDATE_TIME", System.currentTimeMillis());
		return record;
	}

}
