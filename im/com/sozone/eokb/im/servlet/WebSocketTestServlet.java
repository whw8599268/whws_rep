/**
 * 包名：com.sozone.eokb.im.servlet
 * 文件名：WebSocketTestServlet.java<br/>
 * 创建时间：2018-7-31 下午4:01:49<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.servlet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.exception.ValidateException;

/**
 * WebSocket 协议测试类<br/>
 * <p>
 * WebSocket 协议测试类<br/>
 * </p>
 * Time：2018-7-31 下午4:01:49<br/>
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
	private static final long serialVersionUID = 7396550969031158725L;

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(WebSocketTestServlet.class);

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
		logger.debug(LogUtils.format("测试是否支持WebSocket协议", subProtocol, request));
		Record<String, Object> currentUser = null;
		try
		{
			currentUser = ApacheShiroUtils.getCurrentUser();
		}
		catch (ValidateException e)
		{
			logger.error(
					LogUtils.format("当前用户尚未登录不允许创建WS会话!", subProtocol, request),
					e);
			throw new UnsupportedOperationException("当前用户尚未登录不允许创建WS会话!");
		}
		return new TestMessageInbound(currentUser);
	}

	/**
	 * 消息处理器<br/>
	 * <p>
	 * 消息处理器<br/>
	 * </p>
	 * Time：2018-7-31 下午4:08:24<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private class TestMessageInbound extends MessageInbound
	{

		private Record<String, Object> currentUser = null;

		/**
		 * @param currentUser
		 */
		private TestMessageInbound(Record<String, Object> currentUser)
		{
			super();
			this.currentUser = currentUser;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.apache.catalina.websocket.MessageInbound#onBinaryMessage(java
		 * .nio.ByteBuffer)
		 */
		@Override
		protected void onBinaryMessage(ByteBuffer message) throws IOException
		{
			// 当参会人发送了字节流消息时触发
			throw new UnsupportedOperationException(
					"Binary message not supported.");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.apache.catalina.websocket.MessageInbound#onTextMessage(java.nio
		 * .CharBuffer)
		 */
		@Override
		protected void onTextMessage(CharBuffer message) throws IOException
		{
			String msg = message.toString();
			this.getWsOutbound().writeTextMessage(
					CharBuffer.wrap(msg
							+ ","
							+ DateFormatUtils.format(new Date(),
									"yyyy-MM-dd HH:mm:ss")));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.catalina.websocket.StreamInbound#onClose(int)
		 */
		@Override
		protected void onClose(int status)
		{
			logger.info(LogUtils.format("关闭WS测试会话!", currentUser, status));
			// 离开
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
			this.getWsOutbound().writeTextMessage(CharBuffer.wrap(message));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.apache.catalina.websocket.StreamInbound#onOpen(org.apache.catalina
		 * .websocket.WsOutbound)
		 */
		@Override
		protected void onOpen(WsOutbound outbound)
		{
			logger.info(LogUtils.format("创建WS测试会话!", currentUser));
			// 加入
			String msg = "尊敬的：" + this.currentUser.getString("V_NAME")
					+ ",您好!WS会话已创建!";
			try
			{
				this.sendMessage(msg);
			}
			catch (IOException e)
			{
				logger.error(LogUtils.format("发送消息发送异常!"), e);
			}
			super.onOpen(outbound);
		}
	}

}
