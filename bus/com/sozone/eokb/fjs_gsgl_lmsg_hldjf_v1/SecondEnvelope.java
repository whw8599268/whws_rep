/**
 * 包名：com.sozone.eokb.fjs_gsgl_jdjl_hldfj_v1
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
package com.sozone.eokb.fjs_gsgl_lmsg_hldjf_v1;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
import com.sozone.eokb.bus.benchmark.Utils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.EOKBFlowCode;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_gsgl.common.GsglUtils;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.NumberToCharUtils;
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
@Path(value = "/fjs_gsgl_lmsg_hldjf_v1/secondenvelope", desc = "第二信封解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class SecondEnvelope extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FirstEnvelope.class);

	/**
	 * 项目CODE
	 */
	private static String projectCode = ConstantEOKB.EOKBBemCode.FJS_GSGL_LMSG_HLDJF_V1;

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
	 * @param data
	 *            AeolusData
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/secondRc/flow/{tpnid}", desc = "第二信封文件解密结果")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSecondDecryptSituation(
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

		String tpid = SessionUtils.getTPID();
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		param.put("tpid", tpid);
		param.put("modelType", EOKBFlowCode.DEXF_OFFER);
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
		// 是否发起确认标识
		model.put("alreadyLaunched", alreadyLaunched);
		// 第二数字信封解密结果一览表
		model.putAll(GsglUtils.getSecondDecryptSituation(alreadyLaunched, tpid,
				EOKBFlowCode.DEXF_OFFER));
		logger.debug(LogUtils.format("成功获取第二信封文件解密结果"));
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/"
					+ EOKBFlowCode.SECOND_ENVELOPE + "/decrypt.zbr.html", model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.SECOND_ENVELOPE
				+ "/decrypt.tbr.html", model);
	}

	/**
	 * 获取评标基准价信息<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/secondRs/flow/{tpnid}", desc = "获取评标基准价信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getBenchmarkPrice(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取评标基准价信息", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		param.setColumn("tpid", tpid);
		int count = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND",
						"V_TPID=#{tpid} AND N_EVALUATION_PRICE IS NOT NULL")
				.count(param);

		model.put("BENCHMARK", false);
		if (count > 0)
		{
			model.put("BENCHMARK", true);
		}
		param.put("modelType", EOKBFlowCode.DEXF_PRICE);
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

		// model.putAll(getXypsModelMap());
		model.putAll(GsglUtils.getBenchmarkView(alreadyLaunched, tpid,
				EOKBFlowCode.DEXF_PRICE));
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		logger.debug(LogUtils.format("成功获取评标基准价信息"));
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/"
					+ EOKBFlowCode.SECOND_ENVELOPE + "/benchmark.zbr.html",
					model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.SECOND_ENVELOPE
				+ "/benchmark.tbr.html", model);
	}

	/**
	 * 
	 * 评标基准价计算<br/>
	 * <p>
	 * 评标基准价计算
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/doBasePrice", desc = "评标基准价计算")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void doBasePrice(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("评标基准价计算", data));
		// 依据AID生成所有标段的评标基准价
		Utils.createBasePrice();
	}

	/**
	 * 
	 * 修改投标报价<br/>
	 * <p>
	 * 修改投标报价
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "/modifyPrice", desc = "修改投标报价")
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void modifyPrice(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("修改投标报价", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		String jsonArray = data.getParam("info");
		// 非空才处理
		if (StringUtils.isNotEmpty(jsonArray))
		{
			JSONArray array = JSON.parseArray(jsonArray);
			JSONObject bidder = null;
			for (int i = 0; i < array.size(); i++)
			{
				param.clear();
				bidder = array.getJSONObject(i);
				param.setColumn("ID", bidder.getString("ID"));
				String price = bidder.getString("PRICE").replaceAll(",", "");
				if (!NumberToCharUtils.isNumeric(price))
				{
					throw new ServiceException("", "请输入数字");
				}
				param.setColumn("N_PRICE", price);
				this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST)
						.modify(param);
			}
		}
		logger.debug(LogUtils.format("修改投标报价成功"));
	}

	/**
	 * 
	 * 修改评标基准价<br/>
	 * <p>
	 * 修改评标基准价
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/changeBasePrice", desc = "修改评标基准价")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void changeBasePrice(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("修改评标基准价", data));
		String sectionId = data.getParam("section_id");
		String eprice = data.getParam("eprice");
		if (!NumberToCharUtils.isNumeric(eprice.replaceAll(",", "")))
		{
			throw new ServiceException("", "请输入数字");
		}
		// 依据标段编码和AID修改评标基准价
		String sql = "UPDATE EKB_T_SECTION_INFO SET N_EVALUATION_PRICE=#{eprice} WHERE V_BID_SECTION_ID=#{section_id} AND V_TPID=#{aid}";

		this.activeRecordDAO.sql(sql)
				.setParam("eprice", eprice.replaceAll(",", ""))
				.setParam("section_id", sectionId)
				.setParam("aid", SessionUtils.getTPID()).update();
		logger.debug(LogUtils.format("修改评标基准价成功"));
	}

	/**
	 * 获取第二信封解密结果<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/price/record/{tpnid}", desc = "评标基准价记录")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getBenchmarkPriceRecord(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("评标基准价记录", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.put("tpid", tpid);
		param.put("modelType", EOKBFlowCode.DEXF_PRICE);
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

		model.putAll(GsglUtils.getBenchmarkRecordView(tpid,
				EOKBFlowCode.DEXF_PRICE));
		logger.debug(LogUtils.format("成功评标基准价记录"));
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.SECOND_ENVELOPE
				+ "/benchmark.record.html", model);
	}
}
