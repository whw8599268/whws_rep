/**
 * 包名：com.sozone.eokb.job
 * 文件名：SyncObtaninTenderProjectDataJob.java<br/>
 * 创建时间：2018-2-27 上午10:51:45<br/>
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

import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.timer.AeolusJobExecutionContext;
import com.sozone.aeolus.timer.job.AeolusBaseJob;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TENDERPROJECT_APP_TYPE;

/**
 * 同步开标招标项目数据定时任务<br/>
 * <p>
 * 同步开标招标项目数据定时任务<br/>
 * </p>
 * Time：2018-2-27 上午10:51:45<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
// 串行注解
@DisallowConcurrentExecution
public class SyncObtaninTenderProjectDataJob extends AeolusBaseJob
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(SyncObtaninTenderProjectDataJob.class);

	/**
	 * 获取明天<br/>
	 * <p>
	 * </p>
	 * 
	 * @return
	 */
	private static String getTomorrowTime()
	{
		Date tomorrow = DateUtils.addDays(new Date(), 1);
		return DateFormatUtils.format(tomorrow, "yyyy-MM-dd");
	}

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
		logger.debug(LogUtils.format("开始获取开标项目信息!"));
		JobDataMap dataMap = context.getJobExecutionContext()
				.getMergedJobDataMap();
		// 获取招标项目的类型
		String tenderProType = dataMap.getString("TENDER_PROJECT_TYPE");
		// 类型中文名
		String tptName = dataMap.getString("TENDER_PROJECT_TYPE_NAME");
		// 开标地区代码
		String areaCode = SystemParamUtils.getString(SysParamKey.AREA_CODE_KEY);
		// 开标会议室NO
		String roomNO = SystemParamUtils.getString(SysParamKey.BID_ROOM_NO_KEY);
		// 如果是厦门市房建市政
		if (TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE.equals(tenderProType)
				|| TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE.equals(tenderProType))
		{
			XiaMenTPDataService.synchronizeData(getTomorrowTime(),
					tenderProType, tptName, areaCode, roomNO, null);
			return;
		}
		// 否则调用高速
		GsPtSyTPDataService.synchronizeData(getTomorrowTime(), tenderProType,
				tptName, areaCode, roomNO, null);
	}

}
