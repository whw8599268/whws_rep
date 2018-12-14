/**
 * 包名：com.sozone.eokb.mobile.bus.im
 * 文件名：MobileIMAction.java<br/>
 * 创建时间：2018-11-23 上午10:43:33<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.bus.im;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
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
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.eokb.bus.project.Project;
import com.sozone.eokb.im.message.MessageFactory;
import com.sozone.eokb.im.pojo.BidOpeningMeetingRoom;
import com.sozone.eokb.im.pojo.Participant;
import com.sozone.eokb.im.pojo.Tenderer;
import com.sozone.eokb.im.pool.BidOpeningMeetingRoomPoolUtils;
import com.sozone.eokb.im.utils.IMUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 移动端即时通讯服务接口<br/>
 * <p>
 * 移动端即时通讯服务接口<br/>
 * </p>
 * Time：2018-11-23 上午10:43:33<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/mobile/im", desc = "移动端即时通讯服务接口")
@Permission(Level.Authenticated)
public class MobileIMAction
{

	/**
	 * ws协议
	 */
	private static final String WEB_SOCKET_SCHEME = "ws://";

	/**
	 * wss协议
	 */
	private static final String WEB_SOCKET_SSL_SCHEME = "wss://";

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Project.class);

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
	@Path(value = "vjoin/{tpid}", desc = "校验当前用户是否允许进入开标会议室")
	@Service
	public ResultVO<Record<String, Object>> doVerifyCurrentCanJoinMetting(
			@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("校验当前用户是否允许进入开标会议室!", tpid, data));
		ResultVO<Record<String, Object>> result = new ResultVO<Record<String, Object>>();
		// 获取开标会议室
		BidOpeningMeetingRoom room = BidOpeningMeetingRoomPoolUtils
				.getTenderProjectMeetingRoom(tpid);
		Record<String, Object> params = new RecordImpl<String, Object>();
		// 判断当前使用的是什么协议
		String scheme = data.getHttpServletRequest().getScheme();
		String wsScheme = WEB_SOCKET_SCHEME;
		// 如果使用的是https协议
		if (StringUtils.equalsIgnoreCase("https", scheme))
		{
			wsScheme = WEB_SOCKET_SSL_SCHEME;
		}

		// websocket路径
		String wsPath = wsScheme + data.getHttpServletRequest().getServerName()
				+ ":" + data.getHttpServletRequest().getServerPort()
				+ data.getHttpServletRequest().getContextPath();
		params.setColumn("WEBSOCKET_URL", wsPath);
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
		// 当前用户信息
		params.setColumn("CURRENT_USER", userInfo);
		// 当前招标项目信息
		// params.setColumn("TENDER_PROJECT_INFO", tenderProject);
		// 是否为招标人
		params.setColumn("IS_TENDERER", IMUtils.isTenderer(tpid));
		result.setSuccess(true);
		result.setResult(params);
		// 如果不存在会议室,即会议室尚未开启
		if (null == room)
		{
			// 如果是投标人
			if (SessionUtils.isBidder())
			{
				logger.warn(LogUtils.format("招标人/招标代理尚未加入开标会议!", tpid, data));
				throw new ServiceException("EOKB-IM-0001", "招标人/招标代理尚未加入开标会议!");
			}
			// 如果是招标人
			return result;
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
			return result;
		}
		// 是否为同一张证书
		Record<String, Object> ptInfo = new RecordImpl<String, Object>();
		ptInfo.setColumn("USER_ID", participant.getUserID());
		ptInfo.setColumn("LOGIN_NAME", participant.getLoginName());
		ptInfo.setColumn("KEY_NAME", participant.getKeyName());
		ptInfo.setColumn("JOIN_TIME", DateFormatUtils.format(new Date(
				participant.getJoinTime()), "yyyy-MM-dd HH:mm:ss"));
		ptInfo.setColumn("IS_MOBILE", participant.isMobileClient());
		ptInfo.setColumn(
				"SAME_CERT",
				StringUtils.equals(participant.getUserID(),
						ApacheShiroUtils.getCurrentUserID()));
		logger.warn(LogUtils.format("存在重复的用户,暂时无法直接登录,等待用户确认!", ptInfo, data));
		// 提示确认
		result.setSuccess(false);
		result.setResult(ptInfo);
		return result;
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
	 * 验证是否允许进入开标会议室<br/>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @return 响应
	 * @throws FacadeException
	 *             FacadeException
	 */
	// @Path(value = "/room/{tpid}", desc = "验证是否允许进入开标会议室")
	// @Service
	// public ResultVO<Record<String, Object>> doVerify(
	// @PathParam("tpid") String tpid, AeolusData data)
	// throws FacadeException
	// {
	// logger.debug(LogUtils.format("验证是否允许进入开标会议室", tpid, data));
	// ResultVO<Record<String, Object>> result = new ResultVO<Record<String,
	// Object>>();
	// Record<String, Object> tenderProject = IMUtils.getTenderProjectInfo(
	// activeRecordDAO, tpid);
	// try
	// {
	// if (CollectionUtils.isEmpty(tenderProject))
	// {
	// logger.error(LogUtils.format("获取不到招标项目信息!"));
	// throw new ServiceException("EOKB-IM-0001", "获取不到招标项目信息!");
	// }
	// BidOpeningMeetingRoomPoolUtils.canAccessMeetingRoom(tpid);
	// }
	// catch (Exception e)
	// {
	// String errorDesc = null;
	// if (e instanceof UnsupportedOperationException)
	// {
	// errorDesc = e.getMessage();
	// }
	// else
	// {
	// errorDesc = "会议无法进入!";
	// }
	// logger.error(LogUtils.format(errorDesc));
	// throw new ServiceException("EOKB-IM-0002", errorDesc);
	// }
	//
	// Record<String, Object> params = new RecordImpl<String, Object>();
	// // 判断当前使用的是什么协议
	// String scheme = data.getHttpServletRequest().getScheme();
	// String wsScheme = WEB_SOCKET_SCHEME;
	// // 如果使用的是https协议
	// if (StringUtils.equalsIgnoreCase("https", scheme))
	// {
	// wsScheme = WEB_SOCKET_SSL_SCHEME;
	// }
	//
	// // websocket路径
	// String wsPath = wsScheme + data.getHttpServletRequest().getServerName()
	// + ":" + data.getHttpServletRequest().getServerPort()
	// + data.getHttpServletRequest().getContextPath();
	// params.setColumn("WEBSOCKET_URL", wsPath);
	// // 当前用户信息
	// Record<String, Object> userInfo = new RecordImpl<String, Object>();
	// userInfo.putAll(ApacheShiroUtils.getCurrentUser());
	// userInfo.remove("PERMITTED_" + HttpMethod.GET + "_URIS");
	// userInfo.remove("PERMITTED_" + HttpMethod.DELETE + "_URIS");
	// userInfo.remove("PERMITTED_" + HttpMethod.POST + "_URIS");
	// userInfo.remove("PERMITTED_" + HttpMethod.PUT + "_URIS");
	// userInfo.remove("PERMITTED_" + HttpMethod.OPTIONS + "_URIS");
	// userInfo.remove("PERMITTED_" + HttpMethod.HEAD + "_URIS");
	// userInfo.remove("PERMITTED_SERVICE_URIS");
	// // 当前用户信息
	// params.setColumn("CURRENT_USER", userInfo);
	// // 当前招标项目信息
	// // params.setColumn("TENDER_PROJECT_INFO", tenderProject);
	// // 是否为招标人
	// params.setColumn("IS_TENDERER", IMUtils.isTenderer(tpid));
	// result.setSuccess(true);
	// result.setResult(params);
	// return result;
	// }

	/**
	 * 获取指定招标项目开标会议室的招标人/招标代理<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @return 开标会议室的招标人/招标代理
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/roomtder/{tpid}", desc = "获取指定招标项目开标会议室的招标人/招标代理")
	@Service
	public ResultVO<Record<String, Object>> getMeetingRoomTenderer(
			@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取指定招标项目开标会议室的招标人/招标代理", tpid, data));
		BidOpeningMeetingRoom room = BidOpeningMeetingRoomPoolUtils
				.getTenderProjectMeetingRoom(tpid);
		if (null == room)
		{
			throw new ServiceException("", "招标项目不存在开标会议室!");
		}
		// 获取会议室招标人
		Tenderer tenderer = room.getTenderer();
		// 返回结果
		ResultVO<Record<String, Object>> result = new ResultVO<Record<String, Object>>();
		// 如果招标人不在
		if (null == tenderer)
		{
			result.setSuccess(false);
			return result;
		}
		result.setSuccess(true);
		result.setResult(tenderer.getUserInfo());
		return result;
	}

	/**
	 * 获取指定招标项目开标会议室的所有在线参会者信息列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @return 所有在线参会者信息列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/participants/{tpid}", desc = "获取指定招标项目开标会议室的所有在线参会者信息列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getMeetingRoomParticipants(
			@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取指定招标项目开标会议室的所有在线参会者信息列表", tpid, data));
		BidOpeningMeetingRoom room = BidOpeningMeetingRoomPoolUtils
				.getTenderProjectMeetingRoom(tpid);
		if (null == room)
		{
			throw new ServiceException("", "招标项目不存在开标会议室!");
		}
		return room.getParticipantInfos();
	}

	/**
	 * 获取消息记录信息<br/>
	 * <p>
	 * 注意这里为了性能问题,只取近期100条记录
	 * </p>
	 * 
	 * @param tenderProjectID
	 *            项目ID
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "hcdatas/{tpid}", desc = "获取消息记录信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getHistoricalChatDatas(
			@PathParam("tpid") String tenderProjectID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取消息记录信息", tenderProjectID, data));
		Record<String, Object> param = data.getRecord();
		param.setColumn("tpid", tenderProjectID);
		param.setColumn("cuid", ApacheShiroUtils.getCurrentUserID());
		return this.activeRecordDAO.statement().selectList(
				"IM.getHistoricalChatDatas", param);
	}
}
