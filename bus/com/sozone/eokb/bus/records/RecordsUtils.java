package com.sozone.eokb.bus.records;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jxl.Sheet;
import jxl.Workbook;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.util.ClassLoaderUtils;
import com.sozone.aeolus.util.CollectionUtils;

/**
 * 
 * 开标记录列表处理工具类<br/>
 * <p>
 * 开标记录列表处理工具类<br/>
 * </p>
 * Time：2017-8-30 上午9:23:57<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
public class RecordsUtils
{

	private static Logger logger = LoggerFactory.getLogger(RecordsUtils.class);

	/**
	 * 
	 * 缓存
	 */
	private static Record<String, Object> recordConfig = null;

	/**
	 * 
	 * @return 获取开标记录配置信息
	 */
	public static Record<String, Object> recordsConfigInfo()
	{
		if (CollectionUtils.isEmpty(recordConfig))
		{
			recordConfig = new RecordImpl<String, Object>();
		}
		else
		{
			return recordConfig;
		}
		Workbook rwb = null;
		// 评标办法对应开标记录列表
		List<Record<String, String>> methodsList = null;
		try
		{
			rwb = Workbook.getWorkbook(getConfigFile());
			Sheet[] rss = rwb.getSheets();
			String tenderProjectTypeTemp = "";
			for (Sheet rs : rss)
			{
				int rows = rs.getRows();// 得到所有的行
				// 遍历每行每列的单元格
				for (int i = 1; i < rows; i++)
				{
					// 评标办法类型
					String tenderProjectType = rs.getCell(1, i).getContents();
					// 处理合并单元格只有在第一行有获取到问题
					if (StringUtils.isEmpty(tenderProjectType))
					{
						tenderProjectType = tenderProjectTypeTemp;
					}
					else
					{
						tenderProjectTypeTemp = tenderProjectType;
					}
					// 开标记录表名称
					String comment = rs.getCell(2, i).getContents().trim();
					// URL
					String url = rs.getCell(3, i).getContents().trim();
					// 文件名称
					String fileName = rs.getCell(4, i).getContents().trim();
					// 获取对应评标办法的开标记录列表
					if (recordConfig.containsKey(tenderProjectType))
					{
						methodsList = (List<Record<String, String>>) recordConfig
								.get(tenderProjectType);
					}
					else
					{
						methodsList = new ArrayList<Record<String, String>>();
					}
					Record<String, String> record = new RecordImpl<String, String>();
					record.put("COMMENT", comment);
					record.put("URL", url);
					record.put("FILE_NAME", fileName);
					methodsList.add(record);
					recordConfig.setColumn(tenderProjectType, methodsList);
				}
			}
			return recordConfig;
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
		return recordConfig;
	}

	/**
	 * @return 获取开标设置配置信息
	 */
	private static File getConfigFile()
	{
		String xsdPath = "com/sozone/eokb/bus/records/records.xls";
		URL url = ClassLoaderUtils.getResource(xsdPath, RecordsUtils.class);
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

	public static void main(String[] args)
	{
		Record<String, Object> record = RecordsUtils.recordsConfigInfo();
		System.out.println(record);
	}
}
