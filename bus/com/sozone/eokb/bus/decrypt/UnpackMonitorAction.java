/**
 * 包名：com.sozone.eokb.bus.decrypt
 * 文件名：UnpackMonitorAction.java<br/>
 * 创建时间：2017-12-20 下午4:38:29<br/>
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
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;

/**
 * 解压监控服务接口<br/>
 * <p>
 * 解压监控服务接口<br/>
 * </p>
 * Time：2017-12-20 下午4:38:29<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
@Path(value = "unpmonitor", desc = "解压监控服务")
public class UnpackMonitorAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(UnpackMonitorAction.class);

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
	 * 获取解压记录信息分页封装<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 解压记录信息分页封装
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "", desc = "获取解压记录信息分页封装")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getUnpackRecordPage(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取解压记录信息分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> condition = data.getRecord();
		// 这是一种多个排序字段 不同的排序命令
		return this.activeRecordDAO.auto().table(TableName.UNPACK_RECORD)
				.addSortOrder("N_CREATE_TIME", "ASC").page(pageable, condition);
	}

	/**
	 * 重新解压<br/>
	 * <p>
	 * </p>
	 * 
	 * @param rids
	 *            要解压的ID
	 * @param data
	 *            AeolusData
	 * @return 响应
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "unpack/{rids}", desc = "重新解压")
	// POST访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public ResultVO<String> doUnpack(@PathParam("rids") String rids,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("重新解压", rids, data));
		String[] ids = StringUtils.split(rids, ",");
		ResultVO<String> result = new ResultVO<String>(true);
		StringBuilder msg = new StringBuilder();
		Record<String, Object> record = null;
		File targetDir = null;
		File zipx = null;
		Record<String, Object> temp = new RecordImpl<String, Object>();
		long start = 0;
		long end = 0;
		// zip包存放路径
		File zipFolder = null;
		// zip包解压路径
		File unZipDir = null;
		// zip包路径
		File zip = null;
		for (String recordID : ids)
		{
			record = this.activeRecordDAO.auto().table(TableName.UNPACK_RECORD)
					.get(recordID);
			if (!CollectionUtils.isEmpty(record))
			{
				logger.info(LogUtils.format("解压文件!", record));
				targetDir = new File(record.getString("V_TARGET_DIR_PATH"));
				zipFolder = new File(targetDir, "ZipFolder");
				zipx = new File(record.getString("V_ZIPX_FILE_PATH"));
				unZipDir = new File(zipFolder, FilenameUtils.getBaseName(zipx
						.getName()));
				zip = new File(zipFolder, FilenameUtils.getBaseName(zipx
						.getName()) + ".zip");
				temp.setColumn("ID", record.getString("ID"));
				int status = 1;
				try
				{
					// 这边要先把原来的文件干掉
					//干掉解压目录
					FileUtils.deleteDirectory(unZipDir);
					//干掉zip文件
					FileUtils.deleteQuietly(zip);
					start = System.currentTimeMillis();
					DecryptBidder.unpackZipxFile(record.getString("V_FTID"),
							zipx, targetDir);
					end = System.currentTimeMillis();
					temp.setColumn("N_TIME_CONSUMING", end - start);
					temp.setColumn("N_FINSH_TIME", end);
				}
				catch (Exception e)
				{
					result.setSuccess(false);
					msg.append("投标人：[")
							.append(record.getString("V_BIDDER_NAME"))
							.append("]的[")
							.append(record.getString("V_ZIPX_FILE_PATH"))
							.append("]文件解压失败!详细异常如下:<br/>");
					msg.append(getExceptionStackTrace(e));
					result.setResult(msg.toString());
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
		return result;
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

}
