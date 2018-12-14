/**
 * 包名：com.sozone.eokb.im.pojo
 * 文件名：Tenderer.java<br/>
 * 创建时间：2017-8-16 下午3:28:53<br/>
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

import javax.servlet.http.HttpServletRequest;

/**
 * 招标人抽象实体<br/>
 * <p>
 * 该类用来描述一个招标人<br/>
 * </p>
 * Time：2017-8-16 下午3:28:53<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class Tenderer extends Participant
{
	/**
	 * 构造函数
	 * 
	 * @param request
	 *            HttpServletRequest请求
	 */
	public Tenderer(HttpServletRequest request)
	{
		super(request);
	}
	
	/**
	 * 构造函数
	 * 
	 * @param request
	 *            HttpServletRequest
	 * @param bidOpeningMeetingRoom
	 *            开标会议室
	 */
	public Tenderer(HttpServletRequest request,
			BidOpeningMeetingRoom bidOpeningMeetingRoom)
	{
		this(request);
		this.bidOpeningMeetingRoom = bidOpeningMeetingRoom;
	}
}
