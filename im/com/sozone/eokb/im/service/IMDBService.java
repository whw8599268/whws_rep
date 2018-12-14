/**
 * 包名：com.sozone.eokb.im.service
 * 文件名：IMDBService.java<br/>
 * 创建时间：2017-8-29 下午2:00:15<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.im.common.ConstantIM.TableName;
import com.sozone.eokb.im.utils.IMUtils;

/**
 * 即时聊天数据库服务<br/>
 * Time：2017-8-29 下午2:00:15<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
public class IMDBService
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(IMDBService.class);

	/**
	 * 保存即时聊天的信息<br/>
	 * 
	 * @param roomID
	 *            会议室ID
	 * @param msg
	 *            信息
	 */
	public void saveMsg(String roomID, Record<String, Object> msg)
	{
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("ID", Random.generateUUID());
		record.setColumn("V_ROOM_ID", roomID);
		record.setColumn("V_SENDER_ID", msg.get("SENDER_ID"));
		record.setColumn("V_SENDER_NAME", msg.get("SENDER_NAME"));
		record.setColumn("V_SENDER_LOGO", msg.get("SENDER_LOGO"));
		record.setColumn("V_RECIPIENT_ID", msg.get("RECIPIENT_ID"));
		record.setColumn("V_RECIPIENT_NAME", msg.get("RECIPIENT_NAME"));
		record.setColumn("V_MESSAGE_CONTENT", msg.get("MESSAGE_CONTENT"));
		record.setColumn("N_MESSAGE_TYPE", msg.get("MESSAGE_TYPE"));
		record.setColumn("N_SEND_TIME", msg.get("SEND_TIME"));
		IMUtils.saveIMInfo(TableName.IM_MSG, record);
	}

	/**
	 * 
	 * <br/>
	 * 
	 * @param roomID
	 *            聊天室ID
	 * @param tenderProjectID
	 *            招标项目ID
	 * @param tendererID
	 *            室主ID/招标人ID
	 */
	public void saveRoomInfo(String roomID, String tenderProjectID,
			String tendererID)
	{
		logger.debug(LogUtils.format("保存即时聊天 聊天室信息!", roomID, tenderProjectID,
				tendererID));

		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("ID", roomID);
		record.setColumn("V_TPID", tenderProjectID);
		record.setColumn("V_ROOMER_ID", tendererID);
		record.setColumn("V_OPEN_TIME",
				DateUtils.getDate("yyyy-MM-dd HH:mm:ss"));
		record.setColumn("N_CRAET_TIME", System.currentTimeMillis());
		record.setColumn("V_CREATE_USER", tendererID);

		IMUtils.saveIMInfo(TableName.IM_ROOM, record);
	}

	/**
	 * 
	 * 更新聊天室关闭时间<br/>
	 * 
	 * @param roomID
	 *            聊天室ID
	 */
	public void modifyRoomCloseTime(String roomID)
	{
		logger.debug(LogUtils.format("更新聊天室关闭时间!", roomID));

		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("ID", roomID);
		record.setColumn("V_CLOSE_TIME",
				DateUtils.getDate("yyyy-MM-dd HH:mm:ss"));
		IMUtils.modifyIMInfo(TableName.IM_ROOM, record);
	}

}
