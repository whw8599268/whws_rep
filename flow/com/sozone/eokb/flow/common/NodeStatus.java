/**
 * 包名：com.sozone.eokb.flow.common
 * 文件名：NodeStatus.java<br/>
 * 创建时间：2017-9-7 下午5:32:11<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.flow.common;


/**
 * 流程节点状态<br/>
 * <p>
 * 流程节点状态<br/>
 * </p>
 * Time：2017-9-7 下午5:32:11<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public enum NodeStatus
{

	/**
	 * 未开始
	 */
	NotStarted("未开始", 1),

	/**
	 * 进行中
	 */
	InProgress("进行中", 2),

	/**
	 * 已结束
	 */
	HaveClosed("已结束", 3);

	/**
	 * 名称
	 */
	private String name;

	/**
	 * 状态
	 */
	private int status;

	/**
	 * 构造函数
	 * 
	 * @param name
	 *            名称
	 * @param status
	 *            状态
	 */
	private NodeStatus(String name, int status)
	{
		this.name = name;
		this.status = status;
	}

	/**
	 * name属性的get方法
	 * 
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * status属性的get方法
	 * 
	 * @return the status
	 */
	public int getStatus()
	{
		return status;
	}

	/**
	 * 根据状态值返回枚举值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param status
	 *            状态值
	 * @return 枚举对象
	 */
	public static NodeStatus valueOf(int status)
	{
		for (NodeStatus n : values())
		{
			if (n.status == status)
			{
				return n;
			}
		}
		return null;
	}

}
