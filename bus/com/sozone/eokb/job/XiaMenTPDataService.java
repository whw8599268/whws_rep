/**
 * 包名：com.sozone.eokb.job
 * 文件名：XiaMenTPDataService.java<br/>
 * 创建时间：2018-2-27 上午10:42:05<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.job;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.push.BaValidationKit;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.HttpClientUtils;
import com.sozone.eokb.utils.InterfaceFieldMappingUtils;
import com.sozone.eokb.utils.MsgUtils;
import com.sozone.eokb.utils.TenderProjectParamUtils;

/**
 * 厦门房建市政开评标数据同步服务接口<br/>
 * <p>
 * 厦门房建市政开评标数据同步服务接口<br/>
 * </p>
 * Time：2018-2-27 上午10:42:05<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class XiaMenTPDataService
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(XiaMenTPDataService.class);

	/**
	 * 根据操作系统 文件路劲 "/" 符号
	 */
	private final static String TAG = "/";

	/**
	 * 同步开标数据<br/>
	 * <p>
	 * 同步开标数据
	 * </p>
	 * 
	 * @param openTime
	 *            开标时间
	 * @param tenderProType
	 *            招标项目类
	 * @param tenderProName
	 *            类型名称
	 * @param areaCode
	 *            开标地区
	 * @param roomNO
	 *            会议室
	 * @param tpids
	 *            招标项目ID数组字符串
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static void synchronizeData(String openTime, String tenderProType,
			String tenderProName, String areaCode, String roomNO, String tpids)
			throws ServiceException
	{
		ActiveRecordDAO dao = ActiveRecordDAOImpl.getInstance();
		try
		{
			Record<String, String> params = new RecordImpl<String, String>();
			params.setColumn("OPENING_BID_TIME", openTime);
			params.setColumn("APP_TYPE", tenderProType);
			// 开标区域
			params.setColumn("AREA_CODE", areaCode);
			// 会议室
			params.setColumn("SITEA", roomNO);
			// 如果ID存在
			if (StringUtils.isNotEmpty(tpids))
			{
				params.setColumn("TENDER_PROJECT_IDS", tpids);
			}
			// 获取开标项目信息
			List<Record<String, Object>> tenderProjects = getOpenBidProList(
					params, tenderProType, dao);
			logger.debug(LogUtils.format("获取招标项目信息:", tenderProjects));
			if (CollectionUtils.isEmpty(tenderProjects))
			{
				logger.warn(LogUtils.format(openTime + ":无开标项目信息!"));
				return;
			}
			// 标段
			List<Record<String, Object>> sections = null;
			Record<String, String> param = new RecordImpl<String, String>();
			Record<String, Object> tempProject = null;
			String tpid = null;
			// 迭代项目
			for (Record<String, Object> tpInfo : tenderProjects)
			{
				param.clear();
				param.setColumn("tenderProjectId",
						tpInfo.getString("V_TENDER_PROJECT_ID"));
				param.setColumn("REVIEWSORT", tpInfo.getString("N_BID_ORDER"));
				// 获取招标项目的标段信息
				sections = getSectionInfoList(param, tenderProType);
				logger.debug(LogUtils.format("获取招标项目标段信息:", sections));
				// 迭代标段
				for (Record<String, Object> section : sections)
				{
					logger.debug(LogUtils.format("开始拆分招标项目:", sections));
					// 开始把招标项目拆掉
					tpid = Random.generateUUID();
					tempProject = new RecordImpl<String, Object>();
					tempProject.putAll(tpInfo);
					tempProject.setColumn("ID", tpid);
					// 设置标段ID
					tempProject.setColumn("V_BID_SECTION_ID",
							section.getString("V_BID_SECTION_ID"));
					// 编号
					tempProject.setColumn("V_TENDER_PROJECT_CODE",
							section.getString("V_BID_SECTION_CODE"));
					// 设置开标时间
					tempProject.setColumn("V_BIDOPEN_TIME",
							section.getString("V_BIDOPEN_TIME"));
					// 设置项目名称
					tempProject.setColumn("V_TENDER_PROJECT_NAME",
							tpInfo.getString("V_TENDER_PROJECT_NAME") + "-标段:["
									+ section.getString("V_BID_SECTION_NAME")
									+ "]");
					logger.debug(LogUtils.format("拆分的招标项目:", tempProject));
					// 保存招标项目
					dao.auto().table(TableName.EKB_T_TENDER_PROJECT_INFO)
							.save(tempProject);
					// 设置标段信息
					section.setColumn("V_TPID", tpid);
					logger.debug(LogUtils.format("拆分的标段:", section));

					// 下载招标文件
					downloadTenderDocumentFile(
							tpInfo.getString("V_TENDER_PROJECT_ID"),
							section.getString("V_BID_SECTION_CODE"),
							tenderProType);
					// 下载补遗文件
					downloadAddendumFile(
							tpInfo.getString("V_TENDER_PROJECT_ID"),
							section.getString("V_BID_SECTION_ID"),
							section.getString("V_BID_SECTION_CODE"),
							tenderProType);
					// 下载控制价文件
					downloadPriceFile(tpInfo.getString("V_TENDER_PROJECT_ID"),
							section.getString("V_BID_SECTION_ID"),
							section.getString("V_BID_SECTION_CODE"),
							tenderProType);

					// 获取控制价，暂列金额总和，专业工程暂估价总和
					section.setColumn("V_JSON_OBJ",
							getPrice(section.getString("V_BID_SECTION_CODE")));
					// 保存标段信息
					dao.auto().table(TableName.EKB_T_SECTION_INFO)
							.save(section);
				}

			}

		}
		catch (ServiceException e)
		{
			MsgUtils.send("开标项目同步：失败，" + tenderProName + "，请处理!");
			logger.error(LogUtils.format("获取开标项目数据失败!"), e);
			throw e;
		}
		catch (Exception e)
		{
			// 招标项目名称
			MsgUtils.send("开标项目同步：失败，" + tenderProName + "，请处理!");
			logger.error(LogUtils.format("获取开标项目数据失败!"), e);
			throw new ServiceException("", "获取开标项目数据失败!", e);
		}
	}

	/**
	 * 获取标段的控制价描述<br/>
	 * <p>
	 * </p>
	 * 
	 * @param bidSectionCode
	 *            标段编号
	 * @return
	 * @throws ServiceException
	 */
	private static String getPrice(String bidSectionCode)
			throws ServiceException
	{
		try
		{
			String kpFileSystemPath = SystemParamUtils
					.getString(SysParamKey.EBIDKB_FILE_PATH_URL);
			String controlPricePath = kpFileSystemPath + bidSectionCode
					+ "/控制价/控制价文件描述.json";
			String xmlFilePath = getControlPriceXmlFile(controlPricePath,
					bidSectionCode);
			if (StringUtils.isEmpty(xmlFilePath))
			{
				return null;
			}
			Map<String, Double> prices = BaValidationKit.getPrices(xmlFilePath);
			JSONObject jobj = new JSONObject();
			jobj.putAll(prices);
			return jobj.toJSONString();
		}
		catch (Exception e)
		{
			throw new ServiceException("", "读取控制价XML文件发生异常!", e);
		}
	}

	/**
	 * 获取控制价xml文件路径<br/>
	 * 
	 * @param controlPricePath
	 * @param systemInspectPath
	 * @param bidSectionCode
	 */
	private static String getControlPriceXmlFile(String controlPricePath,
			String bidSectionCode)
	{
		try
		{
			File jsonFile = new File(controlPricePath);
			if (!jsonFile.exists())
			{
				return null;
			}
			String jsonStr = FileUtils.readFileToString(jsonFile, "UTF-8");
			JSONArray array = JSON.parseArray(jsonStr);
			for (int i = 0; i < array.size(); i++)
			{
				JSONObject object = array.getJSONObject(i);
				JSONArray fileJsonArray = object.getJSONArray("ADDENDUM_LIST");
				if (null == fileJsonArray || fileJsonArray.size() < 1)
				{
					continue;
				}
				String bidSecTionCodeJSON = object
						.getString("BID_SECTION_CODE");
				if (!StringUtils.equals(bidSectionCode, bidSecTionCodeJSON))
				{
					continue;
				}

				for (int j = 0; j < fileJsonArray.size(); j++)
				{
					JSONObject object2 = fileJsonArray.getJSONObject(j);
					String attachTypeID = object2.getString("V_ATTACH_TYPE_ID");
					if ("9171".equals(attachTypeID))
					{
						return object2.getString("FILE_PATH");
					}
				}
			}
		}
		catch (Exception e)
		{
			return null;
		}
		return null;
	}

	/**
	 * 下载招标文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @throws ServiceException
	 */
	private static void downloadTenderDocumentFile(String tenderProjectID,
			String tednerProjectCode, String type) throws ServiceException
	{
		// 获取头部Token
		Record<String, String> headMap = HttpClientUtils.getHeadMapOfToken();
		// 招标文件获取请求路径拼接
		String url = TenderProjectParamUtils.getSystemParamValue(
				SysParamKey.EDE_DOWNLOAD_TENDER_FILE_URL, type)
				+ "/"
				+ tenderProjectID;
		// 保存的路径
		String savePath = SystemParamUtils
				.getString(SysParamKey.EBIDKB_FILE_PATH_URL)
				+ tednerProjectCode;
		// 保存到本地的招标文件名称
		String fileName = tednerProjectCode + ".szb";
		logger.debug(LogUtils.format("下载招标文件:", url, headMap, savePath,
				fileName));
		try
		{
			HttpClientUtils.doFileDownLoad(url, null, headMap, savePath,
					fileName);
		}
		catch (Exception e)
		{
			throw new ServiceException("", "获取招标文件失败");
		}
	}

	/**
	 * 
	 * 获取开标项目信息 标段信息 招标文件 补遗文件<br/>
	 * <p>
	 * 获取开标项目信息 标段信息 招标文件 补遗文件
	 * </p>
	 * 
	 * @param params
	 *            开标时间,开标地点
	 * @param type
	 *            类型
	 * @param dao
	 *            ActiveRecordDAO
	 * @return 招标项目列表
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static List<Record<String, Object>> getOpenBidProList(
			Record<String, String> params, String type, ActiveRecordDAO dao)
			throws ServiceException
	{
		// 获取Token请求头信息
		Record<String, String> tokenParam = HttpClientUtils.getHeadMapOfToken();
		// 获取招标项目信息路径
		String url = TenderProjectParamUtils.getSystemParamValue(
				SysParamKey.EDE_OPENID_LIST_URL_KEY, type);
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
		List<Record<String, Object>> tenderProjectInfos = new ArrayList<Record<String, Object>>();
		Record<String, Object> tenderProject = null;
		JSONObject tpinfo = null;
		// 临时变量
		Record<String, Object> temp = null;
		JSONObject jobj = null;
		// 扩展信息
		JSONObject ext = null;
		// 预审标记
		boolean flag = false;
		// 迭代处理
		for (int i = 0; i < tpinfos.size(); i++)
		{
			tpinfo = tpinfos.getJSONObject(i);
			tenderProject = new RecordImpl<String, Object>();
			temp = new RecordImpl<String, Object>();
			temp.putAll(tpinfo);
			// 将交易平台传过来的招标项目信息转换成本地招标项目信息字段
			tenderProject.putAll(InterfaceFieldMappingUtils
					.FiledlocalToInterFace(temp,
							TableName.EKB_T_TENDER_PROJECT_INFO));
			ext = new JSONObject();
			// N_TP_TYPE 招标项目是预审还是后审标记，1表示该招标项目资格审查类型为预审，2表示后审
			// N_BID_ORDER 预审的开标顺序，1表示该记录为预审开标，2表示正常的开标
			// 是否为预审
			flag = StringUtils
					.equals("1", tenderProject.getString("N_TP_TYPE"));
			ext.put("IS_PRETRIAL", flag);
			ext.put("N_BID_ORDER", tenderProject.get("N_BID_ORDER"));
			ext.put("N_TP_TYPE", tenderProject.getString("N_TP_TYPE"));
			// 招标文件开始发出日期
			ext.put("TP_DOC_START_TIME",
					tenderProject.getString("TP_DOC_START_TIME"));

			// 为了页面字符串装json不报错，把后面增加的投标文件配置干掉
			jobj = JSON.parseObject(tenderProject.getString("V_BEM_INFO_JSON"));
			jobj.remove("V_JSON");
			// 如果是预审
			if (flag)
			{
				jobj.put("V_CODE", jobj.getString("V_CODE") + "_"
						+ tenderProject.get("N_BID_ORDER"));
			}
			// --------------------------------
			// 判断小项目
			flag = StringUtils.equals("1",
					tenderProject.getString("N_IS_SMALL_TP"));
			ext.put("IS_SMALL_TP", flag);
			tenderProject.setColumn("V_JSON_OBJ", ext.toJSONString());

			tenderProject.setColumn("V_BEM_INFO_JSON", jobj.toJSONString());
			tenderProject.put("ID", SZUtilsID.getUUID());
			tenderProject.put("V_CREATE_USER", "JOB");
			tenderProject.put("N_CREATE_TIME", System.currentTimeMillis());
			tenderProject.put("V_STATUS", "0");
			tenderProjectInfos.add(tenderProject);
		}
		return tenderProjectInfos;
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
	private static List<Record<String, Object>> getSectionInfoList(
			Record<String, String> params, String type) throws ServiceException
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
		JSONArray sectionInfo = sectionObj.getJSONArray("rows");
		// 返回的标段信息
		List<Record<String, Object>> sectionsResuslt = new ArrayList<Record<String, Object>>();
		// 如果没有标段信息返回空的
		if (null == sectionInfo || sectionInfo.isEmpty())
		{
			return sectionsResuslt;
		}
		// 缓存
		Record<String, Object> temp = new RecordImpl<String, Object>();

		JSONObject section = null;
		Record<String, Object> record = null;
		for (int i = 0; i < sectionInfo.size(); i++)
		{
			temp.clear();
			record = new RecordImpl<String, Object>();
			// 接口字段名称转本地字段名称
			section = sectionInfo.getJSONObject(i);
			temp.putAll(section);
			record.putAll(InterfaceFieldMappingUtils.FiledlocalToInterFace(
					temp, TableName.EKB_T_SECTION_INFO));
			record.setColumn("ID", SZUtilsID.getUUID());
			record.setColumn("V_CREATE_USER", "JOB");
			record.setColumn("N_CREATE_TIME", System.currentTimeMillis());
			record.setColumn("V_BID_OPEN_STATUS", "0");
			record.setColumn("V_BID_EVALUATION_STATUS", "0");
			record.setColumn("V_TENDER_PROJECT_ID",
					params.getString("tenderProjectId"));
			sectionsResuslt.add(record);
		}
		return sectionsResuslt;
	}

	/**
	 * 根据招标项目编号ID获取补遗文件列表<br/>
	 * 
	 * @param params
	 * 
	 * @throws ServiceException
	 *             ServiceException
	 * @return List<Record<String, Object>>
	 */
	private static List<Record<String, Object>> getAddendumList(
			Record<String, String> params, String type) throws ServiceException
	{
		// 获去头部token
		Record<String, String> headMap = HttpClientUtils.getHeadMapOfToken();
		//
		String url = TenderProjectParamUtils.getSystemParamValue(
				SysParamKey.EDE_DOCU_QUES_FILE_LIST_URL, type);
		logger.debug(LogUtils.format("获取补遗文件列表:", url, headMap, params));
		String json = null;
		try
		{
			json = HttpClientUtils.doGet(url, params, headMap,
					ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("根据招标项目编号ID获取补遗文件列表!"), e);
			throw new ServiceException("E-1008", "根据招标项目编号ID获取补遗文件列表!", e);
		}
		JSONObject jsonObject = (JSONObject) JSON.parse(json);
		boolean success = jsonObject.getBoolean("success");
		if (!success)
		{
			throw new ServiceException(jsonObject.getString("errorCode"),
					jsonObject.getString("errorDesc"));
		}
		JSONArray array = (JSONArray) jsonObject.get("rows");
		List<Record<String, Object>> list = new ArrayList<Record<String, Object>>();
		Record<String, Object> record = null;
		for (int i = 0; null != array && i < array.size(); i++)
		{
			record = new RecordImpl<String, Object>();
			record.putAll((JSONObject) array.get(i));
			list.add(record);
		}
		return list;
	}

	/**
	 * 根据招标项目编号ID获取已发布的控制价列表<br/>
	 * 
	 * @param params
	 * 
	 * @throws ServiceException
	 *             ServiceException
	 * @return List<Record<String, Object>>
	 */
	private static List<Record<String, Object>> getPriceList(
			Record<String, String> params, String type) throws ServiceException
	{
		// 获去头部token
		Record<String, String> headMap = HttpClientUtils.getHeadMapOfToken();
		//
		String url = TenderProjectParamUtils.getSystemParamValue(
				SysParamKey.EDE_PRICE_FILE_LIST_URL, type);
		logger.debug(LogUtils.format("获取控制价文件列表:", url, headMap, params));
		String json = null;
		try
		{
			json = HttpClientUtils.doGet(url, params, headMap,
					ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("根据招标项目编号ID获取控制价文件列表!"), e);
			throw new ServiceException("", "根据招标项目编号ID获取控制价文件列表!", e);
		}
		if (StringUtils.isEmpty(json))
		{
			return null;
		}
		JSONObject jsonObject = (JSONObject) JSON.parse(json);
		boolean success = jsonObject.getBoolean("success");
		if (!success)
		{
			throw new ServiceException(jsonObject.getString("errorCode"),
					jsonObject.getString("errorDesc"));
		}
		JSONArray array = (JSONArray) jsonObject.get("rows");
		List<Record<String, Object>> list = new ArrayList<Record<String, Object>>();
		Record<String, Object> record = null;
		for (int i = 0; null != array && i < array.size(); i++)
		{
			record = new RecordImpl<String, Object>();
			record.putAll((JSONObject) array.get(i));
			list.add(record);
		}
		return list;
	}

	/**
	 * 下载补遗文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectID
	 * @param sectionID
	 * @param tenderProjectCode
	 * @param type
	 * @throws ServiceException
	 */
	private static void downloadAddendumFile(String tenderProjectID,
			String sid, String tenderProjectCode, String type)
			throws ServiceException
	{
		logger.debug(LogUtils.format("根据招标项目ID下载补遗文件！", tenderProjectID, sid,
				tenderProjectCode, type));
		// 请求参数
		Record<String, String> param = new RecordImpl<String, String>();
		param.setColumn("tenderProjectId", tenderProjectID);
		param.setColumn("sectionId", sid);
		// 获取补遗文件列表，该项目下的所有标段包补遗文件
		List<Record<String, Object>> addendums = getAddendumList(param, type);
		// 获取补遗文件
		File jsonFile = new File(
				SystemParamUtils.getString(SysParamKey.EBIDKB_FILE_PATH_URL)
						+ tenderProjectCode + TAG + "补遗" + TAG + "补遗文件描述.json");
		if (CollectionUtils.isEmpty(addendums))
		{
			// 写一个空的
			saveJsonFile(jsonFile, new LinkedList<String>());
			return;
		}
		param.clear();
		// 标段包ID与标段的包补遗描述图
		Map<String, Record<String, Object>> sectionAddendumDescs = new HashMap<String, Record<String, Object>>();
		// 标段补遗描述
		Record<String, Object> addendumDesc = null;
		// 标段编号
		String sectionID = null;
		// 补遗文件描述列表
		List<Record<String, Object>> addendumFileDescs = null;
		// 补遗文件描述
		Record<String, Object> addendumFileDesc = null;
		String fileName = null;
		String savePath = null;
		File addendumFile = null;
		// 迭代补遗文件
		for (Record<String, Object> addendum : addendums)
		{
			sectionID = addendum.getString("V_BID_SECTION_ID");
			// 补遗文件描述
			addendumFileDesc = new RecordImpl<String, Object>();
			// 补遗文件ID
			String attachId = addendum.getString("V_ATTACH_ID");
			// 文件名称
			String attachName = addendum.getString("V_ATTACH_NAME");
			// 标段编号
			String sectionCode = addendum.getString("V_BID_SECTION_CODE");
			// 标段名称
			String sectionName = addendum.getString("V_BID_SECTION_NAME");
			// 招标项目编号
			String tenderProCode = addendum.getString("V_TENDER_PROJECT_CODE");
			// 补遗描述
			addendumDesc = sectionAddendumDescs.get(sectionID);
			// 如果不存在
			if (null == addendumDesc)
			{
				addendumDesc = new RecordImpl<String, Object>();
				addendumDesc.setColumn("BID_SECTION_ID", sectionID);
				addendumDesc.setColumn("BID_SECTION_CODE", sectionCode);
				addendumDesc.setColumn("BID_SECTION_NAME", sectionName);
				sectionAddendumDescs.put(sectionID, addendumDesc);
			}
			// 获取文件描述列表
			addendumFileDescs = addendumDesc.getColumn("ADDENDUM_LIST");
			if (null == addendumFileDescs)
			{
				addendumFileDescs = new LinkedList<Record<String, Object>>();
				addendumDesc.setColumn("ADDENDUM_LIST", addendumFileDescs);
			}
			// 加入原来的所有值
			addendumFileDesc.putAll(addendum);
			// 下载文件
			// 文件名
			fileName = attachId + "." + FilenameUtils.getExtension(attachName);
			savePath = SystemParamUtils
					.getString(SysParamKey.EBIDKB_FILE_PATH_URL)
					+ tenderProCode + TAG + "补遗" + TAG + sectionCode;
			param.setColumn("fileName", fileName);
			param.setColumn("savePath", savePath);
			param.setColumn("attachId", attachId);
			// 下载文件
			addendumFile = getDocuQuesFile(param, type);
			if (addendumFile == null)
			{
				continue;
			}
			// "ADDENDUM_ID":"补遗文件ID",
			addendumFileDesc.setColumn("ADDENDUM_ID", SZUtilsID.getUUID());
			// "ADDENDUM_NAME":"补遗文件名称",
			addendumFileDesc.setColumn("ADDENDUM_NAME", attachName);
			// "FILE_PATH":"文件路径,文件绝对路径例如(D:/fileEbid-kp/招标项目编号/补遗/标段包编号/XXXXXX.doc)",
			addendumFileDesc.setColumn("FILE_PATH", FilenameUtils
					.separatorsToUnix(addendumFile.getAbsolutePath()));
			// "FILE_PATH_BASE64":"文件绝对路径BASE64字符串例如(RDpcZmlsZUViaWQta3Bc5oub5qCH6aG555uu57yW5Y+3XOihpemBl1zooaXpgZfmlofku7bmj4/ov7AuanNvbg==)",
			addendumFileDesc.setColumn("FILE_PATH_BASE64", Base64
					.encodeBase64String(addendumFile.getAbsolutePath()
							.getBytes()));
			addendumFileDescs.add(addendumFileDesc);
		}
		saveJsonFile(jsonFile, sectionAddendumDescs.values());
	}

	/**
	 * 下载补遗文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectID
	 * @param sectionID
	 * @param bidsectionCode
	 * @param type
	 * @throws ServiceException
	 */
	private static void downloadPriceFile(String tenderProjectID, String sid,
			String bidsectionCode, String type) throws ServiceException
	{
		logger.debug(LogUtils.format("根据招标项目ID下载补遗文件！", tenderProjectID, sid,
				bidsectionCode, type));
		// 请求参数
		Record<String, String> param = new RecordImpl<String, String>();
		param.setColumn("tenderProjectId", tenderProjectID);
		param.setColumn("sectionId", sid);
		// 获取补遗文件列表，该项目下的所有标段包补遗文件
		List<Record<String, Object>> priceList = getPriceList(param, type);
		// 获取补遗文件
		File jsonFile = new File(
				SystemParamUtils.getString(SysParamKey.EBIDKB_FILE_PATH_URL)
						+ bidsectionCode + TAG + "控制价" + TAG + "控制价文件描述.json");
		if (CollectionUtils.isEmpty(priceList))
		{
			// 写一个空的
			saveJsonFile(jsonFile, new LinkedList<String>());
			return;
		}
		param.clear();
		// 标段包ID与标段的包补遗描述图
		Map<String, Record<String, Object>> sectionAddendumDescs = new HashMap<String, Record<String, Object>>();
		// 标段补遗描述
		Record<String, Object> addendumDesc = null;
		// 标段编号
		String sectionID = null;
		// 补遗文件描述列表
		List<Record<String, Object>> addendumFileDescs = null;
		// 补遗文件描述
		Record<String, Object> addendumFileDesc = null;
		String fileName = null;
		String savePath = null;
		File addendumFile = null;
		// 迭代补遗文件
		for (Record<String, Object> addendum : priceList)
		{
			sectionID = addendum.getString("V_BID_SECTION_ID");
			// 补遗文件描述
			addendumFileDesc = new RecordImpl<String, Object>();
			// 补遗文件ID
			String attachId = addendum.getString("V_ATTACH_ID");
			// 文件名称
			String attachName = addendum.getString("V_ATTACH_NAME");
			// 标段编号
			String sectionCode = addendum.getString("V_BID_SECTION_CODE");
			// 标段名称
			String sectionName = addendum.getString("V_BID_SECTION_NAME");
			// 招标项目编号
			String tenderProCode = addendum.getString("V_TENDER_PROJECT_CODE");
			// 补遗描述
			addendumDesc = sectionAddendumDescs.get(sectionID);
			// 如果不存在
			if (null == addendumDesc)
			{
				addendumDesc = new RecordImpl<String, Object>();
				addendumDesc.setColumn("BID_SECTION_ID", sectionID);
				addendumDesc.setColumn("BID_SECTION_CODE", sectionCode);
				addendumDesc.setColumn("BID_SECTION_NAME", sectionName);
				sectionAddendumDescs.put(sectionID, addendumDesc);
			}
			// 获取文件描述列表
			addendumFileDescs = addendumDesc.getColumn("ADDENDUM_LIST");
			if (null == addendumFileDescs)
			{
				addendumFileDescs = new LinkedList<Record<String, Object>>();
				addendumDesc.setColumn("ADDENDUM_LIST", addendumFileDescs);
			}
			// 加入原来的所有值
			addendumFileDesc.putAll(addendum);
			// 下载文件
			// 文件名
			fileName = attachId + "." + FilenameUtils.getExtension(attachName);
			savePath = SystemParamUtils
					.getString(SysParamKey.EBIDKB_FILE_PATH_URL)
					+ tenderProCode + TAG + "控制价" + TAG + sectionCode;
			param.setColumn("fileName", fileName);
			param.setColumn("savePath", savePath);
			param.setColumn("attachId", attachId);
			// 下载文件
			addendumFile = getDocuQuesFile(param, type);// 此处共用下载补遗文件的方法
			if (addendumFile == null)
			{
				continue;
			}
			// "ADDENDUM_ID":"控制价文件ID",
			addendumFileDesc.setColumn("ADDENDUM_ID", SZUtilsID.getUUID());
			// "ADDENDUM_NAME":"文件名称",
			addendumFileDesc.setColumn("ADDENDUM_NAME", attachName);
			// "FILE_PATH":"文件路径,文件绝对路径例如(D:/fileEbid-kp/招标项目编号/控制价/标段包编号/XXXXXX.doc)",
			addendumFileDesc.setColumn("FILE_PATH", FilenameUtils
					.separatorsToUnix(addendumFile.getAbsolutePath()));
			// "FILE_PATH_BASE64":"文件绝对路径BASE64字符串例如(RDpcZmlsZUViaWQta3Bc5oub5qCH6aG555uu57yW5Y+3XOihpemBl1zooaXpgZfmlofku7bmj4/ov7AuanNvbg==)",
			addendumFileDesc.setColumn("FILE_PATH_BASE64", Base64
					.encodeBase64String(addendumFile.getAbsolutePath()
							.getBytes()));
			addendumFileDescs.add(addendumFileDesc);
		}
		saveJsonFile(jsonFile, sectionAddendumDescs.values());
	}

	/**
	 * 保存JSON文件<br/>
	 * 
	 * @param file
	 * @param object
	 * @throws ServiceException
	 */
	private static void saveJsonFile(File file, Object object)
			throws ServiceException
	{
		String json = JSON.toJSONString(object);
		try
		{
			FileUtils.write(file, json, ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (IOException e)
		{
			throw new ServiceException("", "保存JSON文件失败!", e);
		}
	}

	/**
	 * 根据附件ID获取补遗文件<br/>
	 * 
	 * @param params
	 *            附件ID 附件名称 保存路径
	 * @throws ServiceException
	 *             ServiceException
	 */
	private static File getDocuQuesFile(Record<String, String> params,
			String type) throws ServiceException
	{
		Record<String, String> headMap = HttpClientUtils.getHeadMapOfToken();
		String url = TenderProjectParamUtils.getSystemParamValue(
				SysParamKey.EDE_DOWNLOAD_QUES_FILE_URL, type)
				+ "/"
				+ params.getString("attachId");
		String fileName = params.getString("fileName");
		String savePath = params.getString("savePath");
		logger.debug(LogUtils.format("下载补遗文件:", url, headMap, savePath,
				fileName));
		try
		{
			return HttpClientUtils.doFileDownLoad(url, null, headMap, savePath,
					fileName);
		}
		catch (Exception e)
		{
			throw new ServiceException("E-1009", "根据附件ID获取补遗文件!", e);
		}
	}

}
