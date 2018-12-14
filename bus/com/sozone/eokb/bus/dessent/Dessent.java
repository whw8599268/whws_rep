/**
 * 包名：com.sozone.eokb.bus.dessent
 * 文件名：Dessent.java<br/>
 * 创建时间：2017-11-29 下午1:38:22<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.dessent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.download.util.AeolusDownloadUtils;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.upload.handler.MultipartFormDataHandler;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.LogFormatUtils;
import com.sozone.eokb.bus.video.Video;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 开标异议服务接口<br/>
 * <p>
 * 开标异议服务接口<br/>
 * </p>
 * Time：2017-11-29 下午1:38:22<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/dessent", desc = "开标异议服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class Dessent
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Dessent.class);

	/**
	 * 结束第一信封标识
	 */
	private static String firstEnvelopeOver = ConstantEOKB.EOKBFlowCode.FIRST_OVER;

	/**
	 * 结束第二信封标识
	 */
	private static String secondEnvelopeOver = ConstantEOKB.EOKBFlowCode.SECOND_OVER;

	/**
	 * 结束开标标识
	 */
	private static String bidOver = ConstantEOKB.EOKBFlowCode.BID_OVER;

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
	 * 开标异议信息列表<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/list", desc = "开标异议信息列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("开标异议信息列表", data));
		String tpid = SessionUtils.getTPID();
		// 投标人只能看到自己发起的异议
		if (SessionUtils.isBidder())
		{
			String orgCode = SessionUtils.getEntUniqueCode();
			return this.activeRecordDAO.auto().table(TableName.EKB_T_DISSENT)
					.setCondition("AND", " V_TPID = '" + tpid + "'")
					.setCondition("AND", " V_ORG_CODE = '" + orgCode + "'")
					.addSortOrder("V_DISSENT_TIME", "ASC")
					.page(data.getPageRequest(), data.getRecord());
		}
		return this.activeRecordDAO.auto().table(TableName.EKB_T_DISSENT)
				.setCondition("AND", " V_TPID = '" + tpid + "'")
				.addSortOrder("V_DISSENT_TIME", "ASC")
				.page(data.getPageRequest(), data.getRecord());
	}

	/**
	 * 保存开标异议<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/save", desc = "保存开标异议")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void saveDissent(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存开标异议", data));
		String bidderName = data.getParam("bidderName");
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("ID", Random.generateUUID());
		record.setColumn("V_TPID", SessionUtils.getTPID());
		record.setColumn("V_BIDDER_NAME", bidderName);
		record.setColumn("V_DISSENT_TIME", new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()));
		record.setColumn("V_DISSENT_CONTENT", data.getParam("dissentContent"));
		record.setColumn("V_DISSENT_REVERT", data.getParam("dissentRevert"));
		this.activeRecordDAO.auto().table(TableName.EKB_T_DISSENT).save(record);
	}

	/**
	 * 
	 * 上传异议文件<br/>
	 * <p>
	 * 上传异议文件
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param id
	 *            招标项目主键
	 * @throws FacadeException
	 *             FacadeException
	 * @throws UnsupportedEncodingException
	 *             UnsupportedEncodingException
	 */
	// 声明路径
	@Path(value = "upload/{id}", desc = "上传异议文件")
	// POST方法
	@HttpMethod(HttpMethod.POST)
	@Handler(MultipartFormDataHandler.class)
	public void uploadFiles(AeolusData data, @PathParam("id") String id)
			throws FacadeException, UnsupportedEncodingException
	{
		logger.debug(LogUtils.format("", "上传异议文件"));
		String key = "FILE";
		FileItem item = null;
		// 文件保存路径
		String rootPath = null;
		// 如果是投标人则是发起异议，代理上传回复异议
		if (SessionUtils.isBidder())
		{
			// 文件保存路径
			rootPath = SystemParamUtils
					.getString(ConstantEOKB.SysParamKey.EBIDKB_FILE_PATH_URL)
					+ "bidReport/" + SessionUtils.getTPID() + "/dissent";
		}
		else
		{
			// 文件保存路径
			rootPath = SystemParamUtils
					.getString(ConstantEOKB.SysParamKey.EBIDKB_FILE_PATH_URL)
					+ "bidReport/" + SessionUtils.getTPID() + "/revert";
		}

		File savePath = new File(rootPath);
		// 创建根目录
		if (!savePath.exists())
		{
			savePath.mkdirs();
		}
		File file = null;
		item = data.getParam(key);
		key = "INPUT";
		String title = data.getParam(key);
		if (!StringUtils.isEmpty(title))
		{
			title = new String(title.getBytes("ISO8859-1"), "UTF-8");
		}
		String uuid = Random.generateUUID();
		// 组织机构代码
		String orgCode = SessionUtils.getEntUniqueCode();
		if (null != item)
		{
			logger.debug(LogUtils.format("验证是否是ssp文件", item.getName()));

			if (!StringUtils.endsWith(item.getName(), ".ssp"))
			{
				throw new FacadeException("", "请上传ssp文件");
			}
			if (StringUtils.equals("1", id))
			{
				file = new File(savePath, uuid + ".ssp");
			}
			else
			{
				file = new File(savePath, id + ".ssp");
			}

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
		logger.debug(LogUtils.format("ssp文件已成功持久化，并准备保存异议信息"));
		Record<String, Object> record = new RecordImpl<String, Object>();
		// 如果是投标人则是发起异议，代理上传回复异议
		if (SessionUtils.isBidder())
		{
			// 添加异议数据
			record.setColumn("ID", uuid);
			record.setColumn("V_TPID", SessionUtils.getTPID());
			record.setColumn("N_STATUS", 0);
			record.setColumn("V_BIDDER_NAME", SessionUtils.getCompanyName());
			record.setColumn("V_DESSENT_TITLE", title);
			record.setColumn("V_ORG_CODE", orgCode);
			record.setColumn("V_DISSENT_FOLDERPATH", rootPath + "/" + uuid
					+ ".ssp");
			record.setColumn("V_DISSENT_TIME", new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()));
			this.activeRecordDAO.auto().table(TableName.EKB_T_DISSENT)
					.save(record);
		}
		else
		{
			record.setColumn("V_REVERT_FOLDERPATH", rootPath + "/" + id
					+ ".ssp");
			record.setColumn("V_REVERT_TIME", new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()));
			record.setColumn("N_STATUS", 1);
			record.setColumn("V_REVERT_NAME", SessionUtils.getCompanyName());
			record.setColumn("ID", id);
			this.activeRecordDAO.auto().table(TableName.EKB_T_DISSENT)
					.modify(record);
		}
		logger.debug(LogUtils.format("保存异议信息完成"));

	}

	/**
	 * 
	 * 下载异议文件<br/>
	 * <p>
	 * 下载异议文件
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param id
	 *            异议主键
	 * @param flag
	 *            角色类型
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/download/{id}/{flag}", desc = "下载异议文件")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void download(AeolusData data, @PathParam("id") String id,
			@PathParam("flag") boolean flag) throws FacadeException
	{
		logger.debug(LogUtils.format("下载异议文件", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("ID", id);
		Record<String, Object> record = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_DISSENT).get(param);
		if (!CollectionUtils.isEmpty(record))
		{
			String path = null;
			String fileName = null;
			// 异议
			if (flag)
			{
				path = record.getString("V_DISSENT_FOLDERPATH");
				fileName = "异议.ssp";
			}
			else
			{
				path = record.getString("V_REVERT_FOLDERPATH");
				fileName = "答复.ssp";
			}

			// 获取异议文件
			File file = new File(path);
			if (!file.exists())
			{
				throw new ValidateException("", path + "不存在该异议文件!");
			}
			// 下载异议文件
			try
			{
				InputStream input = new FileInputStream(file);
				AeolusDownloadUtils.write(data, input, fileName);
			}
			catch (FileNotFoundException e)
			{
				throw new ValidateException("", "下载异议文件出现异常!");
			}
		}
	}

	/**
	 * 
	 * 获取异议倒计时时间<br/>
	 * <p>
	 * 获取异议倒计时时间
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param flowid
	 *            流程节点ID
	 * @return 开始时间和结束时间
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/getTime/{flowid}", desc = "获取异议倒计时时间")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Record<String, Object> getTime(AeolusData data,
			@PathParam("flowid") String flowid) throws FacadeException
	{
		logger.debug(LogUtils.format("获取异议倒计时时间", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("ID", flowid);
		param.setColumn("N_STATUS", 2);
		Record<String, Object> record = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_STATUS_TIME).get(param);
		if (CollectionUtils.isEmpty(record))
		{
			throw new FacadeException("", "获取异议时间失败");
		}
		record.setColumn("NOW_TIME", System.currentTimeMillis());
		record.setColumn("END_TIME", record.getLong("N_START_TIME")
				+ ConstantEOKB.CONFIRM_DURATION_TEN);
		return record;
	}

	/**
	 * 
	 * 增加倒计时时间（10分钟）<br/>
	 * <p>
	 * 增加倒计时时间（10分钟）
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param flowid
	 *            流程节点ID
	 * @return 起始时间和结束时间
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/addTime/{flowid}", desc = "增加倒计时时间（10分钟）")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Record<String, Object> addTime(AeolusData data,
			@PathParam("flowid") String flowid) throws FacadeException
	{
		logger.debug(LogUtils.format("增加倒计时时间", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("ID", flowid);
		param.setColumn("N_STATUS", 2);
		// 先获取节点时间信息
		Record<String, Object> record = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_STATUS_TIME).get(param);
		if (CollectionUtils.isEmpty(record))
		{
			throw new FacadeException("", "获取节点时间失败");
		}
		// 延长十分钟
		record.setColumn("N_START_TIME", record.getLong("N_START_TIME")
				+ ConstantEOKB.CONFIRM_DURATION_TEN);
		// 保存数据
		this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_STATUS_TIME)
				.modify(record);

		record.setColumn("NOW_TIME", System.currentTimeMillis());
		record.setColumn("END_TIME", record.getLong("N_START_TIME")
				+ ConstantEOKB.CONFIRM_DURATION_TEN);
		return record;
	}

	/**
	 * 结束开标<br/>
	 * 
	 * @throws FacadeException
	 *             FacadeException
	 * @throws IOException
	 */
	// 定义路径
	@Path(value = "/bid/over", desc = "结束开标")
	// GET访问方式
	@Service
	public void overBid() throws FacadeException
	{
		logger.debug(LogUtils.format("结束开标"));
		// 设置项目ID
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 设置修改条件
		param.setColumn("tpid", tpid);
		// 评标状态0:未评标,2-1:第一信封评标完成,2:评标完成,10:评标终止\r\n
		param.setColumn("V_BID_OPEN_STATUS", bidOver);
		this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.modify(param);

		// 设置开标状态结束
		SessionUtils.setAttribute(ConstantEOKB.BID_OPEN_STATUS_SESSION_KEY,
				true);
		// 开标视频是否可用
		int videoStatus = SystemParamUtils
				.getInteger(ConstantEOKB.SysParamKey.EOV_VIDEO_STATUS);
		if (videoStatus == 1)
		{
			VideoTask task = new VideoTask(tpid);
			task.start();
		}

	}

	/**
	 * 
	 * 推送视频操作线程<br/>
	 * <p>
	 * 推送视频操作线程<br/>
	 * </p>
	 * Time：2018-10-9 下午2:10:41<br/>
	 * 
	 * @author wengdm
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private class VideoTask extends Thread
	{

		/**
		 * 项目ID
		 */
		private String tpid = null;

		/**
		 * 构造函数
		 * 
		 * @param tpid
		 * @param tpName
		 */
		private VideoTask(String tpid)
		{
			super();
			this.tpid = tpid;
			this.setName("推送视频操作线程");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run()
		{
			// 结束录制视频
			Video.endVideo(tpid);
			// 禁用视频流
			Video.disableVideo(tpid);
		}

	}

	/**
	 * 结束第一信封开标<br/>
	 * 
	 * @throws FacadeException
	 *             FacadeException
	 * @throws IOException
	 */
	// 定义路径
	@Path(value = "/first/over", desc = "结束第一信封开标")
	// GET访问方式
	@Service
	public void overFirstEnvelope() throws FacadeException
	{
		logger.debug(LogUtils.format("结束第一信封开标"));
		// 设置项目ID
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 设置修改条件
		param.setColumn("tpid", tpid);
		// 评标状态0:未评标,2-1:第一信封评标完成,2:评标完成,10:评标终止\r\n
		param.setColumn("V_BID_OPEN_STATUS", firstEnvelopeOver);
		// 普通勘察第二信封不用开，修改状态2-2即可
		if (StringUtils.contains(SessionUtils.getTenderProjectTypeCode(),
				"fjs_ptgl_kcsj"))
		{
			param.setColumn("V_BID_OPEN_STATUS", secondEnvelopeOver);
		}
		this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.modify(param);
	}

	/**
	 * 结束第二信封开标<br/>
	 * 
	 * @throws FacadeException
	 *             FacadeException
	 * @throws IOException
	 */
	// 定义路径
	@Path(value = "/second/over", desc = "结束第二信封开标")
	// GET访问方式
	@Service
	public void overSecondEnvelope() throws FacadeException
	{
		logger.debug(LogUtils.format("结束第二信封开标"));
		// 设置项目ID
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 设置修改条件
		param.setColumn("tpid", tpid);
		// 评标状态0:未评标,2-1:第一信封评标完成,2:评标完成,10:评标终止\r\n
		param.setColumn("V_BID_OPEN_STATUS", secondEnvelopeOver);
		this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.modify(param);
	}
}
