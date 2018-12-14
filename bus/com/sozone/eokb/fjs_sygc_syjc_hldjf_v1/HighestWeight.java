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
 * 最高限价权重系数μ服务接口<br/>
 * <p>
 * 最高限价权重系数μ服务接口<br/>
 * </p>
 * Time：2017-9-23 上午10:24:34<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fjs_sygc_syjc_hldjf_v1/hw", desc = "下浮系数服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class HighestWeight extends BaseAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(HighestWeight.class);

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
	 * 打开最高限价权重系数μ抽取页面<br/>
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
	@Path(value = "view/{tpnid}", desc = "打开最高限价权重系数μ抽取页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openView(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开最高限价权重系数μ抽取页面", tenderProjectNodeID,
				data));
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
				.setCondition("AND", "V_MODEL_TYPE = 'DEXF_HIGHEST_WEIGHT'")
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

		// 最高权重视图
		model.put("TENDER_PROJECT_HW_LIST",
				SygcUtils.getHighestWeightView(tpid, tenderProjectNodeID));
		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			// 如果已经发起确认
			if (!CollectionUtils.isEmpty(confirm)
					&& StringUtils.equals("1", confirm.getString("V_STATUS")))
			{
				return new ModelAndView(
						getTheme(data.getHttpServletRequest())
								+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/highest.weight.tbr.html",
						model);
			}
			// 未发起确认
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/highest.weight.tbr.none.html",
					model);
		}
		// 如果招标人或者代理以及结束了节点，或者已经公布过信息
		if ((null != tpNode.getInteger("N_STATUS") && 3 == tpNode
				.getInteger("N_STATUS"))
				|| !CollectionUtils.isEmpty(confirm)
				&& StringUtils.equals("1", confirm.getString("V_STATUS")))
		{
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/highest.weight.view.html",
					model);
		}
		// 如果招标人或者招标代理
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/highest.weight.zbr.html",
				model);
	}

	/**
	 * 打开最高限价权重系数μ抽取记录表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return ModelAndView 打开最高限价权重系数μ抽取记录表
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "record", desc = "打开最高限价权重系数μ抽取记录表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openRecord(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("打开最高限价权重系数μ抽取记录表", data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		ModelMap model = new ModelMap();

		// 最高权重视图
		model.put("TENDER_PROJECT_HW_LIST",
				SygcUtils.getHighestWeightView(tpid, ""));
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/fjs_sygc_syjc_hldjf_v1/bidOpen/highest.weight.view.html",
				model);
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
			for (JSONObject hw : sms)
			{
				temp.clear();
				indexNOs.clear();
				sectionID = hw.getString("V_BID_SECTION_ID");
				param.setColumn("sid", sectionID);
				empty = true;
				for (int i = 0; i < 7; i++)
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
				cv = hw.getString("WEIGHT_VALUE");
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
					JSONObject cinfo = jobj.getJSONObject("WEIGHT_INFO");
					if (null == cinfo || cinfo.isEmpty())
					{
						throw new ServiceException("", "无法获取最高权重信息!");
					}
					JSONArray vsArray = cinfo.getJSONArray("LIST_VALUS");

					JSONObject vl = null;
					for (int i = 0; i < vsArray.size(); i++)
					{
						vl = vsArray.getJSONObject(i);
						vl.put("NO", indexNOs.get(i));
					}
					jobj.put("YAOHAO_NO", yhno);
					jobj.put("WEIGHT_VALUE", cv);
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
