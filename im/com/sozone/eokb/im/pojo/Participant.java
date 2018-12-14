/**
 * 包名：com.sozone.eokb.im.pojo
 * 文件名：Participant.java<br/>
 * 创建时间：2017-8-16 下午3:23:07<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.pojo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.WsOutbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.im.message.MessageFactory;
import com.sozone.eokb.im.service.IMDBService;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 参会者抽闲实体<br/>
 * <p>
 * 该类用于抽象一个参与会议的人<br/>
 * </p>
 * Time：2017-8-16 下午3:23:07<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class Participant extends MessageInbound
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Participant.class);

	/**
	 * HTTP请求对象
	 */
	protected final HttpServletRequest request;

	/**
	 * 参会者的用户ID,唯一标识
	 */
	protected String userID;

	/**
	 * 姓名,公司名称
	 */
	protected String userName;

	/**
	 * 证书的LoginName
	 */
	protected String loginName;

	/**
	 * 证书的KeyName
	 */
	protected String keyName;

	/**
	 * 用户LOGO
	 */
	protected String userLogo;

	/**
	 * 禁言
	 */
	protected boolean mute;

	/**
	 * 当前用户信息
	 */
	protected Record<String, Object> userInfo;

	/**
	 * 开标会议室
	 */
	protected BidOpeningMeetingRoom bidOpeningMeetingRoom;

	/**
	 * 最后一次参与时间（排序使用）
	 */
	protected long joinTime;

	/**
	 * 企业唯一代码（统一社会信用代码或者机构代码）
	 */
	protected String entUniqueCode;

	/**
	 * 是否为移动端用户
	 */
	protected boolean mobileClient;

	/**
	 * 构造函数
	 * 
	 * @param request
	 *            HttpServletRequest
	 * @param bidOpeningMeetingRoom
	 *            开标会议室
	 */
	public Participant(HttpServletRequest request,
			BidOpeningMeetingRoom bidOpeningMeetingRoom)
	{
		this(request);
		this.mute = false;
		this.bidOpeningMeetingRoom = bidOpeningMeetingRoom;
	}

	/**
	 * 构造函数
	 * 
	 * @param request
	 *            HttpServletRequest
	 */
	public Participant(HttpServletRequest request)
	{
		this.request = request;
		try
		{
			this.userID = ApacheShiroUtils.getCurrentUserID();
			this.userName = SessionUtils.getCompanyName();
			this.entUniqueCode = SessionUtils.getEntUniqueCode();
			this.mute = false;
			this.mobileClient = SessionUtils.isMobileClient();
			//
			this.loginName = SessionUtils.getLoginName();
			this.keyName = ApacheShiroUtils.getCurrentUser().getColumn(
					"V_KEY_NAME");
			this.userInfo = new RecordImpl<String, Object>();
			this.userInfo.setColumn("USER_ID", this.userID);
			this.userInfo.setColumn("USER_NAME", this.userName);
			this.userInfo.setColumn("ENT_UNIQUE_CODE", this.entUniqueCode);

			this.userInfo.setColumn("MUTE", this.mute);
			this.joinTime = System.currentTimeMillis();
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("创建会议参与者失败!"), e);
		}
	}

	/**
	 * entUniqueCode属性的get方法
	 * 
	 * @return the socialcreditNO
	 */
	public String getEntUniqueCode()
	{
		return entUniqueCode;
	}

	/**
	 * joinTime属性的get方法
	 * 
	 * @return the joinTime
	 */
	public long getJoinTime()
	{
		return joinTime;
	}

	/**
	 * mute属性的get方法
	 * 
	 * @return the mute
	 */
	public boolean getMute()
	{
		return mute;
	}

	/**
	 * mute属性的set方法
	 * 
	 * @param mute
	 *            the mute to set
	 */
	public void setMute(boolean mute)
	{
		this.mute = mute;
		if (!CollectionUtils.isEmpty(this.userInfo))
		{
			this.userInfo.setColumn("MUTE", this.mute);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.catalina.websocket.MessageInbound#onBinaryMessage(java.nio
	 * .ByteBuffer)
	 */
	@Override
	protected void onBinaryMessage(ByteBuffer message) throws IOException
	{
		// 当参会人发送了字节流消息时触发
		throw new UnsupportedOperationException("Binary message not supported.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.catalina.websocket.MessageInbound#onTextMessage(java.nio.
	 * CharBuffer)
	 */
	@Override
	protected void onTextMessage(CharBuffer message) throws IOException
	{
		if (null == bidOpeningMeetingRoom)
		{
			throw new UnsupportedOperationException("无法找到对应的开标会议室对象!");
		}

		// 反序列化对象
		JSONObject obj = JSON.parseObject(message.toString());
		// 当参会人发送了字符串消息时触发
		Record<String, Object> msg = MessageFactory.createMessge(this, obj);
		new IMDBService().saveMsg(bidOpeningMeetingRoom.getRoomID(), msg);
		int messageType = msg.getInteger("MESSAGE_TYPE");
		switch (messageType)
		{
		// 禁言
			case -2:
				bidOpeningMeetingRoom.doMute(this, msg);
				break;
			// 解禁
			case -3:
				bidOpeningMeetingRoom.doUnMute(this, msg);
				break;
			// 投标人回复招标人（命令）
			case -4:
				bidOpeningMeetingRoom.replyTendererCommond(this, msg);
				break;
			// 标准字符串消息,发给所类型
			case 0:
				bidOpeningMeetingRoom.speak(this, msg);
				break;
			case 1:
				// 如果是WEB端消息,只发给WEB端用户
				bidOpeningMeetingRoom.speakToWeb(this, msg);
				break;
			case 100:
				// 如果是移动端消息,只发给移动端用户
				bidOpeningMeetingRoom.speakToMobile(this, msg);
				break;
			default:
				bidOpeningMeetingRoom.speak(this, msg);
				break;
		}
	}

	/**
	 * request属性的get方法
	 * 
	 * @return the request
	 */
	public HttpServletRequest getHttpServletRequest()
	{
		return request;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.catalina.websocket.StreamInbound#onClose(int)
	 */
	@Override
	protected void onClose(int status)
	{
		// 离开聊天室时触发
		if (null != bidOpeningMeetingRoom)
		{
			bidOpeningMeetingRoom.leave(this);
			if (this instanceof Tenderer)
			{
				bidOpeningMeetingRoom.releaseTenderer();
			}
			// 一定将bidOpeningMeetingRoom制空
			this.bidOpeningMeetingRoom = null;
		}
		super.onClose(status);
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
		// 加入聊天室时触发
		bidOpeningMeetingRoom.join(this);
		super.onOpen(outbound);
	}

	/**
	 * 发送JSON消息字符串<br/>
	 * <p>
	 * </p>
	 * 
	 * @param message
	 *            消息内容
	 */
	public void sendMessage(Record<String, Object> message)
	{

		try
		{
			this.getWsOutbound().writeTextMessage(
					CharBuffer.wrap(JSON.toJSONString(message)));
		}
		catch (IOException e)
		{
			logger.warn(LogUtils.format("发送消息发生异常!"), e);
		}
	}

	/**
	 * userID属性的get方法
	 * 
	 * @return the userID
	 */
	public String getUserID()
	{
		return userID;
	}

	/**
	 * userName属性的get方法
	 * 
	 * @return the userName
	 */
	public String getUserName()
	{
		return userName;
	}

	/**
	 * userLogo属性的get方法
	 * 
	 * @return the userLogo
	 */
	public String getUserLogo()
	{
		return userLogo;
	}

	/**
	 * userLogo属性的set方法
	 * 
	 * @param userLogo
	 *            the userLogo to set
	 */
	public void setUserLogo(String userLogo)
	{
		this.userLogo = userLogo;
	}

	/**
	 * bidOpeningMeetingRoom属性的get方法
	 * 
	 * @return the bidOpeningMeetingRoom
	 */
	public BidOpeningMeetingRoom getBidOpeningMeetingRoom()
	{
		return bidOpeningMeetingRoom;
	}

	/**
	 * bidOpeningMeetingRoom属性的set方法
	 * 
	 * @param bidOpeningMeetingRoom
	 *            the bidOpeningMeetingRoom to set
	 */
	public void setBidOpeningMeetingRoom(
			BidOpeningMeetingRoom bidOpeningMeetingRoom)
	{
		this.bidOpeningMeetingRoom = bidOpeningMeetingRoom;
	}

	/**
	 * userInfo属性的get方法
	 * 
	 * @return the userInfo
	 */
	public Record<String, Object> getUserInfo()
	{
		userInfo.setColumn("MUTE", this.mute);
		return userInfo;
	}

	/**
	 * userInfo属性的set方法
	 * 
	 * @param userInfo
	 *            the userInfo to set
	 */
	public void setUserInfo(Record<String, Object> userInfo)
	{
		this.userInfo = userInfo;
	}

	/**
	 * 强制退出
	 */
	public void closeWebSocket()
	{
		this.onClose(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Participant [IPAddr=" + ApacheShiroUtils.getClientIP()
				+ ", userID=" + userID + ", userName=" + userName + ", mute="
				+ mute + ", userInfo=" + userInfo + ", joinTime=" + joinTime
				+ "]";
	}

	/**
	 * mobileClient属性的get方法
	 * 
	 * @return the mobileClient
	 */
	public boolean isMobileClient()
	{
		return mobileClient;
	}

	/**
	 * mobileClient属性的set方法
	 * 
	 * @param mobileClient
	 *            the mobileClient to set
	 */
	public void setMobileClient(boolean mobileClient)
	{
		this.mobileClient = mobileClient;
	}

	/**
	 * loginName属性的get方法
	 * 
	 * @return the loginName
	 */
	public String getLoginName()
	{
		return loginName;
	}

	/**
	 * keyName属性的get方法
	 * 
	 * @return the keyName
	 */
	public String getKeyName()
	{
		return keyName;
	}

}
