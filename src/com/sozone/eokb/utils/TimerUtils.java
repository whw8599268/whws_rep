/**
 * 包名：com.sozone.eokb.utils
 * 文件名：TimerUtils.java<br/>
 * 创建时间：2017年9月5日 下午1:53:39<br/>
 * 创建者：don<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.utils;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易调度工具类<br/>
 * <p>
 * 简易调度工具类<br/>
 * </p>
 * Time：2017年9月5日 下午1:53:39<br/>
 * 
 * @author don
 * @version 1.0.0
 * @since 1.0.0
 */
public final class TimerUtils
{
	/**
	 * 招标项目ID与调度对应关系图
	 */
	private static final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<String, Timer>();

	/**
	 * 
	 * 开启调度<br/>
	 * <p>
	 * 开启调度
	 * </p>
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @param task
	 *            调度任务
	 * @param time
	 *            触发时间
	 */
	public static void startTimer(String tenderProjectID, TimerTask task,
			Date time)
	{
		Timer t = new Timer();
		t.schedule(task, time);
		timers.put(task.getClass().getName() + "_" + tenderProjectID, t);
	}

	/**
	 * 
	 * 关闭调度<br/>
	 * <p>
	 * 关闭调度
	 * </p>
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @param clasz
	 *            调度任务类
	 */
	public static void closeTimer(String tenderProjectID, Class<?> clasz)
	{
		Timer t = timers.get(clasz.getName() + "_" + tenderProjectID);
		if (t != null)
		{
			t.cancel();
		}
	}

}
