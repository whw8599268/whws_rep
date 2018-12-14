/**
 * 包名：com.sozone.eokb.bus.mr
 * 文件名：MeetingRoom.java<br/>
 * 创建时间：2017-9-4 下午1:33:42<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.mr;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Redirect;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.im.message.MessageFactory;
import com.sozone.eokb.im.pojo.BidOpeningMeetingRoom;
import com.sozone.eokb.im.pojo.Participant;
import com.sozone.eokb.im.pool.BidOpeningMeetingRoomPoolUtils;
import com.sozone.eokb.im.utils.IMUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 开标会议室服务接口<br/>
 * <p>
 * 开标会议室服务接口<br/>
 * </p>
 * Time：2017-9-4 下午1:33:42<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Permission(Level.Authenticated)
@Path(value = "room", desc = "开标会议室服务接口")
public class MeetingRoom extends BaseAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(MeetingRoom.class);
	/**
	 * 持久化接口
	 */
	protected ActiveRecordDAO activeRecordDAO = null;

	/**
	 * activeRecordDAO属性的set方法
	 * 
	 * @param activeRecordDAO
	 *            the activeRecordDAO to set
	 */
	public void setActiveRecordDAO(ActiveRecordDAO activeRecordDAO)
	{
		this.activeRecordDAO = activeRecordDAO;
	}

	/**
	 * 保持用户在线<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 视图
	 * @throws FacadeException
	 *             Facade异常
	 */
	// 定义路径
	@Path(value = "kuol", desc = "")
	@Service
	public boolean keepUserOnline(AeolusData data) throws FacadeException
	{
		return true;
	}

	/**
	 * 校验当前用户是否允许进入开标会议室<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @return 视图
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "join/{tpid}", desc = "校验当前用户是否允许进入开标会议室")
	@Service
	public ModelAndView doVerifyCurrentCanJoinMetting(
			@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("校验当前用户是否允许进入开标会议室!", tpid, data));
		String viewName = getTheme(data.getHttpServletRequest())
				+ "/metting_room/error.html";
		ModelMap model = new ModelMap();
		model.put("user_type", SessionUtils.getAttribute("roleCode"));
		model.put("TENDER_PROJECT_INFO",
				IMUtils.getTenderProjectInfo(activeRecordDAO, tpid));

		// 获取开标会议室
		BidOpeningMeetingRoom room = BidOpeningMeetingRoomPoolUtils
				.getTenderProjectMeetingRoom(tpid);
		// 如果不存在会议室,即会议室尚未开启
		if (null == room)
		{
			// 如果是投标人
			if (SessionUtils.isBidder())
			{
				logger.warn(LogUtils.format("招标人/招标代理尚未加入开标会议!", tpid, data));
				// 跳转到错误页面去
				model.put("errorDesc", "招标人/招标代理尚未加入开标会议!");
				ApacheShiroUtils.getSession().removeAttribute(
						ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY);
				return new ModelAndView(viewName, model);
			}
			// 如果是招标人
			String url = "/metting_room/index.html";
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ url, model);
		}
		// -------------------------
		// 如果会议室已经开启了
		// -------------------------

		// 获取同一个公司的参会人信息
		Participant participant = room.getSameCompanyParticipant();
		// 如果不存在同一个公司的参会人员
		if (null == participant)
		{
			// 成功进入
			String url = "/metting_room/index.html";
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ url, model);
		}

		// 是否为同一张证书
		model.put(
				"SAME_CERT",
				StringUtils.equals(participant.getUserID(),
						ApacheShiroUtils.getCurrentUserID()));
		Record<String, Object> ptInfo = new RecordImpl<String, Object>();
		ptInfo.setColumn("USER_ID", participant.getUserID());
		ptInfo.setColumn("LOGIN_NAME", participant.getLoginName());
		ptInfo.setColumn("KEY_NAME", participant.getKeyName());
		ptInfo.setColumn("JOIN_TIME", DateFormatUtils.format(new Date(
				participant.getJoinTime()), "yyyy-MM-dd HH:mm:ss"));
		ptInfo.setColumn("IS_MOBILE", participant.isMobileClient());
		model.put("PARTICIPANT", ptInfo);
		logger.warn(LogUtils.format("存在重复的用户,暂时无法直接登录,等待用户确认!", ptInfo, data));
		// 跳转到确认页面
		String url = "/metting_room/join.confirm.html";
		return new ModelAndView(getTheme(data.getHttpServletRequest()) + url,
				model);
	}

	/**
	 * 强制踢出指定USER ID的会议参与者<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param participantID
	 *            要踢掉的参与者ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "kout/{tpid}/{uid}", desc = "强制踢出指定USER ID的会议参与者")
	@Service
	public void doKickOutParticipant(@PathParam("tpid") String tpid,
			@PathParam("uid") String participantID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("强制踢出指定USER ID的会议参与者!", participantID,
				data));
		// 获取开标会议室
		BidOpeningMeetingRoom room = BidOpeningMeetingRoomPoolUtils
				.getTenderProjectMeetingRoom(tpid);
		if (null == room)
		{
			throw new ServiceException("", "当前招标项目,尚未开启会议室!");
		}
		// 踢掉的参与者
		Participant participant = room.removeParticipant(participantID);
		// 如果存在
		if (null != participant)
		{
			// 构造一个被踢出的命令
			Record<String, Object> command = new RecordImpl<String, Object>();
			command.setColumn("COMMAND", "doKickedOut");
			// 当前用户信息
			Record<String, Object> userInfo = new RecordImpl<String, Object>();
			userInfo.putAll(ApacheShiroUtils.getCurrentUser());
			userInfo.remove("PERMITTED_" + HttpMethod.GET + "_URIS");
			userInfo.remove("PERMITTED_" + HttpMethod.DELETE + "_URIS");
			userInfo.remove("PERMITTED_" + HttpMethod.POST + "_URIS");
			userInfo.remove("PERMITTED_" + HttpMethod.PUT + "_URIS");
			userInfo.remove("PERMITTED_" + HttpMethod.OPTIONS + "_URIS");
			userInfo.remove("PERMITTED_" + HttpMethod.HEAD + "_URIS");
			userInfo.remove("PERMITTED_SERVICE_URIS");
			// 把用户信息传过去
			command.setColumn("PARAM", userInfo);
			Record<String, Object> cmd = MessageFactory.createCommand(
					participant, participant.getUserID(),
					participant.getUserName(), command);
			// 先发送一个消息通知对方被踹了
			// 如果是移动端用户
			if (participant.isMobileClient())
			{
				// 创建移动端消息
				Record<String, Object> mobileCmd = MessageFactory
						.createMobileCommand(participant, command);
				participant.sendMessage(mobileCmd);
			}
			else
			{
				// WEB端用户
				participant.sendMessage(cmd);
			}
			
			return;
		}
	}

	/**
	 * 加入开标会议<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @return 视图
	 * @throws FacadeException
	 *             Facade异常
	 */
	// 定义路径
	// @Path(value = "join/{tpid}", desc = "加入开标会议")
	// @Service
	// public ModelAndView joinMetting(@PathParam("tpid") String tpid,
	// AeolusData data) throws FacadeException
	// {
	// logger.debug(LogUtils.format("加入开标会议!", tpid, data));
	//
	// String viewName = getTheme(data.getHttpServletRequest())
	// + "/metting_room/error.html";
	// ModelMap model = new ModelMap();
	// model.put("user_type", SessionUtils.getAttribute("roleCode"));
	// model.put("TENDER_PROJECT_INFO",
	// IMUtils.getTenderProjectInfo(activeRecordDAO, tpid));
	// BidOpeningMeetingRoom room = BidOpeningMeetingRoomPoolUtils
	// .getTenderProjectMeetingRoom(tpid);
	// boolean bidder = IMUtils.isBidder(tpid);
	// if (bidder && null == room)
	// {
	// model.put("errorDesc", "招标人/招标代理尚未加入开标会议!");
	// ApacheShiroUtils.getSession().removeAttribute(
	// ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY);
	// return new ModelAndView(viewName, model);
	// }
	//
	// // 重复登陆，发消息通知 用户已在其他地方登陆，被退出，剔除原本的账号。
	// letParticipantOut(tpid);
	// if (!IMUtils.isTendererOrBidder(tpid))
	// {
	// model.put("errorDesc", "当前用户(不是招标人/招标代理或投标人)不允许进入开标会议室!");
	// ApacheShiroUtils.getSession().removeAttribute(
	// ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY);
	// return new ModelAndView(viewName, model);
	// }
	//
	// Long imLastRefreshTime = SessionUtils
	// .getAttribute("IM_LAST_REFRESH_TIME");
	// long timeMillis = System.currentTimeMillis();
	// if (null == imLastRefreshTime)
	// {
	// SessionUtils.setAttribute("IM_LAST_REFRESH_TIME", timeMillis);
	// SessionUtils.setAttribute("IM_LAST_REFRESH_COUNT", 1);
	// }
	// else
	// {
	// String timestamp = TimestampUtils.parseTimestamp(imLastRefreshTime,
	// "yyyy-MM-dd HH:mm");
	//
	// String timestamp2 = TimestampUtils.parseTimestamp(timeMillis,
	// "yyyy-MM-dd HH:mm");
	//
	// if (StringUtils.equals(timestamp, timestamp2))
	// {
	// Integer count = SessionUtils
	// .getAttribute("IM_LAST_REFRESH_COUNT");
	// if (null != count)
	// {
	// if (count >= 5)
	// {
	// model.put("errorDesc", "刷新太过频繁!请在一分钟后再进行刷新！");
	// ApacheShiroUtils.getSession().removeAttribute(
	// ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY);
	// return new ModelAndView(viewName, model);
	// }
	// else
	// {
	// SessionUtils.setAttribute("IM_LAST_REFRESH_COUNT",
	// count + 1);
	// }
	// }
	// else
	// {
	// model.put("errorDesc", "获取最近刷新时间失败!");
	// ApacheShiroUtils.getSession().removeAttribute(
	// ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY);
	// return new ModelAndView(viewName, model);
	// }
	//
	// }
	// else
	// {
	// SessionUtils.setAttribute("IM_LAST_REFRESH_TIME", timeMillis);
	// SessionUtils.setAttribute("IM_LAST_REFRESH_COUNT", 1);
	// }
	//
	// }
	//
	// // if (IMUtils.isTenderer(tenderProjectID))
	// // {
	// // viewName = getTheme(data.getHttpServletRequest())
	// // + "/frame/frame.kb.zbr.html";
	// // }
	// // else
	// // {
	// // viewName = getTheme(data.getHttpServletRequest())
	// // + "/frame/frame.kb.tbr.html";
	// // }
	// String url = "/metting_room/index.html";
	//
	// return new ModelAndView(getTheme(data.getHttpServletRequest()) + url,
	// model);
	// }

	/**
	 * 离开开标会议并退出系统<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             Facade异常
	 * @return String
	 */
	// 定义路径
	@Path(value = "logout/{tpid}", desc = "离开开标会议")
	@Service
	@Redirect(suffix = "")
	public String logoutMettingRoom(@PathParam("tpid") String tenderProjectID,
			AeolusData data) throws FacadeException
	{
		boolean tenderer = IMUtils.isTenderer(tenderProjectID);
		if (tenderer)
		{
			BidOpeningMeetingRoomPoolUtils.closeMeetingRoom(tenderProjectID);
		}
		SecurityUtils.getSubject().logout();
		return "";
	}

	// /**
	// * 重复登陆，发消息通知 用户已在其他地方登陆，被退出，剔除原本的账号<br/>
	// *
	// * @param tenderProjectID
	// * @throws ValidateException
	// */
	// private void letParticipantOut(String tenderProjectID)
	// throws ValidateException
	// {
	// BidOpeningMeetingRoom room = BidOpeningMeetingRoomPoolUtils
	// .getTenderProjectMeetingRoom(tenderProjectID);
	// if (null == room)
	// {
	// return;
	// }
	// Record<String, String> userIDMap = new RecordImpl<String, String>();
	// String userID = ApacheShiroUtils.getCurrentUserID();
	// userIDMap.setColumn(userID, userID);
	// String entUniqueCode = SessionUtils.getEntUniqueCode();
	// Record<String, String> userIDMap2 = room
	// .getSameSocialcreditNOUserIDs(entUniqueCode);
	// userIDMap.putAll(userIDMap2);
	//
	// Set<String> keySet = userIDMap.keySet();
	// for (String key : keySet)
	// {
	// sendMsgAndLogout(room, key);
	// }
	// }

	// /**
	// * @param room
	// * @param userID
	// */
	// private void sendMsgAndLogout(BidOpeningMeetingRoom room, String userID)
	// {
	// if (!room.hasParticipant(userID))
	// {
	// return;
	// }
	// // 要强退的回话
	// Participant participant = room.removeParticipant(userID);
	//
	// Record<String, Object> command = new RecordImpl<String, Object>();
	// command.setColumn("COMMAND", "getOut");
	// command.setColumn("PARAM", participant.getUserInfo());
	// Record<String, Object> cmd = MessageFactory.createCommand(participant,
	// participant.getUserID(), participant.getUserName(), command);
	//
	// new IMDBService().saveMsg(room.getRoomID(), cmd);
	// try
	// {
	// if (participant.isMobileClient())
	// {
	// // 移动端的消息另外发送
	// }
	// else
	// {
	// //这里只发送给WEB端
	// participant.sendMessage(cmd);
	// }
	// participant.closeWebSocket();
	// }
	// catch (Exception e)
	// {
	// logger.error(LogUtils.format("发送会议参与人离开公告发生异常", participant, cmd),
	// e);
	// }
	// }

}
