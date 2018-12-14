/**
 * 包名：com.sozone.eokb.bus.bidcheck
 * 文件名：BidCheckUtils.java<br/>
 * 创建时间：2018-6-7 下午6:06:16<br/>
 * 创建者：wengdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.bidcheck;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.utils.HttpClientUtils;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.utils.TenderProjectParamUtils;

/**
 * 开标环境检测工具类<br/>
 * <p>
 * 开标环境检测工具类<br/>
 * </p>
 * Time：2018-6-7 下午6:06:16<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
public class BidCheckUtils
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(BidCheckUtils.class);

	/**
	 * 招标文件类型
	 */
	private static String biddingDocumenType = ".szb";

	/**
	 * 补遗目录
	 */
	private static String addendumDirName = "答疑";

	/**
	 * 补遗文件json
	 */
	private static String addendumFileName = "补遗文件描述.json";
	/**
	 * 控制价目录
	 */
	private static String controlPriceDirName = "控制价";

	/**
	 * 控制价文件json
	 */
	private static String controlPriceFileName = "控制价文件描述.json";

	/**
	 * 开标文件存放盘符
	 */
	private static String rootPath = "D:/";

	/**
	 * 
	 * 检查招标文件<br/>
	 * <p>
	 * 检查招标文件
	 * </p>
	 * 
	 * @param kbRootPath
	 *            文件存放根路径
	 * @param projectCode
	 *            招标项目编号
	 * @return 检测结果
	 */
	public static boolean checkBiddingDocuments(String kbRootPath,
			String projectCode)
	{
		logger.debug(LogUtils.format("检查招标文件", kbRootPath, projectCode));

		// 找到招标文件所在的目录
		kbRootPath = kbRootPath + projectCode + File.separator + projectCode
				+ biddingDocumenType;

		File file = new File(kbRootPath);
		if (file.exists())
		{
			return true;
		}
		return false;
	}

	/**
	 * 
	 * 检查补遗文件<br/>
	 * <p>
	 * <font style="color:red;">补遗文件有可能没有，在有《补遗文件描述.json》
	 * 的情况下且json里面存在文件路径的情况下校验文件是否存在</font>
	 * </p>
	 * 
	 * @param kbRootPath
	 *            文件存放根路径
	 * @param projectCode
	 *            招标项目code
	 * @throws ServiceException
	 *             ServiceException
	 * @return 检测结果
	 */
	public static boolean checkAddendumFile(String kbRootPath,
			String projectCode) throws ServiceException
	{
		logger.debug(LogUtils.format("检查补遗文件", kbRootPath, projectCode));
		String filePath = kbRootPath + projectCode + File.separator
				+ addendumDirName + File.separator + addendumFileName;

		File file = new File(filePath);

		if (file.exists())
		{
			try
			{
				String json = FileUtils.readFileToString(file,
						ConstantEOKB.DEFAULT_CHARSET);
				JSONArray jarr = JSON.parseArray(json);
				// 如果jarr不为空，说明有文件
				if (!CollectionUtils.isEmpty(jarr))
				{
					checkFile(jarr);
				}
			}
			catch (IOException e)
			{
				return false;
			}

		}
		return true;
	}

	/**
	 * 
	 * 检查控制价<br/>
	 * <p>
	 * 检查控制价
	 * </p>
	 * 
	 * @param sections
	 *            b标段列表
	 * @return 检测结果
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static boolean checkControlPrice(
			List<Record<String, Object>> sections) throws ServiceException
	{
		logger.debug(LogUtils.format("检查每个标段的控制价", sections));

		// 任意标段的控制价为空都属于异常
		for (Record<String, Object> section : sections)
		{
			if (StringUtils.isEmpty(section.getString("N_CONTROL_PRICE")))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * 检查控制价文件<br/>
	 * <p>
	 * <font style="color:red;">控制价文件有可能没有，在有《控制价文件描述.json》
	 * 的情况下且json里面存在文件路径的情况下校验文件是否存在</font>
	 * </p>
	 * 
	 * @param kbRootPath
	 *            开标文件存放路径
	 * @param projectCode
	 *            招标项目编号
	 * @throws ServiceException
	 *             ServiceException
	 * @return 检测结果
	 */
	public static boolean checkControlPriceFile(String kbRootPath,
			String projectCode) throws ServiceException
	{
		logger.debug(LogUtils.format("检查控制价文件", kbRootPath, projectCode));
		String filePath = kbRootPath + projectCode + File.separator
				+ controlPriceDirName + File.separator + controlPriceFileName;
		File priceJson = new File(filePath);
		// 控制价文件存在的情况下
		if (priceJson.exists())
		{
			try
			{
				String json = FileUtils.readFileToString(priceJson,
						ConstantEOKB.DEFAULT_CHARSET);
				JSONArray jarr = JSON.parseArray(json);
				// 如果jarr不为空，说明有文件
				if (!CollectionUtils.isEmpty(jarr))
				{
					checkFile(jarr);
				}
			}
			catch (IOException e)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * 根据文件描述JSON文件校验文件是否存在<br/>
	 * <p>
	 * 根据文件描述JSON文件校验文件是否存在
	 * </p>
	 * 
	 * @param jarr
	 *            文件描述信息集
	 * @return
	 */
	private static void checkFile(JSONArray jarr) throws ServiceException
	{
		logger.debug(LogUtils.format("根据文件描述JSON文件校验文件是否存在", jarr));

		JSONObject jobj;
		// 文件路径
		String filePath;
		for (int i = 0; i < jarr.size(); i++)
		{
			jobj = jarr.getJSONObject(i);
			if (CollectionUtils.isEmpty(jobj))
			{
				continue;
			}
			filePath = jobj.getString("FILE_PATH");
			if (StringUtils.isBlank(filePath))
			{
				continue;
			}
			// 文件路径存在，校验文件
			if (!new File(filePath).exists())
			{
				logger.debug(LogUtils.format("校验文件-文件不存在", filePath));
				// 文件没有同步到
				throw new ServiceException("", "文件不存在");
			}
		}
	}

	/**
	 * 
	 * 检查投标文件<br/>
	 * <p>
	 * 检查投标文件
	 * </p>
	 * 
	 * @param projectInfo
	 *            项目信息
	 * @param tbRootpath
	 *            投递文件存放根路径
	 * @param inviteno
	 *            招标编号
	 * @param sections
	 *            标段列表
	 * @return 检测结果
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static boolean checkTbFiles(Record<String, Object> projectInfo,
			String tbRootpath, String inviteno,
			List<Record<String, Object>> sections) throws ServiceException
	{
		logger.debug(LogUtils.format("检查每个标段投标文件", inviteno, sections));
		// 请求参数
		Map<String, String> param = new HashMap<String, String>();

		// 招标项目主键
		param.put("TENDERPROJECTID",
				projectInfo.getString("V_TENDER_PROJECT_ID"));
		// 如果有扩展信息
		String json = projectInfo.getString("V_JSON_OBJ");
		if (StringUtils.isEmpty(json))
		{
			param.put("REVIEWSORT", 2 + "");
		}
		else
		{
			JSONObject ext = JSON.parseObject(json);
			param.put("REVIEWSORT", ext.getString("N_BID_ORDER"));
		}

		// 判断是否是房建市政
		String keyPrefix = "";
		if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE,
				projectInfo.getString("V_TENDERPROJECT_APP_TYPE"))
				|| StringUtils.equals(
						ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE,
						projectInfo.getString("V_TENDERPROJECT_APP_TYPE")))
		{
			keyPrefix = "xm.fjsz.";
			param.put("SECTIONID", projectInfo.getString("V_BID_SECTION_ID"));
		}
		String paramKey = SysParamKey.EDE_ENTBID_COUNT_URL;
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("type", keyPrefix);
		StrSubstitutor strs = new StrSubstitutor(params);
		paramKey = strs.replace(paramKey);

		// 获取头部Token
		Record<String, String> headMap = HttpClientUtils.getHeadMapOfToken();
		String url = SystemParamUtils.getString(paramKey);
		// url = "http://test-ebid-api.okap.com/authorize/api/ekb/tbcount";
		try
		{
			json = HttpClientUtils.doGet(url, param, headMap,
					ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (Exception e)
		{
			throw new ServiceException("E-1007", "获取企业投标信息失败!", e);
		}

		logger.error(LogUtils.format("响应结果", json));
		JSONObject result = JSON.parseObject(json);
		boolean success = result.getBoolean("success");
		if (!success)
		{
			throw new ServiceException(result.getString("errorCode"),
					result.getString("errorDesc"));
		}
		result = result.getJSONObject("result");
		int count = result.getInteger("num");

		// 文件路径又招标项目编号+标段名称
		String fileDir;
		File dir;
		int fileCount = 0;
		for (Record<String, Object> section : sections)
		{
			// 0：投递文件保存使用的文件夹命名规则是： 招标编号+标段名称
			// 1：投递文件保存使用的文件夹明明规则是： 标段编号
			if (0 == section.getInteger("N_TB_NEW_DIC"))
			{
				fileDir = tbRootpath
						+ inviteno
						+ "-"
						+ getDirName(projectInfo,
								section.getString("V_BID_SECTION_NAME"));
			}
			else
			{
				fileDir = tbRootpath
						+ getDirName(projectInfo,
								section.getString("V_BID_SECTION_CODE"));
			}

			dir = new File(fileDir);
			// logger.error(LogUtils.format("fileDir:", fileDir));
			// 先判断是否是个文件夹
			if (!dir.isDirectory())
			{
				return false;

			}
			// logger.error(LogUtils.format("dir.list():", dir.list().length));
			fileCount += dir.list().length;
		}
		// logger.error(LogUtils.format("count:", count));
		// logger.error(LogUtils.format("fileCount:", fileCount));

		return count == fileCount;
	}

	/**
	 * 
	 * 获取目录名<br/>
	 * <p>
	 * 如果是预审需要加上"-预审"
	 * </p>
	 * 
	 * @param projectInfo
	 * @param sectionName
	 * @return
	 */
	public static String getDirName(Record<String, Object> projectInfo,
			String sectionName)
	{
		JSONObject jobj = projectInfo.getJSONObject("V_JSON_OBJ");
		if (CollectionUtils.isEmpty(jobj))
		{
			return sectionName;
		}

		Boolean pretrial = jobj.getBoolean("IS_PRETRIAL");
		if (null != pretrial && pretrial)
		{
			sectionName = sectionName + "-预审";
		}
		return sectionName;
	}

	/**
	 * 
	 * 获取硬盘剩余空间（单位G）<br/>
	 * <p>
	 * 获取硬盘剩余空间（单位G）
	 * </p>
	 * 
	 * @return 磁盘可用空间大小
	 */
	public static long checkDiskFreeSpace()
	{
		logger.debug(LogUtils.format("获取硬盘剩余空间（单位G）"));
		File file = new File(rootPath);
		long freeSpace = file.getFreeSpace();
		return freeSpace / 1024 / 1024 / 1024;
	}

	/**
	 * 
	 * 检查交易平台URL连接是否正常<br/>
	 * <p>
	 * 检查交易平台URL连接是否正常
	 * </p>
	 * 
	 * @param appType
	 *            招标项目类型
	 * @return 检测结果
	 */
	public static boolean checkEdeUrl(String appType)
	{
		logger.debug(LogUtils.format("检查交易平台URL连接是否正常"));

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("KEY", SysParamKey.PUSH_BIDDERS_URL);
		param.setColumn("TYPE", appType);

		// 例：http://ebid-data-exchange-fjjt-test.okap.com/authorize/ekb/subjectList
		String url = TenderProjectParamUtils.getSystemParamValue(param);
		String[] strs = url.split("authorize");
		return checkUrl(strs[0]);
	}

	/**
	 * 
	 * 检查视频地址是否可用<br/>
	 * <p>
	 * 检查视频地址是否可用
	 * </p>
	 * 
	 * @return 视频地址检测结果
	 */
	public static boolean checkEovUrl()
	{
		logger.debug(LogUtils.format("检查视频工程URL连接是否正常"));
		String url = SystemParamUtils.getString(SysParamKey.EOV_VIDEO_JSP);
		return checkUrl(url);
	}

	/**
	 * 
	 * 检测cdn地址是否可用<br/>
	 * <p>
	 * 检测cdn地址是否可用
	 * </p>
	 * 
	 * @return cdn地址检测结果
	 */
	public static boolean checkCdnUrl()
	{
		logger.debug(LogUtils.format("检测cdn地址是否可用"));
		String url = SystemParamUtils.getString(SysParamKey.CDN_VIDEO_URL,
				"http://171.8.242.118/mserver/");
		return checkUrl(url);
	}

	/**
	 * 
	 * 检测录制地址是否可用<br/>
	 * <p>
	 * 检测录制地址是否可用
	 * </p>
	 * 
	 * @return cdn地址检测结果
	 */
	public static boolean checkRecUrl()
	{
		logger.debug(LogUtils.format("检测录制地址是否可用"));
		String url = SystemParamUtils.getString(SysParamKey.REC_VIDEO_URL,
				"http://117.25.161.109:22134/mserver/");
		return checkUrl(url);
	}

	/**
	 * 
	 * 检测地址是否能用<br/>
	 * <p>
	 * 检测地址是否能用
	 * </p>
	 * 
	 * @param urlString
	 *            网址
	 * @return true：可用，false:不可用
	 */
	private static boolean checkUrl(String urlString)
	{
		URL url;
		InputStream in = null;
		try
		{
			url = new URL(urlString);
			in = url.openStream();
			return true;
		}
		catch (Exception e1)
		{
			return false;
		}
		finally
		{
			try
			{
				if (null != in)
				{
					in.close();
				}
			}
			catch (IOException e)
			{
				logger.debug(e.getMessage());
			}
		}
	}
}
