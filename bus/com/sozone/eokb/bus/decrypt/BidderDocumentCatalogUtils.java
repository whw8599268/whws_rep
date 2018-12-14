/**
 * 包名：com.sozone.eokb.common.bus.decrypt
 * 文件名：BidderDocumentCatalogUtils.java<br/>
 * 创建时间：2017-8-29 上午10:24:09<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;

/**
 * 投标文件目录工具类<br/>
 * <p>
 * </p>
 * Time：2017-8-29 上午10:24:09<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
public class BidderDocumentCatalogUtils
{

	/**
	 * 读取招标文件中的TreeViewInfo.xml<br/>
	 * <p>
	 * </p>
	 * 
	 * @param file
	 *            TreeViewInfo.xml文件
	 * @return 目录树形节点
	 * @throws Exception
	 *             异常
	 */
	public static List<Record<String, Object>> readTreeViewXml(File file)
			throws Exception
	{
		SAXReader reader = new SAXReader();
		Document document = reader.read(file);
		// 获取文档的根节点.
		Element root = document.getRootElement();
		List<Record<String, Object>> fileInfos = readElements(root);
		Map<String, Record<String, Object>> idMap = new HashMap<String, Record<String, Object>>();
		Record<String, Object> parent;
		List<Record<String, Object>> children = null;
		List<Record<String, Object>> roots = new LinkedList<Record<String, Object>>();
		File target = null;
		for (Record<String, Object> fi : fileInfos)
		{
			idMap.put(fi.getString("ID"), fi);
			String pid = fi.getString("PID");
			parent = idMap.get(pid);

			// 如果是文件
			if (StringUtils.equals("file", fi.getString("TYPE")))
			{
				target = new File(file.getParentFile(), fi.getString("PATH"));
				fi.setColumn("PATH", FilenameUtils.separatorsToUnix(target
						.getAbsolutePath()));
			}

			// 如果是顶层节点
			if (null == parent)
			{
				roots.add(fi);
				continue;
			}
			children = parent.getList("CHILDREN");
			if (null == children)
			{
				children = new LinkedList<Record<String, Object>>();
			}
			children.add(fi);
			parent.setColumn("CHILDREN", children);
		}

		return roots;
	}

	@SuppressWarnings("unchecked")
	private static List<Record<String, Object>> readElements(Element root)
			throws Exception
	{
		List<Record<String, Object>> eles = new LinkedList<Record<String, Object>>();
		List<Element> elements = root.elements("Table1");
		Record<String, Object> ele = null;
		List<Element> pros = null;
		String key = null;
		String value = null;
		for (Element element : elements)
		{
			ele = new RecordImpl<String, Object>();
			pros = element.elements();
			for (Element pro : pros)
			{
				key = pro.getName();
				value = pro.getText();
				ele.setColumn(key.toUpperCase(), value);
			}
			format(ele);
			eles.add(ele);
		}
		return eles;
	}

	private static void format(Record<String, Object> node)
	{
		node.setColumn("ALLOW_ADD", node.remove("CANADD"));
		node.setColumn("ALLOW_REMOVE", node.remove("CANDEL"));
		node.setColumn("ALLOW_CHANGE", node.remove("CANCHANGE"));
		node.setColumn("ALLOW_IMPORT", node.remove("CANIMPORT"));
		node.setColumn("ORDER_KEY", node.remove("SORTINDEX"));
		node.setColumn("PID", node.remove("PARENTID"));
		node.setColumn("MUST_SEAL", node.remove("NEEDSTAMP"));
		node.setColumn("ALLOW_RENAME", node.remove("CANRENAME"));
		node.setColumn("TYPE", 1 == node.getInteger("FOLDERORFILE") ? "file"
				: "dir");
		String rp = node.getString("REFFILE");
		if (StringUtils.isNotEmpty(rp))
		{
			String ext = FilenameUtils.getExtension(rp);
			String path = node.getString("PATH");
			path = path + "/" + node.getString("NAME") + "." + ext;
			path = FilenameUtils.separatorsToUnix(path);
			node.setColumn("PATH", path);
		}
		node.remove("FOLDERORFILE");
		node.remove("GROUP");
		node.remove("ISDEMO");
		node.remove("REFFILE");

	}

}
