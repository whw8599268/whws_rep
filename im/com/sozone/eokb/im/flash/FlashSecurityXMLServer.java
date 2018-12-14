/**
 * 包名：com.sozone.eokb.im.flash
 * 文件名：FlashSecurityXMLServer.java<br/>
 * 创建时间：2017-8-18 下午1:50:18<br/>
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.util.ClassLoaderUtils;
import com.sozone.eokb.common.ConstantEOKB;

/**
 * 纯粹为了解决Flash Socket通信的安全策略问题<br/>
 * <p>
 * </p>
 * Time：2017-8-18 下午1:50:18<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public final class FlashSecurityXMLServer
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(FlashSecurityXMLServer.class);
	/**
	 * Socket处理线程池
	 */
	private static ExecutorService executorService = null;// 线程池

	/**
	 * Flash Socket通信的安全策略 XML 内容
	 */
	private static String crossDomainPolicyXml = null;

	/**
	 * 服务器端Socket
	 */
	private ServerSocket serverSocket = null;

	/**
	 * 静态初始化器
	 */
	static
	{
		InputStream in = null;
		try
		{

			// 获取安全策略配置文件流
			in = ClassLoaderUtils.getResourceAsStream(
					"com/sozone/eokb/im/flash/cross-domain-policy.xml",
					FlashSecurityXMLServer.class);
			List<String> lines = IOUtils.readLines(in,
					ConstantEOKB.DEFAULT_CHARSET);
			StringBuilder sb = new StringBuilder();
			for (String line : lines)
			{
				sb.append(line);
			}
			// 末尾一定要有这个结束符,必须有
			sb.append(" \0");
			crossDomainPolicyXml = sb.toString();
		}
		catch (IOException e)
		{
			logger.error(LogUtils.format("读取Flash Socket通信的安全策略配置文件失败!"), e);
		}
		finally
		{
			IOUtils.closeQuietly(in);
		}

	}

	/**
	 * 启动服务<br/>
	 * <p>
	 * </p>
	 * 
	 * @param port
	 *            服务端口,默认843
	 */
	public synchronized void startServer(int port)
	{
		logger.info(LogUtils.format("启动Flash Socket通信的安全策略服务!"));
		try
		{
			if (executorService == null)
			{
				// 创建线程池
				executorService = Executors.newFixedThreadPool(Runtime
						.getRuntime().availableProcessors() * 20);
				// 创建服务端 Socket
				serverSocket = new ServerSocket(port);
				// 启动服务端线程
				ServerThread serverThread = new ServerThread();
				new Thread(serverThread).start();
			}
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("启动Flash Socket通信的安全策略服务失败!"), e);
		}
	}

	/**
	 * 关闭服务<br/>
	 * <p>
	 * </p>
	 */
	public void shutDownServer()
	{
		if (serverSocket != null && !serverSocket.isClosed())
		{
			try
			{
				serverSocket.close();
			}
			catch (IOException e)
			{
				logger.error(LogUtils.format("关闭Flash Socket通信的安全策略服务失败!"), e);
			}
		}
	}

	/**
	 * 服务线程<br/>
	 * <p>
	 * </p>
	 * Time：2017-8-18 下午2:02:33<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private class ServerThread implements Runnable
	{

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			try
			{
				while (true)
				{
					if (serverSocket == null || serverSocket.isClosed())
					{
						break;
					}
					// 监听
					Socket scoket = serverSocket.accept();
					// 执行处理
					executorService.execute(new Handler(scoket));
				}
			}
			catch (IOException e)
			{
				logger.error(LogUtils.format("Flash Socket通信的安全策略服务发生异常!"), e);
			}
		}

	}

	/**
	 * 处理程序线程<br/>
	 * <p>
	 * </p>
	 * Time：2017-8-18 下午2:03:51<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private class Handler implements Runnable
	{
		private Socket socket;

		/**
		 * 构造函数
		 * 
		 * @param socket
		 */
		public Handler(Socket socket)
		{
			this.socket = socket;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			logger.info(LogUtils.format("向客户端输出Flash Socket通信的安全策略配置!"));
			try
			{
				OutputStream out = this.socket.getOutputStream();
				IOUtils.write(crossDomainPolicyXml, out,
						ConstantEOKB.DEFAULT_CHARSET);
				IOUtils.closeQuietly(out);
			}
			catch (IOException e)
			{
				logger.error(LogUtils.format("向客户端输出Flash Socket通信的安全策略配置失败!"),
						e);
			}
			finally
			{
				IOUtils.closeQuietly(this.socket);
			}
		}
	}
}
