/**
 * 包名：com.sozone.eokb.im.servlet
 * 文件名：BidOpeningMeetingIMServlet.java<br/>
 * 创建时间：2017-8-16 下午5:21:17<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.servlet;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.eokb.im.pojo.BidOpeningMeetingRoom;
import com.sozone.eokb.im.pojo.Bidder;
import com.sozone.eokb.im.pojo.Tenderer;
import com.sozone.eokb.im.pool.BidOpeningMeetingRoomPoolUtils;
import com.sozone.eokb.im.utils.IMUtils;

/**
 * 开标会议即时聊天Servlet<br/>
 * <p>
 * 开标会议即时聊天Servlet<br/>
 * </p>
 * Time：2017-8-16 下午5:21:17<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class BidOpeningMeetingIMServlet extends WebSocketServlet
{

	/**
	 * 序列化版本号
	 */
	private static final long serialVersionUID = 8619950227298746510L;

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(BidOpeningMeetingIMServlet.class);

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
		logger.debug(LogUtils.format("一个新的用户进入开标会议室", subProtocol, request));
		Record<String, Object> currentUser = null;
		try
		{
			currentUser = ApacheShiroUtils.getCurrentUser();
		}
		catch (ValidateException e)
		{
			logger.error(LogUtils.format("当前用户尚未登录不允许进入开标会议室!", subProtocol,
					request), e);
			throw new UnsupportedOperationException("当前用户尚未登录不允许进入开标会议室!");
		}

		// 获取招标项目ID
		String tenderProjectID = request.getParameter("tpid");

		if (StringUtils.isEmpty(tenderProjectID))
		{
			logger.error(LogUtils.format("无法获取招标项目ID!", subProtocol, request));
			throw new UnsupportedOperationException("无法获取招标项目ID!");
		}

		boolean tendererFlag = IMUtils.isTenderer(tenderProjectID);

		// 开标会议室
		BidOpeningMeetingRoom room = BidOpeningMeetingRoomPoolUtils
				.getTenderProjectMeetingRoom(tenderProjectID);

		// 重复登录
		if (null != room
				&& room.hasParticipant(currentUser.getString("USER_ID")))
		{
			logger.error(LogUtils.format("重复登录的用户!", subProtocol, request));
			throw new UnsupportedOperationException("该账号已经登录到开标会议室,无法重复登录!");
		}

		// 如果是招标人
		if (tendererFlag)
		{
			Tenderer tenderer = new Tenderer(request);
			// 如果会议室不存在
			if (null == room)
			{
				room = new BidOpeningMeetingRoom(tenderProjectID, tenderer);
				tenderer.setBidOpeningMeetingRoom(room);
				// 开启一个新的会议室
				BidOpeningMeetingRoomPoolUtils.openMeetingRoom(tenderProjectID,
						room);
				return tenderer;
			}
			// 如果会议室存在
			room.setTenderer(tenderer);
			tenderer.setBidOpeningMeetingRoom(room);
			return tenderer;
		}
		// 如果是招标人
		// 如果会议室不存在
		if (null == room)
		{
			logger.error(LogUtils.format("开标会议尚未开始!", subProtocol, request));
			throw new UnsupportedOperationException("开标会议尚未开始!");
		}
		Bidder bidder = new Bidder(request, room);
		return bidder;
	}
}
