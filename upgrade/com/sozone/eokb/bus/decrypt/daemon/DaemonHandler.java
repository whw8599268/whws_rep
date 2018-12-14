/**
 * 包名：com.sozone.eokb.bus.decrypt.daemon
 * 文件名：DaemonHandler.java<br/>
 * 创建时间：2018-5-16 下午2:01:47<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt.daemon;

import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.Auto.Table;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.eokb.bus.decrypt.BidderDocumentDecryptor;
import com.sozone.eokb.bus.decrypt.DecryptStatus;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.MsgUtils;

/**
 * 投标文件解密解压工具类<br/>
 * <p>
 * 投标文件解密解压工具类<br/>
 * </p>
 * Time：2018-5-16 下午2:01:47<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class DaemonHandler
{

	/**
	 * 线程池大小
	 */
	private static final int THREAD_POOL_SIZE = 1;

	/**
	 * 队列长度
	 */
	private static final int QUEUE_SIZE = 30;

	/**
	 * 等待解析的队列,这个队列要大一些
	 */
	private static final BlockingQueue<Record<String, Object>> WAIT_FOR_PARSE_QUEUE = new LinkedBlockingDeque<Record<String, Object>>(
			1000);

	/**
	 * 解析线程池
	 */
	private static final ExecutorService PARSE_POOL = Executors
			.newFixedThreadPool(THREAD_POOL_SIZE);

	/**
	 * 等待解密的队列,队列中放的是等待解密的招标项目ID,投标人统一社会信用代码,投标人组织机构代码.
	 */
	private static final BlockingQueue<Record<String, Object>> WAIT_FOR_DECRYPT_QUEUE = new LinkedBlockingDeque<Record<String, Object>>(
			QUEUE_SIZE);

	/**
	 * 解密线程池
	 */
	private static final ExecutorService DECRYPT_POOL = Executors
			.newFixedThreadPool(THREAD_POOL_SIZE);

	/**
	 * 等待解压的队列
	 */
	private static final BlockingQueue<Record<String, Object>> WAIT_FOR_UNPACK_QUEUE = new LinkedBlockingDeque<Record<String, Object>>(
			QUEUE_SIZE);

	/**
	 * 解密线程池
	 */
	private static final ExecutorService UNPACK_POOL = Executors
			.newFixedThreadPool(THREAD_POOL_SIZE);

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(DaemonHandler.class);

	/**
	 * 获取待解析数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param record
	 * @return
	 */
	private static List<Record<String, Object>> getWaitForParseDatas(
			Record<String, Object> record)
	{
		StatefulDAO statefulDAO = null;
		try
		{
			// 获取状态为密码解密成功,即待解析状态
			record.setColumn("status",
					DecryptStatus.DecryptSummarySuccess.getStatus());
			statefulDAO = new StatefulDAOImpl();
			Table table = statefulDAO.auto().table(TableName.DECRYPT_TEMP_DATA)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "N_STATUS = #{status}");
			// 如果同时存在
			if (StringUtils.isNotEmpty(record.getString("unify_code"))
					&& StringUtils.isNotEmpty(record.getString("org_code")))
			{
				table.setCondition("AND",
						"(V_ORG_CODE = #{org_code} OR V_UNIFY_CODE = #{unify_code})");
			}
			else if (StringUtils.isEmpty(record.getString("unify_code"))
					&& StringUtils.isNotEmpty(record.getString("org_code")))
			{
				table.setCondition("AND", "V_ORG_CODE = #{org_code}");
			}
			else if (StringUtils.isNotEmpty(record.getString("unify_code"))
					&& StringUtils.isEmpty(record.getString("org_code")))
			{
				table.setCondition("AND", "V_UNIFY_CODE = #{unify_code}");
			}
			// 获取需要解密的数据
			List<Record<String, Object>> datas = table.list(record);
			// 提交事务
			statefulDAO.commit();
			return datas;
		}
		catch (Exception e)
		{
			// 回滚事务
			if (null != statefulDAO)
			{
				statefulDAO.rollback();
			}
			logger.error(LogUtils.format("获取待解析数据发生异常!", record), e);
			// 招标项目名称
			StringBuilder sb = new StringBuilder();
			sb.append("开标获取待解析数据：失败，投标人组织机构代码:[")
					.append(record.getString("org_code")).append("]统一社会信用代码[")
					.append(record.getString("unify_code")).append("]，请处理!");
			MsgUtils.send(sb.toString());
		}
		finally
		{
			// 关闭事务
			if (null != statefulDAO)
			{
				statefulDAO.close();
			}
		}
		return null;
	}

	/**
	 * 解析临时文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param record
	 *            信息
	 * @throws ServiceException
	 *             ServiceException
	 */
	private static void parseTemps(Record<String, Object> record)
			throws ServiceException
	{
		logger.info(LogUtils.format("解析临时文件!", record));
		List<Record<String, Object>> datas = getWaitForParseDatas(record);
		if (null == datas || datas.isEmpty())
		{
			return;
		}
		parseTempFiles(datas);
	}

	/**
	 * 解析临时文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param datas
	 *            投标文件信息列表
	 * @throws ServiceException
	 *             服务异常
	 */
	public static void parseTempFiles(List<Record<String, Object>> datas)
			throws ServiceException
	{
		// 投标文件目录
		String stbxPath = null;
		String tempPath = null;
		// 临时文件
		File efbx = null;
		long start = 0;
		long end = 0;
		// 迭代开始解密
		for (Record<String, Object> data : datas)
		{
			stbxPath = data.getString("V_STBX_FILE_PATH");
			tempPath = data.getString("V_TEMP_DIR_PATH");
			try
			{
				start = System.currentTimeMillis();
				// 解析临时文件
				efbx = BidderDocumentDecryptor.getEfbxTempFile(new File(
						stbxPath), new File(tempPath));
				end = System.currentTimeMillis();

				// 设置临时文件
				data.setColumn("V_EFBX_FILE_PATH",
						FilenameUtils.separatorsToUnix(efbx.getAbsolutePath()));
				// 解析耗时
				data.setColumn("N_PARSE_CONSUMING", end - start);
				// 设置状态
				data.setColumn("N_STATUS",
						DecryptStatus.ParseTempFileSuccess.getStatus());
				logger.info(LogUtils.format("临时文件解析成功!", data));
				// 只有成功才发起解密请求
				new PutRecordToQueue(WAIT_FOR_DECRYPT_QUEUE, data).start();
			}
			catch (ServiceException e)
			{
				// 设置状态
				data.setColumn("N_STATUS",
						DecryptStatus.ParseTempFileFail.getStatus());
				logger.error(LogUtils.format("临时文件解析发生异常!", data), e);
				// 招标项目名称
				StringBuilder sb = new StringBuilder();
				sb.append("开标解析临时文件：失败，投标人组织机构代码:[")
						.append(data.getString("V_ORG_CODE")).append("]投标人名称[")
						.append(data.getString("V_BIDDER_NAME"))
						.append("]，请处理!");
				MsgUtils.send(sb.toString());
				throw e;
			}
			finally
			{
				// 修改数据状态
				modifyData(TableName.DECRYPT_TEMP_DATA, data);
			}
		}
	}

	/**
	 * 解析线程<br/>
	 * <p>
	 * </p>
	 * Time：2018-5-29 下午4:23:03<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private static class ParseThread extends Thread
	{
		/**
		 * 队列
		 */
		private final BlockingQueue<Record<String, Object>> queue;

		/**
		 * 构造函数
		 * 
		 * @param queue
		 *            队列
		 */
		private ParseThread(BlockingQueue<Record<String, Object>> queue)
		{
			this.queue = queue;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run()
		{
			try
			{
				// 一直跑
				while (true)
				{
					try
					{
						logger.info(LogUtils.format("后台Daemon服务--开始解析投标文件!"));
						parseTemps(queue.take());
						logger.info(LogUtils.format("后台Daemon服务--投标文件解析完成!"));
					}
					catch (ServiceException e)
					{
						logger.error(
								LogUtils.format("后台Daemon服务--投标文件解析发生异常!"), e);
					}
				}
			}
			catch (InterruptedException e)
			{
				logger.error(LogUtils.format("后台Daemon服务--从待解析队列中获取解析数据发生异常!"),
						e);
			}
		}
	}

	/**
	 * 解密线程<br/>
	 * <p>
	 * </p>
	 * Time：2018-5-16 上午11:26:07<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private static class DecryptThread extends Thread
	{

		/**
		 * 队列
		 */
		private final BlockingQueue<Record<String, Object>> queue;

		/**
		 * 构造函数
		 * 
		 * @param queue
		 *            队列
		 */
		private DecryptThread(BlockingQueue<Record<String, Object>> queue)
		{
			this.queue = queue;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run()
		{
			try
			{
				// 一直跑
				while (true)
				{
					try
					{
						logger.info(LogUtils
								.format("后台Daemon服务--开始解密efbx临时文件!"));
						decryptEfbx(queue.take());
						logger.info(LogUtils
								.format("后台Daemon服务--临时文件efbx文件解密完成!"));
					}
					catch (ServiceException e)
					{
						logger.error(
								LogUtils.format("后台Daemon服务--解密efbx文件发生异常!"), e);
					}
				}
			}
			catch (InterruptedException e)
			{
				logger.error(LogUtils.format("后台Daemon服务--从待解密队列中获取解密数据发生异常!"),
						e);
			}
		}

	}

	/**
	 * 解密Efbx文件列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param record
	 *            临时信息
	 * @throws ServiceException
	 *             服务异常
	 */
	public static void decryptEfbx(Record<String, Object> record)
			throws ServiceException
	{
		// 加密块数据文件完整路径
		String efbxPath = null;
		// 解密后的存放路径
		String targetDir = null;
		// 密码
		String pwd = null;
		efbxPath = record.getString("V_EFBX_FILE_PATH");
		targetDir = record.getString("V_TARGET_DIR_PATH");
		pwd = record.getString("V_PWD");
		try
		{
			// 解密临时文件
			long consuming = BidderDocumentDecryptor.decryptEfbxFile(new File(
					efbxPath), record.getInteger("N_ALGORITHM_TYPE"), pwd,
					new File(targetDir));
			// 解密耗时
			record.setColumn("N_DECRYPT_FILE_CONSUMING", consuming);
			// 设置状态
			record.setColumn("N_STATUS",
					DecryptStatus.DecryptTempFileSuccess.getStatus());
			logger.info(LogUtils.format("临时文件解密成功!", record));
			// 只有成功才发起解压请求
			new PutRecordToQueue(WAIT_FOR_UNPACK_QUEUE, record).start();
		}
		catch (ServiceException e)
		{
			// 设置状态为
			record.setColumn("N_STATUS",
					DecryptStatus.DecryptTempFileFail.getStatus());
			logger.error(LogUtils.format("解密临时文件发生异常!", record), e);
			// 招标项目名称
			StringBuilder sb = new StringBuilder();
			sb.append("开标解密临时文件：失败，投标人组织机构代码:[")
					.append(record.getString("V_ORG_CODE")).append("]投标人名称[")
					.append(record.getString("V_BIDDER_NAME")).append("]，请处理!");
			MsgUtils.send(sb.toString());
			throw e;
		}
		finally
		{
			// 修改数据状态
			modifyData(TableName.DECRYPT_TEMP_DATA, record);
		}
	}

	/**
	 * 修改信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tableName
	 *            表名
	 * @param data
	 *            数据
	 */
	private static void modifyData(String tableName, Record<String, Object> data)
	{
		StatefulDAO statefulDAO = null;
		try
		{
			statefulDAO = new StatefulDAOImpl();
			statefulDAO.auto().table(tableName).modify(data);
			// 提交事务
			statefulDAO.commit();
		}
		catch (Exception e)
		{
			// 回滚事务
			if (null != statefulDAO)
			{
				statefulDAO.rollback();
			}
		}
		finally
		{
			// 关闭事务
			if (null != statefulDAO)
			{
				statefulDAO.close();
			}
		}
	}

	/**
	 * 解压线程<br/>
	 * <p>
	 * </p>
	 * Time：2018-5-16 上午11:52:54<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private static class UnpackThread extends Thread
	{

		/**
		 * 队列
		 */
		private final BlockingQueue<Record<String, Object>> queue;

		/**
		 * 构造函数
		 * 
		 * @param queue
		 *            队列
		 */
		private UnpackThread(BlockingQueue<Record<String, Object>> queue)
		{
			this.queue = queue;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run()
		{
			try
			{
				// 一直跑
				while (true)
				{
					try
					{
						logger.info(LogUtils.format("后台Daemon服务--开始解压ZIP文件!"));
						unzip(queue.take());
						logger.info(LogUtils.format("后台Daemon服务--完成ZIP文件解压!"));
					}
					catch (ServiceException e)
					{
						logger.error(LogUtils.format("后台Daemon服务--解压文件发生异常!"),
								e);
					}
				}
			}
			catch (InterruptedException e)
			{
				logger.error(LogUtils.format("后台Daemon服务--从待解压队列中获取解压数据发生异常!"),
						e);
			}
		}

	}

	/**
	 * 解压<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            信息
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static void unzip(Record<String, Object> data)
			throws ServiceException
	{
		logger.info(LogUtils.format("解压ZIP文件!", data));
		String efbx = data.getString("V_EFBX_FILE_PATH");
		// 目标路径
		String targetDir = data.getString("V_TARGET_DIR_PATH");
		// zip包存放路径
		File zipFolder = new File(targetDir, "ZipFolder");
		// zip zip文件路径
		File zip = new File(zipFolder, FilenameUtils.getBaseName(efbx) + ".zip");
		try
		{
			// 解压文件
			long consuming = BidderDocumentDecryptor.unpackBidderDocumentFile(
					zip, new File(targetDir), data.getInteger("N_USE_CASE"));
			// 解压耗时
			data.setColumn("N_UNPACK_CONSUMING", consuming);
			// 设置状态为
			data.setColumn("N_STATUS",
					DecryptStatus.UnpackFileSuccess.getStatus());
			logger.info(LogUtils.format("ZIP文件解压成功!", data));
		}
		catch (ServiceException e)
		{
			// 设置状态为
			data.setColumn("N_STATUS", DecryptStatus.UnpackFileFail.getStatus());
			logger.error(LogUtils.format("解压文件发生异常!", data), e);
			// 招标项目名称
			StringBuilder sb = new StringBuilder();
			sb.append("开标文件解压：失败，投标人组织机构代码:[")
					.append(data.getString("V_ORG_CODE")).append("]投标人名称[")
					.append(data.getString("V_BIDDER_NAME")).append("]，请处理!");
			MsgUtils.send(sb.toString());
			throw e;
		}
		finally
		{
			// 修改数据状态
			modifyData(TableName.DECRYPT_TEMP_DATA, data);
		}
	}

	/**
	 * 增加队列内容<br/>
	 * <p>
	 * 增加队列内容<br/>
	 * </p>
	 * Time：2018-5-16 下午1:56:39<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private static class PutRecordToQueue extends Thread
	{
		/**
		 * Record
		 */
		private Record<String, Object> record = null;

		/**
		 * 队列
		 */
		private BlockingQueue<Record<String, Object>> queue = null;

		/**
		 * 构造函数
		 * 
		 * @param queue
		 *            队列
		 * @param record
		 *            Record
		 */
		private PutRecordToQueue(BlockingQueue<Record<String, Object>> queue,
				Record<String, Object> record)
		{
			this.queue = queue;
			this.record = record;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run()
		{
			try
			{
				this.queue.put(this.record);
			}
			catch (InterruptedException e)
			{
				logger.error(LogUtils.format("将解密临时数据放入等待队列发生异常!"), e);
			}
		}
	}

	/**
	 * 增加数据到解析队列<br/>
	 * <p>
	 * </p>
	 * 
	 * @param record
	 *            数据
	 */
	public static void addDataToParseTempQueue(Record<String, Object> record)
	{
		new PutRecordToQueue(WAIT_FOR_PARSE_QUEUE, record).start();
	}

	/**
	 * 启动Daemon服务<br/>
	 * <p>
	 * </p>
	 */
	public static void start()
	{
		logger.debug(LogUtils.format("启动Daemon服务!"));
		for (int i = 0; i < THREAD_POOL_SIZE; i++)
		{
			PARSE_POOL.submit(new ParseThread(WAIT_FOR_PARSE_QUEUE));
			DECRYPT_POOL.submit(new DecryptThread(WAIT_FOR_DECRYPT_QUEUE));
			UNPACK_POOL.submit(new UnpackThread(WAIT_FOR_UNPACK_QUEUE));
		}
	}

	/**
	 * 关闭Daemon服务<br/>
	 * <p>
	 * </p>
	 */
	public static void shutdown()
	{
		logger.debug(LogUtils.format("关闭Daemon服务!"));
		PARSE_POOL.shutdown();
		DECRYPT_POOL.shutdown();
		UNPACK_POOL.shutdown();
	}

}
