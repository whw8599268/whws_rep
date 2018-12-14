/**
 * 包名：com.sozone.eokb.bus.bidlog
 * 文件名：BidOperationLog.java<br/>
 * 创建时间：2018-4-3 上午9:03:18<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.bidlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 开标操作log<br/>
 * <p>
 * 开标操作log<br/>
 * </p>
 * Time：2018-4-3 上午9:03:18<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/bidlog", desc = "开标操作log")
// 登录即可访问
@Permission(Level.Authenticated)
public class BidOperationLog
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(BidOperationLog.class);

	/**
	 * 
	 * 获取DAO<br/>
	 * <p>
	 * 获取DAO
	 * </p>
	 * 
	 * @return DAO
	 */
	private static ActiveRecordDAO getActiveRecordDAO()
	{
		return ActiveRecordDAOImpl.getInstance();
	}

	/**
	 * 
	 * 获取符合条件的log分页<br/>
	 * <p>
	 * 获取符合条件的log分页
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return log分页
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "", desc = "获取符合条件的log分页")
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getLogs(AeolusData data)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取符合条件的log分页", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = data.getRecord();
		return getActiveRecordDAO().auto().table(TableName.EKB_T_OPERATION_LOG)
				.page(pageable, param);
	}

	/**
	 * 
	 * 保存操作日志信息<br/>
	 * <p>
	 * 保存操作日志信息
	 * </p>
	 * 
	 * @param memo
	 *            备注
	 * @param sourceValue
	 *            源值
	 * @param finalValue
	 *            目标值
	 * @throws ServiceException
	 *             log分页
	 */
	public static void addOperationLog(String memo, String sourceValue,
			String finalValue) throws ServiceException
	{
		logger.debug(LogUtils.format("保存操作日志信息", memo, sourceValue, finalValue));

		Record<String, Object> log = new RecordImpl<String, Object>();
		// 主键
		log.put("ID", Random.generateUUID());
		// 招标项目ID
		log.put("V_TPID", SessionUtils.getTPID());
		// 项目名称
		log.put("V_TENDER_PROJECT_NAME", SessionUtils.getBidProjectName());
		// 开标时间
		log.put("V_BIDOPEN_TIME", SessionUtils.getBidOpenTime());
		// 操作说明
		log.put("V_OPERATION_MEMO", memo);
		// 源值
		log.put("V_SOURCE_VALUE", sourceValue);
		// 目标值
		log.put("V_FINAL_VALUE", finalValue);
		// 操作时间
		log.put("N_OPERATION_TIME", System.currentTimeMillis());
		// 操作人
		log.put("V_OPERATION_USER", ApacheShiroUtils.getCurrentUserName());

		getActiveRecordDAO().auto().table(TableName.EKB_T_OPERATION_LOG)
				.save(log);
	}

	/**
	 * 
	 * 保存操作日志信息<br/>
	 * <p>
	 * 保存操作日志信息
	 * </p>
	 * 
	 * @param memo
	 *            备注
	 * @param sourceValue
	 *            源值
	 * @param finalValue
	 *            目标值
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static void addOperationLog(String memo, String sourceValue,
			String finalValue, String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("保存操作日志信息", memo, sourceValue, finalValue));

		Record<String, Object> projectInfo = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO)
				.get(new RecordImpl<String, Object>().setColumn("ID", tpid));
		if (!CollectionUtils.isEmpty(projectInfo))
		{
			Record<String, Object> log = new RecordImpl<String, Object>();
			// 主键
			log.put("ID", Random.generateUUID());
			// 招标项目ID
			log.put("V_TPID", tpid);
			// 项目名称
			log.put("V_TENDER_PROJECT_NAME",
					projectInfo.getString("V_TENDER_PROJECT_NAME"));
			// 开标时间
			log.put("V_BIDOPEN_TIME", projectInfo.getString("V_BIDOPEN_TIME"));
			// 操作说明
			log.put("V_OPERATION_MEMO", memo);
			// 源值
			log.put("V_SOURCE_VALUE", sourceValue);
			// 目标值
			log.put("V_FINAL_VALUE", finalValue);
			// 操作时间
			log.put("N_OPERATION_TIME", System.currentTimeMillis());
			// 操作人
			log.put("V_OPERATION_USER", ApacheShiroUtils.getCurrentUserName());

			getActiveRecordDAO().auto().table(TableName.EKB_T_OPERATION_LOG)
					.save(log);
		}
	}
}
