/**
 * 包名：com.sozone.eokb.flow
 * 文件名：TreeService.java<br/>
 * 创建时间：2017-8-21 下午4:06:27<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.flow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.util.CollectionUtils;

/**
 * 树形结构服务类<br/>
 * <p>
 * 树形结构服务类<br/>
 * </p>
 * Time：2017-8-21 下午4:06:27<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class TreeService
{

	/**
	 * 获取树形数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param nodes
	 *            按照深度和序号升序排列的节点列表
	 * @param idKey
	 *            ID KEY
	 * @param textName
	 *            节点标签名称
	 * @return 树形根节点列表
	 */
	public static List<Record<String, Object>> getTreeList(
			List<Record<String, Object>> nodes, String idKey, String textName)
	{
		List<Record<String, Object>> roots = new LinkedList<Record<String, Object>>();
		if (CollectionUtils.isEmpty(nodes))
		{
			return roots;
		}
		Map<String, Record<String, Object>> idMap = new HashMap<String, Record<String, Object>>();
		Record<String, Object> parent;
		List<Record<String, Object>> children = null;
		for (Record<String, Object> node : nodes)
		{
			idMap.put(node.getString(idKey), node);
			node.setColumn("id", node.getString("ID"));
			node.setColumn("text", node.getString(textName));
			String pid = node.getString("V_PID");
			parent = idMap.get(pid);
			// 设置条款序列号
			node.setColumn("SEQUENCE_NUM",
					getSequenceNum(node.getInteger("N_INDEX"), parent));
			// 如果是顶层节点
			if (null == parent)
			{
				roots.add(node);
				continue;
			}
			children = parent.getList("children");
			if (null == children)
			{
				children = new LinkedList<Record<String, Object>>();
			}
			children.add(node);
			parent.setColumn("children", children);
		}
		return roots;
	}

	/**
	 * 生成条款序号<br/>
	 * <p>
	 * </p>
	 * 
	 * @param serial
	 * @param parent
	 * @return
	 */
	private static String getSequenceNum(int serial,
			Record<String, Object> parent)
	{

		if (CollectionUtils.isEmpty(parent))
		{
			return "1." + serial;
		}
		return parent.getString("SEQUENCE_NUM") + "." + serial;
	}

}
