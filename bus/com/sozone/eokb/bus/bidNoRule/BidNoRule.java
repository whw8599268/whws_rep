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
package com.sozone.eokb.bus.bidNoRule;

import java.util.ArrayList;
import java.util.Collections;
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
import com.sozone.aeolus.authorize.utlis.GenerateCodeUtils;
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
 * 投标人代表号生成规则接口<br/>
 * <p>
 * 投标人代表号生成规则接口<br/>
 * </p>
 * Time：2017-9-23 上午10:24:34<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bidnorule", desc = "投标人代表号生成规则接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class BidNoRule extends BaseAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(BidNoRule.class);

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
	 * 打开投标人代表号生成规则抽取页面<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            招标项目流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 打开投标人代表号生成规则抽取页面
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "flow/{tpnid}", desc = "打开投标人代表号生成规则抽取页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openView(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开投标人代表号生成规则抽取页面", tenderProjectNodeID,
				data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		param.setColumn("type", ConstantEOKB.XMFJSZ_BIDDER_NO_BUS_FLAG_TYPE);

		ModelMap model = new ModelMap();

		// 查询流程节点
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		// 查询球号生成规则JSON
		logger.debug(LogUtils.format("查询球号生成规则JSON", param));
		String bnrJson = this.activeRecordDAO.statement().getOne(
				"bidnorule.getBidNoRuleJson", param);
		// 如果球号生成规则为空,说明是第一次进入
		if (StringUtils.isEmpty(bnrJson))
		{
			bnrJson = initData(tpid, tenderProjectNodeID);
		}
		model.put("RULE_INFO", JSON.parseObject(bnrJson));

		// 如果结束了节点
		if (null != tpNode.getInteger("N_STATUS")
				&& 3 == tpNode.getInteger("N_STATUS"))
		{
			// 如果招标人或者招标代理
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/bid_no_rule/bid.no.rule.view.html", model);

		}
		// 如果是投标人
		if (SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/bid_no_rule/bid.no.rule.none.html", model);
		}

		// 如果招标人或者招标代理
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/bid_no_rule/bid.no.rule.html", model);
	}

	/**
	 * 
	 * 初始化球号生成规则JSON<br/>
	 * <p>
	 * 初始化球号生成规则JSON
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @return
	 * @throws ServiceException
	 *             服务异常
	 */
	private String initData(String tpid, String tenderProjectNodeID)
			throws ServiceException
	{
		logger.debug(LogUtils
				.format("初始化球号生成规则JSON", tpid, tenderProjectNodeID));
		// 如果有数据
		Record<String, Object> ruleInfo = new RecordImpl<String, Object>();
		Record<String, Object> record = null;
		String jsonStr = null;

		// 构建下浮信息对象
		record = new RecordImpl<String, Object>();
		ruleInfo.setColumn("ID", Random.generateUUID());
		ruleInfo.setColumn("V_BUS_FLAG_TYPE",
				ConstantEOKB.XMFJSZ_BIDDER_NO_BUS_FLAG_TYPE);
		ruleInfo.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
				.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		ruleInfo.setColumn("V_TPID", tpid);
		ruleInfo.setColumn("V_TPFN_ID", tenderProjectNodeID);

		List<Record<String, Object>> cl = getRuleList();
		record.setColumn("RULE_LIST", cl);
		jsonStr = JSON.toJSONString(record);
		ruleInfo.setColumn("V_JSON_OBJ", jsonStr);
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.save(ruleInfo);
		logger.debug(LogUtils.format("初始化球号生成规则JSON完成", jsonStr));
		return new RecordImpl<String, Object>().setColumn("RULE_LIST", cl)
				.toString();
	}

	/**
	 * 编号生成规则列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param type
	 * @return
	 */
	private List<Record<String, Object>> getRuleList()
	{
		logger.debug(LogUtils.format("编号生成规则列表"));
		List<Record<String, Object>> cl = new LinkedList<Record<String, Object>>();
		int min = 0;
		int max = 1;

		String[] arr = { "按照投标人递交投标文件的正顺序", "按照投标人递交投标文件的逆顺序" };
		Record<String, Object> temp = null;
		for (int i = 0; min <= max; i++)
		{
			temp = new RecordImpl<String, Object>();
			temp.setColumn("VALUE", min);
			temp.setColumn("RULE", arr[i]);
			min += 1;
			cl.add(temp);
		}
		return cl;
	}

	/**
	 * 保存编号生成规则摇号结果<br/>
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
	@Path(value = "modify/{tpnid}", desc = "保存编号生成规则摇号结果")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyLowerCoefficient(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("保存编号生成规则摇号结果", tenderProjectNodeID, data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnid", tenderProjectNodeID);
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		param.setColumn("tpid", tpid);
		// 各标段列表
		List<JSONObject> sms = data.getParams("BID_NO_RULE");

		// 规则编号
		String ruleNO = null;

		if (!CollectionUtils.isEmpty(sms))
		{
			List<String> indexNOs = new ArrayList<String>();
			String yhno = null;
			String indexNO = null;
			String cv = null;
			Record<String, Object> record = null;
			String json = null;
			JSONObject jobj = null;
			JSONArray vsArray = null;
			Record<String, Object> temp = new RecordImpl<String, Object>();
			boolean empty = true;
			for (JSONObject hw : sms)
			{
				temp.clear();
				indexNOs.clear();
				empty = true;
				for (int i = 0; i < 2; i++)
				{
					// 验证分配的球号是否对号码球根据号码大小进行排序，最小的号码球代表规则一，次小的号码球代表规则二。
					if (i == 0
							&& hw.getInteger(i + "") >= hw.getInteger(i + 1
									+ ""))
					{
						throw new FacadeException("", "规则一的球号必须小于规则二的球号");
					}

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
				cv = hw.getString("RULE");
				// 如果有值需要修改
				record = this.activeRecordDAO.auto()
						.table(TableName.TENDER_PROJECT_NODE_DATA)
						.setCondition("AND", "V_TPID = #{tpid}")
						.setCondition("AND", "V_TPFN_ID = #{tpnid}").get(param);
				if (!CollectionUtils.isEmpty(record))
				{
					json = record.getString("V_JSON_OBJ");
					jobj = JSON.parseObject(json);

					vsArray = jobj.getJSONArray("RULE_LIST");

					JSONObject vl = null;
					for (int i = 0; i < vsArray.size(); i++)
					{
						vl = vsArray.getJSONObject(i);
						if (StringUtils.equals(yhno, indexNOs.get(i)))
						{
							ruleNO = vl.getString("VALUE");
							jobj.put("RESULT_ROLE", vl.get("VALUE"));
						}
						vl.put("NO", indexNOs.get(i));
					}
					jobj.put("RESULT_BALL", yhno);
					jobj.put("RULE", cv);
					temp.setColumn("ID", record.getString("ID"));
					temp.setColumn("V_JSON_OBJ", jobj.toJSONString());
					this.activeRecordDAO.auto()
							.table(TableName.TENDER_PROJECT_NODE_DATA)
							.modify(temp);
				}
			}
		}
		// 生成代表号
		generateDeliveryNO(ruleNO);
		logger.debug(LogUtils.format("成功保存编号生成规则摇号结果"));
	}

	/**
	 * 生成代表号<br/>
	 * 
	 * @param ruleNO
	 * @throws ServiceException
	 */
	private void generateDeliveryNO(String ruleNO) throws ServiceException
	{
		if (StringUtils.isNotEmpty(ruleNO))
		{
			// 如果是正顺序
			if (StringUtils.equals("0", ruleNO))
			{
				// 正顺序
				generatePositiveDeliveryNO();
			}
			// 如果是逆顺序
			if (StringUtils.equals("1", ruleNO))
			{
				// 生成逆顺序
				generateReverseDeliveryNO();
			}
			// 如果是随机
			else if (StringUtils.equals("-1", ruleNO))
			{
				// 生成随机顺序
				generateRandomDeliveryNO();
			}
		}
	}

	/**
	 * 生成随机代表号<br/>
	 * <p>
	 * </p>
	 * 
	 * @throws ServiceException
	 */
	private void generateRandomDeliveryNO() throws ServiceException
	{
		logger.info(LogUtils.format("生成随机投标人编号"));
		String tpid = SessionUtils.getTPID();
		// 获取指定招标项目的每一个投标人的最后投递时间
		List<String> rows = this.activeRecordDAO.statement().loadList(
				"XiamenFJSZ.getDeliveryOrgCodes", tpid);
		if (CollectionUtils.isEmpty(rows))
		{
			return;
		}
		// 随机一下
		Collections.shuffle(rows);
		// 总长3
		int maxTotalLength = 3;
		// 最长3
		int maxLength = 3;
		// 投递位数
		int length = StringUtils.length(rows.size() + "");
		if (length > 3)
		{
			maxTotalLength = length;
			maxLength = length;
		}
		// 投递编号
		String deliveryNO = null;
		Record<String, Object> record = new RecordImpl<String, Object>();
		for (String orgCode : rows)
		{
			record.clear();
			// 生成投递编号
			deliveryNO = GenerateCodeUtils.generateNextCode(null, deliveryNO,
					maxTotalLength, maxLength);
			// 设置编号
			record.setColumn("V_DELIVER_NO", deliveryNO);
			record.setColumn("orgCode", orgCode);
			// 设置tpid
			record.setColumn("tpid", tpid);
			// 修改该招标项目下该投标人的所有投递数据的编号以及时间
			this.activeRecordDAO.auto().table(TableName.EKB_T_TBIMPORTBIDDING)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", " V_ORG_CODE = #{orgCode}")
					.modify(record);
		}
	}

	/**
	 * 
	 * 生成倒序编号<br/>
	 * <p>
	 * </p>
	 */
	private void generateReverseDeliveryNO() throws ServiceException
	{
		logger.info(LogUtils.format("生成倒序投标人编号"));
		String tpid = SessionUtils.getTPID();
		// 获取指定招标项目的每一个投标人的最后投递时间
		List<Record<String, Object>> rows = this.activeRecordDAO.statement()
				.selectList("XiamenFJSZ.getReverseDelivery", tpid);
		if (CollectionUtils.isEmpty(rows))
		{
			return;
		}
		// 总长3
		int maxTotalLength = 3;
		// 最长3
		int maxLength = 3;
		// 投递位数
		int length = StringUtils.length(rows.size() + "");
		if (length > 3)
		{
			maxTotalLength = length;
			maxLength = length;
		}
		// 投递编号
		String deliveryNO = null;
		for (Record<String, Object> record : rows)
		{
			// 生成投递编号
			deliveryNO = GenerateCodeUtils.generateNextCode(null, deliveryNO,
					maxTotalLength, maxLength);
			// 设置编号
			record.setColumn("V_DELIVER_NO", deliveryNO);
			// 设置tpid
			record.setColumn("tpid", tpid);
			// 修改该招标项目下该投标人的所有投递数据的编号以及时间
			this.activeRecordDAO.auto().table(TableName.EKB_T_TBIMPORTBIDDING)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", " V_ORG_CODE = #{V_ORG_CODE}")
					.modify(record);
		}
	}

	/**
	 * 
	 * 生成正序编号<br/>
	 * <p>
	 * </p>
	 */
	private void generatePositiveDeliveryNO() throws ServiceException
	{
		logger.info(LogUtils.format("生成正序投标人编号"));
		String tpid = SessionUtils.getTPID();
		// 获取指定招标项目的每一个投标人的最后投递时间
		List<Record<String, Object>> rows = this.activeRecordDAO.statement()
				.selectList("StartUp.getTenderProjectMaxDeliveryTime", tpid);
		if (CollectionUtils.isEmpty(rows))
		{
			return;
		}
		// 总长3
		int maxTotalLength = 3;
		// 最长3
		int maxLength = 3;
		// 投递位数
		int length = StringUtils.length(rows.size() + "");
		if (length > 3)
		{
			maxTotalLength = length;
			maxLength = length;
		}
		// 投递编号
		String deliveryNO = null;
		for (Record<String, Object> record : rows)
		{
			// 生成投递编号
			deliveryNO = GenerateCodeUtils.generateNextCode(null, deliveryNO,
					maxTotalLength, maxLength);
			// 设置编号
			record.setColumn("V_DELIVER_NO", deliveryNO);
			// 设置tpid
			record.setColumn("tpid", tpid);
			// 修改该招标项目下该投标人的所有投递数据的编号以及时间
			this.activeRecordDAO.auto().table(TableName.EKB_T_TBIMPORTBIDDING)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", " V_ORG_CODE = #{V_ORG_CODE}")
					.modify(record);
		}
	}

}
