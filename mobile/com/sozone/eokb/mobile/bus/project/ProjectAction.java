/**
 * 包名：com.sozone.eokb.mobile.bus.project
 * 文件名：ProjectAction.java<br/>
 * 创建时间：2018-11-19 下午12:47:56<br/>
 * 创建者：wengdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.bus.project;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jxl.Sheet;
import jxl.Workbook;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.util.ClassLoaderUtils;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.LogFormatUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.HttpClientUtils;
import com.sozone.eokb.utils.ListSortUtils;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.utils.TenderProjectParamUtils;

/**
 * 招标项目<br/>
 * <p>
 * 招标项目<br/>
 * </p>
 * Time：2018-11-19 下午12:47:56<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/mobile/project", desc = "开标项目信息服务类")
// 登录即可访问
@Permission(Level.Authenticated)
public class ProjectAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(ProjectAction.class);
	/**
	 * 
	 * 缓存
	 */
	private static Map<String, Map<String, Record<String, Object>>> bidUrlConfig = null;

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
	 * 获取投标人可以参与开标的招标项目信息列表<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/list", desc = "获取投标人可以参与开标的招标项目信息列表")
	@Service
	@Permission(Level.Guest)
	public List<Record<String, Object>> getTenderProjectList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取投标人可以参与开标的招标项目信息列表", data));
		Record<String, Object> param = data.getRecord();
		String kbTime = param.getString("kbTime");

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

		// 列表信息要从接口获取，接口不能抛异常
		List<Record<String, Object>> projectList = new ArrayList<Record<String, Object>>();

		// 交通项目列表
		List<Record<String, Object>> jtProjectList = null;
		// 房建项目列表
		List<Record<String, Object>> fjszProjectList = null;
		try
		{
			jtProjectList = getOpenBidProList(null);
		}
		catch (Exception e)
		{
			logger.error("同步交通开标项目异常", e);
		}
		if (null != jtProjectList)
		{
			projectList.addAll(jtProjectList);
		}

		try
		{
			fjszProjectList = getOpenBidProList("A01");
		}
		catch (Exception e)
		{
			logger.error("同步房建市政项目异常", e);
		}
		if (null != fjszProjectList)
		{
			projectList.addAll(fjszProjectList);
		}
		// 开标时间升序
		ListSortUtils.sort(projectList, true, "V_BIDOPEN_TIME");

		// 列表信息要从接口获取
		projectList = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO)
				.setCondition("AND",
						"V_BIDOPEN_TIME <=#{end} AND V_BIDOPEN_TIME>=#{start} ")
				.list(params);

		getProjectStatus(projectList);

		for (Record<String, Object> p : projectList)
		{
			List<Record<String, Object>> list = new LinkedList<Record<String, Object>>();
			Record<String, Object> record = new RecordImpl<String, Object>();
			record.setColumn("label", "开标时间");
			record.setColumn("value", p.getString("V_BIDOPEN_TIME"));
			list.add(record);

			p.put("PROJECT_INFOS", list);
		}
		return projectList;
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
	 * 获取流程主键<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProject
	 *            招标项目
	 * @return flowID
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static String getFlowID(Record<String, Object> tenderProject)
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
		// 查询流程
		Record<String, Object> flow = ActiveRecordDAOImpl.getInstance()
				.pandora().SELECT("ID").FROM(TableName.FLOW_INFO)
				.EQUAL("V_BEM_CODE", code).EQUAL("N_IS_SECTION_GROUP", flag)
				.get();
		if (CollectionUtils.isEmpty(flow))
		{
			throw new ServiceException("", "无法获取到对应的开标流程");
		}
		return flow.getString("ID");
	}

	/**
	 * 
	 * 获取开标项目信息 标段信息 招标文件 补遗文件<br/>
	 * <p>
	 * 获取开标项目信息 标段信息 招标文件 补遗文件
	 * </p>
	 * 
	 * @param type
	 *            类型
	 * @return 招标项目列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	public List<Record<String, Object>> getOpenBidProList(String type)
			throws FacadeException
	{
		// 获取Token请求头信息
		Record<String, String> tokenParam = HttpClientUtils.getHeadMapOfToken();
		// 获取招标项目信息路径
		String url = "http://test-ebid-api.okap.com/authorize/api/mobile/ekb/ist";
		if (StringUtils.isNotEmpty(type))
		{
			// url = "http://117.25.161.106:6065/EDE/authorize/phone/ekb/list";
			url = "http://117.25.161.106:6065/EDE/authorize/phone/ekb/list";
		}

		Record<String, String> params = new RecordImpl<String, String>();
		// params.setColumn("OPENING_BID_TIME",
		// DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
		params.setColumn("OPENING_BID_TIME", "2018-12-28");
		// 招标项目信息列表JSON
		String result = null;
		try
		{
			// 请求招标项目信息
			result = HttpClientUtils.doGet(url, params, tokenParam,
					ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (Exception e)
		{
			throw new ServiceException("E-2000", "获取开标列表失败!", e);
		}
		JSONObject robj = JSON.parseObject(result);
		boolean success = robj.getBoolean("success");
		if (!success)
		{
			throw new ServiceException(robj.getString("errorCode"),
					robj.getString("errorDesc"));
		}
		// 招标项目信息列表
		JSONArray tpinfos = robj.getJSONArray("rows");
		// 如果没有招标项目直接返回空的
		if (null == tpinfos || tpinfos.isEmpty())
		{
			return null;
		}

		return parseResult(tpinfos, type);
	}

	/**
	 * 
	 * 解析项目列表<br/>
	 * <p>
	 * 解析项目列表
	 * </p>
	 * 
	 * @param tpinfos
	 * @param type
	 * @return
	 * @throws FacadeException
	 */
	private List<Record<String, Object>> parseResult(JSONArray tpinfos,
			String type) throws FacadeException
	{
		logger.debug(LogUtils.format("解析项目列表", tpinfos, type));
		Record<String, String> params = new RecordImpl<String, String>();

		// 项目列表
		List<Record<String, Object>> tenderProjectInfos = new ArrayList<Record<String, Object>>();
		Record<String, Object> tenderProject = null;
		JSONObject section = null;
		JSONObject tpinfo = null;

		// 项目类型
		String appType = null;
		// 开标地址
		String url = null;
		// 迭代处理
		for (int i = 0; i < tpinfos.size(); i++)
		{
			tpinfo = tpinfos.getJSONObject(i);
			tenderProject = new RecordImpl<String, Object>();
			tenderProject.setColumn("V_BIDOPEN_TIME",
					tpinfo.getString("V_BIDOPEN_TIME"));
			tenderProject.setColumn("V_TENDER_PROJECT_NAME",
					tpinfo.getString("V_TENDER_PROJECT_NAME"));
			tenderProject.setColumn("V_TENDER_PROJECT_ID",
					tpinfo.getString("V_TENDER_PROJECT_ID"));

			// 判断开标地址
			appType = tpinfo.getString("TENDERPROJECT_APP_TYPE");

			// 不是房建和市政类型，按照项目开标
			if (!StringUtils.equals("A01", appType)
					&& !StringUtils.equals("A02", appType))
			{
				url = getBidOpenUrl(tpinfo);
				tenderProject.setColumn("URL", url);
				tenderProjectInfos.add(tenderProject);
				continue;
			}

			// 房建市政的项目名称需要特殊处理
			params.setColumn("tenderProjectId",
					tpinfo.getString("V_TENDER_PROJECT_ID"));
			params.setColumn("REVIEWSORT", tpinfo.getString("REVIEWSORT"));
			JSONArray sections = getSectionInfoList(params, type);

			for (int j = 0; j < sections.size(); j++)
			{
				section = sections.getJSONObject(j);
				tenderProject.setColumn("V_BID_SECTION_ID",
						section.getString("V_SECTION_ID"));
				tenderProject.setColumn("V_TENDER_PROJECT_NAME",
						tpinfo.getString("V_TENDER_PROJECT_NAME") + "-标段:["
								+ section.getString("V_SECTION_NAME") + "]");
				// 是小项目
				url = "http://ebid-online-kb.okap.com";
				tenderProject.setColumn("URL", url);
				// 不是小项目
				if (!StringUtils.equals("1",
						tpinfo.getString("SMALL_PROJECT_IDENTIFICATION")))
				{
					getBidOpenUrl(tenderProject, tpinfo,
							section.getString("V_SECTION_ID"));
				}
				tenderProjectInfos.add(tenderProject);
			}
		}
		return tenderProjectInfos;
	}

	/**
	 * 
	 * 获取开标地址（房建市政）<br/>
	 * <p>
	 * 获取开标地址（房建市政）
	 * </p>
	 * 
	 * @param projectInfo
	 *            项目信息
	 * @return
	 * @throws FacadeException
	 */
	private void getBidOpenUrl(Record<String, Object> projectRecord,
			JSONObject projectInfo, String sectionID) throws FacadeException
	{
		logger.debug(LogUtils.format("获取开标地址", projectInfo));

		// 项目类型
		String appType = projectInfo.getString("TENDERPROJECT_APP_TYPE");

		Map<String, Record<String, Object>> urlMap = recordsConfigInfo().get(
				appType);

		// 开标室
		JSONArray rooms = projectInfo.getJSONArray("ROOMNAME");
		// 未分配开标室
		if (CollectionUtils.isEmpty(rooms))
		{
			projectRecord.setColumn("URL", "");
			return;
		}
		// 开标室信息
		JSONObject room = null;
		// 开标室对应的地址信息
		Record<String, Object> urlRecord = null;
		for (int i = 0; i < rooms.size(); i++)
		{
			room = rooms.getJSONObject(i);
			if (StringUtils.equals(sectionID, room.getString("BIDSECTIONID")))
			{
				urlRecord = urlMap.get(room.getString("SITEA"));
				if (CollectionUtils.isEmpty(urlRecord))
				{
					continue;
				}
				projectRecord.setColumn("URL", urlRecord.getString("BID_URL"));

				// 摄像头列表
				projectRecord.setColumn("VIDEOS", urlRecord.getList("VIDEOS"));
			}
		}
	}

	/**
	 * 
	 * 获取开标地址（高速普通水运）<br/>
	 * <p>
	 * 获取开标地址（高速普通水运）
	 * </p>
	 * 
	 * @param projectInfo
	 *            项目信息
	 * @return
	 * @throws FacadeException
	 */
	private String getBidOpenUrl(JSONObject projectInfo) throws FacadeException
	{
		logger.debug(LogUtils.format("获取开标地址", projectInfo));

		// 项目类型
		String appType = projectInfo.getString("TENDERPROJECT_APP_TYPE");
		// 交易场所
		String area = projectInfo.getString("V_JYCS");

		Map<String, Record<String, Object>> urlMap = recordsConfigInfo().get(
				appType);
		if (null == urlMap)
		{
			throw new FacadeException("", "获取开标地址失败");
		}

		Record<String, Object> urlRecord = urlMap.get(area);
		if (CollectionUtils.isEmpty(urlRecord))
		{
			return "";
		}
		return urlRecord.getString("BID_URL");
	}

	/**
	 * 
	 * @return 获取开标记录配置信息
	 */
	public static Map<String, Map<String, Record<String, Object>>> recordsConfigInfo()
	{
		if (CollectionUtils.isEmpty(bidUrlConfig))
		{
			bidUrlConfig = new HashMap<String, Map<String, Record<String, Object>>>();
		}
		else
		{
			return bidUrlConfig;
		}
		Workbook rwb = null;
		// 摄像头列表
		List<Record<String, Object>> videos = null;

		// 摄像头信息
		Record<String, Object> video = null;
		Record<String, Object> urlRecord = null;

		String areaName = null;
		// 评标办法对应开标记录列表
		Map<String, Record<String, Object>> map = null;
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
					String tenderProjectType = rs.getCell(0, i).getContents();
					// 处理合并单元格只有在第一行有获取到问题
					if (StringUtils.isEmpty(tenderProjectType))
					{
						tenderProjectType = tenderProjectTypeTemp;
					}
					else
					{
						tenderProjectTypeTemp = tenderProjectType;
					}

					if (null == bidUrlConfig.get(tenderProjectType))
					{
						map = new HashMap<String, Record<String, Object>>();
					}
					else
					{
						map = bidUrlConfig.get(tenderProjectType);
					}

					if (!StringUtils.isEmpty(rs.getCell(1, i).getContents()))
					{
						areaName = rs.getCell(1, i).getContents();
					}
					if (null == map.get(areaName))
					{
						urlRecord = new RecordImpl<String, Object>();
						// 开标地址
						urlRecord.setColumn("BID_URL", rs.getCell(2, i)
								.getContents());
					}
					else
					{
						urlRecord = map.get(areaName);
					}

					// 视频列表
					if (null == urlRecord.getList("VIDEOS"))
					{
						videos = new ArrayList<Record<String, Object>>();
					}
					else
					{
						videos = urlRecord.getList("VIDEOS");
					}
					video = new RecordImpl<String, Object>();
					// 视频地址
					video.setColumn("src", rs.getCell(3, i).getContents());
					// 视频名称
					video.setColumn("name", rs.getCell(4, i).getContents());
					if (StringUtils.isNotEmpty(video.getString("src"))
							&& StringUtils.isNotEmpty(video.getString("name")))
					{
						videos.add(video);
					}
					urlRecord.setColumn("VIDEOS", videos);

					map.put(areaName, urlRecord);
					bidUrlConfig.put(tenderProjectType, map);
				}
			}
			return bidUrlConfig;
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
		return bidUrlConfig;
	}

	/**
	 * @return 获取开标设置配置信息
	 */
	private static File getConfigFile()
	{
		String xsdPath = "com/sozone/eokb/mobile/bus/project/EKB_URL.xls";
		URL url = ClassLoaderUtils.getResource(xsdPath, ProjectAction.class);
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

	/**
	 * 获取标段列表信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param params
	 * @param type
	 * @return
	 * @throws ServiceException
	 */
	private static JSONArray getSectionInfoList(Record<String, String> params,
			String type) throws ServiceException
	{
		// 获取Token请求头信息
		Record<String, String> tokenParam = HttpClientUtils.getHeadMapOfToken();
		// 获取标段请求URL
		String url = TenderProjectParamUtils.getSystemParamValue(
				SysParamKey.EDE_SECTION_LIST_URL, type);
		String json = null;
		try
		{
			json = HttpClientUtils.doGet(url, params, tokenParam,
					ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (Exception e)
		{
			throw new ServiceException("E-2000", "获取标段信息列表失败!", e);
		}
		JSONObject sectionObj = JSON.parseObject(json);
		boolean success = sectionObj.getBoolean("success");
		if (!success)
		{
			throw new ServiceException(sectionObj.getString("errorCode"),
					sectionObj.getString("errorDesc"));
		}
		// 获取标段信息JSON
		JSONArray sectionInfos = sectionObj.getJSONArray("rows");

		return sectionInfos;
	}

	/**
	 * 获取项目信息<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 项目信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/info", desc = "获取项目信息")
	@Service
	public Record<String, Object> getTenderProjectInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取投标人可以参与开标的招标项目信息列表", data));
		Record<String, Object> params = new RecordImpl<String, Object>();

		params.setColumn("tenderProjectID", data.getParam("tenderProjectID"));
		params.setColumn("sectionID", data.getParam("sectionID"));
		params.setColumn("orgcode", SessionUtils.getCompanyCode());
		Record<String, Object> project = this.activeRecordDAO.statement()
				.selectOne("mobile.getTenderProjectListOfMobile", params);

		if (CollectionUtils.isEmpty(project))
		{
			throw new FacadeException("", "未获取到项目信息，请稍后重试！");
		}

		// 是否签到标识
		boolean hasSignin = true;
		if (StringUtils.isEmpty(project.getString("V_SIGN_IN_TIME")))
		{
			hasSignin = false;
		}
		List<Record<String, Object>> list = new LinkedList<Record<String, Object>>();
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("label", "招标项目编号");
		record.setColumn("value", project.getString("V_INVITENO"));
		list.add(record);
		record = new RecordImpl<String, Object>();
		record.setColumn("label", "招标项目名称");
		record.setColumn("value", project.getString("V_TENDER_PROJECT_NAME"));
		list.add(record);
		record = new RecordImpl<String, Object>();
		record.setColumn("label", "评标办法");
		record.setColumn("value", project.getJSONObject("V_BEM_INFO_JSON")
				.getString("V_BID_EVALUATION_METHOD_NAME"));
		list.add(record);
		record = new RecordImpl<String, Object>();
		record.setColumn("label", "招标人");
		record.setColumn("value", project.getString("V_TENDERER_NAME"));
		list.add(record);
		record = new RecordImpl<String, Object>();
		record.setColumn("label", "签到时间");
		record.setColumn("value",
				hasSignin ? project.getString("V_SIGN_IN_TIME") : "未签到");
		list.add(record);
		record = new RecordImpl<String, Object>();
		record.setColumn("label", "开标时间");
		record.setColumn("value", project.getString("V_BIDOPEN_TIME"));
		list.add(record);

		project.put("PROJECT_INFOS", list);
		project.put("HAS_SIGNIN", hasSignin);
		return project;
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
	 * @return 流程主键
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/into/{tpid}", desc = "进入项目-投标人")
	@HttpMethod(HttpMethod.GET)
	public String initProjectInfoByBidder(@PathParam("tpid") String tpid,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogFormatUtils.formatOperateMessage("", "进入项目-投标人", data));
		Record<String, Object> tenderProject = this.activeRecordDAO
				.pandora()
				.SELECT_ALL_FROM(
						ConstantEOKB.TableName.EKB_T_TENDER_PROJECT_INFO)
				.EQUAL("ID", tpid).get();
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

		// String tpType = tenderProject.getString("V_TENDERPROJECT_APP_TYPE");
		// // 如果是房建市政普通
		// if (StringUtils.equals(
		// ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE, tpType)
		// || StringUtils.equals(
		// ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE,
		// tpType)
		// || StringUtils.equals(
		// ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_PTGL_TYPE,
		// tpType))
		// {
		// String pc = tenderProject.getString("V_TENDER_PROJECT_CODE");
		// // 获取招标文件
		// File szb = new File(
		// SystemParamUtils
		// .getProperty(SysParamKey.EBIDKB_FILE_PATH_URL),
		// pc + "/" + pc + ".szb");
		// File dir = ProjectUtils.unpackTenderDoc(szb);
		// if (null == dir || !dir.exists())
		// {
		// throw new ServiceException("", "无法获取到招标项目的招标文件!");
		// }
		// ProjectUtils.getPBMethodJsonObject(dir, tenderProject);
		// }

		// 设置流程信息
		return getFlowID(tenderProject);
	}
}
