/**
 * 包名：com.sozone.eokb.flow
 * 文件名：TenderProjectFlowAction.java<br/>
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
package com.sozone.eokb.mobile.bus.flow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.Auto.Table;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.flow.TreeService;
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
@Path(value = "mobile/tpflow", desc = "招标项目流程服务接口")
@Permission(Level.Authenticated)
public class TenderProjectFlowAction extends BaseAction
{

	/**
	 * 项目锁
	 */
	private static final Map<String, ReentrantLock> PROJECT_LOCK = new ConcurrentHashMap<String, ReentrantLock>();

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(TenderProjectFlowAction.class);

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
	@Path(value = "list", desc = "打开招标项目流程页面")
	@Service
	public Map<String, Object> getTpFlowList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开招标项目流程页面!", data));
		String tpID = data.getParam("tpID");

		Record<String, Object> projectInfo = this.activeRecordDAO.pandora()
				.SELECT_ALL_FROM(TableName.EKB_T_TENDER_PROJECT_INFO)
				.EQUAL("ID", tpID).get();

		if (CollectionUtils.isEmpty(projectInfo))
		{
			throw new FacadeException("", "未获取到招标项目信息");
		}
		// 流程ID
		String flowID = getFlowID(projectInfo);

		// 招标项目ID
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("flowid", flowID);
		param.setColumn("tpid", tpID);
		// 查记录
		int count = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "V_FLOW_ID = #{flowid}")
				.setCondition("AND", "V_TPID = #{tpid}").count(param);
		if (count <= 0)
		{
			// 初始化
			initTenderProjectFlow(flowID, tpID);
		}
		// 查询列表
		Table lt = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "V_FLOW_ID = #{flowid}")
				.setCondition("AND", "V_TPID = #{tpid}");
		// 投标人
		// if (SessionUtils.isBidder())
		// {
		lt.setCondition("AND", "N_BIDDER_IS_SHOW = 1");
		// }
		// else
		// {
		// // 招标人
		// lt.setCondition("AND", "N_TENDERER_IS_SHOW = 1");
		// }

		// 获取招标项目流程节点
		List<Record<String, Object>> flowNodes = lt
				.addSortOrder("N_DEPTH", "ASC").addSortOrder("N_INDEX", "ASC")
				.list(param);
		// 获取树形结构
		List<Record<String, Object>> rootNodes = TreeService.getTreeList(
				flowNodes, "V_NODE_ID", "V_NODE_NAME");
		Map<String, Object> mode = new HashMap<String, Object>();
		List<Record<String, Object>> menuNodes = new LinkedList<Record<String, Object>>();
		List<Record<String, Object>> childrenNodes = null;
		for (Record<String, Object> node : rootNodes)
		{
			childrenNodes = node.getList("children");
			if (CollectionUtils.isEmpty(childrenNodes))
			{
				continue;
			}
			for (Record<String, Object> childNode : childrenNodes)
			{
				menuNodes.add(childNode);
			}
		}

		// 获取项目信息
		List<Record<String, Object>> infos = new LinkedList<Record<String, Object>>();
		param.clear();

		param = new RecordImpl<String, Object>();
		param.setColumn("label", "招标人");
		param.setColumn("value", projectInfo.getString("V_TENDERER_NAME"));
		infos.add(param);

		param = new RecordImpl<String, Object>();
		param.setColumn("label", "评标办法");
		param.setColumn("value", projectInfo.getJSONObject("V_BEM_INFO_JSON")
				.getString("V_BID_EVALUATION_METHOD_NAME"));
		infos.add(param);

		mode.put("NODE_LIST", menuNodes);
		mode.put("PROJECT_NAME", projectInfo.getString("V_TENDER_PROJECT_NAME"));
		mode.put("INFOS", infos);
		mode.put("IS_BIDDER", SessionUtils.isBidder());

		return mode;
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

	/**
	 * 获取流程主键<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProject
	 *            招标项目
	 * @return flowID
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static String getFlowID(Record<String, Object> tenderProject)
			throws ServiceException
	{
		// 获取招标项目流程信息
		String bemJson = tenderProject.getString("V_BEM_INFO_JSON");
		if (StringUtils.isEmpty(bemJson))
		{
			// 获取不到评标办法
			throw new ValidateException("", "无法获取招标项目对应的评标办法信息");
		}
		JSONObject bem = JSON.parseObject(bemJson);
		String code = bem.getString("V_CODE");
		// 判断是否有标段组
		Integer flag = tenderProject.getInteger("N_IS_SECTION_GROUP");
		if (null == flag)
		{
			flag = 0;
		}
		// 查询流程
		Record<String, Object> flow = ActiveRecordDAOImpl.getInstance()
				.pandora().SELECT("ID").FROM(TableName.FLOW_INFO)
				.EQUAL("V_BEM_CODE", code).EQUAL("N_IS_SECTION_GROUP", flag)
				.get();
		if (CollectionUtils.isEmpty(flow))
		{
			throw new ServiceException("", "无法获取到对应的开标流程");
		}
		return flow.getString("ID");
	}
}
