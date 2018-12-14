/**
 * 包名：com.sozone.eokb.fjs_gsgl_ljsg_hldjf_v1
 * 文件名：SecondEnvelope.java<br/>
 * 创建时间：2017-8-29 下午2:05:22<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_ptgl_kcsj_hldjfxyf_v1;

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
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.bus.createFile.CreateFilePTGL;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.EOKBFlowCode;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_ptgl.common.PtglUtils;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 第二信封解密服务接口<br/>
 * <p>
 * 第二信封解密服务接口<br/>
 * </p>
 * Time：2017-8-29 下午2:05:22<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fjs_ptgl_kcsj_hldjfxyf_v1/secondenvelope", desc = "第二信封解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class SecondEnvelope extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(SecondEnvelope.class);

	/**
	 * 项目CODE
	 */
	private static String projectCode = ConstantEOKB.EOKBBemCode.FJS_PTGL_KCSJ_HLDJFXYF_V1;

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
	 * 第二信封文件解密结果<br/>
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
	@Path(value = "/secondRc/flow/{tpnid}", desc = "第二信封文件解密结果")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSecondRc(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第二信封文件解密结果", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		String tpid = SessionUtils.getTPID();
		model.putAll(PtglUtils.getDecryptSuccessCount(tpid));
		// 第二数字信封解密视图
		model.putAll(PtglUtils.getSecondEnvelopeDecryptForKc(tpid));

		// 招标人
		if (!SessionUtils.isBidder())
		{
			// 合理低价法
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/"
					+ EOKBFlowCode.SECOND_ENVELOPE + "/decrypt.zbr.html", model);

		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.SECOND_ENVELOPE
				+ "/decrypt.tbr.html", model);
	}

	/**
	 * 
	 * 有效报价算数平均值计算表<br/>
	 * <p>
	 * 有效报价算数平均值计算表
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/view/{tpnid}", desc = "有效报价算数平均值计算表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openView(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("有效报价算数平均值计算表", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		model.putAll(PtglUtils.getAverageValue(tpid));
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.SECOND_ENVELOPE
				+ "/bspm.zbr.html", model);
	}

	/**
	 * 开标记录表二<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/secondRecord", desc = "开标记录表二")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSecondRecord(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("开标记录表二", data));
		String tpid = SessionUtils.getTPID();
		ModelMap model = new ModelMap();

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 是否保存过备注
		param.setColumn("flag", ConstantEOKB.SECOND_REMARK);
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

		model.putAll(PtglUtils.getSecondBidRecordForKc(tpid));
		// 项目名称
		model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		model.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));

		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.SECOND_ENVELOPE
				+ "/second.record.view.html", model);
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
	public void saveSign(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存投标人备注", data));

		String json = data.getParam("bidders");
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isNotEmpty(json))
		{
			PtglUtils.saveFirstRemark(data, json, tpid, "secondRemark",
					ConstantEOKB.SECOND_REMARK);
		}

		CreateFilePTGL ptgl = new CreateFilePTGL();
		ptgl.createDocFileForKcHldjf(data, tpid, "secondRecord", projectCode);
	}
}
