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
package com.sozone.eokb.xms_fwjz_sg_jpsdzdtbjzbf_A_v1;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
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
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
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
import com.sozone.eokb.utils.ListSortUtils;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.xms_fjsz.common.FjszUtils;

/**
 * 评标前准备服务接口<br/>
 * <p>
 * 评标前准备服务接口<br/>
 * </p>
 * Time：2017-8-28 下午2:13:39<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/xms_fwjz_sg_jpsdzdtbjzbf_A_v1/prebid", desc = "评标前准备服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class Prebid extends BaseAction
{

	private static final NumberFormat FMT_D = new DecimalFormat("###,##0.00",
			new DecimalFormatSymbols());

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Prebid.class);

	private static String sqlName = ConstantEOKB.EOKBBemCode.XMS_FWJZ_SG_JPSDZDTBJZBF_A_V1
			+ "_prebid";

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
	 * 抽取K值<br/>
	 * <p>
	 * 抽取K值
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
	// 定义路径
	@Path(value = "/chouquK/flow/{tpnid}", desc = "抽取K值")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView chouquK(@PathParam("tpnid") String tenderProjectNodeID,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("抽取K值", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		String status = data.getParam("status");
		if (StringUtils.isEmpty(status))
		{
			status = "";
		}
		param.setColumn("type", ConstantEOKB.K_VALUE + status);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		Record<String, Object> section = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").get(param);
		JSONObject jobj = section.getJSONObject("V_JSON_OBJ");
		if (CollectionUtils.isEmpty(jobj))
		{
			throw new FacadeException("", "获取标段扩展信息失败");
		}
		Double maxK = jobj.getDouble("MAX_K");
		Double minK = jobj.getDouble("MIN_K");
		if (null == maxK || null == minK)
		{
			throw new FacadeException("", "无法获取到k值信息");
		}
		// 查询k值JSON
		List<String> kJsons = this.activeRecordDAO.statement().loadList(
				sqlName + ".getKvalue", param);
		// 如果k值JSON为空,说明是初始化数据
		if (CollectionUtils.isEmpty(kJsons))
		{
			if (StringUtils.equals("1", status))
			{
				kJsons = FjszUtils.initDataK1(tpid, tenderProjectNodeID, maxK,
						minK);
			}
			else if (StringUtils.equals("2", status))
			{
				kJsons = FjszUtils.initDataK2(tpid, tenderProjectNodeID, maxK,
						minK);
			}
			else if (StringUtils.equals("3", status))
			{
				kJsons = FjszUtils.initDataK3(tpid, tenderProjectNodeID, maxK,
						minK);
			}
			else
			{
				kJsons = FjszUtils.initDataK4(tpid, tenderProjectNodeID, maxK,
						minK);
			}

		}

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		model.put("KINFOS", FjszUtils.getKValueView(kJsons));
		model.put("MAX_K", maxK);
		model.put("MIN_K", minK);
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/k_value/chouquK" + (status == null ? "" : status)
				+ ".html", model);
	}

	/**
	 * 保存摇号结果<br/>
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
	@Path(value = "first/{tpnid}", desc = "保存摇号结果")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyKvalue(@PathParam("tpnid") String tenderProjectNodeID,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存摇号结果", tenderProjectNodeID, data));
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
			for (JSONObject hw : sms)
			{
				temp.clear();
				indexNOs.clear();
				sectionID = hw.getString("V_BID_SECTION_ID");
				param.setColumn("sid", sectionID);
				empty = true;
				// 如果有值需要修改
				record = this.activeRecordDAO.auto()
						.table(TableName.TENDER_PROJECT_NODE_DATA)
						.setCondition("AND", "V_TPID = #{tpid}")
						.setCondition("AND", "V_TPFN_ID = #{tpnid}")
						.setCondition("AND", "V_BUS_ID = #{sid}").get(param);
				if (CollectionUtils.isEmpty(record))
				{
					throw new ServiceException("", "未查询到K值初始化信息");
				}
				String json = record.getString("V_JSON_OBJ");
				JSONObject jobj = JSON.parseObject(json);
				JSONObject cinfo = jobj.getJSONObject("K_INFO");
				if (null == cinfo || cinfo.isEmpty())
				{
					throw new ServiceException("", "无法获取k值信息!");
				}
				JSONArray vsArray = cinfo.getJSONArray("K_VALUES");
				for (int i = 0; i <= vsArray.size(); i++)
				{
					indexNO = hw.getString(i + "");
					if (StringUtils.isNotEmpty(indexNO))
					{
						indexNOs.add(indexNO);
						empty = false;
						continue;
					}
					indexNOs.add("");
				}

				yhno = hw.getString("YAOHAO_RESULT");
				// 如果是空对象
				if (empty && StringUtils.isEmpty(yhno))
				{
					continue;
				}
				cv = hw.getString(ConstantEOKB.K_VALUE);

				if (!CollectionUtils.isEmpty(record))
				{

					JSONObject vl = null;
					for (int i = 0; i < vsArray.size(); i++)
					{
						vl = vsArray.getJSONObject(i);
						vl.put("NO", indexNOs.get(i));
					}
					jobj.put("YAOHAO_NO", yhno);
					jobj.put(ConstantEOKB.K_VALUE, cv);
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
	 * 
	 * 基准价主页<br/>
	 * <p>
	 * 基准价主页
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
	// 定义路径
	@Path(value = "/benchmark/flow/{tpnid}", desc = "基准价主页")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView benchmarkIndex(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("基准价主页", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("type", ConstantEOKB.BENCHMARK);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 查询评标基准价JSON
		List<String> bkJsons = this.activeRecordDAO.statement().loadList(
				sqlName + ".getBenchmark", param);

		List<Record<String, Object>> bkInfos = null;
		if (CollectionUtils.isEmpty(bkJsons))
		{
			bkJsons = FjszUtils.initBenchmarkData(tpid, tenderProjectNodeID);
		}

		bkInfos = new LinkedList<Record<String, Object>>();
		Record<String, Object> bkInfo = null;
		JSONObject jobj = null;
		for (String json : bkJsons)
		{
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "基准价信息为空!");
			}
			jobj = JSON.parseObject(json);
			bkInfo = new RecordImpl<String, Object>();
			bkInfo.putAll(jobj);

			bkInfos.add(bkInfo);
		}

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		model.put("BKINFOS", bkInfos);
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/xms_fwjz_sg_jpsdzdtbjzbf_A_v1/prebid/benchmark.index.html",
				model);
	}

	/**
	 * 
	 * 基准价计算<br/>
	 * <p>
	 * 基准价计算
	 * </p>
	 * 
	 * @param sid
	 *            标段主键
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/benchmark/flow/{tpnid}/{sid}", desc = "基准价计算")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView benchmarkCaculate(@PathParam("sid") String sid,
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("基准价计算", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("type", ConstantEOKB.BENCHMARK);
		param.setColumn("tpid", tpid);
		param.setColumn("sid", sid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 查询评标基准价JSON
		String bkJsons = this.activeRecordDAO.statement().getOne(
				sqlName + ".getBenchmarkBySid", param);

		Record<String, Object> bkInfo = null;
		JSONObject jobj = null;
		// 如果json为空
		if (StringUtils.isEmpty(bkJsons))
		{
			throw new ServiceException("", "基准价信息为空!");
		}
		jobj = JSON.parseObject(bkJsons);
		bkInfo = new RecordImpl<String, Object>();
		bkInfo.putAll(jobj);

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		model.put("BKINFO", bkInfo);
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/xms_fwjz_sg_jpsdzdtbjzbf_A_v1/prebid/benchmark.edit.html",
				model);
	}

	/**
	 * 
	 * 获取入围投标人<br/>
	 * <p>
	 * 获取入围投标人
	 * </p>
	 * 
	 * @param sid
	 *            标段主键
	 * @param status
	 *            入围状态
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/getPreTenderList/{sid}/{status}", desc = "获取入围投标人")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getPreTenderList(
			@PathParam("sid") String sid, @PathParam("status") String status,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取符合搜索条件的入围投标人列表分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目");
		}

		param.put("tpid", tpid);
		param.put("sid", sid);
		param.put("status", status);
		param.put("TYPE", ConstantEOKB.BENCHMARK);
		param.put("bidderNo", data.getParam("V_BIDDER_NO"));
		Page<Record<String, Object>> page = null;
		if (StringUtils.equals("77", status))
		{
			page = this.activeRecordDAO.statement().selectPage(
					sqlName + ".getPreTenderListLimit", pageable, param);
		}
		else
		{
			page = this.activeRecordDAO.statement().selectPage(
					sqlName + ".getPreTenderList", pageable, param);
		}
		List<Record<String, Object>> list = page.getContent();
		for (Record<String, Object> record : list)
		{
			if (record.containsKey("V_JSON")
					&& StringUtils.isNotEmpty(record.getString("V_JSON")))
			{
				JSONObject json = JSONObject.parseObject(record
						.getString("V_JSON"));
				record.setColumn("SCOPE", json.getString("SCOPE"));
				record.setColumn("BENCHMARK", json.getString("BENCHMARK"));
			}
			record.setColumn("SCORE", record.getFloat("N_CREDITSCORE"));
			record.setColumn("N_PRICE",
					FMT_D.format(record.getDouble("N_PRICE")));
		}
		return page;
	}

	/**
	 * 
	 * 获取符合搜索条件的入围投标人列表分页封装<br/>
	 * <p>
	 * 获取符合搜索条件的入围投标人列表分页封装
	 * </p>
	 * 
	 * @param sid
	 *            标段主键
	 * @param status
	 *            入围状态
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/getBenchamarkList/{sid}/{status}", desc = "获取符合搜索条件的入围投标人列表分页封装")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getBenchamarkList(
			@PathParam("sid") String sid, @PathParam("status") String status,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取符合搜索条件的入围投标人列表分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目");
		}

		param.put("tpid", tpid);
		param.put("sid", sid);
		param.put("status", status);
		param.put("TYPE", ConstantEOKB.BENCHMARK);
		param.put("bidderNo", data.getParam("V_BIDDER_NO"));
		Page<Record<String, Object>> page = this.activeRecordDAO.statement()
				.selectPage(sqlName + ".getBencharkList", pageable, param);
		List<Record<String, Object>> list = page.getContent();
		for (Record<String, Object> record : list)
		{
			if (record.containsKey("V_JSON")
					&& StringUtils.isNotEmpty(record.getString("V_JSON")))
			{
				JSONObject json = JSONObject.parseObject(record
						.getString("V_JSON"));
				record.setColumn("SCOPE", json.getString("SCOPE"));
				record.setColumn("BENCHMARK", json.getString("BENCHMARK"));
			}
			record.setColumn("SCORE", record.getFloat("N_CREDITSCORE"));
			record.setColumn("N_PRICE",
					FMT_D.format(record.getDouble("N_PRICE")));
		}
		return page;
	}

	/**
	 * 
	 * 数据重新排序<br/>
	 * <p>
	 * </p>
	 * 
	 * @param list
	 *            数据源
	 * @param key
	 *            要排序的值所在的KEY值
	 * @param orderValue
	 *            参数数组
	 * @param isAscArr
	 *            字段正序还是逆序 true升序，false降序
	 * @return 重新排序后的投标人列表
	 */
	public static List<Record<String, Object>> orderList(
			List<Record<String, Object>> list, String key, String[] orderValue,
			boolean[] isAscArr)
	{
		// 将唱标摘要引入对象
		for (Record<String, Object> r : list)
		{
			JSONObject json = JSONObject.parseObject(r.getString("V_JSON_OBJ"));
			JSONObject objUser = getObjSing(json.getJSONArray(key));
			r.putAll(objUser);
		}
		ListSortUtils.sort(list, orderValue, isAscArr);
		return list;
	}

	/**
	 * 
	 * 读取唱标摘要<br/>
	 * <p>
	 * 读取唱标摘要
	 * </p>
	 * 
	 * @param objSings
	 *            唱标摘要数组
	 * @return 唱标摘要
	 */
	public static JSONObject getObjSing(JSONArray objSings)
	{
		JSONObject objSing = new JSONObject();

		for (int i = 0; i < objSings.size(); i++)
		{
			JSONObject obj = objSings.getJSONObject(i);
			objSing.putAll(obj);
		}
		return objSing;
	}

	/**
	 * 
	 * 打开抽取投标人页面<br/>
	 * <p>
	 * 打开抽取投标人页面
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
	// 定义路径
	@Path(value = "/preTenderList/flow/{tpnid}", desc = "打开抽取投标人页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView preTenderList(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开抽取投标人页面", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		for (Record<String, Object> section : sections)
		{
			String sectionId = section.getString("V_BID_SECTION_ID");
			param.setColumn("sid", sectionId);
			// 查询
			Record<String, Object> tpDate = this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA)
					.setCondition("AND", "V_TPFN_ID=#{tpnoid}")
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BUS_ID=#{sid}").get(param);

			if (CollectionUtils.isEmpty(tpDate))
			{
				tpDate = extract(tpid, tenderProjectNodeID, sectionId);
			}

			int method = FjszUtils.chouMethod(tpid, sectionId, "1");
			// method为1时小于等于50家，全部入围
			if (method == 1)
			{
				param.setColumn("sid", sectionId);
				param.setColumn("status", 1);
				this.activeRecordDAO
						.sql("UPDATE EKB_T_TENDER_LIST SET N_ENVELOPE_9=#{status} WHERE V_TPID=#{tpid} AND V_BID_SECTION_ID=#{sid} AND N_ENVELOPE_9=0")
						.build(param).update();
			}

			section.setColumn("METHOD", method);
			section.setColumn("STATUS", tpDate.getString("V_BUS_FLAG_TYPE"));
		}
		model.put("SECTIONS", sections);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/xms_fwjz_sg_jpsdzdtbjzbf_A_v1/prebid/preTenderList.index.html",
				model);
	}

	/**
	 * 
	 * 修改入围状态<br/>
	 * <p>
	 * 修改入围状态
	 * </p>
	 * 
	 * @param id
	 *            投标人主键
	 * @param status
	 *            入围状态
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "beSelected/{id}/{status}", desc = "修改入围状态")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyBidder(@PathParam("id") String id,
			@PathParam("status") String status, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("修改入围状态", status, data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("ID", id);
		param.setColumn("N_ENVELOPE_9", status);
		this.activeRecordDAO
				.sql("UPDATE EKB_T_TENDER_LIST SET N_ENVELOPE_9 = #{N_ENVELOPE_9} WHERE ID=#{ID}")
				.build(param).update();
	}

	/**
	 * 
	 * 获取入围数量<br/>
	 * <p>
	 * 获取入围数量
	 * </p>
	 * 
	 * @param sid
	 *            标段ID
	 * @param status
	 *            入围状态
	 * @param data
	 *            AeolusData
	 * @return 入围数量
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "count/{sid}/{status}", desc = "获取入围数量")
	// PUT访问方式
	@HttpMethod(HttpMethod.GET)
	public int getCount(@PathParam("sid") String sid,
			@PathParam("status") String status, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取入围数量", data));
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("sid", sid);
		param.setColumn("status", status);
		int n = this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "N_ENVELOPE_9=#{status}")
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_SECTION_ID=#{sid}").count(param);
		return n;
	}

	/**
	 * 
	 * 初始化抽取状态<br/>
	 * <p>
	 * 初始化抽取状态
	 * </p>
	 * 
	 * @param tpid
	 * @param tenderProjectNodeID
	 * @param sid
	 * @return
	 * @throws ServiceException
	 */
	private Record<String, Object> extract(String tpid,
			String tenderProjectNodeID, String sid) throws ServiceException
	{
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("ID", Random.generateUUID());
		record.setColumn("V_BUS_FLAG_TYPE", "0");
		record.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
				.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		record.setColumn("V_TPID", tpid);
		record.setColumn("V_TPFN_ID", tenderProjectNodeID);
		record.setColumn("V_BUS_ID", sid);
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.save(record);

		return record;
	}

	/**
	 * 
	 * 修改抽取状态<br/>
	 * <p>
	 * 修改抽取状态
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param sid
	 *            标段ID
	 * @param status
	 *            入围状态
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "extract/{tenderProjectNodeID}/{sid}/{status}", desc = "修改抽取状态")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyExtract(
			@PathParam("tenderProjectNodeID") String tenderProjectNodeID,
			@PathParam("status") String status, @PathParam("sid") String sid,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("修改抽取状态", status, data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("V_TPFN_ID", tenderProjectNodeID);
		param.setColumn("V_TPID", tpid);
		param.setColumn("V_BUS_ID", sid);
		param.setColumn("V_BUS_FLAG_TYPE", status);
		this.activeRecordDAO
				.sql("UPDATE EKB_T_TPFN_DATA_INFO SET V_BUS_FLAG_TYPE = #{V_BUS_FLAG_TYPE} WHERE V_TPFN_ID=#{V_TPFN_ID} AND V_TPID=#{V_TPID} AND V_BUS_ID=#{V_BUS_ID}")
				.build(param).update();
	}

	/**
	 * 
	 * 打开入围投标人页面<br/>
	 * <p>
	 * 打开入围投标人页面
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
	// 定义路径
	@Path(value = "/preTenderListResult/flow/{tpnid}", desc = "打开入围投标人页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView preTenderListResult(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开入围投标人页面", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);

		model.put("SECTIONS", sections);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/xms_fwjz_sg_jpsdzdtbjzbf_A_v1/prebid/preTenderList.result.html",
				model);
	}

	/**
	 * 
	 * 获取入围投标人<br/>
	 * <p>
	 * 获取入围投标人
	 * </p>
	 * 
	 * @param sid
	 *            标段主键
	 * @param price
	 *            控制价
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/getPreTenderListResult/{sid}/{price}", desc = "获取入围投标人")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getPreTenderResultList(
			@PathParam("sid") String sid, @PathParam("price") String price,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取符合搜索条件的入围投标人列表分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目");
		}

		param.put("tpid", tpid);
		param.put("sid", sid);
		param.put("price", price);
		param.put("type", ConstantEOKB.BENCHMARK);
		Page<Record<String, Object>> page = null;
		page = this.activeRecordDAO.statement().selectPage(
				sqlName + ".getPreTenderListResult", pageable, param);
		List<Record<String, Object>> list = page.getContent();

		// 2018.03.28 wengdm 投标报价与评标基准价的绝对值出现小数点后多位，强制转换成小数点后两位
		NumberFormat fmt = new DecimalFormat("#####0.00",
				new DecimalFormatSymbols());
		for (Record<String, Object> record : list)
		{
			if (record.containsKey("V_JSON")
					&& StringUtils.isNotEmpty(record.getString("V_JSON")))
			{
				JSONObject json = JSONObject.parseObject(record
						.getString("V_JSON"));
				record.setColumn("SCOPE", json.getString("SCOPE"));
				record.setColumn("BENCHMARK", json.getString("BENCHMARK"));
				record.setColumn("ABSOLUTE",
						fmt.format(record.getDouble("ABSOLUTE")));
			}
			record.setColumn("SCORE", record.getFloat("N_CREDITSCORE"));
			record.setColumn("N_PRICE",
					FMT_D.format(record.getDouble("N_PRICE")));
		}
		return page;
	}

	/**
	 * 
	 * 打开进入评审的人员名单页面<br/>
	 * <p>
	 * 打开进入评审的人员名单页面
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
	// 定义路径
	@Path(value = "/reviewTenderList/flow/{tpnid}", desc = "打开进入评审的人员名单页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getReviewTender(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开进入评审的人员名单页面", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);

		model.put("SECTIONS", sections);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/xms_fwjz_sg_jpsdzdtbjzbf_A_v1/prebid/reviewTenderList.html",
				model);
	}

	/**
	 * 
	 * 获取进入评审的人员名单<br/>
	 * <p>
	 * 获取进入评审的人员名单
	 * </p>
	 * 
	 * @param sid
	 *            标段主键
	 * @param price
	 *            评标基准价
	 * @param data
	 *            AeolusData
	 * @return 进入评审的人员名单
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/getReviewTenderList/{sid}/{price}", desc = "获取进入评审的人员名单")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getReviewTenderList(
			@PathParam("sid") String sid, @PathParam("price") String price,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取符合搜索条件的入围投标人列表分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目");
		}

		param.put("tpid", tpid);
		param.put("sid", sid);
		param.put("price", price);
		Page<Record<String, Object>> page = null;
		page = this.activeRecordDAO.statement().selectPage(
				sqlName + ".getReviewTenderList", pageable, param);
		List<Record<String, Object>> list = page.getContent();
		Double absolute = -1d;
		int rank = 0;

		// 2018.03.28 wengdm 投标报价与评标基准价的绝对值出现小数点后多位，强制转换成小数点后两位
		NumberFormat fmt = new DecimalFormat("#####0.00",
				new DecimalFormatSymbols());
		for (Record<String, Object> record : list)
		{
			if (absolute.compareTo(record.getDouble("ABSOLUTE")) != 0)
			{
				rank++;
			}
			record.setColumn("SCORE", record.getFloat("N_CREDITSCORE"));
			record.setColumn("RANK", rank);
			record.setColumn("N_PRICE",
					FMT_D.format(record.getDouble("N_PRICE")));
			record.setColumn("ABSOLUTE",
					fmt.format(record.getDouble("ABSOLUTE")));
			absolute = record.getDouble("ABSOLUTE");
		}
		return page;
	}

	/**
	 * 
	 * 获取全部人员名单<br/>
	 * <p>
	 * 获取全部人员名单
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/getTenderList", desc = "获取全部人员名单")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getTenderList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取符合搜索条件的入围投标人列表分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目");
		}

		param.put("tpid", tpid);
		Page<Record<String, Object>> page = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid} AND N_ENVELOPE_0=1")
				.page(pageable, param);
		return page;
	}
}
