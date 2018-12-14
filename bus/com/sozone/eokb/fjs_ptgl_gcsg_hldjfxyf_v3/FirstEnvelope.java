/**
 * 包名：com.sozone.eokb.fjs_gsgl_ljsg_hldjf_v1
 * 文件名：FirstEnvelope.java<br/>
 * 创建时间：2017-8-28 下午2:13:39<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_ptgl_gcsg_hldjfxyf_v3;

import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.bus.createFile.CreateFilePTGL;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_ptgl.common.PtglUtils;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 第一信封解密服务接口<br/>
 * <p>
 * 第一信封解密服务接口<br/>
 * </p>
 * Time：2017-8-28 下午2:13:39<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fjs_ptgl_gcsg_hldjfxyf_v3/firstenvelope", desc = "第一信封解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class FirstEnvelope extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FirstEnvelope.class);

	/**
	 * 范本CODE
	 */
	private static String projectCode = ConstantEOKB.EOKBBemCode.FJS_PTGL_GCSG_HLDJFXYF_V3;

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
	 * 第一信封解密情况一览表<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/gfed/flow/{tpnid}", desc = "第一信封解密情况一览表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstEnvelopeDecrypt(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封解密情况一览表", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpnoid", tenderProjectNodeID);

		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		model.putAll(PtglUtils.getDecryptSuccessCount(tpid));

		// 流程信息
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		// 第一数字信封解密视图
		model.put("SECTION_LIST",
				PtglUtils.getFirstEnvelopeDecryptSituation(tpid));

		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(
					getTheme(data.getHttpServletRequest()) + "/eokb/"
							+ projectCode + "/firstEnvelope/credit.zbr.html",
					model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/firstEnvelope/credit.tbr.html",
				model);
	}

	/**
	 * 第一信封解密情况一览表（标段组）<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/gfebg/flow/{tpnid}", desc = "第一信封解密情况一览表（标段组）")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstEnvelopeByGroup(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封解密情况一览表（标段组）", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpnoid", tenderProjectNodeID);

		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		model.putAll(PtglUtils.getDecryptSuccessCount(tpid));

		// 流程信息
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		// 第一数字信封解密视图
		model.put("SECTION_LIST",
				PtglUtils.getFirstEnvelopeDecryptSituation(tpid));

		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode
					+ "/firstEnvelope/credit.group.zbr.html", model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode
				+ "/firstEnvelope/credit.group.tbr.html", model);
	}

	/**
	 * 第一信封评审结果<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/review/flow/{tpnid}", desc = "第一信封评审结果")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstReview(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封评审结果", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 流程信息
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		// 获取当前招标项目的评标状态
		String psover = "0";
		Record<String, Object> evaluationStatus = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.get(param);
		String status = evaluationStatus.getColumn("V_BID_EVALUATION_STATUS");
		if (!StringUtils.equals("0", status))
		{
			psover = "1";
			// 评审结果视图信息
			model.put("SECTION_LIST", PtglUtils.getFirstReviewSituation(tpid));
		}
		model.put("PSOVER", psover);
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(
					getTheme(data.getHttpServletRequest()) + "/eokb/"
							+ projectCode + "/firstEnvelope/review.zbr.html",
					model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/firstEnvelope/review.tbr.html",
				model);
	}

	/**
	 * 第一信封评审结果有标段组<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/reviewGroup/flow/{tpnid}", desc = "第一信封评审结果有标段组")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstReviewGroup(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封评审结果有标段组", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 获取当前招标项目的评标状态
		String psover = "0";
		Record<String, Object> evaluationStatus = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.get(param);
		String status = evaluationStatus.getColumn("V_BID_EVALUATION_STATUS");
		if (!StringUtils.equals("0", status))
		{
			psover = "1";
			model.put("SECTION_LIST",
					PtglUtils.getFirstReviewGroupSituation(tpid));
		}
		model.put("PSOVER", psover);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode
					+ "/firstEnvelope/review.group.zbr.html", model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode
				+ "/firstEnvelope/review.group.tbr.html", model);
	}

	/**
	 * 开标记录表一<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 * @throws ParseException
	 *             ParseException
	 */
	// 定义路径
	@Path(value = "/firstRecord", desc = "开标记录表一")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstRecord(AeolusData data) throws FacadeException,
			ParseException
	{
		logger.debug(LogUtils.format("开标记录表一", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		model.putAll(PtglUtils.getFirstOpenBidRecordForm(tpid));
		// 项目名称
		model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 组别
		model.put("GROUP", "无标段组");
		if (SessionUtils.isSectionGroup())
		{
			model.put("GROUP", "有标段组");
		}
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		model.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode
				+ "/firstenvelope/first.record.view.html", model);
	}

	/**
	 * 
	 * 保存投标人备注<br/>
	 * <p>
	 * 保存投标人备注
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/saveBidders", desc = "保存投标人备注")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void saveRemark(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存投标人备注", data));

		String json = data.getParam("bidders");
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isNotEmpty(json))
		{
			PtglUtils.saveFirstRemark(data, json, tpid, "firstRemark",
					ConstantEOKB.FIRST_REMARK);
		}

		CreateFilePTGL ptgl = new CreateFilePTGL();
		ptgl.createDocFileForGcsgAndGdyh(data, tpid, projectCode, "firstRecord");
	}
}
