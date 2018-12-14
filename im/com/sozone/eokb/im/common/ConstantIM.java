/**
 * 包名：com.sozone.eokb.im.common
 * 文件名：ConstantIM.java<br/>
 * 创建时间：2017-8-29 下午2:09:48<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.common;

/**
 * 常量定义<br/>
 * Time：2017-8-29 下午2:09:48<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ConstantIM
{
	/**
	 * 表名常量<br/>
	 * Time：2017-8-29 下午2:10:28<br/>
	 * 
	 * @author Administrator
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	public interface TableName
	{
		/**
		 * 即时聊天会议室表
		 */
		String IM_ROOM = "EKB_T_IM_ROOM";

		/**
		 * 即时聊天信息表
		 */
		String IM_MSG = "EKB_T_IM_MSG";

		/**
		 * 即时聊天日志表
		 */
		String IM_LOG = "EKB_T_IM_LOG";

		/**
		 * 空
		 * 
		 * @return 空
		 */
		String toString();
	}

	/**
	 * 空
	 * 
	 * @return 空
	 */
	String toString();
}
