package com.sozone.eokb.bus.project;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.LogFormatUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.ArchiverUtils;
import com.sozone.eokb.utils.MsgUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 
 * 项目列表服务类<br/>
 * <p>
 * 项目列表服务类<br/>
 * </p>
 * Time：2017-8-29 下午4:30:29<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/project", desc = "开标项目信息服务类")
// 登录即可访问
@Permission(Level.Authenticated)
public class Project
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Project.class);

	/**
	 * 旧版投标文件制作系统
	 */
	private static String decryptV1 = "/eokb/bus/decrypt/three.parts.decrypt.zbr.v2.jsonp.html";
	private static String decryptTbV1 = "/eokb/bus/decrypt/three.parts.decrypt.tbr.html";

	/**
	 * 新版投标文件制作系统
	 */
	private static String decryptV2 = "/eokb/bus/decrypt/upgrade/three.parts.decrypt.zbr.v3.jsonp.html";
	private static String decryptTbV2 = "/eokb/bus/decrypt/upgrade/three.parts.decrypt.tbr.v3.html";

	/**
	 * 2018-10-01 00:00:00
	 */
	private static Long noticeTime = 1538323200000l;

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
	 * 招标代理获取开标信息列表<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/dllist", desc = "招标代理获取开标信息列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getKaiBiaoListOfTender(AeolusData data)
			throws FacadeException
	{
		Record<String, Object> param = data.getRecord();
		String kbTime = param.getString("kbTime");
		// 组织机构代码
		String orgCode = SessionUtils.getCompanyCode();
		// 招标项目类型
		String tenderProType = SessionUtils.getTenderProjectAppType(data
				.getHttpServletRequest());
		String nowDate = "";
		String startTime = "";
		String endTime = "";
		if (StringUtils.isNotEmpty(kbTime))
		{
			startTime = kbTime + " 00:00:00";
			endTime = kbTime + " 23:59:59";
		}
		else
		{
			// 获取今天日期
			nowDate = DateUtils.getDate("yyyy-MM-dd");
			// 开始时间
			startTime = nowDate + " 00:00:00";
			endTime = nowDate + " 23:59:59";
		}
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("start", startTime);
		params.setColumn("end", endTime);
		params.setColumn("orgcode", orgCode);
		params.setColumn("type", tenderProType);

		Pageable pageable = data.getPageRequest();

		Page<Record<String, Object>> page = this.activeRecordDAO
				.auto()
				.table(ConstantEOKB.TableName.EKB_T_TENDER_PROJECT_INFO)
				.setCondition("AND",
						"(V_TENDER_AGENCY_CODE = #{orgcode} OR V_TENDERER_CODE = #{orgcode})")
				.setCondition("AND", "V_TENDERPROJECT_APP_TYPE = #{type}")
				.setCondition(
						"AND",
						"((V_BIDOPEN_TIME >= #{start} AND V_BIDOPEN_TIME <= #{end}) OR N_FB_STATUS =1 ) ")
				.addSortOrder("V_BIDOPEN_TIME", "ASC").page(pageable, params);

		// 高速要体现解密时间 ，有数据的情况下要读取开标解密时间
		if (StringUtils.equals("10", tenderProType) && page.getSize() != 0)
		{
			List<Record<String, Object>> projects = page.getContent();
			getDecryptTime(projects);
		}

		getProjectStatus(page.getContent());
		return page;
	}

	/**
	 * 
	 * 获取项目封标状态<br/>
	 * <p>
	 * 获取项目封标状态
	 * </p>
	 * 
	 * @param page
	 */
	private void getProjectStatus(List<Record<String, Object>> projectList)
	{
		logger.debug(LogUtils.format("", "获取项目封标状态"));
		// V_JSON_OBJ
		JSONObject jobj = null;
		for (Record<String, Object> project : projectList)
		{
			jobj = project.getJSONObject("V_JSON_OBJ");
			project.setColumn("IS_FB", false);
			if (null != jobj && null != jobj.getBoolean("IS_FB")
					&& jobj.getBoolean("IS_FB"))
			{
				project.setColumn("IS_FB", true);
			}
		}
	}

	/**
	 * 
	 * 获取解密时间<br/>
	 * <p>
	 * 获取解密时间
	 * </p>
	 * 
	 * @param projects
	 * @throws FacadeException
	 */
	private void getDecryptTime(List<Record<String, Object>> projects)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取解密时间", projects));
		JSONObject jsonObj;
		String decryptTime;
		for (Record<String, Object> project : projects)
		{
			jsonObj = project.getJSONObject("V_JSON_OBJ");
			// 代理未设置过解密时间，默认取30分钟
			if (CollectionUtils.isEmpty(jsonObj))
			{
				decryptTime = "30";
			}
			else
			{
				// 代理设置过解密时间
				if (!StringUtils.isEmpty(jsonObj.getString("decrypt_time")))
				{
					decryptTime = jsonObj.getString("decrypt_time");
				}
				// 代理未设置过解密时间，默认取30分钟
				else
				{
					decryptTime = "30";
				}
			}
			project.setColumn("DECRYPT_TIME", decryptTime);
		}
	}

	/**
	 * 获取投标人可以参与开标的招标项目信息列表<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/tbrlist", desc = "获取投标人可以参与开标的招标项目信息列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getTenderProjectListOfBidder(
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取投标人可以参与开标的招标项目信息列表", data));
		Record<String, Object> param = data.getRecord();
		String kbTime = param.getString("kbTime");
		// 组织机构代码
		String orgCode = SessionUtils.getCompanyCode();
		// 招标项目类型
		String tenderProType = SessionUtils.getTenderProjectAppType(data
				.getHttpServletRequest());
		String nowDate = "";
		String startTime = "";
		String endTime = "";
		if (StringUtils.isNotEmpty(kbTime))
		{
			startTime = kbTime + " 00:00:00";
			endTime = kbTime + " 23:59:59";
		}
		else
		{
			// 获取今天日期
			nowDate = DateUtils.getDate("yyyy-MM-dd");
			// 开始时间
			startTime = nowDate + " 00:00:00";
			// 结束时间
			endTime = nowDate + " 23:59:59";
		}
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("start", startTime);
		params.setColumn("end", endTime);
		params.setColumn("orgcode", orgCode);
		params.setColumn("type", tenderProType);

		List<Record<String, Object>> projectList = this.activeRecordDAO
				.statement().selectList("Project.getTenderProjectListOfBidder",
						params);

		getProjectStatus(projectList);
		return projectList;
	}

	/**
	 * 
	 * 修改解密时间<br/>
	 * <p>
	 * 修改解密时间
	 * </p>
	 * 
	 * @param id
	 *            招标项目ID
	 * @param time
	 *            解密时间
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/mdt/{id}/{time}", desc = "修改解密时间")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void modifyDecryptTime(@PathParam("id") String id,
			@PathParam("time") String time, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("修改解密时间", id));

		// 获取项目信息
		Record<String, Object> project = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO).get(id);
		if (CollectionUtils.isEmpty(project))
		{
			throw new FacadeException("", "找不到对应的招标项目");
		}

		// 获取JSON字段
		JSONObject jsonObj = project.getJSONObject("V_JSON_OBJ");
		// 没有就重新赋值
		if (CollectionUtils.isEmpty(jsonObj))
		{
			jsonObj = new JSONObject();
		}
		jsonObj.put("decrypt_time", time);

		project.setColumn("V_JSON_OBJ", jsonObj.toJSONString());
		this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_PROJECT_INFO)
				.modify(project);
	}

	/**
	 * 设置解密菜单<br/>
	 * <p>
	 * 设置解密菜单
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/setfun/{tpid}", desc = "设置解密菜单")
	@HttpMethod(HttpMethod.GET)
	public void setFlowFunction(@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogFormatUtils.formatOperateMessage("", "设置解密菜单", data));
		Record<String, Object> project = this.activeRecordDAO.pandora()
				.SELECT("V_BEM_INFO_JSON,V_NOTICE_PUBLIC_TIME")
				.FROM(ConstantEOKB.TableName.EKB_T_TENDER_PROJECT_INFO)
				.EQUAL("ID", tpid).get();
		if (CollectionUtils.isEmpty(project))
		{
			// 获取不到项目信息
			throw new ValidateException("E-1003");
		}

		// 获取公告发布时间
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Long timeStamp = 0L;
		try
		{
			timeStamp = format.parse(project.getString("V_NOTICE_PUBLIC_TIME"))
					.getTime();
		}
		catch (Exception e)
		{
			logger.error(
					LogUtils.format("转化公告时间失败",
							project.getString("V_NOTICE_PUBLIC_TIME")), e);
			return;
		}

		// 获取评标办法信息
		JSONObject bemJson = project.getJSONObject("V_BEM_INFO_JSON");
		if (CollectionUtils.isEmpty(bemJson))
		{
			throw new FacadeException("", "无法获取到评标办法信息");
		}
		String bemCode = bemJson.getString("V_CODE");
		if (StringUtils.isEmpty(bemCode))
		{
			throw new FacadeException("", "无法获取评标办法");
		}
		String func = decryptV1;
		String tbFunc = decryptTbV1;
		// 2018-10-01 之后的时间是新版的投标文件制作系统
		if (noticeTime <= timeStamp)
		{
			func = decryptV2;
			tbFunc = decryptTbV2;
		}

		// 获取解密菜单信息
		Record<String, Object> node = this.activeRecordDAO.pandora()
				.SELECT("N.ID,N.V_TENDERER_PAGE_URL").FROM("EKB_T_FLOW_INFO F")
				.JOIN("EKB_T_FLOW_NODE_INFO N ON (F.ID=N.V_FLOW_ID)")
				.WHERE("N.V_NODE_NAME='投标文件解密' AND N.V_PID IS NOT NULL ")
				.EQUAL("F.V_FLOW_CODE", bemCode).get();
		if (CollectionUtils.isEmpty(node))
		{
			throw new FacadeException("", "查询不到对应的流程信息");
		}

		// 判断当前菜单是否需要变更
		if (!StringUtils.equals(func, node.getString("V_TENDERER_PAGE_URL")))
		{
			node.put("V_TENDERER_PAGE_URL", func);
			node.put("V_BIDDER_PAGE_URL", tbFunc);
			this.activeRecordDAO.pandora().UPDATE("EKB_T_FLOW_NODE_INFO")
					.SET(node).EQUAL("ID", node.getString("ID")).excute();
		}
	}

	/**
	 * 招标代理进入项目流程<br/>
	 * <p>
	 * 招标代理进入项目流程
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/zbrgo/{tpid}", desc = "招标代理进入项目流程")
	@HttpMethod(HttpMethod.GET)
	public void initProjectInfoByTender(@PathParam("tpid") String tpid,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogFormatUtils
				.formatOperateMessage("", "招标代理进入项目流程", data));
		logger.debug(LogFormatUtils.formatOperateMessage("", "进入项目-代理", data));
		Record<String, Object> tenderProject = this.activeRecordDAO.auto()
				.table(ConstantEOKB.TableName.EKB_T_TENDER_PROJECT_INFO)
				.get(tpid);
		if (CollectionUtils.isEmpty(tenderProject))
		{
			// 获取不到项目信息
			throw new ValidateException("E-1003");
		}
		// 获取开标时间
		String openTime = tenderProject.getString("V_BIDOPEN_TIME");
		if (StringUtils.isEmpty(openTime))
		{
			throw new ServiceException("", "招标项目开标时间为空!");
		}
		Date opTime = DateUtils.parseDate(openTime, "yyyy-MM-dd HH:mm:ss");
		if (opTime.getTime() > System.currentTimeMillis())
		{
			throw new ServiceException("", "招标项目开标时间尚未开始,请耐心等待!");
		}
		// 清空session 信息
		ApacheShiroUtils.getSession().removeAttribute(
				ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY);
		SessionUtils.setAttribute(ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY,
				tenderProject);
		// 设置流程信息
		setFlowInfo(tenderProject);
		String tpType = tenderProject.getString("V_TENDERPROJECT_APP_TYPE");
		// 如果是房建市政普通
		if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE, tpType)
				|| StringUtils.equals(
						ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE,
						tpType)
				|| StringUtils.equals(
						ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_PTGL_TYPE,
						tpType))
		{
			String pc = tenderProject.getString("V_TENDER_PROJECT_CODE");
			// 获取招标文件
			File szb = new File(
					SystemParamUtils
							.getProperty(SysParamKey.EBIDKB_FILE_PATH_URL),
					pc + "/" + pc + ".szb");
			File dir = unpackTenderDoc(szb);
			if (null == dir || !dir.exists())
			{
				throw new ServiceException("", "无法获取到招标项目的招标文件!");
			}
			// 设置评标办法
			JSONObject pbMethod = getPBMethodJsonObject(dir, tenderProject);
			// 设置评标办法JSON
			SessionUtils.setAttribute(
					ConstantEOKB.PB_METHOD_JSON_INFO_SESSION_KEY, pbMethod);

			// IS_APPRAISAL_PRICE_RATE=“1” 是采用费率 否则采用投标报价
			SessionUtils.setAttribute(ConstantEOKB.IS_APPRAISAL_PRICE_RATE,
					pbMethod.getString("IS_APPRAISAL_PRICE_RATE"));
		}
		// 设置开标工具功能是否可用的session信息
		setToolSessionInfo();
	}

	/**
	 * 获取招标文件中的招标办法JSON信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param bidDocFile
	 *            招标文件
	 * @param tp
	 *            招标项目信息
	 * @return 招标办法JSON信息
	 * @throws ServiceException
	 *             自定义服务异常
	 */
	private JSONObject getPBMethodJsonObject(File bidDocFile,
			Record<String, Object> tp) throws ServiceException
	{
		File pbbf = new File(bidDocFile, "评标办法.json");
		try
		{
			String json = FileUtils.readFileToString(pbbf,
					ConstantEOKB.DEFAULT_CHARSET);
			// 如果文件时空的
			if (StringUtils.isEmpty(json))
			{
				// 招标项目名称
				String tpName = SessionUtils.getBidProjectName();
				StringBuilder sb = new StringBuilder();
				sb.append("开标进入开标项目：招标办法JSON文件是空的，招标项目:[").append(tpName)
						.append("]文件路径[").append(pbbf.getAbsolutePath())
						.append("]，请处理!");
				MsgUtils.send(sb.toString());
				throw new ServiceException("", "无效的招标办法信息描述文件!");
			}

			JSONObject obj = JSON.parseObject(json);
			// 评标办法信息
			JSONObject bemInfo = obj.getJSONObject("BEM_INFO");
			// return bemInfo;
			// 招标项目表中的bem
			JSONObject tbem = JSON.parseObject(tp.getString("V_BEM_INFO_JSON"));
			// 将文件中的编码改成数据库中的
			if (null != tbem)
			{
				bemInfo.put("V_CODE", tbem.getString("V_CODE"));
			}
			obj.put("BEM_INFO", bemInfo);
			FileUtils.write(pbbf, obj.toJSONString(),
					ConstantEOKB.DEFAULT_CHARSET);
			return obj;

		}
		catch (IOException e)
		{
			// 招标项目名称
			String tpName = SessionUtils.getBidProjectName();
			StringBuilder sb = new StringBuilder();
			sb.append("开标进入开标项目：读取招标办法JSON失败，招标项目:[").append(tpName)
					.append("]文件路径[").append(pbbf.getAbsolutePath())
					.append("]，请处理!");
			MsgUtils.send(sb.toString());
			throw new ServiceException("", "读取评标办法失败!", e);
		}
	}

	/**
	 * 解压招标文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param szb
	 * @return
	 * @throws ServiceException
	 */
	private File unpackTenderDoc(File szb) throws ServiceException
	{
		if (null == szb || !szb.exists())
		{
			throw new ServiceException("", "招标文件不存在!");
		}
		// 如果不是.sfb文件
		if (!StringUtils.equalsIgnoreCase("szb",
				FilenameUtils.getExtension(szb.getName())))
		{
			throw new ServiceException("", "无效的招标文件格式!");
		}
		File target = new File(szb.getParentFile(),
				ConstantEOKB.TENDER_DOC_UNPACK_DIR_NAME);
		synchronized (this)
		{
			if (target.exists())
			{
				return target;
			}
			target.mkdirs();
			try
			{
				ArchiverUtils.unZip(szb, target);
			}
			catch (IOException e)
			{
				// 招标项目名称
				String tpName = SessionUtils.getBidProjectName();
				StringBuilder sb = new StringBuilder();
				sb.append("开标房建市政解压招标文件：失败，招标项目:[").append(tpName)
						.append("]文件路径[").append(szb.getAbsolutePath())
						.append("]，请处理!");
				MsgUtils.send(sb.toString());
				throw new ServiceException("", "读取招标文件失败!", e);
			}
			return target;
		}
	}

	/**
	 * 设置流程信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProject
	 *            招标项目
	 */
	private void setFlowInfo(Record<String, Object> tenderProject)
			throws ServiceException
	{
		// 获取招标项目流程信息
		String bemJson = tenderProject.getString("V_BEM_INFO_JSON");
		if (StringUtils.isEmpty(bemJson))
		{
			// 获取不到评标办法
			throw new ValidateException("", "无法获取招标项目对应的评标办法信息");
		}
		JSONObject bem = JSON.parseObject(bemJson);
		String code = bem.getString("V_CODE");
		// 判断是否有标段组
		Integer flag = tenderProject.getInteger("N_IS_SECTION_GROUP");
		if (null == flag)
		{
			flag = 0;
		}
		Record<String, Object> ps = new RecordImpl<String, Object>();
		ps.setColumn("bemcode", code);
		// 没标段组
		ps.setColumn("group", 0);
		// 有标段组
		if (1 == flag)
		{
			ps.setColumn("group", 1);
		}
		// 查询流程
		Record<String, Object> flow = this.activeRecordDAO.auto()
				.table(TableName.FLOW_INFO)
				.setCondition("AND", "V_BEM_CODE = #{bemcode}")
				.setCondition("AND", "N_IS_SECTION_GROUP = #{group}").get(ps);
		if (CollectionUtils.isEmpty(flow))
		{
			throw new ValidateException("", "无法获取招标项目对应的流程信息");
		}
		SessionUtils.setAttribute(
				ConstantEOKB.TENDER_PROJECT_FLOW_INFO_SESSION_KEY, flow);
	}

	/**
	 * 
	 * 设置开标工具功能是否可用的session信息<br/>
	 * <p>
	 * 设置开标工具功能是否可用的session信息
	 * </p>
	 */
	private void setToolSessionInfo() throws ServiceException
	{
		// 开标视频是否可用
		int videoStatus = SystemParamUtils
				.getInteger(ConstantEOKB.SysParamKey.EOV_VIDEO_STATUS);
		SessionUtils.setAttribute(ConstantEOKB.VIDEO_STATUS_SESSION_KEY,
				videoStatus);

		// 大师的即时通讯聊天是否可用
		int imStatus = SystemParamUtils
				.getInteger(ConstantEOKB.SysParamKey.EOKB_IM_STATUS);
		SessionUtils.setAttribute(ConstantEOKB.IM_STATUS_SESSION_KEY, imStatus);

		// 云视睿博聊天是否可用
		int ntvStatus = SystemParamUtils
				.getInteger(ConstantEOKB.SysParamKey.EOKB_NTV_STATUS);
		SessionUtils.setAttribute(ConstantEOKB.NTV_STATUS_SESSION_KEY,
				ntvStatus);

		if (ntvStatus == 1)
		{
			String ntvUrl = SystemParamUtils
					.getString(ConstantEOKB.SysParamKey.EOKB_NTV_URL);
			SessionUtils.setAttribute(ConstantEOKB.NTV_URL_SESSION_KEY, ntvUrl);
		}

		// 电子唱标是否可用
		int singStatus = SystemParamUtils
				.getInteger(ConstantEOKB.SysParamKey.EOKB_SING_STATUS);
		SessionUtils.setAttribute(ConstantEOKB.SING_STATUS_SESSION_KEY,
				singStatus);

		// 结束开标按钮是否可显示
		int reEnd = SystemParamUtils
				.getInteger(ConstantEOKB.SysParamKey.EOKB_RE_END_STATUS);
		SessionUtils
				.setAttribute(ConstantEOKB.RE_END_STATUS_SESSION_KEY, reEnd);

		// 获取开标状态
		SessionUtils.setAttribute(ConstantEOKB.BID_OPEN_STATUS_SESSION_KEY,
				getBidOpenStatus());
	}

	/**
	 * 
	 * 获取开标状态<br/>
	 * <p>
	 * 获取开标状态
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private boolean getBidOpenStatus() throws ServiceException
	{
		logger.debug(LogUtils.format("", "获取开标状态"));
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		Record<String, Object> section = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.get(param);
		// 为空则说明全部流标
		if (CollectionUtils.isEmpty(section))
		{
			return false;
		}
		// 开标结束
		if (StringUtils.equals("2", section.getString("V_BID_OPEN_STATUS")))
		{
			return true;
		}
		// 开标进行中
		return false;
	}

	/**
	 * 进入项目-投标人<br/>
	 * <p>
	 * 进入项目-投标人
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/tbrgo/{tpid}", desc = "进入项目-投标人")
	@HttpMethod(HttpMethod.GET)
	public void initProjectInfoByBidder(@PathParam("tpid") String tpid,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogFormatUtils.formatOperateMessage("", "进入项目-投标人", data));
		Record<String, Object> tenderProject = this.activeRecordDAO.auto()
				.table(ConstantEOKB.TableName.EKB_T_TENDER_PROJECT_INFO)
				.get(tpid);
		if (CollectionUtils.isEmpty(tenderProject))
		{
			// 获取不到项目信息
			throw new ValidateException("E-1003");
		}
		// 获取开标时间
		String openTime = tenderProject.getString("V_BIDOPEN_TIME");
		if (StringUtils.isEmpty(openTime))
		{
			throw new ServiceException("", "招标项目开标时间为空!");
		}
		Date opTime = DateUtils.parseDate(openTime, "yyyy-MM-dd HH:mm:ss");
		if (opTime.getTime() > System.currentTimeMillis())
		{
			throw new ServiceException("", "招标项目开标时间尚未开始,请耐心等待!");
		}
		// 清空session 信息
		ApacheShiroUtils.getSession().removeAttribute(
				ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY);
		// 设置SESSION信息
		SessionUtils.setAttribute(ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY,
				tenderProject);
		// 设置流程信息
		setFlowInfo(tenderProject);

		String tpType = tenderProject.getString("V_TENDERPROJECT_APP_TYPE");
		// 如果是房建市政普通
		if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE, tpType)
				|| StringUtils.equals(
						ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE,
						tpType)
				|| StringUtils.equals(
						ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_PTGL_TYPE,
						tpType))
		{
			String pc = tenderProject.getString("V_TENDER_PROJECT_CODE");
			// 获取招标文件
			File szb = new File(
					SystemParamUtils
							.getProperty(SysParamKey.EBIDKB_FILE_PATH_URL),
					pc + "/" + pc + ".szb");
			File dir = unpackTenderDoc(szb);
			if (null == dir || !dir.exists())
			{
				throw new ServiceException("", "无法获取到招标项目的招标文件!");
			}
			// 设置评标办法
			JSONObject pbMethod = getPBMethodJsonObject(dir, tenderProject);
			// 设置评标办法JSON
			SessionUtils.setAttribute(
					ConstantEOKB.PB_METHOD_JSON_INFO_SESSION_KEY, pbMethod);

			// IS_APPRAISAL_PRICE_RATE=“1” 是采用费率 否则采用投标报价
			SessionUtils.setAttribute(
					ConstantEOKB.IS_APPRAISAL_PRICE_RATE,
					StringUtils.equals("1",
							pbMethod.getString("IS_APPRAISAL_PRICE_RATE")));
		}

		// 设置开标工具功能是否可用的session信息
		setToolSessionInfo();
	}
}
