/**
 * 包名：com.sozone.eokb.flow
 * 文件名：Flow.java<br/>
 * 创建时间：2017-9-6 下午7:31:01<br/>
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
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.dao.validate.RecordValidator;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.eokb.common.ConstantEOKB.TableName;

/**
 * 开标流程服务接口<br/>
 * <p>
 * 开标流程服务接口<br/>
 * </p>
 * Time：2017-9-6 下午7:31:01<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/flow", desc = "开标流程服务接口")
public class Flow
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Flow.class);

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
	 * 获取符合搜索条件的开标流程信息分页封装<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 开标流程信息分页封装
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "", desc = "获取符合搜索条件的开标流程信息分页封装")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getFlowsPage(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取符合搜索条件的开标流程信息分页封装封", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = data.getRecord();
		return this.activeRecordDAO.auto().table(TableName.FLOW_INFO)
				.addSortOrder("N_CREATE_TIME", "DESC").page(pageable, param);
	}

	/**
	 * 增加开标流程信息对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "", desc = "增加开标流程信息对象")
	// POST访问方式
	@HttpMethod(HttpMethod.POST)
	public void addFlow(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("增加开标流程信息对象", data));
		Record<String, Object> flow = data.getTableRecord(TableName.FLOW_INFO);
		// 设置开标ID
		flow.setColumn("ID", Random.generateUUID());
		// 设置创建人创建时间
		flow.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
				.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		// 校验入参
		RecordValidator.validateRecord(TableName.FLOW_INFO, flow);
		this.activeRecordDAO.auto().table(TableName.FLOW_INFO).save(flow);
	}

	/**
	 * 修改开标流程信息对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "modify", desc = "修改开标流程信息对象")
	// POST访问方式
	@HttpMethod(HttpMethod.POST)
	public void modifyFlow(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("修改开标流程信息对象", data));
		Record<String, Object> flow = data.getTableRecord(TableName.FLOW_INFO);
		// 校验入参
		RecordValidator.validateRecord(TableName.FLOW_INFO, flow);
		this.activeRecordDAO.auto().table(TableName.FLOW_INFO).modify(flow);
	}

	/**
	 * 删除开标流程信息对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param fids
	 *            需要删除的开标ID数组
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "remove/{fids}", desc = "删除开标流程信息对象")
	@Service
	public void removeFlows(@PathParam("fids") String fids)
			throws FacadeException
	{
		logger.debug(LogUtils.format("删除开标流程信息对象!", fids));
		String[] flowIDs = StringUtils.split(fids, ",");
		Record<String, Object> param = new RecordImpl<String, Object>();
		for (String flowID : flowIDs)
		{
			param.clear();
			param.setColumn("flowID", flowID);
			// 删除流程节点
			this.activeRecordDAO.auto().table(TableName.FLOW_NODE_INFO)
					.setCondition("AND", "V_FLOW_ID = #{flowID}").remove(param);
			// 删除开标流程
			this.activeRecordDAO.auto().table(TableName.FLOW_INFO)
					.remove(flowID);
		}
	}

}
