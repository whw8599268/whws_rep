/**
 * 包名：com.sozone.eokb.fjs_sygc.common
 * 文件名：SygcUtils.java<br/>
 * 创建时间：2017-12-29 下午4:11:20<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_sygc.common;

import java.util.LinkedList;
import java.util.List;
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
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.orm.DataEntry;
import com.sozone.aeolus.ext.orm.impl.DataEntryImpl;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 水运工程通用类<br/>
 * <p>
 * 水运工程通用类<br/>
 * </p>
 * Time：2017-12-29 下午4:11:20<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SygcUtils
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(SygcUtils.class);

	private static final String SQLNAME = ConstantEOKB.EOKBBemCode.FJS_SYGC_COMMON;

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
	 * 
	 * 获取水运解密情况信息<br/>
	 * <p>
	 * 获取水运解密情况信息
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID getDecryptSituation
	 * @return List
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<Record<String, Object>> getDecryptSituation(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取水运解密情况信息", tpid));
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

		logger.debug(LogUtils.format("获取未流标的标段信息", sections));
		// 投标人信息的json字段
		String vjson = null;
		JSONObject jobj = null;
		// 投标人唱标信息集
		JSONArray sings = null;

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
			}
			section.setColumn("TENDER_LIST", tenderList);
		}
		logger.debug(LogUtils.format("成功获取水运解密情况信息", sections));
		return sections;
	}

	/**
	 * 
	 * 获取水运解密家数<br/>
	 * <p>
	 * 获取水运解密家数
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 解密家数
	 * @throws ServiceException
	 *             服务异常
	 */
	public static String getDecryptSuccessCount(String tpid)
			throws ServiceException
	{

		logger.debug(LogUtils.format("获取水运解密家数"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		// 查询成功家数
		String sql = "SELECT COUNT(A.V_BID_SECTION_ID) NUM,B.V_BID_SECTION_NAME,A.V_BID_SECTION_ID FROM EKB_T_TENDER_LIST A "
				+ "RIGHT JOIN EKB_T_SECTION_INFO B ON A.V_BID_SECTION_ID = B.V_BID_SECTION_ID AND B.V_BID_OPEN_STATUS NOT LIKE '10%' "
				+ "WHERE A.V_TPID=#{tpid} AND A.N_ENVELOPE_0=1  GROUP BY A.V_BID_SECTION_ID ORDER BY B.V_BID_SECTION_NAME";
		List<Record<String, Object>> recordYX = getActiveRecordDAO().sql(sql)
				.setParam("tpid", tpid).list();
		logger.debug(LogUtils.format("解密成功家数", recordYX.size()));
		// 查询全部家数
		sql = "SELECT COUNT(A.V_BID_SECTION_ID) NUM,A.V_BID_SECTION_ID FROM EKB_T_TBIMPORTBIDDING A  "
				+ "WHERE A.V_TPID=#{tpid} AND V_FTID='111' GROUP BY A.V_BID_SECTION_ID";
		List<Record<String, Object>> recordAX = getActiveRecordDAO().sql(sql)
				.setParam("tpid", tpid).list();
		logger.debug(LogUtils.format("全部家数家数", recordAX.size()));
		StringBuffer jmMsg = new StringBuffer();
		for (Record<String, Object> record : recordYX)
		{
			for (Record<String, Object> re : recordAX)
			{
				if (StringUtils.equals(record.getString("V_BID_SECTION_ID"),
						re.getString("V_BID_SECTION_ID")))
				{
					jmMsg.append("【标段："
							+ record.getString("V_BID_SECTION_NAME") + "，投标人数："
							+ record.getString("NUM") + "家;失败："
							+ (re.getInteger("NUM") - record.getInteger("NUM"))
							+ "家】");
				}
			}
		}
		return jmMsg.toString();
	}

	/**
	 * 
	 * 评标基准价方法抽取视图<br/>
	 * <p>
	 * 评标基准价方法抽取视图
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @return List
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static List<Record<String, Object>> getBidStandardPriceMethodView(
			String tpid, String tenderProjectNodeID) throws ServiceException
	{
		logger.debug(LogUtils.format("评标基准价方法抽取视图", tpid, tenderProjectNodeID));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		// 先查评标基准价信息算法JSON
		List<String> bspmJsons = getActiveRecordDAO().statement().loadList(
				SQLNAME + ".getBidStandardPriceMethodInfos", param);

		logger.debug(LogUtils.format("评标基准价信息算法JSON", bspmJsons));
		// 如果不存在信息,即第一次进入
		if (CollectionUtils.isEmpty(bspmJsons))
		{
			bspmJsons = SygcUtils.initBSPMData(tpid, tenderProjectNodeID);
		}
		List<Record<String, Object>> bspmInfos = new LinkedList<Record<String, Object>>();
		Record<String, Object> bspmInfo = null;
		JSONObject jobj = null;
		for (String json : bspmJsons)
		{
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "评标基准价计算方法信息为空!");
			}
			jobj = JSON.parseObject(json);
			bspmInfo = new RecordImpl<String, Object>();
			bspmInfo.putAll(jobj);
			bspmInfos.add(bspmInfo);
		}
		logger.debug(LogUtils.format("成功获取评标基准价方法抽取信息", bspmJsons));
		return bspmInfos;
	}

	/**
	 * 
	 * 评标基准价方法JSON数据初始化<br/>
	 * <p>
	 * 评标基准价方法JSON数据初始化
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @return List
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<String> initBSPMData(String tpid,
			String tenderProjectNodeID) throws ServiceException
	{
		logger.debug(LogUtils.format("评标基准价方法JSON数据初始化"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		List<String> bspmJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 查询出所有未流标的标段信息
		DataEntry de = new DataEntryImpl(TableName.EKB_T_SECTION_INFO);
		de.select("ID", "V_BID_SECTION_ID", "V_BID_SECTION_CODE",
				"V_BID_SECTION_GROUP_CODE", "V_BID_SECTION_NAME",
				"N_CONTROL_PRICE").orderBy("V_BID_SECTION_NAME", "ASC");
		de.and().equalTo("V_TPID", tpid)
				.notEqualTo("V_BID_OPEN_STATUS", "10-0")
				.notEqualTo("V_BID_OPEN_STATUS", "10-1")
				.notEqualTo("V_BID_OPEN_STATUS", "10-2")
				.notEqualTo("V_BID_EVALUATION_STATUS", "10");
		List<Record<String, Object>> sections = de.persist().load();
		logger.debug(LogUtils.format("获取未流标的标段信息", sections));
		if (CollectionUtils.isEmpty(sections))
		{
			throw new ServiceException("", "找不到任何的标段信息!");
		}
		param.clear();
		param.setColumn("tpid", tpid);
		Record<String, Object> bspm = null;
		Record<String, Object> record = new RecordImpl<String, Object>();

		// 删除冗余数据,为了防止出现重复
		param.setColumn("flag", ConstantEOKB.BSPM_BUS_FLAG_TYPE);
		getActiveRecordDAO().auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").remove(param);
		param.remove("flag");
		String json = null;
		// 迭代构造标段评标基准价计算方法信息
		for (Record<String, Object> section : sections)
		{
			record.clear();
			bspm = new RecordImpl<String, Object>();
			record.setColumn("ID", Random.generateUUID());
			record.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.BSPM_BUS_FLAG_TYPE);
			record.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			record.setColumn("V_TPID", tpid);
			record.setColumn("V_TPFN_ID", tenderProjectNodeID);
			record.setColumn("V_BUS_ID", section.getString("V_BID_SECTION_ID"));

			// 标段信息
			bspm.setColumn("SECTION_INFO", section);
			bspm.setColumn("METHOD_ONE_ADAPTE", false);
			bspm.setColumn("METHOD_TWO_ADAPTE", false);
			json = JSON.toJSONString(bspm);
			record.setColumn("V_JSON_OBJ", json);
			getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(record);
			bspmJsons.add(json);
		}
		logger.debug(LogUtils.format("完成评标基准价方法JSON数据初始化"));
		return bspmJsons;
	}

	/**
	 * 
	 * 获取最高权重系数视图<br/>
	 * <p>
	 * 获取最高权重系数视图
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @return List
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<Record<String, Object>> getHighestWeightView(
			String tpid, String tenderProjectNodeID) throws ServiceException
	{
		logger.debug(LogUtils.format("最高权重抽取视图"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 查询最高权重JSON
		List<String> hwJsons = getActiveRecordDAO().statement().loadList(
				SQLNAME + ".getHighestWeightInfos", param);
		List<Record<String, Object>> hwInfos = new LinkedList<Record<String, Object>>();
		logger.debug(LogUtils.format("获取最高权重抽的信息", hwInfos));
		// 如果最高权重为空,说明是第一次进入
		if (CollectionUtils.isEmpty(hwJsons))
		{
			hwJsons = initHWData(tpid, tenderProjectNodeID);
		}
		Record<String, Object> hwInfo = null;
		JSONObject jobj = null;
		for (String json : hwJsons)
		{
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "最高权重信息为空!");
			}
			jobj = JSON.parseObject(json);
			hwInfo = new RecordImpl<String, Object>();
			hwInfo.putAll(jobj);

			hwInfos.add(hwInfo);
		}
		logger.debug(LogUtils.format("完成最高权重抽取视图的信息获取", hwInfos));
		return hwInfos;
	}

	/**
	 * 
	 * 初始化最高权重数据<br/>
	 * <p>
	 * 初始化最高权重数据
	 * </p>
	 * 
	 * @param tpid
	 * @param tenderProjectNodeID
	 * @return
	 * @throws ServiceException
	 */
	private static List<String> initHWData(String tpid,
			String tenderProjectNodeID) throws ServiceException
	{
		logger.debug(LogUtils.format("开始初始化最高权重数据", tpid, tenderProjectNodeID));
		List<String> hwJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		// 删除冗余数据,为了防止出现重复
		param.setColumn("flag", ConstantEOKB.HW_BUS_FLAG_TYPE);
		getActiveRecordDAO().auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").remove(param);
		param.remove("flag");

		param.setColumn("flag", ConstantEOKB.BSPM_BUS_FLAG_TYPE);
		// 先查评标基准价信息算法JSON
		List<String> bspmJsons = getActiveRecordDAO().statement().loadList(
				SQLNAME + ".getBidStandardPriceMethodInfos", param);
		logger.debug(LogUtils.format("获取评标基准价信息算法JSON", bspmJsons));
		// 如果不存在信息
		if (CollectionUtils.isEmpty(bspmJsons))
		{
			throw new ServiceException("", "无法获取评标基准价抽取结果!");
		}
		// 如果有数据
		JSONObject jobj = null;
		Record<String, Object> hwInfo = new RecordImpl<String, Object>();
		Record<String, Object> record = null;
		String jsonStr = null;
		int bidders = 0;
		for (String json : bspmJsons)
		{
			param.clear();
			hwInfo.clear();
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "评标基准价计算方法信息为空!");
			}
			jobj = JSON.parseObject(json);

			// 构建下浮信息对象
			record = new RecordImpl<String, Object>();
			hwInfo.setColumn("ID", Random.generateUUID());
			hwInfo.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.HW_BUS_FLAG_TYPE);
			hwInfo.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			hwInfo.setColumn("V_TPID", tpid);
			hwInfo.setColumn("V_TPFN_ID", tenderProjectNodeID);
			hwInfo.setColumn("V_BUS_ID", jobj.getJSONObject("SECTION_INFO")
					.getString("V_BID_SECTION_ID"));
			param.setColumn(
					"V_BID_SECTION_ID",
					jobj.getJSONObject("SECTION_INFO").getString(
							"V_BID_SECTION_ID"));
			param.setColumn("V_TPID", tpid);
			// 标段信息
			record.setColumn("SECTION_INFO", jobj.getJSONObject("SECTION_INFO"));

			Integer ct = jobj.getInteger("YAOHAO_RESULT_METHOD");
			if (null == ct)
			{
				throw new ServiceException("", "无法获取评标基准价计算方法摇号结果!");
			}
			bidders = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "N_ENVELOPE_0=1").count(param);
			// 抽取的方法
			record.setColumn("METHOD", ct);
			// 投标人数
			record.setColumn("BIDDER_NUM", bidders);
			Record<String, Object> cinfo = new RecordImpl<String, Object>();
			List<Record<String, Object>> cl = getWeight(ct);
			cinfo.put("LIST_VALUS", cl);
			record.setColumn("WEIGHT_INFO", cinfo);
			jsonStr = JSON.toJSONString(record);
			hwInfo.setColumn("V_JSON_OBJ", jsonStr);
			getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(hwInfo);
			hwJsons.add(jsonStr);
		}
		logger.debug(LogUtils.format("成功初始化最高权重数据", bspmJsons));
		return hwJsons;
	}

	/**
	 * 获取最高权重列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param type
	 * @return
	 */
	private static List<Record<String, Object>> getWeight(int type)
	{
		List<Record<String, Object>> cl = new LinkedList<Record<String, Object>>();
		int min = 0;
		int max = 0;
		if (2 == type)
		{
			min = 20;
			max = 40;
		}
		// 方法一不用抽取最高权重
		else if (1 == type)
		{
			return cl;
		}

		Record<String, Object> temp = null;
		for (; min <= max;)
		{
			temp = new RecordImpl<String, Object>();
			temp.setColumn("VALUE", min);
			min += 4;
			cl.add(temp);
		}
		return cl;
	}

	/**
	 * 
	 * 下浮系数抽取视图<br/>
	 * <p>
	 * 下浮系数抽取视图
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @return List
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<Record<String, Object>> getLowerCoefficientView(
			String tpid, String tenderProjectNodeID) throws ServiceException
	{
		logger.debug(LogUtils.format("下浮系数抽取视图", tpid, tenderProjectNodeID));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 查询下浮系数JSON
		List<String> lcJsons = getActiveRecordDAO().statement().loadList(
				SQLNAME + ".getLowerCoefficientInfos", param);
		logger.debug(LogUtils.format("成功查询下浮系数JSON", lcJsons));
		List<Record<String, Object>> lcInfos = null;
		// 如果下浮系数为空,说明是第一次进入
		if (CollectionUtils.isEmpty(lcJsons))
		{
			lcJsons = initLCData(tpid, tenderProjectNodeID);
		}
		// 下浮系数不为空
		lcInfos = new LinkedList<Record<String, Object>>();
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
		logger.debug(LogUtils.format("成功获取下浮系数抽取视图", lcInfos));
		return lcInfos;
	}

	/**
	 * 
	 * 初始化下浮系数JSON数据<br/>
	 * <p>
	 * 初始化下浮系数JSON数据
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @return
	 * @throws ServiceException
	 *             服务异常
	 */
	private static List<String> initLCData(String tpid,
			String tenderProjectNodeID) throws ServiceException
	{
		logger.debug(LogUtils.format("开始初始化下浮系数JSON数据", tpid,
				tenderProjectNodeID));
		List<String> lcJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		// 删除冗余数据,为了防止出现重复
		param.setColumn("flag", ConstantEOKB.LC_BUS_FLAG_TYPE);
		getActiveRecordDAO().auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").remove(param);
		param.remove("flag");

		param.setColumn("flag", ConstantEOKB.HW_BUS_FLAG_TYPE);
		// 先查评标基准价信息算法JSON
		List<String> bspmJsons = getActiveRecordDAO().statement().loadList(
				SQLNAME + ".getHighestWeightInfos", param);
		logger.debug(LogUtils.format("先获取评标基准价信息算法JSON", bspmJsons));
		// 如果不存在信息
		if (CollectionUtils.isEmpty(bspmJsons))
		{
			throw new ServiceException("", "无法获取评标基准价抽取结果!");
		}
		// 如果有数据
		JSONObject jobj = null;
		Record<String, Object> lcInfo = new RecordImpl<String, Object>();
		Record<String, Object> record = null;
		String jsonStr = null;
		for (String json : bspmJsons)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			jobj = JSON.parseObject(json);
			param.setColumn("sid", jobj.getJSONObject("SECTION_INFO")
					.getString("V_BID_SECTION_ID"));
			lcInfo.clear();
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "评标基准价计算方法信息为空!");
			}

			// 构建下浮信息对象
			record = new RecordImpl<String, Object>();
			lcInfo.setColumn("ID", Random.generateUUID());
			lcInfo.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.LC_BUS_FLAG_TYPE);
			lcInfo.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			lcInfo.setColumn("V_TPID", tpid);
			lcInfo.setColumn("V_TPFN_ID", tenderProjectNodeID);
			lcInfo.setColumn("V_BUS_ID", jobj.getJSONObject("SECTION_INFO")
					.getString("V_BID_SECTION_ID"));
			// 标段信息
			record.setColumn("SECTION_INFO", jobj.getJSONObject("SECTION_INFO"));
			// 标段最高限价
			record.setColumn("CONTROL_PRICE", jobj
					.getJSONObject("SECTION_INFO")
					.getInteger("N_CONTROL_PRICE"));
			// 投标人数
			record.setColumn("BIDDER_NUM", jobj.getInteger("BIDDER_NUM"));
			// 评标方法
			record.setColumn("METHOD", jobj.getInteger("METHOD"));

			// 下浮系数列表
			List<Record<String, Object>> cl = null;
			// 方法2，有抽取权重和权重号
			if (jobj.getInteger("METHOD") == 2)
			{
				// 权重
				record.setColumn("WEIGHT_VALUE", jobj.getDouble("WEIGHT_VALUE"));
				// 抽取的权重球号
				record.setColumn("WEIGHT", jobj.getInteger("YAOHAO_NO"));
			}
			cl = getCoefficient();

			Record<String, Object> cinfo = new RecordImpl<String, Object>();
			cinfo.put("LIST_VALUS", cl);
			record.setColumn("COEFFICIENT_INFO", cinfo);
			jsonStr = JSON.toJSONString(record);
			lcInfo.setColumn("V_JSON_OBJ", jsonStr);
			getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(lcInfo);
			lcJsons.add(jsonStr);
		}
		logger.debug(LogUtils.format("成功初始化下浮系数JSON数据", bspmJsons));
		return lcJsons;
	}

	/**
	 * 获取下浮系数列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param type
	 * @return
	 */
	private static List<Record<String, Object>> getCoefficient()
	{
		logger.debug(LogUtils.format("获取下浮系数列表"));
		List<Record<String, Object>> cl = new LinkedList<Record<String, Object>>();
		int min = 1;
		int max = 7;
		Record<String, Object> temp = null;
		for (; min <= max;)
		{
			temp = new RecordImpl<String, Object>();
			temp.setColumn("VALUE", "K" + min);
			min += 1;
			cl.add(temp);
		}
		return cl;
	}

	/**
	 * 
	 * E值抽取视图<br/>
	 * <p>
	 * E值抽取视图
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @return List
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<Record<String, Object>> getEvalueView(String tpid,
			String tenderProjectNodeID) throws ServiceException
	{
		logger.debug(LogUtils.format("E值抽取视图", tpid, tenderProjectNodeID));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 查询下浮系数JSON
		List<String> evJsons = getActiveRecordDAO().statement().loadList(
				SQLNAME + ".getEvalueInfos", param);
		logger.debug(LogUtils.format("查询下浮系数JSON", evJsons));
		List<Record<String, Object>> lcInfos = null;
		// 如果E值JSON为空,说明是第一次进入
		if (CollectionUtils.isEmpty(evJsons))
		{
			evJsons = initEVData(tpid, tenderProjectNodeID);
		}
		// 下浮系数不为空
		lcInfos = new LinkedList<Record<String, Object>>();
		Record<String, Object> evInfo = null;
		JSONObject jobj = null;
		for (String json : evJsons)
		{
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "E值信息为空!");
			}
			jobj = JSON.parseObject(json);
			evInfo = new RecordImpl<String, Object>();
			evInfo.putAll(jobj);

			lcInfos.add(evInfo);
		}
		logger.debug(LogUtils.format("成功获取E值抽取视图", lcInfos));
		return lcInfos;
	}

	/**
	 * 
	 * 初始化E值信息<br/>
	 * <p>
	 * 初始化E值信息
	 * </p>
	 * 
	 * @param tpid
	 * @param tenderProjectNodeID
	 * @return
	 * @throws ServiceException
	 */
	private static List<String> initEVData(String tpid,
			String tenderProjectNodeID) throws ServiceException
	{
		logger.debug(LogUtils.format("开始初始化E值抽取视图", tpid, tenderProjectNodeID));
		List<String> evJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		// 删除冗余数据,为了防止出现重复
		param.setColumn("flag", ConstantEOKB.EV_BUS_FLAG_TYPE);
		getActiveRecordDAO().auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").remove(param);
		param.remove("flag");

		param.setColumn("flag", ConstantEOKB.LC_BUS_FLAG_TYPE);
		// 先查下浮系数抽取JSON
		List<String> lcJsons = getActiveRecordDAO().statement().loadList(
				SQLNAME + ".getLowerCoefficientInfos", param);
		logger.debug(LogUtils.format("成功获取下浮系数抽取JSON", lcJsons));
		// 如果不存在信息
		if (CollectionUtils.isEmpty(lcJsons))
		{
			throw new ServiceException("", "无法下浮系数抽取结果!");
		}
		// 如果有数据
		JSONObject jobj = null;
		Record<String, Object> evInfo = new RecordImpl<String, Object>();
		Record<String, Object> record = null;
		String jsonStr = null;
		for (String json : lcJsons)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			jobj = JSON.parseObject(json);
			param.setColumn("sid", jobj.getJSONObject("SECTION_INFO")
					.getString("V_BID_SECTION_ID"));
			evInfo.clear();
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "评标基准价计算方法信息为空!");
			}

			// 构建下浮信息对象
			record = new RecordImpl<String, Object>();
			evInfo.setColumn("ID", Random.generateUUID());
			evInfo.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.EV_BUS_FLAG_TYPE);
			evInfo.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			evInfo.setColumn("V_TPID", tpid);
			evInfo.setColumn("V_TPFN_ID", tenderProjectNodeID);
			evInfo.setColumn("V_BUS_ID", jobj.getJSONObject("SECTION_INFO")
					.getString("V_BID_SECTION_ID"));
			// 标段信息
			record.setColumn("SECTION_INFO", jobj.getJSONObject("SECTION_INFO"));
			// 标段最高限价
			record.setColumn("CONTROL_PRICE", jobj
					.getJSONObject("SECTION_INFO")
					.getInteger("N_CONTROL_PRICE"));
			// 投标人数
			record.setColumn("BIDDER_NUM", jobj.getInteger("BIDDER_NUM"));
			// 评标方法
			record.setColumn("METHOD", jobj.getInteger("METHOD"));

			// 下浮系数列表
			List<Record<String, Object>> ev1 = null;
			List<Record<String, Object>> ev2 = null;
			// 方法2，有抽取权重和权重号
			if (jobj.getInteger("METHOD") == 2)
			{
				// 权重
				record.setColumn("WEIGHT_VALUE", jobj.getDouble("WEIGHT_VALUE"));
				// 抽取的权重球号
				record.setColumn("WEIGHT", jobj.getInteger("WEIGHT"));

			}
			// 抽取的下浮系数
			record.setColumn("COEFFCIENT_K_VALUE",
					jobj.getString("COEFFCIENT_K_VALUE"));
			record.setColumn("COEFFCIENT_VALUE",
					jobj.getString("COEFFCIENT_VALUE"));
			// 抽取的下浮系数球号
			record.setColumn("COEFFCIENT", jobj.getInteger("YAOHAO_NO"));

			// 算数平均值A
			record.setColumn("AVG", jobj.getDouble("AVG"));
			Record<String, Object> cinfo = new RecordImpl<String, Object>();
			// E1列表
			ev1 = getEvalues(1);
			cinfo.setColumn("LIST_VALUS_ONE", ev1);
			// ev1.clear();
			// E2列表
			ev2 = getEvalues(2);
			cinfo.setColumn("LIST_VALUS_TWO", ev2);
			record.setColumn("EVALUE_INFO", cinfo);
			jsonStr = JSON.toJSONString(record);
			evInfo.setColumn("V_JSON_OBJ", jsonStr);
			getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(evInfo);
			evJsons.add(jsonStr);
		}
		logger.debug(LogUtils.format("成功获取下浮系数抽取JSON", lcJsons));
		return evJsons;
	}

	/**
	 * 获取E值列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param type
	 * @return
	 */
	private static List<Record<String, Object>> getEvalues(int type)
	{
		logger.debug(LogUtils.format("获取E值列表", type));
		List<Record<String, Object>> cl = new LinkedList<Record<String, Object>>();
		float min = 0;
		float max = 0;
		if (1 == type)
		{
			min = 1.0f;
			max = 1.6f;
		}
		else if (2 == type)
		{
			min = 0.6f;
			max = 1.1f;
		}
		Record<String, Object> temp = null;
		for (; min <= max;)
		{
			temp = new RecordImpl<String, Object>();
			temp.setColumn("VALUE",
					new StringBuffer(String.valueOf(min)).substring(0, 3));
			min += 0.1;
			cl.add(temp);
		}
		return cl;
	}

	/**
	 * 
	 * E值记录表<br/>
	 * <p>
	 * E值记录表
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID"
	 * @return List
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<Record<String, Object>> getEValueRecord(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("开始E值记录表信息", tpid));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 查询E值系数JSON
		List<String> lcJsons = getActiveRecordDAO().statement().loadList(
				SQLNAME + ".getEvalueInfos", param);
		logger.debug(LogUtils.format("成功获取E值Json", lcJsons));
		List<Record<String, Object>> lcInfos = null;
		// 如果E值JSON为空,说明是第一次进入
		lcInfos = new LinkedList<Record<String, Object>>();
		Record<String, Object> lcInfo = null;
		JSONObject jobj = null;
		List<Record<String, Object>> bidders = null;
		for (String json : lcJsons)
		{
			StringBuilder msg = new StringBuilder();
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "下浮系数信息为空!");
			}
			jobj = JSON.parseObject(json);
			param.clear();
			param.setColumn("tpid", tpid);
			String sectionId = jobj.getJSONObject("SECTION_INFO").getString(
					"V_BID_SECTION_ID");
			param.setColumn("sid", sectionId);

			// 水运除了勘察设计以外需要查询全部投标人信息
			if (StringUtils.equals(
					ConstantEOKB.EOKBBemCode.FJS_SYGC_KCSJ_HLDJF_V1,
					SessionUtils.getTenderProjectTypeCode()))
			{
				bidders = getActiveRecordDAO().auto()
						.table(TableName.EKB_T_TENDER_LIST)
						.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
						.setCondition("AND", "V_TPID=#{tpid}").list(param);
			}
			else
			{
				bidders = getActiveRecordDAO().auto()
						.table(TableName.EKB_T_TENDER_LIST)
						.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
						.setCondition("AND", "V_TPID=#{tpid}")
						.setCondition("AND", "N_ENVELOPE_0=1").list(param);
			}

			for (Record<String, Object> tender : bidders)
			{
				String vjson = tender.getString("V_JSON_OBJ");
				JSONObject vjobj = JSON.parseObject(vjson);
				JSONArray sings = vjobj.getJSONArray("objSing");
				for (int i = 0; i < sings.size(); i++)
				{
					JSONObject sing = sings.getJSONObject(i);
					for (Entry<String, Object> entry : sing.entrySet())
					{
						tender.setColumn(entry.getKey(), entry.getValue());
					}
				}
			}
			lcInfo = new RecordImpl<String, Object>();
			msg.append("元，        方法" + jobj.getString("METHOD"));
			msg.append("， K值 = " + jobj.getString("COEFFCIENT_VALUE"));
			if (StringUtils.equals("2", jobj.getString("METHOD")))
			{
				msg.append("，μ值 = " + jobj.getString("WEIGHT_VALUE"));
			}
			msg.append("，E1 = " + jobj.getString("E_VALUE_1"));
			msg.append("，E2 = " + jobj.getString("E_VALUE_2"));
			jobj.put("BIDDERS", bidders);
			jobj.put("MSG", msg);
			lcInfo.putAll(jobj);

			lcInfos.add(lcInfo);
		}
		logger.debug(LogUtils.format("成功获取E值记录表信息", lcJsons));
		return lcInfos;
	}

	/**
	 * 
	 * 获取水运工程开标记录表信息<br/>
	 * <p>
	 * 获取水运工程开标记录表信息
	 * </p>
	 * 
	 * @param tpid
	 *            项目主键
	 * @return 开标记录表信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getOpenBidRecordForm(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("开始获取水运工程开标记录表信息", tpid));

		Record<String, Object> result = new RecordImpl<String, Object>();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);

		// 查询E值系数JSON
		List<String> evJsons = getActiveRecordDAO().statement().loadList(
				SQLNAME + ".getEvalueInfos", param);
		logger.debug(LogUtils.format("获取E值系数JSON", evJsons));

		List<Record<String, Object>> evInfos = null;
		// 如果E值JSON为空,说明是第一次进入
		evInfos = new LinkedList<Record<String, Object>>();
		Record<String, Object> evInfo = null;
		JSONObject jobj = null;
		List<Record<String, Object>> bidders = null;

		// 投标人信息的json字段
		String vjson = null;
		JSONObject vjobj = null;
		// 投标人唱标信息集
		JSONArray sings = null;
		// 投标人投标人备注
		JSONObject remark = null;
		for (String json : evJsons)
		{
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "下浮系数信息为空!");
			}
			jobj = JSON.parseObject(json);
			param.clear();
			param.setColumn("tpid", tpid);
			String sectionId = jobj.getJSONObject("SECTION_INFO").getString(
					"V_BID_SECTION_ID");
			param.setColumn("sid", sectionId);

			// 水运除了勘察设计以外需要查询全部投标人信息
			if (StringUtils.equals(
					ConstantEOKB.EOKBBemCode.FJS_SYGC_KCSJ_HLDJF_V1,
					SessionUtils.getTenderProjectTypeCode()))
			{
				bidders = getActiveRecordDAO().auto()
						.table(TableName.EKB_T_TENDER_LIST)
						.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
						.setCondition("AND", "V_TPID=#{tpid}").list(param);
			}
			else
			{
				bidders = getActiveRecordDAO().auto()
						.table(TableName.EKB_T_TENDER_LIST)
						.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
						.setCondition("AND", "V_TPID=#{tpid}")
						.setCondition("AND", "N_ENVELOPE_0=1").list(param);
			}

			for (Record<String, Object> tender : bidders)
			{
				vjson = tender.getString("V_JSON_OBJ");
				vjobj = JSON.parseObject(vjson);
				sings = vjobj.getJSONArray("objSing");
				for (int i = 0; i < sings.size(); i++)
				{
					JSONObject sing = sings.getJSONObject(i);
					tender.putAll(sing);
				}
				// 开标记录表中代理填入的备注信息
				remark = vjobj.getJSONObject("remark");
				if (!CollectionUtils.isEmpty(remark))
				{
					// 投标人备注
					tender.setColumn("firstRemark", remark.get("firstRemark"));
				}
			}

			evInfo = new RecordImpl<String, Object>();
			jobj.put("BIDDERS", bidders);
			jobj.put("METHOD", jobj.getString("METHOD"));
			jobj.put("WEIGHT_VALUE", jobj.getString("WEIGHT_VALUE"));
			jobj.put("COEFFCIENT_K_VALUE", jobj.getString("COEFFCIENT_VALUE"));
			jobj.put("E_VALUE_1", jobj.getString("E_VALUE_1"));
			jobj.put("E_VALUE_2", jobj.getString("E_VALUE_2"));
			evInfo.putAll(jobj);

			evInfos.add(evInfo);
		}
		result.put("TENDER_PROJECT_EV_LIST", evInfos);
		logger.debug(LogUtils.format("成功获取水运工程开标记录表信息", evJsons));
		return result;
	}
}
