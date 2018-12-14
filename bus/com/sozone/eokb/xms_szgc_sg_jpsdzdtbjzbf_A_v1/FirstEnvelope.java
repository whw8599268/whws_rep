/**
 * 包名：com.sozone.eokb.xms_szgc_sg_jpsdzdtbjzbf_A_v1
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
package com.sozone.eokb.xms_szgc_sg_jpsdzdtbjzbf_A_v1;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
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
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.LogFormatUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.xms_fjsz.common.FjszUtils;

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
@Path(value = "/xms_szgc_sg_jpsdzdtbjzbf_A_v1/firstenvelope", desc = "第一信封解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class FirstEnvelope extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FirstEnvelope.class);

	/**
	 * 持久化接口
	 */
	protected ActiveRecordDAO activeRecordDAO = null;

	/**
	 * 项目CODE
	 */
	private final String projectCode = ConstantEOKB.EOKBBemCode.XMS_SZGC_SG_JPSDZDTBJZBF_A_V1;

	/**
	 * 第一信封
	 */
	private final String firstEnvelope = ConstantEOKB.EOKBFlowCode.FIRST_ENVELOPE;

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
	 * 第一数字 信封解密情况一览表<br/>
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
	@Path(value = "/credit/flow/{tpnid}", desc = "第一数字 信封解密情况一览表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstEnvelopeDecrypt(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一数字 信封解密情况一览表", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		String tpid = SessionUtils.getTPID();
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		// 获取分页参数
		String page = "1";
		String size = "300";

		if (null != data.getParam("page"))
		{
			page = data.getParam("page");
		}

		if (null != data.getParam("size"))
		{
			size = data.getParam("size");
		}

		model.putAll(FjszUtils.getFirstEnvelopeDecryptPage(tpid,
				Integer.valueOf(page), Integer.valueOf(size)));
		logger.debug(LogUtils.format("成功获取第一数字信封解密情况一览表"));
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/" + firstEnvelope
					+ "/credit.zbr.html", model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + firstEnvelope
				+ "/credit.tbr.html", model);
	}

	/**
	 * 
	 * 生成excel,无返回值<br/>
	 * <p>
	 * 生成excel,无返回值
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/export", desc = "生成excel,无返回值")
	public void exclAssets(AeolusData data) throws FacadeException
	{
		logger.debug(LogFormatUtils.formatOperateMessage("", "生成excel,无返回值",
				data));
		String tpid = SessionUtils.getTPID();
		FjszUtils.createExclAssets(tpid, data);
		logger.debug(LogUtils.format("成功生成excel"));
	}

	/**
	 * 
	 * 开标记录表一<br/>
	 * <p>
	 * 开标记录表一
	 * </p>
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
	public ModelAndView getFirstRecord(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("开标记录表一", data));

		String tpid = SessionUtils.getTPID();

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		ModelMap model = new ModelMap();
		model.putAll(FjszUtils.getOpenBidRecordForm(tpid));

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
		logger.debug(LogUtils.format("成功获取开标记录表一的信息"));
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + firstEnvelope
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
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "/saveBidders", desc = "保存投标人备注")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void saveRemark(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("保存投标人备注", data));

		String json = data.getParam("bidders");
		String tpid = SessionUtils.getTPID();

		if (StringUtils.isNotEmpty(json))
		{
			FjszUtils.saveBidderRemark(data, json, tpid);
		}
		logger.debug(LogUtils.format("成功保存投标人备注"));
	}

	/**
	 * 
	 * 查询信用得分<br/>
	 * <p>
	 * 查询信用得分
	 * </p>
	 * 
	 * @param orgCode
	 *            组织机构号码
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "/queryCredit/{orgCode}", desc = "查询信用得分")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView queryCredit(@PathParam("orgCode") String orgCode,
			AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("查询信用得分", data, orgCode));

		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("orgCode", orgCode);
		Record<String, Object> tender = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BIDDER_ORG_CODE=#{orgCode}").get(param);

		if (CollectionUtils.isEmpty(tender))
		{
			throw new ServiceException("", "未查询到对应的投标人信息");
		}
		ModelMap model = new ModelMap();
		model.putAll(tender);
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + firstEnvelope
				+ "/credit.score.modify.html", model);
	}

	/**
	 * 
	 * 修改信用得分<br/>
	 * <p>
	 * 修改信用得分
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "/updateCredit", desc = "修改信用得分")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void updateCredit(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("修改信用得分", data));

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("ID", data.getParam("ID"));
		Record<String, Object> bidder = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_LIST).get(param);
		if (CollectionUtils.isEmpty(bidder))
		{
			throw new ServiceException("", "未查询到投标人");
		}

		String creditScore = data.getParam("creditScore");
		JSONObject jobj = bidder.getJSONObject("V_JSON_OBJ");
		if (!CollectionUtils.isEmpty(jobj))
		{
			JSONObject credit = jobj.getJSONObject("cerditScore");
			credit.put("N_TOTAL_SCORE", creditScore);
		}
		bidder.put("N_CREDITSCORE", creditScore);
		bidder.put("V_JSON_OBJ", jobj);
		this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST)
				.modify(bidder);
	}
}