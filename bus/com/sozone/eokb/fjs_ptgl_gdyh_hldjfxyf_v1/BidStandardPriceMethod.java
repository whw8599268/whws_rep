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
package com.sozone.eokb.fjs_ptgl_gdyh_hldjfxyf_v1;

import java.util.LinkedList;
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
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.orm.DataEntry;
import com.sozone.aeolus.ext.orm.impl.DataEntryImpl;
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
@Path(value = "/fjs_ptgl_gdyh_hldjfxyf_v1/bspm", desc = "评标基准价计算方法服务")
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
	private static String SQLNAME_FIRSTENVELOPE = ConstantEOKB.EOKBBemCode.FJS_PTGL_GDYH_HLDJFXYF_V1
			+ "_BidStandardPriceMethod";

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

		// 先查评标基准价信息算法JSON
		List<String> bspmJsons = this.activeRecordDAO.statement().loadList(
				SQLNAME_FIRSTENVELOPE + ".getBidStandardPriceMethodInfos",
				param);

		// 如果不存在信息,即第一次进入
		if (CollectionUtils.isEmpty(bspmJsons))
		{
			bspmJsons = initBSPMData(tpid, tenderProjectNodeID);
		}
		model.putAll(PtglUtils.getBidStandardPriceMethodView(bspmJsons));
		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			// 如果已经发起确认
			if (!CollectionUtils.isEmpty(confirm)
					&& StringUtils.equals("1", confirm.getString("V_STATUS")))
			{
				return new ModelAndView(
						getTheme(data.getHttpServletRequest())
								+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/bspm.tbr.html",
						model);
			}
			// 未发起确认
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/bspm.tbr.none.html",
					model);
		}
		// 如果招标人或者代理以及结束了节点
		if (null != tpNode.getInteger("N_STATUS")
				&& 3 == tpNode.getInteger("N_STATUS"))
		{
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/bspm.view.html",
					model);
		}
		// 如果招标人或者招标代理
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/bspm.zbr.html",
				model);
	}

	private List<String> initBSPMData(String tpid, String tenderProjectNodeID)
			throws ServiceException
	{
		List<String> bspmJsons = new LinkedList<String>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 查询出所有未流标的标段信息
		DataEntry de = new DataEntryImpl(TableName.EKB_T_SECTION_INFO);
		de.select("ID", "V_BID_SECTION_ID", "V_BID_SECTION_CODE",
				"V_BID_SECTION_GROUP_CODE", "V_BID_SECTION_NAME",
				"N_CONTROL_PRICE", "N_CONTROL_MIN_PRICE");
		de.and().equalTo("V_TPID", tpid)
				.notEqualTo("V_BID_OPEN_STATUS", "10-1")
				.notEqualTo("V_BID_OPEN_STATUS", "10-2")
				.notEqualTo("V_BID_EVALUATION_STATUS", "10");
		List<Record<String, Object>> sections = de.persist().load();
		if (CollectionUtils.isEmpty(sections))
		{
			throw new ServiceException("", "找不到任何的标段信息!");
		}
		param.clear();
		param.setColumn("tpid", tpid);
		// 投标报价列表
		List<Double> prices = null;
		// 有效投标报价列表
		List<Double> effectivePrices = null;
		// 投标人总数
		int totalBidder = 0;
		// 有效投标人总数
		int totalEffectiveBidder = 0;
		// 最高限价
		Double maxPrice = 0D;
		// 最低限价
		Double minPrice = 0D;
		Record<String, Object> bspm = null;
		Record<String, Object> record = new RecordImpl<String, Object>();

		// 删除冗余数据,为了防止出现重复
		param.setColumn("flag", ConstantEOKB.BSPM_BUS_FLAG_TYPE);
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").remove(param);
		param.remove("flag");
		String json = null;
		// 迭代构造标段评标基准价计算方法信息
		for (Record<String, Object> section : sections)
		{
			record.clear();
			bspm = new RecordImpl<String, Object>();
			record.setColumn("ID", Random.generateUUID());
			record.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.BSPM_BUS_FLAG_TYPE);
			record.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			record.setColumn("V_TPID", tpid);
			record.setColumn("V_TPFN_ID", tenderProjectNodeID);
			record.setColumn("V_BUS_ID", section.getString("V_BID_SECTION_ID"));
			// 标段信息
			bspm.setColumn("SECTION_INFO", section);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			// 最高限价
			maxPrice = section.getDouble("N_CONTROL_PRICE");
			if (null == maxPrice)
			{
				maxPrice = 0D;
			}
			// 最低限价
			minPrice = section.getDouble("N_CONTROL_MIN_PRICE");
			if (null == minPrice)
			{
				minPrice = 0D;
			}
			// 获取投标人投标价列表,包含超过最高限价的
			prices = this.activeRecordDAO.statement().loadList(
					SQLNAME_FIRSTENVELOPE + ".getSectionEffectivePrice", param);
			totalBidder = prices.size();
			bspm.setColumn("BID_PRICES", prices);
			bspm.setColumn("TOTAL_BIDDER", totalBidder);
			// 有效投标报价列表
			effectivePrices = getEffectivePrice(prices, maxPrice, minPrice);
			totalEffectiveBidder = effectivePrices.size();
			bspm.setColumn("BID_EFFECTIVE_PRICES", effectivePrices);
			bspm.setColumn("TOTAL_EFFECTIVE_BIDDER", totalEffectiveBidder);
			bspm.setColumn("IS_LESS_THAN_FIFTEEN", false);

			// 如果有效家数小于15使用，小于15家算法
			if (totalEffectiveBidder < 15)
			{
				bspm.setColumn("IS_LESS_THAN_FIFTEEN", true);
				bspm.setColumn("YAOHAO_RESULT_METHOD", -1);
				// 获取计算系数
				bspm.setColumn("COEFFICIENT_INFO",
						BidBenchmarkPriceAlgorithmUtils.getLTFCoefficient(
								effectivePrices, maxPrice));
				json = JSON.toJSONString(bspm);
				record.setColumn("V_JSON_OBJ", json);
				this.activeRecordDAO.auto()
						.table(TableName.TENDER_PROJECT_NODE_DATA).save(record);
				bspmJsons.add(json);
				continue;
			}
			// 如果使用的是大于等于15家算法
			// 方法一
			bspm.setColumn("METHOD_ONE", BidBenchmarkPriceAlgorithmUtils
					.canUseMethodOne(effectivePrices, maxPrice));
			// 方法二
			bspm.setColumn("METHOD_TWO", BidBenchmarkPriceAlgorithmUtils
					.canUseMethodTwo(effectivePrices, maxPrice));
			// 方法三
			bspm.setColumn("METHOD_THREE", BidBenchmarkPriceAlgorithmUtils
					.canUseMethodThree(effectivePrices, maxPrice));
			json = JSON.toJSONString(bspm);
			record.setColumn("V_JSON_OBJ", json);
			this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(record);
			bspmJsons.add(json);
		}
		return bspmJsons;
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
	@Path(value = "viewgroup/{tpnid}", desc = "打开评标基准价计算方法编辑页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openGroupView(
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

		// 先查评标基准价信息算法JSON
		List<String> bspmJsons = this.activeRecordDAO.statement().loadList(
				SQLNAME_FIRSTENVELOPE + ".getBidStandardPriceMethodInfos",
				param);
		// 如果不存在信息,即第一次进入
		if (CollectionUtils.isEmpty(bspmJsons))
		{
			bspmJsons = initBSPMData(tpid, tenderProjectNodeID);
		}
		// 如果有数据
		model.putAll(PtglUtils.getBidStandardPriceMethodGroupView(bspmJsons));
		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			// 如果已经发起确认
			if (!CollectionUtils.isEmpty(confirm)
					&& StringUtils.equals("1", confirm.getString("V_STATUS")))
			{
				return new ModelAndView(
						getTheme(data.getHttpServletRequest())
								+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/bspm.group.tbr.html",
						model);
			}
			// 未发起确认
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/bspm.tbr.none.html",
					model);
		}
		// 如果招标人或者代理以及结束了节点
		if (null != tpNode.getInteger("N_STATUS")
				&& 3 == tpNode.getInteger("N_STATUS"))
		{
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/bspm.group.view.html",
					model);
		}
		// 如果招标人或者招标代理
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/bspm.group.zbr.html",
				model);
	}

	/**
	 * 获取有效的投标报价列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param prices
	 * @param maxPrice
	 *            最高限价
	 * @return
	 */
	private static List<Double> getEffectivePrice(List<Double> prices,
			Double maxPrice, Double minPrice)
	{
		if (CollectionUtils.isEmpty(prices))
		{
			return prices;
		}
		List<Double> rs = new LinkedList<Double>();
		for (Double price : prices)
		{
			if (null != price && price <= maxPrice && price >= minPrice)
			{
				rs.add(price);
			}
		}
		return rs;
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
			String m3no = null;
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
				m3no = bspm.getString("METHOD_THREE_NO");
				yhno = bspm.getString("YAOHAO_RESULT");

				// 如果是空对象
				if (StringUtils.isEmpty(m1no) && StringUtils.isEmpty(m2no)
						&& StringUtils.isEmpty(m3no)
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
				else if (StringUtils.equals(yhno, m3no))
				{
					yhr = "方法三";
					yhmr = 3;
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
					// 方法一
					if (StringUtils.isNotEmpty(m1no))
					{
						JSONObject methodOne = jobj.getJSONObject("METHOD_ONE");
						methodOne.put("NO", m1no);
						jobj.put("METHOD_ONE", methodOne);
					}
					// 方法二
					if (StringUtils.isNotEmpty(m2no))
					{
						JSONObject methodTwo = jobj.getJSONObject("METHOD_TWO");
						methodTwo.put("NO", m2no);
						jobj.put("METHOD_TWO", methodTwo);
					}
					// 方法三
					if (StringUtils.isNotEmpty(m3no))
					{
						JSONObject methodThree = jobj
								.getJSONObject("METHOD_THREE");
						methodThree.put("NO", m3no);
						jobj.put("METHOD_THREE", methodThree);
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
