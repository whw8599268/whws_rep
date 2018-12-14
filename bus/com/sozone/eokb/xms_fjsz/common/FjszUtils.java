/**
 * 包名：com.sozone.eokb.xms_fjsz.common
 * 文件名：FjszUtils.java<br/>
 * 创建时间：2017-12-28 下午5:54:20<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.xms_fjsz.common;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import net.sf.jxls.transformer.XLSTransformer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
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
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.PageImpl;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.download.util.AeolusDownloadUtils;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.ClassLoaderUtils;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.bus.createFile.CreateFileFJSZ;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.Arith;
import com.sozone.eokb.utils.SessionUtils;
import com.strongsoft.encrypt.EncryptTools;

/**
 * 房建市政工具类<br/>
 * <p>
 * 房建市政工具类<br/>
 * </p>
 * Time：2017-12-28 下午5:54:21<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
public final class FjszUtils
{

	/**
	 * 小数点后两位
	 */
	private static final NumberFormat FMT_D = new DecimalFormat("#####0.00",
			new DecimalFormatSymbols());

	/**
	 * 整数
	 */
	private static final NumberFormat FMT_I = new DecimalFormat("#####0",
			new DecimalFormatSymbols());
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FjszUtils.class);

	private static String sqlName = ConstantEOKB.EOKBBemCode.XMS_FJSZ_COMMON;

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
	 * 获取房建市政第一信封解密情况信息<br/>
	 * <p>
	 * 获取房建市政第一信封解密情况信息
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 解密情况信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getFirstEnvelopeDecryptSituation(
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取房建市政第一信封解密情况信息", tpid));

		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		Record<String, Object> result = new RecordImpl<String, Object>();
		// 查询成功家数
		logger.debug(LogUtils.format("开始获取解密成功家数"));
		String sql = "SELECT COUNT(A.V_BID_SECTION_ID) NUM,B.V_BID_SECTION_NAME,A.V_BID_SECTION_ID FROM EKB_T_TENDER_LIST A "
				+ "RIGHT JOIN EKB_T_SECTION_INFO B ON A.V_BID_SECTION_ID = B.V_BID_SECTION_ID AND B.V_BID_OPEN_STATUS NOT LIKE '10%' AND A.V_TPID = B.V_TPID "
				+ "WHERE A.V_TPID=#{tpid} GROUP BY A.V_BID_SECTION_ID ORDER BY B.V_BID_SECTION_NAME";
		List<Record<String, Object>> recordYX = getActiveRecordDAO().sql(sql)
				.setParam("tpid", tpid).list();

		StringBuffer jmMsg = new StringBuffer();
		for (Record<String, Object> record : recordYX)
		{
			jmMsg.append("【标段：" + record.getString("V_BID_SECTION_NAME")
					+ "，共有：" + record.getString("NUM") + "家投标人解密成功】");
			logger.debug(LogUtils.format(
					"标段" + record.getString("V_BID_SECTION_NAME") + "解密成功家数",
					record.getString("NUM")));
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
		logger.debug(LogUtils.format("成功获取所有未流标的标段", sections));
		String vjson = null;
		JSONObject jobj = null;
		JSONArray sings = null;
		JSONObject sing = null;

		// 查出投标人名单
		List<Record<String, Object>> tenderList = null;
		// 迭代标段组
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			tenderList = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);

			for (Record<String, Object> tender : tenderList)
			{
				vjson = tender.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				sings = jobj.getJSONArray("objSing");
				for (int i = 0; i < sings.size(); i++)
				{
					sing = sings.getJSONObject(i);
					tender.putAll(sing);
				}
				if (!StringUtils.isEmpty(jobj.getString("cerditScore")))
				{
					// 季度信用分信息
					tender.setColumn("cerditScore", jobj.get("cerditScore"));
				}
			}
			section.setColumn("TENDER_LIST", tenderList);
		}
		result.put("SECTION_LIST", sections);
		result.put("YX_N", jmMsg.toString());
		logger.debug(LogUtils.format("成功获取获取房建市政_施工第一信封解密情况信息", result));
		return result;
	}

	/**
	 * 
	 * 获取房建市政第一信封解密情况分页信息<br/>
	 * <p>
	 * 获取房建市政第一信封解密情况分页信息
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param page
	 *            页码
	 * @param size
	 *            分页大小
	 * @return Record
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getFirstEnvelopeDecryptPage(
			String tpid, int page, int size) throws ServiceException
	{
		logger.debug(LogUtils.format("获取房建市政第一信封解密情况分页信息", tpid));

		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		Record<String, Object> result = new RecordImpl<String, Object>();
		// 查询成功家数
		logger.debug(LogUtils.format("开始获取解密成功家数"));
		String sql = "SELECT COUNT(A.V_BID_SECTION_ID) NUM,B.V_BID_SECTION_NAME,A.V_BID_SECTION_ID FROM EKB_T_TENDER_LIST A "
				+ "RIGHT JOIN EKB_T_SECTION_INFO B ON A.V_BID_SECTION_ID = B.V_BID_SECTION_ID AND B.V_BID_OPEN_STATUS NOT LIKE '10%' AND A.V_TPID = B.V_TPID "
				+ "WHERE A.V_TPID=#{tpid} GROUP BY A.V_BID_SECTION_ID ORDER BY B.V_BID_SECTION_NAME";
		List<Record<String, Object>> recordYX = getActiveRecordDAO().sql(sql)
				.setParam("tpid", tpid).list();

		StringBuffer jmMsg = new StringBuffer();
		for (Record<String, Object> record : recordYX)
		{
			jmMsg.append("【标段：" + record.getString("V_BID_SECTION_NAME")
					+ "，共有：" + record.getString("NUM") + "家投标人解密成功】");
			logger.debug(LogUtils.format(
					"标段" + record.getString("V_BID_SECTION_NAME") + "解密成功家数",
					record.getString("NUM")));
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
		logger.debug(LogUtils.format("成功获取所有未流标的标段", sections));
		String vjson = null;
		JSONObject jobj = null;
		JSONArray sings = null;
		JSONObject sing = null;

		// 查出投标人名单
		List<Record<String, Object>> tenderList = null;
		// 判断是否是有经过筛选算法
		int count;

		// 分页起始数据下标
		int begin = page * size - size;
		// 筛选标识
		boolean hasAlgorithm = false;

		int total = 0;
		// 迭代标段组
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.put("size", size);
			param.put("begin", begin);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			count = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}").count(param);
			// 有筛选筛选，所以在唱标环节要体现投递时间
			if (count > 451)
			{
				tenderList = getActiveRecordDAO().statement().selectList(
						sqlName + ".getBidderBiddingInfo", param);
				hasAlgorithm = true;
			}
			else
			{
				tenderList = getActiveRecordDAO()
						.auto()
						.table(TableName.EKB_T_TENDER_LIST)
						.setCondition("AND", "V_TPID=#{tpid}")
						.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
						.addSortOrder("V_BIDDER_NO",
								"ASC LIMIT " + begin + "," + size).list(param);
			}
			total = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}").count(param);
			for (Record<String, Object> tender : tenderList)
			{
				vjson = tender.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				sings = jobj.getJSONArray("objSing");
				for (int i = 0; i < sings.size(); i++)
				{
					sing = sings.getJSONObject(i);
					tender.putAll(sing);
				}
				if (!StringUtils.isEmpty(jobj.getString("cerditScore")))
				{
					// 季度信用分信息
					tender.setColumn("cerditScore", jobj.get("cerditScore"));
				}
			}
			section.setColumn("TENDER_LIST", tenderList);
		}
		result.put("SECTION_LIST", sections);
		result.put("YX_N", jmMsg.toString());
		result.put("HAS_ALGORITHM", hasAlgorithm);
		result.put("PAGE", page);
		result.put("SIZE", size);
		result.put("TOTAL", total);
		logger.debug(LogUtils.format("成功获取获取房建市政_施工第一信封解密情况信息", result));
		return result;
	}

	/**
	 * 
	 * 获取房建市政第一信封解密情况信息通用版<br/>
	 * <p>
	 * 获取房建市政第一信封解密情况信息通用版
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return Record
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getFirstEnvelopeDecryptForTyb(
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取房建市政第一信封解密情况信息通用版", tpid));

		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		Record<String, Object> result = new RecordImpl<String, Object>();
		logger.debug(LogUtils.format("开始获取解密成功家数"));
		// 查询成功家数
		String sql = "SELECT COUNT(A.V_BID_SECTION_ID) NUM,B.V_BID_SECTION_NAME,A.V_BID_SECTION_ID FROM EKB_T_TENDER_LIST A "
				+ "RIGHT JOIN EKB_T_SECTION_INFO B ON A.V_BID_SECTION_ID = B.V_BID_SECTION_ID AND B.V_BID_OPEN_STATUS NOT LIKE '10%' "
				+ "WHERE A.V_TPID=#{tpid} AND A.N_ENVELOPE_0=1  GROUP BY A.V_BID_SECTION_ID ORDER BY B.V_BID_SECTION_NAME";
		List<Record<String, Object>> recordYX = getActiveRecordDAO().sql(sql)
				.setParam("tpid", tpid).list();

		StringBuffer jmMsg = new StringBuffer();
		for (Record<String, Object> record : recordYX)
		{
			jmMsg.append("【标段：" + record.getString("V_BID_SECTION_NAME")
					+ "，共有：" + record.getString("NUM") + "家投标人解密成功】");
			logger.debug(LogUtils.format(
					"标段" + record.getString("V_BID_SECTION_NAME") + "解密成功家数",
					record.getString("NUM")));
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

		logger.debug(LogUtils.format("获取所有未流标的标段", sections));

		String vjson = null;
		JSONObject jobj = null;
		JSONArray sings = null;
		JSONObject sing = null;
		// 表标题
		List<String> tableTitle = new LinkedList<String>();
		boolean makeTitle = false;
		boolean havaContent = false;

		List<Record<String, Object>> tenderList = null;
		// 中文正则表达式
		Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
		// 迭代标段组
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.put("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));

			// 查出投标人名单
			tenderList = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			for (Record<String, Object> tender : tenderList)
			{
				// 表内容
				List<String> tableContent = new LinkedList<String>();
				vjson = tender.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				sings = jobj.getJSONArray("objSing");
				for (int i = 0; i < sings.size(); i++)
				{
					havaContent = false;
					sing = sings.getJSONObject(i);
					if (!makeTitle)
					{
						// 添架表的标题
						for (Entry<String, Object> entry : sing.entrySet())
						{
							// 从唱标要素中获取中文的key作为标题集
							if (p.matcher(entry.getKey()).find())
							{
								tableTitle.add(entry.getKey());
							}
						}
					}
					// 去除下标（Index）
					sing.remove("index");
					// 添架表的内容列
					for (Entry<String, Object> entry : sing.entrySet())
					{
						// 从唱标要素中获取英文的value作为表内容集
						if (!p.matcher(entry.getKey()).find())
						{
							tableContent.add(entry.getValue().toString());
							havaContent = true;
						}
					}
					// 若是没有获取到英文的唱标要素，补一个
					if (!havaContent)
					{
						tableContent.add("无");
					}
				}
				makeTitle = true;
				tender.setColumn("TABLECONTENT", tableContent);
			}
			section.setColumn("TENDER_LIST", tenderList);
			section.setColumn("TABLETITLE", tableTitle);
		}
		result.put("SECTION_LIST", sections);
		result.put("YX_N", jmMsg.toString());
		logger.debug(LogUtils.format("成功获取房建市政第一信封解密情况信息通用版", sections));
		return result;
	}

	/**
	 * 
	 * 获取开标记录表视图<br/>
	 * <p>
	 * 获取开标记录表视图
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return Record
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getOpenBidRecordForm(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取房建市政开标记录表信息", tpid));

		Record<String, Object> result = new RecordImpl<String, Object>();
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

		// 标段名称集
		StringBuilder sectionName = new StringBuilder();

		// 投标人信息的json字段
		String vjson = null;
		JSONObject jobj = null;
		// 投标人唱标信息集
		JSONArray sings = null;
		// 投标人投标人备注
		JSONObject remark = null;

		// 投标人的名称与信用分系统的名称是否一致标识
		boolean sameNameFlag = false;
		// 迭代标段组
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

			// 查出投标人名单
			List<Record<String, Object>> tenderList = getActiveRecordDAO()
					.auto().table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
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
				remark = jobj.getJSONObject("remark");
				if (!CollectionUtils.isEmpty(remark))
				{
					// 投标人备注
					tender.setColumn("firstRemark", remark.get("firstRemark"));
				}

				JSONObject cerditJson = jobj.getJSONObject("cerditScore");
				if (!CollectionUtils.isEmpty(cerditJson))
				{
					sameNameFlag = false;
					if (StringUtils.isEmpty(cerditJson
							.getString("V_COMPANY_NAME")))
					{
						tender.setColumn("nameFlag", sameNameFlag);
						tender.setColumn("MSG",
								"评价系统中未获取到" + tender.getString("V_BIDDER_NAME")
										+ "的相关信息！");
						// 季度信用分信息
						tender.setColumn("cerditScore", jobj.get("cerditScore"));
						continue;
					}
					// 季度信用分信息
					tender.setColumn("cerditScore", jobj.get("cerditScore"));
					// 判断从企业信用分中获取的企业信息是否与开标中的系统一致
					if (StringUtils.equals(
							cerditJson.getString("V_COMPANY_NAME"),
							tender.getString("V_BIDDER_NAME")))
					{
						sameNameFlag = true;
					}
					tender.setColumn("nameFlag", sameNameFlag);
					tender.setColumn("MSG", tender.getString("V_BIDDER_NAME")
							+ "与评价系统中获取的信息不一致");
				}
			}
			section.setColumn("TENDER_LIST", tenderList);
		}

		result.put("SECTION_LIST", sections);
		result.put("SECTIONS", sectionName.toString());
		// 项目名称
		result.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		result.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		result.put("YEAR", DateUtils.getYear());
		result.put("MONTH", DateUtils.getMonth());
		result.put("DAY", DateUtils.getDay());

		logger.debug(LogUtils.format("成功获取房建市政开标记录表信息", result));
		return result;
	}

	/**
	 * 
	 * 获取通用版开标记录表视图<br/>
	 * <p>
	 * 获取开标记录表视图
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return Record
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getOpenBidRecordFormForTyb(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取通用版开标记录表视图", tpid));

		Record<String, Object> result = new RecordImpl<String, Object>();
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

		logger.debug(LogUtils.format("成功获取未流标的标段", sections));

		// 标段名称集
		StringBuilder sectionName = new StringBuilder();

		// 投标人信息的json字段
		String vjson = null;
		JSONObject jobj = null;
		// 投标人唱标信息集
		JSONArray sings = null;
		// 投标人投标人备注
		JSONObject remark = null;

		// 表标题
		List<String> tableTitle = new LinkedList<String>();
		boolean makeTitle = false;
		boolean havaContent = false;

		// 中文正则表达式
		Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
		// 迭代标段组
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
			// 查出投标人名单
			List<Record<String, Object>> tenderList = getActiveRecordDAO()
					.auto().table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			for (Record<String, Object> tender : tenderList)
			{
				if (StringUtils.isEmpty(tender.getString("V_JSON_OBJ")))
				{
					throw new ServiceException("", "无法获取到投标人的扩展信息[V_JSON_OBJ]");
				}
				// 表内容
				List<String> tableContent = new LinkedList<String>();
				vjson = tender.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				sings = jobj.getJSONArray("objSing");
				for (int i = 0; i < sings.size(); i++)
				{
					havaContent = false;
					JSONObject sing = sings.getJSONObject(i);
					if (!makeTitle)
					{
						// 添架表的标题
						for (Entry<String, Object> entry : sing.entrySet())
						{
							// 从唱标要素中获取中文的key作为标题集
							if (p.matcher(entry.getKey()).find())
							{
								tableTitle.add(entry.getKey());
							}
						}
					}
					// 去除下标（Index）
					sing.remove("index");
					// 添架表的内容列
					for (Entry<String, Object> entry : sing.entrySet())
					{
						// 从唱标要素中获取英文的value作为表内容集
						if (!p.matcher(entry.getKey()).find())
						{
							tableContent.add(entry.getValue().toString());
							havaContent = true;
						}
					}
					// 若是没有获取到英文的唱标要素，补一个
					if (!havaContent)
					{
						tableContent.add("无");
					}
				}
				makeTitle = true;
				tender.setColumn("TABLECONTENT", tableContent);
				remark = jobj.getJSONObject("remark");
				if (!CollectionUtils.isEmpty(remark))
				{
					// 投标人备注
					tender.setColumn("firstRemark", remark.get("firstRemark"));
				}
			}
			section.setColumn("TENDER_LIST", tenderList);
			section.setColumn("TABLETITLE", tableTitle);
		}
		result.put("SECTION_LIST", sections);
		result.put("SECTIONS", sectionName.toString());

		// 项目名称
		result.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		result.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		logger.debug(LogUtils.format("成功获取通用版开标记录表视图", result));
		return result;
	}

	/**
	 * 
	 * 获取施工抽取方法<br/>
	 * <p>
	 * 获取施工抽取方法
	 * </p>
	 * 
	 * @return 方法
	 * @throws FacadeException
	 *             FacadeException
	 */
	public static int getMethod() throws FacadeException
	{
		logger.debug(LogUtils.format("获取施工抽取方法"));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());
		param.setColumn("type", ConstantEOKB.XMFJSZ_JL_METHOD);
		Record<String, Object> tpDate = getActiveRecordDAO().auto()
				.table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE=#{type}").get(param);

		if (CollectionUtils.isEmpty(tpDate))
		{
			throw new FacadeException("", "无法获取抽取方法");
		}

		return tpDate.getJSONObject("V_JSON_OBJ").getInteger(
				ConstantEOKB.XMFJSZ_JL_METHOD);
	}

	/**
	 * 
	 * <p>
	 * 获取施工和简易抽取方法
	 * </p>
	 * 
	 * @param v_tpid
	 *            项目编码
	 * @param section_id
	 *            标段编码
	 * @param type
	 *            1、经评审A 2、经评审B 3.简易招标办法 4.监理_简易招标办法_V2 5.监理_综合评分法_V2
	 * @return 方法
	 * @throws FacadeException
	 *             FacadeException
	 */
	public static int chouMethod(String v_tpid, String section_id, String type)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取施工抽取方法", v_tpid, section_id, type));
		ActiveRecordDAO dao = getActiveRecordDAO();
		String sql = "SELECT * FROM EKB_T_SECTION_INFO WHERE V_TPID=#{V_TPID} AND V_BID_SECTION_ID=#{V_BID_SECITON_ID}";
		Record<String, Object> sectionRecord = dao.sql(sql)
				.setParam("V_TPID", v_tpid)
				.setParam("V_BID_SECITON_ID", section_id).get();

		sql = "SELECT * FROM EKB_T_TPFN_DATA_INFO T WHERE T.V_BUS_FLAG_TYPE='BENCHMARK' AND T.V_TPID=#{V_TPID} AND T.V_BUS_ID=#{V_BUS_ID}";
		Record<String, Object> dataRecord = dao.sql(sql)
				.setParam("V_TPID", v_tpid).setParam("V_BUS_ID", section_id)
				.get();
		long epSectionStart = 0l;
		long epSectionEnd = 0l;
		if (!CollectionUtils.isEmpty(dataRecord))
		{
			epSectionStart = JSON.parseObject(
					dataRecord.getString("V_JSON_OBJ")).getLong(
					"EPSECTIONSTART");// 基准价最低
			epSectionEnd = JSON.parseObject(dataRecord.getString("V_JSON_OBJ"))
					.getLong("EPSECTIONEND");// 基准价最高
		}
		if (StringUtils.equals("1", type))
		{
			logger.debug(LogUtils.format("经评审A"));
			long controlPrice = sectionRecord.getLong("N_CONTROL_PRICE");// 控制价
			sql = "SELECT * FROM EKB_T_TENDER_LIST WHERE N_PRICE>=#{E_START} AND N_PRICE<#{E_END} AND V_TPID=#{V_TPID} AND V_BID_SECTION_ID=#{V_BID_SECTION_ID}";
			List<Record<String, Object>> tenderRecord = dao.sql(sql)
					.setParam("V_TPID", v_tpid)
					.setParam("V_BID_SECTION_ID", section_id)
					.setParam("E_START", epSectionStart)
					.setParam("E_END", epSectionEnd).list();
			if (tenderRecord.size() <= 50)
			{
				return 1;
			}
			else
			{
				if (controlPrice < 30000000)// 《=0.3亿
				{
					return 2;
				}
				else if (30000000 <= controlPrice && controlPrice < 100000000)// 0.3亿<=控制价<1亿
				{
					return 3;
				}
				else if (controlPrice >= 100000000)// 控制价>=1亿
				{
					return 4;
				}
			}
		}
		else if (StringUtils.equals("2", type))
		{
			logger.debug(LogUtils.format("经评审B"));
			sql = "SELECT * FROM EKB_T_TENDER_LIST WHERE N_PRICE>=#{E_START} AND N_PRICE<#{E_END} AND V_TPID=#{V_TPID} AND V_BID_SECTION_ID=#{V_BID_SECTION_ID}";
			List<Record<String, Object>> tenderRecord = dao.sql(sql)
					.setParam("V_TPID", v_tpid)
					.setParam("V_BID_SECTION_ID", section_id)
					.setParam("E_START", epSectionStart)
					.setParam("E_END", epSectionEnd).list();
			if (tenderRecord.size() <= 50)
			{
				return 1;
			}
			else
			{
				return 2;
			}
		}
		else if (StringUtils.equals("3", type))
		{
			logger.debug(LogUtils.format("简易招标办法"));
			sql = "SELECT * FROM EKB_T_TENDER_LIST WHERE N_ENVELOPE_0=1 AND V_TPID=#{V_TPID} AND V_BID_SECTION_ID=#{V_BID_SECTION_ID}";
			List<Record<String, Object>> tenderRecord = dao.sql(sql)
					.setParam("V_TPID", v_tpid)
					.setParam("V_BID_SECTION_ID", section_id).list();
			if (tenderRecord.size() <= 20)
			{
				return 1;
			}
			else
			{
				return 2;
			}
		}
		else if (StringUtils.equals("4", type))
		{
			logger.debug(LogUtils.format("监理_简易招标办法_V2"));
			sql = "SELECT * FROM EKB_T_TENDER_LIST WHERE N_ENVELOPE_0=1 AND V_TPID=#{V_TPID} AND V_BID_SECTION_ID=#{V_BID_SECTION_ID}";
			List<Record<String, Object>> tenderRecord = dao.sql(sql)
					.setParam("V_TPID", v_tpid)
					.setParam("V_BID_SECTION_ID", section_id).list();
			if (tenderRecord.size() <= 10)
			{
				return 1;
			}
			else
			{
				return 2;
			}
		}

		else if (StringUtils.equals("5", type))
		{
			logger.debug(LogUtils.format("监理_综合评分法_V2"));
			sql = "SELECT * FROM EKB_T_TENDER_LIST WHERE N_ENVELOPE_0=1 AND V_TPID=#{V_TPID} AND V_BID_SECTION_ID=#{V_BID_SECTION_ID}";
			List<Record<String, Object>> tenderRecord = dao.sql(sql)
					.setParam("V_TPID", v_tpid)
					.setParam("V_BID_SECTION_ID", section_id).list();
			// 小于10，无需抽取
			if (tenderRecord.size() <= 10)
			{
				return 1;
			}
			/** 2018.04.18 wengdm 为防止代理控制价发答疑导致选择抽取方法有误，故取消系统判断，改成统一由代理选择抽取方法 */
			// Long control_price = sectionRecord.getLong("N_CONTROL_PRICE");
			// // 控制价没有值，需要代理选择抽取方法
			// if (null == control_price || control_price < 500000)
			// {
			// return 99;
			// }
			//
			// if (500000 <= control_price && control_price < 3000000)//
			// 大于等于50万，小于300万
			// {
			// return 2;
			// }
			// if (control_price >= 3000000)// 大于等于300万
			// {
			// return 3;
			// }
			return 99;

		}
		return 0;
	}

	/**
	 * 
	 * 获取K值<br/>
	 * <p>
	 * 获取K值
	 * </p>
	 * 
	 * @param min
	 *            K最小值
	 * @param max
	 *            K最大值
	 * @param k
	 *            K值
	 * @return K值
	 */
	public static List<Record<String, Object>> chouK(double min, double max,
			String k)
	{
		logger.debug(LogUtils.format("获取K值", min, max, k));
		String maxString = String.valueOf(max);// 最大值转字符
		String minString = String.valueOf(min);// 最小值转字符
		int kLength = k.length();// 抽取K值的长度
		double maxTwo = Double.parseDouble(maxString.substring(0,
				maxString.length() - 1));// 获取两位
		double maxThree = Double.parseDouble(maxString.substring(0,
				maxString.length()));// 获取三位
		double minTwo = Double.parseDouble(minString.substring(0,
				minString.length() - 1));// 获取两位
		double minThree = Double.parseDouble(minString.substring(0,
				minString.length()));// 获取三位
		List<Record<String, Object>> list = new ArrayList<Record<String, Object>>();
		DecimalFormat df = new DecimalFormat("######0.00");
		if (StringUtils.equals(k, ""))// 取整数位
		{
			logger.debug(LogUtils.format("取整数位", k));
			int kValue = 0;
			for (int h = 0; h < max; h++)
			{
				Record<String, Object> r = new RecordImpl<String, Object>();
				if ((int) min <= kValue && kValue <= (int) max)
				{
					if (Double.parseDouble(df.format(kValue)) != max)
					{
						r.setColumn("VALUE", h);
						list.add(r);
					}
				}
				kValue++;
			}
		}
		// K值大于10的时候K值整数位2位
		else if (kLength == 1 || kLength == 2)// 取小数第一位
		{
			logger.debug(LogUtils.format("取小数第一位", k));
			double kValue = Double.parseDouble(k);
			for (int h = 0; h < 10; h++)
			{
				Record<String, Object> r = new RecordImpl<String, Object>();
				if (minTwo <= kValue && kValue <= maxTwo)
				{
					r.setColumn("VALUE", h);
					list.add(r);
				}
				kValue = Arith.add(kValue, 0.1);
			}
		}
		else if (kLength >= 3)// 取小数第二位
		{
			logger.debug(LogUtils.format("取小数第二位", k));
			double kValue = Double.parseDouble(k);
			for (int h = 0; h < 10; h++)
			{

				Record<String, Object> r = new RecordImpl<String, Object>();
				if (minThree <= kValue && kValue < maxThree)
				{
					r.setColumn("VALUE", h);
					list.add(r);
				}
				kValue = Arith.add(kValue, 0.01);
			}
		}
		logger.debug(LogUtils.format("成功获取K值列表", list));
		return list;
	}

	/**
	 * 
	 * 初始化基准价信息<br/>
	 * <p>
	 * 初始化基准价信息
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @return 基准价信息
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static List<String> initBenchmarkData(String tpid,
			String tenderProjectNodeID) throws ServiceException
	{
		logger.debug(LogUtils.format("初始化基准价信息", tpid, tenderProjectNodeID));
		List<String> kJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		logger.debug(LogUtils.format("查询未流标的标段", sections));
		if (CollectionUtils.isEmpty(sections))
		{
			throw new ServiceException("", "查询不到对应的投标项目!");
		}

		// 如果有数据
		Record<String, Object> benchmarkInfo = new RecordImpl<String, Object>();
		String jsonStr = null;
		JSONObject jobj = null;
		JSONObject kJobj = null;
		Double maxK;
		Double minK;

		for (Record<String, Object> section : sections)
		{

			benchmarkInfo.clear();
			param.clear();

			jobj = section.getJSONObject("V_JSON_OBJ");
			maxK = jobj.getDouble("MAX_K");
			minK = jobj.getDouble("MIN_K");
			logger.debug(LogUtils.format("获取K值范围的最大值和最小值", maxK, minK));

			if (CollectionUtils.isEmpty(section))
			{
				throw new ServiceException("", "获取标段扩展信息失败");
			}

			// 删除冗余数据,为了防止出现重复
			param.setColumn("flag", ConstantEOKB.BENCHMARK);
			param.setColumn("tpid", tpid);
			getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}")
					.remove(param);

			param.put("sid", section.getString("V_BID_SECTION_ID"));
			param.put("type", ConstantEOKB.K_VALUE);
			// 查询K值
			Record<String, Object> kValue = getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA)
					.setCondition("AND", "V_BUS_ID=#{sid}")
					.setCondition("AND", "V_BUS_FLAG_TYPE=#{type}").get(param);
			if (CollectionUtils.isEmpty(kValue))
			{
				throw new ServiceException("", "查询不到对应的K值信息!");
			}

			kJobj = JSON.parseObject(kValue.getString("V_JSON_OBJ"));

			benchmarkInfo.setColumn("ID", Random.generateUUID());
			benchmarkInfo.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.BENCHMARK);
			benchmarkInfo.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			benchmarkInfo.setColumn("V_TPID", tpid);
			benchmarkInfo.setColumn("V_TPFN_ID", tenderProjectNodeID);
			benchmarkInfo.setColumn("V_BUS_ID",
					section.getString("V_BID_SECTION_ID"));

			// 初始化基准价json字段信息
			jsonStr = getBenchmarkJsonInfo(maxK, minK, section,
					kJobj.getDouble("RESULT"), tpid);
			benchmarkInfo.setColumn("V_JSON_OBJ", jsonStr);
			getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA)
					.save(benchmarkInfo);
			kJsons.add(jsonStr);
		}
		logger.debug(LogUtils.format("成功初始化基准价信息"));
		return kJsons;
	}

	/**
	 * 
	 * 初始化基准价json字段信息<br/>
	 * <p>
	 * 初始化基准价json信息
	 * </p>
	 * 
	 * @param maxK
	 *            K值最大值
	 * @param minK
	 *            K值最小值
	 * @param section
	 *            标段信息
	 * @param kValue
	 *            抽取的k值
	 * @param tpid
	 *            招标项目id
	 * @return
	 * @throws ServiceException
	 */
	private static String getBenchmarkJsonInfo(double maxK, double minK,
			Record<String, Object> section, double kValue, String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("开始计算评标基准价", kValue));
		// 构建基准价信息对象
		Record<String, Object> record = new RecordImpl<String, Object>();
		// Record<String, Object> prices = null;
		// PushData psData = new PushData();
		// 暂列金额总和
		Double ep1 = 0D;
		// 专业工程暂估价总和
		Double ep2 = 0D;
		// 控制价
		Double controlPrice = section.getDouble("N_CONTROL_PRICE");

		// 从json中获取暂列金额总和，专业工程暂估价总和信息
		String vjson = section.getString("V_JSON_OBJ");
		if (StringUtils.isEmpty(vjson))
		{
			throw new ServiceException("", "无法获取标段的扩展信息");
		}
		JSONObject jobj = JSON.parseObject(vjson);
		ep1 = jobj.getDouble("ZLJE");
		if (ep1 == null)
		{
			throw new ServiceException("", "无法从标段的扩展信息中获取暂列金额");
		}
		ep2 = jobj.getDouble("ZYGCZGJ");
		if (ep2 == null)
		{
			throw new ServiceException("", "无法从标段的扩展信息中获取专业工程暂估价");
		}
		logger.debug(LogUtils.format("获取暂列金额总和，专业工程暂估价总和", ep1, ep2));
		// 获取暂列金额总和，专业工程暂估价总和
		// prices = psData.getPrices();
		// if (CollectionUtils.isEmpty(prices))
		// {
		// throw new ServiceException("", "获取暂列金额总和，专业工程暂估价总和失败");
		// }
		// ep1 = prices.getDouble("ZLJE");
		// ep2 = prices.getDouble("ZYGCZGJ");

		// 标段名称
		record.setColumn("V_BID_SECTION_NAME",
				section.getString("V_BID_SECTION_NAME"));
		// 标段ID
		record.setColumn("V_BID_SECTION_ID",
				section.getString("V_BID_SECTION_ID"));
		// 最高限价
		record.setColumn("N_CONTROL_PRICE",
				section.getDouble("N_CONTROL_PRICE"));

		record.setColumn("MAXK", maxK);
		record.setColumn("MINK", minK);
		record.setColumn("K_VALUE", kValue);

		// 基准价范围最大值
		Double maxEvaluation = 0d;
		// 基准价范围最小值
		Double minEvaluation = 0d;
		// 基准价
		Double ep = calcEvaluationPrice(controlPrice, ep1, ep2, kValue);
		// 将最小K值代入公式计算
		Double epSectionStart = calcEvaluationPrice(controlPrice, ep1, ep2,
				maxK);
		// 将最大K值代入公式计算
		Double epSectionEnd = calcEvaluationPrice(controlPrice, ep1, ep2, minK);
		// 判断是否epSectionStart大于epSectionEnd，确定基准价范围
		if (epSectionStart.compareTo(epSectionEnd) > 0)
		{
			maxEvaluation = epSectionStart;
			minEvaluation = epSectionEnd;
		}
		else
		{
			maxEvaluation = epSectionEnd;
			minEvaluation = epSectionStart;
		}
		logger.debug(LogUtils.format("基准价最小值，基准价最大值，基准价", minEvaluation,
				maxEvaluation, ep));
		record.put("BENCHMARK", FMT_I.format(ep));
		record.put("EP1", FMT_D.format(ep1));
		record.put("EP2", FMT_D.format(ep2));
		record.put("EPSECTIONSTART", FMT_I.format(minEvaluation));
		record.put("EPSECTIONEND", FMT_I.format(maxEvaluation));
		record.put("SCOPE",
				FMT_I.format(minEvaluation) + "~" + FMT_I.format(maxEvaluation));

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		param.put("N_EVALUATION_PRICE", FMT_I.format(ep));
		// 将基准价存入标段表
		param.put("sid", section.getString("V_BID_SECTION_ID"));
		String sql = "UPDATE EKB_T_SECTION_INFO SET N_EVALUATION_PRICE = #{N_EVALUATION_PRICE} WHERE V_TPID=#{tpid} AND V_BID_SECTION_ID = #{sid}";
		getActiveRecordDAO().sql(sql).build(param).update();
		// 获取投标人，价格在基准价范围之内的（闭区间）则预入围
		List<Record<String, Object>> tenderList = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
				.setCondition("AND", "N_ENVELOPE_0=1")
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		// 设置入围名单
		setShortList(minEvaluation, maxEvaluation, tenderList);
		logger.debug(LogUtils.format("评标基准价计算完成"));
		return JSON.toJSONString(record);
	}

	/**
	 * 
	 * 设置入围名单<br/>
	 * <p>
	 * 设置入围名单
	 * </p>
	 * 
	 * @param minEvaluation
	 * @param maxEvaluation
	 * @param tenderList
	 * @throws ServiceException
	 */
	private static void setShortList(double minEvaluation,
			double maxEvaluation, List<Record<String, Object>> tenderList)
			throws ServiceException
	{
		logger.debug(LogUtils.format("设置入围名单"));
		// 循环投标人入围和未入围
		for (Record<String, Object> tender : tenderList)
		{
			double price = tender.getDouble("N_PRICE");
			if (price >= minEvaluation && price <= maxEvaluation)
			{
				tender.setColumn("N_ENVELOPE_9", "0");// 预入围
			}
			else
			{
				tender.setColumn("N_ENVELOPE_9", "-1");
			}
			getActiveRecordDAO().auto().table(TableName.EKB_T_TENDER_LIST)
					.modify(tender);
		}
	}

	/**
	 * 计算评标基准价<br/>
	 * <p>
	 * 计算评标基准价
	 * </p>
	 * 
	 * @param controlPrice
	 *            控制价
	 * @param ep1
	 *            暂列金额
	 * @param ep2
	 *            专业工程暂估价
	 * @param k
	 *            K值
	 * @return 评标基准价
	 */
	public static Double calcEvaluationPrice(Double controlPrice, Double ep1,
			Double ep2, Double k)
	{
		logger.debug(LogUtils.format("计算评标基准价"));
		// 评标基准价计算公式：(B-暂列金额-专业工程暂估价)×(1-K)+暂列金额+专业工程暂估价
		double evaluationOne = 0d;
		double evaluationTwo = 0d;
		double evaluationThree = 0d;
		evaluationOne = Arith.sub(controlPrice, ep1);
		evaluationOne = Arith.sub(evaluationOne, ep2);
		// evaluation_one = Arith.sub(evaluation_one, ep3);
		evaluationTwo = Arith.mul(k, 0.01);
		evaluationTwo = Arith.sub(Double.parseDouble(String.valueOf(1)),
				evaluationTwo);
		evaluationThree = Arith.add(ep1, ep2);
		// evaluation_three = Arith.add(evaluation_three, ep3);
		Double price = (evaluationOne * evaluationTwo) + evaluationThree;
		/*
		 * Double price = (( controlPrice - ep1 - ep2 - ep3) * (1 - k * 0.01)) +
		 * ep1 + ep2 + ep3;
		 */
		// 评标基准价计算取值范围的上、下限和评标基准价均取整数（以“元”为单位，小数点后第一位“四舍五入”，第二位及以后不计）。
		return round(price, 0);

	}

	/**
	 * 提供精确的小数位四舍五入处理。
	 * 
	 * @param v
	 *            需要四舍五入的数字
	 * @param scale
	 *            小数点后保留几位
	 * @return 四舍五入后的结果
	 */
	public static Double round(Double v, Integer scale)
	{
		if (scale < 0)
		{
			throw new IllegalArgumentException(
					"The scale must be a positive integer or zero");
		}
		if (v == null || v.isNaN())
		{
			return 0.0;
		}
		BigDecimal b = new BigDecimal(Double.toString(v));
		BigDecimal one = new BigDecimal("1");
		return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	/**
	 * 
	 * 初始化K值整数位<br/>
	 * <p>
	 * 初始化K值整数位
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param maxK
	 *            K最大值
	 * @param minK
	 *            k最小值
	 * @return K值整数位信息
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static List<String> initDataK1(String tpid,
			String tenderProjectNodeID, double maxK, double minK)
			throws ServiceException
	{
		logger.debug(LogUtils.format("初始化K值整数位"));
		List<String> kJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		logger.debug(LogUtils.format("获取未流标的标段", sections));
		if (CollectionUtils.isEmpty(sections))
		{
			throw new ServiceException("", "查询不到对应的投标项目!");
		}
		// 如果有数据
		Record<String, Object> kInfo = new RecordImpl<String, Object>();
		Record<String, Object> record = null;
		String jsonStr = null;
		for (Record<String, Object> section : sections)
		{
			param.clear();
			kInfo.clear();

			// 构建K值信息对象
			record = new RecordImpl<String, Object>();
			kInfo.setColumn("ID", Random.generateUUID());
			kInfo.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.K_VALUE_1);
			kInfo.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			kInfo.setColumn("V_TPID", tpid);
			kInfo.setColumn("V_TPFN_ID", tenderProjectNodeID);
			kInfo.setColumn("V_BUS_ID", section.getString("V_BID_SECTION_ID"));
			Record<String, Object> kinfo = new RecordImpl<String, Object>();
			List<Record<String, Object>> cl = FjszUtils.chouK(minK, maxK, "");
			kinfo.put("K_VALUES", cl);
			record.setColumn("K_INFO", kinfo);
			record.setColumn("V_BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));
			record.setColumn("V_BID_SECTION_ID",
					section.getString("V_BID_SECTION_ID"));
			record.setColumn("K_LENGTH", cl.size());
			jsonStr = JSON.toJSONString(record);
			kInfo.setColumn("V_JSON_OBJ", jsonStr);
			getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(kInfo);
			kJsons.add(jsonStr);
		}
		logger.debug(LogUtils.format("完成初始化K值整数位"));
		return kJsons;
	}

	/**
	 * 
	 * 初始化K值小数点后第一位<br/>
	 * <p>
	 * 初始化K值小数点后第一位
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param maxK
	 *            K最大值
	 * @param minK
	 *            k最小值
	 * @return K值小数点后第一位信息
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static List<String> initDataK2(String tpid,
			String tenderProjectNodeID, double maxK, double minK)
			throws ServiceException
	{
		logger.debug(LogUtils.format("初始化K值小数点第一位"));
		List<String> kJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);

		param.setColumn("type", ConstantEOKB.K_VALUE_1);

		// 查询k值JSON
		List<String> kValueJsons = getActiveRecordDAO().statement().loadList(
				sqlName + ".getKvalue", param);
		logger.debug(LogUtils.format("获取K值整数位的信息", kValueJsons));
		// 如果有数据
		Record<String, Object> kInfo = new RecordImpl<String, Object>();
		Record<String, Object> record = null;
		String jsonStr = null;
		JSONObject jobj = null;
		for (String json : kValueJsons)
		{
			kInfo.clear();
			jobj = JSON.parseObject(json);

			// 构建K值信息对象
			record = new RecordImpl<String, Object>();
			kInfo.setColumn("ID", Random.generateUUID());
			kInfo.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.K_VALUE_2);
			kInfo.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			kInfo.setColumn("V_TPID", tpid);
			kInfo.setColumn("V_TPFN_ID", tenderProjectNodeID);
			kInfo.setColumn("V_BUS_ID", jobj.getString("V_BID_SECTION_ID"));
			Record<String, Object> kinfo = new RecordImpl<String, Object>();
			List<Record<String, Object>> cl = FjszUtils.chouK(minK, maxK,
					jobj.getString(ConstantEOKB.K_VALUE));
			kinfo.put("K_VALUES", cl);
			record.setColumn("K_INFO", kinfo);
			record.setColumn("V_BID_SECTION_NAME",
					jobj.getString("V_BID_SECTION_NAME"));
			record.setColumn("V_BID_SECTION_ID",
					jobj.getString("V_BID_SECTION_ID"));
			record.setColumn("K_LENGTH", cl.size());
			record.setColumn("K1", jobj.getString(ConstantEOKB.K_VALUE));
			record.setColumn("K1_NO", jobj.getString("YAOHAO_NO"));
			jsonStr = JSON.toJSONString(record);
			kInfo.setColumn("V_JSON_OBJ", jsonStr);
			getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(kInfo);
			kJsons.add(jsonStr);
		}
		logger.debug(LogUtils.format("成功初始化K值小数点第一位"));
		return kJsons;
	}

	/**
	 * 
	 * 初始化K值小数点后第二位<br/>
	 * <p>
	 * 初始化K值小数点后第二位
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param maxK
	 *            K最大值
	 * @param minK
	 *            k最小值
	 * @return K值小数点后第二位信息
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static List<String> initDataK3(String tpid,
			String tenderProjectNodeID, double maxK, double minK)
			throws ServiceException
	{
		logger.debug(LogUtils.format("初始化K值小数点第二位"));
		List<String> kJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);

		param.setColumn("type", ConstantEOKB.K_VALUE_2);

		// 查询k值JSON
		List<String> kValueJsons = getActiveRecordDAO().statement().loadList(
				sqlName + ".getKvalue", param);
		logger.debug(LogUtils.format("获取K值第一位的JSON信息", kValueJsons));
		// 如果有数据
		Record<String, Object> kInfo = new RecordImpl<String, Object>();
		Record<String, Object> record = null;
		String jsonStr = null;
		JSONObject jobj = null;
		for (String json : kValueJsons)
		{
			kInfo.clear();
			jobj = JSON.parseObject(json);

			// 构建K值信息对象
			record = new RecordImpl<String, Object>();
			kInfo.setColumn("ID", Random.generateUUID());
			kInfo.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.K_VALUE_3);
			kInfo.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			kInfo.setColumn("V_TPID", tpid);
			kInfo.setColumn("V_TPFN_ID", tenderProjectNodeID);
			kInfo.setColumn("V_BUS_ID", jobj.getString("V_BID_SECTION_ID"));
			Record<String, Object> kinfo = new RecordImpl<String, Object>();
			List<Record<String, Object>> cl = FjszUtils.chouK(
					minK,
					maxK,
					jobj.getString("K1") + "."
							+ jobj.getString(ConstantEOKB.K_VALUE));
			kinfo.put("K_VALUES", cl);
			record.setColumn("K_INFO", kinfo);
			record.setColumn("K1", jobj.getString("K1"));
			record.setColumn("K1_NO", jobj.getString("K1_NO"));
			record.setColumn("K2", jobj.getString(ConstantEOKB.K_VALUE));
			record.setColumn("K2_NO", jobj.getString("YAOHAO_NO"));
			record.setColumn("V_BID_SECTION_NAME",
					jobj.getString("V_BID_SECTION_NAME"));
			record.setColumn("V_BID_SECTION_ID",
					jobj.getString("V_BID_SECTION_ID"));
			// k范围长度
			record.setColumn("K_LENGTH", cl.size());
			jsonStr = JSON.toJSONString(record);
			kInfo.setColumn("V_JSON_OBJ", jsonStr);
			getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(kInfo);
			kJsons.add(jsonStr);
		}
		logger.debug(LogUtils.format("成功初始化K值小数点第二位"));
		return kJsons;
	}

	/**
	 * 
	 * 初始化K值抽取结果<br/>
	 * <p>
	 * 初始化K值抽取结果
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param maxK
	 *            K最大值
	 * @param minK
	 *            k最小值
	 * @return K值抽取结果
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<String> initDataK4(String tpid,
			String tenderProjectNodeID, double maxK, double minK)
			throws ServiceException
	{
		logger.debug(LogUtils.format("初始化K值抽取结果"));
		List<String> kJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);

		param.setColumn("type", ConstantEOKB.K_VALUE_3);

		// 查询k值小数点后两位JSON
		List<String> kValueJsons = getActiveRecordDAO().statement().loadList(
				sqlName + ".getKvalue", param);
		logger.debug(LogUtils.format("查询k值小数点后两位JSON", kValueJsons));
		// 如果有数据
		Record<String, Object> kInfo = new RecordImpl<String, Object>();
		Record<String, Object> record = null;
		String jsonStr = null;
		JSONObject jobj = null;
		for (String json : kValueJsons)
		{
			kInfo.clear();
			jobj = JSON.parseObject(json);

			// 构建K值信息对象
			record = new RecordImpl<String, Object>();
			kInfo.setColumn("ID", Random.generateUUID());
			kInfo.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.K_VALUE);
			kInfo.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			kInfo.setColumn("V_TPID", tpid);
			kInfo.setColumn("V_TPFN_ID", tenderProjectNodeID);
			kInfo.setColumn("V_BUS_ID", jobj.getString("V_BID_SECTION_ID"));

			// k整数位
			record.setColumn("K1", jobj.getString("K1"));
			record.setColumn("K1_NO", jobj.getString("K1_NO"));
			// K小数后一位
			record.setColumn("K2", jobj.getString("K2"));
			record.setColumn("K2_NO", jobj.getString("K2_NO"));
			// K小数后两位
			record.setColumn("K3", jobj.getString(ConstantEOKB.K_VALUE));
			record.setColumn("K3_NO", jobj.getString("YAOHAO_NO"));
			// K值最后结果
			record.setColumn(
					"RESULT",
					jobj.getString("K1") + "." + jobj.getString("K2")
							+ jobj.getString(ConstantEOKB.K_VALUE));
			// 标段信息
			record.setColumn("V_BID_SECTION_NAME",
					jobj.getString("V_BID_SECTION_NAME"));
			record.setColumn("V_BID_SECTION_ID",
					jobj.getString("V_BID_SECTION_ID"));

			jsonStr = JSON.toJSONString(record);
			kInfo.setColumn("V_JSON_OBJ", jsonStr);
			getActiveRecordDAO().auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(kInfo);
			kJsons.add(jsonStr);
		}
		logger.debug(LogUtils.format("成功初始化K值抽取结果"));
		return kJsons;
	}

	/**
	 * 
	 * 获取K值得视图<br/>
	 * <p>
	 * 获取K值得视图
	 * </p>
	 * 
	 * @param kJsons
	 *            K值信息集
	 * @return K值信息集
	 * @throws ServiceException
	 *             服务异常
	 */
	public static List<Record<String, Object>> getKValueView(List<String> kJsons)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取K值得视图"));
		List<Record<String, Object>> kInfos = new LinkedList<Record<String, Object>>();
		Record<String, Object> hwInfo = null;
		JSONObject jobj = null;
		for (String json : kJsons)
		{
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "K值信息为空!");
			}
			jobj = JSON.parseObject(json);
			hwInfo = new RecordImpl<String, Object>();
			hwInfo.putAll(jobj);

			kInfos.add(hwInfo);
		}
		return kInfos;
	}

	/**
	 * 
	 * 下载投标人名单<br/>
	 * <p>
	 * 下载投标人名单
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static void createExclAssets(String tpid, AeolusData data)
			throws ServiceException
	{
		logger.debug(LogUtils.format("下载投标人名单", tpid, data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		// 获取投标人
		List<Record<String, Object>> bidders = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid}")
				// .setCondition("AND", "N_ENVELOPE_0=1")
				.addSortOrder("V_BIDDER_NO", "ASC").list(param);

		String fileName = "投标人名单";
		// 输出文件名
		String outFileName = fileName + ".xls";

		HttpServletResponse response = data.getHttpServletResponse();
		String mimeType = AeolusDownloadUtils.getMimeType(outFileName);
		response.setContentType(mimeType);

		response.setHeader("Content-Disposition", "attachment;filename="
				+ AeolusDownloadUtils.encodeFileName(data, outFileName));

		InputStream input = null;
		OutputStream out = null;

		param.setColumn("bidders", bidders);

		try
		{
			input = ClassLoaderUtils
					.getResourceAsStream(
							"/com/sozone/eokb/xms_fjsz/common/bidder_info_template.xls",
							FjszUtils.class);
			XLSTransformer transformer = new XLSTransformer();
			transformer.groupCollection("department.staff");
			Workbook resultWorkbook = transformer.transformXLS(input, param);
			// 获取输出流
			out = response.getOutputStream();
			resultWorkbook.write(out);
			out.flush();
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("导出投标人信息列表发生异常!"), e);
			throw new ServiceException("", "导出投标人信息列表发生异常!", e);
		}
		finally
		{
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(out);
		}
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
	 * @throws ServiceException
	 *             服务异常
	 */
	public static void saveBidderRemark(AeolusData data, String remarks,
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("保存投标人备注信息", remarks, tpid));
		JSONObject jobj = null;
		Record<String, Object> bidder = null;
		Record<String, Object> param = new RecordImpl<String, Object>();
		JSONObject jsonObj = null;
		// 投标人备注
		JSONObject remark = new JSONObject();
		JSONArray arr = JSON.parseArray(remarks);

		String id = null;

		for (int i = 0; i < arr.size(); i++)
		{
			jobj = arr.getJSONObject(i);
			for (Entry<String, Object> entry : jobj.entrySet())
			{
				param.clear();
				// 获取投标人ID
				id = entry.getKey();
				param.setColumn("ID", id);
				bidder = getActiveRecordDAO().auto()
						.table(ConstantEOKB.TableName.EKB_T_TENDER_LIST)
						.get(param);
				if (!CollectionUtils.isEmpty(bidder))
				{
					jsonObj = JSONObject.parseObject(bidder
							.getString("V_JSON_OBJ"));

					remark.clear();
					remark.put("firstRemark", entry.getValue().toString());
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

		// 代理保存后将数据插入节点状态表
		Record<String, Object> tpData = new RecordImpl<String, Object>();
		tpData.setColumn("ID", Random.generateUUID());
		tpData.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.FIRST_REMARK);
		tpData.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
				.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		tpData.setColumn("V_TPID", tpid);
		getActiveRecordDAO().auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.save(tpData);

		logger.debug(LogUtils.format("重新生成开标记录表doc"));
		// 重新生成开标记录表doc
		CreateFileFJSZ fjsz = new CreateFileFJSZ();
		fjsz.createKFirstRecordDoc(data, tpid,
				SessionUtils.getTenderProjectTypeCode());
		logger.debug(LogUtils.format("成功生成开标记录表doc"));
	}

	/**
	 * 
	 * 只获取投标函路径<br/>
	 * <p>
	 * 只获取投标函路径
	 * </p>
	 * 
	 * @param jarray
	 *            投标函信息
	 * @return 投标函路径
	 */
	public static JSONObject getTenderDocuments(JSONArray jarray)
	{
		if (null == jarray || jarray.isEmpty())
		{
			return null;
		}
		JSONObject obj = null;
		JSONArray children = null;
		for (int i = 0; i < jarray.size(); i++)
		{
			obj = jarray.getJSONObject(i);
			if (StringUtils.equals("投标函及投标函附录", obj.getString("NAME")))
			{
				return obj;
			}
			// 查找下级
			children = obj.getJSONArray("CHILDREN");
			if (null == children || children.isEmpty())
			{
				continue;
			}
			obj = getTenderDocuments(children);
			if (null != obj)
			{
				return obj;
			}
		}
		return null;
	}

	/**
	 * 
	 * 获取投标报价等分<br/>
	 * <p>
	 * 函数的详细描述
	 * </p>
	 * 
	 * @param evaluationPrice
	 *            基准价
	 * @param perPrice
	 *            投标报价
	 * @return 投标报价得分
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static double getPriceScore(double evaluationPrice, double perPrice)
			throws ServiceException
	{
		DecimalFormat df = new DecimalFormat("######0.000");
		Double score = 0d;// 如果投标基准价异常无法计算 偏差率100%
		Record<String, Object> section = getActiveRecordDAO()
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.get(new RecordImpl<String, Object>().setColumn("tpid",
						SessionUtils.getTPID()));
		if (CollectionUtils.isEmpty(section))
		{
			throw new ServiceException("", "获取标段扩展信息失败");
		}
		JSONObject jobj = section.getJSONObject("V_JSON_OBJ");

		Double q1 = jobj.getDouble("Q1");
		Double q2 = jobj.getDouble("Q2");

		if (null == q1 || null == q2)
		{
			throw new ServiceException("", "无法获取到Q1和Q2值");
		}

		// 当合格投标人的投标报价≤评标基准价时，Q的取值为
		if (perPrice <= evaluationPrice)
		{
			score = 100
					- (Math.abs(perPrice - evaluationPrice) / evaluationPrice)
					* 100 * q1;
		}
		// 当合格投标人的投标报价>评标基准价时
		else if (perPrice > evaluationPrice)
		{
			score = 100
					- (Math.abs(perPrice - evaluationPrice) / evaluationPrice)
					* 100 * q2;
		}

		// 取前三位，不四舍五入
		df.setMaximumFractionDigits(3);
		df.setGroupingSize(0);
		df.setRoundingMode(RoundingMode.FLOOR);

		BigDecimal b = new BigDecimal(df.format(score));
		// 保留两位小数
		double f1 = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		return f1;
	}

	/**
	 * 
	 * 获取前N名企业<br/>
	 * <p>
	 * 若第N家有多家信用分相同的，同时进入抽取
	 * </p>
	 * 
	 * @param page
	 *            分页信息
	 * @param limit
	 *            第N名
	 * @param pageable
	 *            分页参数
	 * @return 前N名企业分页
	 * @throws FacadeException
	 *             FacadeException
	 */
	public static Page<Record<String, Object>> getTheTopBidders(
			Page<Record<String, Object>> page, int limit, Pageable pageable)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取前N名企业", page, limit));
		// 先判断是否需要筛选信用分前n名的投标人
		if (page.getContent().size() <= 0 || page.getContent().size() <= limit)
		{
			return page;
		}
		List<Record<String, Object>> bidders = page.getContent();
		// 前n名的投标人列表若第N家有多家信用分相同的，同时进入抽取
		List<Record<String, Object>> topBidders = new LinkedList<Record<String, Object>>();
		Record<String, Object> tempBidder = null;
		String credit = null;
		// 构建新的投标人名单
		for (int i = 0; i < bidders.size(); i++)
		{
			tempBidder = bidders.get(i);

			// 第N名都投标人企业信用分
			if (i == (limit - 1))
			{
				credit = FMT_D.format(tempBidder.getDouble("N_CREDITSCORE"));
			}

			// 前N-1家
			if (i < limit)
			{
				topBidders.add(tempBidder);
				continue;
			}

			// 超过第N家，需要判断信用分是否和第N家的信用分一致，若一致，则进入抽取，如不一致，取消循环
			if (StringUtils.equals(credit,
					FMT_D.format(tempBidder.getDouble("N_CREDITSCORE"))))
			{
				topBidders.add(tempBidder);
				continue;
			}
			break;
		}

		page = new PageImpl<Record<String, Object>>(topBidders, pageable,
				topBidders.size());
		return page;
	}

	/**
	 * 
	 * 解析工程量清单<br/>
	 * <p>
	 * 解析工程量清单
	 * </p>
	 * 
	 * @param xmlPath
	 *            工程量清单路径
	 * @param bidder
	 *            投标人
	 * @param jmsxlhs
	 *            加密锁序列号列表
	 * @param cpuxlhs
	 *            cpu地址列表
	 * @param wkdzs
	 *            网卡地址列表
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static void parseXml(String xmlPath, Record<String, Object> bidder,
			List<String> jmsxlhs, List<String> cpuxlhs, List<String> wkdzs)
			throws ServiceException
	{
		logger.debug(LogUtils.format("解析工程量清单", bidder, xmlPath));
		// 判断文件是否存在
		File xml = new File(xmlPath);
		if (!xml.isFile())
		{
			logger.error(LogUtils.format("获取工程量清单失败", bidder, xmlPath));
			return;
		}
		try
		{
			SAXReader reader = new SAXReader();
			Document dom = reader.read(xml);
			Element root = dom.getRootElement();

			Element xtxx = root.element("XTXX");
			Element pyjxx = xtxx.element("RYJXX");

			String log = pyjxx.asXML();
			// 被加密内容
			log = log.substring(log.indexOf(">") + 1, log.lastIndexOf("<"));

			// 密文
			Attribute xxmw = pyjxx.attribute("XXMW");
			String sign = xxmw.getText();

			// 比对密文是否一致 1:一致，0：失败，-3密钥错误
			int result = EncryptTools.getInstance().SignVerify(log, sign,
					"93b198d6bf484e199614b14cc64d5b34");
			boolean encryptFlag = true;
			if (1 != result)
			{
				encryptFlag = false;
			}
			bidder.setColumn("ENCRYPT_FLAG", encryptFlag);

			// 获取最后一条记录的硬件信息
			List<Element> es = pyjxx.elements();
			if (null == es)
			{
				bidder.setColumn("JMSXLH", "");
				bidder.setColumn("CPUXLH", "");
				bidder.setColumn("WKDZ", "");
				return;
			}

			Element e = es.get(es.size() - 1);

			// 加密锁序列号
			Attribute attr = e.attribute("JMSXLH");
			if (null == attr)
			{
				bidder.setColumn("JMSXLH", "");
			}
			String jmsxlh = attr.getText();
			bidder.setColumn("JMSXLH", jmsxlh);
			if (!StringUtils.isBlank(jmsxlh))
			{
				jmsxlhs.add(jmsxlh);
			}
			// CPU序列号
			attr = e.attribute("CPUXLH");
			if (null == attr)
			{
				bidder.setColumn("CPUXLH", "");
			}
			String cpuxlh = attr.getText();
			bidder.setColumn("CPUXLH", cpuxlh);
			if (!StringUtils.isBlank(cpuxlh))
			{
				cpuxlhs.add(cpuxlh);
			}
			// 网卡MAC地址
			attr = e.attribute("WKDZ");
			if (null == attr)
			{
				bidder.setColumn("WKDZ", "");
			}
			String wkdz = attr.getText();
			bidder.setColumn("WKDZ", wkdz);
			if (!StringUtils.isBlank(wkdz))
			{
				wkdzs.add(wkdz);
			}
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("解析xml失败", e.getMessage()));
		}
	}

	/**
	 * 
	 * 解析海迈投标人报价导则里的硬件信息<br/>
	 * <p>
	 * 解析海迈投标人报价导则里的硬件信息
	 * </p>
	 * 
	 * @param xmlPath
	 *            xml文件路径
	 * @param bidder
	 *            投标人
	 * @throws ServiceException
	 *             服务异常
	 */
	public static void parseXml(String xmlPath, Record<String, Object> bidder)
			throws ServiceException
	{
		logger.debug(LogUtils.format("解析海迈投标人报价导则里的硬件信息", bidder));
		// 判断文件是否存在
		File xml = new File(xmlPath);
		if (!xml.isFile())
		{
			logger.error(LogUtils.format("获取工程量清单失败", bidder, xmlPath));
			return;
		}
		try
		{
			SAXReader reader = new SAXReader();
			Document dom = reader.read(xml);
			Element root = dom.getRootElement();

			Element xtxx = root.element("XTXX");
			Element pyjxx = xtxx.element("RYJXX");

			bidder.setColumn("VALUATION_XML",
					null == pyjxx ? "" : pyjxx.asXML());
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("解析xml失败", e.getMessage()));
		}
	}

	/**
	 * 
	 * 筛选出重复数据<br/>
	 * <p>
	 * 筛选出重复数据
	 * </p>
	 * 
	 * @param list
	 *            需要筛选的数据集
	 * 
	 * @return 重复数据的集
	 */
	public static List<String> disjunction(List<String> list)
	{
		logger.debug(LogUtils.format("筛选出重复数据", list));

		Map<String, Integer> hs = new HashMap<String, Integer>();
		Integer count;
		for (String str : list)
		{
			count = 1;
			if (hs.get(str) != null)
			{
				count = hs.get(str) + 1;
			}
			hs.put(str, count);
		}
		list.clear();
		for (String key : hs.keySet())
		{
			if (hs.get(key) != null & hs.get(key) > 1)
			{
				list.add(key);
			}
		}

		return list;
	}

	/**
	 * 
	 * 获取是否需要筛选标识<br/>
	 * <p>
	 * 由于施工经评审AB有可能超过450家需要进行筛选摇号，先判断家数，如果小于450家直接下一步，如果大于450家且未筛选，则进行
	 * </p>
	 * 
	 * @param isExtractingTwo
	 *            是否是房建市政两次抽取
	 * @return 是否需要筛选标识
	 * @throws FacadeException
	 *             FacadeException
	 */
	public static int getCanSieveBcFlag(boolean isExtractingTwo)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取是否需要筛选标识"));

		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 判断合格投标人名单人数（简易，经评审B，经评审A方法二）
		Table table = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid} AND N_ENVELOPE_9 = 0");
		return table.count(param);
	}
}
