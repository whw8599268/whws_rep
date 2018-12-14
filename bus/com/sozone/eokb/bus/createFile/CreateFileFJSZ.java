/**
 * 包名：com.sozone.eokb.bus.createFile
 * 文件名：CreateFileSYGC.java<br/>
 * 创建时间：2017-10-20 下午2:06:55<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.createFile;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.beetl.core.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.aeolus.utils.FileUtils;
import com.sozone.aeolus.view.BeetlView;
import com.sozone.eokb.bus.hardware.HardWareAction;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.ListSortUtils;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.xms_fjsz.common.FjszUtils;

/**
 * 房建市政生成doc服务接口<br/>
 * <p>
 * 房建市政生成doc服务接口<br/>
 * </p>
 * Time：2017-10-20 下午2:06:55<br/>
 * 
 * @author wengdm
 * @version 1.0.0m
 * @since 1.0.0
 */
@Path(value = "/fjsz/bus/createFile", desc = "创建文件")
// 登录即可访问
@Permission(Level.Authenticated)
public class CreateFileFJSZ extends BaseAction
{

	private static final NumberFormat FMT_D = new DecimalFormat("###,##0.00",
			new DecimalFormatSymbols());

	private static final NumberFormat FMT = new DecimalFormat("#####0.00",
			new DecimalFormatSymbols());
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(CreateFileFJSZ.class);

	private static String sqlName = ConstantEOKB.EOKBBemCode.XMS_FWJZ_SG_JPSDZDTBJZBF_A_V1
			+ "_prebid";

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
	 * 创建doc文件<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return
	 * @throws Exception
	 *             Exception
	 */
	// 定义路径
	@Path(value = "/doc", desc = "创建DOC文件")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void buildDocFile(AeolusData data) throws Exception
	{
		logger.debug(LogUtils.format("创建DOC文件", data));
		String template = data.getParam("template");
		String type = data.getParam("modelType");
		String tpid = SessionUtils.getTPID();
		// k值记录表
		if (StringUtils.equals(type, ConstantEOKB.K_VALUE))
		{
			createKvalueRecordDoc(data, tpid);
			return;
		}
		// 同安小项目球号录入记录表
		if (StringUtils.equals(type, "bidderNo"))
		{
			createBidderNoRecordDoc(data, tpid, template);
			return;
		}
		// 硬件信息记录表
		if (StringUtils.equals(type, "HARDWARE_INFO"))
		{
			createhardwareInfoRecordDoc(data, tpid);
			return;
		}
		// 入围投标人名单确定表(基准价绝对值排序)
		if (StringUtils.contains(type, "PRE_ABSOLUTE"))
		{
			getPreTenderAbsoluteRecordDoc(data, tpid, template, type);
			return;
		}
		// 进入评审名单确定表(基准价绝对值排序)
		if (StringUtils.equals(type, "REVIEW_ABSOLUTE"))
		{
			getReviewAbsoluteRecordDoc(data, tpid, template);
			return;
		}
		// 入围投标人名单确定表(报价文件得分排序)
		if (StringUtils.contains(type, "PRE_SCORE"))
		{
			getPreTenderScoreRecordDoc(data, tpid, template, type);
			return;
		}
		// 进入评审名单确定表(报价文件得分排序)
		if (StringUtils.equals(type, "REVIEW_SCORE"))
		{
			getReviewScoreRecordDoc(data, tpid, template);
			return;
		}
		// 入围投标人名单确定表(简易)
		if (StringUtils.contains(type, "PRE_JY"))
		{
			getReviewRecordDocForJY(data, tpid, template, type);
			return;
		}

		// 入围投标人名单确定表(简易)
		if (StringUtils.contains(type, "PRE_JL"))
		{
			getReviewRecordDocForJlV2(data, tpid, template, type);
			return;
		}

		// 开标记录表
		if (StringUtils.equals(type, "firstRecord"))
		{
			createKFirstRecordDoc(data, tpid, template);
			return;
		}
	}

