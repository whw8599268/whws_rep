/**
 * 包名：com.sozone.eokb.fjs_ptgl.common
 * 文件名：PtglUtils.java<br/>
 * 创建时间：2017-12-28 上午9:35:59<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_ptgl.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.orm.DataEntry;
import com.sozone.aeolus.ext.orm.impl.DataEntryImpl;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_ptgl_gcsg_hldjf_v1.BidBenchmarkPriceAlgorithmUtils;
import com.sozone.eokb.utils.Arith;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 普通公路工具类<br/>
 * <p>
 * 普通公路工具类<br/>
 * </p>
 * Time：2017-12-28 上午9:35:59<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public final class PtglUtils
{

	/**
	 * 数据库xml
	 */
	private static String mybatisName = ConstantEOKB.EOKBBemCode.FJS_PTGL_COMMON;

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(PtglUtils.class);

	/**
	 * activeRecordDAO属性的get方法
	 * 
	 * @return the activeRecordDAO
	 */
	private static ActiveRecordDAO getActiveRecordDAO()
	{
		return ActiveRecordDAOImpl.getInstance();
	}

	/**
	 * 获取普通公路第一信封解密情况信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 第一信封解密情况信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<Record<String, Object>> getFirstEnvelopeDecryptSituation(
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取第一信封解密结果"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		// 非标段组
		if (!SessionUtils.isSectionGroup())
		{
			return getFirstEnvelopeSectionInfo(tpid);
		}
		// 标段组
		return getFirstEnvelopeGroupInfo(tpid);
	}

	/**
	 * 
	 * 获取普通公路第一信封无标段组的标段信息
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return
	 * @throws ServiceException
	 *             服务异常
	 */
	private static List<Record<String, Object>> getFirstEnvelopeSectionInfo(
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取普通公路第一信封无标段组的标段信息", tpid));

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS !='10-0'")
				.setCondition("AND", "V_BID_OPEN_STATUS !='10-1'")
				.addSortOrder("V_BID_SECTION_NAME", "ASC").list(param);

		//
		List<String> macList = null;
		logger.debug(LogUtils.format("获取标段列表", sections));
		// 迭代标段组
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));

			// 标段编号
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));
			// 查出投标人名单
			List<Record<String, Object>> tenderList = getActiveRecordDAO()
					.auto().table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.setCondition("AND", "N_ENVELOPE_0=1")
					.addSortOrder("N_SORT_FILE_BID", "ASC").list(param);

			// 获取所有该标段mac地址相同的企业的组织机构号
			macList = getActiveRecordDAO().statement().loadList(
					"Mac.getSameMacOrgCode", param);

			if (CollectionUtils.isEmpty(macList))
			{
				macList = new ArrayList<String>();
			}

			// 设置Mac地址是否重复标识
			setBidderMacSomeFlag(tenderList, macList);
			// 读取投标人的扩展信息
			analyzeBidderExtendedInfoForFirstEnvelope(tenderList);
			section.setColumn("TENDER_LIST", tenderList);
		}
		logger.debug(LogUtils.format("成功获取普通公路第一信封无标段组的标段信息", sections));
		return sections;
	}

	/**
	 * 
	 * 设置Mac地址是否重复标识<br/>
	 * <p>
	 * 设置Mac地址是否重复标识
	 * </p>
	 * 
	 * @param tenderList
	 *            投标人列表
	 * @param macList
	 *            重复MAC地址的企业的组织机构号
	 */
	private static void setBidderMacSomeFlag(
			List<Record<String, Object>> tenderList, List<String> macList)
	{
		logger.debug(LogUtils.format("设置Mac地址是否重复标识", tenderList, macList));
		for (Record<String, Object> tender : tenderList)
		{
			tender.setColumn("SAME_MAC",
					macList.contains(tender.getString("V_BIDDER_ORG_CODE")));
		}
	}

	/**
	 * 
	 * 获取第一信封解密成功家数<br/>
	 * <p>
	 * 获取第一信封解密成功家数
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 解密成功家数
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getDecryptSuccessCount(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取第一信封解密成功家数"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		Record<String, Object> result = new RecordImpl<String, Object>();
		if (SessionUtils.isSectionGroup())
		{
			String sql = "SELECT V_BIDDER_NAME FROM EKB_T_DECRYPT_INFO "
					+ " A WHERE A.V_TPID=#{tpid} GROUP BY V_BIDDER_NAME";
			List<Record<String, Object>> recordYX = getActiveRecordDAO()
					.sql(sql).setParam("tpid", tpid).list();
			sql = "SELECT V_ORG_CODE FROM EKB_T_TBIMPORTBIDDING A WHERE A.V_TPID=#{tpid} GROUP BY V_ORG_CODE";
			List<Record<String, Object>> recordAX = getActiveRecordDAO()
					.sql(sql).setParam("tpid", tpid).list();
			result.put("YX_N", recordYX.size());
			result.put("WX_N", recordAX.size() - recordYX.size());
			logger.debug(LogUtils.format("全部投递家数", recordAX.size()));
			logger.debug(LogUtils.format("解密成功家数", recordYX.size()));
		}
		else
		{
			// 查询成功家数
			String sql = "SELECT COUNT(A.V_BID_SECTION_ID) NUM,B.V_BID_SECTION_NAME,A.V_BID_SECTION_ID FROM EKB_T_TENDER_LIST A "
					+ "RIGHT JOIN EKB_T_SECTION_INFO B ON A.V_BID_SECTION_ID = B.V_BID_SECTION_ID AND B.V_BID_OPEN_STATUS NOT LIKE '10%' "
					+ "WHERE A.V_TPID=#{tpid}  GROUP BY A.V_BID_SECTION_ID ORDER BY B.V_BID_SECTION_NAME";
			List<Record<String, Object>> recordYX = getActiveRecordDAO()
					.sql(sql).setParam("tpid", tpid).list();

			// 查询全部家数
			sql = "SELECT COUNT(A.V_BID_SECTION_ID) NUM,A.V_BID_SECTION_ID FROM EKB_T_TBIMPORTBIDDING A  "
					+ "WHERE A.V_TPID=#{tpid} AND V_FTID='111' GROUP BY A.V_BID_SECTION_ID";
			List<Record<String, Object>> recordAX = getActiveRecordDAO()
					.sql(sql).setParam("tpid", tpid).list();

			logger.debug(LogUtils.format("全部投递家数", recordAX.size()));
			logger.debug(LogUtils.format("解密成功家数", recordYX.size()));

			StringBuffer jmMsg = new StringBuffer();
			for (Record<String, Object> record : recordYX)
			{
				for (Record<String, Object> re : recordAX)
				{
					if (StringUtils.equals(
							record.getString("V_BID_SECTION_ID"),
							re.getString("V_BID_SECTION_ID")))
					{
						jmMsg.append("【标段："
								+ record.getString("V_BID_SECTION_NAME")
								+ "，投标人数："
								+ record.getString("NUM")
								+ "家；失败："
								+ (re.getInteger("NUM") - record
										.getInteger("NUM")) + "家】");
					}
				}
			}
			result.put("YX_N", jmMsg);
		}
		return result;
	}

	/**
	 * 
	 * 获取有标段组第一数字信封解密标段组信息情况
	 * <p>
	 * 获取有标段组第一数字信封解密标段组信息情况
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return
	 * @throws ServiceException
	 *             服务异常
	 */
	private static List<Record<String, Object>> getFirstEnvelopeGroupInfo(
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取有标段组第一数字信封解密标段组信息情况", tpid));

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sectionGroups = getActiveRecordDAO()
				.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS !='10-0'")
				.setCondition("AND", "V_BID_OPEN_STATUS !='10-1'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.addSortOrder("V_BID_SECTION_GROUP_CODE", "ASC").list(param);

		List<Record<String, Object>> sections = null;
		List<Record<String, Object>> tenderList = null;
		List<String> groupMacList = new ArrayList<String>();
		List<String> macList = null;
		// 迭代标段组
		for (Record<String, Object> sectionGroup : sectionGroups)
		{
			groupMacList.clear();
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("groupId",
					sectionGroup.getString("V_BID_SECTION_GROUP_CODE"));

			// 标段编号
			sectionGroup.setColumn("BID_SECTION_GROUP_CODE",
					sectionGroup.getString("V_BID_SECTION_GROUP_CODE"));

			// 查出投标人名单
			tenderList = getActiveRecordDAO().statement().selectList(
					mybatisName + ".getBiddertInfoByGroup", param);
			logger.debug(LogUtils.format("获取投标人列表", tenderList));

			sections = getActiveRecordDAO()
					.auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND",
							"V_BID_SECTION_GROUP_CODE = #{groupId}")
					.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
					.setCondition("AND", "V_BID_OPEN_STATUS !='10-0'")
					.setCondition("AND", "V_BID_OPEN_STATUS !='10-1'")
					.list(param);

			// 循环标段
			for (Record<String, Object> section : sections)
			{
				param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
				// 获取所有该标段mac地址相同的企业的组织机构号
				macList = getActiveRecordDAO().statement().loadList(
						"Mac.getSameMacOrgCode", param);

				// 如果值重新赋值
				if (CollectionUtils.isEmpty(macList))
				{
					continue;
				}
				groupMacList.addAll(macList);
			}
			// 设置Mac地址是否重复标识
			setBidderMacSomeFlag(tenderList, groupMacList);
			sectionGroup.setColumn("TENDER_LIST",
					analyzeBidderExtendedInfoForFirstEnvelope(tenderList));
		}

		return sectionGroups;
	}

	/**
	 * 
	 * 获取有标段组第二数字信封投标人信息
	 * <p>
	 * 获取有标段组第二数字信封投标人信息
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 标段分配信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<Record<String, Object>> getSecondEnvelopeGroupInfo(
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取有标段组第二数字信封投标人信息", tpid));

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sectionGroups = getActiveRecordDAO()
				.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.addSortOrder("V_BID_SECTION_GROUP_CODE", "ASC").list(param);

		getAllocationInfo(sectionGroups, tpid);

		return sectionGroups;
	}

	/**
	 * 
	 * 获取分配标段信息<br/>
	 * <p>
	 * 获取分配标段信息
	 * </p>
	 * 
	 * @param sectionGroups
	 *            标段组
	 * @param tpid
	 *            招标项目主键
	 * @throws ServiceException
	 *             ServiceException
	 */
	private static void getAllocationInfo(
			List<Record<String, Object>> sectionGroups, String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取分配标段信息", sectionGroups));

		Record<String, Object> param = new RecordImpl<String, Object>();
		List<Record<String, Object>> tenderList = null;
		List<Record<String, Object>> sectionList = null;
		// 投标人分配的标段信息
		JSONObject extInfo = null;
		JSONObject sectionInfo = null;
		// 已分配标识
		boolean isAllocate;
		// 迭代标段组
		for (Record<String, Object> sectionGroup : sectionGroups)
		{
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("groupId",
					sectionGroup.getString("V_BID_SECTION_GROUP_CODE"));

			// 查出投标人名单
			tenderList = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_DECRYPT_INFO)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_GROUP_CODE=#{groupId}")
					.setCondition("AND", "N_ENVELOPE_1=1")
					.setCondition("GROUP BY", "V_BIDDER_ORG_CODE")
					.addSortOrder("N_SORT_FILE_BID", "ASC").list(param);
			logger.debug(LogUtils.format("获取投标人列表", tenderList));

			// 从投标人扩展信息获取已分配标段信息
			for (Record<String, Object> tender : tenderList)
			{
				isAllocate = false;
				extInfo = tender.getJSONObject("V_JSON_OBJ");
				sectionInfo = extInfo.getJSONObject("section");
				if (null != sectionInfo)
				{
					// 有标段信息才做处理
					tender.setColumn("SECTION_ID",
							sectionInfo.getString("V_BID_SECTION_ID"));
					isAllocate = true;
				}
				tender.setColumn("ISALLOCATE", isAllocate);
			}

			// 投标人信息
			sectionGroup.setColumn("TENDER_LIST", tenderList);
			logger.debug(LogUtils.format("投标人信息处理完毕开始处理标段信息"));

			// 获取当前标段组的所有标段
			sectionList = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
					.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
					.setCondition("AND", "V_BID_SECTION_GROUP_CODE=#{groupId}")
					.addSortOrder("V_BID_SECTION_NAME", "ASC").list(param);
			// 标段信息
			sectionGroup.setColumn("SECTION_LIST", sectionList);
		}
	}

	/**
	 * 
	 * 获取第一信封的投标人扩展信息<br/>
	 * <p>
	 * 获取第一信封的投标人扩展信息
	 * </p>
	 * 
	 * @param tenderList
	 *            投标人列表
	 * @return
	 * @throws ServiceException
	 */
	private static List<Record<String, Object>> analyzeBidderExtendedInfoForFirstEnvelope(
			List<Record<String, Object>> tenderList) throws ServiceException
	{
		logger.debug(LogUtils.format("获取第一信封的投标人扩展信息", tenderList));
		// 投标人信息的json字段
		String vjson = null;
		JSONObject jobj = null;
		// 投标人唱标信息集
		JSONArray sings = null;
		// 投标人mac地址集
		JSONArray macs = null;
		// 投标人mac地址
		JSONObject mac = null;
		// 投标人投标人备注
		JSONObject remark = null;
		for (Record<String, Object> tender : tenderList)
		{
			if (StringUtils.isEmpty(tender.getString("V_JSON_OBJ")))
			{
				throw new ServiceException("", "无法获取到投标人的扩展信息[V_JSON_OBJ]");
			}
			vjson = tender.getString("V_JSON_OBJ");
			jobj = JSON.parseObject(vjson);
			sings = jobj.getJSONArray("objSing");
			if (CollectionUtils.isEmpty(sings))
			{
				throw new ServiceException("", "无法获取到投标人的唱标信息!");
			}
			for (int i = 0; i < sings.size(); i++)
			{
				JSONObject sing = sings.getJSONObject(i);
				tender.putAll(sing);
			}
			macs = jobj.getJSONArray("macs");
			if (!CollectionUtils.isEmpty(macs))
			{
				for (int i = 0; i < macs.size(); i++)
				{
					mac = macs.getJSONObject(i);
					if (0 == mac.getInteger("ENVELOPE_INDEX"))
					{
						// 获取第一信封的mac地址
						tender.setColumn("firstMac", mac.getString("MAC"));
					}
					if (1 == mac.getInteger("ENVELOPE_INDEX"))
					{
						// 获取第二信封的mac地址
						tender.setColumn("secondMac", mac.getString("MAC"));
					}
					if (2 == mac.getInteger("ENVELOPE_INDEX"))
					{
						// 获取第三信封的mac地址
						tender.setColumn("thirdMac", mac.getString("MAC"));
					}
				}
			}
			remark = jobj.getJSONObject("remark");
			if (!CollectionUtils.isEmpty(remark))
			{
				// 投标人备注
				tender.setColumn("firstRemark", remark.get("firstRemark"));
			}
		}
		return tenderList;
	}

	/**
	 * 获取普通公路第一信封无标段组评审结果视图<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 获取普通公路第一信封无标段组评审结果视图
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<Record<String, Object>> getFirstReviewSituation(
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取普通公路第一信封无标段组评审结果视图"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);

		logger.debug(LogUtils.format("成功获取标段集的信息", sections));
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.put(ConstantEOKB.PB_DB_ID_VAR,
					SystemParamUtils.getString(SysParamKey.PB_DB_ID_KEY));

			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			// 查出投标人名单
			section.setColumn("TENDER_LIST", getActiveRecordDAO().statement()
					.selectList(mybatisName + ".getReviewResult", param));

		}

		logger.debug(LogUtils.format("成功获取普通公路第一信封无标段组评审结果视图", sections));

		return sections;
	}

	/**
	 * 获取普通公路第一信封有标段组评审结果视图<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 获取普通公路第一信封有标段组评审结果视图
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<Record<String, Object>> getFirstReviewGroupSituation(
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取普通公路第一信封有标段组评审结果视图"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		List<Record<String, Object>> sections = null;
		// 获取当前招标项目的所有标段组
		List<Record<String, Object>> groups = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.list(param);
		logger.debug(LogUtils.format("获取标段组信息", groups));

		String pbDb = SystemParamUtils.getString(SysParamKey.PB_DB_ID_KEY);

		// 循环标段组
		for (Record<String, Object> group : groups)
		{

			param.put("tpid", SessionUtils.getTPID());
			param.setColumn("group",
					group.getString("V_BID_SECTION_GROUP_CODE"));

			sections = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
					.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
					.setCondition("AND", "V_BID_SECTION_GROUP_CODE = #{group}")
					.list(param);

			// 循环标段
			for (Record<String, Object> section : sections)
			{
				param.clear();
				param.put("tpid", tpid);
				param.setColumn("sid", section.getString("V_BID_SECTION_ID"));

				param.put(ConstantEOKB.PB_DB_ID_VAR, pbDb);
				// 标段名称
				section.setColumn("BID_SECTION_NAME",
						section.getString("V_BID_SECTION_NAME"));

				// 查出投标人名单
				section.setColumn(
						"BIDDERS",
						getActiveRecordDAO().statement().selectList(
								mybatisName + ".getReviewResultGroup", param));
			}

			group.setColumn("SECTIONS", sections);
		}

		logger.debug(LogUtils.format("成功获取普通公路第一信封有标段组评审结果视图", groups));
		return groups;
	}

	/**
	 * 获取普通公路勘察设计评审结果<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param status
	 *            开标状态（2-1：第一信封，2-2第二信封）
	 * @return 普通公路勘察设计评审结果视图
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getReviewRecordForKc(String tpid,
			String status) throws ServiceException
	{
		logger.debug(LogUtils.format("获取普通公路勘察设计评审结果"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);

		logger.debug(LogUtils.format("成功获取标段信息", sections));

		// 获取信封状态下标
		String statusIndex = getPbStatusIndex(0, tpid);

		List<Record<String, Object>> bidders = null;
		// 标段名称集
		StringBuilder sectionName = new StringBuilder();
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			if (0 == sectionName.length())
			{
				sectionName.append(section.getString("V_BID_SECTION_NAME"));
			}
			else
			{
				sectionName.append(',').append(
						section.getString("V_BID_SECTION_NAME"));
			}
			param.clear();
			param.put("tpid", tpid);
			param.put("index", statusIndex);
			param.put("status", status);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.put(ConstantEOKB.PB_DB_ID_VAR,
					SystemParamUtils.getString(SysParamKey.PB_DB_ID_KEY));

			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			bidders = getActiveRecordDAO().statement().selectList(
					mybatisName + ".getReviewResultForKc", param);

			// 查出投标人名单
			section.setColumn("TENDER_LIST", bidders);

			String vjson;
			// 勘察设计的开标记录需要展示项目经理名称
			for (Record<String, Object> bidder : bidders)
			{
				vjson = bidder.getString("V_JSON_OBJ");
				bidder.setColumn("tbPeName", BidderElementParseUtils
						.getSingObjAttributeSum(vjson, "tbPeName"));
			}

		}
		result.setColumn("SECTIONS", sectionName);
		result.setColumn("SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功获取普通公路勘察设计第一信封评审结果和开标记录表视图", sections));
		return result;
	}

	/**
	 * 
	 * 根据信封获取评标下标<br/>
	 * <p>
	 * 根据信封获取评标下标
	 * </p>
	 * 
	 * @param index
	 * @param tpid
	 * @return
	 * @throws ServiceException
	 */
	private static String getPbStatusIndex(int index, String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("根据信封获取评标下标", index, tpid));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("type", "MAIL_INDEX_INFO");
		param.setColumn(ConstantEOKB.PB_DB_ID_VAR,
				SystemParamUtils.getString(SysParamKey.PB_DB_ID_KEY));
		// String dataBase =
		// SystemParamUtils.getString(SysParamKey.PB_DB_ID_KEY);
		Record<String, Object> pbRecord = getActiveRecordDAO().statement()
				.selectOne(mybatisName + ".getPbStatusIndex", param);
		if (CollectionUtils.isEmpty(pbRecord))
		{
			throw new ServiceException("", "获取不到评标json");
		}

		JSONObject pbJson = pbRecord.getJSONObject("V_JSON_STRING");
		String key = "firstMail";
		if (index == 1)
		{
			key = "secondMail";
		}
		if (index == 2)
		{
			key = "thirdMail";
		}

		JSONObject statusJson = pbJson.getJSONObject(key);
		param.clear();
		String[] end = statusJson.getString("endMail").split("-");
		return end[end.length - 1];
	}

	/**
	 * 获取普通公路勘察设计第二信封评审结果和开标记录表视图<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 获取普通公路勘察设计第二信封评审结果和开标记录表视图
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getSecondBidRecordForKc(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取普通公路勘察设计第二信封评审结果和开标记录表视图"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);

		logger.debug(LogUtils.format("获取标段信息", sections));

		List<Record<String, Object>> bidders = null;
		// 标段名称集
		StringBuilder sectionName = new StringBuilder();
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			if (0 == sectionName.length())
			{
				sectionName.append(section.getString("V_BID_SECTION_NAME"));
			}
			else
			{
				sectionName.append(',').append(
						section.getString("V_BID_SECTION_NAME"));
			}
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.put(ConstantEOKB.PB_DB_ID_VAR,
					SystemParamUtils.getString(SysParamKey.PB_DB_ID_KEY));

			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			bidders = getActiveRecordDAO().statement().selectList(
					mybatisName + ".getSecReviewResult", param);

			// 查出投标人名单
			section.setColumn("TENDER_LIST", bidders);
		}
		result.setColumn("SECTIONS", sectionName);
		result.setColumn("SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功获取普通公路勘察设计第二信封评审结果和开标记录表视图", result));
		return result;
	}

	/**
	 * 获取普通公路勘察设计第三信封开标记录表视图<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 获取普通公路勘察设计第三信封开标记录表视图
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getThirdBidRecordForKc(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取普通公路勘察设计第二信封评审结果和开标记录表视图", tpid));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);

		logger.debug(LogUtils.format("获取标段信息", sections));

		List<Record<String, Object>> bidders = null;
		// 标段名称集
		StringBuilder sectionName = new StringBuilder();
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			if (0 == sectionName.length())
			{
				sectionName.append(section.getString("V_BID_SECTION_NAME"));
			}
			else
			{
				sectionName.append(',').append(
						section.getString("V_BID_SECTION_NAME"));
			}
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.put(ConstantEOKB.PB_DB_ID_VAR,
					SystemParamUtils.getString(SysParamKey.PB_DB_ID_KEY));

			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			bidders = getActiveRecordDAO().statement().selectList(
					mybatisName + ".getThrReviewResult", param);

			// 查出投标人名单
			section.setColumn("TENDER_LIST", bidders);
		}
		result.setColumn("SECTIONS", sectionName);
		result.setColumn("SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功获取普通公路勘察设计第二信封评审结果和开标记录表视图", result));
		return result;
	}

	/**
	 * 
	 * 获取第一信封开标记录表<br/>
	 * <p>
	 * 获取第一信封开标记录表
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 开标记录
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getFirstOpenBidRecordForm(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取第一信封开标记录表", tpid));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.addSortOrder("V_BID_SECTION_NAME", "ASC").list(param);

		logger.debug(LogUtils.format("获取标段信息", sections));

		// 标段名称集
		StringBuilder sectionName = new StringBuilder();
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			if (0 == sectionName.length())
			{
				sectionName.append(section.getString("V_BID_SECTION_NAME"));
			}
			else
			{
				sectionName.append(',').append(
						section.getString("V_BID_SECTION_NAME"));
			}
		}
		// 非标段组
		if (!SessionUtils.isSectionGroup())
		{
			sections = getFirstEnvelopeSectionInfo(tpid);
		}
		else
		{
			// 标段组
			sections = getFirstEnvelopeGroupInfo(tpid);
		}

		// 是否保存过备注
		param.setColumn("flag", ConstantEOKB.FIRST_REMARK);
		Record<String, Object> tpData = getActiveRecordDAO().auto()
				.table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").get(param);
		boolean remarkflag = true;
		if (!CollectionUtils.isEmpty(tpData))
		{
			remarkflag = false;
		}

		logger.debug(LogUtils.format("获取是否保存过备注", remarkflag));

		result.setColumn("REMARKFLAG", remarkflag);
		result.setColumn("SECTION_LIST", sections);
		result.setColumn("SECTIONS", sectionName.toString());
		return result;
	}

	/**
	 * 获取普通公路第二信封解密情况信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 第二信封解密情况信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getSecondEnvelopeDecryptSituation(
			String tpid) throws ServiceException
	{
		Record<String, Object> result = new RecordImpl<String, Object>();
		logger.debug(LogUtils.format("获取第二信封解密结果"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.addSortOrder("V_BID_SECTION_NAME", "ASC").list(param);
		logger.debug(LogUtils.format("获取当前招标项目的所有标段", param, sections));
		// 标段名称集
		StringBuilder sectionName = new StringBuilder();
		// 查出投标人列表
		List<Record<String, Object>> tenders = null;
		List<String> macList = null;
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			if (0 == sectionName.length())
			{
				sectionName.append(section.getString("V_BID_SECTION_NAME"));
			}
			else
			{
				sectionName.append(',').append(
						section.getString("V_BID_SECTION_NAME"));
			}
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));
			// 查出投标人名单
			tenders = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "N_ENVELOPE_1=1")
					.addSortOrder("N_SORT_FILE_BID", "ASC").list(param);

			// 获取所有该标段mac地址相同的企业的组织机构号
			macList = getActiveRecordDAO().statement().loadList(
					"Mac.getSameMacOrgCode", param);

			if (CollectionUtils.isEmpty(macList))
			{
				macList = new ArrayList<String>();
			}

			// 设置Mac地址是否重复标识
			setBidderMacSomeFlag(tenders, macList);

			section.setColumn("TENDER_LIST",
					analyzeBidderExtendedInfoForFirstEnvelope(tenders));
		}

		// 是否保存过备注
		param.setColumn("flag", ConstantEOKB.SECOND_REMARK);
		Record<String, Object> tpData = getActiveRecordDAO().auto()
				.table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").get(param);
		boolean remarkflag = true;
		if (!CollectionUtils.isEmpty(tpData))
		{
			remarkflag = false;
		}
		logger.debug(LogUtils.format("获取是否保存过备注", remarkflag));
		result.setColumn("SECTION_LIST", sections);
		result.setColumn("SECTIONS", sectionName.toString());
		result.setColumn("REMARKFLAG", remarkflag);
		return result;
	}

	/**
	 * 获取普通公路第二信封解密情况信息（标段组）<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 第二信封解密情况信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getSecondEnvelopeDecryptByGroup(
			String tpid) throws ServiceException
	{
		Record<String, Object> result = new RecordImpl<String, Object>();
		logger.debug(LogUtils.format("获取第二信封解密结果"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		// 获取当前招标项目的所有标段组
		List<Record<String, Object>> groups = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE ")
				.addSortOrder("V_BID_SECTION_GROUP_CODE", "ASC").list(param);
		logger.debug(LogUtils.format("获取当前招标项目的所有标段组", param, groups));
		// 查出投标人列表
		List<Record<String, Object>> tenders = null;
		List<Record<String, Object>> sections = null;
		// 迭代标段组
		for (Record<String, Object> group : groups)
		{
			param.clear();
			param.setColumn("group",
					group.getString("V_BID_SECTION_GROUP_CODE"));
			param.put("tpid", tpid);

			// 获取标段
			sections = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_GROUP_CODE = #{group}")
					.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
					.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
					.addSortOrder("V_BID_SECTION_NAME", "ASC").list(param);
			// 迭代标段
			for (Record<String, Object> section : sections)
			{
				param.clear();
				param.put("tpid", tpid);
				param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
				// 标段名称
				section.setColumn("BID_SECTION_NAME",
						section.getString("V_BID_SECTION_NAME"));
				// 查出投标人名单
				tenders = getActiveRecordDAO().auto()
						.table(TableName.EKB_T_TENDER_LIST)
						.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
						.setCondition("AND", "V_TPID=#{tpid}")
						.setCondition("AND", "N_ENVELOPE_1=1")
						.addSortOrder("N_SORT_FILE_BID", "ASC").list(param);
				section.setColumn("TENDER_LIST",
						analyzeBidderExtendedInfo(tenders));
			}
			group.setColumn("SECTION_LIST", sections);
		}
		result.setColumn("GROUP_LIST", groups);
		return result;
	}

	/**
	 * 获取普通公路勘察设计第二信封解密情况信息<br/>
	 * <p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 第二信封解密情况信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getSecondEnvelopeDecryptForKc(
			String tpid) throws ServiceException
	{
		Record<String, Object> result = new RecordImpl<String, Object>();
		logger.debug(LogUtils.format("获取普通公路勘察设计第二信封解密情况信息", tpid));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();

		param.put("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.addSortOrder("V_BID_SECTION_NAME", "ASC").list(param);

		logger.debug(LogUtils.format("获取当前招标项目的所有标段", param, sections));

		List<Record<String, Object>> tenderList = null;
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));

			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			// 查出投标人名单
			tenderList = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID= #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.setCondition("AND", "N_ENVELOPE_1 = 1")
					.addSortOrder("N_SORT_FILE_BID", "ASC").list(param);

			section.setColumn("TENDER_LIST",
					analyzeBidderExtendedInfo(tenderList));
		}
		result.setColumn("SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功获取普通公路勘察设计第二信封解密情况信息", result));
		return result;
	}

	/**
	 * 获取普通公路勘察设计第三信封解密情况信息<br/>
	 * <p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 第三信封解密情况信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getThridEnvelopeDecryptForKc(
			String tpid) throws ServiceException
	{
		Record<String, Object> result = new RecordImpl<String, Object>();
		logger.debug(LogUtils.format("获取普通公路勘察设计第三信封解密情况信息", tpid));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();

		param.put("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.addSortOrder("V_BID_SECTION_NAME", "ASC").list(param);

		logger.debug(LogUtils.format("获取当前招标项目的所有标段", param, sections));

		List<Record<String, Object>> tenderList = null;
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));

			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			// 查出投标人名单
			tenderList = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID= #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.setCondition("AND", "N_ENVELOPE_1 = 1")
					.addSortOrder("N_SORT_FILE_BID", "ASC").list(param);

			section.setColumn("TENDER_LIST",
					analyzeBidderExtendedInfo(tenderList));
		}
		result.setColumn("SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功获取普通公路勘察设计第三信封解密情况信息", result));
		return result;
	}

	/**
	 * 解析投标人扩展信息<br/>
	 * <p>
	 * 解析投标人扩展信息
	 * </p>
	 * 
	 * @param tenders
	 *            投标人列表
	 * @return
	 * @throws ServiceException
	 *             服务异常
	 */
	private static List<Record<String, Object>> analyzeBidderExtendedInfo(
			List<Record<String, Object>> tenders) throws ServiceException
	{
		logger.debug(LogUtils.format("解析投标人扩展信息", tenders));
		// 扩展信息
		JSONObject extObject = null;
		// 唱标信息列表
		JSONArray sings = null;
		// 单个唱标信息
		JSONObject singObject = null;
		// 开标记录表中代理填入的备注信息
		JSONObject remarks = null;
		// 查出投标人名单
		for (Record<String, Object> tender : tenders)
		{
			if (StringUtils.isEmpty(tender.getString("V_JSON_OBJ")))
			{
				throw new ServiceException("", "无法获取到投标人的扩展信息[V_JSON_OBJ]");
			}
			extObject = JSON.parseObject(tender.getString("V_JSON_OBJ"));
			sings = extObject.getJSONArray("objSing");
			if (CollectionUtils.isEmpty(sings))
			{
				throw new ServiceException("", "无法获取到投标人的唱标信息!");
			}
			for (int i = 0; i < sings.size(); i++)
			{
				singObject = sings.getJSONObject(i);
				// 把唱标信息发到投标人记录中便于页面读取
				tender.putAll(singObject);
				// 开标记录表中代理填入的备注信息
				remarks = extObject.getJSONObject("remark");
				if (!CollectionUtils.isEmpty(remarks))
				{
					// 备注
					tender.setColumn("secondRemark",
							remarks.get("secondRemark"));
					// 备注
					tender.setColumn("thirdRemark", remarks.get("thirdRemark"));
				}
			}

			// 采用费率
			if (StringUtils.equals(
					"1",
					SessionUtils
							.getAttribute(ConstantEOKB.IS_APPRAISAL_PRICE_RATE)
							+ ""))
			{

				tender.setColumn("N_PRICE", tender.getDouble("xfl"));
			}
		}
		return tenders;
	}

	/**
	 * 
	 * 获取评标基准价计算方法视图<br/>
	 * <p>
	 * 获取评标基准价计算方法视图
	 * </p>
	 * 
	 * @param bspmJsons
	 *            基准价方法json字段列表
	 * @return 评标基准价计算方法
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getBidStandardPriceMethodView(
			List<String> bspmJsons) throws ServiceException
	{
		logger.debug(LogUtils.format("获取评标基准价计算方法信息", bspmJsons));

		Record<String, Object> result = new RecordImpl<String, Object>();
		List<Record<String, Object>> bspmInfos = new LinkedList<Record<String, Object>>();
		Record<String, Object> bspmInfo = null;
		JSONObject jobj = null;
		for (String json : bspmJsons)
		{
			// 统计本标段符合的方法的个数
			int methodCount = 0;
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "评标基准价计算方法信息为空!");
			}
			jobj = JSON.parseObject(json);

			// 判断本标段符合的方法的个数
			if (!jobj.getBoolean("IS_LESS_THAN_FIFTEEN"))
			{
				if (jobj.getJSONObject("METHOD_TWO").getBoolean("ADAPTE"))
				{
					methodCount++;
				}
				if (jobj.getJSONObject("METHOD_ONE").getBoolean("ADAPTE"))
				{
					methodCount++;
				}
				if (jobj.getJSONObject("METHOD_THREE").getBoolean("ADAPTE"))
				{
					methodCount++;
				}
			}

			bspmInfo = new RecordImpl<String, Object>();
			bspmInfo.putAll(jobj);
			// 只有一种基准价方法标记
			bspmInfo.put("ONLY_ONE", methodCount == 1 ? true : false);
			bspmInfos.add(bspmInfo);
		}
		result.setColumn("TENDER_PROJECT_BSPM_LIST", bspmInfos);
		logger.debug(LogUtils.format("成功获取评标基准价计算方法信息", result));

		return result;
	}

	/**
	 * 
	 * 获取标段组评标基准价计算方法视图<br/>
	 * <p>
	 * 获取标段组评标基准价计算方法视图
	 * </p>
	 * 
	 * @param bspmJsons
	 *            基准价方法json字段列表
	 * @return 评标基准价计算方法
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getBidStandardPriceMethodGroupView(
			List<String> bspmJsons) throws ServiceException
	{
		logger.debug(LogUtils.format("获取标段组评标基准价计算方法信息", bspmJsons));
		Record<String, Object> rs = new RecordImpl<String, Object>();
		// 标段组与数据对应关系
		Record<String, List<JSONObject>> result = new RecordImpl<String, List<JSONObject>>();
		Map<String, String> groupName = new LinkedHashMap<String, String>();
		JSONObject jobj = null;
		String groupCode = null;
		List<JSONObject> jobjs = null;
		for (String json : bspmJsons)
		{
			int methodCount = 0;
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "评标基准价计算方法信息为空!");
			}
			jobj = JSON.parseObject(json);
			// 获取标段组编码
			groupCode = jobj.getJSONObject("SECTION_INFO").getString(
					"V_BID_SECTION_GROUP_CODE");
			if (StringUtils.isEmpty(groupCode))
			{
				groupCode = "未知";
			}
			jobjs = result.get(groupCode);
			if (null == jobjs)
			{
				jobjs = new LinkedList<JSONObject>();
			}
			// 判断本标段符合的方法的个数
			if (!jobj.getBoolean("IS_LESS_THAN_FIFTEEN"))
			{
				if (jobj.getJSONObject("METHOD_TWO").getBoolean("ADAPTE"))
				{
					methodCount++;
				}
				if (jobj.getJSONObject("METHOD_ONE").getBoolean("ADAPTE"))
				{
					methodCount++;
				}
				if (jobj.getJSONObject("METHOD_THREE").getBoolean("ADAPTE"))
				{
					methodCount++;
				}
			}
			// 只有一种基准价方法标识
			jobj.put("ONLY_ONE", methodCount == 1 ? true : false);
			jobjs.add(jobj);
			result.setColumn(groupCode, jobjs);
			groupName.put(groupCode, groupCode);
		}
		rs.setColumn("SECTION_GROUP_MAP", result);
		rs.setColumn("SECTION_GROUP_NAMES", groupName.keySet());
		logger.debug(LogUtils.format("成功获取标段组评标基准价计算方法信息", rs));
		return rs;
	}

	/**
	 * 
	 * 获取下浮系数视图<br/>
	 * <p>
	 * 获取下浮系数视图
	 * </p>
	 * 
	 * @param lcJsons
	 *            下浮系数json字段
	 * @return 下浮系数视图
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getLowerCoefficientView(
			List<String> lcJsons) throws ServiceException
	{
		logger.debug(LogUtils.format("获取下浮系数信息", lcJsons));
		Record<String, Object> result = new RecordImpl<String, Object>();
		// 下浮系数不为空
		List<Record<String, Object>> lcInfos = new LinkedList<Record<String, Object>>();
		Record<String, Object> lcInfo = null;
		JSONObject jobj = null;
		for (String json : lcJsons)
		{
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "下浮系数信息为空!");
			}
			jobj = JSON.parseObject(json);
			lcInfo = new RecordImpl<String, Object>();
			lcInfo.putAll(jobj);
			lcInfos.add(lcInfo);
		}

		result.setColumn("TENDER_PROJECT_LC_LIST", lcInfos);
		logger.debug(LogUtils.format("成功获取下浮系数信息", result));
		return result;
	}

	/**
	 * 
	 * 获取有标段组下浮系数视图<br/>
	 * <p>
	 * 获取有标段组下浮系数视图
	 * </p>
	 * 
	 * @param lcJsons
	 *            下浮系数json字段
	 * @return 有标段组下浮系数视图
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getLowerCoefficientGroupView(
			List<String> lcJsons) throws ServiceException
	{
		logger.debug(LogUtils.format("获取有标段组下浮系数", lcJsons));
		Record<String, Object> rs = new RecordImpl<String, Object>();
		Record<String, List<JSONObject>> result = new RecordImpl<String, List<JSONObject>>();
		Map<String, String> groupName = new LinkedHashMap<String, String>();
		JSONObject jobj = null;
		String groupCode = null;
		List<JSONObject> jobjs = null;
		// 标段组与数据对应关系
		for (String json : lcJsons)
		{
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "下浮系数信息为空!");
			}
			jobj = JSON.parseObject(json);
			// 获取标段组编码
			groupCode = jobj.getJSONObject("SECTION_INFO").getString(
					"V_BID_SECTION_GROUP_CODE");
			if (StringUtils.isEmpty(groupCode))
			{
				groupCode = "未知";
			}
			jobjs = result.get(groupCode);
			if (null == jobjs)
			{
				jobjs = new LinkedList<JSONObject>();
			}
			jobjs.add(jobj);
			result.setColumn(groupCode, jobjs);
			groupName.put(groupCode, groupCode);
		}
		rs.put("SECTION_GROUP_MAP", result);
		rs.put("SECTION_GROUP_NAMES", groupName.keySet());
		logger.debug(LogUtils.format("成功获取有标段组下浮系数", rs));
		return rs;
	}

	/**
	 * 
	 * 获取评标基准价视图<br/>
	 * <p>
	 * 获取评标基准价视图
	 * </p>
	 * 
	 * @param jsons
	 *            评标基准价json字段集
	 * @param tpid
	 *            项目主键
	 * @return 评标基准价视图
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getCalculateBidBenchmarkPriceView(
			List<String> jsons, String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取评标基准价信息", jsons, tpid));
		Record<String, Object> result = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		Map<String, JSONObject> sectionRecrod = cbspJsonToMap(jsons);
		// 查询出所有未流标的标段信息
		DataEntry de = new DataEntryImpl(TableName.EKB_T_SECTION_INFO);
		de.select("ID", "V_BID_SECTION_ID", "V_BID_SECTION_CODE",
				"V_BID_SECTION_GROUP_CODE", "V_BID_SECTION_NAME",
				"N_BIDDER_NUMBER", "N_CONTROL_PRICE", "N_CONTROL_MIN_PRICE",
				"N_EVALUATION_PRICE", "N_PROJECT_PROVISIONAL_MONEY",
				"N_PROJECT_TEMPORARY_VALUATION")
				.orderBy("V_BID_SECTION_GROUP_CODE", "ASC")
				.orderBy("V_BID_SECTION_NAME", "ASC");
		de.and().equalTo("V_TPID", tpid)
				.notEqualTo("V_BID_OPEN_STATUS", "10-0")
				.notEqualTo("V_BID_OPEN_STATUS", "10-1")
				.notEqualTo("V_BID_OPEN_STATUS", "10-2")
				.notEqualTo("V_BID_EVALUATION_STATUS", "10");
		List<Record<String, Object>> sections = de.persist().load();
		if (CollectionUtils.isEmpty(sections))
		{
			throw new ServiceException("", "找不到任何的标段信息!");
		}
		List<Record<String, Object>> tenders = null;
		String vjson = null;
		Record<String, Object> tpData = null;
		JSONObject jobj = null;
		Double minPrice = 0d;
		Double maxPrice = 0d;
		// 2018-07-23 wengdm 新增暂列金和暂估价，如果这两个有值，计算评标基准价的投标人报价需要减去这两个价格
		// 暂估价
		Double temporaryValuation = 0D;
		// 暂列金
		Double provisionalMmoney = 0D;
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.setColumn("flag", ConstantEOKB.CBSP_BUS_FLAG_TYPE);

			tpData = getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BUS_ID = #{sid}")
					.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}")
					.get(param);

			if (CollectionUtils.isEmpty(tpData))
			{
				throw new ServiceException("", "未查询到相应的基准价信息");
			}
			vjson = tpData.getString("V_JSON_OBJ");
			jobj = JSON.parseObject(vjson);
			// 方法
			String method = jobj.getString("METHOD");
			// 有效投标企业家数
			tenders = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "N_ENVELOPE_1 = 1")
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			logger.debug(LogUtils.format("开始有效投标人列表", tenders));
			int effectiveNum = 0;
			Double price = null;
			minPrice = section.getDouble("N_CONTROL_MIN_PRICE");
			if (minPrice == null)
			{
				minPrice = 0d;
			}
			maxPrice = section.getDouble("N_CONTROL_PRICE");
			if (maxPrice == null)
			{
				maxPrice = 0d;
			}
			for (Record<String, Object> tender : tenders)
			{
				price = tender.getDouble("N_PRICE");
				if (null != price && price <= maxPrice && price >= minPrice)
				{
					effectiveNum++;
				}

			}
			logger.debug(LogUtils.format("开始获取平均值信息", result));

			// 暂估价（元）
			temporaryValuation = section
					.getDouble("N_PROJECT_TEMPORARY_VALUATION");
			if (null == temporaryValuation)
			{
				temporaryValuation = 0D;
			}
			// 暂列金额（元）
			provisionalMmoney = section
					.getDouble("N_PROJECT_PROVISIONAL_MONEY");
			if (null == provisionalMmoney)
			{
				provisionalMmoney = 0D;
			}

			// 平均值
			section.setColumn(
					"EFFECTIVE_PRICE_AVG",
					effectivePriceAvg(tenders, maxPrice, minPrice, method,
							effectiveNum, temporaryValuation, provisionalMmoney));
			section.setColumn("TENDER_LIST", tenders);
			section.putAll(sectionRecrod.get(section
					.getString("V_BID_SECTION_ID")));
		}
		result.put("TENDER_PROJECT_SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功获取评标基准价信息", result));
		return result;

	}

	/**
	 * 
	 * 获取评标基准价视图（勘察）<br/>
	 * <p>
	 * 获取评标基准价视图（勘察）
	 * </p>
	 * 
	 * @param jsons
	 *            评标基准价json字段集
	 * @param tpid
	 *            项目主键
	 * @return 评标基准价视图（勘察）
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getCalculateForKc(List<String> jsons,
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取评标基准价信息", jsons, tpid));
		Record<String, Object> result = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		Map<String, JSONObject> sectionRecrod = cbspJsonToMap(jsons);
		// 查询出所有未流标的标段信息
		DataEntry de = new DataEntryImpl(TableName.EKB_T_SECTION_INFO);
		de.select("ID", "V_BID_SECTION_ID", "V_BID_SECTION_CODE",
				"V_BID_SECTION_GROUP_CODE", "V_BID_SECTION_NAME",
				"N_BIDDER_NUMBER", "N_CONTROL_PRICE", "N_CONTROL_MIN_PRICE",
				"N_EVALUATION_PRICE")
				.orderBy("V_BID_SECTION_GROUP_CODE", "ASC")
				.orderBy("V_BID_SECTION_NAME", "ASC");
		de.and().equalTo("V_TPID", tpid)
				.notEqualTo("V_BID_OPEN_STATUS", "10-0")
				.notEqualTo("V_BID_OPEN_STATUS", "10-1")
				.notEqualTo("V_BID_OPEN_STATUS", "10-2")
				.notEqualTo("V_BID_EVALUATION_STATUS", "10");
		List<Record<String, Object>> sections = de.persist().load();

		logger.debug(LogUtils.format("成功获取标段信息", sections));

		if (CollectionUtils.isEmpty(sections))
		{
			throw new ServiceException("", "找不到任何的标段信息!");
		}
		List<Record<String, Object>> tenders = null;
		Double minPrice = 0d;
		Double maxPrice = 0d;
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			// 有效投标企业家数
			tenders = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "N_ENVELOPE_1 = 1")
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			minPrice = section.getDouble("N_CONTROL_MIN_PRICE");
			if (minPrice == null)
			{
				minPrice = 0d;
			}
			maxPrice = section.getDouble("N_CONTROL_PRICE");
			if (maxPrice == null)
			{
				maxPrice = 0d;
			}
			logger.debug(LogUtils.format("最低限价", minPrice));
			logger.debug(LogUtils.format("最高限价", maxPrice));
			// 平均值
			section.setColumn("EFFECTIVE_PRICE_AVG",
					effectivePriceAvg(tenders, maxPrice, minPrice));
			section.setColumn("TENDER_LIST", tenders);
			section.putAll(sectionRecrod.get(section
					.getString("V_BID_SECTION_ID")));
		}
		result.put("TENDER_PROJECT_SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功获取评标基准价信息", result));
		return result;
	}

	/**
	 * 有效报价平均值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenders
	 * @param maxPrice
	 * @return
	 */
	private static Long effectivePriceAvg(List<Record<String, Object>> tenders,
			Double maxPrice, Double minPrice)
	{

		List<Double> prices = new LinkedList<Double>();
		Double price = null;
		for (Record<String, Object> tender : tenders)
		{
			price = tender.getDouble("N_PRICE");
			if (null != price && price <= maxPrice && price >= minPrice)
			{
				prices.add(price);
			}
		}
		return BidBenchmarkPriceAlgorithmUtils.average(prices);
	}

	/**
	 * 
	 * 获取评标基准价标段组视图<br/>
	 * <p>
	 * 获取评标基准价标段组视图
	 * </p>
	 * 
	 * @param jsons
	 *            基准价信息
	 * @param tpid
	 *            招标项目主键
	 * @return 评标基准价标段组视图
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getCalculateBidBenchmarkPriceGroupView(
			List<String> jsons, String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取标段组评标基准价信息", jsons, tpid));
		Record<String, Object> rs = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		Map<String, JSONObject> sectionRecrod = cbspJsonToMap(jsons);
		// 查询出所有未流标的标段信息
		DataEntry de = new DataEntryImpl(TableName.EKB_T_SECTION_INFO);
		de.select("ID", "V_BID_SECTION_ID", "V_BID_SECTION_CODE",
				"V_BID_SECTION_GROUP_CODE", "V_BID_SECTION_NAME",
				"N_BIDDER_NUMBER", "N_CONTROL_PRICE", "N_CONTROL_MIN_PRICE",
				"N_EVALUATION_PRICE")
				.orderBy("V_BID_SECTION_GROUP_CODE", "ASC")
				.orderBy("V_BID_SECTION_NAME", "ASC");
		de.and().equalTo("V_TPID", tpid)
				.notEqualTo("V_BID_OPEN_STATUS", "10-0")
				.notEqualTo("V_BID_OPEN_STATUS", "10-1")
				.notEqualTo("V_BID_OPEN_STATUS", "10-2")
				.notEqualTo("V_BID_EVALUATION_STATUS", "10");
		List<Record<String, Object>> sections = de.persist().load();
		logger.debug(LogUtils.format("成功获取标段信息", sections));
		if (CollectionUtils.isEmpty(sections))
		{
			throw new ServiceException("", "找不到任何的标段信息!");
		}
		List<Record<String, Object>> tenders = null;
		String groupCode = null;
		List<Record<String, Object>> scs = null;
		// 标段组与数据对应关系
		Record<String, List<Record<String, Object>>> result = new RecordImpl<String, List<Record<String, Object>>>();
		Map<String, String> groupName = new LinkedHashMap<String, String>();
		String vjson = null;
		Record<String, Object> tpData = null;
		JSONObject jobj = null;
		Double minPrice = 0d;
		Double maxPrice = 0d;
		// 2018-07-23 wengdm 新增暂列金和暂估价，如果这两个有值，计算评标基准价的投标人报价需要减去这两个价格
		// 暂估价
		Double temporaryValuation = 0D;
		// 暂列金
		Double provisionalMmoney = 0D;
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.setColumn("flag", ConstantEOKB.CBSP_BUS_FLAG_TYPE);
			tpData = getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BUS_ID = #{sid}")
					.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}")
					.get(param);
			if (CollectionUtils.isEmpty(tpData))
			{
				throw new ServiceException("", "未查询到相应的基准价信息");
			}
			vjson = tpData.getString("V_JSON_OBJ");
			jobj = JSON.parseObject(vjson);
			// 方法
			String method = jobj.getString("METHOD");
			// 有效投标企业家数
			tenders = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "N_ENVELOPE_1 = 1")
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			logger.debug(LogUtils.format("有效投标企业", tenders));
			int effectiveNum = 0;
			Double price = null;
			minPrice = section.getDouble("N_CONTROL_MIN_PRICE");
			if (minPrice == null)
			{
				minPrice = 0d;
			}
			maxPrice = section.getDouble("N_CONTROL_PRICE");
			if (maxPrice == null)
			{
				maxPrice = 0d;
			}

			// 暂估价（元）
			temporaryValuation = section
					.getDouble("N_PROJECT_TEMPORARY_VALUATION");
			if (null == temporaryValuation)
			{
				temporaryValuation = 0D;
			}
			// 暂列金额（元）
			provisionalMmoney = section
					.getDouble("N_PROJECT_PROVISIONAL_MONEY");
			if (null == provisionalMmoney)
			{
				provisionalMmoney = 0D;
			}
			for (Record<String, Object> tender : tenders)
			{
				price = tender.getDouble("N_PRICE");
				if (null != price && price <= maxPrice && price >= minPrice)
				{
					effectiveNum++;
				}

			}
			section.setColumn(
					"EFFECTIVE_PRICE_AVG",
					effectivePriceAvg(tenders, maxPrice, minPrice, method,
							effectiveNum, temporaryValuation, provisionalMmoney));
			section.setColumn("TENDER_LIST", tenders);
			if (null == sectionRecrod
					.get(section.getString("V_BID_SECTION_ID")))
			{
				continue;
			}
			section.putAll(sectionRecrod.get(section
					.getString("V_BID_SECTION_ID")));

			// 获取标段组编码
			groupCode = section.getString("V_BID_SECTION_GROUP_CODE");
			if (StringUtils.isEmpty(groupCode))
			{
				groupCode = "未知";
			}
			scs = result.get(groupCode);
			if (null == scs)
			{
				scs = new LinkedList<Record<String, Object>>();
			}

			scs.add(section);
			result.setColumn(groupCode, scs);
			groupName.put(groupCode, groupCode);
		}
		rs.put("SECTION_GROUP_MAP", result);
		rs.put("SECTION_GROUP_NAMES", groupName.keySet());

		return rs;
	}

	private static Map<String, JSONObject> cbspJsonToMap(List<String> jsons)
	{
		Map<String, JSONObject> rs = new HashMap<String, JSONObject>();
		JSONObject jobj = null;
		for (String json : jsons)
		{
			jobj = JSON.parseObject(json);
			rs.put(jobj.getString("V_BID_SECTION_ID"), jobj);
		}
		return rs;
	}

	/**
	 * 有效报价平均值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenders
	 * @param maxPrice
	 * @return
	 */
	private static Long effectivePriceAvg(List<Record<String, Object>> tenders,
			Double maxPrice, Double minPrice, String method, int effectiveNum,
			Double temporaryValuation, Double provisionalMmoney)
	{

		List<Double> prices = new LinkedList<Double>();
		Double price = null;

		// 大于等于15家
		if (effectiveNum >= 15)
		{
			for (Record<String, Object> tender : tenders)
			{
				// price = Arith.sub(Arith.sub(tender.getDouble("N_PRICE"),
				// temporaryValuation), provisionalMmoney);
				price = tender.getDouble("N_PRICE");

				// 方法1
				if (StringUtils.equals(method, "1"))
				{
					// 将所有大于等于最高限价的95%的有效投标报价进行算数平均
					if (null != price && price <= maxPrice && price >= minPrice
							&& price >= Arith.mul(maxPrice, 0.95))
					{
						prices.add(price);
					}
				}
				// 方法2
				else if (StringUtils.equals(method, "2"))
				{
					// 将所有小于最高限价的95%且大于最高限价的90%的有效投标报价进行算数平均
					if (null != price && price <= maxPrice && price >= minPrice
							&& price >= Arith.mul(maxPrice, 0.90)
							&& price < Arith.mul(maxPrice, 0.95))
					{
						prices.add(price);
					}
				}
				// 方法3
				else if (StringUtils.equals(method, "3"))
				{
					// 将所有小于等于最高限价的90%的有效投标报价进行算数平均
					if (null != price && price <= maxPrice && price >= minPrice
							&& price < Arith.mul(maxPrice, 0.90))
					{
						prices.add(price);
					}
				}
			}
			if (SessionUtils.isPtGdyh())
			{
				for (int i = 0; i < prices.size(); i++)
				{
					prices.set(i, Arith.sub(
							Arith.sub(prices.get(i), temporaryValuation),
							provisionalMmoney));
				}
			}
			return BidBenchmarkPriceAlgorithmUtils.average(prices);
		}

		// 小于15家
		for (Record<String, Object> tender : tenders)
		{
			// price = Arith.sub(
			// Arith.sub(tender.getDouble("N_PRICE"), temporaryValuation),
			// provisionalMmoney);
			price = tender.getDouble("N_PRICE");

			if (null != price && price <= maxPrice && price >= minPrice)
			{
				prices.add(price);
			}
		}
		if (SessionUtils.isPtGdyh())
		{
			for (int i = 0; i < prices.size(); i++)
			{
				prices.set(i, Arith.sub(
						Arith.sub(prices.get(i), temporaryValuation),
						provisionalMmoney));
			}
		}
		return BidBenchmarkPriceAlgorithmUtils.average(prices);
	}

	/**
	 * 
	 * 获取监理的评标基准价视图<br/>
	 * <p>
	 * 获取监理的评标基准价视图
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param tenderProjectNodeID
	 *            流程节点
	 * @return 评标基准价视图
	 * @throws FacadeException
	 *             FacadeException
	 */
	public static List<Record<String, Object>> getCalculateViewForJl(
			String tpid, String tenderProjectNodeID) throws FacadeException
	{
		logger.debug(LogUtils.format("获取监理的评标基准价视图", tpid, tenderProjectNodeID));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("flag", ConstantEOKB.CBSP_BUS_FLAG_TYPE);
		int count = getActiveRecordDAO().auto()
				.table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").count(param);

		// 查询出所有未流标的标段信息
		DataEntry de = new DataEntryImpl(TableName.EKB_T_SECTION_INFO);
		de.select("ID", "V_BID_SECTION_ID", "V_BID_SECTION_CODE",
				"V_BID_SECTION_GROUP_CODE", "V_BID_SECTION_NAME",
				"N_BIDDER_NUMBER", "N_CONTROL_PRICE", "N_CONTROL_MIN_PRICE",
				"N_EVALUATION_PRICE").orderBy("V_BID_SECTION_NAME", "ASC");
		de.and().equalTo("V_TPID", tpid)
				.notEqualTo("V_BID_OPEN_STATUS", "10-0")
				.notEqualTo("V_BID_OPEN_STATUS", "10-1")
				.notEqualTo("V_BID_OPEN_STATUS", "10-2")
				.notEqualTo("V_BID_EVALUATION_STATUS", "10");
		List<Record<String, Object>> sections = de.persist().load();
		logger.debug(LogUtils.format("查询出所有未流标的标段信息", sections));

		if (CollectionUtils.isEmpty(sections))
		{
			throw new ServiceException("", "找不到任何的标段信息!");
		}
		List<Record<String, Object>> tenders = null;
		LinkedList<Double> effectivePrices = null;
		long pbjzj = 0;
		Double minPrice = 0d;
		Double maxPrice = 0d;
		// 节点数据
		Record<String, Object> temp = new RecordImpl<String, Object>();
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			// 这里要查出确认情况列表
			tenders = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "N_ENVELOPE_1 = 1")
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);

			analyzeBidderExtendedInfo(tenders);

			minPrice = section.getDouble("N_CONTROL_MIN_PRICE");
			if (minPrice == null)
			{
				minPrice = 0d;
			}
			maxPrice = section.getDouble("N_CONTROL_PRICE");
			if (maxPrice == null)
			{
				maxPrice = 0d;
			}
			// 获取有效的投标报价列表
			effectivePrices = getEffectivePrices(tenders, maxPrice, minPrice);
			section.setColumn("EFFECTIVE_PRICE_COUNT", effectivePrices.size());
			// 超出控制价的投标报价数量
			section.setColumn("INVALID_PRICE_COUNT", tenders.size()
					- effectivePrices.size());

			// 计算评标基准价
			pbjzj = calculate(effectivePrices, section);
			logger.debug(LogUtils.format("评标基准价", pbjzj));
			// 如果没有计算过,为了防止招标人自己主动去改了评标基准价
			if (count <= 0)
			{
				// 修改标段上的评标基准价值
				param.setColumn("N_EVALUATION_PRICE", pbjzj);
				// 修改标段的评标基准价
				getActiveRecordDAO().auto().table(TableName.EKB_T_SECTION_INFO)
						.setCondition("AND", "V_TPID = #{tpid}")
						.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
						.modify(param);
				section.setColumn("N_EVALUATION_PRICE", pbjzj);

				// 节点数据
				temp.clear();
				temp.setColumn("ID", Random.generateUUID());
				temp.setColumn("V_BUS_FLAG_TYPE",
						ConstantEOKB.CBSP_BUS_FLAG_TYPE);
				temp.setColumn("V_CREATE_USER",
						ApacheShiroUtils.getCurrentUserID()).setColumn(
						"N_CREATE_TIME", System.currentTimeMillis());
				temp.setColumn("V_TPID", tpid);
				temp.setColumn("V_TPFN_ID", tenderProjectNodeID);
				temp.setColumn("V_BUS_ID",
						section.getString("V_BID_SECTION_ID"));
				// 保存节点数据
				getActiveRecordDAO().auto()
						.table(TableName.TENDER_PROJECT_NODE_DATA).save(temp);
			}

			section.setColumn("TENDER_LIST", tenders);
		}
		logger.debug(LogUtils.format("成功获取监理的评标基准价视图", sections));
		return sections;
	}

	/**
	 * 
	 * 有效报价算数平均值计算表视图（勘察设计合理低价法加信用分）<br/>
	 * <p>
	 * 有效报价算数平均值计算表视图（勘察设计合理低价法加信用分）
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 有效报价算数平均值计算表视图
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getAverageValue(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("有效报价算数平均值计算表视图（勘察设计合理低价法加信用分）"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		Record<String, Object> result = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		logger.debug(LogUtils.format("获取所有未流标的标段", sections));
		// 最高限价的95%
		Double ninetyFive = null;
		// 最高限价的90%
		Double ninety = null;
		// 有效平均值
		Double avg = null;

		// 有效的投标人
		List<Record<String, Object>> bidders = null;
		// 全部投标人
		List<Record<String, Object>> allBidders = null;
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", SessionUtils.getTPID());
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			if (!StringUtils.isEmpty(section.getString("N_CONTROL_PRICE")))
			{
				ninetyFive = section.getInteger("N_CONTROL_PRICE") * 0.95;
				ninety = section.getInteger("N_CONTROL_PRICE") * 0.9;
			}
			section.setColumn("NINETYFIVE", ninetyFive);
			section.setColumn("NINETY", ninety);
			// 有效的投标人
			bidders = getActiveRecordDAO().statement().selectList(
					mybatisName + ".getSecValidBidderInfo", param);
			// 全部的投标人
			allBidders = getActiveRecordDAO().statement().selectList(
					mybatisName + ".getSecBidderInfoAll", param);
			if (!CollectionUtils.isEmpty(bidders))
			{
				avg = avgPrice(bidders);
			}
			section.setColumn("BIDDERS", bidders);
			section.setColumn("AVG", avg);
			section.setColumn("VALIDNUM", bidders.size());
			section.setColumn("ALLNUM", allBidders.size());
			section.setColumn("NOVALIDNUM", allBidders.size() - bidders.size());

		}
		result.put("SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功有效报价算数平均值计算表视图（勘察设计合理低价法加信用分）"));
		return result;
	}

	/**
	 * 
	 * 计算有效报价平均值<br/>
	 * <p>
	 * 计算有效报价平均值
	 * </p>
	 * 
	 * @param bidders
	 * @return
	 */
	private static Double avgPrice(List<Record<String, Object>> bidders)
	{
		Double sumPrice = (double) 0;
		for (Record<String, Object> bidder : bidders)
		{
			if (StringUtils.isNotEmpty(bidder.getString("N_PRICE")))
			{
				sumPrice += bidder.getDouble("N_PRICE");
			}
		}

		int count = bidders.size();
		return sumPrice / count;
	}

	/**
	 * 获取有效投标报价列<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenders
	 * @param maxPrice
	 * @param minPrice
	 * @return
	 */
	private static LinkedList<Double> getEffectivePrices(
			List<Record<String, Object>> tenders, Double maxPrice,
			Double minPrice)
	{

		LinkedList<Double> prices = new LinkedList<Double>();
		Double price = null;
		for (Record<String, Object> tender : tenders)
		{
			price = tender.getDouble("N_PRICE");
			// 采用费率
			if (StringUtils.equals(
					"1",
					SessionUtils
							.getAttribute(ConstantEOKB.IS_APPRAISAL_PRICE_RATE)
							+ ""))
			{
				price = tender.getDouble("xfl");
			}
			if (null != price && price <= maxPrice && price >= minPrice)
			{
				prices.add(price);
			}
		}
		return prices;
	}

	/**
	 * 计算评标基准价(监理)<br/>
	 * <p>
	 * （1）评标价的确定： 评标价=财务建议书递交函中监理服务费总额文字报价（未超出招标人设定的控制价上、下限）。<br/>
	 * （2）评标基准价的确定：当评标价大于等于6家时，以所有被宣读的评标价去掉一个最低值和最高值后的算术平均值作为评标基准价；当评标价小于6家时，
	 * 以所有被宣读的评标价去掉一个最低值后的算术平均值作为评标基准价。
	 * 评标基准价在整个评标期间保持不变，不随通过财务建议书符合性审查和详细评审的投标人的数量发生变化。
	 * 
	 * </p>
	 * 
	 * @param effectivePrices
	 *            有效投标报价列表
	 * @param section
	 *            标段
	 * @return
	 * @throws ServiceException
	 */
	private static Long calculate(LinkedList<Double> effectivePrices,
			Record<String, Object> section) throws ServiceException
	{
		// 排序下方便后面的操作
		Collections.sort(effectivePrices);
		int count = effectivePrices.size();
		// （1）评标价的确定：
		// 评标价=财务建议书递交函中监理服务费总额文字报价（未超出招标人设定的控制价上、下限）。
		// （2）评标基准价的确定：当评标价大于等于6家时，以所有被宣读的评标价去掉一个最低值和最高值后的算术平均值作为评标基准价；
		if (6 <= count)
		{
			// 干掉一个最低价
			effectivePrices.removeFirst();
			// 干掉一个最高价
			effectivePrices.removeLast();
			// 参与计算的数量
			section.setColumn("PARTAKE_IN_CALCULATE_COUNT",
					effectivePrices.size());
			return BidBenchmarkPriceAlgorithmUtils.average(effectivePrices);
		}
		// 当评标价小于6家时，以所有被宣读的评标价去掉一个最低值后的算术平均值作为评标基准价。
		// 干掉一个最低价
		effectivePrices.removeFirst();
		// 参与计算的数量
		section.setColumn("PARTAKE_IN_CALCULATE_COUNT", effectivePrices.size());
		// 评标基准价在整个评标期间保持不变，不随通过财务建议书符合性审查和详细评审的投标人的数量发生变化。
		return BidBenchmarkPriceAlgorithmUtils.average(effectivePrices);
	}

	/**
	 * 
	 * 保存投标人备注信息<br/>
	 * <p>
	 * 保存投标人备注信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param remarks
	 *            页面获取的投标人的备注
	 * @param tpid
	 *            项目ID
	 * @param envelopeFlag
	 *            信封标识
	 * @param dbFlag
	 *            dbFlag
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static void saveFirstRemark(AeolusData data, String remarks,
			String tpid, String envelopeFlag, String dbFlag)
			throws ServiceException
	{
		logger.debug(LogUtils.format("保存投标人第一信封备注信息", remarks, tpid));
		// 投标人备注
		JSONArray arr = JSON.parseArray(remarks);

		if (!CollectionUtils.isEmpty(arr))
		{
			saveBidderJsonRemark(arr, envelopeFlag);
		}
		// 代理保存后将数据插入节点状态表
		Record<String, Object> tpData = new RecordImpl<String, Object>();
		tpData.setColumn("ID", Random.generateUUID());
		tpData.setColumn("V_BUS_FLAG_TYPE", dbFlag);
		tpData.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
				.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		tpData.setColumn("V_TPID", SessionUtils.getTPID());
		getActiveRecordDAO().auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.save(tpData);
	}

	/**
	 * 
	 * 保存投标人备注信息<br/>
	 * <p>
	 * 保存投标人备注信息
	 * </p>
	 * 
	 * @param arr
	 * @param envelopeFlag
	 * @throws ServiceException
	 */
	private static void saveBidderJsonRemark(JSONArray arr, String envelopeFlag)
			throws ServiceException
	{
		JSONObject jobj = null;
		Record<String, Object> bidder = null;
		Record<String, Object> param = new RecordImpl<String, Object>();
		JSONObject jsonObj = null;
		// 投标人备注
		JSONObject remark = new JSONObject();
		for (int i = 0; i < arr.size(); i++)
		{
			jobj = arr.getJSONObject(i);
			for (Entry<String, Object> entry : jobj.entrySet())
			{
				param.clear();
				// 获取投标人ID
				String id = entry.getKey();
				param.setColumn("ID", id);
				bidder = getActiveRecordDAO().auto()
						.table(ConstantEOKB.TableName.EKB_T_TENDER_LIST)
						.get(param);
				if (!CollectionUtils.isEmpty(bidder))
				{
					jsonObj = JSONObject.parseObject(bidder
							.getString("V_JSON_OBJ"));

					remark = jsonObj.getJSONObject("remark");
					if (remark == null)
					{
						remark = new JSONObject();
					}
					remark.put(envelopeFlag, entry.getValue().toString());
					jsonObj.put("remark", remark);
					param.clear();
					param.setColumn("ID", id);
					param.setColumn("V_JSON_OBJ", jsonObj.toString());
					getActiveRecordDAO().auto()
							.table(ConstantEOKB.TableName.EKB_T_TENDER_LIST)
							.modify(param);
				}
			}
		}
	}
}
