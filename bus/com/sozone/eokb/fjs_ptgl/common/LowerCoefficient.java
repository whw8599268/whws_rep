/**
 * 包名：com.sozone.eokb.fjs_ptgl_gcsg_hldjf_v1
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
package com.sozone.eokb.fjs_ptgl.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 下浮系数服务接口<br/>
 * <p>
 * 下浮系数服务接口<br/>
 * </p>
 * Time：2017-9-23 上午10:24:34<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/ptc/lc", desc = "下浮系数服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class LowerCoefficient extends BaseAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(CalculateBidBenchmarkPrice.class);

	/**
	 * 数据库xml
	 */
	private static String mybatisName = ConstantEOKB.EOKBBemCode.FJS_PTGL_COMMON;

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
				.setCondition("AND", "V_MODEL_TYPE = 'DEXF_LOWER_COEFFICIENT'")
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
		// 查询下浮系数JSON
		List<String> lcJsons = this.activeRecordDAO.statement().loadList(
				mybatisName + ".getLowerCoefficientInfos", param);
		// 如果下浮系数为空,说明是第一次进入
		if (CollectionUtils.isEmpty(lcJsons))
		{
			lcJsons = initLCData(tpid, tenderProjectNodeID);
		}
		// 下浮系数视图
		model.putAll(PtglUtils.getLowerCoefficientView(lcJsons));
		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			// 如果已经发起确认
			if (!CollectionUtils.isEmpty(confirm)
					&& StringUtils.equals("1", confirm.getString("V_STATUS")))
			{
				return new ModelAndView(getTheme(data.getHttpServletRequest())
						+ "/eokb/bus/common/lower.coefficient.tbr.html", model);
			}
			// 未发起确认
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/common/lower.coefficient.tbr.none.html", model);
		}
		// 如果招标人或者代理以及结束了节点
		if (null != tpNode.getInteger("N_STATUS")
				&& 3 == tpNode.getInteger("N_STATUS"))
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/common/lower.coefficient.view.html", model);
		}
		// 如果招标人或者招标代理
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/common/lower.coefficient.zbr.html", model);
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
	@Path(value = "viewgroup/{tpnid}", desc = "打开下浮系数抽取页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openGroupView(
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
				.setCondition("AND", "V_MODEL_TYPE = 'DEXF_LOWER_COEFFICIENT'")
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
		// 查询下浮系数JSON
		List<String> lcJsons = this.activeRecordDAO.statement().loadList(
				mybatisName + ".getLowerCoefficientInfos", param);
		// 如果下浮系数为空,说明是第一次进入
		if (CollectionUtils.isEmpty(lcJsons))
		{
			lcJsons = initLCData(tpid, tenderProjectNodeID);
		}
		// 下浮系数视图
		model.putAll(PtglUtils.getLowerCoefficientGroupView(lcJsons));

		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			// 如果已经发起确认
			if (!CollectionUtils.isEmpty(confirm)
					&& StringUtils.equals("1", confirm.getString("V_STATUS")))
			{
				return new ModelAndView(getTheme(data.getHttpServletRequest())
						+ "/eokb/bus/common/lower.group.coefficient.tbr.html",
						model);
			}
			// 未发起确认
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/common/lower.coefficient.tbr.none.html", model);
		}
		// 如果招标人或者代理以及结束了节点
		if (null != tpNode.getInteger("N_STATUS")
				&& 3 == tpNode.getInteger("N_STATUS"))
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/common/lower.group.coefficient.view.html",
					model);
		}
		// 如果招标人或者招标代理
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/common/lower.group.coefficient.zbr.html", model);
	}

	private List<String> initLCData(String tpid, String tenderProjectNodeID)
			throws ServiceException
	{
		List<String> lcJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		// 删除冗余数据,为了防止出现重复
		param.setColumn("flag", ConstantEOKB.LC_BUS_FLAG_TYPE);
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").remove(param);
		param.remove("flag");

		param.setColumn("flag", ConstantEOKB.BSPM_BUS_FLAG_TYPE);
		// 先查评标基准价信息算法JSON
		List<String> bspmJsons = this.activeRecordDAO.statement().loadList(
				mybatisName + ".getBidStandardPriceMethodInfos", param);

		// 如果不存在信息
		if (CollectionUtils.isEmpty(bspmJsons))
		{
			throw new ServiceException("", "无法获取评标基准价抽取结果!");
		}
		// 如果有数据
		JSONObject jobj = null;
		Record<String, Object> lcInfo = new RecordImpl<String, Object>();
		Record<String, Object> record = null;
		String jsonStr = null;
		for (String json : bspmJsons)
		{
			lcInfo.clear();
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "评标基准价计算方法信息为空!");
			}
			jobj = JSON.parseObject(json);

			// 构建下浮信息对象
			record = new RecordImpl<String, Object>();
			lcInfo.setColumn("ID", Random.generateUUID());
			lcInfo.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.LC_BUS_FLAG_TYPE);
			lcInfo.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			lcInfo.setColumn("V_TPID", tpid);
			lcInfo.setColumn("V_TPFN_ID", tenderProjectNodeID);
			lcInfo.setColumn("V_BUS_ID", jobj.getJSONObject("SECTION_INFO")
					.getString("V_BID_SECTION_ID"));
			// 标段信息
			record.setColumn("SECTION_INFO", jobj.getJSONObject("SECTION_INFO"));
			// 投标报价
			record.setColumn("BID_PRICES", jobj.getJSONArray("BID_PRICES"));
			record.setColumn("TOTAL_BIDDER", jobj.getInteger("TOTAL_BIDDER"));
			// 有效投标报价
			record.setColumn("BID_EFFECTIVE_PRICES",
					jobj.getJSONArray("BID_EFFECTIVE_PRICES"));
			record.setColumn("TOTAL_EFFECTIVE_BIDDER",
					jobj.getInteger("TOTAL_EFFECTIVE_BIDDER"));
			// 判断是否为小于15家算法
			if (jobj.getBooleanValue("IS_LESS_THAN_FIFTEEN"))
			{
				record.setColumn("METHOD", -1);
				JSONObject cinfo = jobj.getJSONObject("COEFFICIENT_INFO");
				if (null == cinfo || cinfo.isEmpty())
				{
					throw new ServiceException("", "无法获取小于15家算法中的下浮系数信息!");
				}
				// 判断使用的下浮系数
				Integer ct = cinfo.getInteger("COEFFICIENT_NO");
				List<Record<String, Object>> cl = getCoefficients(ct);
				cinfo.put("LIST_VALUS", cl);
				record.setColumn("COEFFICIENT_INFO", cinfo);
				jsonStr = JSON.toJSONString(record);
				lcInfo.setColumn("V_JSON_OBJ", jsonStr);
				this.activeRecordDAO.auto()
						.table(TableName.TENDER_PROJECT_NODE_DATA).save(lcInfo);
				lcJsons.add(jsonStr);
				continue;
			}
			// 方法三种的算法
			// 判断使用的下浮系数
			Integer ct = jobj.getInteger("YAOHAO_RESULT_METHOD");
			if (null == ct)
			{
				if (jobj.getJSONObject("METHOD_TWO").getBoolean("ADAPTE"))
				{
					ct = 2;
				}
				if (jobj.getJSONObject("METHOD_ONE").getBoolean("ADAPTE"))
				{
					ct = 1;
				}
				if (jobj.getJSONObject("METHOD_THREE").getBoolean("ADAPTE"))
				{
					ct = 3;
				}
			}
			record.setColumn("METHOD", ct);
			Record<String, Object> cinfo = new RecordImpl<String, Object>();
			cinfo.setColumn("COEFFICIENT_NO", ct);
			cinfo.setColumn("MEMO", "抽取评标基准价计算方法：" + ct);
			List<Record<String, Object>> cl = getCoefficients(ct);
			cinfo.put("LIST_VALUS", cl);
			record.setColumn("COEFFICIENT_INFO", cinfo);
			jsonStr = JSON.toJSONString(record);
			lcInfo.setColumn("V_JSON_OBJ", jsonStr);
			this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(lcInfo);
			lcJsons.add(jsonStr);
		}
		return lcJsons;
	}

	/**
	 * 获取下浮系数列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param type
	 * @return
	 */
	private List<Record<String, Object>> getCoefficients(int type)
	{
		List<Record<String, Object>> cl = new LinkedList<Record<String, Object>>();
		float min = 0;
		float max = 0;
		if (1 == type)
		{
			min = 2;
			max = 5;
		}
		else if (2 == type)
		{
			min = -1;
			max = 2;
		}
		else if (3 == type)
		{
			min = -4;
			max = -1;
		}
		Record<String, Object> temp = null;
		for (; min <= max;)
		{
			temp = new RecordImpl<String, Object>();
			temp.setColumn("VALUE", min);
			min += 0.5;
			cl.add(temp);
		}
		return cl;
	}

	/**
	 * 保存下浮系数摇号结果<br/>
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
	@Path(value = "{tpnid}", desc = "保存下浮系数摇号结果")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyLowerCoefficient(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("保存下浮系数摇号结果", tenderProjectNodeID, data));
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
			List<String> indexNOs = new ArrayList<String>();
			String yhno = null;
			String indexNO = null;
			String cv = null;
			Record<String, Object> record = null;
			Record<String, Object> temp = new RecordImpl<String, Object>();
			boolean empty = true;
			for (JSONObject lc : sms)
			{
				temp.clear();
				indexNOs.clear();
				sectionID = lc.getString("V_BID_SECTION_ID");
				param.setColumn("sid", sectionID);
				empty = true;
				for (int i = 0; i < 7; i++)
				{
					indexNO = lc.getString(i + "");
					if (StringUtils.isNotEmpty(indexNO))
					{
						indexNOs.add(indexNO);
						empty = false;
						continue;
					}
					indexNOs.add("");
				}
				yhno = lc.getString("YAOHAO_RESULT");
				// 如果是空对象
				if (empty && StringUtils.isEmpty(yhno))
				{
					continue;
				}
				cv = lc.getString("COEFFCIENT_VALUE");
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
					JSONObject cinfo = jobj.getJSONObject("COEFFICIENT_INFO");
					if (null == cinfo || cinfo.isEmpty())
					{
						throw new ServiceException("", "无法获取小于15家算法中的下浮系数信息!");
					}
					JSONArray vsArray = cinfo.getJSONArray("LIST_VALUS");
					if (null == vsArray || 0 == vsArray.size())
					{
						throw new ServiceException("", "无法获取下浮系数列表!");
					}
					JSONObject vl = null;
					for (int i = 0; i < vsArray.size(); i++)
					{
						vl = vsArray.getJSONObject(i);
						vl.put("NO", indexNOs.get(i));
					}
					jobj.put("YAOHAO_NO", yhno);
					jobj.put("COEFFCIENT_VALUE", cv);
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
