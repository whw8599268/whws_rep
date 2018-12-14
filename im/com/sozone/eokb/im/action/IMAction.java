/**
 * 包名：com.sozone.eokb.im.action
 * 文件名：IMAction.java<br/>
 * 创建时间：2017-8-17 上午9:17:38<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
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
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.im.common.ConstantIM;
import com.sozone.eokb.im.pojo.BidOpeningMeetingRoom;
import com.sozone.eokb.im.pojo.Tenderer;
import com.sozone.eokb.im.pool.BidOpeningMeetingRoomPoolUtils;
import com.sozone.eokb.im.utils.IMUtils;

/**
 * 即时通讯服务接口<br/>
 * <p>
 * 即时通讯服务接口<br/>
 * </p>
 * Time：2017-8-17 上午9:17:38<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/im", desc = "即时通讯服务接口")
@Permission(Level.Authenticated)
public class IMAction extends BaseAction
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
	private static Logger logger = LoggerFactory.getLogger(IMAction.class);

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
	 * 进入招标项目开标会议室<br/>
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
	@Path(value = "/room/{tpid}", desc = "进入招标项目开标会议室")
	@Service
	public ModelAndView openMeetingRoomView(@PathParam("tpid") String tpid,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("进入招标项目开标会议室", tpid, data));

		Record<String, Object> tenderProject = IMUtils.getTenderProjectInfo(
				activeRecordDAO, tpid);
		try
		{
			if (CollectionUtils.isEmpty(tenderProject))
			{
				throw new UnsupportedOperationException("获取不到招标项目信息！");
			}
			BidOpeningMeetingRoomPoolUtils.canAccessMeetingRoom(tpid);
		}
		catch (Exception e)
		{
			String url = "/im/im.error.html";
			ModelMap modelMap = new ModelMap();
			String s = null;
			if (e instanceof UnsupportedOperationException)
			{
				s = e.getMessage();
			}
			else
			{
				s = "会议无法进入！";
			}
			modelMap.put("MSG", s);
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ url, modelMap);
		}

		String url = "/im/index.im.html";
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("CURRENT_USER", ApacheShiroUtils.getCurrentUser());

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
		// 用于解决Flash不能主动携带SESSION_ID问题
		params.setColumn("SESSION_ID", data.getHttpServletRequest()
				.getSession().getId());

		// xmlsocket 路径
		String xsPath = "xmlsocket://"
				+ data.getHttpServletRequest().getServerName()
				+ ":"
				+ SystemParamUtils.getProperty(
						SysParamKey.FLASH_SOCKET_SECURITY_POLICY_FILE_PORT,
						"843");
		params.setColumn("XMLSOCKET_URL", xsPath);
		params.setColumn("TENDER_PROJECT_INFO", tenderProject);
		params.setColumn("IS_TENDERER", IMUtils.isTenderer(tpid));
		return new ModelAndView(getTheme(data.getHttpServletRequest()) + url,
				params);
	}

	/**
	 * 获取指定招标项目开标会议室的所有在线参会者信息列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectID
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
			@PathParam("tpid") String tenderProjectID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取指定招标项目开标会议室的所有在线参会者信息列表",
				tenderProjectID, data));
		BidOpeningMeetingRoom room = BidOpeningMeetingRoomPoolUtils
				.getTenderProjectMeetingRoom(tenderProjectID);
		if (null == room)
		{
			throw new ValidateException("", "当前招标项目不存在开标会议室!");
		}
		return room.getParticipantInfos();
	}

	/**
	 * 获取所有开标会议室信息列表<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 所有在线参会者信息列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/roomList", desc = "获取所有开标会议室信息列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getMeetingRoomList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取所有开标会议室信息列表", data));
		ConcurrentHashMap<String, BidOpeningMeetingRoom> rooms = BidOpeningMeetingRoomPoolUtils
				.getRooms();

		List<Record<String, Object>> list = new ArrayList<Record<String, Object>>();
		if (CollectionUtils.isEmpty(rooms))
		{
			return list;
		}
		Set<String> keySet = rooms.keySet();
		Record<String, Object> record = null;
		BidOpeningMeetingRoom room = null;
		Tenderer tenderer = null;
		for (String key : keySet)
		{
			room = rooms.get(key);
			record = new RecordImpl<String, Object>();
			record.setColumn("V_TPID", key);
			// record.putAll(room.getTenderProject());
			tenderer = room.getTenderer();
			if (null != tenderer)
			{
				record.setColumn("V_TENDERER_ID", tenderer.getUserID());
				record.setColumn("V_TENDERER_NAME", tenderer.getUserName());
			}
			list.add(record);
		}
		return list;
	}

	/**
	 * 
	 * 关闭开标会议室<br/>
	 * 
	 * @param tenderProjectID
	 *            项目ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/closeRoom/{tpid}", desc = "关闭开标会议室")
	@Service
	public void closeMeetingRoom(@PathParam("tpid") String tenderProjectID,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("关闭开标会议室", data));
		boolean admin = ApacheShiroUtils.isAdmin();
		if (admin)
		{
			BidOpeningMeetingRoomPoolUtils.closeMeetingRoom(tenderProjectID);
		}
		else
		{
			throw new ValidateException("", "只有超级管理员能进行此操作");
		}
	}

	/**
	 * 获取消息记录分页信息<br/>
	 * <p>
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
	@Path(value = "hns/{tpid}", desc = "获取消息记录分页信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getHistoricalNewsPage(
			@PathParam("tpid") String tenderProjectID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取消息记录分页信息", tenderProjectID, data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = data.getRecord();
		param.setColumn("tpid", tenderProjectID);
		param.setColumn("currentUserID", ApacheShiroUtils.getCurrentUserID());
		return this.activeRecordDAO
				.auto()
				.table(ConstantIM.TableName.IM_MSG)
				.setCondition("AND", "N_MESSAGE_TYPE = 0")
				.setCondition(
						"AND",
						"(V_SENDER_ID = #{currentUserID} OR V_RECIPIENT_ID = #{currentUserID} OR V_RECIPIENT_ID IS NULL OR V_RECIPIENT_ID = '')")
				.setCondition(
						"AND",
						"V_ROOM_ID IN (SELECT ID FROM "
								+ ConstantIM.TableName.IM_ROOM
								+ " WHERE V_TPID = #{tpid})")
				.addSortOrder("N_SEND_TIME", "DESC").page(pageable, param);
	}
}
