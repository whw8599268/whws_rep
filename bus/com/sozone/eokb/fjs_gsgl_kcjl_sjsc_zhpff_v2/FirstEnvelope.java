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
package com.sozone.eokb.fjs_gsgl_kcjl_sjsc_zhpff_v2;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
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
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.EOKBFlowCode;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_gsgl.common.GsglUtils;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.ListSortUtils;
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
@Path(value = "/fjs_gsgl_kcjl_sjsc_zhpff_v2/firstenvelope", desc = "第一信封解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class FirstEnvelope extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FirstEnvelope.class);
	/**
	 * 项目CODE
	 */
	private static String projectCode = ConstantEOKB.EOKBBemCode.FJS_GSGL_KCJL_SJSC_ZHPFF_V2;

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
	 * 第一信封解密情况<br/>
	 * <p>
	 * 第一信封解密情况
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/credit/flow/{tpnid}", desc = "第一信封解密情况")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstCredit(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封解密情况", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		String tpid = SessionUtils.getTPID();
		model.putAll(GsglUtils.getDecryptSuccessCountNoGroup(tpid));
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		param.put("tpid", tpid);
		param.put("modelType", EOKBFlowCode.DYXF_CREDIT);
		// 查询是否已经发起过确认
		Record<String, Object> cm = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_CHECK_MODEL)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND",
						"V_STATUS = '1' AND V_MODEL_TYPE = #{modelType}")
				.get(param);
		boolean alreadyLaunched = true;
		if (CollectionUtils.isEmpty(cm))
		{
			alreadyLaunched = false;
		}
		model.put("alreadyLaunched", alreadyLaunched);
		model.putAll(GsglUtils.getfirstDecryptSituationForZhpgf(tpid,
				alreadyLaunched, EOKBFlowCode.DYXF_CREDIT));
		logger.debug(LogUtils.format("获取第一信封解密情况成功"));
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
	 * 修改投标人信用等级<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/mbcr", desc = "修改投标人信用等级")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void modifyBidderCreditRating(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("修改投标人信用等级", data));
		String bidderCreditRatings = data.getParam("BIDDER_CREDIT_RATINGS");
		String tpid = SessionUtils.getTPID();

		// 非空才处理
		if (StringUtils.isNotEmpty(bidderCreditRatings))
		{
			GsglUtils.modifyBidderCreditRatingNoGroup(
					new ArrayList<Record<String, Object>>(), tpid,
					bidderCreditRatings);
		}
		logger.debug(LogUtils.format("修改投标人信用等级成功"));
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
		Record<String, Object> model = new RecordImpl<String, Object>();
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
			model.putAll(GsglUtils.getfirstReviewView(tpid));
			// 获取各个标段的投标人并且将他们按照评审得分降序
			List<Record<String, Object>> sections = model
					.getList("SECTION_LIST");
			List<Record<String, Object>> bidders = null;
			JSONObject json = null;
			for (Record<String, Object> section : sections)
			{
				bidders = section.getList("TENDER_LIST");
				if (CollectionUtils.isEmpty(bidders))
				{
					continue;
				}
				// 保存投标人排名
				for (Record<String, Object> bidder : bidders)
				{
					Record<String, Object> temp = this.activeRecordDAO
							.pandora().SELECT("ID,V_JSON_OBJ")
							.FROM(TableName.EKB_T_TENDER_LIST)
							.EQUAL("ID", bidder.getString("V_BIDDER_ID")).get();

					json = temp.getJSONObject("V_JSON_OBJ");
					json.put("N_TOTAL", bidder.getDouble("N_TOTAL"));
					temp.setColumn("V_JSON_OBJ", json.toJSONString());
					this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST).modify(temp);
				}

				ListSortUtils.sort(bidders, false, "N_TOTAL");
				section.setColumn("TENDER_LIST", bidders);
			}
			model.put("SECTION_LIST", sections);
		}

		// model.putAll(getXypsModelMap());
		model.put("PSOVER", psover);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		logger.debug(LogUtils.format("获取第一信封结果开标结果成功"));
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
	 * 第一信封结束进入评审<br/>
	 * 
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/first/end", desc = "第一信封结束进入评审")
	// GET访问方式
	@Service
	public void beginReview() throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封结束进入评审"));
		// 设置项目ID
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 设置修改条件
		param.setColumn("tpid", tpid);
		// 评标状态0:未评标,2-1:第一信封评标完成,2:评标完成,10:评标终止\r\n
		param.setColumn("V_BID_OPEN_STATUS", EOKBFlowCode.FIRST_OVER);
		this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.modify(param);
		logger.debug(LogUtils.format("第一信封结束进入评审成功"));
	}

	/**
	 * 结束开标<br/>
	 * 
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/firstRs/end", desc = "结束开标")
	// GET访问方式
	@Service
	public void finishBid() throws FacadeException
	{
		logger.debug(LogUtils.format("结束开标"));
		// 设置项目ID
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 设置修改条件
		param.setColumn("tpid", tpid);
		// 评标状态0:未评标,2-1:第一信封评标完成,2:评标完成,10:评标终止\r\n
		param.setColumn("V_BID_OPEN_STATUS", EOKBFlowCode.BID_OVER);
		this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.modify(param);
		logger.debug(LogUtils.format("结束开标成功"));
	}
}
