/**
 * 包名：com.sozone.eokb.bus.video
 * 文件名：Video.java<br/>
 * 创建时间：2018-2-11 下午3:17:04<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.video;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.base.utils.sms.SZUtilSMS;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.DAOException;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.HttpClientUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 视频管理<br/>
 * <p>
 * 视频管理<br/>
 * </p>
 * Time：2018-2-11 下午3:17:04<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/video", desc = "视频管理")
// 登录即可访问
@Permission(Level.Authenticated)
public class Video
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Video.class);

	/**
	 * 直播JSP
	 */
	private static String videoShow = "video.jsp";

	/**
	 * 回播JSP
	 */
	private static String videoBack = "video.back.jsp";

	/**
	 * 直播或回播JSP
	 */
	private static String videoLor = "video.live.or.review.jsp";

	/**
	 * 
	 * 获取DAO<br/>
	 * <p>
	 * 获取DAO
	 * </p>
	 * 
	 * @return DAO
	 */
	private static ActiveRecordDAO getActiveRecordDAO()
	{
		return ActiveRecordDAOImpl.getInstance();
	}

	/**
	 * 
	 * 管理员启动视频录制<br/>
	 * <p>
	 * 管理员启动视频录制
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "stvr/{tpid}", desc = "管理员启动视频录制")
	@HttpMethod(HttpMethod.GET)
	public void startVideoRec(@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("管理员启动视频录制", tpid, data));
		// 先解除禁用在录制
		relieveVideo(tpid);
		beginVideo(tpid);
		logger.debug(LogUtils.format("管理员成功启动视频录制"));
	}

	/**
	 * 
	 * 管理员结束视频录制<br/>
	 * <p>
	 * 管理员结束视频录制
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "spovr/{tpid}", desc = "管理员结束视频录制")
	@HttpMethod(HttpMethod.GET)
	public void stopVideoRec(@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("管理员结束视频录制", tpid, data));
		// 先停止录制再禁用
		endVideo(tpid);
		disableVideo(tpid);
		logger.debug(LogUtils.format("管理员成功结束视频录制"));
	}

	/**
	 * 
	 * 获取视频信息<br/>
	 * <p>
	 * 获取视频信息
	 * </p>
	 * 
	 * @param data
	 *            页面请求
	 * @return 获取直播地址
	 * @throws ServiceException
	 *             服务异常
	 */
	@Path(value = "/getVideos", desc = "获取视频信息")
	@HttpMethod(HttpMethod.GET)
	public String getVideoList(AeolusData data) throws ServiceException
	{
		logger.info(LogUtils.format("获取视频信息", data));
		String videoInfos = SystemParamUtils
				.getString(SysParamKey.EOV_VIDEO_INFO);
		String videoUrl = "";
		StringBuilder sb = new StringBuilder();

		if (StringUtils.isNotEmpty(videoInfos))
		{
			JSONObject jsonObject = JSON.parseObject(videoInfos);
			JSONObject info = jsonObject.getJSONObject("info");
			// 地区
			String roomArea = info.getString("room_area");

			// 名称
			String roomName = info.getString("room_name");

			ActiveRecordDAO dao = getActiveRecordDAO();
			Record<String, Object> videoInfo = dao
					.auto()
					.table(TableName.EKB_T_VIDEO_INFO)
					.setCondition("AND", "V_TPID=#{tpid}")
					.get(new RecordImpl<String, Object>().setColumn("tpid",
							SessionUtils.getTPID()));

			if (!CollectionUtils.isEmpty(videoInfo))
			{
				roomArea = videoInfo.getString("V_ROOM_AREA");
				roomName = videoInfo.getString("V_ROOM_NAME");
			}

			videoUrl = SystemParamUtils.getString(SysParamKey.EOV_VIDEO_JSP);
			sb.append(videoUrl).append(videoShow).append("?area=")
					.append(roomArea).append("&room=").append(roomName);
			videoUrl = sb.toString();
		}
		return videoUrl;
	}

	/**
	 * 
	 * 开始录制视频<br/>
	 * <p>
	 * 开始录制视频
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 */
	public static void beginVideo(String tpid)
	{
		logger.debug(LogUtils.format("开始录制视频"));
		String videoUrl = "";
		String url = "";
		StringBuilder sb = new StringBuilder();
		try
		{
			// 获取开标摄像头所在地区和名称
			Record<String, Object> videoInfo = getVideoInfos(tpid);
			String roomArea = videoInfo.getString("roomArea");
			String roomName = videoInfo.getString("roomName");

			// 推送给视频工程的视频直播地址
			videoUrl = SystemParamUtils.getString(SysParamKey.EOV_VIDEO_JSP);
			sb.append(videoUrl).append(videoShow).append("?area=")
					.append(roomArea).append("&room=").append(roomName);
			videoUrl = sb.toString();

			// 开启录制视频地址
			url = SystemParamUtils.getString(SysParamKey.EOV_VIDEO_URL)
					+ "/recVideo/" + roomArea + "/" + roomName;

			Map<String, String> params = new HashMap<String, String>();
			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("tpid", tpid);

			// 获取项目信息
			ActiveRecordDAO dao = getActiveRecordDAO();
			Record<String, Object> projectInfo = dao.statement().selectOne(
					"Video.getProjectInfo", param);

			// 交易平台的项目ID
			params.put("V_TENDER_PROJECT_ID",
					projectInfo.getString("V_TENDER_PROJECT_ID"));
			// 标段ID
			params.put("SECTION_IDS", projectInfo.getString("SECTION_IDS"));
			// 开标时间
			params.put("V_TENDER_PROJECT_NAME",
					projectInfo.getString("V_TENDER_PROJECT_NAME"));
			// 开标项目名称
			params.put("V_BIDOPEN_TIME",
					projectInfo.getString("V_BIDOPEN_TIME"));
			// 視頻url
			params.put("V_BID_VIDEO_JSP", videoUrl);

			// 开始录制视频并返回推送给交易平台的数据
			String result = HttpClientUtils.doPost(url, params);
			logger.debug("结果1" + result);
			JSONObject jobj = JSON.parseObject(result);
			if (!jobj.getBoolean("SUCCESS"))
			{
				logger.debug(LogUtils.format("开始录制视频失败", jobj));
				throw new ServiceException("", "开始录制视频失败");
			}

			/** 对接视频工程完成，开始对接交易平台 */
			jobj.remove("SUCCESS");
			// 推送视频地址
			url = SystemParamUtils.getString(SysParamKey.PUSH_VIDEO_URL);

			// token
			Record<String, String> headMap = HttpClientUtils
					.getHeadMapOfToken();
			// 推送给交易平台的视频直播或者地址
			jobj.put(
					"URL",
					new StringBuilder(SystemParamUtils
							.getString(SysParamKey.EOV_VIDEO_JSP))
							.append(videoLor).append("?sid=")
							.append(projectInfo.getString("SECTION_IDS"))
							.toString());
			logger.debug("结果3" + result);
			// 将开标视频信息推送到交易平台
			result = HttpClientUtils.sendJsonPostRequest(url,
					JSON.toJSONString(jobj), headMap,
					ConstantEOKB.DEFAULT_CHARSET);
			logger.debug("结果4" + result);
			jobj = JSON.parseObject(result);
			if (!jobj.getBoolean("success"))
			{
				logger.debug(LogUtils.format("开标直播视频推送到交易平台失败", jobj));
				throw new ServiceException("", "开标直播视频推送到交易平台失败");
			}
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("开始录制视频失败", url, e));
			// 异常发送短息
			sendMessageToPhone("开标启动：" + e.getMessage(), tpid);
		}
	}

	/**
	 * 
	 * 结束录制视频<br/>
	 * <p>
	 * 结束录制视频
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 */
	public static void endVideo(String tpid)
	{
		logger.debug(LogUtils.format("结束录制视频"));
		String url = "";
		try
		{
			Record<String, Object> videoInfo = getVideoInfos(tpid);
			String roomArea = videoInfo.getString("roomArea");
			String roomName = videoInfo.getString("roomName");
			// 停止视频的url
			url = SystemParamUtils.getString(SysParamKey.EOV_VIDEO_URL)
					+ "/stopVideo/" + roomArea + "/" + roomName;

			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("tpid", tpid);

			// 获取项目信息
			ActiveRecordDAO dao = getActiveRecordDAO();
			Record<String, Object> projectInfo = dao.statement().selectOne(
					"Video.getProjectInfo", param);
			Map<String, String> params = new HashMap<String, String>();
			// 交易平台的项目ID
			params.put("V_TENDER_PROJECT_ID",
					projectInfo.getString("V_TENDER_PROJECT_ID"));
			// 标段ID
			params.put("SECTION_IDS", projectInfo.getString("SECTION_IDS"));
			// 开标时间
			params.put("V_TENDER_PROJECT_NAME",
					projectInfo.getString("V_TENDER_PROJECT_NAME"));
			// 开标项目名称
			params.put("V_BIDOPEN_TIME",
					projectInfo.getString("V_BIDOPEN_TIME"));

			// 回播地址
			String backUrl = SystemParamUtils
					.getString(SysParamKey.EOV_VIDEO_JSP);
			StringBuilder sb = new StringBuilder(backUrl);
			sb.append(videoBack).append("?sid=")
					.append(projectInfo.getString("SECTION_IDS"));
			backUrl = sb.toString();
			// 視頻url
			params.put("V_BID_VIDEO_JSP", backUrl);

			Record<String, String> headMap = HttpClientUtils
					.getHeadMapOfToken();
			logger.debug(LogUtils.format("停止录制视屏_url") + url);
			// 停止录制视频并返回推送到交易平台的录播文件信息
			String result = HttpClientUtils.doPost(url, params, headMap,
					ConstantEOKB.DEFAULT_CHARSET);
			logger.debug(LogUtils.format("停止录制视屏") + result);
			JSONObject jobj = JSON.parseObject(result);
			if (!jobj.getBoolean("SUCCESS"))
			{
				logger.debug(LogUtils.format("结束录制视频失败", jobj));
				throw new ServiceException("", "结束录制视频失败");
			}

			/** 对接视频工程完成，开始对接交易平台 */
			url = SystemParamUtils.getString(SysParamKey.PUSH_VIDEO_URL);
			jobj.remove("SUCCESS");
			setMd5ToJson(jobj);

			// 推送给交易平台直播或者回播地址
			jobj.put(
					"URL",
					new StringBuilder(SystemParamUtils
							.getString(SysParamKey.EOV_VIDEO_JSP))
							.append(videoLor).append("?sid=")
							.append(projectInfo.getString("SECTION_IDS"))
							.toString());
			headMap = HttpClientUtils.getHeadMapOfToken();
			String jsonString = JSON.toJSONString(jobj);
			logger.debug(LogUtils.format("推送视频到交易平台_url") + url);
			// 录播视频信息推送到交易平台
			result = HttpClientUtils.sendJsonPostRequest(url, jsonString,
					headMap, ConstantEOKB.DEFAULT_CHARSET);
			logger.debug(LogUtils.format("推送视频到交易平台_result") + result);
			jobj = JSON.parseObject(result);
			if (!jobj.getBoolean("success"))
			{
				logger.debug(LogUtils.format("推送视频到交易平台失败", jobj));
				throw new ServiceException("", "推送视频到交易平台失败");
			}
		}
		catch (Exception e)
		{
			logger.debug(LogUtils.format("结束录制视频", url, e));
			// 异常发送短息
			sendMessageToPhone("开标结束：" + e.getMessage(), tpid);
		}
	}

	/**
	 * 
	 * 解除禁用的视频<br/>
	 * <p>
	 * 解除禁用的视频
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 */
	public static void relieveVideo(String tpid)
	{

		logger.debug(LogUtils.format("解除禁用的视频"));
		String url = "";
		try
		{
			// 获取开标摄像头所在地区和名称
			Record<String, Object> videoInfo = getVideoInfos(tpid);

			String roomArea = videoInfo.getString("roomArea");
			String roomName = videoInfo.getString("roomName");

			// 开启录制视频地址
			url = SystemParamUtils.getString(SysParamKey.EOV_VIDEO_URL)
					+ "/relieveVideo/" + roomArea + "/" + roomName;

			Map<String, String> params = new HashMap<String, String>();

			// 开始录制视频并返回推送给交易平台的数据
			String result = HttpClientUtils.doPost(url, params);
			JSONObject jobj = JSON.parseObject(result);
			if (!jobj.getBoolean("SUCCESS"))
			{
				logger.debug(LogUtils.format("解除禁用的视频", jobj));
				throw new ServiceException("", "解除禁用的视频");
			}
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("解除禁用的视频", url, e));
			// 异常发送短息
			sendMessageToPhone("解除禁用的视频" + e.getMessage(), tpid);
		}
	}

	/**
	 * 
	 * 禁用视频<br/>
	 * <p>
	 * 禁用视频
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 */
	public static void disableVideo(String tpid)
	{

		logger.error(LogUtils.format("禁用视频"));
		String url = "";
		try
		{
			// 获取开标摄像头所在地区和名称
			Record<String, Object> videoInfo = getVideoInfos(tpid);

			String roomArea = videoInfo.getString("roomArea");
			String roomName = videoInfo.getString("roomName");

			// 禁用视频地址
			url = SystemParamUtils.getString(SysParamKey.EOV_VIDEO_URL)
					+ "/disableVideo/" + roomArea + "/" + roomName;

			Map<String, String> params = new HashMap<String, String>();

			// 禁用视频
			String result = HttpClientUtils.doPost(url, params);
			JSONObject jobj = JSON.parseObject(result);
			if (!jobj.getBoolean("SUCCESS"))
			{
				logger.error(LogUtils.format("禁用视频", jobj));
				throw new ServiceException("", "禁用视频");
			}
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("禁用视频", url, e));
			// 异常发送短息
			sendMessageToPhone("禁用视频" + e.getMessage(), tpid);
		}
	}

	/**
	 * 
	 * 获取开标室信息<br/>
	 * <p>
	 * 获取开标室信息
	 * </p>
	 * 
	 * @return 摄像头信息
	 * @throws DAOException
	 *             DAOException
	 */
	private static Record<String, Object> getVideoInfos(String tpid)
			throws DAOException
	{
		logger.equals(LogUtils.format("获取开标室信息"));
		String videoInfos = SystemParamUtils
				.getString(SysParamKey.EOV_VIDEO_INFO);

		// 地区
		String roomArea = "";
		// 名称
		String roomName = "";

		if (StringUtils.isNotEmpty(videoInfos))
		{
			// 获取开标摄像头所在地区和名称
			JSONObject jsonObject = JSON.parseObject(videoInfos);
			JSONObject info = jsonObject.getJSONObject("info");

			roomArea = info.getString("room_area");
			roomName = info.getString("room_name");

			// 优先获取表中的数据
			ActiveRecordDAO dao = getActiveRecordDAO();
			Record<String, Object> videoInfo = dao
					.auto()
					.table(TableName.EKB_T_VIDEO_INFO)
					.setCondition("AND", "V_TPID=#{tpid}")
					.get(new RecordImpl<String, Object>().setColumn("tpid",
							tpid));

			if (!CollectionUtils.isEmpty(videoInfo))
			{
				roomArea = videoInfo.getString("V_ROOM_AREA");
				roomName = videoInfo.getString("V_ROOM_NAME");
			}

		}
		Record<String, Object> info = new RecordImpl<String, Object>();
		info.setColumn("roomArea", roomArea);
		info.setColumn("roomName", roomName);
		return info;
	}

	/**
	 * 
	 * 发送运维信息<br/>
	 * <p>
	 * 发送运维信息
	 * </p>
	 * 
	 * @param msg
	 */
	private static void sendMessageToPhone(String msg, String tpid)
	{
		Record<String, Object> videoInfo = new RecordImpl<String, Object>();
		try
		{
			videoInfo = getVideoInfos(tpid);
		}
		catch (DAOException e)
		{
			logger.debug("获取摄像头失败", e);
		}
		String roomName = videoInfo.getString("roomName");
		JSONObject result2 = SZUtilSMS
				.sendRsJson(SystemParamUtils
						.getString(SysParamKey.VIDEO_PERSON_PHONES_KEY),
						"开标室编号：" + roomName + "推送视频到交易平台失败。错误信息：" + msg);
		logger.warn(result2.toString());
	}

	/**
	 * 
	 * 设置md5值<br/>
	 * <p>
	 * 设置md5值
	 * </p>
	 * 
	 * @param jobj
	 * @throws InterruptedException
	 */
	private static void setMd5ToJson(JSONObject jobj)
			throws InterruptedException
	{
		logger.debug(LogUtils.format("设置md5值", jobj));
		JSONArray jarr = jobj.getJSONArray("DATA");
		JSONObject tempJobj = null;
		for (int i = 0; i < jarr.size(); i++)
		{
			tempJobj = jarr.getJSONObject(i);
			tempJobj.put("MD5", getFileMD5(tempJobj.getString("URL")));
			tempJobj.remove("URL");
		}
	}

	/**
	 * 
	 * 获取文件的md5值<br/>
	 * <p>
	 * 获取文件的md5值
	 * </p>
	 * 
	 * @param urlStr
	 *            回播地址
	 * @return
	 * @throws InterruptedException
	 */
	private static String getFileMD5(String urlStr) throws InterruptedException
	{
		/** 网络的url地址 */
		URL url = null;
		/** http连接 */
		HttpURLConnection httpConn = null;
		/** 输入流 */
		InputStream in = null;
		try
		{
			Thread.sleep(5000);
			logger.debug(LogUtils.format("getFileMD5_urlStr_1") + urlStr);
			url = new URL(urlStr);
			logger.debug(LogUtils.format("getFileMD5_urlStr_2") + urlStr);
			httpConn = (HttpURLConnection) url.openConnection();
			HttpURLConnection.setFollowRedirects(true);
			httpConn.setRequestMethod("GET");
			httpConn.setRequestProperty("User-Agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows 2000)");
			logger.debug(LogUtils.format("getFileMD5_urlStr_3") + urlStr);
			Thread.sleep(5000);
			in = httpConn.getInputStream();
			logger.debug(LogUtils.format("getFileMD5_urlStr_4") + urlStr);
			// byte[] bt = new byte[in.available()];
			// in.read(bt);
			// return Base64.encode(bt);
			String md5 = DigestUtils.md5Hex(in);
			logger.debug(LogUtils.format("getFileMD5_md5") + md5);
			return md5;
		}
		catch (IOException e)
		{
			logger.error(LogUtils.format("获取文件的MD5值失败", e));
		}
		finally
		{
			try
			{
				in.close();
				httpConn.disconnect();
			}
			catch (Exception ex)
			{
				logger.debug(LogUtils.format("", "InputStream流关闭失败"));
			}
		}
		return "";
	}

	// public static void main(String[] args) throws Exception
	// {
	// String paramUrl = "";
	// Map<String, String> params = new HashMap<String, String>();
	// String result = "";
	// JSONObject jobj = new JSONObject();
	// /*
	// * String url=
	// *
	// "http://117.25.161.109:22132/eov/authorize/bidVideo/recVideo/xms_szx/C403"
	// * ; Map<String,String> params=new HashMap<String, String>();
	// * params.put("V_TENDER_PROJECT_ID",
	// * "573c8c9ab08748b48241d3d653a22743"); params.put("SECTION_IDS",
	// * "540f03748ca340ef83857950f24d6c33"); String result =
	// * HttpClientUtils.doPost(url,params); JSONObject jobj =
	// * JSON.parseObject(result); if (!jobj.getBoolean("SUCCESS")) {
	// * logger.debug(LogUtils.format("开始录制视频失败", jobj)); throw new
	// * ServiceException("", "开始录制视频失败"); }
	// */
	// paramUrl =
	// "http://117.25.161.109:22132/eov/authorize/bidVideo/stopVideo/xms_szx/C403";
	// params = new HashMap<String, String>();
	// params.put("V_TENDER_PROJECT_ID", "573c8c9ab08748b48241d3d653a22743");
	// params.put("SECTION_IDS", "540f03748ca340ef83857950f24d6c37");
	// result = HttpClientUtils.doPost(paramUrl, params);
	// jobj = JSON.parseObject(result);
	// if (!jobj.getBoolean("SUCCESS"))
	// {
	// logger.error(LogUtils.format("结束录制视频失败", jobj));
	// throw new ServiceException("", "结束录制视频失败");
	// }
	// JSONArray jsonArray = jobj.getJSONArray("DATA");
	// for (int i = 0; i < jsonArray.size(); i++)
	// {
	// JSONObject j = (JSONObject) jsonArray.get(i);
	// InputStream in = null;
	// String strUrl =
	// "http://117.25.161.109:22134/mp4/liveshow/rtsp401/rtsp401_v10.mp4";
	// // String strUrl=j.getString("URL");
	// URL url = null;
	// // ** http连接 *//*
	// HttpURLConnection httpConn = null;
	// try
	// {
	// url = new URL(strUrl);
	// httpConn = (HttpURLConnection) url.openConnection();
	// HttpURLConnection.setFollowRedirects(true);
	// httpConn.setRequestMethod("GET");
	// httpConn.setRequestProperty("User-Agent",
	// "Mozilla/4.0 (compatible; MSIE 6.0; Windows 2000)");
	// in = httpConn.getInputStream();
	// String md5 = DigestUtils.md5Hex(in);
	// System.out.println();
	// }
	// catch (Exception e)
	// {
	// // TODO: handle exception
	// }
	// finally
	// {
	// in.close();
	// }
	// }
	//
	// }
	// /**
	// *
	// * 获取视频信息<br/>
	// * <p>
	// * 获取视频信息
	// * </p>
	// *
	// * @param data
	// * 页面请求
	// * @return
	// * @throws ServiceException
	// * 服务异常
	// */
	// @Path(value = "/getVideoReplay/{sid}", desc = "获取视频信息")
	// @HttpMethod(HttpMethod.GET)
	// public List<String> getVideoReplay(@PathParam("sid") String sid,
	// AeolusData data) throws FacadeException
	// {
	// Map<String, String> params = new HashMap<String, String>();
	// String url = SystemParamUtils
	// .getString(SysParamKey.EOV_VIDEO_URL)
	// + "/getVideoReplay";
	// List<String> urls = new LinkedList<String>();
	// try
	// {
	// params.put("sid", sid);
	// String result = HttpClientUtils.doPost(url, params);
	// JSONObject jobj = JSON.parseObject(result);
	// if (!jobj.getBoolean("SUCCESS"))
	// {
	// throw new FacadeException("");
	// }
	// JSONArray jarr = jobj.getJSONArray("RESULT");
	// for (int i = 0; i < jarr.size(); i++)
	// {
	// urls.add(jarr.getJSONObject(i).getString("URL"));
	// }
	// }
	// catch (Exception e)
	// {
	//
	// }
	// return urls;
	// }
}
