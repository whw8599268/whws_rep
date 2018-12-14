/**
 * 包名：com.sozone.eokb.im.message
 * 文件名：MessageFactory.java<br/>
 * 创建时间：2017-8-16 下午4:36:04<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.message;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang.time.DateFormatUtils;

import com.alibaba.fastjson.JSON;
import com.sozone.aeolus.authorize.common.Constant;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.eokb.im.pojo.Participant;
import com.sozone.eokb.im.pojo.Tenderer;

/**
 * 消息工厂类<br/>
 * <p>
 * </p>
 * Time：2017-8-16 下午4:36:04<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MessageFactory
{

	private MessageFactory()
	{
	}

	/**
	 * 创建消息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param send
	 *            发送人
	 * @param message
	 *            消息
	 * @return 对象
	 */
	public static Record<String, Object> createMessge(Participant send,
			Map<String, Object> message)
	{
		Record<String, Object> result = new RecordImpl<String, Object>();
		if (null != send)
		{
			result.setColumn("SENDER_ID", send.getUserID());
			result.setColumn("SENDER_NAME", send.getUserName());
			result.setColumn("SENDER_LOGO", send.getUserLogo());
		}
		if (null != message)
		{
			result.putAll(message);
		}

		Object msgContent = message.get("MESSAGE_CONTENT");
		if (null != msgContent && msgContent instanceof String)
		{
			Map<Object, Object> params = new HashMap<Object, Object>();
			// 日期
			params.put("date", DateFormatUtils.format(new Date(),
					Constant.DATE_YYYY_MM_DD_HH_MM_SS));
			params.putAll(SystemParamUtils.getProperties());
			// 表达式解析器,创建一个#{}的表达式解析器
			StrSubstitutor strs = new StrSubstitutor(params, "#{", "}");
			msgContent = strs.replace(msgContent);
			result.setColumn("MESSAGE_CONTENT", msgContent);
		}

		Integer integer = result.getInteger("MESSAGE_TYPE");
		if (null == integer)
		{
			result.setColumn("MESSAGE_TYPE", 0);
		}
		if (send instanceof Tenderer)
		{
			result.setColumn("IS_TENDERER", true);
		}
		else
		{
			result.setColumn("IS_TENDERER", false);
		}
		result.setColumn("SEND_TIME", System.currentTimeMillis());
		return result;
	}

	/**
	 * 创建消息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param send
	 *            发送人
	 * @param recipientID
	 *            接收人ID
	 * @param recipientName
	 *            接收人名称
	 * @param message
	 *            消息
	 * @param type
	 *            类型
	 * @return 对象
	 */
	public static Record<String, Object> createMessge(Participant send,
			String recipientID, String recipientName, Object message, int type)
	{
		Record<String, Object> result = new RecordImpl<String, Object>();
		if (null != send)
		{
			result.setColumn("SENDER_ID", send.getUserID());
			result.setColumn("SENDER_NAME", send.getUserName());
			result.setColumn("SENDER_LOGO", send.getUserLogo());
		}
		result.setColumn("RECIPIENT_ID", recipientID);
		result.setColumn("RECIPIENT_NAME", recipientName);

		if (null != message && message instanceof String)
		{
			Map<Object, Object> params = new HashMap<Object, Object>();
			// 日期
			params.put("date", DateFormatUtils.format(new Date(),
					Constant.DATE_YYYY_MM_DD_HH_MM_SS));
			params.putAll(SystemParamUtils.getProperties());
			// 表达式解析器,创建一个#{}的表达式解析器
			StrSubstitutor strs = new StrSubstitutor(params, "#{", "}");
			message = strs.replace(message);
		}
		result.setColumn("MESSAGE_CONTENT", message);
		result.setColumn("MESSAGE_TYPE", type);
		result.setColumn("SEND_TIME", System.currentTimeMillis());
		if (send instanceof Tenderer)
		{
			result.setColumn("IS_TENDERER", true);
		}
		else
		{
			result.setColumn("IS_TENDERER", false);
		}
		result.setColumn("SEND_TIME", System.currentTimeMillis());
		return result;
	}

	/**
	 * 创建公告<br/>
	 * <p>
	 * </p>
	 * 
	 * @param message
	 *            消息内容
	 * @return 消息对象
	 */
	public static Record<String, Object> createNotice(Object message)
	{
		return createMessge(null, null, null, message, -1);
	}

	/**
	 * 创建命令<br/>
	 * <p>
	 * </p>
	 * 
	 * @param current
	 *            当前参会人
	 * @param command
	 *            消息内容
	 * @return 命令对象
	 */
	public static Record<String, Object> createCommand(Participant current,
			Object command)
	{
		return createCommand(null, null, null, command);
	}

	/**
	 * 创建移动端消息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param current
	 *            当前参会人
	 * @param command
	 *            消息内容
	 * @return 命令对象
	 */
	public static Record<String, Object> createMobileCommand(
			Participant current, Object command)
	{
		return createMessge(null, null, null, JSON.toJSONString(command), 100);
	}

	/**
	 * 
	 * 创建命令<br/>
	 * 
	 * @param current
	 *            当前参会人
	 * @param recipientID
	 *            接收人ID
	 * @param recipientName
	 *            接收人名字
	 * @param command
	 *            消息内容
	 * @return 命令对象
	 */
	public static Record<String, Object> createCommand(Participant current,
			String recipientID, String recipientName, Object command)
	{
		return createMessge(null, recipientID, recipientName,
				JSON.toJSONString(command), 1);
	}

}
