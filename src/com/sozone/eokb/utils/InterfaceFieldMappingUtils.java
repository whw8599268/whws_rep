package com.sozone.eokb.utils;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jxl.Sheet;
import jxl.Workbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.util.ClassLoaderUtils;
import com.sozone.aeolus.util.CollectionUtils;

/**
 * 
 * 接口字段处理工具类<br/>
 * <p>
 * 接口字段处理工具类<br/>
 * </p>
 * Time：2017-8-30 上午9:23:57<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
public class InterfaceFieldMappingUtils
{

	private static Logger logger = LoggerFactory
			.getLogger(InterfaceFieldMappingUtils.class);
	//本地字段接口字段对应缓存
	private static Map<String, List<Record<String, String>>> interfaceDataRecord = null;

	/**
	 * 
	 * 接口字段转成本地字段<br/>
	 * <p>
	 * 接口字段转成本地字段
	 * </p>
	 * 
	 * @param paramValue 待转换Map
	 * @param tableName
	 * @return
	 */
	public static Record<String, Object> FiledlocalToInterFace(
			Record<String, Object> paramValue, String tableName)
	{
		//转换后的Map
		Record<String, Object> result = new RecordImpl<String, Object>();
		//本地字段接口字段映射List
		List<Record<String, String>> fieldMappingList = exportConfigInterfaceData(tableName);
		Record<String, String> record = null;
		for (int i = 0; i < fieldMappingList.size(); i++)
		{
			record = fieldMappingList.get(i);
			String interFaceFiledName = record.getString("INTERFACENAME");
			String localFiledName = record.getString("LOCALNAME");
			result.setColumn(localFiledName,
					paramValue.getString(interFaceFiledName));
		}
		return result;
	}

	/**
	 * 
	 * @return 映射excel清单
	 */
	private static List<Record<String, String>> exportConfigInterfaceData(
			String tableName)
	{
		if (CollectionUtils.isEmpty(interfaceDataRecord))
		{
			interfaceDataRecord = new RecordImpl<String, List<Record<String, String>>>();
		}
		if (interfaceDataRecord.containsKey(tableName))
		{
			return interfaceDataRecord.get(tableName);
		}
		Workbook rwb = null;
		List<Record<String, String>> list = new ArrayList<Record<String, String>>();
		try
		{
			rwb = Workbook.getWorkbook(getConfigFile(tableName));
			Sheet[] rss = rwb.getSheets();
			for (Sheet rs : rss)
			{
				int rows = rs.getRows();// 得到所有的行

				// 遍历每行每列的单元格
				for (int i = 1; i < rows; i++)
				{
					//本地数据库字段名称
					String localName = rs.getCell(0, i).getContents().trim();
					//接口字段名称
					String interfaceName = rs.getCell(1, i).getContents()
							.trim();
					//字段注释
					String annotation = rs.getCell(2, i).getContents().trim();
					Record<String, String> table = new RecordImpl<String, String>();
					table.put("LOCALNAME", localName);
					table.put("INTERFACENAME", interfaceName);
					table.put("ANNOTATION", annotation);
					list.add(table);
				}
			}
			interfaceDataRecord.put(tableName, list);
		}
		catch (Exception e)
		{
			logger.error("导出数据表单失败", e);
		}
		finally
		{
			if (rwb != null)
			{
				rwb.close();
			}
		}
		return list;
	}

	/**
	 * @return 获取接口与本地数据字段映射关系文件
	 */
	private static File getConfigFile(String tableName)
	{
		String xsdPath = "com/sozone/eokb/bus/excel/" + tableName + ".xls";
		URL url = ClassLoaderUtils.getResource(xsdPath,
				InterfaceFieldMappingUtils.class);
		if (null != url)
		{
			try
			{
				return new File(url.toURI());
			}
			catch (URISyntaxException e)
			{
				return null;
			}
		}
		return null;
	}
}