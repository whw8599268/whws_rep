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
package com.sozone.eokb.fjs_sygc_kcsj_hldjfxyf_v2;

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
 * 下浮系数服务接口<br/>
 * <p>
 * 下浮系数服务接口<br/>
 * </p>
 * Time：2017-10-18 上午10:24:34<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fjs_sygc_kcsj_hldjfxyf_v2/lc", desc = "下浮系数服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class LowerCoefficient extends BaseAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(LowerCoefficient.class);

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

		// 下浮系数视图
		model.put("TENDER_PROJECT_LC_LIST",
				SygcUtils.getLowerCoefficientView(tpid, tenderProjectNodeID));
		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			// 如果已经发起确认
			if (!CollectionUtils.isEmpty(confirm)
					&& StringUtils.equals("1", confirm.getString("V_STATUS")))
			{
				return new ModelAndView(
						getTheme(data.getHttpServletRequest())
								+ "/eokb/fjs_sygc_kcsj_hldjfxyf_v2/bidOpen/lower.coefficient.tbr.html",
						model);
			}
			// 未发起确认
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_sygc_kcsj_hldjfxyf_v2/bidOpen/lower.coefficient.tbr.none.html",
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
							+ "/eokb/fjs_sygc_kcsj_hldjfxyf_v2/bidOpen/lower.coefficient.view.html",
					model);
		}
		// 如果招标人或者招标代理
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/fjs_sygc_kcsj_hldjfxyf_v2/bidOpen/lower.coefficient.zbr.html",
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

		// 下浮系数视图
		model.put("TENDER_PROJECT_LC_LIST",
				SygcUtils.getLowerCoefficientView(tpid, ""));
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/fjs_sygc_gcsg_hldjf_v1/bidOpen/lower.coefficient.view.html",
				model);
	}

	/**
	 * 
	 * 获取方法二的下浮系数列表<br/>
	 * <p>
	 * 获取方法二的下浮系数列表
	 * </p>
	 * 
	 * @param prices
	 * @param weight
	 * @return
	 */
	// private List<Record<String, Object>> getCoefficientsMethodTwo(
	// List<Object> prices, double weight, double cPrice)
	// {
	// List<Record<String, Object>> cl = new LinkedList<Record<String,
	// Object>>();
	// float min = 0;
	// float max = 0;
	// // 投标人数
	// int bidderNum = prices.size();
	//
	// double avg = 0;
	//
	// // 投标价
	// for (int i = 0; i < bidderNum; i++)
	// {
	// if (prices.get(i) != null)
	// {
	// double price = Double.parseDouble(prices.get(i).toString());
	// avg += price * (weight / 100);
	// }
	// }
	// avg = avg / (weight / 100 * bidderNum);
	//
	// //
	// 算术平均值或加权平均值大于等于最高限价的95%时的下浮值，在3%~6%之间，以0.5%为一档抽取（K1对应3%、K2对应3.5%、K3对应4%……K7对应6%）
	// if (avg >= cPrice * 0.95)
	// {
	// min = 3;
	// max = 6;
	// }
	// //
	// 算术平均值或加权平均值小于最高限价的95%且大于最高限价的90%时的下浮值，在1%~4%之间，以0.5%为一档抽取（K1对应1%、K2对应1.5%、K3对应2%……K7对应4%）；
	// else if (cPrice * 0.9 < avg && avg < cPrice * 0.95)
	// {
	// min = 1;
	// max = 4;
	// }
	// //
	// 算术平均值或加权平均值小于等于最高限价的90%时的下浮值，在-1%~2%之间，以0.5%为一档抽取（K1对应-1%、K2对应-0.5%、K3对应0%……K7对应2%）。
	// else
	// {
	// min = -1;
	// max = 2;
	// }
	//
	// Record<String, Object> temp = null;
	// for (; min <= max;)
	// {
	// temp = new RecordImpl<String, Object>();
	// temp.setColumn("VALUE", min);
	// min += 0.5;
	// cl.add(temp);
	// }
	// caculateAvg = avg;
	// return cl;
	// }

	/**
	 * 
	 * 获取方法一的下浮系数列表<br/>
	 * <p>
	 * 获取方法一的下浮系数列表
	 * </p>
	 * 
	 * @param prices
	 * @return
	 */
	// private List<Record<String, Object>> getCoefficientsMethodOne(
	// List<Object> prices, double cPrice)
	// {
	// List<Record<String, Object>> cl = new LinkedList<Record<String,
	// Object>>();
	//
	// // 投标人数
	// int bidderNum = prices.size();
	//
	// double avg = 0;
	//
	// float min = 0;
	// float max = 0;
	//
	// // 若参与评标基准价计算的投标人超过5家，则剔除最高、最低标后，进行算术平均。
	// if (bidderNum >= 5)
	// {
	// for (int i = 1; i < bidderNum; i++)
	// {
	// // 剔除最高、最低标
	// if (i != bidderNum - 1)
	// {
	// if (prices.get(i) != null)
	// {
	// double price = Double.parseDouble(prices.get(i)
	// .toString());
	// avg += price;
	// }
	// }
	// }
	// avg = avg / (bidderNum - 2);
	// }
	// // 若参与评标基准价计算的投标人未超过5家
	// else
	// {
	//
	// for (int i = 0; i < bidderNum; i++)
	// {
	// if (prices.get(i) != null)
	// {
	// double price = Double.parseDouble(prices.get(i).toString());
	// avg += price;
	// }
	// }
	// avg = avg / bidderNum;
	// }
	// //
	// 算术平均值或加权平均值大于等于最高限价的95%时的下浮值，在3%~6%之间，以0.5%为一档抽取（K1对应3%、K2对应3.5%、K3对应4%……K7对应6%）
	// if (avg >= cPrice * 0.95)
	// {
	// min = 3;
	// max = 6;
	// }
	// //
	// 算术平均值或加权平均值小于最高限价的95%且大于最高限价的90%时的下浮值，在1%~4%之间，以0.5%为一档抽取（K1对应1%、K2对应1.5%、K3对应2%……K7对应4%）；
	// else if (cPrice * 0.9 < avg && avg < cPrice * 0.95)
	// {
	// min = 1;
	// max = 4;
	// }
	// //
	// 算术平均值或加权平均值小于等于最高限价的90%时的下浮值，在-1%~2%之间，以0.5%为一档抽取（K1对应-1%、K2对应-0.5%、K3对应0%……K7对应2%）。
	// else
	// {
	// min = -1;
	// max = 2;
	// }
	//
	// Record<String, Object> temp = null;
	// for (; min <= max;)
	// {
	// temp = new RecordImpl<String, Object>();
	// temp.setColumn("VALUE", min);
	// min += 0.5;
	// cl.add(temp);
	// }
	//
	// caculateAvg = avg;
	// return cl;
	// }

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
