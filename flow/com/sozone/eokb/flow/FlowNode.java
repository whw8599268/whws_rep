/**
 * 包名：com.sozone.eokb.flow
 * 文件名：FlowNode.java<br/>
 * 创建时间：2017-9-7 上午11:57:57<br/>
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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.dao.validate.RecordValidator;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.orm.DataEntry;
import com.sozone.aeolus.ext.orm.impl.DataEntryImpl;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;

/**
 * 开标流程节点服务接口<br/>
 * <p>
 * 开标流程节点服务接口<br/>
 * </p>
 * Time：2017-9-7 上午11:57:57<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fnode", desc = "开标流程节点服务接口")
public class FlowNode extends BaseAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FlowNode.class);

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
	 * 打开流程节点编辑视图<br/>
	 * <p>
	 * </p>
	 * 
	 * @param flowID
	 *            流程ID
	 * @param data
	 *            AeolusData
	 * @return 视图
	 * @throws FacadeException
	 *             服务异常
	 */
	@Path(value = "view/{flowid}", desc = "打开流程节点编辑视图")
	@Service
	public ModelAndView openFlowNodeEditView(
			@PathParam("flowid") String flowID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开流程节点编辑视图!", flowID, data));
		// 获取流程信息
		Record<String, Object> flow = this.activeRecordDAO.auto()
				.table(TableName.FLOW_INFO).get(flowID);
		// 如果流程不存在
		if (CollectionUtils.isEmpty(flow))
		{
			throw new ServiceException("", "找不到对应的流程信息!");
		}

		String url = "/flow_node/index.html";
		return new ModelAndView(getTheme(data.getHttpServletRequest()) + url,
				flow);
	}

	/**
	 * 上移流程节点位置<br/>
	 * <p>
	 * </p>
	 * 
	 * @param id
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "up/{id}", desc = "上移流程节点位置")
	// POST访问方式
	@HttpMethod(HttpMethod.POST)
	public void moveUp(@PathParam("id") String id, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("上移流程节点位置", id, data));
		// 获取当前流程节点对象
		Record<String, Object> catalog = this.activeRecordDAO.auto()
				.table(TableName.FLOW_NODE_INFO).get(id);
		// 如果不存在
		if (CollectionUtils.isEmpty(catalog))
		{
			return;
		}
		// 父节点ID
		String parentID = catalog.getString("V_PID");
		// 当前层序号
		int serial = catalog.getInteger("N_INDEX");
		// 如果已经是第一
		if (1 == serial)
		{
			return;
		}
		// 要调整到的序号
		int upSerial = serial - 1;

		// 修改要交换的节点
		DataEntry dao = new DataEntryImpl(TableName.FLOW_NODE_INFO);
		dao.setProperty("N_INDEX", serial).and().equalTo("N_INDEX", upSerial);
		// 如果是最顶层
		if (StringUtils.isEmpty(parentID))
		{
			// 条件必须是类型加父节点
			dao.and().isNull("V_PID")
					.equalTo("V_FLOW_ID", catalog.getString("V_FLOW_ID"));
		}
		else
		{
			// 如果不是最顶层
			dao.and().equalTo("V_PID", parentID);
		}
		dao.persist().modify();

		// 修改目标流程节点序号
		DataEntry de = new DataEntryImpl(TableName.FLOW_NODE_INFO);
		de.setProperty("N_INDEX", upSerial).and().equalTo("ID", id);
		de.persist().modify();

	}

	/**
	 * 下移流程节点位置<br/>
	 * <p>
	 * </p>
	 * 
	 * @param catalogID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "down/{id}", desc = "下移流程节点位置")
	// POST访问方式
	@HttpMethod(HttpMethod.POST)
	public void moveDown(@PathParam("id") String catalogID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("下移流程节点位置", catalogID, data));
		// 获取当前流程节点对象
		Record<String, Object> catalog = this.activeRecordDAO.auto()
				.table(TableName.FLOW_NODE_INFO).get(catalogID);
		// 如果不存在
		if (CollectionUtils.isEmpty(catalog))
		{
			return;
		}

		// 父节点ID
		String parentID = catalog.getString("V_PID");
		// 获取最大值
		DataEntry d = new DataEntryImpl(TableName.FLOW_NODE_INFO);
		d.select("MAX(N_INDEX) AS MAX_SERIAL");

		// 如果是最顶层
		if (StringUtils.isEmpty(parentID))
		{
			// 条件必须是类型加父节点
			d.and().isNull("V_PID")
					.equalTo("V_FLOW_ID", catalog.getString("V_FLOW_ID"));
		}
		else
		{
			// 如果不是最顶层
			d.and().equalTo("V_PID", parentID);
		}
		Record<String, Object> record = d.persist().get();
		int max = record.getInteger("MAX_SERIAL");
		// 当前层序号
		int serial = catalog.getInteger("N_INDEX");
		// 如果已经是最后一个了
		if (max == serial)
		{
			return;
		}
		// 要调整到的序号
		int upSerial = serial + 1;

		// 修改要交换的节点
		DataEntry dao = new DataEntryImpl(TableName.FLOW_NODE_INFO);
		dao.setProperty("N_INDEX", serial).and().equalTo("N_INDEX", upSerial);
		// 如果是最顶层
		if (StringUtils.isEmpty(parentID))
		{
			// 条件必须是类型加父节点
			dao.and().isNull("V_PID")
					.equalTo("V_FLOW_ID", catalog.getString("V_FLOW_ID"));
		}
		else
		{
			// 如果不是最顶层
			dao.and().equalTo("V_PID", parentID);
		}
		dao.persist().modify();

		// 修改目标流程节点序号
		DataEntry de = new DataEntryImpl(TableName.FLOW_NODE_INFO);
		de.setProperty("N_INDEX", upSerial).and().equalTo("ID", catalogID);
		de.persist().modify();
	}

	/**
	 * 获取指定流程节点信息树型列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param flowID
	 *            流程ID
	 * @param data
	 *            AeolusData
	 * @return 流程节点信息列表
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "tree/{flowid}", desc = "获取指定流程节点信息树型列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getTreeList(
			@PathParam("flowid") String flowID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取指定流程节点信息树型列表", data));
		DataEntry de = new DataEntryImpl(TableName.FLOW_NODE_INFO);
		de.and().equalTo("V_FLOW_ID", flowID);
		// 设置排序关系
		de.orderBy("N_DEPTH", "ASC").orderBy("N_INDEX", "ASC");
		// 根据深度升序,序号升序
		List<Record<String, Object>> catalogs = de.persist().load();
		return TreeService.getTreeList(catalogs, "ID", "V_NODE_NAME");
	}

	/**
	 * 增加流程节点信息对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "", desc = "增加流程节点信息对象")
	// POST访问方式
	@HttpMethod(HttpMethod.POST)
	public void addFlowNode(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("增加流程节点信息对象", data));
		Record<String, Object> catalog = data
				.getTableRecord(TableName.FLOW_NODE_INFO);
		// 获取父项ID
		String pid = catalog.getString("V_PID");
		DataEntry de = new DataEntryImpl(TableName.FLOW_NODE_INFO);
		de.select("MAX(N_INDEX) AS MAX_SERIAL");
		de.and().equalTo("V_FLOW_ID", catalog.getString("V_FLOW_ID"));
		// 如果是最顶层
		if (null == pid)
		{
			catalog.setColumn("N_DEPTH", 1);
			de.and().isNull("V_PID");
		}
		else
		{
			// 如果不是最顶层
			DataEntry dep = new DataEntryImpl(TableName.FLOW_NODE_INFO);
			dep.and().equalTo("ID", pid);
			// 获取父节点
			Record<String, Object> parent = dep.persist().get();
			catalog.setColumn("N_DEPTH", parent.getInteger("N_DEPTH") + 1);
			de.and().equalTo("V_PID", pid);
		}
		// 查询最大子序号
		Record<String, Object> max = de.persist().get();
		// 最大子序号
		Integer maxSerial = max.getInteger("MAX_SERIAL");
		if (null == maxSerial)
		{
			maxSerial = 0;
		}
		catalog.setColumn("N_INDEX", maxSerial + 1);
		// 设置评标规则流程节点ID
		catalog.setColumn("ID", Random.generateUUID());
		// 设置创建人创建时间
		catalog.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
				.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		// 设置修改人修改时间
		catalog.setColumn("N_UPDATE_TIME", System.currentTimeMillis())
				.setColumn("V_UPDATE_USER", ApacheShiroUtils.getCurrentUserID());
		// 校验入参
		RecordValidator.validateRecord(TableName.FLOW_NODE_INFO, catalog);
		this.activeRecordDAO.auto().table(TableName.FLOW_NODE_INFO)
				.save(catalog);
	}

	/**
	 * 修改评标规则流程节点信息对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "modify", desc = "修改评标规则流程节点信息对象")
	// POST访问方式
	@HttpMethod(HttpMethod.POST)
	public void modifyFlowNode(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("修改评标规则流程节点信息对象", data));
		Record<String, Object> catalog = data
				.getTableRecord(TableName.FLOW_NODE_INFO);
		// 为了过校验
		catalog.setColumn("V_FLOW_ID", Random.generateUUID());
		catalog.setColumn("N_INDEX", 1);
		catalog.setColumn("N_DEPTH", 1);
		// 校验入参
		RecordValidator.validateRecord(TableName.FLOW_NODE_INFO, catalog);
		// 不允许修改的删掉
		catalog.remove("V_FLOW_ID");
		catalog.remove("N_INDEX");
		catalog.remove("N_DEPTH");
		// 设置修改人修改时间
		catalog.setColumn("N_UPDATE_TIME", System.currentTimeMillis())
				.setColumn("V_UPDATE_USER", ApacheShiroUtils.getCurrentUserID());
		this.activeRecordDAO.auto().table(TableName.FLOW_NODE_INFO)
				.modify(catalog);
	}

	/**
	 * 删除流程节点信息对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param ids
	 *            需要删除的流程节点ID数组
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "remove/{ids}", desc = "删除流程节点信息对象")
	@Service
	public void removeFlowNodes(@PathParam("ids") String ids)
			throws FacadeException
	{
		logger.debug(LogUtils.format("删除流程节点信息对象!", ids));
		String[] catalogIDs = StringUtils.split(ids, ",");
		Record<String, Object> catalog = null;
		// 如果是子节点
		String sql = "UPDATE "
				+ TableName.FLOW_NODE_INFO
				+ " SET N_INDEX = N_INDEX-1 WHERE V_PID = #{pid} AND N_INDEX > #{serial}";
		// 如果是顶层节点
		String sql2 = "UPDATE "
				+ TableName.FLOW_NODE_INFO
				+ " SET N_INDEX = N_INDEX-1 WHERE V_PID IS NULL AND V_FLOW_ID = #{flowid} AND N_INDEX > #{serial}";
		for (String catalogID : catalogIDs)
		{
			// 获取当前流程节点对象
			catalog = this.activeRecordDAO.auto()
					.table(TableName.FLOW_NODE_INFO).get(catalogID);
			if (CollectionUtils.isEmpty(catalog))
			{
				continue;
			}
			// 递归删除评标规则流程节点信息
			removeFlowNode(catalog);
			// 修改同级别后面子节点的序号
			if (StringUtils.isEmpty(catalog.getString("V_PID")))
			{
				this.activeRecordDAO.sql(sql2)
						.setParam("flowid", catalog.getString("V_FLOW_ID"))
						.setParam("serial", catalog.getInteger("N_INDEX"))
						.update();
			}
			else
			{
				this.activeRecordDAO.sql(sql)
						.setParam("pid", catalog.getString("V_PID"))
						.setParam("serial", catalog.getInteger("N_INDEX"))
						.update();
			}
		}
	}

	/**
	 * 递归删除评标规则流程节点信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param catalog
	 * @throws ServiceException
	 */
	private void removeFlowNode(Record<String, Object> catalog)
			throws ServiceException
	{
		List<Record<String, Object>> children = this.activeRecordDAO
				.auto()
				.table(TableName.FLOW_NODE_INFO)
				.setCondition("AND", "V_PID = #{pid}")
				.list(new RecordImpl<String, Object>().setColumn("pid",
						catalog.getString("ID")));
		if (CollectionUtils.isEmpty(children))
		{
			// 删除评标规则流程节点信息
			this.activeRecordDAO.auto().table(TableName.FLOW_NODE_INFO)
					.remove(catalog.getString("ID"));
			return;
		}
		for (Record<String, Object> record : children)
		{
			removeFlowNode(record);
		}
		// 删除评标规则流程节点信息
		this.activeRecordDAO.auto().table(TableName.FLOW_NODE_INFO)
				.remove(catalog.getString("ID"));
	}

}
