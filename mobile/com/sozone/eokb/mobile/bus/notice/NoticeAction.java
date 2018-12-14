/**
 * 包名：com.sozone.eokb.mobile.bus.notice
 * 文件名：NoticeAction.java<br/>
 * 创建时间：2018-11-27 下午5:10:33<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.bus.notice;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.eokb.common.ConstantEOKB.TableName;

/**
 * 系统公告接口<br/>
 * <p>
 * 系统公告接口<br/>
 * </p>
 * Time：2018-11-27 下午5:10:33<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/mobile/notice", desc = "系统公告接口")
@Permission(Level.Authenticated)
public class NoticeAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(NoticeAction.class);

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
	 * 获取当前系统的所有公告列表<br/>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @return 获取当前系统的所有公告列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "{tpid}", desc = "获取当前系统的所有公告列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getTPAllNotices(
			@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取当前系统的所有公告列表", data));
		return this.activeRecordDAO.pandora()
				.SELECT_ALL_FROM(TableName.EKB_T_NOTICE).EQUAL("V_TPID", tpid)
				.ORDER_BY("N_CREATE_TIME").list();
	}

	/**
	 * 获取指定时间戳之后的招标项目公告数量<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param lastTime
	 *            最后时间戳
	 * @param data
	 *            AeolusData
	 * @return 获取指定时间戳之后的招标项目公告数量
	 * @throws FacadeException
	 *             FacadeException
	 */
	public ResultVO<Long> getTPNoticeCount(@PathParam("tpid") String tpid,
			@PathParam("lt") String lastTime, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取指定时间戳之后的招标项目公告数量", data));
		long count = 0;
		if (StringUtils.equals(lastTime, "0"))
		{
			count = this.activeRecordDAO.pandora()
					.SELECT_COUNT_FROM(TableName.EKB_T_NOTICE)
					.EQUAL("V_TPID", tpid).ORDER_BY("N_CREATE_TIME").count();
		}
		else
		{
			count = this.activeRecordDAO.pandora()
					.SELECT_COUNT_FROM(TableName.EKB_T_NOTICE)
					.EQUAL("V_TPID", tpid)
					.GREATER_THAN_OR_EQUAL("N_CREATE_TIME", lastTime)
					.ORDER_BY("N_CREATE_TIME").count();
		}
		ResultVO<Long> result = new ResultVO<Long>(true);
		result.setResult(count);
		return result;
	}

	/**
	 * 获取指定时间戳之后的招标项目公告列表<br/>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param lastTime
	 *            最后时间戳
	 * @param data
	 *            AeolusData
	 * @return 获取指定时间戳之后的招标项目公告列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "{tpid}/{lt}", desc = "获取指定时间戳之后的招标项目公告列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getTPNotices(
			@PathParam("tpid") String tpid, @PathParam("lt") String lastTime,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取指定时间戳之后的招标项目公告列表", data));
		if (StringUtils.equals(lastTime, "0"))
		{
			return this.activeRecordDAO.pandora()
					.SELECT_ALL_FROM(TableName.EKB_T_NOTICE)
					.EQUAL("V_TPID", tpid).ORDER_BY("N_CREATE_TIME").list();
		}
		return this.activeRecordDAO.pandora()
				.SELECT_ALL_FROM(TableName.EKB_T_NOTICE).EQUAL("V_TPID", tpid)
				.GREATER_THAN_OR_EQUAL("N_CREATE_TIME", lastTime)
				.ORDER_BY("N_CREATE_TIME").list();
	}

}
