/**
 * 包名：com.sozone.eokb.fjs_ptgl_gdyh_hldjfxyf_v1
 * 文件名：LowerCoefficient.java<br/>
 * 创建时间：2017-9-23 上午10:24:34<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_sygc_syjc_hldjf_v1;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

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
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.DAOException;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_sygc.common.SygcUtils;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * E值服务接口<br/>
 * <p>
 * E值服务接口<br/>
 * </p>
 * Time：2017-9-23 上午10:24:34<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fjs_sygc_syjc_hldjf_v1/ev", desc = "E值服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class Evalue extends BaseAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Evalue.class);

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
	 * 打开下浮系数抽取页面<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            招标项目流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 打开下浮系数抽取页面
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "view/{tpnid}", desc = "打开下浮系数抽取页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openView(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开下浮系数抽取页面", tenderProjectNodeID, data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);

		// 查询确认情况
		Record<String, Object> confirm = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_CHECK_MODEL)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_MODEL_TYPE = 'DEXF_E_VALUE'")
				.get(param);
		ModelMap model = new ModelMap();

		boolean alreadyLaunched = true;
		if (CollectionUtils.isEmpty(confirm))
		{
			alreadyLaunched = false;
		}

		// 是否发起确认标识
		model.put("alreadyLaunched", alreadyLaunched);

		// 查询流程节点
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		model.put("TENDER_PROJECT_EV_LIST",
				SygcUtils.getEvalueView(tpid, tenderProjectNodeID));
		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			// 如果已经发起确认
			if (!CollectionUtils.isEmpty(confirm)
					&& StringUtils.equals("1", confirm.getString("V_STATUS")))
			{
				return new ModelAndView(
						getTheme(data.getHttpServletRequest())
								+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/e.value.tbr.html",
						model);
			}
			// 未发起确认
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/e.value.tbr.none.html",
					model);
		}
		// 如果招标人或者代理以及结束了节点，或者已经公布过信息
		if ((null != tpNode.getInteger("N_STATUS") && 3 == tpNode
				.getInteger("N_STATUS"))
				|| !CollectionUtils.isEmpty(confirm)
				&& StringUtils.equals("1", confirm.getString("V_STATUS")))
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/e.value.view.html",
					model);
		}
		// 如果招标人或者招标代理
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/e.value.zbr.html",
				model);
	}

	/**
	 * 打开下浮系数抽取记录表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 打开下浮系数抽取记录表
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "record", desc = "打开下浮系数抽取记录表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openRecord(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("打开下浮系数抽取页面", data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}

		ModelMap model = new ModelMap();

		model.put("TENDER_PROJECT_EV_LIST", SygcUtils.getEvalueView(tpid, ""));
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/e.value.view.html",
				model);
	}

	/**
	 * 保存E值系数摇号结果<br/>
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
	@Path(value = "evalue/{tpnid}", desc = "保存E值系数摇号结果")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyEvalue(@PathParam("tpnid") String tenderProjectNodeID,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存E值系数摇号结果", tenderProjectNodeID, data));
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
			List<String> indexOneNOs = new ArrayList<String>();
			List<String> indexTwoNOs = new ArrayList<String>();
			String yhno1 = null;
			String yhno2 = null;
			String indeOneNO = null;
			String indexTwoNO = null;
			String cv1 = null;
			String cv2 = null;
			Record<String, Object> record = null;
			Record<String, Object> temp = new RecordImpl<String, Object>();
			boolean empty = true;
			for (JSONObject ev : sms)
			{
				temp.clear();
				indexOneNOs.clear();
				indexTwoNOs.clear();
				sectionID = ev.getString("V_BID_SECTION_ID");
				param.setColumn("sid", sectionID);
				empty = true;
				for (int i = 0; i < 7; i++)
				{
					indeOneNO = ev.getString(sectionID + "-" + i + "_1");
					indexTwoNO = ev.getString(sectionID + "-" + i + "_2");
					if (StringUtils.isNotEmpty(indeOneNO))
					{
						indexOneNOs.add(indeOneNO);
						empty = false;
					}
					else
					{
						indexOneNOs.add("");
					}
					if (StringUtils.isNotEmpty(indexTwoNO))
					{
						indexTwoNOs.add(indexTwoNO);
						empty = false;
					}
					else
					{
						indexTwoNOs.add("");
					}
				}
				yhno1 = ev.getString("YAOHAO_RESULT_1");
				yhno2 = ev.getString("YAOHAO_RESULT_2");

				// 如果是空对象
				if (empty && StringUtils.isEmpty(yhno1))
				{
					continue;
				}
				// 如果是空对象
				if (empty && StringUtils.isEmpty(yhno2))
				{
					continue;
				}
				cv1 = ev.getString("E_VALUE_1");
				cv2 = ev.getString("E_VALUE_2");
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
					// 获取E值json信息
					JSONObject cinfo = jobj.getJSONObject("EVALUE_INFO");
					if (null == cinfo || cinfo.isEmpty())
					{
						throw new ServiceException("", "无法获取E值信息!");
					}
					// 获取E1信息
					JSONArray vsArray1 = cinfo.getJSONArray("LIST_VALUS_ONE");
					if (null == vsArray1 || 0 == vsArray1.size())
					{
						throw new ServiceException("", "无法获取E1列表!");
					}
					JSONObject vl1 = null;
					for (int i = 0; i < vsArray1.size(); i++)
					{
						vl1 = vsArray1.getJSONObject(i);
						vl1.put("NO", indexOneNOs.get(i));
					}
					// 获取E2信息
					JSONArray vsArray2 = cinfo.getJSONArray("LIST_VALUS_TWO");
					if (null == vsArray2 || 0 == vsArray2.size())
					{
						throw new ServiceException("", "无法获取E2列表!");
					}
					JSONObject vl2 = null;
					for (int i = 0; i < vsArray2.size(); i++)
					{
						vl2 = vsArray2.getJSONObject(i);
						vl2.put("NO", indexTwoNOs.get(i));
					}
					jobj.put("YAOHAO_NO_1", yhno1);
					jobj.put("E_VALUE_1", cv1);
					jobj.put("YAOHAO_NO_2", yhno2);
					jobj.put("E_VALUE_2", cv2);
					temp.setColumn("ID", record.getString("ID"));
					temp.setColumn("V_JSON_OBJ", jobj.toJSONString());
					this.activeRecordDAO.auto()
							.table(TableName.TENDER_PROJECT_NODE_DATA)
							.modify(temp);
				}
			}
		}
	}

	/**
	 * E值记录表<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 打开E值系数抽取页面
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "evalue/record", desc = "E值记录表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView recordView(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("E值记录表", data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		ModelMap model = new ModelMap();

		model.put("TENDER_PROJECT_EV_LIST", SygcUtils.getEValueRecord(tpid));

		// 如果招标人或者招标代理
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/e.value.record.html",
				model);
	}

	/**
	 * 开标记录表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 开标记录表
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "bid/record", desc = "开标记录表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView bidRecordView(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("开标记录表", data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		ModelMap model = new ModelMap();

		model.putAll(SygcUtils.getOpenBidRecordForm(tpid));
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

		// 项目名称
		model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		Date time = DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss");
		model.put("TIME", DateUtils.formatDate(time, "yyyy年MM月dd日 HH时mm分"));

		// 如果招标人或者招标代理
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/bid.record.view.html",
				model);
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
	 * @throws DAOException
	 *             DAOException
	 */
	@Path(value = "/saveBidders", desc = "保存投标人备注")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void saveSign(AeolusData data) throws DAOException
	{
		logger.debug(LogUtils.format("保存投标人备注", data));

		String json = data.getParam("bidders");
		JSONObject jobj = null;
		Record<String, Object> bidder = null;
		Record<String, Object> param = new RecordImpl<String, Object>();
		JSONObject jsonObj = null;
		// 投标人备注
		JSONObject remark = new JSONObject();

		if (StringUtils.isNotEmpty(json))
		{
			JSONArray arr = JSON.parseArray(json);
			for (int i = 0; i < arr.size(); i++)
			{
				jobj = arr.getJSONObject(i);
				for (Entry<String, Object> entry : jobj.entrySet())
				{
					param.clear();
					// 获取投标人ID
					String id = entry.getKey();
					param.setColumn("ID", id);
					bidder = this.activeRecordDAO.auto()
							.table(ConstantEOKB.TableName.EKB_T_TENDER_LIST)
							.get(param);
					if (!CollectionUtils.isEmpty(bidder))
					{
						jsonObj = JSONObject.parseObject(bidder
								.getString("V_JSON_OBJ"));

						remark.clear();
						remark.put("firstRemark", entry.getValue().toString());
						jsonObj.put("remark", remark);

						param.clear();
						param.setColumn("ID", id);
						param.setColumn("V_JSON_OBJ", jsonObj.toString());
						this.activeRecordDAO
								.auto()
								.table(ConstantEOKB.TableName.EKB_T_TENDER_LIST)
								.modify(param);
					}
				}
			}
		}

		// 代理保存后将数据插入节点状态表
		Record<String, Object> tpData = new RecordImpl<String, Object>();
		tpData.setColumn("ID", Random.generateUUID());
		tpData.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.FIRST_REMARK);
		tpData.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
				.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		tpData.setColumn("V_TPID", SessionUtils.getTPID());
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.save(tpData);
	}
}
