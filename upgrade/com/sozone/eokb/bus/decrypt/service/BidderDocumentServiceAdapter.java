/**
 * 包名：com.sozone.eokb.bus.decrypt.service
 * 文件名：BidderDocumentServiceAdapter.java<br/>
 * 创建时间：2018-5-15 下午4:19:24<br/>
 * 创建者：zouye<br/>
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.Auto.Table;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.decrypt.daemon.DaemonHandler;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 投标文件解析适配器<br/>
 * <p>
 * 投标文件解析适配器<br/>
 * </p>
 * Time：2018-5-15 下午4:19:24<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class BidderDocumentServiceAdapter implements BidderDocumentService
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(BidderDocumentServiceAdapter.class);

	private static final Map<String, String> ARABIC_CN_NUMS = new HashMap<String, String>();

	static
	{
		ARABIC_CN_NUMS.put("0", "零");
		ARABIC_CN_NUMS.put("1", "一");
		ARABIC_CN_NUMS.put("2", "二");
		ARABIC_CN_NUMS.put("3", "三");
		ARABIC_CN_NUMS.put("4", "四");
		ARABIC_CN_NUMS.put("5", "五");
		ARABIC_CN_NUMS.put("6", "六");
		ARABIC_CN_NUMS.put("7", "七");
		ARABIC_CN_NUMS.put("8", "八");
		ARABIC_CN_NUMS.put("9", "九");
		ARABIC_CN_NUMS.put("10", "十");
	}

	/**
	 * activeRecordDAO属性的get方法
	 * 
	 * @return the activeRecordDAO
	 */
	protected static ActiveRecordDAO getActiveRecordDAO()
	{
		return ActiveRecordDAOImpl.getInstance();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sozone.eokb.bus.decrypt.service.BidderDocumentService#
	 * getDeliveryDocuments(java.lang.String, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public List<Record<String, Object>> getDeliveryDocuments(String tpid,
			String unitfy, String orgCode) throws ServiceException
	{
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", tpid);
		params.setColumn("unify_code", unitfy);
		params.setColumn("org_code", orgCode);
		return getActiveRecordDAO().statement().selectList(
				"DecryptV3.getBidderDeliverys", params);
	}

	/**
	 * 获取投标人临时解密信息数据列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param unitfy
	 *            统一社会信用代码
	 * @param orgCode
	 *            组织机构代码
	 * @return 临时解密信息数据列表
	 * @throws ServiceException
	 *             服务异常
	 */
	protected List<Record<String, Object>> getDecryptTempDatas(String tpid,
			String unitfy, String orgCode) throws ServiceException
	{
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", tpid);
		params.setColumn("unify_code", unitfy);
		params.setColumn("org_code", orgCode);
		return getActiveRecordDAO().statement().selectList(
				"DecryptV3.getDecryptTempDatas", params);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sozone.eokb.bus.decrypt.service.BidderDocumentService#
	 * parseDecryptTempData(java.lang.String, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void parseDecryptTempData(String tpid, String unitfy, String orgCode)
			throws ServiceException
	{
		logger.info(LogUtils.format("开始解析解密临时数据", tpid, unitfy, orgCode));
		// // 评标办法编码
		// String bemCode = SessionUtils.getTenderProjectTypeCode();
		// 是否使用标段组
		boolean group = SessionUtils.isSectionGroup();
		// 临时数据列表
		List<Record<String, Object>> tempDatas = getDecryptTempDatas(tpid,
				unitfy, orgCode);
		logger.info(LogUtils.format("查询到的临时数据", tempDatas));
		if (CollectionUtils.isEmpty(tempDatas))
		{
			return;
		}
		// 信封索引
		int index = 0;
		// 标段与投递信息对应图,标段编号---投递信息
		Map<String, Record<String, Object>> sectionDelivers = new HashMap<String, Record<String, Object>>();
		// 标段编号
		String sectionCode = null;
		// 投递信息临时对象
		Record<String, Object> temp = null;
		// 文件描述JSON对象
		JSONObject fileDescJson = null;
		// 唱标信息列表
		JSONArray array = null;
		// 投标文件制作的机器信息
		JSONObject machineInfo = null;
		// 投标文件制作的机器MAC地址列表
		JSONArray macs = null;
		// mac地址
		String mac = null;
		// 迭代投递信息,构造投标人信息
		for (Record<String, Object> bidderDelivery : tempDatas)
		{
			// 文件描述信息
			fileDescJson = bidderDelivery.getJSONObject("V_FILE_INFO_JSON");
			// 标段编号
			sectionCode = bidderDelivery.getString("V_BID_SECTION_CODE");
			// 获取同标段的投递信息
			temp = sectionDelivers.get(sectionCode);
			if (null == temp)
			{
				temp = bidderDelivery;
				sectionDelivers.put(sectionCode, temp);
			}
			// 获取唱标要素json数组
			array = bidderDelivery.getJSONArray("V_ELEMENT_JSON");
			logger.info(LogUtils.format("当前投标信息摘要以及对应的ID", array,
					bidderDelivery.getString("ID")));
			// 获取信封索引
			index = bidderDelivery.getInteger("N_USE_CASE");

			// 投标文件制作机器信息
			machineInfo = fileDescJson.getJSONObject("MACHINE");
			macs = null;
			if (null != machineInfo)
			{
				// mac地址
				macs = machineInfo.getJSONArray("MACS");
			}
			mac = getMacs(macs);
			// mac地址信息
			Record<String, Object> macInfo = new RecordImpl<String, Object>();
			macInfo.setColumn("ENVELOPE_INDEX", index);
			macInfo.setColumn("MAC", mac);

			// 获取唱标对象
			Record<String, Object> sobj = temp.getColumn("SingObj");
			// mac地址列表
			List<Record<String, Object>> macInfos = null;
			// 如果不存在
			if (null == sobj)
			{
				sobj = new RecordImpl<String, Object>();
				sobj.setColumn("objSing", array);
				temp.setColumn("SingObj", sobj);
				macInfos = new LinkedList<Record<String, Object>>();
				macInfos.add(macInfo);
				sobj.setColumn("macs", macInfos);
			}
			else
			{
				// 如果存在合并
				macInfos = sobj.getColumn("macs");
				macInfos.add(macInfo);
				sobj.setColumn("macs", macInfos);
				// 上一个信封
				JSONArray previous = sobj.getColumn("objSing");
				previous.addAll(array);
				sobj.setColumn("objSing", previous);
				temp.setColumn("SingObj", sobj);
			}
		}
		// 开始迭代
		Record<String, Object> record = null;
		// 参数
		Record<String, Object> params = new RecordImpl<String, Object>();
		for (String scode : sectionDelivers.keySet())
		{
			// 情况
			params.clear();
			// 投递信息
			temp = sectionDelivers.get(scode);
			// 非标段组的话插入解密信息表 标段的话直接插入投标人信息表
			if (!group)
			{
				// 构建非标段租的对象
				record = buildBidderInfo(temp, group);
				params.setColumn("sectionid",
						record.getString("V_BID_SECTION_ID"));
				params.setColumn("boc", record.getString("V_BIDDER_ORG_CODE"));
				// 先删除再插入
				getActiveRecordDAO()
						.auto()
						.table(TableName.EKB_T_TENDER_LIST)
						.setCondition("AND",
								"V_BID_SECTION_ID = #{sectionid} AND V_BIDDER_ORG_CODE = #{boc}")
						.remove(params);
				getActiveRecordDAO().auto().table(TableName.EKB_T_TENDER_LIST)
						.save(record);
				continue;
			}

			// 标段组情况直接插入解密表
			record = buildBidderInfo(temp, group);
			// 先删除再插入
			getActiveRecordDAO()
					.auto()
					.table(TableName.EKB_T_DECRYPT_INFO)
					.setCondition("AND",
							"V_BID_SECTION_ID = #{sectionid} AND V_BIDDER_ORG_CODE = #{boc}")
					.remove(params);
			getActiveRecordDAO().auto().table(TableName.EKB_T_DECRYPT_INFO)
					.save(record);
		}
		// 加入解密队列等待解密
		params.clear();
		params.setColumn("tpid", tpid);
		params.setColumn("unify_code", unitfy);
		params.setColumn("org_code", orgCode);
		DaemonHandler.addDataToParseTempQueue(params);
	}

	/**
	 * 构建投标人信息对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param bidderDelivery
	 *            投递信息对象
	 * @param group
	 *            是否为标段组
	 * @return 投标人信息
	 * @throws ServiceException
	 *             服务异常
	 */
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
		// 投标人信用等级
		String tbRatingsInEvl = BidderElementParseUtils.getSingObjAttribute(
				"tbRatingsInEvl", sings);
		// 投标人编号
		String bidNo = generateBidderNo(tbRatingsInEvl,
				bidderDelivery.getString("V_DELIVER_NO"));
		record.setColumn("V_BIDDER_NO", bidNo);
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

	/**
	 * 获取MAC地址<br/>
	 * <p>
	 * </p>
	 * 
	 * @param macs
	 *            MAC地址列表
	 * @return MAC地址字符串
	 */
	protected String getMacs(JSONArray macs)
	{
		if (null == macs || macs.isEmpty())
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < macs.size(); i++)
		{
			if (i == 0)
			{
				sb.append(macs.getString(i));
				continue;
			}
			sb.append(";").append(macs.getString(i));
		}
		return sb.toString();
	}

	/**
	 * 获取关联企业编号
	 * 
	 * @param unitfy
	 *            统一社会信用代码
	 * @param orgcode
	 *            组织机构代码
	 * @param tpid
	 *            招标项目ID
	 * @return 关联企业编号
	 * @throws ServiceException
	 *             服务异常
	 */
	protected String getCorrelate(String unitfy, String orgcode, String tpid)
			throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		Table table = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_CORRELATE_ENTER)
				.setCondition("AND", "V_TPID = #{tpid}");
		if (StringUtils.isNotEmpty(unitfy))
		{
			if (StringUtils.isNotEmpty(orgcode))
			{
				table.setCondition("AND",
						"(V_UNIFY_CODE = #{unitfy} OR V_ORG_CODE = #{orgcode})");
				param.setColumn("unitfy", unitfy);
				param.setColumn("orgcode", orgcode);
			}
			else
			{
				table.setCondition("AND", "V_UNIFY_CODE = #{unitfy}");
				param.setColumn("unitfy", unitfy);
			}
		}
		else
		{
			if (StringUtils.isNotEmpty(orgcode))
			{
				table.setCondition("AND", "V_ORG_CODE = #{orgcode}");
				param.setColumn("orgcode", orgcode);
			}
			else
			{
				throw new ServiceException("", "组织机构代码或统一社会代码不能为空!");
			}
		}
		Record<String, Object> record = table.get(param);
		if (CollectionUtils.isEmpty(record))
		{
			return "";
		}
		return StringUtils.trimToEmpty(record.getString("V_CORRELATE_CODE"));
	}

	/**
	 * 
	 * 获取递交文件顺序号<br/>
	 * <p>
	 * 获取递交文件顺序号
	 * </p>
	 * 
	 * @param deliverNo
	 *            投递顺序号(ekb_t_tbimportbidding)
	 * @return 顺序号
	 */
	protected int getSortFileBid(String deliverNo)
	{
		int sortFileBid = 0;
		try
		{
			sortFileBid = Integer.valueOf(deliverNo);
		}
		catch (Exception e)
		{
			sortFileBid = 0;
		}
		return sortFileBid;
	}

	/**
	 * 
	 * 投标人编号生成<br/>
	 * <p>
	 * 投标人编号生成
	 * </p>
	 * 
	 * @param creditRatingCode
	 *            信用等级 AA-1 A-2 B-3 C-4 D-5
	 * @param deliverNo
	 *            投递编号
	 * @return 投标人编号
	 * @throws ServiceException
	 *             服务异常
	 */
	protected String generateBidderNo(String creditRatingCode, String deliverNo)
			throws ServiceException
	{
		if (StringUtils.isEmpty(creditRatingCode)
				|| StringUtils.isEmpty(deliverNo))
		{
			throw new ServiceException("", "投标人编号生成错误！");
		}
		if (StringUtils.equals("AA", creditRatingCode))
		{
			return "1-" + deliverNo;
		}
		else if (StringUtils.equals("A", creditRatingCode))
		{
			return "2-" + deliverNo;
		}
		else if (StringUtils.equals("B", creditRatingCode))
		{
			return "3-" + deliverNo;
		}
		else if (StringUtils.equals("C", creditRatingCode))
		{
			return "4-" + deliverNo;
		}
		else if (StringUtils.equals("D", creditRatingCode))
		{
			return "5-" + deliverNo;
		}
		else
		{
			throw new ServiceException("", "投标人编号生成错误！");
		}
	}
}
