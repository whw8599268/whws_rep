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
package com.sozone.eokb.fjs_ptgl_kcsj_zhpgf1_v1;

import java.util.List;

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
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.bus.createFile.CreateFilePTGL;
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.EOKBFlowCode;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
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
@Path(value = "/fjs_ptgl_kcsj_zhpgf1_v1/firstenvelope", desc = "第一信封解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class FirstEnvelope extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FirstEnvelope.class);

	/**
	 * 数据库xml
	 */
	private static String sqlName = ConstantEOKB.EOKBBemCode.FJS_PTGL_KCSJ_ZHPGF1_V1
			+ "_firstenvelope";

	/**
	 * 项目CODE
	 */
	private static String projectCode = ConstantEOKB.EOKBBemCode.FJS_PTGL_KCSJ_ZHPGF1_V1;

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
	 * 修改信息<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 * @throws IOException
	 */

	/**
	 * 第一信封解密情况一览表<br/>
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
	@Path(value = "/credit/flow/{tpnid}", desc = "第一信封解密情况一览表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstEnvelopeDecrypt(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封解密情况一览表", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);

		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		// 流程信息
		model.putAll(PtglUtils.getDecryptSuccessCount(tpid));

		// 第一数字信封解密视图
		model.put("SECTION_LIST",
				PtglUtils.getFirstEnvelopeDecryptSituation(tpid));
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/"
					+ EOKBFlowCode.FIRST_ENVELOPE + "/credit.zbr.html", model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.FIRST_ENVELOPE
				+ "/credit.tbr.html", model);
	}

	/**
	 * 第一信封评审结果<br/>
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
	@Path(value = "/review/flow/{tpnid}", desc = "第一信封评审结果")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstReview(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封结果开标结果", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", SessionUtils.getTPID());
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
			model.put("SECTION_LIST", getDYXFPSJGViewData());
		}
		// model.putAll(getXypsModelMap());
		model.put("PSOVER", psover);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/"
					+ EOKBFlowCode.FIRST_ENVELOPE + "/review.zbr.html", model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.FIRST_ENVELOPE
				+ "/review.tbr.html", model);
	}

	/**
	 * 获取第一信封评审结果视图<br/>
	 * <p>
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private List<Record<String, Object>> getDYXFPSJGViewData()
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
					.selectList(sqlName + ".getReviewResult", param);
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
	 * 开标记录表一<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/firstRecord", desc = "开标记录表一")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSecondRecord(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("开标记录表一", data));
		String tpid = SessionUtils.getTPID();
		ModelMap model = new ModelMap();

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 是否保存过备注
		param.setColumn("flag", ConstantEOKB.FIRST_REMARK);
		Record<String, Object> tpData = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").get(param);
		boolean remarkflag = true;
		// 查询不到记录说明没有保存过
		if (!CollectionUtils.isEmpty(tpData))
		{
			remarkflag = false;
		}

		model.put("REMARKFLAG", remarkflag);

		model.putAll(PtglUtils.getReviewRecordForKc(tpid, "2-1"));
		// 项目名称
		model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		model.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));

		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.FIRST_ENVELOPE
				+ "/first.record.view.html", model);
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
		ptgl.createDocFileForKcZhpgf(data, tpid, "fjs_ptgl_kcsj_hldjfxyf_v1",
				"firstRecord", "firstEnvelope");
	}
}
