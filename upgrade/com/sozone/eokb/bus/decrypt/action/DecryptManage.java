/**
 * 包名：com.sozone.eokb.bus.decrypt.action
 * 文件名：DecryptManage.java<br/>
 * 创建时间：2018-5-16 下午4:50:52<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt.action;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.decrypt.daemon.DaemonHandler;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;

/**
 * 解密管理服务接口<br/>
 * <p>
 * 解密管理服务接口<br/>
 * </p>
 * Time：2018-5-16 下午4:50:52<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "dmgr", desc = "解密管理服务接口")
public class DecryptManage
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(DecryptManage.class);

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
	 * 获取解密解压信息分页封装<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 解密解压信息分页封装
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "", desc = "获取解密解压信息分页封装")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getDecryptStatusPage(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取解密解压信息分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> condition = data.getRecord();
		return this.activeRecordDAO.auto()
				.table(TableName.DECRYPT_UNPACK_STATUS_VIEW)
				.addSortOrder("N_CREATE_TIME", "ASC")
				.addSortOrder("V_DELIVER_TIME", "ASC")
				.page(pageable, condition);
	}

	/**
	 * 解析文件 <br/>
	 * <p>
	 * </p>
	 * 
	 * @param ids
	 *            要解析的ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/parse/{ids}", desc = "解析文件")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void doParse(@PathParam("ids") String ids, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("解析文件", ids, data));
		String[] recordIDs = StringUtils.split(ids, ",");
		Record<String, Object> temp = null;
		List<Record<String,Object>> tfs = new LinkedList<Record<String,Object>>();
		for (String id : recordIDs)
		{
			temp = this.activeRecordDAO.auto()
					.table(TableName.DECRYPT_TEMP_DATA).get(id);
			if (!CollectionUtils.isEmpty(temp))
			{
				tfs.add(temp);
			}
		}
		DaemonHandler.parseTempFiles(tfs);
	}

	/**
	 * 解密文件 <br/>
	 * <p>
	 * </p>
	 * 
	 * @param ids
	 *            要解密的ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/defbx/{ids}", desc = "解密文件")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void doDecrypt(@PathParam("ids") String ids, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("解密文件", ids, data));
		String[] recordIDs = StringUtils.split(ids, ",");
		Record<String, Object> temp = null;
		for (String id : recordIDs)
		{
			temp = this.activeRecordDAO.auto()
					.table(TableName.DECRYPT_TEMP_DATA).get(id);
			if (!CollectionUtils.isEmpty(temp))
			{
				DaemonHandler.decryptEfbx(temp);
			}
		}
	}

	/**
	 * 解压文件 <br/>
	 * <p>
	 * </p>
	 * 
	 * @param ids
	 *            要解密的ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/unzip/{ids}", desc = "解压文件")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void doUnzip(@PathParam("ids") String ids, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("解压文件", ids, data));
		String[] recordIDs = StringUtils.split(ids, ",");
		Record<String, Object> temp = null;
		for (String id : recordIDs)
		{
			temp = this.activeRecordDAO.auto()
					.table(TableName.DECRYPT_TEMP_DATA).get(id);
			if (!CollectionUtils.isEmpty(temp))
			{
				DaemonHandler.unzip(temp);
			}
		}
	}

}
