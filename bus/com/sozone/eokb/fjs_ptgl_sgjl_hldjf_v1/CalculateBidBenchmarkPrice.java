/**
 * 包名：com.sozone.eokb.fjs_ptgl_sgjl_hldjf_v1
 * 文件名：CalculateBidBenchmarkPrice.java<br/>
 * 创建时间：2017-9-25 下午3:57:07<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_ptgl_sgjl_hldjf_v1;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
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
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_ptgl.common.PtglUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 计算评标基准价<br/>
 * <p>
 * 计算评标基准价<br/>
 * </p>
 * Time：2017-9-25 下午3:57:07<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fjs_ptgl_sgjl_hldjf_v1/cbbp", desc = "计算评标基准价服务")
// 登录即可访问
@Permission(Level.Authenticated)
public class CalculateBidBenchmarkPrice extends BaseAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(CalculateBidBenchmarkPrice.class);

	/**
	 * 项目CODE
	 */
	private static String projectCode = ConstantEOKB.EOKBBemCode.FJS_PTGL_SGJL_HLDJF_V1;

	/**
	 * 第一信封
	 */
	private static String secondEnvelope = ConstantEOKB.EOKBFlowCode.SECOND_ENVELOPE;

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
	 * 打开评标基准价计算结果页面<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            招标项目流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 打开评标基准价计算结果页面
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "view/{tpnid}", desc = "打开评标基准价计算结果页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openView(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils
				.format("打开评标基准价计算结果页面", tenderProjectNodeID, data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("tpnoid", tenderProjectNodeID);
		// 查询流程节点
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		// 查询确认情况
		Record<String, Object> confirm = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_CHECK_MODEL)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_MODEL_TYPE = 'DEXF_CBSP'").get(param);

		boolean alreadyLaunched = true;
		if (CollectionUtils.isEmpty(confirm))
		{
			alreadyLaunched = false;
		}

		// 是否发起确认标识
		model.put("alreadyLaunched", alreadyLaunched);

		// 如果尚未发起确认,且是投标人
		if ((CollectionUtils.isEmpty(confirm) || !StringUtils.equals("1",
				confirm.getString("V_STATUS"))) && SessionUtils.isBidder())
		{
			// 未发起确认
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/" + secondEnvelope
					+ "/calculate.bsp.tbr.none.html", model);
		}

		// 判断是否已经计算过
		model.put("TENDER_PROJECT_SECTION_LIST",
				PtglUtils.getCalculateViewForJl(tpid, tenderProjectNodeID));

		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			// 如果已经发起确认
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/" + secondEnvelope
					+ "/calculate.bsp.tbr.html", model);
		}
		// 如果招标人或者招标代理
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + secondEnvelope
				+ "/calculate.bsp.zbr.html", model);

	}

	/**
	 * 修改评标基准价<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            招标项目流程节点ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "{tpnid}", desc = "修改评标基准价")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	public void modifyBidBenchmarkPrice(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("修改评标基准价", tenderProjectNodeID, data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		// param.setColumn("tpnid", tenderProjectNodeID);
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		param.setColumn("tpid", tpid);
		// 各标段列表
		List<JSONObject> sms = data.getParams("SECTION_METHODS");
		if (!CollectionUtils.isEmpty(sms))
		{
			String sectionID = null;
			Double price = null;
			for (JSONObject bbp : sms)
			{
				sectionID = bbp.getString("V_BID_SECTION_ID");
				price = bbp.getDouble("N_EVALUATION_PRICE");
				if (null == price)
				{
					continue;
				}
				param.setColumn("sid", sectionID);
				param.setColumn("N_EVALUATION_PRICE", price);
				// 修改标段的评标基准价
				this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
						.setCondition("AND", "V_TPID = #{tpid}")
						.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
						.modify(param);
			}
		}
	}

}
