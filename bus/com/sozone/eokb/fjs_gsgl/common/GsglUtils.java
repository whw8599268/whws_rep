/**
 * 包名：com.sozone.eokb.fjs_gsgl.common
 * 文件名：GsglUtils.java<br/>
 * 创建时间：2018-1-2 下午6:28:13<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_gsgl.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.bus.benchmark.BenchmarkUtils;
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 高速公路通用工具类<br/>
 * <p>
 * 高速公路通用工具类<br/>
 * </p>
 * Time：2018-1-2 下午6:28:13<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
public final class GsglUtils
{
	/**
	 * 字典表名
	 */
	public static final String T_SYS_DICT = "T_SYS_DICT";

	/**
	 * 信用等级标识
	 */
	public static final String DYXF_CREDIT = ConstantEOKB.EOKBFlowCode.DYXF_CREDIT;

	/**
	 * 电子摇号标识
	 */
	public static final String DYXF_ELECTRONICS = ConstantEOKB.EOKBFlowCode.DYXF_ELECTRONICS;

	/**
	 * 开标结果标识
	 */
	public static final String DYXF_OFFER = ConstantEOKB.EOKBFlowCode.DYXF_OFFER;

	/**
	 * 第一信封
	 */
	public static final String FIRST_ENVELOPE = ConstantEOKB.EOKBFlowCode.FIRST_ENVELOPE;
	/**
	 * 第一信封开标结束标识
	 */
	public static final String FIRST_OVER = ConstantEOKB.EOKBFlowCode.FIRST_OVER;
	/**
	 * 开标完毕标识
	 */
	public static final String BID_OVER = ConstantEOKB.EOKBFlowCode.BID_OVER;
	/**
	 * 第二信封数据库xml
	 */
	private static final String SQLNAME = ConstantEOKB.EOKBBemCode.FJS_GSGL_COMMON;

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(GsglUtils.class);

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
	 * 开始获取第一信封解密情况<br/>
	 * <p>
	 * 开始获取第一信封解密情况
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param alreadyLaunched
	 *            发起确认标识
	 * @param modelType
	 *            模块
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getFirstEnvelopeDecryptSituation(
			String tpid, boolean alreadyLaunched, String modelType)
			throws ServiceException
	{
		logger.debug(LogUtils.format("开始获取第一信封解密情况"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> record = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.list(param);

		List<Record<String, Object>> tenders = null;

		// 迭代标段组
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("modelType", modelType);
			param.setColumn("groupId",
					section.getString("V_BID_SECTION_GROUP_CODE"));

			// 标段编号
			section.setColumn("BID_SECTION_GROUP_CODE",
					section.getString("V_BID_SECTION_GROUP_CODE"));

			tenders = getActiveRecordDAO().statement().selectList(
					SQLNAME + ".getDecryptInfoAndConfirmByGroup", param);

			logger.debug(LogUtils.format("获取投标人列表", tenders));

			// 查出解密表投标人名单
			section.setColumn(
					"TENDER_LIST",
					parseBidderAssociatedEnt(analyzeBidderExtendedInfo(tenders)));

			// 设置是否已经发起确认
			section.setColumn("ALREADY_LAUNCHED", alreadyLaunched);

		}
		record.put("SECTION_LIST", sections);
		logger.debug(LogUtils.format("获取第一信封解密情况成功", record));

		// 2018-03-01 高速公路：开标记录表里面的表格都要加上表头及监管、招标人、招标代理
		// 项目名称
		record.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		record.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));

		return record;
	}

	/**
	 * 
	 * 获取第一信封解密成功家数<br/>
	 * <p>
	 * 获取第一信封解密成功家数
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getDecryptSuccessCount(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取第一信封解密成功家数"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> record = new RecordImpl<String, Object>();
		String sql = "SELECT V_BIDDER_NAME FROM EKB_T_DECRYPT_INFO A WHERE A.V_TPID=#{tpid} AND A.N_ENVELOPE_0=1 GROUP BY V_BIDDER_NAME";
		List<Record<String, Object>> recordYX = getActiveRecordDAO().sql(sql)
				.setParam("tpid", tpid).list();
		sql = "SELECT V_ORG_CODE FROM EKB_T_TBIMPORTBIDDING A WHERE A.V_TPID=#{tpid} GROUP BY V_ORG_CODE";
		List<Record<String, Object>> recordAX = getActiveRecordDAO().sql(sql)
				.setParam("tpid", tpid).list();

		// 已解密的家数
		record.put("YX_N", recordYX.size());
		// 未完成解密的家数 = 全部投标人的家数-已解密的家数
		record.put("WX_N", recordAX.size() - recordYX.size());
		logger.debug(LogUtils.format("获取第一信封解密成功家数完成"));
		return record;
	}

	/**
	 * 
	 * 获取第一信封解密成功家数（无标段组）<br/>
	 * <p>
	 * 获取第一信封解密成功家数（无标段组）
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getDecryptSuccessCountNoGroup(
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取第一信封解密成功家数"));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		// 查询成功家数
		String sql = "SELECT COUNT(A.V_BID_SECTION_ID) NUM,B.V_BID_SECTION_NAME,A.V_BID_SECTION_ID FROM EKB_T_TENDER_LIST A "
				+ "RIGHT JOIN EKB_T_SECTION_INFO B ON A.V_BID_SECTION_ID = B.V_BID_SECTION_ID AND B.V_BID_OPEN_STATUS NOT LIKE '10%' "
				+ "WHERE A.V_TPID=#{tpid} AND A.N_ENVELOPE_0=1  GROUP BY A.V_BID_SECTION_ID ORDER BY B.V_BID_SECTION_NAME";
		List<Record<String, Object>> recordYX = getActiveRecordDAO().sql(sql)
				.setParam("tpid", tpid).list();

		// 查询全部家数
		sql = "SELECT COUNT(A.V_BID_SECTION_ID) NUM,A.V_BID_SECTION_ID FROM EKB_T_TBIMPORTBIDDING A  "
				+ "WHERE A.V_TPID=#{tpid} AND V_FTID='111' GROUP BY A.V_BID_SECTION_ID";
		List<Record<String, Object>> recordAX = getActiveRecordDAO().sql(sql)
				.setParam("tpid", tpid).list();

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
		result.put("YX_N", jmMsg);
		logger.debug(LogUtils.format("获取第一信封解密成功家数完成"));
		return result;
	}

	/**
	 * 
	 * 关联企业合并<br/>
	 * <p>
	 * </p>
	 * 
	 * @param bidders
	 *            企业列表
	 * @return 合并后的企业列表
	 */
	public static List<Record<String, Object>> parseBidderAssociatedEnt(
			List<Record<String, Object>> bidders)
	{
		logger.debug(LogUtils.format("关联企业合并开始"));
		Map<String, Record<String, Object>> nameMap = new HashMap<String, Record<String, Object>>();
		Record<String, Object> firstRecord = null;
		// 关联企业代码
		String conrrelateCode = null;
		for (Record<String, Object> bidder : bidders)
		{
			conrrelateCode = bidder.getString("V_CORRELATE_CODE");
			// 如果关联企业编号为空
			if (StringUtils.isEmpty(conrrelateCode))
			{
				bidder.setColumn("_COUNT", 1);
				continue;
			}
			// 关联企业编号对应的第一行数据
			firstRecord = nameMap.get(conrrelateCode);
			if (null == firstRecord)
			{
				firstRecord = bidder;
				firstRecord.setColumn("_COUNT", 1);
				nameMap.put(conrrelateCode, firstRecord);
				continue;
			}
			firstRecord.setColumn("_COUNT",
					firstRecord.getInteger("_COUNT") + 1);
		}
		logger.debug(LogUtils.format("关联企业合并完成"));

		return bidders;
	}

	/**
	 * 
	 * 修改投标人信用等级<br/>
	 * <p>
	 * 修改投标人信用等级
	 * </p>
	 * 
	 * @param levels
	 *            (A,B,C,D,E)
	 * @param tpid
	 *            招标项目ID
	 * @param bidderCreditRatings
	 *            投标人信用等级信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static void modifyBidderCreditRating(
			List<Record<String, Object>> levels, String tpid,
			String bidderCreditRatings) throws ServiceException
	{
		logger.debug(LogUtils.format("修改投标人信用等级", tpid));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		String tableName = null;
		if (SessionUtils.isSectionGroup())
		{
			tableName = TableName.EKB_T_DECRYPT_INFO;
		}
		else
		{
			tableName = TableName.EKB_T_TENDER_LIST;
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		JSONObject ratingsJsonObj = JSON.parseObject(bidderCreditRatings);
		String[] keys = null;
		List<Record<String, Object>> bidders = null;
		for (String key : ratingsJsonObj.keySet())
		{
			param.clear();
			keys = key.split("\\+");
			param.setColumn("tpid", tpid);
			param.setColumn("BIDDER_NO", keys[0]);
			param.setColumn("BID_SECTION_GROUP_CODE", keys[1]);

			// 同一个标段组下有多个投标人，这里需要对每一个投标人的信用等级修改
			bidders = getActiveRecordDAO()
					.auto()
					.table(tableName)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BIDDER_NO = #{BIDDER_NO}")
					.setCondition("AND",
							"V_BID_SECTION_GROUP_CODE = #{BID_SECTION_GROUP_CODE}")
					.list(param);
			if (!CollectionUtils.isEmpty(bidders))
			{
				modifyBidderJsonInfo(bidders,
						ratingsJsonObj.getJSONObject(key), levels, true);
			}
		}
		logger.debug(LogUtils.format("修改投标人信用等级成功", tpid));

	}

	/**
	 * 
	 * 修改投标人信用等级无标段组<br/>
	 * <p>
	 * 合理低价法需要修改投标人编号，综合评估法不需要
	 * </p>
	 * 
	 * @param levels
	 *            (A,B,C,D,E)
	 * @param tpid
	 *            招标项目ID
	 * @param bidderCreditRatings
	 *            投标人信用等级信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static void modifyBidderCreditRatingNoGroup(
			List<Record<String, Object>> levels, String tpid,
			String bidderCreditRatings) throws ServiceException
	{
		logger.debug(LogUtils.format("修改投标人信用等级无标段组", tpid));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		// 修改投标人编号标识
		boolean modifyBidderNo = true;

		// 投标人等级集为空，则说明不用修改编号
		if (CollectionUtils.isEmpty(levels))
		{
			modifyBidderNo = false;
		}

		Record<String, Object> param = new RecordImpl<String, Object>();
		JSONObject ratingsJsonObj = JSON.parseObject(bidderCreditRatings);
		List<Record<String, Object>> bidders = null;
		String[] keys = null;
		for (String key : ratingsJsonObj.keySet())
		{
			param.clear();
			keys = key.split("\\+");
			param.setColumn("tpid", tpid);
			param.setColumn("BIDDER_NO", keys[0]);
			param.setColumn("BID_SECTION_CODE", keys[1]);
			bidders = getActiveRecordDAO()
					.auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BIDDER_NO = #{BIDDER_NO}")
					.setCondition("AND",
							"V_BID_SECTION_CODE = #{BID_SECTION_CODE}")
					.list(param);
			if (!CollectionUtils.isEmpty(bidders))
			{
				modifyBidderJsonInfo(bidders,
						ratingsJsonObj.getJSONObject(key), levels,
						modifyBidderNo);
			}
		}
	}

	/**
	 * 
	 * 修改投标扩展信息的等级和投标人编号<br/>
	 * <p>
	 * 修改投标扩展信息的等级和投标人编号
	 * </p>
	 * 
	 * @param bidders
	 *            投标人列表
	 * @param jobj
	 *            信用等级信息集
	 * @param levels
	 *            (A,B,C,D,E)
	 * @param modifyBidderNo
	 *            是否修改投标人编号标识
	 * @throws ServiceException
	 */
	private static void modifyBidderJsonInfo(
			List<Record<String, Object>> bidders, JSONObject jobj,
			List<Record<String, Object>> levels, boolean modifyBidderNo)
			throws ServiceException
	{

		logger.debug(LogUtils.format("修改投标扩展信息的等级和投标人编号"));
		// 投标人扩展信息
		JSONObject jsonV = null;
		JSONArray jsonSing = null;

		// 唱标内容
		JSONObject cb = null;

		// 信用等级
		String key = "";
		String tbRatingsInEvl = null;

		// 投标人编号
		String bidderNo = null;
		Record<String, Object> temp = new RecordImpl<String, Object>();

		for (Record<String, Object> bidder : bidders)
		{

			temp.clear();
			bidderNo = bidder.getString("V_BIDDER_NO");
			JSONObject jsV = new JSONObject();
			if (StringUtils.isNotEmpty(bidder.getString("V_JSON_OBJ")))
			{
				jsonV = JSONObject.parseObject(bidder.getString("V_JSON_OBJ"));
				if (StringUtils.isNotEmpty(jsonV.getString("objSing")))
				{
					jsonSing = JSONArray.parseArray(jsonV.getString("objSing"));

					// 循环匹配要获取的唱标信息
					for (int j = 0; j < jsonSing.size(); j++)
					{
						cb = (JSONObject) jsonSing.get(j);
						for (Entry<String, Object> entry : cb.entrySet())
						{
							if (StringUtils.isNotEmpty(jobj.getString(entry
									.getKey())))
							{
								cb.put(entry.getKey(),
										jobj.getString(entry.getKey()));
							}
						}
					}
					jsV.put("objSing", jsonSing);
					bidder.setColumn("V_JSON_OBJ", jsV.toJSONString());
					// 需要修改投标人编号
					if (modifyBidderNo)
					{
						key = "";
						tbRatingsInEvl = jobj.getString("tbRatingsInEvl");
						// 信用等级影响投标人编号
						for (Record<String, Object> level : levels)
						{
							if (StringUtils.equals(tbRatingsInEvl,
									level.getString("DICT_VALUE")))
							{
								key = level.getString("ORDER_KEY");
								break;
							}
						}
						temp.setColumn(
								"V_BIDDER_NO",
								key
										+ bidderNo.substring(
												bidderNo.indexOf("-"),
												bidderNo.length()));
					}
				}
			}
			temp.setColumn("ID", bidder.getString("ID"));
			temp.setColumn("V_JSON_OBJ", jsV.toJSONString());
			String tableName = null;
			if (SessionUtils.isSectionGroup())
			{
				tableName = TableName.EKB_T_DECRYPT_INFO;
			}
			else
			{
				tableName = TableName.EKB_T_TENDER_LIST;
			}
			getActiveRecordDAO().auto().table(tableName).modify(temp);
			temp.setColumn("N_UPDATE_TIME", System.currentTimeMillis());
			temp.setColumn("MID", Random.generateUUID());
			getActiveRecordDAO().auto()
					.table(ConstantEOKB.TableName.EKB_T_DECRYPT_INFO_LOG)
					.save(temp);
		}
		logger.debug(LogUtils.format("修改投标扩展信息的等级和投标人编号完成"));
	}

	/**
	 * 
	 * 获取第一信封电子摇号视图<br/>
	 * <p>
	 * 获取第一信封电子摇号视图
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param alreadyLaunched
	 *            发起确认标识
	 * @param modelType
	 *            模块标识
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getFirstElectronics(String tpid,
			boolean alreadyLaunched, String modelType) throws ServiceException
	{
		logger.debug(LogUtils.format("获取第一信封电子摇号视图", tpid));
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
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.list(param);

		// 迭代标段组
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("modelType", modelType);
			param.setColumn("groupId",
					section.getString("V_BID_SECTION_GROUP_CODE"));

			// 标段编号
			section.setColumn("BID_SECTION_GROUP_CODE",
					section.getString("V_BID_SECTION_GROUP_CODE"));

			// 查出投标人名单
			List<Record<String, Object>> tenders = getActiveRecordDAO()
					.statement().selectList(
							SQLNAME + ".getBidderInfoAndConfirmByGroup", param);

			logger.debug(LogUtils.format("查出投标人名单", tenders));

			section.setColumn("TENDER_LIST",
					parseBidderList(analyzeBidderExtendedInfo(tenders)));

			// 设置是否已经发起确认
			section.setColumn("ALREADY_LAUNCHED", alreadyLaunched);
		}

		logger.debug(LogUtils.format("获取第一信封电子摇号视图完成", sections));

		result.put("SECTION_LIST", sections);

		// 2018-03-01 高速公路：开标记录表里面的表格都要加上表头及监管、招标人、招标代理
		// 项目名称
		result.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		result.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));

		return result;
	}

	/**
	 * 
	 * 标段合并<br/>
	 * <p>
	 * </p>
	 * 
	 * @param bidders
	 * @return
	 */
	private static List<Record<String, Object>> parseBidderList(
			List<Record<String, Object>> bidders)
	{
		logger.debug(LogUtils.format("开始合并标段", bidders));
		Map<String, Record<String, Object>> nameMap = new HashMap<String, Record<String, Object>>();
		Record<String, Object> firstRecord = null;
		for (Record<String, Object> bidder : bidders)
		{
			// 标段名称对应的第一行数据
			firstRecord = nameMap.get(bidder.getString("V_BID_SECTION_NAME"));
			if (null == firstRecord)
			{
				firstRecord = bidder;
				firstRecord.setColumn("_COUNT", 1);
				nameMap.put(bidder.getString("V_BID_SECTION_NAME"), firstRecord);
				continue;
			}
			firstRecord.setColumn("_COUNT",
					firstRecord.getInteger("_COUNT") + 1);
		}
		logger.debug(LogUtils.format("完成合并标段", bidders));
		return bidders;
	}

	/**
	 * 
	 * 获取第一信封开标结果有标段组视图<br/>
	 * <p>
	 * 获取第一信封开标结果有标段组视图
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param modelType
	 *            模块类型
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getBidResultViewForGroup(String tpid,
			String modelType) throws ServiceException
	{
		logger.debug(LogUtils.format("获取第一信封开标结果有标段组视图", tpid));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		List<Record<String, Object>> tenders = null;
		param.put("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);

		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("modelType", modelType);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));

			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			// 查出投标人名单
			tenders = getActiveRecordDAO().statement().selectList(
					SQLNAME + ".getBidderInfoAndConfirm", param);
			logger.debug(LogUtils.format("获取投标人列表", tenders));

			section.setColumn("TENDER_LIST", analyzeBidderExtendedInfo(tenders));
		}
		logger.debug(LogUtils.format("获取第一信封开标结果有标段组视图完成", sections));
		result.put("SECTION_LIST", sections);

		// 2018-03-01 高速公路：开标记录表里面的表格都要加上表头及监管、招标人、招标代理
		// 项目名称
		result.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		result.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		return result;
	}

	/**
	 * 
	 * 获取第一信封开标结果有标段组视图<br/>
	 * <p>
	 * 获取第一信封开标结果有标段组视图
	 * </p>
	 * 
	 * @param alreadyLaunched
	 *            确认标识
	 * @param tpid
	 *            招标项目主键
	 * @param modelType
	 *            模块
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getBidResultView(
			boolean alreadyLaunched, String tpid, String modelType)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取第一信封开标结果无标段组视图", tpid));
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		List<Record<String, Object>> tenders = null;
		param.put("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("modelType", modelType);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));

			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			// 查出投标人名单
			tenders = getActiveRecordDAO().statement().selectList(
					SQLNAME + ".getBidderInfoAndConfirm", param);

			logger.debug(LogUtils.format("获取投标人列表", tenders));

			section.setColumn("TENDER_LIST", analyzeBidderExtendedInfo(tenders));
			// 设置是否已经发起确认
			section.setColumn("ALREADY_LAUNCHED", alreadyLaunched);

		}
		logger.debug(LogUtils.format("获取第一信封开标结果无标段组视图结束", sections));
		result.put("SECTION_LIST", sections);

		// 2018-03-01 高速公路：开标记录表里面的表格都要加上表头及监管、招标人、招标代理
		// 项目名称
		result.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		result.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		return result;
	}

	/**
	 * 获取第一信封评审结果视图<br/>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getfirstReviewView(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取第一信封评审结果视图", tpid));
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
		logger.debug(LogUtils.format("获取第一信封评审结果视图标段信息", sections));
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
					.selectList(SQLNAME + ".getReviewResult", param));
		}
		result.setColumn("SECTION_LIST", sections);
		logger.debug(LogUtils.format("获取第一信封评审结果视图完成", sections));

		// 2018-03-01 高速公路：开标记录表里面的表格都要加上表头及监管、招标人、招标代理
		// 项目名称
		result.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		result.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		return result;
	}

	/**
	 * 第二信封文件解密结果</br>
	 * 
	 * @param alreadyLaunched
	 *            确认标识
	 * @param tpid
	 *            招标项目主键
	 * @param modelType
	 *            模块类型
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getSecondDecryptSituation(
			boolean alreadyLaunched, String tpid, String modelType)
			throws ServiceException
	{
		logger.debug(LogUtils.format("第二信封文件解密结国开始获取视图", tpid));
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
		logger.debug(LogUtils.format("获取标段信息集", sections));

		List<Record<String, Object>> bidders = null;

		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("modelType", modelType);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));

			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			// 查出投标人名单
			bidders = getActiveRecordDAO().statement().selectList(
					SQLNAME + ".getSecBidderInfoAndConfirm", param);
			for (Record<String, Object> bidder : bidders)
			{
				bidder.setColumn(
						"tbbj_CN",
						BidderElementParseUtils.getSingObjAttribute(
								bidder.getString("V_JSON_OBJ"), "tbbj_CN"));
				// 第一信封评审得分
				bidder.setColumn("N_TOTAL", bidder.getJSONObject("V_JSON_OBJ")
						.getDouble("N_TOTAL"));
			}

			section.setColumn("TENDER_LIST", bidders);

			// 设置是否已经发起确认
			section.setColumn("ALREADY_LAUNCHED", alreadyLaunched);

		}
		result.put("SECTION_LIST", sections);
		logger.debug(LogUtils.format("第二信封文件解密结果视图获取完成", sections));

		// 2018-03-01 高速公路：开标记录表里面的表格都要加上表头及监管、招标人、招标代理
		// 项目名称
		result.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		result.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		return result;
	}

	/**
	 * 
	 * 评标基准价视图<br/>
	 * <p>
	 * 评标基准价视图
	 * </p>
	 * 
	 * @param alreadyLaunched
	 *            确认标识
	 * @param tpid
	 *            招标项目主键
	 * @param modelType
	 *            模块类型
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getBenchmarkView(
			boolean alreadyLaunched, String tpid, String modelType)
			throws ServiceException
	{
		logger.debug(LogUtils.format("评标基准价视图", tpid));
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
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("modelType", modelType);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));
			// 查出投标人名单
			logger.debug(LogUtils.format("获取投标人列表"));
			section.setColumn("TENDER_LIST", getActiveRecordDAO().statement()
					.selectList(SQLNAME + ".getSecBidderInfoAndConfirm", param));
			// 查询计算评标基准价的方法以及下浮系数,只要查出一条既可
			logger.debug(LogUtils.format("获取评标基准价信息"));
			section.setColumn(
					"JZJ_INFO",
					getActiveRecordDAO()
							.auto()
							.table(TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION)
							.setCondition("AND", "V_TPID = #{tpid}")
							.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
							.get(param));
			// 设置是否已经发起确认
			section.setColumn("ALREADY_LAUNCHED", alreadyLaunched);
		}
		result.setColumn("SECTION_LIST", sections);
		logger.debug(LogUtils.format("获取评标基准价视图完成", sections));
		// 2018-03-01 高速公路：开标记录表里面的表格都要加上表头及监管、招标人、招标代理
		// 项目名称
		result.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		result.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		return result;
	}

	/**
	 * 评标基准价记录表视图（记录表）</br>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param modelType
	 *            模块类型
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getBenchmarkRecordView(String tpid,
			String modelType) throws ServiceException
	{
		logger.debug(LogUtils.format("开始获取评标基准价视图（记录表）", tpid));
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
		// 迭代标段
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("modelType", "DEXF_price");
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));
			// 查出投标人名单
			section.setColumn("TENDER_LIST", getActiveRecordDAO().statement()
					.selectList(SQLNAME + ".getSecBidderInfoAndConfirm", param));

			BenchmarkUtils.setBenchmarkInfo(section, tpid,
					section.getString("V_BID_SECTION_ID"));

		}
		result.setColumn("SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功获取评标基准价视图（记录表）", sections));

		// 2018-03-01 高速公路：开标记录表里面的表格都要加上表头及监管、招标人、招标代理
		// 项目名称
		result.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		result.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
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

		// 扩展信息
		JSONObject extObject = null;
		// 唱标信息列表
		JSONArray sings = null;
		// 单个唱标信息
		JSONObject singObject = null;
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
			}
		}
		return tenders;
	}

	/**
	 * 
	 * 综合评估法第一信封解密情况<br/>
	 * <p>
	 * 综合评估法第一信封解密情况
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param alreadyLaunched
	 *            确认标识
	 * @param modelType
	 *            模块类型
	 * @return Record
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> getfirstDecryptSituationForZhpgf(
			String tpid, boolean alreadyLaunched, String modelType)
			throws ServiceException
	{
		logger.debug(LogUtils.format("综合评估法第一信封解密情况", tpid));
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
		// 迭代标段组
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("modelType", modelType);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));

			// 标段编号
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			// 查出投标人名单
			List<Record<String, Object>> tenderList = getActiveRecordDAO()
					.statement().selectList(SQLNAME + ".getfirBidderInfo",
							param);

			section.setColumn("TENDER_LIST",
					analyzeBidderExtendedInfo(tenderList));

			// 设置是否已经发起确认
			section.setColumn("ALREADY_LAUNCHED", alreadyLaunched);

		}
		result.put("SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功获取综合评估法第一信封解密情况", sections));

		// 2018-03-01 高速公路：开标记录表里面的表格都要加上表头及监管、招标人、招标代理
		// 项目名称
		result.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		result.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		return result;
	}

	/**
	 * 
	 * 获取非标段组的关联企业信息<br/>
	 * <p>
	 * 获取非标段组的关联企业信息
	 * </p>
	 * 
	 * @return 获取非标段组的关联企业信息
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static List<Record<String, Object>> getCorrelateModel()
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取非标段组的关联企业信息"));
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.put("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS != '10-0'")
				.setCondition("AND", "V_BID_OPEN_STATUS != '10-1'")
				.addSortOrder("V_BID_SECTION_NAME", "ASC").list(param);
		List<Record<String, Object>> tenders = null;

		// 迭代标段组
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("sid", section.getString("V_BID_SECTION_ID"));
			// 标段编号
			section.setColumn("BID_SECTION_GROUP_CODE",
					section.getString("V_BID_SECTION_GROUP_CODE"));

			tenders = getActiveRecordDAO().auto()
					.table(ConstantEOKB.TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID= #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			logger.debug(LogUtils.format("投标人列表", tenders));

			// 查出解密表投标人名单
			section.setColumn("TENDER_LIST", tenders);
		}

		return sections;
	}

	/**
	 * 
	 * 获取标段组的关联企业信息<br/>
	 * <p>
	 * 获取标段组的关联企业信息
	 * </p>
	 * 
	 * @return 获取标段组的关联企业信息
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static List<Record<String, Object>> getCorrelateModelGroup()
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取标段组的关联企业信息"));
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();

		param.put("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS != '10-0'")
				.setCondition("AND", "V_BID_OPEN_STATUS != '10-1'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.addSortOrder("V_BID_SECTION_GROUP_CODE", "ASC").list(param);
		List<Record<String, Object>> tenders = null;

		// 迭代标段组
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("gid", section.getString("V_BID_SECTION_GROUP_CODE"));

			tenders = getActiveRecordDAO().auto()
					.table(ConstantEOKB.TableName.EKB_T_DECRYPT_INFO)
					.setCondition("AND", "V_TPID= #{tpid}")
					.setCondition("AND", "V_BID_SECTION_GROUP_CODE=#{gid}")
					.setCondition("GROUP BY", "V_BIDDER_ORG_CODE")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			logger.debug(LogUtils.format("投标人列表", tenders));

			// 查出解密表投标人名单
			section.setColumn("TENDER_LIST", tenders);
		}
		return sections;
	}
}
