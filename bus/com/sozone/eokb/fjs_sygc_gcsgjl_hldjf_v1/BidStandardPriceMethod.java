/**
 * 包名：com.sozone.eokb.fjs_ptgl_gdyh_hldjfxyf_v1
 * 文件名：BidStandardPriceMethod.java<br/>
 * 创建时间：2017-9-20 下午2:00:40<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_sygc_gcsgjl_hldjf_v1;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
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
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_sygc.common.SygcUtils;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 评标基准价计算方法服务<br/>
 * <p>
 * 评标基准价计算方法服务<br/>
 * </p>
 * Time：2017-9-20 下午2:00:40<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fjs_sygc_gcsgjl_hldjf_v1/bspm", desc = "评标基准价计算方法服务")
// 登录即可访问
@Permission(Level.Authenticated)
public class BidStandardPriceMethod extends BaseAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(BidStandardPriceMethod.class);
	/**
	 * 数据库xml
	 */

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
	 * 打开评标基准价计算方法编辑页面<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            招标项目流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 评标基准价计算方法编辑页面
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "view/{tpnid}", desc = "打开评标基准价计算方法编辑页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openView(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开评标基准价计算方法编辑页面", tenderProjectNodeID,
				data));
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
				.setCondition("AND", "V_MODEL_TYPE = 'DEXF_BSPM_YAOHAO'")
				.get(param);

		boolean alreadyLaunched = true;
		if (CollectionUtils.isEmpty(confirm))
		{
			alreadyLaunched = false;
		}

		// 是否发起确认标识
		model.put("alreadyLaunched", alreadyLaunched);

		// 基准价方法信息
		model.put("TENDER_PROJECT_BSPM_LIST", SygcUtils
				.getBidStandardPriceMethodView(tpid, tenderProjectNodeID));
		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			// 如果已经发起确认
			if (!CollectionUtils.isEmpty(confirm)
					&& StringUtils.equals("1", confirm.getString("V_STATUS")))
			{
				return new ModelAndView(
						getTheme(data.getHttpServletRequest())
								+ "/eokb/fjs_sygc_gcsgjl_hldjf_v1/bidOpen/bspm.tbr.html",
						model);
			}
			// 未发起确认
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_sygc_gcsgjl_hldjf_v1/bidOpen/bspm.tbr.none.html",
					model);
		}
		// 如果招标人或者代理以及结束了节点，或者已经公布过信息
		if ((null != tpNode.getInteger("N_STATUS") && 3 == tpNode
				.getInteger("N_STATUS"))
				|| !CollectionUtils.isEmpty(confirm)
				&& StringUtils.equals("1", confirm.getString("V_STATUS")))
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/fjs_sygc_gcsgjl_hldjf_v1/bidOpen/bspm.view.html",
					model);
		}
		// 如果招标人或者招标代理
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/fjs_sygc_gcsgjl_hldjf_v1/bidOpen/bspm.zbr.html", model);
	}

	/**
	 * 打开评标基准价计算方法记录表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 打开评标基准价计算方法记录表
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "record", desc = "打开评标基准价计算方法记录表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openRecord(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("打开评标基准价计算方法记录表", data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		// 基准价方法信息
		model.put("TENDER_PROJECT_BSPM_LIST",
				SygcUtils.getBidStandardPriceMethodView(tpid, ""));
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/fjs_sygc_gcsg_hldjf_v1/bidOpen/bspm.view.html", model);
	}

	/**
	 * 保存标段评标基准价摇号结果<br/>
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
	@Path(value = "{tpnid}", desc = "保存标段评标基准价摇号结果")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyBidStandardPriceMethod(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils
				.format("保存标段评标基准价摇号结果", tenderProjectNodeID, data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnid", tenderProjectNodeID);
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
			String m1no = null;
			String m2no = null;
			String yhno = null;
			Record<String, Object> record = null;
			Record<String, Object> temp = new RecordImpl<String, Object>();
			for (JSONObject bspm : sms)
			{
				temp.clear();
				sectionID = bspm.getString("V_BID_SECTION_ID");
				param.setColumn("sid", sectionID);
				m1no = bspm.getString("METHOD_ONE_NO");
				m2no = bspm.getString("METHOD_TWO_NO");
				yhno = bspm.getString("YAOHAO_RESULT");

				// 如果是空对象
				if (StringUtils.isEmpty(m1no) && StringUtils.isEmpty(m2no)
						&& StringUtils.isEmpty(yhno))
				{
					continue;
				}

				int yhmr = -1;
				String yhr = "";
				if (StringUtils.equals(yhno, m1no))
				{
					yhr = "方法一";
					yhmr = 1;
				}
				else if (StringUtils.equals(yhno, m2no))
				{
					yhr = "方法二";
					yhmr = 2;
				}
				// 如果有值需要修改
				record = this.activeRecordDAO.auto()
						.table(TableName.TENDER_PROJECT_NODE_DATA)
						.setCondition("AND", "V_TPID = #{tpid}")
						.setCondition("AND", "V_TPFN_ID = #{tpnid}")
						.setCondition("AND", "V_BUS_ID = #{sid}").get(param);

				if (!CollectionUtils.isEmpty(record))
				{
					String json = record.getString("V_JSON_OBJ");
					JSONObject jobj = JSON.parseObject(json);
					if (!StringUtils.isEmpty(m1no))
					{
						jobj.put("METHOD_ONE_ADAPTE", true);
						jobj.put("METHOD_ONE", m1no);
					}
					if (!StringUtils.isEmpty(m2no))
					{
						jobj.put("METHOD_TWO_ADAPTE", true);
						jobj.put("METHOD_TWO", m2no);
					}
					jobj.put("YAOHAO_NO", yhno);
					jobj.put("YAOHAO_RESULT", yhr);
					jobj.put("YAOHAO_RESULT_METHOD", yhmr);
					temp.setColumn("ID", record.getString("ID"));
					temp.setColumn("V_JSON_OBJ", jobj.toJSONString());
					this.activeRecordDAO.auto()
							.table(TableName.TENDER_PROJECT_NODE_DATA)
							.modify(temp);
				}
			}
		}
	}

}
