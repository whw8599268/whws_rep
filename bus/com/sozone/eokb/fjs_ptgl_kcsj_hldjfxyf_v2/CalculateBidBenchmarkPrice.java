/**
 * 包名：com.sozone.eokb.fjs_ptgl_gdyh_hldjfxyf_v1
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
package com.sozone.eokb.fjs_ptgl_kcsj_hldjfxyf_v2;

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
import com.sozone.eokb.fjs_ptgl.common.PtglUtils;
import com.sozone.eokb.handler.OperationLogHandler;
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
@Path(value = "/fjs_ptgl_kcsj_hldjfxyf_v2/cbbp", desc = "计算评标基准价服务")
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
	 * 数据库xml
	 */
	private static String sqlName = ConstantEOKB.EOKBBemCode.FJS_PTGL_KCSJ_HLDJFXYF_V2
			+ "_CalculateBidBenchmarkPrice";

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
		// 如果尚未发起确认,且是投标人
		if ((CollectionUtils.isEmpty(confirm) || !StringUtils.equals("1",
				confirm.getString("V_STATUS"))) && SessionUtils.isBidder())
		{
			// 未发起确认
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_kcsj_hldjfxyf_v2/thirdEnvelope/calculate.bsp.tbr.none.html",
					model);
		}

		// 判断是否计算过评标基准价
		List<String> jsons = this.activeRecordDAO.statement().loadList(
				sqlName + ".getCBSPInfos", param);
		// 没有计算过
		if (CollectionUtils.isEmpty(jsons))
		{
			// 删除冗余数据,为了防止出现重复
			param.setColumn("flag", ConstantEOKB.CBSP_BUS_FLAG_TYPE);
			this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}")
					.remove(param);
			param.remove("flag");
			jsons = calculate(tpid, tenderProjectNodeID);
		}
		model.putAll(PtglUtils.getCalculateForKc(jsons, tpid));

		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			// 如果已经发起确认
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_kcsj_hldjfxyf_v2/thirdEnvelope/calculate.bsp.tbr.html",
					model);
		}
		// 如果招标人或者招标代理
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/fjs_ptgl_kcsj_hldjfxyf_v2/thirdEnvelope/calculate.bsp.zbr.html",
				model);

	}

	private List<String> calculate(String tpid, String tenderProjectNodeID)
			throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("tpnoid", tenderProjectNodeID);
		List<String> jsons = new LinkedList<String>();
		// 开始计算评标基准价
		param.setColumn("flag", ConstantEOKB.LC_BUS_FLAG_TYPE);
		// 获取下浮系数抽取JSON
		List<String> lcJsons = this.activeRecordDAO.statement().loadList(
				sqlName + ".getLCInfos", param);
		// 如果下浮系数为空,说明是第一次进入
		if (CollectionUtils.isEmpty(lcJsons))
		{
			throw new ServiceException("", "无法获取到抽取的下浮系数信息!");
		}
		// 下浮系数不为空
		Record<String, Object> lcInfo = null;
		JSONObject jobj = null;
		JSONObject section = null;
		// 标段ID
		String sid = null;
		// 节点数据
		Record<String, Object> temp = new RecordImpl<String, Object>();
		// json
		Record<String, Object> vj = new RecordImpl<String, Object>();
		String js = null;
		for (String json : lcJsons)
		{
			// 如果json为空
			if (StringUtils.isEmpty(json))
			{
				throw new ServiceException("", "下浮系数信息为空!");
			}
			jobj = JSON.parseObject(json);
			lcInfo = new RecordImpl<String, Object>();
			lcInfo.putAll(jobj);
			// 标段信息
			section = jobj.getJSONObject("SECTION_INFO");
			if (null == section || section.isEmpty())
			{
				throw new ServiceException("", "无法获取下浮系数对应的标段信息!");
			}
			// 标段ID
			sid = section.getString("V_BID_SECTION_ID");
			if (StringUtils.isEmpty(sid))
			{
				throw new ServiceException("", "无法获取下浮系数对应的标段信息!");
			}
			param.setColumn("sid", sid);
			// 节点数据
			temp.clear();
			temp.setColumn("ID", Random.generateUUID());
			temp.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.CBSP_BUS_FLAG_TYPE);
			temp.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
					.setColumn("N_CREATE_TIME", System.currentTimeMillis());
			temp.setColumn("V_TPID", tpid);
			temp.setColumn("V_TPFN_ID", tenderProjectNodeID);
			temp.setColumn("V_BUS_ID", sid);

			// JSON数据
			vj.clear();
			vj.setColumn("V_BID_SECTION_ID", sid);
			// 抽取的下浮系数段
			vj.setColumn(
					"COEFFICIENT_NO",
					jobj.getJSONObject("COEFFICIENT_INFO").getInteger(
							"COEFFICIENT_NO"));
			// 有效投标报价
			JSONArray ps = lcInfo.getColumn("BID_EFFECTIVE_PRICES");
			List<Double> prices = getDoubleList(ps);
			// 下浮系数
			Double coe = lcInfo.getDouble("COEFFCIENT_VALUE");
			coe = coe / 100;
			// 下浮系数
			vj.setColumn("COEFFCIENT_VALUE",
					lcInfo.getDouble("COEFFCIENT_VALUE"));
			// 计算法方法
			vj.setColumn("METHOD", lcInfo.getInteger("METHOD"));

			js = JSON.toJSONString(vj);
			temp.setColumn("V_JSON_OBJ", js);

			// 计算评标基准价
			long bbp = BidBenchmarkPriceAlgorithmUtils.calculate(prices,
					section.getDouble("N_CONTROL_PRICE"), coe,
					lcInfo.getInteger("METHOD"));
			// 修改标段上的评标基准价值
			param.setColumn("N_EVALUATION_PRICE", bbp);
			// 修改标段的评标基准价
			this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
					.modify(param);
			// 保存节点数据
			this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(temp);
			jsons.add(js);
		}
		return jsons;
	}

	private List<Double> getDoubleList(JSONArray array)
	{
		if (null == array || array.isEmpty())
		{
			return null;
		}
		List<Double> rs = new LinkedList<Double>();
		for (int i = 0; i < array.size(); i++)
		{
			rs.add(array.getDouble(i));
		}
		return rs;
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
	@Handler(OperationLogHandler.class)
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
