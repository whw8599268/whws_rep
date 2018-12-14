/**
 * 包名：com.sozone.eokb.im.flash
 * 文件名：FlashSecurityXMLServerStartListener.java<br/>
 * 创建时间：2017-8-18 下午2:25:47<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.flash;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.LogUtils;

/**
 * Flash Socket通信的安全策略服务启动监听器<br/>
 * <p>
 * </p>
 * Time：2017-8-18 下午2:25:47<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class FlashSecurityXMLServerStartListener implements
		ServletContextListener
{

	/**
	 * 安全策略服务监听端口
	 */
	private static final String FLASH_SECURITY_SERVER_PORT = "FLASH_SECURITY_SERVER_PORT";

	/**
	 * 安全策略服务缺省监听端口
	 */
	private static final int DEFAULT_FLASH_SECURITY_SERVER_PORT = 843;

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(FlashSecurityXMLServerStartListener.class);

	/**
	 * 安全策略服务
	 */
	private static FlashSecurityXMLServer flashSecurityXMLServer = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.
	 * ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event)
	{
		logger.debug(LogUtils.format("关闭Flash Socket通信的安全策略服务!"));
		flashSecurityXMLServer.shutDownServer();
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
		ServletContext servletContext = event.getServletContext();
		// 服务监听端口
		String portParam = servletContext
				.getInitParameter(FLASH_SECURITY_SERVER_PORT);
		int port = DEFAULT_FLASH_SECURITY_SERVER_PORT;
		if (StringUtils.isNotEmpty(portParam))
		{
			port = Integer.valueOf(portParam);
		}
		logger.debug(LogUtils.format("启动Flash Socket通信的安全策略服务!", port));
		// 启动服务
		flashSecurityXMLServer = new FlashSecurityXMLServer();
		flashSecurityXMLServer.startServer(port);
	}
}
