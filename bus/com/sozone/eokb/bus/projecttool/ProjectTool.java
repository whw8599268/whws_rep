/**
 * 包名：com.sozone.eokb.bus.projecttool
 * 文件名：ProjectTool.java<br/>
 * 创建时间：2018-3-30 上午11:20:29<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.projecttool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.dao.validate.RecordValidator;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.download.util.AeolusDownloadUtils;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.bidcheck.BidCheckUtils;
import com.sozone.eokb.bus.bidlog.BidOperationLog;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.ArchiverUtils;

/**
 * 后台管理员管理项目信息工具类<br/>
 * <p>
 * 后台管理员管理项目信息工具类<br/>
 * </p>
 * Time：2018-3-30 上午11:20:29<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/projecttool", desc = "后台管理员管理项目信息工具类")
@Permission(Level.Authenticated)
public class ProjectTool extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(ProjectTool.class);

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
	 * 获取符合搜索条件的招标项目信息分页封装<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 招标项目信息分页封装
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "", desc = "获取符合搜索条件的招标项目信息分页封装")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getProjectInfoPage(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取符合搜索条件的招标项目信息分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = data.getRecord();
		Page<Record<String, Object>> page = this.activeRecordDAO.statement()
				.selectPage("pt.getProjectInfo", pageable, param);
		List<Record<String, Object>> content = page.getContent();

		JSONObject bemJson = null;
		for (Record<String, Object> record : content)
		{
			bemJson = record.getJSONObject("V_BEM_INFO_JSON");
			if (!CollectionUtils.isEmpty(bemJson))
			{
				record.setColumn("V_BEM_CODE", bemJson.getString("V_CODE"));
			}
		}
		return page;
	}

	/**
	 * 
	 * 获取符合搜索条件的投标人信息分页封装<br/>
	 * <p>
	 * 获取符合搜索条件的投标人信息分页封装
	 * </p>
	 * 
	 * @param tpID
	 *            招标项目主键
	 * @param isGroup
	 *            是否标段组
	 * @param data
	 *            AeolusData
	 * @return Page
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "getBidder/{tpID}/{isGroup}", desc = "获取符合搜索条件的投标人信息分页封装")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getBiddersInfoPage(
			@PathParam("tpID") String tpID,
			@PathParam("isGroup") String isGroup, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取符合搜索条件的投标人信息分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = data.getRecord();
		param.setColumn("V_TPID", tpID);
		String tableName = TableName.EKB_T_TENDER_LIST;
		if (StringUtils.equals("1", isGroup))
		{
			tableName = TableName.EKB_T_DECRYPT_INFO;
		}
		Page<Record<String, Object>> page = this.activeRecordDAO.auto()
				.table(tableName).addSortOrder("V_BIDDER_NO", "ASC")
				.page(pageable, param);
		return page;
	}

	/**
	 * 修改招标项目头信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "modify", desc = "修改招标项目信息")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyProjectInfo(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("修改招标项目信息", data));
		Record<String, Object> record = data.getRecord();

		// 修改流程code
		String code = record.getString("V_BEM_CODE");

		JSONObject bemJson = record.getJSONObject("V_BEM_INFO_JSON");
		bemJson.put("V_CODE", code);
		record.setColumn("V_BEM_INFO_JSON", bemJson.toJSONString());

		this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_PROJECT_INFO)
				.modify(record);
	}

	/**
	 * 修改投标人信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "bidder/modify", desc = "修改投标人信息")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyBidderInfo(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("修改投标人信息", data));
		Record<String, Object> record = data.getRecord();

		this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST)
				.modify(record);
	}

	/**
	 * 删除招标项目信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "remove/{tpid}", desc = "删除招标项目信息")
	@HttpMethod(HttpMethod.GET)
	@Handler(OperationLogHandler.class)
	public void removeProjectInfos(@PathParam("tpid") String tpid)
			throws FacadeException
	{
		logger.debug(LogUtils.format("删除招标项目信息!", tpid));

		// 删除招标项目信息
		this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_PROJECT_INFO)
				.remove(tpid);
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		// 标段表
		this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);

		// 投标人表
		this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 最新解密临时信息表
		this.activeRecordDAO.auto().table(TableName.DECRYPT_TEMP_DATA)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 投标人联系方式表
		this.activeRecordDAO.auto().table(TableName.EKB_T_BIDDER_PHONE)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 数据确认表
		this.activeRecordDAO.auto().table(TableName.EKB_T_CHECK_DATA)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 解密表
		this.activeRecordDAO.auto().table(TableName.EKB_T_DECRYPT_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 解密表（日志）
		this.activeRecordDAO.auto().table(TableName.EKB_T_DECRYPT_INFO_LOG)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 开标异议表
		this.activeRecordDAO.auto().table(TableName.EKB_T_DISSENT)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 评标基准价计算记录表
		this.activeRecordDAO.auto()
				.table(TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 投标报价（有效报价）通过汇总表
		this.activeRecordDAO.auto()
				.table(TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_VALID_OFFER)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 系统公告表
		this.activeRecordDAO.auto().table(TableName.EKB_T_NOTICE)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 签到表
		this.activeRecordDAO.auto().table(TableName.EKB_T_SIGN_IN)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 投标文件信息表
		this.activeRecordDAO.auto().table(TableName.EKB_T_TBIMPORTBIDDING)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 投标人表
		this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_FLOW_NODE_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 流程节点数据表
		this.activeRecordDAO.auto().table(TableName.EKB_T_TPFN_DATA_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
	}

	/**
	 * 重置招标项目信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "reSet/{tpid}", desc = "删除招标项目信息")
	@HttpMethod(HttpMethod.GET)
	@Handler(OperationLogHandler.class)
	public void reSetProjectInfos(@PathParam("tpid") String tpid)
			throws FacadeException
	{
		logger.debug(LogUtils.format("删除招标项目信息!", tpid));

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);

		// 投标人表
		this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 最新解密临时信息表
		this.activeRecordDAO.auto().table(TableName.DECRYPT_TEMP_DATA)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 投标人联系方式表
		this.activeRecordDAO.auto().table(TableName.EKB_T_BIDDER_PHONE)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 数据确认表
		this.activeRecordDAO.auto().table(TableName.EKB_T_CHECK_DATA)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 解密表
		this.activeRecordDAO.auto().table(TableName.EKB_T_DECRYPT_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 解密表（日志）
		this.activeRecordDAO.auto().table(TableName.EKB_T_DECRYPT_INFO_LOG)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 开标异议表
		this.activeRecordDAO.auto().table(TableName.EKB_T_DISSENT)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 评标基准价计算记录表
		this.activeRecordDAO.auto()
				.table(TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 投标报价（有效报价）通过汇总表
		this.activeRecordDAO.auto()
				.table(TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_VALID_OFFER)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 系统公告表
		this.activeRecordDAO.auto().table(TableName.EKB_T_NOTICE)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 签到表
		this.activeRecordDAO.auto().table(TableName.EKB_T_SIGN_IN)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 投标文件信息表
		this.activeRecordDAO.auto().table(TableName.EKB_T_TBIMPORTBIDDING)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 投标人表
		this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_FLOW_NODE_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
		// 流程节点数据表
		this.activeRecordDAO.auto().table(TableName.EKB_T_TPFN_DATA_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);
	}

	/**
	 * 
	 * 获取全部标段信息<br/>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param methodType
	 *            评标办法
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "infos/{tpid}/{methodType}", desc = "获取全部标段信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSectionInfos(@PathParam("tpid") String tpid,
			@PathParam("methodType") int methodType, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取全部标段信息", data, tpid, methodType));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);

		if (CollectionUtils.isEmpty(sections))
		{
			throw new FacadeException("", "找不到对应得项目信息");
		}
		boolean showK = false;
		boolean showQ = false;
		String mainK = null;
		String maxK = null;
		String q1 = null;
		String q2 = null;

		JSONObject jobj = null;
		for (Record<String, Object> section : sections)
		{

			// 56:经A；57经B；58综A；59综B
			if (56 <= methodType && methodType <= 59)
			{
				jobj = section.getJSONObject("V_JSON_OBJ");
				if (CollectionUtils.isEmpty(jobj))
				{
					continue;
				}
				mainK = jobj.getString("MIN_K");
				maxK = jobj.getString("MAX_K");
				q1 = jobj.getString("Q2");
				q2 = jobj.getString("Q1");

				// K值信息
				if (StringUtils.isNotEmpty(mainK)
						&& StringUtils.isNotEmpty(maxK))
				{
					showK = true;
					section.setColumn("MIN_K", jobj.getString("MIN_K"));
					section.setColumn("MAX_K", jobj.getString("MAX_K"));

				}
				// Q值信息
				if (StringUtils.isNotEmpty(q1) && StringUtils.isNotEmpty(q2))
				{
					showQ = true;
					section.setColumn("Q1", jobj.getString("Q1"));
					section.setColumn("Q2", jobj.getString("Q2"));
				}
			}
			section.setColumn("SHOW_Q", showQ);
			section.setColumn("SHOW_K", showK);
		}
		model.put("SECTIONS", sections);
		model.put("SHOW_K", showK);
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/controlprice/control.price.admin.html", model);
	}

	/**
	 * 
	 * 保存控制价<br/>
	 * <p>
	 * 保存控制价
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/savePrices/{tpid}", desc = "保存控制价")
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void savePrices(@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("保存控制价", data));
		String info = data.getParam("info");
		// 标段信息
		Record<String, Object> section = new RecordImpl<String, Object>();
		// 标段扩展信息
		JSONObject sectionExtInfo = null;
		JSONObject viewJsons = null;
		JSONObject viewJson = null;
		Record<String, Object> param = new RecordImpl<String, Object>();

		if (!StringUtils.isEmpty(info))
		{
			JSONArray jarr = JSONArray.parseArray(info);
			for (int i = 0; i < jarr.size(); i++)
			{
				viewJsons = jarr.getJSONObject(i);
				logger.debug(LogUtils.format("从页面获取标段控制价信息", viewJsons));
				for (String key : viewJsons.keySet())
				{
					param.clear();
					param.setColumn("sid", key);
					param.setColumn("tpid", tpid);

					// 先获取标段
					section = this.activeRecordDAO.auto()
							.table(TableName.EKB_T_SECTION_INFO)
							.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
							.setCondition("AND", "V_TPID=#{tpid}").get(param);
					viewJson = viewJsons.getJSONObject(key);
					// 控制价更新
					section.setColumn("N_CONTROL_PRICE",
							viewJson.getDouble("CONTROL_PRICE"));
					section.setColumn("N_CONTROL_MIN_PRICE",
							viewJson.getDouble("CONTROL_MIN_PRICE"));

					sectionExtInfo = section.getJSONObject("V_JSON_OBJ");
					if (null != sectionExtInfo)
					{
						// 修改KQ值
						modifyKQValue(sectionExtInfo, viewJson, tpid);

						section.setColumn("V_JSON_OBJ",
								sectionExtInfo.toJSONString());
					}

					this.activeRecordDAO.auto()
							.table(TableName.EKB_T_SECTION_INFO)
							.modify(section);
				}
			}
		}
	}

	/**
	 * 
	 * 修改KQ值<br/>
	 * <p>
	 * 修改KQ值
	 * </p>
	 * 
	 * @param sectionExtInfo
	 * @param viewJson
	 * @throws ServiceException
	 */
	private void modifyKQValue(JSONObject sectionExtInfo, JSONObject viewJson,
			String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("修改KQ值", sectionExtInfo, viewJson));
		// 操作日志说明
		String memo = null;
		// 有K值需要处理
		if (StringUtils.isNotEmpty(viewJson.getString("SHOW_K")))
		{
			// K值做了修改
			if (!StringUtils.equals(sectionExtInfo.getString("MAX_K"),
					viewJson.getString("MAX_K")))
			{
				memo = "修改K的最大值";
				BidOperationLog.addOperationLog(memo,
						sectionExtInfo.getString("MAX_K"),
						viewJson.getString("MAX_K"), tpid);
				sectionExtInfo.put("MAX_K", viewJson.getString("MAX_K"));
			}
			if (!StringUtils.equals(sectionExtInfo.getString("MIN_K"),
					viewJson.getString("MIN_K")))
			{
				memo = "修改K的最小值";
				BidOperationLog.addOperationLog(memo,
						sectionExtInfo.getString("MIN_K"),
						viewJson.getString("MIN_K"), tpid);
				sectionExtInfo.put("MIN_K", viewJson.getString("MIN_K"));
			}
		}

		// 有Q值需要处理
		if (StringUtils.isNotEmpty(viewJson.getString("SHOW_Q")))
		{
			// Q1值做了修改
			if (!StringUtils.equals(sectionExtInfo.getString("Q1"),
					viewJson.getString("Q1")))
			{
				memo = "修改Q1值";
				BidOperationLog.addOperationLog(memo,
						sectionExtInfo.getString("Q1"),
						viewJson.getString("Q1"), tpid);
				sectionExtInfo.put("Q1", viewJson.getString("Q1"));
			}
			// Q2值做了修改
			if (!StringUtils.equals(sectionExtInfo.getString("Q2"),
					viewJson.getString("Q2")))
			{
				memo = "修改Q2值";
				BidOperationLog.addOperationLog(memo,
						sectionExtInfo.getString("Q2"),
						viewJson.getString("Q2"), tpid);
				sectionExtInfo.put("Q2", viewJson.getString("Q2"));
			}
		}
	}

	/**
	 * 
	 * 下载开标数据<br/>
	 * <p>
	 * 下载开标数据
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param tpid
	 *            招标项目主键
	 * @param type
	 *            开标数据类型
	 * @throws ServiceException
	 *             ServiceException
	 * @throws FileNotFoundException
	 */
	@Path(value = "dlkpd/{tpid}/{type}")
	@HttpMethod(HttpMethod.GET)
	@Handler(OperationLogHandler.class)
	public void downloadKpData(AeolusData data, @PathParam("tpid") String tpid,
			@PathParam("type") String type) throws ServiceException
	{
		logger.debug(LogUtils.format("下载开标数据", data, tpid));
		String content = getTableData(tpid);
		if (StringUtils.isBlank(content))
		{
			throw new ServiceException("", "未获取到开标数据");
		}
		if (StringUtils.equals("0", type))
		{
			AeolusDownloadUtils.write(data, content.getBytes(), "开标数据.txt");
			return;
		}

		Record<String, Object> project = this.activeRecordDAO
				.pandora()
				.SELECT("V_TENDER_PROJECT_CODE", "V_TENDER_PROJECT_NAME",
						"V_INVITENOTRUE", "V_JSON_OBJ")
				.FROM(TableName.EKB_T_TENDER_PROJECT_INFO).EQUAL("ID", tpid)
				.get();

		FileOutputStream fos = null;
		try
		{
			// 开标文件存放根路径
			String kbRootPath = SystemParamUtils
					.getString(SysParamKey.EBIDKB_FILE_PATH_URL);
			// 投递文件存放根路径
			String tbRootPath = SystemParamUtils.getString(
					SysParamKey.EDE_ENTBID_FILE_PATH, "D:\fileEbid-fileTb");

			String tag = File.separator;
			// 目的地Zip文件
			String zipDir = kbRootPath + "zip" + tag + tpid + tag;
			File dir = new File(zipDir);
			if (!dir.exists())
			{
				dir.mkdirs();
			}

			// 0、写入开标数据
			File txt = new File(zipDir + "sql.txt");

			fos = new FileOutputStream(txt);
			FileUtils.write(txt, content, ConstantEOKB.DEFAULT_CHARSET);

			// 1、复制招标文件
			String sourceDir = kbRootPath
					+ project.getString("V_TENDER_PROJECT_CODE");

			FileUtils.copyDirectory(
					new File(kbRootPath
							+ project.getString("V_TENDER_PROJECT_CODE")),
					new File(zipDir
							+ project.getString("V_TENDER_PROJECT_CODE")));

			// 2、复制开标记录
			File kpFile = new File(kbRootPath + "bidReport" + tag + tpid);
			if (kpFile.exists())
			{
				FileUtils.copyDirectory(kpFile, new File(zipDir + tpid));
			}
			// 3、复制投递文件
			if (StringUtils.equals("2", type))
			{
				List<Record<String, Object>> sections = this.activeRecordDAO
						.pandora().SELECT("V_BID_SECTION_NAME")
						.FROM(TableName.EKB_T_SECTION_INFO)
						.EQUAL("V_TPID", tpid).list();

				for (Record<String, Object> section : sections)
				{
					String tbDir = project.getString("V_INVITENOTRUE")
							+ "-"
							+ BidCheckUtils.getDirName(project,
									section.getString("V_BID_SECTION_NAME"));
					FileUtils.copyDirectory(new File(tbRootPath + tbDir),
							new File(zipDir + tbDir));

				}
			}
			// 将格式为D:\fileEbid-kp\zip\0ffcbe96a03c4fa2b294978a333b3e95\
			// 的最后一个斜杆去除，创建zip文件
			zipDir = zipDir.substring(0, zipDir.length() - 1) + ".zip";
			File zip = new File(zipDir);
			ArchiverUtils.zip(zip, dir.listFiles());
			AeolusDownloadUtils.write(data, zip);
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("打包文件异常", e));
			throw new ServiceException("", e.getMessage());
		}
		finally
		{
			IOUtils.closeQuietly(fos);
		}

	}

	/**
	 * 
	 * 获取开标项目信息并生成插入语句<br/>
	 * <p>
	 * 获取开标项目信息并生成插入语句
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @return 插入语句
	 * @throws ServiceException
	 *             ServiceException
	 */
	private String getTableData(String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("获取开标项目信息并生成插入语句", tpid));
		StringBuilder sb = new StringBuilder();
		// 复制招标项目表
		String tableName = TableName.EKB_T_TENDER_PROJECT_INFO;
		Record<String, Object> project = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO).get(tpid);
		sb.append("#").append(tableName).append(" \r\n");
		sb.append(copyTableData(project, tableName));

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		// 复制标段表
		tableName = TableName.EKB_T_SECTION_INFO;
		List<Record<String, Object>> list = this.activeRecordDAO.auto()
				.table(tableName).setCondition("AND", "V_TPID=#{tpid}")
				.list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}

		// 复制投标人表
		tableName = TableName.EKB_T_TENDER_LIST;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制最新解密临时信息表
		tableName = TableName.DECRYPT_TEMP_DATA;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制投标人联系方式表
		tableName = TableName.EKB_T_BIDDER_PHONE;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制数据确认表
		tableName = TableName.EKB_T_CHECK_DATA;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制解密表
		tableName = TableName.EKB_T_DECRYPT_INFO;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制解密表（日志）
		tableName = TableName.EKB_T_DECRYPT_INFO_LOG;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制开标异议表
		tableName = TableName.EKB_T_DISSENT;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制评标基准价计算记录表
		tableName = TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制投标报价（有效报价）通过汇总表
		tableName = TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_VALID_OFFER;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制系统公告表
		tableName = TableName.EKB_T_NOTICE;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制签到表
		tableName = TableName.EKB_T_SIGN_IN;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制投标文件信息表
		tableName = TableName.EKB_T_TBIMPORTBIDDING;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制投标人表
		tableName = TableName.EKB_T_TENDER_PROJECT_FLOW_NODE_INFO;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		// 复制流程节点数据表
		tableName = TableName.EKB_T_TPFN_DATA_INFO;
		list = this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		sb.append("#").append(tableName).append(" \r\n");
		for (Record<String, Object> tableRecord : list)
		{
			sb.append(copyTableData(tableRecord, tableName));
		}
		logger.debug(LogUtils.format("数据复制完成", sb.toString()));
		return sb.toString();
	}

	/**
	 * 
	 * 根据每条记录生成插入语句<br/>
	 * <p>
	 * 根据每条记录生成插入语句
	 * </p>
	 * 
	 * @param tableRecord
	 *            记录
	 * @param tableName
	 *            表名
	 * @return 插入语句
	 * @throws ServiceException
	 *             ServiceException
	 */
	private String copyTableData(Record<String, Object> tableRecord,
			String tableName) throws ServiceException
	{
		logger.debug(LogUtils.format("根据每条记录生成插入语句", tableRecord, tableName));
		if (CollectionUtils.isEmpty(tableRecord))
		{
			return "";

		}
		// 字段值集
		List<String> columnValues = new LinkedList<String>();
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO ").append(tableName).append(" ( ");

		boolean isFirst = true;
		// 构建字段名
		for (String key : tableRecord.keySet())
		{
			columnValues.add(JSON.toJSONString(tableRecord.getString(key)));

			if (isFirst)
			{
				sb.append(key);
				isFirst = false;
				continue;
			}
			sb.append("," + key);
		}
		sb.append(") VALUES(");

		// 构建值
		isFirst = true;
		for (String str : columnValues)
		{
			if (isFirst)
			{
				sb.append(str);
				isFirst = false;
				continue;
			}
			sb.append("," + str);
		}
		sb.append("); \r\n");

		return sb.toString();
	}

	/**
	 * 
	 * 打开修改摄像头信息页面<br/>
	 * <p>
	 * 打开修改摄像头信息页面
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param tpid
	 *            招标项目主键
	 * @return ModelAndView
	 * @throws ServiceException
	 *             服务异常
	 */
	@Path(value = "mdv/view/{tpid}", desc = "打开修改摄像头信息页面")
	@Service
	public ModelAndView videoView(AeolusData data,
			@PathParam("tpid") String tpid) throws ServiceException
	{
		logger.debug(LogUtils.format("打开修改摄像头信息页面", data, tpid));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		// 获取摄像头信息
		Record<String, Object> video = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_VIDEO_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").get(param);
		// 未录入过信息
		if (CollectionUtils.isEmpty(video))
		{
			video = new RecordImpl<String, Object>();
			video.setColumn("ID", SZUtilsID.getUUID());
			video.setColumn("V_TPID", tpid);
		}
		return new ModelAndView(
				"/default/eokb/bus/project_tool/project.video.edit.html", video);
	}

	/**
	 * 
	 * 修改摄像头信息<br/>
	 * <p>
	 * 修改摄像头信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             服务异常
	 */
	@Path(value = "mdi", desc = "修改摄像头信息")
	@Service
	@Handler(OperationLogHandler.class)
	public void modifyVideoInfo(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("修改摄像头信息", data));
		Record<String, Object> videoInfo = data
				.getTableRecord(TableName.EKB_T_VIDEO_INFO);

		// 校验
		RecordValidator.validateRecord(TableName.EKB_T_VIDEO_INFO, videoInfo);

		this.activeRecordDAO.auto().table(TableName.EKB_T_VIDEO_INFO)
				.save(videoInfo);
	}
}
