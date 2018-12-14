/**
 * 包名：com.sozone.eokb.job
 * 文件名：GsPtSyTPDataService.java<br/>
 * 创建时间：2018-2-26 下午3:14:42<br/>
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
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.HttpClientUtils;
import com.sozone.eokb.utils.InterfaceFieldMappingUtils;
import com.sozone.eokb.utils.MsgUtils;
import com.sozone.eokb.utils.TenderProjectParamUtils;

/**
 * 高速、普通、港航水运同步开标数据服务接口<br/>
 * <p>
 * 高速、普通、港航水运同步开标数据服务接口<br/>
 * </p>
 * Time：2018-2-26 下午3:14:42<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class GsPtSyTPDataService
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(GsPtSyTPDataService.class);

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
		try
		{
			// 获取开标项目信息
			List<Record<String, Object>> tenderProjectInfos = getOpenBidProList(
					params, tenderProType, dao);
			if (CollectionUtils.isEmpty(tenderProjectInfos))
			{
				logger.debug(LogUtils.format(openTime + ":无开标项目信息!"));
				return;
			}
			// 保存招标项目信息列表
			dao.auto().table(TableName.EKB_T_TENDER_PROJECT_INFO)
					.save(tenderProjectInfos);
			List<Record<String, Object>> sections = null;
			Record<String, String> param = new RecordImpl<String, String>();
			Record<String, String> tenderProjectFile = new RecordImpl<String, String>();
			for (Record<String, Object> tenderProjectInfo : tenderProjectInfos)
			{
				param.clear();
				tenderProjectFile.clear();
				String tpid = tenderProjectInfo.getString("ID");
				String tenderProId = tenderProjectInfo
						.getString("V_TENDER_PROJECT_ID");
				String tednerProCode = tenderProjectInfo
						.getString("V_TENDER_PROJECT_CODE");
				param.setColumn("tenderProjectId", tenderProId);
				// 获取各个项目的标段信息
				sections = getSectionInfoList(param, tenderProType, tpid);
				// 保存标段信息
				dao.auto().table(TableName.EKB_T_SECTION_INFO).save(sections);
				// 获取招标文件
				String path = SystemParamUtils
						.getString(SysParamKey.EBIDKB_FILE_PATH_URL)
						+ tednerProCode;
				String fileName = tednerProCode;
				tenderProjectFile.setColumn("savePath", path);
				tenderProjectFile.setColumn("fileName", fileName);
				tenderProjectFile.setColumn("projectID", tenderProId);
				// 获取招标文件
				getTenderFile(tenderProjectFile, tenderProType);
				// 获取补遗文件
				downLoadDocuQues(tenderProId, tednerProCode, tenderProType);
				// 水运和普通需要下载控制价文件
				if (!StringUtils.equals("10", tenderProType))
				{
					for (Record<String, Object> section : sections)
					{
						// 获取控制价文件
						downloadPriceFile(tenderProId, tednerProCode,
								section.getString("V_BID_SECTION_ID"),
								tenderProType);
					}
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
	private static void downloadPriceFile(String tenderProjectID,
			String tednerProCode, String sid, String type)
			throws ServiceException
	{
		logger.debug(LogUtils.format("根据招标项目ID下载补遗文件！", tenderProjectID, sid,
				type));
		// 请求参数
		Record<String, String> param = new RecordImpl<String, String>();
		param.setColumn("tenderProjectId", tenderProjectID);
		param.setColumn("sectionId", sid);
		// 获取补遗文件列表，该项目下的所有标段包补遗文件
		List<Record<String, Object>> priceList = getPriceList(param, type);
		// 获取补遗文件
		File jsonFile = new File(
				SystemParamUtils.getString(SysParamKey.EBIDKB_FILE_PATH_URL)
						+ tednerProCode + TAG + "控制价" + TAG + "控制价文件描述.json");
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
			// 为了页面字符串装json不报错，把后面增加的投标文件配置干掉
			jobj = JSON.parseObject(tenderProject.getString("V_BEM_INFO_JSON"));
			jobj.remove("V_JSON");
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
	 * 
	 * 获取开标标段信息<br/>
	 * <p>
	 * 获取开标标段信息
	 * </p>
	 * 
	 * @param params
	 * @param type
	 *            类型
	 * @param tpid
	 * @return
	 * @throws ServiceException
	 */
	private static List<Record<String, Object>> getSectionInfoList(
			Record<String, String> params, String type, String tpid)
			throws ServiceException
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
			record.setColumn("V_TPID", tpid);
			sectionsResuslt.add(record);
		}
		return sectionsResuslt;

	}

	/**
	 * 获取招标项目的招标文件<br/>
	 * 
	 * @param params
	 *            交易平台对应招标项目ID
	 * @param type
	 *            类型
	 * @throws ServiceException
	 *             ServiceException
	 */
	private static void getTenderFile(Record<String, String> params, String type)
			throws ServiceException
	{
		// 获取头部Token
		Record<String, String> headMap = HttpClientUtils.getHeadMapOfToken();
		// 招标项目ID
		String tenderId = params.getString("projectID");
		// 招标文件获取请求路径拼接
		String url = TenderProjectParamUtils.getSystemParamValue(
				SysParamKey.EDE_DOWNLOAD_TENDER_FILE_URL, type)
				+ "/"
				+ tenderId;
		// 保存到本地的招标文件名称
		String fileName = params.getString("fileName") + ".szb";
		// 保存的路径
		String savePath = params.getString("savePath");
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
	 * 根据招标项目编号ID获取补遗文件列表<br/>
	 * 
	 * @param params
	 * 
	 * @throws ServiceException
	 *             ServiceException
	 * @return List<Record<String, Object>>
	 */
	private static List<Record<String, Object>> getDocuQuesList(
			Record<String, String> params, String type) throws ServiceException
	{
		// 获去头部token
		Record<String, String> headMap = HttpClientUtils.getHeadMapOfToken();
		//
		String url = TenderProjectParamUtils.getSystemParamValue(
				SysParamKey.EDE_DOCU_QUES_FILE_LIST_URL, type);
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
	 * 
	 * 下载补遗文件<br/>
	 * <p>
	 * 下载补遗文件
	 * </p>
	 * 
	 * @param tenderProId
	 *            招标项目ID
	 * @param tenderCode
	 *            招标项目编号
	 * @throws ServiceException
	 */
	private static void downLoadDocuQues(String tenderProId, String tenderCode,
			String type) throws ServiceException
	{
		logger.debug(LogUtils
				.format("根据招标项目ID下载补遗文件！", tenderProId, tenderCode));
		// 请求参数
		Record<String, String> param = new RecordImpl<String, String>();
		param.setColumn("tenderProjectId", tenderProId);
		// 获取补遗文件列表，该项目下的所有标段包补遗文件
		List<Record<String, Object>> addendums = getDocuQuesList(param, type);
		// 获取补遗文件
		File jsonFile = new File(
				SystemParamUtils.getString(SysParamKey.EBIDKB_FILE_PATH_URL)
						+ tenderCode + TAG + "补遗" + TAG + "补遗文件描述.json");
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
