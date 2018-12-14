/**
 * 包名：com.sozone.eokb.mobile.bus.bidder
 * 文件名：BidderAction.java<br/>
 * 创建时间：2018-11-22 上午10:08:14<br/>
 * 创建者：wengdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.bus.bidder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.ListSortUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 投标人信息服务类<br/>
 * <p>
 * 投标人信息服务类<br/>
 * </p>
 * Time：2018-11-22 上午10:08:14<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/mobile/bidder", desc = "投标人信息服务类")
// 登录即可访问
@Permission(Level.Authenticated)
public class BidderAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(BidderAction.class);

	/**
	 * 持久化接口
	 */
	protected ActiveRecordDAO activeRecordDAO = null;

	/**
	 * activeRecordDAO属性的set方法
	 * 
	 * @param activeRecordDAO
	 *            the activeRecordDAO to set
	 */
	public void setActiveRecordDAO(ActiveRecordDAO activeRecordDAO)
	{
		this.activeRecordDAO = activeRecordDAO;
	}

	/**
	 * 
	 * 获取投标人列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 投标人列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "list", desc = "获取投标人列表")
	@Service
	public Record<String, Object> getBidderList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取投标人列表", data));
		String tpID = data.getParam("tpID");
		String index = data.getParam("index");
		String envlopeIndex = "N_ENVELOPE_" + index;

		// 确认模块类型
		String modelType = "DYXF_offer";
		if (StringUtils.equals("1", index))
		{
			modelType = "DEXF_offer";
		}

		Record<String, Object> project = this.activeRecordDAO.pandora()
				.SELECT("V_BEM_INFO_JSON")
				.FROM(TableName.EKB_T_TENDER_PROJECT_INFO).EQUAL("ID", tpID)
				.get();

		if (CollectionUtils.isEmpty(project))
		{
			throw new FacadeException("", "未获取到相关的项目信息");
		}
		// 勘察设计和勘察监理按照第一信封评审总分排名
		Record<String, Object> result = new RecordImpl<String, Object>();
		String pbCode = project.getJSONObject("V_BEM_INFO_JSON").getString(
				"V_CODE");
		result.setColumn("IS_KCSJ", StringUtils.contains(pbCode, "kcjl")
				|| StringUtils.contains(pbCode, "kcsj"));

		// 获取没有流标和已经启动标段列表
		List<Record<String, Object>> sections = this.activeRecordDAO.pandora()
				.SELECT("ID,V_BID_SECTION_ID,V_BID_SECTION_NAME,N_CONTROL_PRICE")
				.FROM(TableName.EKB_T_SECTION_INFO).EQUAL("V_TPID", tpID)
				.NOT_EQUAL("V_BID_OPEN_STATUS", "0")
				.WHERE("V_BID_OPEN_STATUS NOT LIKE '10%'").list();

		if (CollectionUtils.isEmpty(sections))
		{
			throw new FacadeException("", "为获取到相关信息标段");
		}

		// 投标人唱标内容
		JSONObject cbObject = null;
		Record<String, Object> temp = null;
		// 获取每个标段下面的投标人
		for (Record<String, Object> section : sections)
		{
			List<Record<String, Object>> bidders = this.activeRecordDAO
					.pandora()
					.SELECT("ID,V_BIDDER_NAME,V_BIDDER_NO,V_BIDDER_ORG_CODE,N_PRICE")
					.FROM(TableName.EKB_T_TENDER_LIST)
					.EQUAL("V_TPID", tpID)
					.EQUAL(envlopeIndex, 1)
					.EQUAL("V_BID_SECTION_ID",
							section.getString("V_BID_SECTION_ID")).list();
			if (CollectionUtils.isEmpty(bidders))
			{
				continue;
			}
			// 第二信封开始，需要按照评审得分降序排序
			if (StringUtils.equals("1", index) && result.getBoolean("IS_KCSJ"))
			{
				for (Record<String, Object> bidder : bidders)
				{
					temp = this.activeRecordDAO.pandora()
							.SELECT("ID,V_JSON_OBJ,V_BIDDER_ORG_CODE")
							.FROM(TableName.EKB_T_TENDER_LIST)
							.EQUAL("ID", bidder.getString("V_BIDDER_ID")).get();

					cbObject = temp.getJSONObject("V_JSON_OBJ");
					cbObject.put("N_TOTAL", bidder.getDouble("N_TOTAL"));
				}
				// 按照评审分数降序
				ListSortUtils.sort(bidders, false, "N_TOTAL");
			}

			isCurrentProjectBidder(section, bidders, tpID, modelType);
			section.setColumn("BIDDER_LIST", bidders);
		}

		result.setColumn("SECTION_LIST", sections);
		return result;
	}

	/**
	 * 
	 * 获取投标人信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 投标人信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "info", desc = "获取投标人信息")
	@Service
	public Record<String, Object> getBidderInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取投标人信息", data));
		String bidderID = data.getParam("bidderID");
		String index = data.getParam("index");
		String tableName = data.getParam("tableName");
		if (StringUtils.isEmpty(index))
		{
			index = "0";
		}

		String envlopeIndex = "N_ENVELOPE_" + index;
		// 获取投标人信息
		Record<String, Object> bidder = this.activeRecordDAO.pandora()
				.SELECT("V_BIDDER_NAME,V_JSON_OBJ").FROM(tableName)
				.EQUAL("ID", bidderID).EQUAL(envlopeIndex, 1).get();
		if (CollectionUtils.isEmpty(bidder))
		{
			throw new FacadeException("", "未获取到投标人信息");
		}
		JSONObject singData = bidder.getJSONObject("V_JSON_OBJ");
		if (CollectionUtils.isEmpty(singData))
		{
			throw new FacadeException("", "未获取到投标人扩展信息");

		}
		JSONArray objSing = singData.getJSONArray("objSing");
		if (CollectionUtils.isEmpty(objSing))
		{
			throw new FacadeException("", "未获取到投标人唱标信息");
		}

		// 中文正则表达式
		Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
		List<Record<String, Object>> cbList = new LinkedList<Record<String, Object>>();
		for (int i = 0; i < objSing.size(); i++)
		{
			// 唱标信息
			JSONObject sing = objSing.getJSONObject(i);

			if (!StringUtils.equals(index, sing.getString("index")))
			{
				continue;
			}
			// 去除下标（Index）
			sing.remove("index");
			Record<String, Object> singInfo = new RecordImpl<String, Object>();
			// 获取唱标内容
			for (Entry<String, Object> entry : sing.entrySet())
			{
				// 从唱标要素中获取中文的key作为标题集
				if (p.matcher(entry.getKey()).find())
				{
					singInfo.setColumn("label", entry.getKey());
				}
				singInfo.setColumn("value", entry.getValue());
			}

			changeSelectValue(singInfo);
			// objSing.set(i, singInfo);
			cbList.add(singInfo);
		}
		bidder.setColumn("CB_LIST", cbList);
		return bidder;
	}

	/**
	 * 
	 * 部分唱标内容在制作文件系统是下拉选，需要把0,1转换为是否
	 * <p>
	 * </p>
	 * 
	 * @param singInfo
	 *            唱标内容
	 */
	private void changeSelectValue(Record<String, Object> singInfo)
	{
		if (StringUtils.contains(singInfo.getString("label"), "是否"))
		{
			String value = singInfo.getString("value");
			if (StringUtils.equals("0", value))
			{
				value = "否";
			}
			else if (StringUtils.equals("1", value))
			{
				value = "是";
			}
			singInfo.setColumn("value", value);

		}
	}

	/**
	 * 
	 * 获取投标人信用等级列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 投标人信用等级列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "creditlist", desc = "获取投标人信用等级列表")
	@Service
	public List<Record<String, Object>> getBidderCreditList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取投标人信用等级列表", data));
		String tpID = data.getParam("tpID");
		String bidderName = data.getParam("bidderName");
		// 获取没有流标和已经启动标段组列表
		List<Record<String, Object>> groups = this.activeRecordDAO.pandora()
				.SELECT("V_BID_SECTION_GROUP_CODE AS 'group',V_TPID AS tpID ")
				.FROM(TableName.EKB_T_SECTION_INFO).EQUAL("V_TPID", tpID)
				.NOT_EQUAL("V_BID_OPEN_STATUS", "0")
				.WHERE("V_BID_OPEN_STATUS NOT LIKE '10%'")
				.ORDER_BY("V_BID_SECTION_GROUP_CODE")
				.GROUP_BY("V_BID_SECTION_GROUP_CODE").list();

		if (CollectionUtils.isEmpty(groups))
		{
			throw new FacadeException("", "为获取到相关标段组信息");
		}
		// 获取每个标段下面的投标人
		for (Record<String, Object> group : groups)
		{
			group.setColumn("V_BIDDER_NAME", bidderName);
			List<Record<String, Object>> bidders = this.activeRecordDAO
					.statement()
					.selectList("mobile.getBidderCreditList", group);
			if (CollectionUtils.isEmpty(bidders))
			{
				continue;
			}
			setBidderExtInfo(bidders);
			isCurrentProjectBidder(group, bidders, tpID, "DYXF_credit");
			group.setColumn("BIDDERS", bidders);
		}
		return groups;
	}

	/**
	 * 
	 * 判断该用户是否是本项目的投标人<br/>
	 * <p>
	 * 判断该用户是否是本项目的投标人
	 * </p>
	 * 
	 * @param record
	 *            业务记录
	 * @param bidders
	 *            投标人列表
	 * @param tpID
	 *            招标项目主键
	 * @param modelType
	 *            确认模块
	 * @throws FacadeException
	 *             FacadeException
	 */
	private void isCurrentProjectBidder(Record<String, Object> record,
			List<Record<String, Object>> bidders, String tpID, String modelType)
			throws FacadeException
	{
		logger.debug(LogUtils.format("判断该用户是否是本项目的投标人", record, bidders));
		// 用户组织机构号码
		String orgCode = SessionUtils.getCompanyCode();
		record.setColumn("IS_CURRENT_PROJECT_BIDDER", false);
		for (Record<String, Object> bidder : bidders)
		{
			// 是本项目的投标人
			if (StringUtils.equals(orgCode,
					bidder.getString("V_BIDDER_ORG_CODE")))
			{
				record.setColumn("IS_CURRENT_PROJECT_BIDDER", true);
				record.setColumn("BIDDER_ID", bidder.getString("ID"));
				// 在获取用户确认信息
				Record<String, Object> confirm = this.activeRecordDAO.pandora()
						.SELECT("V_STATUS,V_REMARK")
						.FROM(TableName.EKB_T_CHECK_DATA).EQUAL("V_TPID", tpID)
						.EQUAL("V_BUSID", bidder.getString("ID"))
						.EQUAL("V_BUSNAME", modelType).get();
				record.setColumn("HAS_CONFIRM", false);
				// 查询到记录，已确认过
				if (!CollectionUtils.isEmpty(confirm))
				{
					record.setColumn("HAS_CONFIRM", true);
					record.setColumn("CONFIRM", confirm);
				}
				break;
			}
		}
	}

	/**
	 * 
	 * 设置投标人扩展信息<br/>
	 * <p>
	 * 设置投标人扩展信息
	 * </p>
	 * 
	 * @param bidders
	 *            投标人列表
	 */
	private void setBidderExtInfo(List<Record<String, Object>> bidders)
	{
		logger.debug(LogUtils.format("设置投标人扩展信息", bidders));
		Map<String, List<String>> colMap = new HashMap<String, List<String>>();

		// 关联编号
		String correlateCode = null;
		// 投标人名称集
		List<String> bidderNames = null;
		// 第一次循环获取所有的投标人名称和关联企业的映射关系
		for (Record<String, Object> bidder : bidders)
		{
			correlateCode = bidder.getString("V_CORRELATE_CODE");
			// 没有关联编号，不做处理
			if (StringUtils.isEmpty(correlateCode))
			{
				continue;
			}
			// 已有
			if (colMap.containsKey(correlateCode))
			{
				colMap.get(correlateCode)
						.add(bidder.getString("V_BIDDER_NAME"));
				continue;
			}
			bidderNames = new ArrayList<String>();
			bidderNames.add(bidder.getString("V_BIDDER_NAME"));
			colMap.put(correlateCode, bidderNames);
		}

		List<Record<String, Object>> extInfos = null;
		Record<String, Object> extInfo = null;

		// 构建投标人扩展信息
		for (Record<String, Object> bidder : bidders)
		{
			correlateCode = bidder.getString("V_CORRELATE_CODE");
			extInfos = new LinkedList<Record<String, Object>>();

			extInfo = new RecordImpl<String, Object>();
			extInfo.setColumn("label", "投标人编号");
			extInfo.setColumn("value", bidder.getString("V_BIDDER_NO"));
			extInfos.add(extInfo);

			extInfo = new RecordImpl<String, Object>();
			extInfo.setColumn("label", "已投标段");
			extInfo.setColumn("value", bidder.getString("SECTIONS"));
			extInfos.add(extInfo);

			extInfo = new RecordImpl<String, Object>();
			extInfo.setColumn("label", "关联企业");
			extInfo.setColumn("value",
					setAssociatedEnterprises(colMap, correlateCode));
			extInfos.add(extInfo);
			bidder.setColumn("EXT_INFOS", extInfos);
		}
	}

	/**
	 * 
	 * 设置关联企业<br/>
	 * <p>
	 * 设置关联企业
	 * </p>
	 * 
	 * @param colMap
	 *            投标人名称和关联企业的映射关系
	 * @param correlateCode
	 *            关联编号
	 * @return
	 */
	private String setAssociatedEnterprises(Map<String, List<String>> colMap,
			String correlateCode)
	{
		logger.debug(LogUtils.format("设置关联企业", colMap, correlateCode));
		if (StringUtils.isEmpty(correlateCode))
		{
			return "无";
		}
		List<String> bidderNames = colMap.get(correlateCode);
		if (bidderNames.size() <= 1)
		{
			return "无";
		}
		String associatedEnterprises = bidderNames.toString();
		// 去除[]
		return associatedEnterprises.substring(1,
				associatedEnterprises.length() - 1);
	}

	/**
	 * 
	 * 获取电子摇号列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 投标人电子摇号列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "electronicslist", desc = "获取投标人电子摇号列表")
	@Service
	public List<Record<String, Object>> getElectronicsList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取投标人电子摇号列表", data));
		String tpID = data.getParam("tpID");
		// 获取没有流标和已经启动标段组列表
		List<Record<String, Object>> groups = this.activeRecordDAO.pandora()
				.SELECT("V_BID_SECTION_GROUP_CODE ")
				.FROM(TableName.EKB_T_SECTION_INFO).EQUAL("V_TPID", tpID)
				.NOT_EQUAL("V_BID_OPEN_STATUS", "0")
				.WHERE("V_BID_OPEN_STATUS NOT LIKE '10%'")
				.ORDER_BY("V_BID_SECTION_GROUP_CODE")
				.GROUP_BY("V_BID_SECTION_GROUP_CODE").list();

		if (CollectionUtils.isEmpty(groups))
		{
			throw new FacadeException("", "未获取到相关标段信息");
		}
		// 获取每个标段下面的投标人
		boolean hasElectronics;
		List<Record<String, Object>> sections = null;
		List<Record<String, Object>> bidders = null;
		for (Record<String, Object> group : groups)
		{
			hasElectronics = true;
			sections = this.activeRecordDAO
					.pandora()
					.SELECT_ALL_FROM(TableName.EKB_T_SECTION_INFO)
					.EQUAL("V_BID_SECTION_GROUP_CODE",
							group.getString("V_BID_SECTION_GROUP_CODE"))
					.EQUAL("V_TPID", tpID).ORDER_BY("V_BID_SECTION_CODE")
					.list();
			for (Record<String, Object> section : sections)
			{
				bidders = this.activeRecordDAO
						.pandora()
						.SELECT("ID,V_BIDDER_NAME,V_BIDDER_NO,V_JSON_OBJ,V_CORRELATE_CODE,V_BIDDER_ORG_CODE")
						.FROM(TableName.EKB_T_TENDER_LIST)
						.EQUAL("V_TPID", tpID)
						.EQUAL("N_ENVELOPE_0", 1)
						.EQUAL("V_BID_SECTION_ID",
								section.getString("V_BID_SECTION_ID")).list();
				if (CollectionUtils.isEmpty(bidders))
				{
					hasElectronics = false;
					continue;
				}
				// 获取投标人评标使用的信用等级
				for (Record<String, Object> bidder : bidders)
				{
					bidder.setColumn("tbRatingsInEvl", getTbRatingsInEvl(bidder
							.getJSONObject("V_JSON_OBJ")));
				}
				section.setColumn("BIDDERS", bidders);
				section.setColumn("BIDDER_COUNT", bidders.size());
			}
			isCurrentProjectBidder(group, bidders, tpID, "DYXF_electronics");
			group.setColumn("HAS_ELECTRONICS", hasElectronics);
			group.setColumn("SECTIONS", sections);
		}
		return groups;
	}

	/**
	 * 
	 * 获取评标使用的等级<br/>
	 * <p>
	 * 获取评标使用的等级
	 * </p>
	 * 
	 * @param jobj
	 *            唱标信息
	 * @return
	 */
	private String getTbRatingsInEvl(JSONObject jobj)
	{
		logger.debug(LogUtils.format("获取评标使用的等级", jobj));
		if (!CollectionUtils.isEmpty(jobj))
		{
			JSONArray objSing = jobj.getJSONArray("objSing");
			if (!CollectionUtils.isEmpty(objSing))
			{
				for (int i = 0; i < objSing.size(); i++)
				{
					jobj = objSing.getJSONObject(i);
					if (jobj.containsKey("tbRatingsInEvl"))
					{
						return jobj.getString("tbRatingsInEvl");
					}
				}
			}
		}
		return "";
	}

	/**
	 * 
	 * 展示电子摇号过程列表<br/>
	 * <p>
	 * 展示电子摇号过程列表
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 电子摇号列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "electronicsinfo", desc = "展示电子摇号过程列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> showElectronisc(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("展示电子摇号过程列表", data));
		String tpID = data.getParam("tpID");
		String groupCode = data.getParam("groupCode");

		List<Record<String, Object>> electronics = this.activeRecordDAO
				.pandora().SELECT_ALL_FROM(TableName.EKB_T_ELECTRONICS)
				.EQUAL("V_TPID", tpID)
				.EQUAL("V_BID_SECTION_GROUP_CODE", groupCode).list();
		if (CollectionUtils.isEmpty(electronics))
		{
			throw new FacadeException("", "查询不到电子摇号列表");
		}
		return electronics;
	}

	/**
	 * 
	 * 获取评审结果<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 评审结果
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "reviewlist", desc = "获取评审结果")
	@Service
	public Record<String, Object> getReviewList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取评审结果", data));
		String tpID = data.getParam("tpID");
		Record<String, Object> project = this.activeRecordDAO.pandora()
				.SELECT("V_BEM_INFO_JSON")
				.FROM(TableName.EKB_T_TENDER_PROJECT_INFO).EQUAL("ID", tpID)
				.get();

		if (CollectionUtils.isEmpty(project))
		{
			throw new FacadeException("", "未获取到相关的项目信息");
		}
		// 勘察设计和勘察监理按照第一信封评审总分排名
		Record<String, Object> result = new RecordImpl<String, Object>();
		String pbCode = project.getJSONObject("V_BEM_INFO_JSON").getString(
				"V_CODE");
		result.setColumn("IS_KCSJ", StringUtils.contains(pbCode, "kcjl")
				|| StringUtils.contains(pbCode, "kcsj"));

		// 获取没有流标和已经启动标段列表
		List<Record<String, Object>> sections = this.activeRecordDAO
				.pandora()
				.SELECT("ID,V_BID_SECTION_ID,V_BID_SECTION_NAME,V_BID_EVALUATION_STATUS")
				.FROM(TableName.EKB_T_SECTION_INFO).EQUAL("V_TPID", tpID)
				.NOT_EQUAL("V_BID_OPEN_STATUS", "0")
				.WHERE("V_BID_OPEN_STATUS NOT LIKE '10%'").list();

		if (CollectionUtils.isEmpty(sections))
		{
			throw new FacadeException("", "为获取到相关信息标段");
		}
		// 获取每个标段下面的投标人
		boolean isPbOver = true;
		Record<String, Object> param = new RecordImpl<String, Object>();
		List<Record<String, Object>> bidderList = null;
		for (Record<String, Object> section : sections)
		{
			// 有任何一个标段的评审状态为0，则未评审完毕
			if (StringUtils.equals("0",
					section.getString("V_BID_EVALUATION_STATUS")))
			{
				isPbOver = false;
				break;
			}
			param.clear();
			param.put("tpid", tpID);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.put(ConstantEOKB.PB_DB_ID_VAR,
					SystemParamUtils.getString(SysParamKey.PB_DB_ID_KEY));

			// 查出投标人名单
			bidderList = this.activeRecordDAO.statement().selectList(
					"fjs_gsgl_common.getReviewResult", param);
			section.setColumn("BIDDER_LIST", bidderList);

			// 如果是勘察设=设计和监理需要对投标人的分数排序
			if (result.getBoolean("IS_KCSJ"))
			{
				section.setColumn("BIDDER_LIST",
						sortBidderByReviewScore(bidderList));
			}
		}

		result.setColumn("SECTION_lIST", sections);
		result.setColumn("IS_PB_OVER", isPbOver);

		return result;
	}

	/**
	 * 
	 * 按照评审分数排名<br/>
	 * <p>
	 * 按照评审分数排名
	 * </p>
	 * 
	 * @param bidderList
	 *            投标人列表
	 * @return 排序后的投标人列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	private List<Record<String, Object>> sortBidderByReviewScore(
			List<Record<String, Object>> bidderList) throws FacadeException
	{
		if (CollectionUtils.isEmpty(bidderList))
		{
			return bidderList;
		}

		// 投标人唱标内容
		JSONObject cbObject;
		for (Record<String, Object> bidder : bidderList)
		{
			Record<String, Object> temp = this.activeRecordDAO.pandora()
					.SELECT("ID,V_JSON_OBJ").FROM(TableName.EKB_T_TENDER_LIST)
					.EQUAL("ID", bidder.getString("V_BIDDER_ID")).get();

			cbObject = temp.getJSONObject("V_JSON_OBJ");
			cbObject.put("N_TOTAL", bidder.getDouble("N_TOTAL"));
			temp.setColumn("V_JSON_OBJ", cbObject.toJSONString());
			// 保存投标人排名
			this.activeRecordDAO.pandora().UPDATE(TableName.EKB_T_TENDER_LIST)
					.SET(temp).excute();
		}
		// 按照评审分数降序
		ListSortUtils.sort(bidderList, false, "N_TOTAL");
		return bidderList;
	}
}
