/**
 * 包名：com.sozone.eokb.bus.decrypt
 * 文件名：UnpackServiceUtils.java<br/>
 * 创建时间：2017-12-19 下午4:22:54<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.MsgUtils;

/**
 * 解压服务工具类<br/>
 * <p>
 * 解压服务工具类<br/>
 * </p>
 * Time：2017-12-19 下午4:22:54<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
public final class UnpackServiceUtils
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(UnpackServiceUtils.class);

	/**
	 * 招标项目ID与调度对应关系图
	 */
	private static final ConcurrentHashMap<String, Timer> TIMERS = new ConcurrentHashMap<String, Timer>();

	/**
	 * 私有构造函数
	 */
	private UnpackServiceUtils()
	{
	}

	/**
	 * 启动一个后台解压任务br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param tpName
	 *            招标项目名称
	 */
	public static void startUnpackTask(String tpid, String tpName)
	{
		// 先判断是否存在招标项目对应的定时器
		Timer timer = TIMERS.get(tpid);
		// 如果定时任务存在,直接返回
		if (null != timer)
		{
			return;
		}
		// 创建一个定时器
		timer = new Timer();
		TIMERS.put(tpid, timer);
		logger.info(LogUtils.format("解压任务已创建!", tpid));
		// 创建定时任务
		UnpackTask task = new UnpackTask(timer, tpid, tpName);
		// 5000毫秒后开始执行定时任务
		timer.schedule(task, 5000);
	}

	/**
	 * 获取未处理的压缩文件数量<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 未处理数量
	 * @throws ServiceException
	 *             ServiceException
	 */
	private static int getUntreatedCount(String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取未处理的压缩文件数量!", tpid));
		StatefulDAO dao = null;
		try
		{
			dao = new StatefulDAOImpl();
			int count = dao
					.auto()
					.table(TableName.UNPACK_RECORD)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "N_STATUS = 0")
					.count(new RecordImpl<String, Object>().setColumn("tpid",
							tpid));
			// 提交事务
			dao.commit();
			return count;
		}
		catch (ServiceException e)
		{
			// 回滚事务
			dao.rollback();
			logger.error(LogUtils.format("获取未处理的压缩文件数量失败!", tpid), e);
			throw e;
		}
		finally
		{
			// 关闭事务
			dao.close();
		}
	}

	/**
	 * 获取未解压的列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 * @return
	 * @throws ServiceException
	 */
	private static List<Record<String, Object>> getUnpackList(String tpid)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取未处理的压缩文件数量!", tpid));
		StatefulDAO dao = null;
		try
		{
			dao = new StatefulDAOImpl();
			List<Record<String, Object>> records = dao
					.auto()
					.table(TableName.UNPACK_RECORD)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "N_STATUS = 0")
					.list(new RecordImpl<String, Object>().setColumn("tpid",
							tpid));
			// 提交事务
			dao.commit();
			return records;
		}
		catch (ServiceException e)
		{
			// 回滚事务
			dao.rollback();
			logger.error(LogUtils.format("获取未处理的压缩文件数量失败!", tpid), e);
			throw e;
		}
		finally
		{
			// 关闭事务
			dao.close();
		}
	}

	/**
	 * 修改数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param record
	 * @param table
	 * @throws ServiceException
	 */
	private static void modify(Record<String, Object> record, String table)
			throws ServiceException
	{
		StatefulDAO dao = null;
		try
		{
			dao = new StatefulDAOImpl();
			dao.auto().table(table).modify(record);
			// 提交事务
			dao.commit();
		}
		catch (ServiceException e)
		{
			// 回滚事务
			dao.rollback();
			logger.error(LogUtils.format("修改数据失败!"), e);
			throw e;
		}
		finally
		{
			// 关闭事务
			dao.close();
		}
	}

	/**
	 * 解压Zipx文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * 
	 * @throws ServiceException
	 *             ServiceException
	 */
	private static void doUnpack(String tpid) throws ServiceException
	{
		List<Record<String, Object>> records = getUnpackList(tpid);
		// 目标目录
		File targetDir = null;
		File zipx = null;
		Record<String, Object> temp = new RecordImpl<String, Object>();
		long start = 0;
		long end = 0;
		for (Record<String, Object> record : records)
		{
			logger.info(LogUtils.format("解压文件!", record));
			targetDir = new File(record.getString("V_TARGET_DIR_PATH"));
			zipx = new File(record.getString("V_ZIPX_FILE_PATH"));
			temp.setColumn("ID", record.getString("ID"));
			int status = 1;
			try
			{
				start = System.currentTimeMillis();
				DecryptBidder.unpackZipxFile(record.getString("V_FTID"), zipx,
						targetDir);
				end = System.currentTimeMillis();
				temp.setColumn("N_TIME_CONSUMING", end - start);
				temp.setColumn("N_FINSH_TIME", end);
			}
			catch (Exception e)
			{
				logger.error(LogUtils.format("解压投标文件发生异常!", record), e);
				status = -1;
			}
			finally
			{
				temp.setColumn("N_STATUS", status);
				modify(temp, TableName.UNPACK_RECORD);
			}
		}
	}

	/**
	 * 发生错误短信到运维人员<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 * @param tpName
	 *            招标项目名称
	 */
	private static void sendErrorMsg(String tpid, String tpName)
	{
		StatefulDAO dao = null;
		try
		{
			dao = new StatefulDAOImpl();
			int count = dao
					.auto()
					.table(TableName.UNPACK_RECORD)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "N_STATUS = -1")
					.count(new RecordImpl<String, Object>().setColumn("tpid",
							tpid));
			StringBuilder sb = new StringBuilder();
			if (count > 0)
			{
				sb.append("开标投标解压：失败").append(count).append("，").append(tpName)
						.append("，请处理!");
				JSONObject jobj = MsgUtils.send(sb.toString());
				if (!jobj.getBoolean("success"))
				{
					logger.error(LogUtils.format("发送错误短信失败!", jobj));
				}
			}
			else
			{
				sb.append("开标投标解压：成功，").append(tpName).append("。");
				MsgUtils.send(sb.toString());
			}
			// 提交事务
			dao.commit();
		}
		catch (ServiceException e)
		{
			// 回滚事务
			dao.rollback();
			logger.error(LogUtils.format("发送错误短信失败!"), e);
		}
		finally
		{
			// 关闭事务
			dao.close();
		}
	}

	/**
	 * 解压任务<br/>
	 * <p>
	 * 解压时间<br/>
	 * </p>
	 * Time：2017-12-19 下午4:43:55<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private static class UnpackTask extends TimerTask
	{

		/**
		 * 定时器
		 */
		private Timer timer = null;

		/**
		 * 招标项目ID
		 */
		private String tpid = null;

		/**
		 * 招标项目名称
		 */
		private String tpName = null;

		/**
		 * 构造函数
		 * 
		 * @param timer
		 *            定时器
		 * @param tpid
		 *            招标项目ID
		 * @param tpName
		 *            招标项目名称
		 */
		private UnpackTask(Timer timer, String tpid, String tpName)
		{
			super();
			this.timer = timer;
			this.tpid = tpid;
			this.tpName = tpName;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.TimerTask#run()
		 */
		@Override
		public void run()
		{
			logger.info(LogUtils.format("解压任务开始处理!", tpid, tpName));
			try
			{
				while (true)
				{
					int count = getUntreatedCount(this.tpid);
					// 如果没有未处理的
					if (count <= 0)
					{
						break;
					}
					// 解压
					doUnpack(this.tpid);
				}
			}
			catch (Exception e)
			{
				logger.error(LogUtils.format("执行解压任务发生异常!"), e);
			}
			finally
			{
				// 发送短信
				sendErrorMsg(this.tpid, this.tpName);
				TIMERS.remove(tpid);
				// 关闭定时器
				this.timer.cancel();
				logger.info(LogUtils.format("解压任务关闭!", tpid, tpName));
			}
		}
	}

}
