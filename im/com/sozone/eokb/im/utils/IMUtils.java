/**
 * 包名：com.sozone.eokb.im.utils
 * 文件名：IMDBUtils.java<br/>
 * 创建时间：2017-8-29 下午2:35:15<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.exception.AeolusException;
import com.sozone.aeolus.exception.DAOException;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 即时聊天数据库工具类<br/>
 * Time：2017-8-29 下午2:35:15<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
public class IMUtils
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(IMUtils.class);

	/**
	 * 
	 * 通过招标项目ID获取招标项目信息<br/>
	 * 
	 * @param dao
	 *            ActiveRecordDAO
	 * @param tpid
	 *            招标项目ID
	 * @return 招标项目信息
	 * @throws DAOException
	 *             DAO异常
	 */
	public static Record<String, Object> getTenderProjectInfo(
			ActiveRecordDAO dao, String tpid) throws DAOException
	{

		return dao.auto().table(TableName.EKB_T_TENDER_PROJECT_INFO).get(tpid);
	}

	/**
	 * 
	 * 是否是招标人或招标代理<br/>
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @return boolean 是/否
	 */
	public static boolean isTenderer(String tenderProjectID)
	{
		String roleCode = SessionUtils.getAttribute("roleCode");
		if ("0".equals(roleCode))
		{
			return true;
		}
		return false;
	}

	/**
	 * 
	 * 是否是招标人或招标代理<br/>
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @return boolean 是/否
	 */
	public static boolean isBidder(String tenderProjectID)
	{
		String roleCode = SessionUtils.getAttribute("roleCode");
		if ("1".equals(roleCode))
		{
			return true;
		}
		return false;
	}

	/**
	 * 
	 * 是否是招标人或招标代理<br/>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return boolean 是/否
	 */
	public static boolean isTendererOrBidder(String tpid)
	{
		String roleCode = SessionUtils.getAttribute("roleCode");
		if ("0".equals(roleCode) || "1".equals(roleCode))
		{
			return true;
		}
		return false;
	}

	/**
	 * 修改IM信息<br/>
	 * 
	 * @param tableName
	 *            表名
	 * @param data
	 *            数据
	 */
	public static void modifyIMInfo(String tableName,
			Record<String, Object> data)
	{
		logger.debug(LogUtils.format("修改IM信息!", tableName, data));

		StatefulDAO dao = null;
		try
		{
			dao = new StatefulDAOImpl();
			// 更新数据
			dao.auto().table(tableName).modify(data);
			// 提交事务
			dao.commit();
		}
		catch (AeolusException e)
		{
			logger.error(LogUtils.format("修改IM信息失败!", tableName, data), e);
			// 回滚事务
			if (null != dao)
			{
				dao.rollback();
			}
		}
		finally
		{
			// 关闭事务
			if (null != dao)
			{
				dao.close();
			}
		}

	}

	/**
	 * 保存IM信息<br/>
	 * 
	 * @param tableName
	 *            表名
	 * @param data
	 *            数据
	 */
	public static void saveIMInfo(String tableName, Record<String, Object> data)
	{
		logger.debug(LogUtils.format("保存IM信息!", tableName, data));

		StatefulDAO dao = null;
		try
		{
			dao = new StatefulDAOImpl();
			// 更新数据
			dao.auto().table(tableName).save(data);
			// 提交事务
			dao.commit();
		}
		catch (AeolusException e)
		{
			logger.error(LogUtils.format("保存IM信息失败!", tableName, data), e);
			// 回滚事务
			if (null != dao)
			{
				dao.rollback();
			}
		}
		finally
		{
			// 关闭事务
			if (null != dao)
			{
				dao.close();
			}
		}
	}
}