	/**
	 * 
	 * 解析工程量清单xml并生产记录表<br/>
	 * <p>
	 * 解析工程量清单xml并生产记录表
	 * </p>
	 * 
	 * @param data
	 *            页面请求
	 * @param tpid
	 *            招标项目ID
	 * @throws FacadeException
	 *             服务异常
	 */
	public void createhardwareInfoRecordDoc(AeolusData data, String tpid)
			throws FacadeException
	{
		logger.debug(LogUtils.format("解析工程量清单xml并生产记录表", data, tpid));
		String url = "/default/eokb/bus/hardware_info/hardware.info.doc.html";

		String fileName = "/secondRecord.doc";
		// 生成doc
		createDocFile(url, HardWareAction.getModelInfo(), fileName);
	}

	/**
	 * 
	 * 生成同安小项目球号录入记录表<br/>
	 * <p>
	 * 生成同安小项目球号录入记录表
	 * </p>
	 * 
	 * @param data
	 *            页面请求
	 * @param tpid
	 *            招标项目ID
	 * @throws ServiceException
	 *             服务异常
	 */
	private void createBidderNoRecordDoc(AeolusData data, String tpid,
			String template) throws ServiceException
	{
		logger.debug(LogUtils.format("生成同安小项目球号录入记录表", data, tpid));
		String url = getTheme(data.getHttpServletRequest()) + "/eokb/"
				+ template + "/prebid/extract.bidder.no.result.doc.html";

		String fileName = "/bidderNo.doc";
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		List<Record<String, Object>> bidders = this.activeRecordDAO.statement()
				.selectList("xms_fjsz_common.getBidderNoInfo", param);
		if (CollectionUtils.isEmpty(bidders))
		{
			throw new ServiceException("", "未获取到投标人信息");
		}

		Record<String, Object> section = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").get(param);

		if (CollectionUtils.isEmpty(section))
		{
			throw new ServiceException("", "未获取到标段信息");
		}
		Record<String, Object> model = new RecordImpl<String, Object>();
		model.put("V_BID_SECTION_NAME", section.getString("V_BID_SECTION_NAME"));
		model.put("BIDDERS", bidders);

		// 生成doc
		createDocFile(url, model, fileName);
	}

	/**
	 * 
	 * 生成k值记录表<br/>
	 * <p>
	 * 生成k值记录表
	 * </p>
	 * 
	 * @param data
	 *            页面请求
	 * @param tpid
	 *            招标项目ID
	 * @throws ServiceException
	 *             服务异常
	 */
	private void createKvalueRecordDoc(AeolusData data, String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("生成k值记录表", data, tpid));
		String url = getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/k_value/chouquK.doc.html";

		String fileName = "/pbParamRecord.doc";
		Record<String, Object> param = new RecordImpl<String, Object>();
		// K整数位
		param.setColumn("type", ConstantEOKB.K_VALUE_1);
		param.setColumn("tpid", tpid);
		// 查询k值整数位JSON
		List<String> kJsons = this.activeRecordDAO.statement().loadList(
				sqlName + ".getKvalue", param);
		param.put("KINFOS1", FjszUtils.getKValueView(kJsons));

		// K小数点后一位
		param.setColumn("type", ConstantEOKB.K_VALUE_2);
		// 查询k值小数点后一位JSON
		kJsons = this.activeRecordDAO.statement().loadList(
				sqlName + ".getKvalue", param);
		param.put("KINFOS2", FjszUtils.getKValueView(kJsons));

		// K小数点后两位
		param.setColumn("type", ConstantEOKB.K_VALUE_3);
		// 查询k值小数点后两位JSON
		kJsons = this.activeRecordDAO.statement().loadList(
				sqlName + ".getKvalue", param);
		param.put("KINFOS3", FjszUtils.getKValueView(kJsons));

		// K值
		param.setColumn("type", ConstantEOKB.K_VALUE);
		// 查询k值JSON
		kJsons = this.activeRecordDAO.statement().loadList(
				sqlName + ".getKvalue", param);
		param.put("KINFOS", FjszUtils.getKValueView(kJsons));

