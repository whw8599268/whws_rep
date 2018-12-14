package com.sozone.eokb.bus.records;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.download.util.AeolusDownloadUtils;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.upload.handler.MultipartFormDataHandler;
import com.sozone.aeolus.util.ClassLoaderUtils;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.LogFormatUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 
 * 开标记录服务类<br/>
 * <p>
 * 开标记录服务类<br/>
 * </p>
 * Time：2017-9-21 下午1:52:24<br/>
 * 
 * @author wanghw
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/records", desc = "开标记录服务类")
// 登录即可访问
@Permission(Level.Authenticated)
public class Records extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Records.class);

	/**
	 * 持久化接口
	 */
	protected ActiveRecordDAO activeRecordDAO = null;

	/**
	 * activeRecordDAO属性的set方法
	 * 
	 * @param activeRecordDAO
	 *            the activeRecordDAO to set
	 */
	public void setActiveRecordDAO(ActiveRecordDAO activeRecordDAO)
	{
		this.activeRecordDAO = activeRecordDAO;
	}

	/**
	 * 
	 * 开标记录页显示<br/>
	 * <p>
	 * 开标记录页显示
	 * </p>
	 * 
	 * @param data
	 * @return
	 * @throws FacadeException
	 * @throws IOException
	 */
	@Path(value = "/showRecordList/{flowID}", desc = "开标记录页显示")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView showRecord(AeolusData data,
			@PathParam("flowID") String flowID) throws FacadeException,
			IOException
	{
		ModelMap model = new ModelMap();
		// 获取当前招标项目评标办法对应的开标记录列表
		List<Record<String, Object>> recordList = getRecordsList();
		if (CollectionUtils.isEmpty(recordList))
		{
			throw new ValidateException("", "获取当前招标项目评标办法对应的开标记录列表出现异常!");
		}
		model.put("RECORDLIST", recordList);
		model.put("FLOWID", flowID);
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/records/records.list.html", model);
	}

	/**
	 * 
	 * 下载开标记录DOC文件<br/>
	 * <p>
	 * 下载开标记录DOC文件
	 * </p>
	 * 
	 * @param data
	 * @param fileName
	 * @throws FacadeException
	 */
	@Path(value = "/download/{filename}/{comment}", desc = "下载开标记录DOC文件")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void download(AeolusData data,
			@PathParam("filename") String filename,
			@PathParam("comment") String comment) throws FacadeException
	{
		logger.debug(LogUtils.format("下载推送文件", data));
		String fileUrl = SystemParamUtils
				.getString(ConstantEOKB.SysParamKey.EBIDKB_FILE_PATH_URL)
				+ "bidReport"
				+ File.separator
				+ SessionUtils.getTPID()
				+ File.separator + filename + ".doc";
		// 获取开标记录文件
		File file = new File(fileUrl);
		if (!file.exists())
		{
			throw new ValidateException("", fileUrl + "不存在该开标记录文件!");
		}
		// 下载开标记录文件
		try
		{
			InputStream input = new FileInputStream(file);
			AeolusDownloadUtils.write(data, input, comment + ".doc");
		}
		catch (FileNotFoundException e)
		{
			throw new ValidateException("", "下载开标记录文件出现异常!");
		}
	}

	/**
	 * 
	 * 下载抽取xml文件<br/>
	 * <p>
	 * 下载抽取xml文件
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/downloadXml", desc = "下载抽取xml文件")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void downloadXml(AeolusData data) throws FacadeException,
			FileNotFoundException
	{
		logger.debug(LogUtils.format("下载抽取xml文件", data));
		String fileUrl = SystemParamUtils
				.getString(ConstantEOKB.SysParamKey.EBIDKB_FILE_PATH_URL)
				+ "bidReport"
				+ File.separator
				+ SessionUtils.getTPID()
				+ File.separator + "sieve.xml";
		// 获取开标记录文件
		File file = new File(fileUrl);
		if (!file.exists())
		{
			throw new ValidateException("", fileUrl + "不存在该xml文件!");
		}
		AeolusDownloadUtils.write(data, file);
	}

	/**
	 * 
	 * 确认是否存在ssp文件<br/>
	 * <p>
	 * 确认是否存在ssp文件
	 * </p>
	 * 
	 * @param data
	 * @param fileName
	 * @throws FacadeException
	 */
	@Path(value = "/existFile/{filename}/{comment}", desc = "确认是否存在ssp文件")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public boolean existFile(AeolusData data,
			@PathParam("filename") String filename,
			@PathParam("comment") String comment) throws FacadeException
	{
		logger.debug(LogUtils.format("下载推送文件", data));
		String fileUrl = SystemParamUtils
				.getString(ConstantEOKB.SysParamKey.EBIDKB_FILE_PATH_URL)
				+ "bidReport"
				+ File.separator
				+ SessionUtils.getTPID()
				+ File.separator + filename + ".ssp";
		// 获取开标记录文件
		File file = new File(fileUrl);
		if (!file.exists())
		{
			return false;
		}
		return true;
	}

	/**
	 * 
	 * 下载开标记录ssp文件<br/>
	 * <p>
	 * 下载开标记录ssp文件
	 * </p>
	 * 
	 * @param data
	 * @param fileName
	 * @throws FacadeException
	 */
	@Path(value = "/downloadssp/{filename}/{comment}", desc = "下载开标记录ssp文件")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void downloadSSP(AeolusData data,
			@PathParam("filename") String filename,
			@PathParam("comment") String comment) throws FacadeException
	{
		logger.debug(LogUtils.format("下载推送文件", data));
		String fileUrl = SystemParamUtils
				.getString(ConstantEOKB.SysParamKey.EBIDKB_FILE_PATH_URL)
				+ "bidReport"
				+ File.separator
				+ SessionUtils.getTPID()
				+ File.separator + filename + ".ssp";
		// 获取开标记录文件
		File file = new File(fileUrl);
		if (!file.exists())
		{
			throw new ValidateException("", fileUrl + "不存在该开标记录文件，清先上传ssp文件!");
		}
		// 下载开标记录文件
		try
		{
			InputStream input = new FileInputStream(file);
			AeolusDownloadUtils.write(data, input, comment + ".ssp");
		}
		catch (FileNotFoundException e)
		{
			throw new ValidateException("", "下载开标记录文件出现异常!");
		}
	}

	/**
	 * 文件上传<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 文件名称列表
	 * @throws FacadeException
	 *             FacadeException
	 * @throws UnsupportedEncodingException
	 */
	// 声明路径
	@Path(value = "upload/{fileName}", desc = "上传文件")
	// POST方法
	@HttpMethod(HttpMethod.POST)
	@Handler(MultipartFormDataHandler.class)
	public void uploadFiles(@PathParam("fileName") String fileName,
			AeolusData data) throws FacadeException,
			UnsupportedEncodingException
	{
		logger.debug(LogFormatUtils.formatOperateMessage("", "上传文件"));

		String key = "FILE";
		FileItem item = null;
		// 文件保存路径
		String rootPath = SystemParamUtils
				.getString(ConstantEOKB.SysParamKey.EBIDKB_FILE_PATH_URL)
				+ "bidReport/" + SessionUtils.getTPID();

		File savePath = new File(rootPath);
		// 创建根目录
		if (!savePath.exists())
		{
			savePath.mkdirs();
		}
		File file = null;
		item = data.getParam(key);
		if (null != item)
		{
			logger.debug(LogFormatUtils.formatOperateMessage("", "文件名",
					item.getName()));
			if (!StringUtils.endsWith(item.getName(), ".ssp"))
			{
				throw new FacadeException("", "请上传ssp文件");
			}
			file = new File(savePath, fileName + ".ssp");
			try
			{
				item.write(file);
			}
			catch (Exception e)
			{
				logger.error(LogFormatUtils.formatOperateMessage("",
						"保存文件发生异常", item), e);
			}
		}
	}

	/**
	 * 
	 * 获取当前招标项目评标办法对应的开标记录列表<br/>
	 * <p>
	 * 获取当前招标项目评标办法对应的开标记录列表
	 * </p>
	 * 
	 * @return
	 */
	private List<Record<String, Object>> getRecordsList()
			throws FacadeException
	{
		String code = SessionUtils.getTenderProjectTypeCode();
		if (StringUtils.isEmpty(code))
		{
			throw new ValidateException("", "获取评标办法JSON中的评标办法类型异常!");
		}
		// 是否标段组
		boolean isGroup = SessionUtils.isSectionGroup();
		return recordsList(code, isGroup);
	}

	/**
	 * 
	 * 根据招标类型获取开标记录 列表数据<br/>
	 * <p>
	 * 根据招标类型获取开标记录 列表数据
	 * </p>
	 * 
	 * @param code
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Record<String, Object>> recordsList(String code, boolean isGroup)
			throws ServiceException
	{
		if (StringUtils.isEmpty(code))
		{
			throw new ServiceException("", "招标类型不能为空!");
		}
		String keyCode = "";
		if (isGroup)
		{
			keyCode = code + "-1";
		}
		else
		{
			keyCode = code + "-0";
		}
		Record<String, Object> recordsTemp = RecordsUtils.recordsConfigInfo();
		return (List<Record<String, Object>>) recordsTemp.get(keyCode);
	}

	/**
	 * 
	 * 读取开标记录JSON文件<br/>
	 * <p>
	 * 读取开标记录JSON文件
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private String recordListJson() throws ServiceException
	{
		InputStream input = null;
		List<String> lines = null;
		try
		{
			input = ClassLoaderUtils.getResourceAsStream("com" + File.separator
					+ "sozone" + File.separator + "eokb" + File.separator
					+ "bus" + File.separator + "records" + File.separator
					+ "records.JSON", Records.class);
			lines = IOUtils.readLines(input, ConstantEOKB.DEFAULT_CHARSET);
			if (CollectionUtils.isEmpty(lines))
			{
				throw new ServiceException("", "开标记录JSON为空!");
			}
			StringBuilder sb = new StringBuilder();
			for (String line : lines)
			{
				sb.append(line);
			}
			return sb.toString();
		}
		catch (IOException e)
		{
			throw new ServiceException("", "读取开标记录JSON发生异常!", e);
		}
		finally
		{
			IOUtils.closeQuietly(input);
		}
	}
}