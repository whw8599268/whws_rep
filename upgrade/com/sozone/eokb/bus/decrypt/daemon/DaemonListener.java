/**
 * 包名：com.sozone.eokb.bus.decrypt.daemon
 * 文件名：DaemonListener.java<br/>
 * 创建时间：2018-5-16 下午2:04:44<br/>
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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * daemon服务监听器<br/>
 * <p>
 * daemon服务监听器<br/>
 * </p>
 * Time：2018-5-16 下午2:04:44<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class DaemonListener implements ServletContextListener
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.
	 * ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event)
	{
		DaemonHandler.shutdown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.ServletContextListener#contextInitialized(javax.servlet
	 * .ServletContextEvent)
	 */
	@Override
	public void contextInitialized(ServletContextEvent event)
	{
		DaemonHandler.start();
	}
}
