/**
 * 包名：com.sozone.eokb.job
 * 文件名：GetGCJLCreditScoreJob.java<br/>
 * 创建时间：2018-5-15 上午9:26:12<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.job;

import org.apache.commons.lang.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.timer.AeolusJobExecutionContext;
import com.sozone.aeolus.timer.job.AeolusBaseJob;
import com.sozone.eokb.utils.MsgUtils;
import com.ws.client.CreditScoreUtils;

/**
 * 工程监理信用分同步定时器<br/>
 * <p>
 * 工程监理信用分同步定时器<br/>
 * </p>
 * Time：2018-5-15 上午9:26:12<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
// 串行注解
@DisallowConcurrentExecution
public class GetGCJLCreditScoreJob extends AeolusBaseJob
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(GetGCJLCreditScoreJob.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sozone.aeolus.timer.job.AeolusBaseJob#run(com.sozone.aeolus.timer
	 * .AeolusJobExecutionContext)
	 */
	@Override
	public void run(AeolusJobExecutionContext context) throws ServiceException
	{
		logger.info(LogUtils.format("开始同步工程监理信用分!"));
		JobDataMap dataMap = context.getJobExecutionContext()
				.getMergedJobDataMap();
		ActiveRecordDAO dao = context.getActiveRecordDAO();
		String tps = dataMap.getString("TYPE");
		// 地区名称,发短信专用
		String areaName = dataMap.getString("AREA_NAME");
		String[] types = StringUtils.split(tps, ",");
		Record<String, Object> yq = CreditScoreUtils.getYearAndQuarter();
		try
		{
			GetGCJLCreditScoreUtils.getData(dao, types, yq.getString("YEAR"), yq.getString("QUARTER"));
		}
		catch (ServiceException e)
		{
			MsgUtils.send("开标工程监理信用分同步：失败，" + areaName + ", "
					+ yq.getString("YEAR") + "年,"
					+ yq.getString("QUARTER_NAME") + "，请处理!");
			logger.error(LogUtils.format("同步工程监理信用分发生异常!"), e);
			throw e;
		}
		catch (Exception e)
		{
			MsgUtils.send("开标工程监理信用分同步：失败，" + areaName + ", "
					+ yq.getString("YEAR") + "年,"
					+ yq.getString("QUARTER_NAME") + "，请处理!");
			logger.error(LogUtils.format("同步工程监理信用分发生异常!"), e);
			throw new ServiceException("", "同步工程监理信用分发生异常!", e);
		}
	}

}
