/**
 * 包名：com.sozone.websocket.stress
 * 文件名：WebSocketTestServlet.java<br/>
 * 创建时间：2017-10-25 下午4:35:37<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.websocket.stress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.apache.commons.lang.StringUtils;

/**
 * WebSocket 压力测试Servlet<br/>
 * <p>
 * WebSocket 压力测试Servlet<br/>
 * </p>
 * Time：2017-10-25 下午4:35:37<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class WebSocketTestServlet extends WebSocketServlet
{

	/**
	 * 序列化版本号
	 */
	private static final long serialVersionUID = 4737608670982981286L;

	/**
	 * 
	 */
	private static final ConcurrentHashMap<String, TestMessageInbound> participants = new ConcurrentHashMap<String, TestMessageInbound>();

	private class TestMessageInbound extends MessageInbound
	{

		private String threadName;

		/**
		 * @param threadName
		 */
		private TestMessageInbound(String threadName)
		{
			this.threadName = threadName;
		}

		@Override
		protected void onBinaryMessage(ByteBuffer message) throws IOException
		{
		}

		@Override
		protected void onTextMessage(CharBuffer message) throws IOException
		{
			// System.out.println(this.threadName + " say: " +
			// message.toString());

			this.sendMessage(this.threadName + " say: " + message.toString());
			//
			// for (TestMessageInbound m : participants.values())
			// {
			// try
			// {
			// m.sendMessage(this.threadName + " say: "
			// + message.toString());
			// }
			// catch (IOException e)
			// {
			// e.printStackTrace();
			// }
			// }
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.catalina.websocket.StreamInbound#onClose(int)
		 */
		@Override
		protected void onClose(int status)
		{
			System.out.println(this.threadName + " out");
			participants.remove(this.threadName);
			super.onClose(status);
		}

		/**
		 * 发送JSON消息字符串<br/>
		 * <p>
		 * </p>
		 * 
		 * @param message
		 *            消息内容
		 * @throws IOException
		 *             IO异常
		 */
		public void sendMessage(String message) throws IOException
		{
			if (StringUtils.isEmpty(message))
			{
				return;
			}
			WsOutbound out = this.getWsOutbound();
			if (null != out)
			{
				out.writeTextMessage(CharBuffer.wrap(message));
			}
		}

		/**
		 * threadName属性的get方法
		 * 
		 * @return the threadName
		 */
		public String getThreadName()
		{
			return threadName;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.catalina.websocket.WebSocketServlet#createWebSocketInbound
	 * (java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol,
			HttpServletRequest request)
	{
		// 线程名称
		String threadName = Thread.currentThread().getName();
		TestMessageInbound mib = new TestMessageInbound(threadName);
		// for (TestMessageInbound m : participants.values())
		// {
		// try
		// {
		// m.sendMessage("欢迎：" + m.getThreadName() + "加入!目前会议室共"
		// + participants.size() + "人");
		// }
		// catch (IOException e)
		// {
		// e.printStackTrace();
		// }
		// }
		participants.put(threadName, mib);
		System.out.println(participants.size());
		return mib;
	}
}
