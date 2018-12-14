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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.beetl.core.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_ptgl.common.PtglUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 普通公路生成doc服务接口<br/>
 * <p>
 * 普通公路生成doc服务接口<br/>
 * </p>
 * Time：2017-10-20 下午2:06:55<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/ptgl/bus/createFile", desc = "创建文件")
// 登录即可访问
@Permission(Level.Authenticated)
public class CreateFilePTGL extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(CreateFileGSGL.class);

	/**
	 * 普通公路合理低价法数据库xml
	 */
	private static String hldjfSql = ConstantEOKB.EOKBBemCode.FJS_PTGL_GCSG_HLDJF_V1
			+ "_secondenvelope";
	/**
	 * 数据库xml
	 */
	private static String commonSql = ConstantEOKB.EOKBBemCode.FJS_PTGL_COMMON;

	/**
	 * 普通公路勘察设计抽取评标基准价数据库xml
	 */
	private static String bspmSql = ConstantEOKB.EOKBBemCode.FJS_PTGL_KCSJ_HLDJFXYF_V1
			+ "_LowerCoefficient";

	/**
	 * 普通公路勘察设计抽取评标基准价数据库xml
	 */
	private static String kcsjSql = ConstantEOKB.EOKBBemCode.FJS_PTGL_KCSJ_ZHPGF1_V1
			+ "_firstenvelope";

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
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/doc", desc = "创建DOC文件")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void buildDocFile(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("创建DOC文件", data));
		String template = data.getParam("template");
		String envelope = data.getParam("envelope");
		String type = data.getParam("modelType");

		String tpid = SessionUtils.getTPID();

		// 普通工路工程施工 ，国道养护
		if (StringUtils.contains(template, "fjs_ptgl_gcsg")
				|| StringUtils.contains(template, "fjs_ptgl_gdyh"))
		{
			createDocFileForGcsgAndGdyh(data, tpid, template, type);
			return;
		}

		// 普通工路施工监理
		if (StringUtils.equals("fjs_ptgl_sgjl_hldjf_v1", template)
				|| StringUtils.equals("fjs_ptgl_sgjl_hldjfxyf_v2", template)
				|| StringUtils.equals("fjs_ptgl_sgjl_hldjfxyf_v3", template))
		{
			createDocFileForJl(data, tpid, type, template);
			return;
		}

		// 普通工路勘察设计合理低价法加信用分
		if (StringUtils.contains(template, "fjs_ptgl_kcsj_hldjfxyf"))
		{
			createDocFileForKcHldjf(data, tpid, type, template);
			return;
		}

		// 普通公路勘察设计综合评估法
		if (StringUtils.contains(template, "fjs_ptgl_kcsj_zhpgf1"))
		{
			createDocFileForKcZhpgf(data, tpid, template, type, envelope);
		}

	}

	/**
	 * 获取普通公路合理低价法摇号结果<br/>
	 * <p>
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private List<Record<String, Object>> getPTGLHLDJFDEXFPYHGViewData()
			throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());
		// 获取当前招标项目的所有标段组
		List<Record<String, Object>> groups = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.list(param);

		// 循环标段组
		for (Record<String, Object> group : groups)
		{
			param.put("tpid", SessionUtils.getTPID());
			param.setColumn("group",
					group.getString("V_BID_SECTION_GROUP_CODE"));
			// 标段编号
			group.setColumn("BID_SECTION_GROUP_CODE",
					group.getString("V_BID_SECTION_GROUP_CODE"));

			// 获得标段
			List<Record<String, Object>> sections = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
					.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
					.setCondition("AND", "V_BID_SECTION_GROUP_CODE = #{group}")
					.list(param);

			// 循环标段
			for (Record<String, Object> section : sections)
			{
				section.setColumn("VBID_SECTION_NAME",
						section.getColumn("V_BID_SECTION_NAME"));
				param.setColumn("sectionId",
						section.getColumn("V_BID_SECTION_ID"));
				List<Record<String, Object>> rollResult = this.activeRecordDAO
						.auto().table(TableName.EKB_T_DECRYPT_INFO)
						.setCondition("AND", "V_TPID = #{tpid}")
						.setCondition("AND", "V_BID_SECTION_ID = #{sectionId}")
						.addSortOrder("N_SORT_FILE_BID", "ASC").list(param);
				section.setColumn("BIDDERS", rollResult);
			}

			group.setColumn("TENDER_LIST", sections);
		}
		return groups;
	}

	/**
	 * 
	 * 获取普通公路合理低价法标段分配结果<br/>
	 * <p>
	 * 获取普通公路合理低价法标段分配结果
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private List<Record<String, Object>> getPTGLHLDJFROLLRSViewData()
			throws ServiceException
	{
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);

		// 查询是否已经发起过确认
		Record<String, Object> cm = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_CHECK_MODEL)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND",
						"V_STATUS = '1' AND V_MODEL_TYPE = 'DYXF_electronics'")
				.get(param);
		boolean alreadyLaunched = true;
		if (CollectionUtils.isEmpty(cm))
		{
			alreadyLaunched = false;
		}

		// 获取当前招标项目的所有标段组
		List<Record<String, Object>> groups = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.list(param);

		// 循环标段组
		for (Record<String, Object> group : groups)
		{
			param.put("tpid", SessionUtils.getTPID());
			param.setColumn("group",
					group.getString("V_BID_SECTION_GROUP_CODE"));
			// 标段编号
			group.setColumn("BID_SECTION_GROUP_CODE",
					group.getString("V_BID_SECTION_GROUP_CODE"));

			// 获得标段
			List<Record<String, Object>> sections = this.activeRecordDAO.auto()
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
				param.setColumn("tpid", SessionUtils.getTPID());
				section.setColumn("V_BID_SECTION_NAME",
						section.getColumn("V_BID_SECTION_NAME"));
				param.setColumn("sid", section.getColumn("V_BID_SECTION_ID"));
				param.setColumn("modelType", "DYXF_electronics");
				List<Record<String, Object>> rollResult = this.activeRecordDAO
						.statement().selectList(hldjfSql + ".getSecBidderInfo",
								param);
				section.setColumn("BIDDERS", rollResult);
				section.setColumn("ALREADY_LAUNCHED", alreadyLaunched);
			}

			group.setColumn("TENDER_LIST", sections);
		}
		return groups;
	}

	/**
	 * 获取第一信封评审结果视图<br/>
	 * <p>
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private List<Record<String, Object>> getDYXKCSJFPSJGViewData()
			throws ServiceException
	{
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);

		List<Record<String, Object>> sections = null;
		// 获取当前招标项目的所有标段
		sections = this.activeRecordDAO.auto()
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
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.put(ConstantEOKB.PB_DB_ID_VAR,
					SystemParamUtils.getString(SysParamKey.PB_DB_ID_KEY));

			// 标段名称
			section.setColumn("BID_SECTION_NAME",
					section.getString("V_BID_SECTION_NAME"));

			// 标段编号
			section.setColumn("BID_SECTION_GROUP_CODE",
					section.getString("V_BID_SECTION_GROUP_CODE"));

			List<Record<String, Object>> bidders = this.activeRecordDAO
					.statement()
					.selectList(kcsjSql + ".getReviewResult", param);
			for (Record<String, Object> bidder : bidders)
			{
				String vjson = bidder.getString("V_JSON_OBJ");
				bidder.setColumn("tbPeName", BidderElementParseUtils
						.getSingObjAttributeSum(vjson, "tbPeName"));
			}

			// 查出投标人名单
			section.setColumn("TENDER_LIST", bidders);
		}
		return sections;
	}

	/**
	 * 
	 * 工程施工以及国道养护范本生成doc记录文件<br/>
	 * <p>
	 * 工程施工以及国道养护范本生成doc记录文件
	 * </p>
	 * 
	 * @param data
	 *            页面请求
	 * @param tpid
	 *            项目主键
	 * @param template
	 *            范本code
	 * @param type
	 *            文件类型标记
	 * @throws ServiceException
	 *             服务异常
	 */
	public void createDocFileForGcsgAndGdyh(AeolusData data, String tpid,
			String template, String type) throws ServiceException
	{
		Record<String, Object> model = new RecordImpl<String, Object>();
		String url = null;
		String fileName = null;
		if (StringUtils.equals("DYXF_credit", type))// 投标人所投标段组及信用组
		{

			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/credit.doc.html";
			if (SessionUtils.isSectionGroup())
			{
				url = getTheme(data.getHttpServletRequest()) + "/eokb/"
						+ template + "/firstEnvelope/credit.group.doc.html";

			}

			fileName = "/credit.doc";
			model.put("SECTION_LIST",
					PtglUtils.getFirstEnvelopeDecryptSituation(tpid));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DYXF_review", type))// 第一信封评审结果
		{
			if (SessionUtils.isSectionGroup())
			{
				url = getTheme(data.getHttpServletRequest()) + "/eokb/"
						+ template + "/firstEnvelope/review.group.doc.html";
				fileName = "/review.group.doc";
				model.put("SECTION_LIST",
						PtglUtils.getFirstReviewGroupSituation(tpid));
			}
			else
			{
				url = getTheme(data.getHttpServletRequest()) + "/eokb/"
						+ template + "/firstEnvelope/review.doc.html";
				fileName = "/review.doc";
				model.put("SECTION_LIST",
						PtglUtils.getFirstReviewSituation(tpid));
			}
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_ROLLBAL", type))// 第二信封摇号结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/rollbal.doc.html";
			fileName = "/rollbal.doc";
			model.put("SECTION_LIST", getPTGLHLDJFDEXFPYHGViewData());
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_ROLLRS", type))// 第二信封标段分配结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/rollresult.doc.html";
			fileName = "/rollrs.doc";
			model.put("SECTION_LIST", getPTGLHLDJFROLLRSViewData());
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_offer", type))// 第二信封文件解密结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/decrypt.doc.html";
			fileName = "/DEXF_offer.doc";
			model.putAll(PtglUtils.getSecondEnvelopeDecryptSituation(tpid));
			createDocFile(url, model, fileName);
			return;
		}
		else if (StringUtils.equals("DEXF_BSPM_YAOHAO", type))// 评标基准价计算方法
		{
			createBspmDoc(data, tpid, template);
			return;
		}
		else if (StringUtils.equals("DEXF_LOWER_COEFFICIENT", type))// 下浮系数
		{
			createLowerDoc(data, tpid, template);
			return;
		}
		else if (StringUtils.equals("DEXF_CBSP", type))// 计算评标基准价
		{
			createCalculateDoc(data, tpid, template);
			return;
		}

		// 记录表一
		if (StringUtils.equals(type, "firstRecord"))
		{
			// 简易招标办法K值记得从V_BEM_INFO_JSON字段中获取
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/first.record.doc.html";
			fileName = "/firstRecord.doc";
			model.putAll(PtglUtils.getFirstOpenBidRecordForm(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			String group = "无标段组";
			if (SessionUtils.isSectionGroup())
			{
				group = "标段组";
			}
			// 组别
			model.put("GROUP", group);
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
			return;
		}

		// 记录表二
		if (StringUtils.equals(type, "secondRecord"))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/second.record.doc.html";
			fileName = "/secondRecord.doc";
			model.putAll(PtglUtils.getSecondEnvelopeDecryptSituation(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			String group = "无标段组";
			if (SessionUtils.isSectionGroup())
			{
				group = "标段组";
			}
			// 组别
			model.put("GROUP", group);
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
			return;
		}
	}

	/**
	 * 
	 * 普通公路监理范本生成doc记录文件<br/>
	 * <p>
	 * 普通公路监理范本生成doc记录文件
	 * </p>
	 * 
	 * @param data
	 *            页面请求
	 * @param tpid
	 *            项目主键
	 * @param type
	 *            文件类型标记
	 * @param template
	 *            模板
	 * @throws FacadeException
	 *             FacadeException
	 */
	public void createDocFileForJl(AeolusData data, String tpid, String type,
			String template) throws FacadeException
	{
		Record<String, Object> model = new RecordImpl<String, Object>();
		String url = null;
		String fileName = null;
		if (StringUtils.equals("DYXF_credit", type))// 第一数字信封解密情况
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/credit.doc.html";
			fileName = "/credit.doc";
			model.put("SECTION_LIST",
					PtglUtils.getFirstEnvelopeDecryptSituation(tpid));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DYXF_review", type))// 第一信封评审结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/review.doc.html";
			fileName = "/review.doc";
			model.put("SECTION_LIST", PtglUtils.getFirstReviewSituation(tpid));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_offer", type))// 第二信封文件解密结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/decrypt.doc.html";
			fileName = "/DEXF_offer.doc";
			model.putAll(PtglUtils.getSecondEnvelopeDecryptSituation(tpid));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_CBSP", type))// 评标基准价记录表
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/calculate.doc.html";

			fileName = "/calculate.doc";

			model.put("TENDER_PROJECT_SECTION_LIST",
					PtglUtils.getCalculateViewForJl(tpid, "1"));
			createDocFile(url, model, fileName);
			return;
		}

		// 记录表一
		if (StringUtils.equals(type, "firstRecord"))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/first.record.doc.html";
			fileName = "/firstRecord.doc";
			model.putAll(PtglUtils.getFirstOpenBidRecordForm(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			// 组别
			model.put("GROUP", "无标段组");
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
			return;
		}

		// 记录表二
		if (StringUtils.equals(type, "secondRecord"))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/second.record.doc.html";
			fileName = "/secondRecord.doc";
			model.putAll(PtglUtils.getSecondEnvelopeDecryptSituation(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			// 组别
			model.put("GROUP", "无标段组");
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
			return;
		}
	}

	/**
	 * 
	 * 普通公路勘察合理低价法加信用分范本生成doc记录文件<br/>
	 * <p>
	 * 普通公路勘察合理低价法加信用分范本生成doc记录文件
	 * </p>
	 * 
	 * @param data
	 *            页面请求
	 * @param tpid
	 *            项目主键
	 * @param type
	 *            文件类型标记
	 * @param template
	 *            模板
	 * @throws FacadeException
	 *             FacadeException
	 */
	public void createDocFileForKcHldjf(AeolusData data, String tpid,
			String type, String template) throws FacadeException
	{
		Record<String, Object> model = new RecordImpl<String, Object>();
		String url = null;
		String fileName = null;
		if (StringUtils.equals("DYXF_credit", type))// 投标人所投标段组及信用组
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/credit.doc.html";
			fileName = "/credit.doc";
			// 第一数字信封解密视图
			model.put("SECTION_LIST",
					PtglUtils.getFirstEnvelopeDecryptSituation(tpid));
			createDocFile(url, model, fileName);
			return;
		}
		else if (StringUtils.equals("DYXF_review", type))// 第一信封评审结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/review.doc.html";
			fileName = "/review.doc";
			model.putAll(PtglUtils.getReviewRecordForKc(tpid, "2-1"));
			createDocFile(url, model, fileName);
			return;
		}
		else if (StringUtils.equals("DEXF_review", type))// 第二信封评审结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/secondReview.doc.html";
			fileName = "/DEXF_review.doc";
			model.put("SECTION_LIST", getDYXKCSJFPSJGViewData());
			createDocFile(url, model, fileName);
			return;
		}
		else if (StringUtils.equals("DEXF_offer", type))// 第二信封文件解密结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/decrypt.doc.html";
			fileName = "/DEXF_offer.doc";
			model.putAll(PtglUtils.getSecondEnvelopeDecryptForKc(tpid));
			createDocFile(url, model, fileName);
			return;

		}
		else if (StringUtils.equals("DEXF_BSPM_YAOHAO", type))// 第二信封文件解密结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/bspm.doc.html";

			fileName = "/bspm.doc";
			model.putAll(PtglUtils.getAverageValue(tpid));
			createDocFile(url, model, fileName);
			return;
		}
		else if (StringUtils.equals("DEXF_LOWER_COEFFICIENT", type))// 下浮系数结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/lower.doc.html";
			fileName = "/lower.doc";

			model.setColumn("tpid", tpid);
			// 查询下浮系数JSON
			List<String> lcJsons = this.activeRecordDAO.statement().loadList(
					bspmSql + ".getLowerCoefficientInfos", model);
			// 下浮系数视图
			model.putAll(PtglUtils.getLowerCoefficientView(lcJsons));
			createDocFile(url, model, fileName);
			return;
		}
		else if (StringUtils.equals("DEXF_CBSP", type))// 评标基准价结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/calculate.doc.html";

			fileName = "/calculate.doc";
			model.setColumn("tpid", tpid);
			// 判断是否计算过评标基准价
			List<String> jsons = this.activeRecordDAO
					.statement()
					.loadList(
							"fjs_ptgl_kcsj_hldjfxyf_v1_CalculateBidBenchmarkPrice.getCBSPInfos",
							model);
			model.putAll(PtglUtils.getCalculateForKc(jsons, tpid));
			createDocFile(url, model, fileName);
			return;
		}

		// 记录表一
		if (StringUtils.equals(type, "firstRecord"))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/first.record.doc.html";
			fileName = "/firstRecord.doc";
			model.putAll(PtglUtils.getFirstOpenBidRecordForm(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
			return;
		}

		// 记录表二
		if (StringUtils.equals(type, "secondRecord"))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/second.record.doc.html";
			fileName = "/secondRecord.doc";
			model.putAll(PtglUtils.getSecondBidRecordForKc(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
			return;
		}
		// 记录表三
		if (StringUtils.equals(type, "thirdRecord"))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/thirdEnvelope/third.record.doc.html";
			fileName = "/thirdRecord.doc";
			model.putAll(PtglUtils.getSecondEnvelopeDecryptForKc(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
			return;
		}
	}

	/**
	 * 
	 * 普通公路勘察综合评估法范本生成doc记录文件<br/>
	 * <p>
	 * 普通公路勘察综合评估法范本生成doc记录文件
	 * </p>
	 * 
	 * @param data
	 *            页面请求
	 * @param tpid
	 *            项目主键
	 * @param template
	 *            范本code
	 * @param type
	 *            文件类型标记
	 * @param envelope
	 *            信封
	 * @throws FacadeException
	 *             FacadeException
	 */
	public void createDocFileForKcZhpgf(AeolusData data, String tpid,
			String template, String type, String envelope)
			throws FacadeException
	{
		Record<String, Object> model = new RecordImpl<String, Object>();
		String url = null;
		String fileName = null;
		if (StringUtils.equals("DYXF_credit", type))// 投标人所投标段组及信用组
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/" + envelope + "/credit.doc.html";
			fileName = "/credit.doc";
			// 第一数字信封解密视图
			model.put("SECTION_LIST",
					PtglUtils.getFirstEnvelopeDecryptSituation(tpid));
			createDocFile(url, model, fileName);
			return;
		}
		else if (StringUtils.equals("DYXF_review", type))// 第一信封评审结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/" + envelope + "/review.doc.html";
			fileName = "/review.doc";
			model.putAll(PtglUtils.getReviewRecordForKc(tpid, "2-1"));
			createDocFile(url, model, fileName);
			return;
		}
		else if (StringUtils.equals("DEXF_offer", type))// 第二信封文件解密结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/" + envelope + "/decrypt.doc.html";
			fileName = "/DEXF_offer.doc";
			model.putAll(PtglUtils.getSecondEnvelopeDecryptForKc(tpid));
			createDocFile(url, model, fileName);
			return;
		}

		else if (StringUtils.equals("DEXF_review", type))// 第一信封评审结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/" + envelope + "/review.doc.html";
			fileName = "/review.doc";
			model.putAll(PtglUtils.getReviewRecordForKc(tpid, "2-1"));
			createDocFile(url, model, fileName);
			return;
		}
		// 记录表一
		if (StringUtils.equals(type, "firstRecord"))
		{
			// 简易招标办法K值记得从V_BEM_INFO_JSON字段中获取
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/" + envelope + "/first.record.doc.html";
			fileName = "/firstRecord.doc";
			model.putAll(PtglUtils.getFirstOpenBidRecordForm(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
			return;
		}

		// 记录表二
		if (StringUtils.equals(type, "secondRecord"))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/" + envelope + "/second.record.doc.html";
			fileName = "/secondRecord.doc";
			model.putAll(PtglUtils.getSecondBidRecordForKc(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
			return;
		}
		// 记录表三
		if (StringUtils.equals(type, "thirdRecord"))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/" + envelope + "/third.record.doc.html";
			fileName = "/thirdRecord.doc";
			model.putAll(PtglUtils.getSecondEnvelopeDecryptForKc(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
			return;
		}

	}

	/**
	 * 
	 * 评标基准价计算方法记录表doc<br/>
	 * <p>
	 * 评标基准价计算方法记录表doc
	 * </p>
	 * 
	 * @param data
	 * @param tpid
	 * @param template
	 * @throws ServiceException
	 */
	private void createBspmDoc(AeolusData data, String tpid, String template)
			throws ServiceException
	{
		logger.debug(LogUtils.format("评标基准价计算方法记录表doc", data, tpid, template));

		String url = null;
		if (SessionUtils.isSectionGroup())
		{
			url = getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/common/bspm.group.doc.html";
		}
		else
		{
			url = getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/common/bspm.doc.html";
		}

		String fileName = "/bspm.doc";

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 先查评标基准价信息算法JSON
		List<String> bspmJsons = this.activeRecordDAO.statement().loadList(
				commonSql + ".getBidStandardPriceMethodInfos", param);
		// 有标段组
		if (SessionUtils.isSectionGroup())
		{
			param.putAll(PtglUtils
					.getBidStandardPriceMethodGroupView(bspmJsons));
		}
		// 无标段组
		else
		{
			param.putAll(PtglUtils.getBidStandardPriceMethodView(bspmJsons));
		}
		// 生成doc
		createDocFile(url, param, fileName);
	}

	/**
	 * 
	 * 下浮系数记录表doc<br/>
	 * <p>
	 * 下浮系数记录表doc
	 * </p>
	 * 
	 * @param data
	 * @param tpid
	 * @param template
	 * @throws ServiceException
	 */
	private void createLowerDoc(AeolusData data, String tpid, String template)
			throws ServiceException
	{
		logger.debug(LogUtils.format("下浮系数记录表doc", data, tpid, template));

		String url = null;
		if (SessionUtils.isSectionGroup())
		{
			url = getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/common/lower.group.doc.html";
		}
		else
		{
			url = getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/common/lower.doc.html";
		}

		String fileName = "/lower.doc";

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 先查评标基准价信息算法JSON
		// 查询下浮系数JSON
		List<String> lcJsons = this.activeRecordDAO.statement().loadList(
				commonSql + ".getLowerCoefficientInfos", param);
		// 有标段组
		if (SessionUtils.isSectionGroup())
		{
			// 下浮系数视图
			param.putAll(PtglUtils.getLowerCoefficientGroupView(lcJsons));
		}
		else
		{
			// 下浮系数视图
			param.putAll(PtglUtils.getLowerCoefficientView(lcJsons));
		}
		// 生成doc
		createDocFile(url, param, fileName);
	}

	/**
	 * 
	 * 计算评标基准价记录表doc<br/>
	 * <p>
	 * 计算评标基准价记录表doc
	 * </p>
	 * 
	 * @param data
	 * @param tpid
	 * @param template
	 * @throws ServiceException
	 */
	private void createCalculateDoc(AeolusData data, String tpid,
			String template) throws ServiceException
	{
		logger.debug(LogUtils.format("计算评标基准价记录表doc", data, tpid, template));

		String url = null;
		if (SessionUtils.isSectionGroup())
		{
			url = getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/common/calculate.group.doc.html";
		}
		else
		{
			url = getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/common/calculate.doc.html";
		}

		String fileName = "/calculate.doc";

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 判断是否计算过评标基准价
		List<String> jsons = this.activeRecordDAO.statement().loadList(
				commonSql + ".getCBSPInfos", param);
		// 有标段组
		if (SessionUtils.isSectionGroup())
		{
			// 评标基准价视图
			param.putAll(PtglUtils.getCalculateBidBenchmarkPriceGroupView(
					jsons, tpid));
		}
		else
		{
			// 评标基准价视图
			param.putAll(PtglUtils.getCalculateBidBenchmarkPriceView(jsons,
					tpid));
		}
		// 生成doc
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
