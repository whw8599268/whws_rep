/**
 * 包名：com.sozone.eokb.im.pojo
 * 文件名：BidOpeningMeetingRoom.java<br/>
 * 创建时间：2017-8-16 下午3:19:47<br/>
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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.eokb.im.message.MessageFactory;
import com.sozone.eokb.im.service.IMDBService;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 开标会议室抽象类<br/>
 * <p>
 * 该类用于抽象一个开标会议室<br/>
 * </p>
 * Time：2017-8-16 下午3:19:47<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class BidOpeningMeetingRoom
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(BidOpeningMeetingRoom.class);
	/**
	 * 会议室所有参与人队列
	 */
	private final ConcurrentHashMap<String, Participant> participants = new ConcurrentHashMap<String, Participant>();

	/**
	 * 招标项目ID
	 */
	private String tenderProjectID;

	/**
	 * 招标项目名称
	 */
	private String tenderProjectName;

	/**
	 * 招标项目信息
	 */
	private Record<String, Object> tenderProject;

	/**
	 * 招标人/招标代理
	 */
	private Tenderer tenderer;

	/**
	 * 唯一标识符
	 */
	private String roomID;

	// /**
	// * 构造函数
	// *
	// * @param tenderer
	// * 投标人
	// */
	// public BidOpeningMeetingRoom(Tenderer tenderer)
	// {
	// this.tenderer = tenderer;
	// }

	/**
	 * 构造函数
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @param tenderer
	 *            投标人
	 */
	public BidOpeningMeetingRoom(String tenderProjectID, Tenderer tenderer)
	{
		this.tenderProjectID = tenderProjectID;
		this.tenderer = tenderer;
		this.roomID = Random.generateUUID();
		new IMDBService().saveRoomInfo(roomID, tenderProjectID,
				tenderer.getUserID());
	}

	/**
	 * 构造函数
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @param tenderProjectName
	 *            招标项目名称
	 * @param tenderer
	 *            投标人
	 */
	public BidOpeningMeetingRoom(String tenderProjectID,
			String tenderProjectName, Tenderer tenderer)
	{
		this(tenderProjectID, tenderer);
		this.tenderProjectName = tenderProjectName;
	}

	/**
	 * 加入会议<br/>
	 * <p>
	 * </p>
	 * 
	 * @param participant
	 *            参会人
	 */
	public void join(Participant participant)
	{
		logger.debug(LogUtils.format("加入会议!", participant));
		this.participants.put(participant.getUserID(), participant);
		// Record<String, Object> msg = MessageFactory.createNotice(participant
		// .getUserName() + "进入开标会议室!");

		// 增加参会人命令
		Record<String, Object> command = new RecordImpl<String, Object>();
		command.setColumn("COMMAND", "addParticipant");
		command.setColumn("PARAM", participant.getUserInfo());
		Record<String, Object> cmd = MessageFactory.createCommand(tenderer,
				command);
		// new IMDBService().saveMsg(this.roomID, msg);
		new IMDBService().saveMsg(this.roomID, cmd);
		// speak(participant, msg);
		// 发给WEB端
		speakToWeb(participant, cmd);
		// 发送移动端消息
		cmd = MessageFactory.createMobileCommand(tenderer, command);
		speakToMobile(participant, cmd);
	}

	/**
	 * 离开会议<br/>
	 * <p>
	 * </p>
	 * 
	 * @param participant
	 *            参会人
	 */
	public void leave(Participant participant)
	{
		logger.debug(LogUtils.format("离开会议!", participant));
		this.participants.remove(participant.getUserID());
		// 如果是招标人
		if (participant instanceof Tenderer)
		{
			this.tenderer = null;
		}

		// Record<String, Object> msg = MessageFactory.createNotice(participant
		// .getUserName() + "离开开标会议室!");

		// 删除参会人命令
		Record<String, Object> command = new RecordImpl<String, Object>();
		command.setColumn("COMMAND", "removeParticipant");
		command.setColumn("PARAM", participant.getUserInfo());
		Record<String, Object> cmd = MessageFactory.createCommand(tenderer,
				command);
		// new IMDBService().saveMsg(this.roomID, msg);
		new IMDBService().saveMsg(this.roomID, cmd);
		// speak(participant, msg);
		// 发给WEB端
		speakToWeb(participant, cmd);
		// 发送移动端消息
		cmd = MessageFactory.createMobileCommand(tenderer, command);
		speakToMobile(participant, cmd);
	}

	/**
	 * 发给WEB端人员<br/>
	 * <p>
	 * </p>
	 * 
	 * @param current
	 *            当前发言人
	 * @param message
	 *            发言内容
	 */
	public void speakToWeb(Participant current, Record<String, Object> message)
	{
		String recipientID = message.getString("RECIPIENT_ID");
		// 如果接收人不为空,发给特定的人
		if (StringUtils.isNotEmpty(recipientID))
		{
			Participant recipient = this.participants.get(recipientID);
			// 如果接收人不存在
			if (null == recipient)
			{
				return;
			}
			// 接收人接收
			recipient.sendMessage(message);
			// 当前人自己也要接收
			current.sendMessage(message);
			return;
		}
		// 如果是发给所有人
		Collection<Participant> pts = this.participants.values();
		for (Participant pt : pts)
		{
			// 只发给WEB端用户
			if (!pt.isMobileClient())
			{
				pt.sendMessage(message);
			}
		}
	}

	/**
	 * 发给移动端人员<br/>
	 * <p>
	 * </p>
	 * 
	 * @param current
	 *            当前发言人
	 * @param message
	 *            发言内容
	 */
	public void speakToMobile(Participant current,
			Record<String, Object> message)
	{
		String recipientID = message.getString("RECIPIENT_ID");
		// 如果接收人不为空,发给特定的人
		if (StringUtils.isNotEmpty(recipientID))
		{
			Participant recipient = this.participants.get(recipientID);
			// 如果接收人不存在
			if (null == recipient)
			{
				return;
			}
			// 接收人接收
			recipient.sendMessage(message);
			// 当前人自己也要接收
			current.sendMessage(message);
			return;
		}
		// 如果是发给所有人
		Collection<Participant> pts = this.participants.values();
		for (Participant pt : pts)
		{
			// 只发给移动端用户
			if (pt.isMobileClient())
			{
				pt.sendMessage(message);
			}
		}
	}

	/**
	 * 发言,发给所有人<br/>
	 * <p>
	 * </p>
	 * 
	 * @param current
	 *            当前发言人
	 * @param message
	 *            发言内容
	 */
	public void speak(Participant current, Record<String, Object> message)
	{
		String recipientID = message.getString("RECIPIENT_ID");
		// 如果接收人不为空,发给特定的人
		if (StringUtils.isNotEmpty(recipientID))
		{
			Participant recipient = this.participants.get(recipientID);
			// 如果接收人不存在
			if (null == recipient)
			{
				return;
			}
			// 接收人接收
			recipient.sendMessage(message);
			// 当前人自己也要接收
			current.sendMessage(message);
			return;
		}
		// 如果是发给所有人
		Collection<Participant> pts = this.participants.values();
		for (Participant pt : pts)
		{
			pt.sendMessage(message);
		}
	}

	/**
	 * 获取所有参会人信息列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @return 所有参会人信息列表
	 */
	public List<Record<String, Object>> getParticipantInfos()
	{
		List<Record<String, Object>> users = new LinkedList<Record<String, Object>>();
		Collection<Participant> pts = this.participants.values();
		Record<String, Object> userInfo = null;
		for (Participant pt : pts)
		{
			userInfo = new RecordImpl<String, Object>();
			userInfo.putAll(pt.getUserInfo());
			userInfo.setColumn("MUTE", pt.getMute());
			userInfo.setColumn("JOIN_TIME", pt.getJoinTime());
			if (pt instanceof Tenderer)
			{
				userInfo.setColumn("IS_TENDERER", true);
			}
			else
			{
				userInfo.setColumn("IS_TENDERER", false);
			}
			userInfo.setColumn("IS_MOBILE_CLIENT", pt.isMobileClient());
			// userInfo.setColumn("USER_NAME",
			// "福建省冠成天正工程管理有限公司（连城县天正建设工程监理有限公司）");
			users.add(userInfo);
		}

		Collections.sort(users, new Comparator<Record<String, Object>>()
		{

			@Override
			public int compare(Record<String, Object> o1,
					Record<String, Object> o2)
			{
				boolean boolean1 = o1.getBoolean("IS_TENDERER");
				boolean boolean2 = o2.getBoolean("IS_TENDERER");
				if (boolean1 != boolean2)
				{
					if (boolean1)
					{
						return -1;
					}
					return 1;
				}
				long long1 = o1.getLong("JOIN_TIME");
				long long2 = o2.getLong("JOIN_TIME");
				if (long1 > long2)
				{
					return 1;
				}
				else if (long1 < long2)
				{
					return -1;
				}
				else
				{
					return 0;
				}
			}
		});
		return users;
	}

	/**
	 * 判断指定ID的参会人是否存在<br/>
	 * <p>
	 * </p>
	 * 
	 * @param userID
	 *            指定用户ID
	 * @return boolean
	 */
	public boolean hasParticipant(String userID)
	{
		return this.participants.keySet().contains(userID);
	}

	/**
	 * tenderer属性的get方法
	 * 
	 * @return the tenderer
	 */
	public Tenderer getTenderer()
	{
		return this.tenderer;
	}

	/**
	 * tenderer属性的set方法
	 * 
	 * @param tenderer
	 *            the tenderer to set
	 */
	public void setTenderer(Tenderer tenderer)
	{
		if (null != this.tenderer)
		{
			this.tenderer.closeWebSocket();
		}
		this.tenderer = tenderer;
	}

	/**
	 * tenderProjectID属性的get方法
	 * 
	 * @return the tenderProjectID
	 */
	public String getTenderProjectID()
	{
		return tenderProjectID;
	}

	/**
	 * tenderProjectID属性的set方法
	 * 
	 * @param tenderProjectID
	 *            the tenderProjectID to set
	 */
	public void setTenderProjectID(String tenderProjectID)
	{
		this.tenderProjectID = tenderProjectID;
	}

	/**
	 * tenderProjectName属性的get方法
	 * 
	 * @return the tenderProjectName
	 */
	public String getTenderProjectName()
	{
		return tenderProjectName;
	}

	/**
	 * tenderProjectName属性的set方法
	 * 
	 * @param tenderProjectName
	 *            the tenderProjectName to set
	 */
	public void setTenderProjectName(String tenderProjectName)
	{
		this.tenderProjectName = tenderProjectName;
	}

	/**
	 * tenderProject属性的get方法
	 * 
	 * @return the tenderProject
	 */
	public Record<String, Object> getTenderProject()
	{
		return tenderProject;
	}

	/**
	 * tenderProject属性的set方法
	 * 
	 * @param tenderProject
	 *            the tenderProject to set
	 */
	public void setTenderProject(Record<String, Object> tenderProject)
	{
		this.tenderProject = tenderProject;
	}

	/**
	 * participants属性的get方法
	 * 
	 * @return the participants
	 */
	public Collection<Participant> getParticipants()
	{
		return participants.values();
	}

	/**
	 * 获取指定的ID的参会者
	 * 
	 * @param userID
	 *            用户ID
	 * @return 参会者
	 */
	public Participant getParticipant(String userID)
	{
		return this.participants.get(userID);
	}

	/**
	 * roomID属性的get方法
	 * 
	 * @return the roomID
	 */
	public String getRoomID()
	{
		return roomID;
	}

	/**
	 * roomID属性的set方法
	 * 
	 * @param roomID
	 *            the roomID to set
	 */
	public void setRoomID(String roomID)
	{
		this.roomID = roomID;
	}

	/**
	 * 关闭
	 */
	public void close()
	{
		Set<String> set = this.participants.keySet();
		for (String key : set)
		{
			this.participants.get(key).closeWebSocket();
			this.participants.remove(key);
		}
		new IMDBService().modifyRoomCloseTime(this.roomID);
	}

	/**
	 * 禁言<br/>
	 * <p>
	 * </p>
	 * 
	 * @param participant
	 *            发送人
	 * @param message
	 *            消息
	 */
	public void doMute(Participant participant, Record<String, Object> message)
	{
		// 被禁言人
		String recipientID = message.getString("RECIPIENT_ID");
		// 保存消息
		new IMDBService().saveMsg(this.roomID, message);
		// 招标人或者代理
		String tendererID = this.tenderer.getUserID();
		// 当前开标室所有人
		Collection<Participant> pts = this.participants.values();
		// 如果被禁言人的不为空,表示专门针对某个人禁言
		if (StringUtils.isNotEmpty(recipientID))
		{
			// 获取被禁言人
			Participant recipient = this.participants.get(recipientID);
			if (null == recipient)
			{
				return;
			}
			recipient.setMute(true);
			// 如果被禁言/解禁为移动端,要原样发生一个消息给对方
			if (recipient.isMobileClient())
			{
				recipient.sendMessage(message);
			}
		}
		else
		{
			// 禁言所有人员
			for (Participant pt : pts)
			{
				// 如果是招标人或者代理跳过
				if (StringUtils.equals(tendererID, pt.getUserID()))
				{
					continue;
				}
				pt.setMute(true);
				// 如果被禁言/解禁为移动端,要原样发生一个消息给对方
				if (pt.isMobileClient())
				{
					pt.sendMessage(message);
				}
			}
		}

		// 再发一个刷新参会人员列表的指令
		Record<String, Object> command = new RecordImpl<String, Object>();
		command.setColumn("COMMAND", "getMeetingRoomParticipants");
		command.setColumn("PARAM", "");
		Record<String, Object> cmd = MessageFactory.createCommand(tenderer,
				command);
		new IMDBService().saveMsg(this.roomID, cmd);
		// 创建移动端消息
		Record<String, Object> mobileCmd = MessageFactory.createMobileCommand(
				tenderer, command);
		for (Participant pt : pts)
		{
			// 如果是移动端
			if (pt.isMobileClient())
			{
				// 发送移动端消息
				pt.sendMessage(mobileCmd);
				continue;
			}
			// Web端发生web端消息
			pt.sendMessage(cmd);
		}
	}

	/**
	 * 解除禁言<br/>
	 * <p>
	 * </p>
	 * 
	 * @param participant
	 *            发送人
	 * @param message
	 *            消息
	 */
	public void doUnMute(Participant participant, Record<String, Object> message)
	{
		// 被解禁人
		String recipientID = message.getString("RECIPIENT_ID");
		// 保存消息
		new IMDBService().saveMsg(this.roomID, message);
		// 招标人或者代理
		String tendererID = this.tenderer.getUserID();
		// 当前开标室所有人
		Collection<Participant> pts = this.participants.values();
		// 如果被解禁人的不为空,表示专门针对某个人解禁
		if (StringUtils.isNotEmpty(recipientID))
		{
			// 获取被禁言人
			Participant recipient = this.participants.get(recipientID);
			if (null == recipient)
			{
				return;
			}
			// 设置为解禁
			recipient.setMute(false);
			// 如果被禁言/解禁为移动端,要原样发生一个消息给对方
			if (recipient.isMobileClient())
			{
				recipient.sendMessage(message);
			}
		}
		else
		{
			// 解禁所有人员
			for (Participant pt : pts)
			{
				// 如果是招标人或者代理跳过
				if (StringUtils.equals(tendererID, pt.getUserID()))
				{
					continue;
				}
				// 设置为解禁
				pt.setMute(false);
				// 如果被禁言/解禁为移动端,要原样发生一个消息给对方
				if (pt.isMobileClient())
				{
					pt.sendMessage(message);
				}
			}
		}

		// 再发一个刷新参会人员列表的指令
		Record<String, Object> command = new RecordImpl<String, Object>();
		command.setColumn("COMMAND", "getMeetingRoomParticipants");
		command.setColumn("PARAM", "");
		Record<String, Object> cmd = MessageFactory.createCommand(tenderer,
				command);
		new IMDBService().saveMsg(this.roomID, cmd);
		// 创建移动端消息
		Record<String, Object> mobileCmd = MessageFactory.createMobileCommand(
				tenderer, command);
		for (Participant pt : pts)
		{
			// 如果是移动端
			if (pt.isMobileClient())
			{
				// 发送移动端消息
				pt.sendMessage(mobileCmd);
				continue;
			}
			// Web端发生web端消息
			pt.sendMessage(cmd);
		}
	}

	/**
	 * 处理禁言/解禁业务<br/>
	 * 
	 * @param participant
	 *            Participant
	 * @param message
	 *            Record<String, Object>
	 * @param mute
	 *            boolean
	 */
	// public void dealMute(Participant participant,
	// Record<String, Object> message, boolean mute)
	// {
	// String recipientID = message.getString("RECIPIENT_ID");
	// String tendererID = this.tenderer.getUserID();
	//
	// Record<String, Object> command = new RecordImpl<String, Object>();
	// command.setColumn("COMMAND", "getMeetingRoomParticipants");
	// command.setColumn("PARAM", "");
	// Record<String, Object> cmd = MessageFactory.createCommand(tenderer,
	// command);
	// new IMDBService().saveMsg(this.roomID, cmd);
	//
	// // 创建移动端消息
	// Record<String, Object> mobileCmd = MessageFactory.createMobileCommand(
	// tenderer, command);
	// Collection<Participant> pts = this.participants.values();
	// if (StringUtils.isNotEmpty(recipientID))
	// {
	// Participant recipient = this.participants.get(recipientID);
	// if (null == recipient)
	// {
	// return;
	// }
	// recipient.setMute(mute);
	// }
	// else
	// {
	//
	// // 如果是发给所有人
	// for (Participant pt : pts)
	// {
	// if (StringUtils.equals(tendererID, pt.getUserID()))
	// {
	// continue;
	// }
	// pt.setMute(mute);
	// }
	// }
	// for (Participant pt : pts)
	// {
	// // 如果是移动端
	// if (pt.isMobileClient())
	// {
	// // 发送移动端消息
	// pt.sendMessage(mobileCmd);
	// continue;
	// }
	// // Web端发生web端消息
	// pt.sendMessage(cmd);
	// }
	//
	// }

	/**
	 * 让参与者推出会议室<br/>
	 * 
	 * @param userID
	 *            用户ID
	 * @return 参会者
	 */
	public Participant removeParticipant(String userID)
	{
		Participant p = this.participants.remove(userID);
		if (null != p)
		{
			p.setBidOpeningMeetingRoom(null);
		}
		return p;
	}

	/**
	 * 指令回复招标人<br/>
	 * 
	 * @param participant
	 *            发送人
	 * @param cmd
	 *            指令
	 */
	public void replyTendererCommond(Participant participant,
			Record<String, Object> cmd)
	{
		if (null != this.tenderer)
		{
			this.tenderer.sendMessage(cmd);
		}
	}

	/**
	 * 释放Tenderer对象<br/>
	 */
	public void releaseTenderer()
	{
		this.tenderer = null;
	}

	/**
	 * 获取相同公司的会议参与者<br/>
	 * <p>
	 * </p>
	 * 
	 * @return 相同公司的会议参与者,不存在为NULL
	 * @throws ServiceException
	 *             ServiceException
	 */
	public Participant getSameCompanyParticipant() throws ServiceException
	{
		// 获取当前用户的公司代码（组织机构或者统一社会信用代码）
		String entCode = SessionUtils.getEntUniqueCode();
		Collection<Participant> values = this.participants.values();
		for (Participant participant : values)
		{
			if (StringUtils.equals(participant.getEntUniqueCode(), entCode))
			{
				return participant;
			}
		}
		return null;
	}

	/**
	 * <br/>
	 * 
	 * @param entUniqueCode
	 *            entUniqueCode
	 * @return Record<String, String>
	 */
	public Record<String, String> getSameSocialcreditNOUserIDs(
			String entUniqueCode)
	{
		Record<String, String> map = new RecordImpl<String, String>();
		Collection<Participant> values = this.participants.values();
		for (Participant participant : values)
		{
			if (StringUtils.equals(participant.getEntUniqueCode(),
					entUniqueCode))
			{
				String userID = participant.getUserID();
				map.put(userID, userID);
			}
		}
		return map;
	}
}