		// 生成doc
		createDocFile(url, param, fileName);
	}

	/**
	 * 
	 * 生成第一信封开标记录表doc<br/>
	 * <p>
	 * 生成第一信封开标记录表doc
	 * </p>
	 * 
	 * @param data
	 *            请求
	 * @param tpid
	 *            招标项目ID
	 * @param template
	 *            招标项目code
	 * @throws ServiceException
	 *             服务请求
	 */
	public void createKFirstRecordDoc(AeolusData data, String tpid,
			String template) throws ServiceException
	{
		logger.debug(LogUtils.format("生成第一信封开标记录表doc", data, tpid, template));

		String url = getTheme(data.getHttpServletRequest()) + "/eokb/"
				+ template + "/firstEnvelope/first.record.doc.html";
		String fileName = "/firstRecord.doc";

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);

		// 通用版要做特殊处理
		if (template.contains("_tyb_")
				&& !StringUtils.equals("xms_fwjz_taxxm_tyb_v1", template)
				&& !StringUtils.equals("xms_fwjz_jmxxm_tyb_v1", template)
				&& !StringUtils.equals("xms_szgc_taxxm_tyb_v1", template)
				&& !StringUtils.equals("xms_szgc_jmxxm_tyb_v1", template))
		{
			param.putAll(FjszUtils.getOpenBidRecordFormForTyb(tpid));
		}
		else
		{
			param.putAll(FjszUtils.getOpenBidRecordForm(tpid));
		}
		// 生成doc
		createDocFile(url, param, fileName);
	}

	/**
	 * 
	 * 生成进入入围名单记录表doc（基准价绝对值排序）<br/>
	 * <p>
	 * 生成进入入围名单记录表doc（基准价绝对值排序）
	 * </p>
	 * 
	 * @param data
	 *            请求
	 * @param tpid
	 *            招标项目ID
	 * @param template
	 *            招标项目code
	 * @throws ServiceException
	 *             服务请求
	 */
	private void getPreTenderAbsoluteRecordDoc(AeolusData data, String tpid,
			String template, String type) throws ServiceException
	{
		logger.debug(LogUtils.format("生成进入入围名单记录表doc（基准价绝对值排序）", data, tpid,
				template));

		String htmlName = "preTenderList.doc.html";
		String fileName = "/preTenderList.doc";
		if (StringUtils.equals(type, "PRE_ABSOLUTE_GZ"))
		{
			htmlName = "preTenderList.for.gz.doc.html";
			fileName = "/preTenderList.for.gz.doc";
		}
		String url = getTheme(data.getHttpServletRequest()) + "/eokb/"
				+ template + "/prebid/" + htmlName;
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);

		// 标段名称集
		StringBuilder sectionName = new StringBuilder();

		String vjson = null;
		JSONObject jobj = null;
		JSONObject json = null;

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
			param.put("price", section.getDouble("N_EVALUATION_PRICE"));
			param.put("type", ConstantEOKB.BENCHMARK);
			List<Record<String, Object>> tenderList = this.activeRecordDAO
					.statement().selectList(
							sqlName + ".getPreTenderListResult", param);
			for (Record<String, Object> tender : tenderList)
			{
				vjson = tender.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				if (!StringUtils.isEmpty(jobj.getString("cerditScore")))
				{
					// 季度信用分信息
					tender.setColumn("cerditScore", jobj.get("cerditScore"));
				}

				if (tender.containsKey("V_JSON")
						&& StringUtils.isNotEmpty(tender.getString("V_JSON")))
				{
					json = JSONObject.parseObject(tender.getString("V_JSON"));
					tender.setColumn("SCOPE", json.getString("SCOPE"));
					tender.setColumn("BENCHMARK", json.getString("BENCHMARK"));
				}
				// 2018.03.28 wengdm 投标报价与评标基准价的绝对值出现小数点后多位，强制转换成小数点后两位
				tender.setColumn("ABSOLUTE",
						FMT.format(tender.getDouble("ABSOLUTE")));
				tender.setColumn("SCORE", tender.getFloat("N_CREDITSCORE"));
				tender.setColumn("N_PRICE",
						FMT_D.format(tender.getDouble("N_PRICE")));
			}
			section.setColumn("TENDER_LIST", tenderList);
		}
		// 项目名称
		param.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		param.put("SECTIONS", sections);
		param.put("SECTIONNAMES", sectionName.toString());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		param.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		// 生成doc文件
		createDocFile(url, param, fileName);
	}

	/**
	 * 
	 * 生成进入评审名单记录表doc（基准价绝对值排序）<br/>
	 * <p>
	 * 生成进入评审名单记录表doc（基准价绝对值排序）
	 * </p>
	 * 
	 * @param data
	 *            请求
	 * @param tpid
	 *            招标项目ID
	 * @param template
	 *            招标项目code
	 * @throws ServiceException
	 *             服务请求
	 */
	private void getReviewAbsoluteRecordDoc(AeolusData data, String tpid,
			String template) throws ServiceException
	{
		logger.debug(LogUtils.format("生成进入评审名单记录表doc（基准价绝对值排序）", data, tpid,
				template));

		String url = getTheme(data.getHttpServletRequest()) + "/eokb/"
				+ template + "/prebid/reviewTenderList.doc.html";
		String fileName = "/reviewTenderList.doc";
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		String vjson = null;
		JSONObject jobj = null;

		// 迭代标段组
		for (Record<String, Object> section : sections)
		{

			param.clear();
			param.put("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.put("price", section.getDouble("N_EVALUATION_PRICE"));
			param.put("type", ConstantEOKB.BENCHMARK);
			List<Record<String, Object>> tenderList = this.activeRecordDAO
					.statement().selectList(sqlName + ".getReviewTenderList",
							param);
			for (Record<String, Object> tender : tenderList)
			{
				vjson = tender.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				if (!StringUtils.isEmpty(jobj.getString("cerditScore")))
				{
					// 季度信用分信息
					tender.setColumn("cerditScore", jobj.get("cerditScore"));
				}
				tender.setColumn("N_PRICE",
						FMT_D.format(tender.getDouble("N_PRICE")));
				// 2018.03.28 wengdm 投标报价与评标基准价的绝对值出现小数点后多位，强制转换成小数点后两位
				tender.setColumn("ABSOLUTE",
						FMT.format(tender.getDouble("ABSOLUTE")));
			}
			section.setColumn("TENDER_LIST", tenderList);
		}
		param.put("SECTIONS", sections);
		// 生成doc文件
		createDocFile(url, param, fileName);
	}

	/**
	 * 
	 * 生成进入入围名单记录表doc（报价文件得分排序）<br/>
	 * <p>
	 * 生成进入入围名单记录表doc（报价文件得分排序）
	 * </p>
	 * 
	 * @param data
	 *            请求
	 * @param tpid
	 *            招标项目ID
	 * @param template
	 *            招标项目code
	 * @throws ServiceException
	 *             服务请求
	 */
	private void getPreTenderScoreRecordDoc(AeolusData data, String tpid,
			String template, String type) throws ServiceException
	{
		logger.debug(LogUtils.format("生成进入入围名单记录表doc（报价文件得分排序）", data, tpid,
				template));

		String htmlName = "preTenderList.doc.html";
		String fileName = "/preTenderList.doc";
		if (StringUtils.equals(type, "PRE_SCORE_GZ"))
		{
			htmlName = "preTenderList.for.gz.doc.html";
			fileName = "/preTenderList.for.gz.doc";
		}

		String url = getTheme(data.getHttpServletRequest()) + "/eokb/"
				+ template + "/prebid/" + htmlName;
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		// 标段名称集
		StringBuilder sectionName = new StringBuilder();
		String vjson = null;
		JSONObject jobj = null;
		JSONObject json = null;
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
			param.put("price", section.getDouble("N_CONTROL_PRICE"));
			param.put("TYPE", ConstantEOKB.BENCHMARK);
			param.put("status", "1");
			List<Record<String, Object>> tenderList = this.activeRecordDAO
					.statement()
					.selectList(
							"xms_fwjz_sg_jpsdzdtbjzbf_B_v1_prebid.getPreTenderList",
							param);
			for (Record<String, Object> record : tenderList)
			{
				vjson = record.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				if (!StringUtils.isEmpty(jobj.getString("cerditScore")))
				{
					// 季度信用分信息
					record.setColumn("cerditScore", jobj.get("cerditScore"));
				}
				double benchmark = 0D;
				if (record.containsKey("V_JSON")
						&& StringUtils.isNotEmpty(record.getString("V_JSON")))
				{
					json = JSONObject.parseObject(record.getString("V_JSON"));
					record.setColumn("SCOPE", json.getString("SCOPE"));
					record.setColumn("BENCHMARK", json.getString("BENCHMARK"));
					benchmark = Double.parseDouble(json.getString("BENCHMARK"));
				}
				record.setColumn(
						"SCORE",
						FjszUtils.getPriceScore(benchmark,
								record.getDouble("N_PRICE")));
				record.setColumn("N_PRICE",
						FMT_D.format(record.getDouble("N_PRICE")));
			}
			String[] orderValue = { "SCORE" };
			boolean[] isAscArr = { false };
			ListSortUtils.sort(tenderList, orderValue, isAscArr);
			section.setColumn("TENDER_LIST", tenderList);
		}
		param.put("SECTIONNAMES", sectionName.toString());
		// 项目名称
		param.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		param.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		param.put("SECTIONS", sections);
		// 生成doc文件
		createDocFile(url, param, fileName);
	}

	/**
	 * 
	 * 生成进入评审名单记录表doc（报价文件得分排序）<br/>
	 * <p>
	 * 生成进入评审名单记录表doc（报价文件得分排序）
	 * </p>
	 * 
	 * @param data
	 *            请求
	 * @param tpid
	 *            招标项目ID
	 * @param template
	 *            招标项目code
	 * @throws ServiceException
	 *             服务请求
	 */
	private void getReviewScoreRecordDoc(AeolusData data, String tpid,
			String template) throws ServiceException
	{
		logger.debug(LogUtils.format("生成进入评审名单记录表doc（报价文件得分排序）", data, tpid,
				template));

		String url = getTheme(data.getHttpServletRequest()) + "/eokb/"
				+ template + "/prebid/reviewTenderList.doc.html";
		String fileName = "/reviewTenderList.doc";
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		String vjson = null;
		JSONObject jobj = null;
		double benchmark;
		// 迭代标段组
		for (Record<String, Object> section : sections)
		{

			param.clear();
			param.put("tpid", tpid);
			param.put("status", "1");
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.put("price", section.getDouble("N_CONTROL_PRICE"));
			param.put("TYPE", ConstantEOKB.BENCHMARK);
			List<Record<String, Object>> tenderList = this.activeRecordDAO
					.statement()
					.selectList(
							"xms_fwjz_sg_jpsdzdtbjzbf_B_v1_prebid.getPreTenderList",
							param);
			for (Record<String, Object> record : tenderList)
			{
				vjson = record.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				if (!StringUtils.isEmpty(jobj.getString("cerditScore")))
				{
					// 季度信用分信息
					record.setColumn("cerditScore", jobj.get("cerditScore"));
				}
				benchmark = 0D;
				if (record.containsKey("V_JSON")
						&& StringUtils.isNotEmpty(record.getString("V_JSON")))
				{
					JSONObject json = JSONObject.parseObject(record
							.getString("V_JSON"));
					benchmark = Double.parseDouble(json.getString("BENCHMARK"));
				}
				record.setColumn(
						"SCORE",
						FjszUtils.getPriceScore(benchmark,
								record.getDouble("N_PRICE")));
				record.setColumn("N_PRICE",
						FMT_D.format(record.getDouble("N_PRICE")));
			}
			String[] orderValue = { "SCORE" };
			boolean[] isAscArr = { false };
			ListSortUtils.sort(tenderList, orderValue, isAscArr);
			// 取前五名
			int count = 0;
			double score = 0d;
			List<Record<String, Object>> tempList = new LinkedList<Record<String, Object>>();
			for (Record<String, Object> record : tenderList)
			{
				if (record.getDouble("SCORE") != score)
				{
					count++;
				}
				if (count > 5)
				{
					break;
				}
				score = record.getDouble("SCORE");
				tempList.add(record);
			}
			tenderList = tempList;
			section.setColumn("TENDER_LIST", tenderList);
		}
		param.put("SECTIONS", sections);
		// 生成doc文件
		createDocFile(url, param, fileName);
	}

	/**
	 * 
	 * 生成进入评审名单记录表doc（简易评标办法）<br/>
	 * <p>
	 * 生成进入评审名单记录表doc（简易评标办法）
	 * </p>
	 * 
	 * @param data
	 *            请求
	 * @param tpid
	 *            招标项目ID
	 * @param template
	 *            招标项目code
	 * @throws ServiceException
	 *             服务请求
	 */
	private void getReviewRecordDocForJY(AeolusData data, String tpid,
			String template, String type) throws ServiceException
	{
		logger.debug(LogUtils.format("生成进入评审名单记录表doc（简易评标办法）", data, tpid,
				template));

		String htmlName = "preTenderList.doc.html";
		String fileName = "/preTenderList.doc";
		if (StringUtils.equals(type, "PRE_JY_GZ"))
		{
			htmlName = "preTenderList.for.gz.doc.html";
			fileName = "/preTenderList.for.gz.doc";
		}

		String url = getTheme(data.getHttpServletRequest()) + "/eokb/"
				+ template + "/prebid/" + htmlName;
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		// 标段名称集
		StringBuilder sectionName = new StringBuilder();
		String vjson = null;
		JSONObject jobj = null;
		JSONArray sings = null;
		JSONObject sing = null;
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
			List<Record<String, Object>> tenderList = this.activeRecordDAO
					.auto().table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.setCondition("AND", "N_ENVELOPE_9=1")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			for (Record<String, Object> tender : tenderList)
			{
				vjson = tender.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				sings = jobj.getJSONArray("objSing");
				for (int i = 0; i < sings.size(); i++)
				{
					sing = sings.getJSONObject(i);
					for (Entry<String, Object> entry : sing.entrySet())
					{
						tender.setColumn(entry.getKey(), entry.getValue());
					}
				}
				tender.setColumn("N_PRICE",
						FMT_D.format(tender.getDouble("N_PRICE")));
			}

			section.setColumn("TENDER_LIST", tenderList);
		}
		// 项目名称
		param.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		param.put("SECTIONNAMES", sectionName.toString());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		param.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		param.put("SECTIONS", sections);
		// 生成doc文件
		createDocFile(url, param, fileName);
	}

	/**
	 * 
	 * 生成进入入围名单记录表doc（监理V2简易评标办法）<br/>
	 * <p>
	 * 生成进入入围名单记录表doc（监理V2简易评标办法）
	 * </p>
	 * 
	 * @param data
	 *            请求
	 * @param tpid
	 *            招标项目ID
	 * @param template
	 *            招标项目code
	 * @throws ServiceException
	 *             服务请求
	 */
	private void getReviewRecordDocForJlV2(AeolusData data, String tpid,
			String template, String type) throws ServiceException
	{
		logger.debug(LogUtils.format(" 生成进入入围名单记录表doc（监理V2简易评标办法）", data, tpid,
				template));

		String htmlName = "preTenderList.doc.html";
		String fileName = "/preTenderList.doc";
		if (StringUtils.equals(type, "PRE_JL_GZ_V2"))
		{
			htmlName = "preTenderList.for.gz.doc.html";
			fileName = "/preTenderList.for.gz.doc";
		}

		String url = getTheme(data.getHttpServletRequest()) + "/eokb/"
				+ template + "/prebid/" + htmlName;
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		// 标段名称集
		StringBuilder sectionName = new StringBuilder();
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
			List<Record<String, Object>> tenderList = this.activeRecordDAO
					.auto().table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.setCondition("AND", "N_ENVELOPE_9 IN (1,2,3)")
					.addSortOrder("N_CREDITSCORE", "DESC")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			for (Record<String, Object> tender : tenderList)
			{
				tender.setColumn("N_PRICE",
						FMT_D.format(tender.getDouble("N_PRICE")));
			}

			section.setColumn("TENDER_LIST", tenderList);
		}
		// 项目名称
		param.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		param.put("SECTIONNAMES", sectionName.toString());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		param.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		param.put("SECTIONS", sections);
		// 生成doc文件
		createDocFile(url, param, fileName);
	}

	/**
	 * 生成doc文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param url
	 * @param model
	 * @param fileName
	 * @throws ServiceException
	 */
	private void createDocFile(String url, Record<String, Object> model,
			String fileName) throws ServiceException
	{
		try
		{
			// 添加DOC文件
			StringBuffer buf = new StringBuffer();
			String header = new BeetlView().converthtmlByNone(new ModelAndView(
					"/default/eokb/bus/template/kb.report.css.html"));
			buf.append(header);
			Template bodyUrl = new BeetlView()
					.convertToTemplate(new ModelAndView(url, model));
			buf.append(bodyUrl.render());
			buf.append("</div></body></html>");
			String fileUrl = SystemParamUtils
					.getString("aeolus.ebidKB.file.path.url")
					+ "bidReport/"
					+ SessionUtils.getTPID();
			File dir = new File(fileUrl);
			// 判断文件夹是否存在
			if (!dir.exists())
			{
				dir.mkdirs();
			}
			url = fileUrl + fileName;
			FileUtils.write(new File(url), buf.toString(),
					ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("生成DOC文件失败!"), e);
			throw new ServiceException("", "生成DOC文件失败!");
		}
	}
}
