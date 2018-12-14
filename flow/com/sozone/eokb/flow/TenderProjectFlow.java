/**
 * 包名：com.sozone.eokb.flow
 * 文件名：TenderProjectFlow.java<br/>
 * 创建时间：2017-9-7 下午5:22:27<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.flow;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.Auto.Table;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.flow.common.NodeStatus;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 招标项目流程服务接口<br/>
 * <p>
 * 招标项目流程服务接口<br/>
 * </p>
 * Time：2017-9-7 下午5:22:27<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "tpflow", desc = "招标项目流程服务接口")
@Permission(Level.Authenticated)
public class TenderProjectFlow extends BaseAction
{

	/**
	 * 项目锁
	 */
	private static final Map<String, ReentrantLock> PROJECT_LOCK = new ConcurrentHashMap<String, ReentrantLock>();

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(TenderProjectFlow.class);

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
	 * 获取招标项目锁<br/>
	 * <p>
	 * 为了防止同一个项目在同一个时间段内重复操作,所有为每一个项目加了一个同步锁,该方法用于创建项目单例互斥锁
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 互斥锁锁对象
	 */
	private static synchronized ReentrantLock getTPLock(String tpid)
	{
		// 获取项目锁
		ReentrantLock lock = PROJECT_LOCK.get(tpid);
		// 如果锁不存在
		if (null == lock)
		{
			// 构建互斥锁
			lock = new ReentrantLock();
			PROJECT_LOCK.put(tpid, lock);
		}
		return lock;
	}

	/**
	 * 打开招标项目流程页面<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 招标项目流程页面
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "view", desc = "打开招标项目流程页面")
	@Service
	public ModelAndView openFlowStatusView(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开招标项目流程页面!", data));
		// 流程ID
		String flowID = SessionUtils.getTenderProjectFlowID();
		// 招标项目ID
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("flowid", flowID);
		param.setColumn("tpid", tpid);
		// 查记录
		int count = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "V_FLOW_ID = #{flowid}")
				.setCondition("AND", "V_TPID = #{tpid}").count(param);
		if (count <= 0)
		{
			// 初始化
			initTenderProjectFlow(flowID, tpid);
		}
		// 查询列表
		Table lt = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "V_FLOW_ID = #{flowid}")
				.setCondition("AND", "V_TPID = #{tpid}");
		// 投标人
		if (SessionUtils.isBidder())
		{
			lt.setCondition("AND", "N_BIDDER_IS_SHOW = 1");
		}
		else
		{
			// 招标人
			lt.setCondition("AND", "N_TENDERER_IS_SHOW = 1");
		}

		// 获取招标项目流程节点
		List<Record<String, Object>> flowNodes = lt
				.addSortOrder("N_DEPTH", "ASC").addSortOrder("N_INDEX", "ASC")
				.list(param);
		// 获取树形结构
		List<Record<String, Object>> rootNodes = TreeService.getTreeList(
				flowNodes, "V_NODE_ID", "V_NODE_NAME");
		String url = "/metting_room/tender.project.flow.html";
		Map<String, Object> mode = new HashMap<String, Object>();
		mode.put("NODE_TREE", rootNodes);
		mode.put("NODE_TREE_JSON", JSON.toJSONString(rootNodes));
		mode.put("IS_BIDDER", SessionUtils.isBidder());
		return new ModelAndView(getTheme(data.getHttpServletRequest()) + url,
				mode);
	}

	/**
	 * 打开招标项目流程节点页面<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            招标项目流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 招标项目流程页面
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "node/{tpnid}", desc = "打开招标项目流程节点页面")
	@Service
	public ModelAndView openNodeView(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils
				.format("打开招标项目流程节点页面!", tenderProjectNodeID, data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("status", NodeStatus.NotStarted.getStatus());

		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}")
				.setCondition("AND", "N_STATUS != #{status}").get(param);
		if (CollectionUtils.isEmpty(tpNode))
		{
			throw new FacadeException("", "找不到流程节点,或者流程节点尚未开始!");
		}

		StringBuilder url = new StringBuilder("");
		// 投标人
		if (SessionUtils.isBidder())
		{
			url.append(tpNode.getString("V_BIDDER_PAGE_URL"));
		}
		else
		{
			// 招标人
			url.append(tpNode.getString("V_TENDERER_PAGE_URL"));
		}
		Map<String, Object> mode = new HashMap<String, Object>();
		mode.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		try
		{
			// 打开视图
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ url.toString(), mode);
		}
		catch (Exception e)
		{
			String estMsg = getExceptionStackTrace(e);
			mode.put("errorDesc", estMsg);
			// 打开视图
			return new ModelAndView("/default/error.html", mode);
		}
	}

	/**
	 * 打印异常堆栈<br/>
	 * <p>
	 * </p>
	 * 
	 * @param e
	 * @return
	 */
	private static String getExceptionStackTrace(Exception e)
	{
		StringBuilderWriter sb = new StringBuilderWriter();
		e.printStackTrace(new PrintWriter(sb));
		String rs = sb.toString();
		rs = StringUtils.replace(rs, "\r\n", "<br/>");
		rs = StringUtils.replace(rs, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
		return rs;
	}

	/**
	 * 结束招标项目流程节点<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            招标项目流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 下一个开始的节点对象,如果没有就是空对象
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "finish/{tpnid}", desc = "结束招标项目流程节点")
	@Service
	public Record<String, Object> finishFlowNode(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.info(LogUtils.format("结束招标项目流程节点!", tenderProjectNodeID, data));
		// 招标项目ID
		String tpid = SessionUtils.getTPID();
		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPLock(tpid);
		logger.info(LogUtils.format("获取锁实例成功"));
		// 获取锁,这一步其实就是为了让线程挂起
		lock.lock();
		logger.info(LogUtils.format("获取锁成功"));
		try
		{
			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("tpnoid", tenderProjectNodeID);
			Record<String, Object> tpNode = this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_FLOW_VIEW)
					.setCondition("AND", "ID = #{tpnoid}").get(param);

			if (StringUtils.isEmpty(SessionUtils.getTPID()))
			{
				throw new FacadeException("", "会议中断，请重新刷新网页！");
			}

			if (CollectionUtils.isEmpty(tpNode))
			{
				throw new FacadeException("", "找不到流程节点!");
			}
			// 如果已经结束的节点
			if (NodeStatus.HaveClosed.getStatus() == tpNode
					.getInteger("N_STATUS"))
			{
				throw new FacadeException("", "当前流程节点已经结束!");
			}
			// 修改当前节点状态
			Record<String, Object> temp = new RecordImpl<String, Object>();
			temp.setColumn("ID", tpNode.getString("ID"));
			temp.setColumn("N_STATUS", NodeStatus.HaveClosed.getStatus());
			this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE)
					.modify(temp);
			// 修改下一个节点的状态
			return unlockNextFlowNode(tpNode);
		}
		finally
		{
			// 如果已经没有线程等待获取锁
			if (!lock.hasQueuedThreads())
			{
				// 干掉锁
				PROJECT_LOCK.remove(tpid);
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	/**
	 * 结束招标项目流程<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "finish", desc = "结束招标项目流程")
	@Service
	public void finishTenderProjectFlow(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("结束招标项目流程!", data));
		// 流程ID
		String flowID = SessionUtils.getTenderProjectFlowID();
		// 招标项目ID
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("flowID", flowID);
		param.setColumn("tpid", tpid);
		param.setColumn("N_STATUS", NodeStatus.HaveClosed.getStatus());
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE)
				.setCondition("AND", "V_TPID = #{tpid}")
				// .setCondition("AND", "V_FLOW_NODE_ID = #{flowID}")
				.modify(param);
	}

	/**
	 * 开启下一个流程节点<br/>
	 * <p>
	 * </p>
	 * 
	 * @param currentNode
	 *            当前流程节点
	 * @return 下一个开始的节点对象
	 * @throws ServiceException
	 *             服务异常
	 */
	protected Record<String, Object> unlockNextFlowNode(
			Record<String, Object> currentNode) throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("flowid", currentNode.getString("V_FLOW_ID"));
		param.setColumn("tpid", currentNode.getString("V_TPID"));
		param.setColumn("index", currentNode.getInteger("N_INDEX") + 1);
		param.setColumn("pid", currentNode.getString("V_PID"));
		Table query = this.activeRecordDAO.auto().table(
				TableName.TENDER_PROJECT_FLOW_VIEW);
		// 如果是顶层
		if (StringUtils.isEmpty(currentNode.getString("V_PID")))
		{
			query.setCondition("AND", "V_PID IS NULL");
		}
		else
		{
			query.setCondition("AND", "V_PID = #{pid}");
		}
		// 先查询同级别下一个节点
		Record<String, Object> nextNode = query
				.setCondition("AND", "V_FLOW_ID = #{flowid}")
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "N_INDEX = #{index}").get(param);
		// 如果同级别的下一个节点不存在,说明已经是当前父节点的最后一个子节点了,此时开始递归结束当前父节点
		if (CollectionUtils.isEmpty(nextNode))
		{
			nextNode = finishParentFlowNode(currentNode);
		}
		// 递归开启下一个节点
		if (CollectionUtils.isEmpty(nextNode))
		{
			// 最后一个节点
			return new RecordImpl<String, Object>();
		}
		// 开启下一个节点
		return startUpChildFlowNode(nextNode);
	}

	/**
	 * 递归开启子节点<br/>
	 * <p>
	 * </p>
	 * 
	 * @param parent
	 *            父节点
	 * @return 下一个开始的节点对象
	 * @throws ServiceException
	 *             服务异常
	 */
	private Record<String, Object> startUpChildFlowNode(
			Record<String, Object> parent) throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 修改当前节点状态
		param.clear();
		param.setColumn("ID", parent.getString("ID"));
		param.setColumn("N_STATUS", NodeStatus.InProgress.getStatus());
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE)
				.modify(param);
		// 保存节点状态时间
		param.setColumn("N_START_TIME", System.currentTimeMillis());
		this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_STATUS_TIME).save(param);
		// 开始处理子节点
		param.clear();
		param.setColumn("flowid", parent.getString("V_FLOW_ID"));
		param.setColumn("tpid", parent.getString("V_TPID"));
		param.setColumn("pid", parent.getString("V_NODE_ID"));
		// 获取子节点
		Record<String, Object> child = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "V_PID = #{pid}")
				.setCondition("AND", "V_FLOW_ID = #{flowid}")
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "N_INDEX = 1").get(param);
		// 如果不存在
		if (CollectionUtils.isEmpty(child))
		{
			return parent;
		}
		return startUpChildFlowNode(child);
	}

	/**
	 * 递归结束父节点<br/>
	 * <p>
	 * </p>
	 * 
	 * @param child
	 *            子节点
	 * @return 最后一级结束的节点
	 * @throws ServiceException
	 *             服务异常
	 */
	private Record<String, Object> finishParentFlowNode(
			Record<String, Object> child) throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 如果是最顶层节点
		if (StringUtils.isEmpty(child.getString("V_TPID")))
		{
			// 看看下一个节点
			param.clear();
			param.setColumn("flowid", child.getString("V_FLOW_ID"));
			param.setColumn("tpid", child.getString("V_TPID"));
			param.setColumn("index", child.getInteger("N_INDEX") + 1);
			// 返回下一个顶层节点
			return this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_FLOW_VIEW)
					.setCondition("AND", "V_PID IS NULL")
					.setCondition("AND", "V_FLOW_ID = #{flowid}")
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "N_INDEX = #{index}").get(param);
		}
		param.clear();
		param.setColumn("pid", child.getString("V_PID"));
		param.setColumn("tpid", child.getString("V_TPID"));
		Record<String, Object> parent = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "V_NODE_ID = #{pid}")
				.setCondition("AND", "V_TPID = #{tpid}").get(param);
		if (CollectionUtils.isEmpty(parent))
		{
			return null;
		}
		// 修改当前节点状态
		param.clear();
		param.setColumn("ID", parent.getString("ID"));
		param.setColumn("N_STATUS", NodeStatus.HaveClosed.getStatus());
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE)
				.modify(param);
		// 保存节点状态时间
		param.setColumn("N_START_TIME", System.currentTimeMillis());
		this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_STATUS_TIME).save(param);

		// 看看下一个节点
		param.clear();
		param.setColumn("flowid", parent.getString("V_FLOW_ID"));
		param.setColumn("tpid", parent.getString("V_TPID"));
		param.setColumn("index", parent.getInteger("N_INDEX") + 1);
		param.setColumn("pid", parent.getString("V_PID"));
		Table query = this.activeRecordDAO.auto().table(
				TableName.TENDER_PROJECT_FLOW_VIEW);
		// 如果是顶层
		if (StringUtils.isEmpty(parent.getString("V_PID")))
		{
			query.setCondition("AND", "V_PID IS NULL");
		}
		else
		{
			query.setCondition("AND", "V_PID = #{pid}");
		}
		// 先查询同级别下一个节点
		Record<String, Object> nextNode = query
				.setCondition("AND", "V_FLOW_ID = #{flowid}")
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "N_INDEX = #{index}").get(param);
		// 如果存在同级下一个节点
		if (!CollectionUtils.isEmpty(nextNode))
		{
			return nextNode;
		}
		return finishParentFlowNode(parent);
	}

	/**
	 * 初始化招标项目流程<br/>
	 * <p>
	 * </p>
	 */
	private void initTenderProjectFlow(String flowID, String tpid)
			throws ServiceException
	{
		logger.info(LogUtils.format("初始化招标项目流程", flowID, tpid));
		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPLock(tpid);
		logger.info(LogUtils.format("获取锁实例成功"));
		// 获取锁,这一步其实就是为了让线程挂起
		lock.lock();
		logger.info(LogUtils.format("获取锁成功"));
		StatefulDAO statefulDAO = null;
		try
		{
			statefulDAO = new StatefulDAOImpl();
			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("flowid", flowID);
			param.setColumn("tpid", tpid);
			// 查记录
			int count = statefulDAO.auto()
					.table(TableName.TENDER_PROJECT_FLOW_VIEW)
					.setCondition("AND", "V_FLOW_ID = #{flowid}")
					.setCondition("AND", "V_TPID = #{tpid}").count(param);
			if (count <= 0)
			{

				List<Record<String, Object>> flowNodes = statefulDAO
						.auto()
						.table(TableName.FLOW_NODE_INFO)
						.setCondition("AND", "V_FLOW_ID = #{flowid}")
						.addSortOrder("N_DEPTH", "ASC")
						.addSortOrder("N_INDEX", "ASC")
						.list(new RecordImpl<String, Object>().setColumn(
								"flowid", flowID));
				if (CollectionUtils.isEmpty(flowNodes))
				{
					return;
				}
				// 项目节点链表
				List<Record<String, Object>> tpNodes = new LinkedList<Record<String, Object>>();
				Record<String, Object> tpNode = null;
				Map<String, Record<String, Object>> idMap = new HashMap<String, Record<String, Object>>();
				for (Record<String, Object> node : flowNodes)
				{
					tpNode = new RecordImpl<String, Object>();
					tpNode.setColumn("ID", Random.generateUUID());
					tpNode.setColumn("V_FLOW_NODE_ID", node.getString("ID"));
					tpNode.setColumn("V_PID", node.getString("V_PID"));
					tpNode.setColumn("N_STATUS",
							NodeStatus.NotStarted.getStatus());
					// 如果是启动节点
					if (1 == node.getInteger("N_IS_START_UP_NODE"))
					{
						tpNode.setColumn("N_STATUS",
								NodeStatus.InProgress.getStatus());
						// 递归设置父节点状态
						setParentStatus(idMap, node.getString("V_PID"),
								NodeStatus.InProgress);
					}

					// 放入map,注意这里的Key是流程节点的ID
					idMap.put(node.getString("ID"), tpNode);
					tpNode.setColumn("V_TPID", tpid);
					tpNode.setColumn("V_CREATE_USER",
							ApacheShiroUtils.getCurrentUserID()).setColumn(
							"N_CREATE_TIME", System.currentTimeMillis());
					tpNodes.add(tpNode);
				}
				// 保存所有
				statefulDAO.auto().table(TableName.TENDER_PROJECT_NODE)
						.save(tpNodes);
			}
			// 提交事务
			statefulDAO.commit();
		}
		catch (ServiceException e)
		{
			// 回滚事务
			if (null != statefulDAO)
			{
				statefulDAO.rollback();
			}
			throw e;
		}
		finally
		{
			// 关闭事务
			if (null != statefulDAO)
			{
				statefulDAO.close();
			}
			// 如果已经没有线程等待获取锁
			if (!lock.hasQueuedThreads())
			{
				// 干掉锁
				PROJECT_LOCK.remove(tpid);
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	/**
	 * 递归设置父节点状态<br/>
	 * <p>
	 * </p>
	 * 
	 * @param idMap
	 * @param parentID
	 * @param status
	 */
	private void setParentStatus(Map<String, Record<String, Object>> idMap,
			String parentID, NodeStatus status)
	{
		Record<String, Object> parent = idMap.get(parentID);
		if (CollectionUtils.isEmpty(parent))
		{
			return;
		}
		// 设置父节点状态
		parent.setColumn("N_STATUS", status.getStatus());
		setParentStatus(idMap, parent.getString("V_PID"), status);
	}

}
