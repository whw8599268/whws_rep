/**
 * 包名：com.sozone.eokb.bus.noteTool
 * 文件名：NoteTool.java<br/>
 * 创建时间：2017-11-7 下午3:49:30<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.nodeTool;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.BigTimeOutHttpClientUtils;

/**
 * 修改节点状态金手指<br/>
 * <p>
 * 修改节点状态金手指<br/>
 * </p>
 * Time：2017-11-7 下午3:49:30<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */

@Path(value = "/bus/nodeTool", desc = "修改节点状态服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class NodeTool
{
	private static final NumberFormat FMT_D = new DecimalFormat("###,##0",
			new DecimalFormatSymbols());

	private static final String PUSH_URL = "aeolus.ede.push.bidders.url";

	private static final String PUSH_FJSZ_URL = "aeolus.ede.push.bidders.xm.fjsz.url";

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(NodeTool.class);

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
	 * 修改任意节点状态<br/>
	 * <p>
	 * 修改任意节点状态
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 */
	// 定义路径
	@Path(value = "/modify", desc = "修改任意节点状态")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void modifyNodeStatus(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("修改任意节点状态", data));
		// 工程名称
		String projectName = data.getParam("projectName");
		// 节点名称
		String nodeName = data.getParam("nodeName");
		// 节点状态
		String nodeStatus = data.getParam("nodeStatus");

		Record<String, Object> param = new RecordImpl<String, Object>();

		param.setColumn("projectName", projectName);

		// 先查项目表获取
		Record<String, Object> project = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO)
				.setCondition("AND", "V_TENDER_PROJECT_NAME=#{projectName}")
				.get(param);

		// 没有查到该项目
		if (CollectionUtils.isEmpty(project))
		{
			throw new ServiceException("", "未查询到该项目信息");
		}
		String tpid = project.getString("ID");
		param.setColumn("code", project.getJSONObject("V_BEM_INFO_JSON")
				.getString("V_CODE"));
		Record<String, Object> flowInfo = null;
		// 有标段组
		if (project.getInteger("N_IS_SECTION_GROUP") == 1)
		{
			// 再查询流程信息表
			flowInfo = this.activeRecordDAO.auto().table(TableName.FLOW_INFO)
					.setCondition("AND", "N_IS_SECTION_GROUP=1")
					.setCondition("AND", "V_BEM_CODE=#{code}").get(param);
		}
		else
		{
			// 再查询流程信息表
			flowInfo = this.activeRecordDAO.auto().table(TableName.FLOW_INFO)
					.setCondition("AND", "N_IS_SECTION_GROUP=0")
					.setCondition("AND", "V_BEM_CODE=#{code}").get(param);

		}

		param.setColumn("flowInfoId", flowInfo.getString("ID"));
		param.setColumn("nodeName", nodeName);
		// 再查流程节点信息表，获取是子节点的节点信息
		Record<String, Object> flowNodeInfo = this.activeRecordDAO.auto()
				.table(TableName.FLOW_NODE_INFO)
				.setCondition("AND", "V_FLOW_ID=#{flowInfoId}")
				.setCondition("AND", "V_NODE_NAME=#{nodeName}")
				.setCondition("AND", "N_DEPTH=2").get(param);
		if (CollectionUtils.isEmpty(flowNodeInfo))
		{
			throw new ServiceException("", "未查询到该节点信息");
		}

		param.setColumn("flowNodeInfoId", flowNodeInfo.getColumn("ID"));
		param.setColumn("tpid", tpid);
		param.setColumn("N_STATUS", Integer.parseInt(nodeStatus));
		// 修改流程节点状态
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE)
				.setCondition("AND", "V_FLOW_NODE_ID=#{flowNodeInfoId}")
				.setCondition("AND", "V_TPID=#{tpid}").modify(param);

		// 如果是抽取球号环节做修改，必须清楚下一个环节的json数据，保证数据一致V_PID
		int index = flowNodeInfo.getInteger("N_INDEX");

		// 父节点ID
		String pid = flowNodeInfo.getString("V_PID");
		// 后续环节都需要做出修改,开启递归
		nextFlowNode(index, flowInfo.getString("ID"), tpid, pid);

		// 获取父节点深度
		parentNodeIndex(pid, tpid);
	}

	/**
	 * 
	 * 获取父节点深度<br/>
	 * <p>
	 * 获取父节点深度
	 * </p>
	 * 
	 * @param pid
	 *            父节点ID
	 * @param tpid
	 *            招标项目ID
	 * @throws ServiceException
	 *             ServiceException
	 */
	private void parentNodeIndex(String pid, String tpid)
			throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("ID", pid);
		Record<String, Object> parentNodeInfo = this.activeRecordDAO.auto()
				.table(TableName.FLOW_NODE_INFO)
				.setCondition("AND", "ID=#{ID}").get(param);
		// 本父节点深度
		int index = parentNodeInfo.getInteger("N_INDEX");
		// 流程节点ID
		String flowId = parentNodeInfo.getString("V_FLOW_ID");
		// 寻找父节点的弟弟
		uncletNode(index, flowId, tpid);
	}

	/**
	 * 
	 * 获取父节点的弟弟<br/>
	 * 
	 * @param index
	 * @param flowId
	 * @param tpid
	 * @throws ServiceException
	 *             ServiceException
	 */
	private void uncletNode(int index, String flowId, String tpid)
			throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		index++;
		param.setColumn("index", index);
		param.setColumn("flowInfoId", flowId);
		// 获取下一个节点的流程节点信息
		Record<String, Object> flowNodeInfo = this.activeRecordDAO.auto()
				.table(TableName.FLOW_NODE_INFO)
				.setCondition("AND", "V_FLOW_ID=#{flowInfoId}")
				.setCondition("AND", "N_DEPTH=1")
				.setCondition("AND", "N_INDEX=#{index}").get(param);
		// 如果找不到下一步流程节点信息，则是最后一个父节点，无需进行其他操作
		if (CollectionUtils.isEmpty(flowNodeInfo))
		{
			return;
		}

		nextFlowNode(0, flowId, tpid, flowNodeInfo.getString("ID"));
		// 递归删除本节点下的子节点
		uncletNode(index, flowId, tpid);
	}

	/**
	 * 
	 * 下一个流程节点<br/>
	 * <p>
	 * 下一个流程节点
	 * </p>
	 * 
	 * @param index
	 *            流程节点下标
	 * @param flowInfoId
	 *            流程节点ID
	 * @param tpid
	 *            招标项目ID
	 * @param pid
	 *            父节点id
	 * @throws ServiceException
	 *             ServiceException
	 */
	public void nextFlowNode(int index, String flowInfoId, String tpid,
			String pid) throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		index++;
		param.setColumn("index", index);
		param.setColumn("flowInfoId", flowInfoId);
		param.setColumn("pid", pid);
		// 获取下一个节点的流程节点信息
		Record<String, Object> flowNodeInfo = this.activeRecordDAO.auto()
				.table(TableName.FLOW_NODE_INFO)
				.setCondition("AND", "V_FLOW_ID=#{flowInfoId}")
				.setCondition("AND", "N_DEPTH=2")
				.setCondition("AND", "N_INDEX=#{index}")
				.setCondition("AND", "V_PID=#{pid}").get(param);
		// 如果找不到下一步流程节点信息，则是最后一个节点，删除父节点结束时间以及下一个叔叔节点的开启时间的开标流程节点状态时间表记录，停止递归
		if (CollectionUtils.isEmpty(flowNodeInfo))
		{
			deleteTimeTableRecord(pid, flowInfoId, tpid);
			return;
		}

		// 若有下一个节点点则做修改
		param.clear();
		param.setColumn("tpid", tpid);
		param.setColumn("flowNodeInfoId", flowNodeInfo.getColumn("ID"));

		// 获取流程节点状态信息
		Record<String, Object> flowNodeStatusInfo = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE)
				.setCondition("AND", "V_FLOW_NODE_ID=#{flowNodeInfoId}")
				.setCondition("AND", "V_TPID=#{tpid}").get(param);

		// 未开始的状态
		param.setColumn("N_STATUS", 1);
		// 修改流程节点状态
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE)
				.setCondition("AND", "V_FLOW_NODE_ID=#{flowNodeInfoId}")
				.setCondition("AND", "V_TPID=#{tpid}").modify(param);

		param.clear();
		param.setColumn("ID", flowNodeStatusInfo.getString("ID"));
		// 删除招标项目开标流程节点状态时间表
		this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_STATUS_TIME).remove(param);

		// 删除EKB_T_TPFN_DATA_INFO表中的节点json，让数据重新初始化
		// 获取流程节点ID
		param.setColumn("tpfnID", flowNodeStatusInfo.getString("ID"));
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPFN_ID=#{tpfnID}").remove(param);

		// 递归
		nextFlowNode(index, flowInfoId, tpid, pid);
	}

	/**
	 * 
	 * 删除节点状态时间表中的父节点结束时间和叔叔节点开始时间的记录
	 * 
	 * @param pid
	 *            父节点ID
	 * @param flowInfoId
	 *            节点ID
	 * @param tpid
	 *            招标项目ID
	 * @throws ServiceException
	 *             ServiceException
	 */
	public void deleteTimeTableRecord(String pid, String flowInfoId, String tpid)
			throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("pid", pid);
		param.setColumn("flowInfoId", flowInfoId);
		// 先查父节点
		Record<String, Object> parentFlowNodeInfo = this.activeRecordDAO.auto()
				.table(TableName.FLOW_NODE_INFO)
				.setCondition("AND", "V_FLOW_ID=#{flowInfoId}")
				.setCondition("AND", "N_DEPTH=1")
				.setCondition("AND", "ID=#{pid}").get(param);

		param.setColumn("tpid", tpid);
		param.setColumn("flowNodeInfoId", parentFlowNodeInfo.getColumn("ID"));

		// 获取流程节点状态信息
		Record<String, Object> flowNodeStatusInfo = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE)
				.setCondition("AND", "V_FLOW_NODE_ID=#{flowNodeInfoId}")
				.setCondition("AND", "V_TPID=#{tpid}").get(param);
		param.clear();
		param.setColumn("ID", flowNodeStatusInfo.getString("ID"));
		param.setColumn("N_STATUS", 3);
		// 删除招标项目开标流程父节点结束状态时间
		this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_STATUS_TIME).remove(param);

		param.clear();
		// 再查叔叔节点
		param.setColumn("flowInfoId", flowInfoId);
		param.setColumn("uncleIndex",
				parentFlowNodeInfo.getInteger("N_INDEX") + 1);

		Record<String, Object> uncleFlowNodeInfo = this.activeRecordDAO.auto()
				.table(TableName.FLOW_NODE_INFO)
				.setCondition("AND", "V_FLOW_ID=#{flowInfoId}")
				.setCondition("AND", "N_DEPTH=1")
				.setCondition("AND", "N_INDEX=#{uncleIndex}").get(param);
		// 如果父节点是最后一个节点则没有叔叔节点uncleFlowNodeInfo=null,否则删除该记录在状态时间表中的记录
		if (!CollectionUtils.isEmpty(uncleFlowNodeInfo))
		{
			param.setColumn("flowNodeInfoId", uncleFlowNodeInfo.getString("ID"));
			param.setColumn("tpid", tpid);
			flowNodeStatusInfo = this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_NODE)
					.setCondition("AND", "V_FLOW_NODE_ID=#{flowNodeInfoId}")
					.setCondition("AND", "V_TPID=#{tpid}").get(param);

			param.clear();
			param.setColumn("ID", flowNodeStatusInfo.getString("ID"));
			// 删除招标项目开标流程叔叔节点开始状态时间
			this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_NODE_STATUS_TIME)
					.remove(param);
		}
	}

	/**
	 * 
	 * 获取项目分页信息<br/>
	 * <p>
	 * 获取项目分页信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 获取项目列
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "/getpl", desc = "获取项目获取项目列")
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getProjectList(AeolusData data)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取项目获取项目列", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = data.getRecord();
		return this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO)
				.addSortOrder("V_BIDOPEN_TIME", "DESC").page(pageable, param);

	}

	/**
	 * 
	 * 推送投标人信息到交易平台<br/>
	 * <p>
	 * 推送投标人信息到交易平台
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param appType
	 *            项目类型
	 * @param group
	 *            标段组标识
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/push/{tpid}/{appType}/{group}", desc = "推送投标人信息到交易平台")
	@Service
	public void sendBidders(@PathParam("tpid") String tpid,
			@PathParam("appType") String appType,
			@PathParam("group") String group, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("推送投标人信息到交易平台", data));
		String key = PUSH_URL;
		if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE, appType)
				|| StringUtils.equals(
						ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE,
						appType))
		{
			key = PUSH_FJSZ_URL;
		}
		Record<String, String> headMap = BigTimeOutHttpClientUtils.getHeadMapOfToken();

		String url = SystemParamUtils.getString(key);
		// 获取招标项目投标人信息
		List<Record<String, Object>> bidders = getTenderProjectBidders(tpid,
				group, appType);
		Record<String, Object> bidderInfos = new RecordImpl<String, Object>();
		bidderInfos.setColumn("BIDDERS", bidders);
		String rsJson = null;
		try
		{
			logger.debug(LogUtils.format("发送http请求到数据接口平台!", url, bidderInfos,
					headMap));
			rsJson = BigTimeOutHttpClientUtils.sendJsonPostRequest(url,
					JSON.toJSONString(bidderInfos), headMap,
					ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("推送投标人信息到交易平台发生异常!", rsJson), e);
			throw new ServiceException("", "推送投标人信息到交易平台发生异常!", e);
		}
		logger.debug(LogUtils.format("推送投标人信息到交易平台-rsJson", rsJson));
		JSONObject result = JSON.parseObject(rsJson);
		boolean success = result.getBoolean("success");
		if (success)
		{
			throw new ServiceException("", "发送成功");
		}
		logger.error(result.getString("errorDesc"));
		throw new ServiceException("", result.getString("errorDesc"));
	}

	/**
	 * 
	 * 获取推送的投标人列表<br/>
	 * <p>
	 * 获取推送的投标人列表
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param group
	 *            标段组标识
	 * @param appType
	 *            招标项目类型
	 * @return 投标人列表
	 * @throws ServiceException
	 *             服务异常
	 */
	private List<Record<String, Object>> getTenderProjectBidders(String tpid,
			String group, String appType) throws ServiceException
	{

		String mybatisName = "PushData.getTenderProjectBidders";
		if (StringUtils.equals(group, "1"))
		{
			mybatisName = "PushData.getTenderProjectBiddersByGroup";
		}
		List<Record<String, Object>> bidders = this.activeRecordDAO.statement()
				.selectList(mybatisName, tpid);
		// "ID":"主键",
		// "BIDDER_ORG_CODE":"投标人代码(三证合一或组织机构代码）",
		// "BIDDER_NAME":"投标人名称",
		// "TENDER_PROJECT_ID":"招标项目ID",
		// "BID_SECTION_ID":"标段ID",
		// "BIDDER_TOTAL":"投标总价",
		// "BIDDER_FILE_ENCRYPT_STATE":"投标文件密封情况",
		// "BIDDER_DURATION":"投标工期",
		// "BIDDER_QUALITY_STANDARD":"质量标准",
		// "BIDDER_INVALIDFILE_REASON":"无效投标文件及无效原因",
		// "IS_SUBMIT_BAIL":"是否递交保证金",
		// "MARGIN_PAY_FORM":"保证金递交方式",
		// "PROJECT_LEADER":"项目负责人",
		// "QIYE_ZIZHI_DENGJI":"企业资质等级"
		Record<String, Object> projectInfo = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO)
				.get(new RecordImpl<String, Object>().setColumn("ID", tpid));
		String projectCode = projectInfo.getJSONObject("V_BEM_INFO_JSON")
				.getString("V_CODE");
		try
		{
			// 如果非空
			if (!CollectionUtils.isEmpty(bidders))
			{
				String cbJson = null;
				// 读取系统配置获取项目负责人KEY值
				String fzr = "tbPeName";
				String pushBidders = SystemParamUtils
						.getString(SysParamKey.PUSH_BIDDERS_LEADER);
				JSONObject jsonObject = JSON.parseObject(pushBidders);
				if (StringUtils.isNotEmpty(jsonObject.getString(projectCode)))
				{
					fzr = jsonObject.getString(projectCode);
				}
				for (Record<String, Object> bidder : bidders)
				{
					cbJson = bidder.getString("V_JSON_OBJ");
					String gongqi = BidderElementParseUtils
							.getSingObjAttributeSum(cbJson, "gongqi");
					logger.debug(LogUtils.format("推送投标人信息到交易平台-工期", gongqi));
					bidder.setColumn("BIDDER_FILE_ENCRYPT_STATE", "完好");
					// 投标工期
					bidder.setColumn("BIDDER_DURATION", gongqi);
					// 质量标准
					bidder.setColumn("BIDDER_QUALITY_STANDARD",
							BidderElementParseUtils.getSingObjAttribute(cbJson,
									"zlmb"));
					// 保证金递交方式
					bidder.setColumn("MARGIN_PAY_FORM", BidderElementParseUtils
							.getSingObjAttribute(cbJson, "bzjdjqk"));
					if (null != bidder.getDouble("BIDDER_TOTAL"))
					{
						// 投标总价
						bidder.setColumn("BIDDER_TOTAL",
								FMT_D.format(bidder.getDouble("BIDDER_TOTAL"))
										.toString());
					}
					// 项目负责人
					bidder.setColumn("PROJECT_LEADER", BidderElementParseUtils
							.getSingObjAttribute(cbJson, fzr));
					// 企业资质等级
					bidder.setColumn("QIYE_ZIZHI_DENGJI",
							BidderElementParseUtils.getSingObjAttribute(cbJson,
									"tbTbrCR"));
					// 删除字段
					bidder.remove("V_JSON_OBJ");
				}
			}
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("推送投标人信息到交易平台发生异常!", e), e);
		}
		return bidders;
	}

	/**
	 * 
	 * 开标流程停止，跳转至异议环节<br/>
	 * <p>
	 * 开标流程停止，跳转至异议环节
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             服务异常
	 */
	@Path(value = "gtdf/{tpid}", desc = "开标流程停止，跳转至异议环节")
	@Service
	public void gotoDessentFlow(@PathParam("tpid") String tpid, AeolusData data)
			throws ServiceException
	{

		logger.debug(LogUtils.format("开标流程停止，跳转至异议环节", tpid));

		// 先获取招标流程
		Record<String, Object> project = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO).get(tpid);

		if (CollectionUtils.isEmpty(project))
		{
			throw new ServiceException("", "无法获取到该项目的流程");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("code", project.getJSONObject("V_BEM_INFO_JSON")
				.getString("V_CODE"));
		Record<String, Object> flowInfo = null;
		// 有标段组
		if (project.getInteger("N_IS_SECTION_GROUP") == 1)
		{
			// 再查询流程信息表
			flowInfo = this.activeRecordDAO.auto().table(TableName.FLOW_INFO)
					.setCondition("AND", "N_IS_SECTION_GROUP=1")
					.setCondition("AND", "V_BEM_CODE=#{code}").get(param);
		}
		else
		{
			// 再查询流程信息表
			flowInfo = this.activeRecordDAO.auto().table(TableName.FLOW_INFO)
					.setCondition("AND", "N_IS_SECTION_GROUP=0")
					.setCondition("AND", "V_BEM_CODE=#{code}").get(param);

		}

		// 将流标环节设置成已结束的状态
		modifyFlowNodeStatus(flowInfo.getString("ID"), "信息确认", tpid, "3");
		// 开启开标异议环节
		modifyFlowNodeStatus(flowInfo.getString("ID"), "开标异议", tpid, "2");
	}

	/**
	 * 
	 * 修改流程节点状态<br/>
	 * <p>
	 * 修改流程节点状态
	 * </p>
	 * 
	 * @param flowNodeInfoId
	 *            流程节点ID
	 * @param tpid
	 *            招标项目ID
	 * @param status
	 *            节点状态
	 * @throws ServiceException
	 *             ServiceException
	 */
	private void modifyFlowNodeStatus(String flowInfoID, String nodeName,
			String tpid, String status) throws ServiceException
	{
		logger.debug(LogUtils.format("修改流程节点状态", flowInfoID, tpid, nodeName,
				status));
		Record<String, Object> param = new RecordImpl<String, Object>();

		param.setColumn("flowInfoId", flowInfoID);
		param.setColumn("nodeName", nodeName);
		// 再查流程节点信息表，获取是子节点的节点信息
		Record<String, Object> flowNodeInfo = this.activeRecordDAO.auto()
				.table(TableName.FLOW_NODE_INFO)
				.setCondition("AND", "V_FLOW_ID=#{flowInfoId}")
				.setCondition("AND", "V_NODE_NAME=#{nodeName}")
				.setCondition("AND", "N_DEPTH=2").get(param);

		if (CollectionUtils.isEmpty(flowNodeInfo))
		{
			throw new ServiceException("", "未查询到该节点信息");
		}

		param.setColumn("flowNodeInfoId", flowNodeInfo.getString("ID"));
		param.setColumn("tpid", tpid);
		param.setColumn("N_STATUS", status);
		// 修改流程节点状态
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE)
				.setCondition("AND", "V_FLOW_NODE_ID=#{flowNodeInfoId}")
				.setCondition("AND", "V_TPID=#{tpid}").modify(param);
	}
}
