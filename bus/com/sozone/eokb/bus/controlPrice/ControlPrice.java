/**
 * 包名：com.sozone.eokb.bus.controlPrice
 * 文件名：ControlPrice.java<br/>
 * 创建时间：2017-12-19 下午3:59:21<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.controlPrice;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
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
import com.sozone.eokb.bus.bidlog.BidOperationLog;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 控制价服务接口<br/>
 * <p>
 * 控制价服务接口<br/>
 * </p>
 * Time：2017-12-19 下午3:59:21<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/controlprice", desc = "控制价服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class ControlPrice extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(ControlPrice.class);

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
	 * 获取全部标段信息<br/>
	 * <p>
	 * 获取全部标段信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 标段信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "infos", desc = "获取全部标段信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSectionInfos(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取全部标段信息", data));
		String tpid = SessionUtils.getTPID();
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);

		if (CollectionUtils.isEmpty(sections))
		{
			throw new FacadeException("", "找不到对应得项目信息");
		}
		boolean showK = false;
		boolean showQ = false;
		String minK = null;
		String maxK = null;
		String q1 = null;
		String q2 = null;
		int methodType = SessionUtils.getEvaluationMethodType();

		JSONObject jobj = null;
		for (Record<String, Object> section : sections)
		{

			// 56:经A；57经B；58综A；59综B
			if (56 <= methodType && methodType <= 59)
			{
				jobj = section.getJSONObject("V_JSON_OBJ");
				minK = jobj.getString("MIN_K");
				maxK = jobj.getString("MAX_K");
				q1 = jobj.getString("Q2");
				q2 = jobj.getString("Q1");

				// K值信息
				if (StringUtils.isNotEmpty(minK)
						&& StringUtils.isNotEmpty(maxK))
				{
					showK = true;
					section.setColumn("MIN_K", jobj.getString("MIN_K"));
					section.setColumn("MAX_K", jobj.getString("MAX_K"));

				}
				// Q值信息
				if (StringUtils.isNotEmpty(q1) && StringUtils.isNotEmpty(q2))
				{
					showQ = true;
					section.setColumn("Q1", jobj.getString("Q1"));
					section.setColumn("Q2", jobj.getString("Q2"));
				}
			}
			section.setColumn("SHOW_Q", showQ);
			section.setColumn("SHOW_K", showK);
		}
		model.put("SECTIONS", sections);
		model.put("SHOW_K", showK);
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/controlprice/control.price.html", model);
	}

	/**
	 * 
	 * 获取全部标段信息<br/>
	 * <p>
	 * 获取全部标段信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 全部标段信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "infosForGsgl", desc = "获取全部标段信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSectionInfosForGsgl(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取全部标段信息", data));
		String tpid = SessionUtils.getTPID();
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);

		if (CollectionUtils.isEmpty(sections))
		{
			throw new FacadeException("", "找不到对应得项目信息");
		}
		model.put("SECTIONS", sections);
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/controlprice/control.price.gsgl.html", model);
	}

	/**
	 * 
	 * 保存控制价（高速公路）<br/>
	 * <p>
	 * 保存控制价
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/savePricesForGsgl", desc = "保存控制价（高速公路）")
	@HttpMethod(HttpMethod.POST)
	public void savePricesForGsgl(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存控制价（高速公路）", data));
		String info = data.getParam("info");
		String tpid = SessionUtils.getTPID();
		JSONObject jobj = null;
		String sql = null;
		Double makPrice = null;
		Double minPrice = null;
		Record<String, Object> param = new RecordImpl<String, Object>();
		if (!StringUtils.isEmpty(info))
		{
			JSONArray jarr = JSONArray.parseArray(info);
			for (int i = 0; i < jarr.size(); i++)
			{
				jobj = jarr.getJSONObject(i);
				for (Entry<String, Object> entry : jobj.entrySet())
				{
					param.clear();
					String sid = entry.getKey();
					JSONObject json = JSONObject.parseObject(entry.getValue()
							.toString());
					makPrice = json.getDouble("CONTROL_PRICE");
					minPrice = json.getDouble("CONTROL_MIN_PRICE");
					param.setColumn("sid", sid);
					param.setColumn("makPrice", makPrice);
					param.setColumn("minPrice", minPrice);
					param.setColumn("tpid", tpid);
					sql = "UPDATE EKB_T_SECTION_INFO SET N_CONTROL_PRICE=#{makPrice},N_CONTROL_MIN_PRICE=#{minPrice} WHERE V_TPID=#{tpid} AND V_BID_SECTION_ID=#{sid}";
					this.activeRecordDAO.sql(sql).build(param).update();
				}
			}
		}

	}

	/**
	 * 
	 * 保存控制价<br/>
	 * <p>
	 * 保存控制价
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/savePrices", desc = "保存控制价")
	@HttpMethod(HttpMethod.POST)
	public void savePrices(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存控制价", data));
		String info = data.getParam("info");
		// 招标项目ID
		String tpid = SessionUtils.getTPID();
		// 标段信息
		Record<String, Object> section = new RecordImpl<String, Object>();
		// 标段扩展信息
		JSONObject sectionExtInfo = null;
		JSONObject viewJsons = null;
		JSONObject viewJson = null;
		Record<String, Object> param = new RecordImpl<String, Object>();

		if (!StringUtils.isEmpty(info))
		{
			JSONArray jarr = JSONArray.parseArray(info);
			for (int i = 0; i < jarr.size(); i++)
			{
				viewJsons = jarr.getJSONObject(i);
				logger.debug(LogUtils.format("从页面获取标段控制价信息", viewJsons));
				for (String key : viewJsons.keySet())
				{
					param.clear();
					param.setColumn("sid", key);
					param.setColumn("tpid", tpid);

					// 先获取标段
					section = this.activeRecordDAO.auto()
							.table(TableName.EKB_T_SECTION_INFO)
							.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
							.setCondition("AND", "V_TPID=#{tpid}").get(param);
					viewJson = viewJsons.getJSONObject(key);
					// 控制价更新
					section.setColumn("N_CONTROL_PRICE",
							viewJson.getDouble("CONTROL_PRICE"));
					section.setColumn("N_CONTROL_MIN_PRICE",
							viewJson.getDouble("CONTROL_MIN_PRICE"));

					sectionExtInfo = section.getJSONObject("V_JSON_OBJ");

					// 修改KQ值
					modifyKQValue(sectionExtInfo, viewJson);

					section.setColumn("V_JSON_OBJ",
							sectionExtInfo.toJSONString());
					this.activeRecordDAO.auto()
							.table(TableName.EKB_T_SECTION_INFO)
							.modify(section);
				}
			}
		}

	}

	/**
	 * 
	 * 修改KQ值<br/>
	 * <p>
	 * 修改KQ值
	 * </p>
	 * 
	 * @param sectionExtInfo
	 * @param viewJson
	 * @throws ServiceException
	 */
	private void modifyKQValue(JSONObject sectionExtInfo, JSONObject viewJson)
			throws ServiceException
	{
		logger.debug(LogUtils.format("修改KQ值", sectionExtInfo, viewJson));
		// 操作日志说明
		String memo = null;
		// 有K值需要处理
		if (StringUtils.isNotEmpty(viewJson.getString("SHOW_K")))
		{
			// K值做了修改
			if (!StringUtils.equals(sectionExtInfo.getString("MAX_K"),
					viewJson.getString("MAX_K")))
			{
				memo = "修改K的最大值";
				BidOperationLog.addOperationLog(memo,
						sectionExtInfo.getString("MAX_K"),
						viewJson.getString("MAX_K"));
				sectionExtInfo.put("MAX_K", viewJson.getString("MAX_K"));
			}
			if (!StringUtils.equals(sectionExtInfo.getString("MIN_K"),
					viewJson.getString("MIN_K")))
			{
				memo = "修改K的最小值";
				BidOperationLog.addOperationLog(memo,
						sectionExtInfo.getString("MIN_K"),
						viewJson.getString("MIN_K"));
				sectionExtInfo.put("MIN_K", viewJson.getString("MIN_K"));
			}
		}

		// 有Q值需要处理
		if (StringUtils.isNotEmpty(viewJson.getString("SHOW_Q")))
		{
			// Q1值做了修改
			if (!StringUtils.equals(sectionExtInfo.getString("Q1"),
					viewJson.getString("Q1")))
			{
				memo = "修改Q1值";
				BidOperationLog.addOperationLog(memo,
						sectionExtInfo.getString("Q1"),
						viewJson.getString("Q1"));
				sectionExtInfo.put("Q1", viewJson.getString("Q1"));
			}
			// Q2值做了修改
			if (!StringUtils.equals(sectionExtInfo.getString("Q2"),
					viewJson.getString("Q2")))
			{
				memo = "修改Q2值";
				BidOperationLog.addOperationLog(memo,
						sectionExtInfo.getString("Q2"),
						viewJson.getString("Q2"));
				sectionExtInfo.put("Q2", viewJson.getString("Q2"));
			}
		}
	}
}
