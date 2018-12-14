/**
 * 包名：com.sozone.eokb.im.pool
 * 文件名：BidOpeningMeetingRoomPoolUtils.java<br/>
 * 创建时间：2017-8-16 下午5:24:43<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.pool;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.eokb.im.pojo.BidOpeningMeetingRoom;
import com.sozone.eokb.im.utils.IMUtils;

/**
 * 开标会议室池工具类<br/>
 * <p>
 * 开标会议室池工具类<br/>
 * </p>
 * Time：2017-8-16 下午5:24:43<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public final class BidOpeningMeetingRoomPoolUtils
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(BidOpeningMeetingRoomPoolUtils.class);

	/**
	 * 招标项目ID与开标会议室对应关系图
	 */
	private static final ConcurrentHashMap<String, BidOpeningMeetingRoom> ROOMS = new ConcurrentHashMap<String, BidOpeningMeetingRoom>();

	/**
	 * 获取招标项目ID对应的开标会议室对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @return 开标会议室对象
	 */
	public static BidOpeningMeetingRoom getTenderProjectMeetingRoom(
			String tenderProjectID)
	{
		return ROOMS.get(tenderProjectID);
	}

	/**
	 * 
	 * 获取会议室信息列表<br/>
	 * 
	 * @return 获取会议室信息列表
	 */
	public static ConcurrentHashMap<String, BidOpeningMeetingRoom> getRooms()
	{
		return ROOMS;
	}

	/**
	 * 开启一个新的开标会议室对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @param room
	 *            开标会议室对象
	 */
	public static void openMeetingRoom(String tenderProjectID,
			BidOpeningMeetingRoom room)
	{
		boolean tenderer = IMUtils.isTenderer(tenderProjectID);
		if (tenderer)
		{
			BidOpeningMeetingRoom r = ROOMS.get(tenderProjectID);
			if (null == r)
			{
				ROOMS.put(tenderProjectID, room);
				return;
			}
		}
		else
		{
			logger.error(LogUtils.format("非招标人/招标代理不允许关闭开标会议室!"));
			throw new UnsupportedOperationException("非招标人/招标代理不允许关闭开标会议室!");
		}
	}

	/**
	 * 关闭一个开标会议室<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 */
	public static void closeMeetingRoom(String tenderProjectID)
	{
		boolean admin = ApacheShiroUtils.isAdmin();
		boolean tenderer = IMUtils.isTenderer(tenderProjectID);
		if (admin || tenderer)
		{
			BidOpeningMeetingRoom room = ROOMS.get(tenderProjectID);
			if (null != room)
			{
				room.close();
				ROOMS.remove(tenderProjectID);
			}
		}
		else
		{
			logger.error(LogUtils.format("非管理员和招标人/招标代理不允许关闭开标会议室!"));
			throw new UnsupportedOperationException("非管理员和招标人/招标代理不允许关闭开标会议室!");
		}
	}

	/**
	 * 
	 * 判断当前登录人是否能进入会议室<br/>
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @return 值
	 */
	public static boolean canAccessMeetingRoom(String tenderProjectID)
	{
		Record<String, Object> currentUser = null;
		try
		{
			currentUser = ApacheShiroUtils.getCurrentUser();
		}
		catch (ValidateException e)
		{
			logger.error(LogUtils.format("当前用户尚未登录不允许进入开标会议室!", e));
			throw new UnsupportedOperationException("当前用户尚未登录不允许进入开标会议室!");
		}

		if (StringUtils.isEmpty(tenderProjectID))
		{
			logger.error(LogUtils.format("无法获取招标项目ID!"));
			throw new UnsupportedOperationException("无法获取招标项目ID!");
		}
		boolean tendererOrBidder = IMUtils.isTendererOrBidder(tenderProjectID);
		if (!tendererOrBidder)
		{
			throw new UnsupportedOperationException(
					"当前用户(不是招标人/招标代理或投标人)不允许进入开标会议室!");
		}
		// 开标会议室
		BidOpeningMeetingRoom room = BidOpeningMeetingRoomPoolUtils
				.getTenderProjectMeetingRoom(tenderProjectID);

		// 重复登录
		if (null != room
				&& room.hasParticipant(currentUser.getString("USER_ID")))
		{
			logger.error(LogUtils.format("重复登录的用户!"));
			throw new UnsupportedOperationException("该账号已经登录到开标会议室,无法重复登录!");
		}
		// ----------
		boolean bidder = IMUtils.isBidder(tenderProjectID);
		// 如果当前人是投标人
		if (bidder)
		{
			// 并且会议室不存在
			if (null == room)
			{
				logger.error(LogUtils.format("开标会议尚未开始!"));
				throw new UnsupportedOperationException("开标会议尚未开始!");
			}
			// 下面这种情况不能开启,怕会出现一种情况,招标人中途刷新或者突然断线
			// // 如果招标人已经离开
			// if (null == room.getTenderer())
			// {
			// logger.error(LogUtils.format("招标人/招标代理不在会议室!"));
			// throw new UnsupportedOperationException("招标人/招标代理不在会议室!");
			// }
		}
		return true;
	}

}
