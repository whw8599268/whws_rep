/**
 * 包名：com.sozone.eokb.bus.push
 * 文件名：PushData.java<br/>
 * 创建时间：2017-11-8 下午4:01:52<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.push;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.ArchiverUtils;
import com.sozone.eokb.utils.BigTimeOutHttpClientUtils;
import com.sozone.eokb.utils.HttpClientUtils;
import com.sozone.eokb.utils.MsgUtils;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.utils.TenderProjectParamUtils;
import com.sozone.eokb.utils.UtilCollaGEN;

/**
 * 推送数据接口服务<br/>
 * <p>
 * 推送数据接口服务<br/>
 * </p>
 * Time：2017-11-8 下午4:01:52<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/push", desc = "推送数据接口服务")
// 登录即可访问
@Permission(Level.Authenticated)
// 增加操作日志
@Handler(OperationLogHandler.class)
public class PushData
{

	private static final NumberFormat FMT_D = new DecimalFormat("###,##0",
			new DecimalFormatSymbols());
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(PushData.class);

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
	 * 推送投标人信息到交易平台<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             服务异常
	 */
	@Path(value = "/bidders", desc = "推送投标人信息到交易平台")
	@Service
	public void sendBidders(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("推送投标人信息到交易平台", data));
		String tpid = SessionUtils.getTPID();

		// 设置代理推送标致
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 删除冗余数据,为了防止出现重复
		param.setColumn("flag", ConstantEOKB.PUSH_BIDDER_INFO);
		param.setColumn("tpid", tpid);
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").remove(param);

		param.clear();
		param.setColumn("ID", Random.generateUUID());
		param.setColumn("V_TPID", tpid);
		param.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.PUSH_BIDDER_INFO);
		param.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
				.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.save(param);

		// 招标项目名称
		String tpName = SessionUtils.getBidProjectName();
		// 启动一个线程去推送
		SendBiddersTask task = new SendBiddersTask(tpid, tpName);
		task.start();
		// Record<String, String> headMap = HttpClientUtils.getHeadMapOfToken();
		// String url = TenderProjectParamUtils
		// .getSystemParamValue(SysParamKey.PUSH_BIDDERS_URL);
		// // 获取招标项目投标人信息
		// List<Record<String, Object>> bidders = getTenderProjectBidders(tpid);
		// Record<String, Object> bidderInfos = new RecordImpl<String,
		// Object>();
		// bidderInfos.setColumn("BIDDERS", bidders);
		// String rsJson = null;
		// try
		// {
		// logger.debug(LogUtils.format("发送http请求到数据接口平台!", url, bidderInfos,
		// headMap));
		// rsJson = HttpClientUtils.sendJsonPostRequest(url,
		// JSON.toJSONString(bidderInfos), headMap,
		// ConstantEOKB.DEFAULT_CHARSET);
		// }
		// catch (Exception e)
		// {
		// logger.error(LogUtils.format("推送投标人信息到交易平台发生异常!", rsJson), e);
		// throw new ServiceException("", "推送投标人信息到交易平台发生异常!", e);
		// }
		// logger.debug(LogUtils.format("推送投标人信息到交易平台-rsJson", rsJson));
		// JSONObject result = JSON.parseObject(rsJson);
		// boolean success = result.getBoolean("success");
		// if (success)
		// {
		// throw new ServiceException("", "发送成功");
		// }
		// logger.error(result.getString("errorDesc"));
		// throw new ServiceException("", result.getString("errorDesc"));
	}

	/**
	 * 推送投标人信息<br/>
	 * <p>
	 * 推送投标人信息<br/>
	 * </p>
	 * Time：2018-6-13 下午5:02:51<br/>
	 * 
	 * @author wengdm
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private class SendBiddersTask extends Thread
	{
		/**
		 * 项目ID
		 */
		private String tpid = null;

		/**
		 * 
		 */
		private String tpName = null;

		/**
		 * 构造函数
		 * 
		 * @param tpid
		 * @param tpName
		 */
		private SendBiddersTask(String tpid, String tpName)
		{
			super();
			this.tpid = tpid;
			this.tpName = tpName;
			this.setName("推送投标人到交易平台线程");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run()
		{
			String rsJson = null;
			try
			{
				Record<String, String> headMap = BigTimeOutHttpClientUtils
						.getHeadMapOfToken();
				String url = TenderProjectParamUtils
						.getSystemParamValue(SysParamKey.PUSH_BIDDERS_URL);
				// 获取招标项目投标人信息
				List<Record<String, Object>> bidders = getTenderProjectBidders(tpid);
				Record<String, Object> bidderInfos = new RecordImpl<String, Object>();
				bidderInfos.setColumn("BIDDERS", bidders);
				logger.debug(LogUtils.format("发送http请求到数据接口平台!", url,
						bidderInfos, headMap));
				rsJson = BigTimeOutHttpClientUtils.sendJsonPostRequest(url,
						JSON.toJSONString(bidderInfos), headMap,
						ConstantEOKB.DEFAULT_CHARSET);
			}
			catch (Exception e)
			{
				logger.error(LogUtils.format("推送投标人信息到交易平台发生异常!", rsJson), e);
				StringBuilder sb = new StringBuilder();
				sb.append("推送投标人信息：失败，招标项目:[").append(tpName).append("]，请处理!");
				MsgUtils.send(sb.toString());
				return;
			}
			logger.debug(LogUtils.format("推送投标人信息到交易平台-rsJson", rsJson));
			JSONObject result = JSON.parseObject(rsJson);
			if (null != result)
			{
				boolean success = result.getBoolean("success");
				if (success)
				{
					StringBuilder sb = new StringBuilder();
					sb.append("推送投标人信息：成功，招标项目:[").append(tpName).append("]!");
					MsgUtils.send(sb.toString());
					return;
				}
				logger.error(result.getString("errorDesc"));
			}
			StringBuilder sb = new StringBuilder();
			sb.append("推送投标人信息：失败，招标项目:[").append(tpName).append("]，请处理!");
			MsgUtils.send(sb.toString());
		}
	}

	/**
	 * 获取招标项目投标人信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 * @return
	 * @throws ServiceException
	 */
	public List<Record<String, Object>> getTenderProjectBidders(String tpid)
			throws ServiceException
	{

		String mybatisName = "PushData.getTenderProjectBidders";
		if (SessionUtils.isSectionGroup())
		{
			mybatisName = "PushData.getTenderProjectBiddersByGroup";
		}
		List<Record<String, Object>> bidders = this.activeRecordDAO.statement()
				.selectList(mybatisName, tpid);
		// "ID":"主键",
		// "BIDDER_ORG_CODE":"投标人代码(三证合一或组织机构代码）",
		// "BIDDER_NAME":"投标人名称",
		// "TENDER_PROJECT_ID":"招标项目ID",
		// "BID_SECTION_ID":"标段ID",
		// "BIDDER_TOTAL":"投标总价",
		// "BIDDER_FILE_ENCRYPT_STATE":"投标文件密封情况",
		// "BIDDER_DURATION":"投标工期",
		// "BIDDER_QUALITY_STANDARD":"质量标准",
		// "BIDDER_INVALIDFILE_REASON":"无效投标文件及无效原因",
		// "IS_SUBMIT_BAIL":"是否递交保证金",
		// "MARGIN_PAY_FORM":"保证金递交方式",
		// "PROJECT_LEADER":"项目负责人",
		// "QIYE_ZIZHI_DENGJI":"企业资质等级"
		try
		{
			// 如果非空
			if (!CollectionUtils.isEmpty(bidders))
			{
				String cbJson = null;
				// 读取系统配置获取项目负责人KEY值
				String fzr = "tbPeName";
				String pushBidders = TenderProjectParamUtils
						.getSystemParamValue(SysParamKey.PUSH_BIDDERS_LEADER);
				JSONObject jsonObject = JSON.parseObject(pushBidders);
				if (StringUtils.isNotEmpty(jsonObject.getString(SessionUtils
						.getTenderProjectTypeCode())))
				{
					fzr = jsonObject.getString(SessionUtils
							.getTenderProjectTypeCode());
				}
				for (Record<String, Object> bidder : bidders)
				{
					cbJson = bidder.getString("V_JSON_OBJ");
					String gongqi = BidderElementParseUtils
							.getSingObjAttributeSum(cbJson, "gongqi");
					logger.debug(LogUtils.format("推送投标人信息到交易平台-工期", gongqi));
					bidder.setColumn("BIDDER_FILE_ENCRYPT_STATE", "完好");
					// 投标工期
					bidder.setColumn("BIDDER_DURATION", gongqi);
					// 质量标准
					bidder.setColumn("BIDDER_QUALITY_STANDARD",
							BidderElementParseUtils.getSingObjAttribute(cbJson,
									"zlmb"));
					// 保证金递交方式
					bidder.setColumn("MARGIN_PAY_FORM", BidderElementParseUtils
							.getSingObjAttribute(cbJson, "bzjdjqk"));
					if (null != bidder.getDouble("BIDDER_TOTAL"))
					{
						// 投标总价
						bidder.setColumn("BIDDER_TOTAL",
								FMT_D.format(bidder.getDouble("BIDDER_TOTAL"))
										.toString());
					}
					// 项目负责人
					bidder.setColumn("PROJECT_LEADER", BidderElementParseUtils
							.getSingObjAttribute(cbJson, fzr));
					// 企业资质等级
					bidder.setColumn("QIYE_ZIZHI_DENGJI",
							BidderElementParseUtils.getSingObjAttribute(cbJson,
									"tbTbrCR"));
					// 删除字段
					bidder.remove("V_JSON_OBJ");
				}
			}
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("推送投标人信息到交易平台发生异常!", e), e);
		}
		return bidders;
	}

	/**
	 * 
	 * 执行预清标（含新建评标项目等）<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 取费文件或主要项目设置页面的URL" //url不为空时需要访问该url
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/haimai/systemInspect", desc = "执行预清标（含新建评标项目等）")
	@Service
	@Deprecated
	public String systemInspect(AeolusData data) throws FacadeException
	{
		logger.error(LogUtils.format("执行预清标（含新建评标项目等）", data));
		String tpID = SessionUtils.getTPID();
		Record<String, Object> bidSectionRecord = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.get(new RecordImpl<String, Object>().setColumn("V_TPID", tpID));
		if (CollectionUtils.isEmpty(bidSectionRecord))
		{
			throw new ValidateException("", "找不到标段信息!");
		}
		String sectionID = bidSectionRecord.getString("V_BID_SECTION_ID");
		// String tpID = data.getParam("TPID");
		// String sectionID = data.getParam("SECTION_ID");
		Record<String, Object> record = new RecordImpl<String, Object>();
		Record<String, Object> info = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO).get(tpID);

		record.setColumn("bidId", sectionID);
		record.setColumn("projectId", tpID);
		record.setColumn("reviewId", sectionID + "-1");
		record.setColumn("code", info.getString("V_INVITENO"));
		record.setColumn("tenderUnit", info.getString("V_TENDERER_NAME"));
		record.setColumn("agentUnit", info.getString("V_TENDER_AGENCY_NAME"));
		record.setColumn("tenderUnitCode", info.getString("V_TENDERER_CODE"));
		record.setColumn("agentUnitCode",
				info.getString("V_TENDER_AGENCY_CODE"));
		if (StringUtils.isEmpty(info.getString("V_TENDER_AGENCY_CODE")))
		{ // 如果代理机构的代码为空就使用招标人的机构代码
			record.setColumn("userKey", info.getString("V_TENDERER_CODE"));
		}
		else
		{
			record.setColumn("userKey", info.getString("V_TENDER_AGENCY_CODE"));
		}

		// 综合评估法开标没有抽取环节，需要将投标人N_ENVELOPE_9改成1入选状态
		List<Record<String, Object>> bidders = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_SECTION_ID = #{sectionID}")
				.list(new RecordImpl<String, Object>().setColumn("tpid", tpID)
						.setColumn("sectionID", sectionID));
		for (Record<String, Object> bidder : bidders)
		{
			if (StringUtils.isEmpty(bidder.getString("N_ENVELOPE_9")))
			{
				bidder.setColumn("N_ENVELOPE_9", 1);
				this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST)
						.modify(bidder);
			}

		}

		Record<String, Object> sectionInfo = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_SECTION_ID = #{sectionID}")
				.get(new RecordImpl<String, Object>().setColumn("tpid", tpID)
						.setColumn("sectionID", sectionID));

		record.setColumn("name", info.getString("V_TENDER_PROJECT_NAME") + "-"
				+ sectionInfo.getString("V_BID_SECTION_NAME"));

		String evaluationMethod = info
				.getString("V_BID_EVALUATION_METHOD_TYPE");
		if (StringUtils.isEmpty(evaluationMethod))
		{
			throw new ValidateException("", "评标办法不能为空!");
		}
		String method = null;
		// if ("55".equals(evaluationMethod))
		// {
		// method = "3";
		// }
		// else
		if ("56".equals(evaluationMethod) || "57".equals(evaluationMethod))
		{
			method = "1";
		}
		else if ("58".equals(evaluationMethod) || "59".equals(evaluationMethod))
		{
			method = "2";
		}
		if (StringUtils.isEmpty(method))
		{
			throw new ValidateException("", "不支持该评标办法[" + evaluationMethod
					+ "]执行预清标!");
		}
		record.setColumn("method", method);

		String url = SystemParamUtils.getString("com.sozone.ekb.url")
				+ "/authorize/bus/push/haimai/systemInspectZip?sectionID="
				+ sectionID;
		record.setColumn("zipPath", url);

		// 评标地址
		record.setColumn("serveUrl",
				SystemParamUtils.getString("com.sozone.epb.url"));

		String type = null;
		// 工程类别支持三类：房屋建筑工程、市政工程、其他工程，由交易平台处理转换。
		String appType = info.getString("V_TENDERPROJECT_APP_TYPE");
		if ("A01".equals(appType))
		{
			type = "房屋建筑工程";
		}
		else if ("A02".equals(appType))
		{
			type = "市政工程";
		}
		else
		{
			type = "其他工程";
		}
		record.setColumn("type", type);
		record.setColumn("evaluationTime", DateUtils.getDate());
		logger.error(LogUtils.format("执行预清标（含新建评标项目等）开始拷贝", record));

		String haimaiUrl = SystemParamUtils
				.getString("com.sozone.ekb.haimaiUrl");
		String haimaiUrlPath = "/rest/systemInspect";
		String fileName = createSystemInspectZip(tpID, sectionID);
		if (haimaiUrl.toLowerCase().startsWith(UtilCollaGEN.PROTOCOL))
		{
			// 集美的接口不支持文件下载，服务器上会同步文件，因此存放文件名。
			fileName = fileName.substring(0, fileName.lastIndexOf("."));// 海迈要求不传后缀名
			record.setColumn("zipPath", fileName);
		}
		Record<String, String> params = new RecordImpl<String, String>();
		params.setColumn("dataJson", JSON.toJSONString(record));
		logger.error(LogUtils.format("执行预清标（含新建评标项目等）结束拷贝", params));
		try
		{
			logger.error(LogUtils.format("执行预清标（含新建评标项目等）", haimaiUrl
					+ haimaiUrlPath, params));
			String result = "";
			if (haimaiUrl.toLowerCase().startsWith(UtilCollaGEN.PROTOCOL))
			{
				result = UtilCollaGEN.post(haimaiUrl, haimaiUrlPath, params);
			}
			else
			{
				result = BigTimeOutHttpClientUtils.doPost(haimaiUrl + haimaiUrlPath,
						params);
			}
			logger.error(LogUtils.format("执行预清标（含新建评标项目等） 结果", result));

			JSONObject resultObject = JSON.parseObject(result);
			Boolean resultboolean = resultObject.getBoolean("result");
			String url2 = resultObject.getString("url");
			logger.error(LogUtils.format(
					"项目名称：" + info.getString("V_TENDER_PROJECT_NAME")
							+ "执行预清标 URL", url2));
			if (resultboolean)
			{
				logger.error(LogUtils.format("执行预清标（含新建评标项目等） URL", url2));
				return url2;
			}
			if (StringUtils.isNotBlank(url2))
			{
				return url2;
			}
			throw new ValidateException("", resultObject.getString("message"));
		}
		catch (ValidateException e)
		{
			logger.error(e.getErrorDesc(), e);
			throw e;
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			throw new ValidateException("", e.getMessage());
		}
		// return JSON.toJSONString(params);

		// bidId:"标段id",
		// code:"项目（标段）编号",
		// tenderUnit:"招标单位",
		// agentUnit:"代理单位",
		// projectId:"项目id",
		// reviewId:"评审id",
		// name:"项目（标段）名称",
		// method:"评标办法(1:经评审2:综合评估法3:简易评标法)",
		// zipPath:"招投标文件zip包存放路径"
		// type:"工程类别",
		// evaluationTime:"评标时间(格式：yyyy-MM-dd)",
	}

	// /**
	// * 下载招投标文件zip包<br/>
	// *
	// * @param data
	// * AeolusData
	// * @throws FacadeException
	// * FacadeException
	// */
	// @Path(value = "/haimai/systemInspectZip", desc = "下载招投标文件zip包")
	// @Service
	// public void downloadSystemInspectZip(AeolusData data)
	// throws FacadeException
	// {
	// logger.debug(LogUtils.format("下载招投标文件zip包", data));
	// String sectionID = data.getParam("sectionID");
	// if (StringUtils.isEmpty(sectionID))
	// {
	// throw new ValidateException("", "标段ID不能为空!");
	// }
	// Record<String, Object> record = this.activeRecordDAO
	// .auto()
	// .table(TableName.EKB_T_SECTION_INFO)
	// .setCondition("AND", "V_BID_SECTION_ID=#{sectionID}")
	// .get(new RecordImpl<String, Object>().setColumn("sectionID",
	// sectionID));
	// if (CollectionUtils.isEmpty(record))
	// {
	// throw new ValidateException("", "找不到对应的标段信息");
	// }
	// String bidSectionCode = record.getString("V_BID_SECTION_CODE");
	// String systemInspectZipPath = SystemParamUtils
	// .getString(SysParamKey.EBIDKB_FILE_PATH_URL)
	// + "systemInspect/"
	// + bidSectionCode + ".zip";
	// File file = new File(systemInspectZipPath);
	// if (!file.exists())
	// {
	// throw new ValidateException("", "文件不存在");
	// }
	// AeolusDownloadUtils.write(data, file);
	// }

	/**
	 * 创建预清标文件<br/>
	 * 
	 * @param tpID
	 * @param sectionID
	 * @return 文件名
	 * @throws ServiceException
	 */
	private String createSystemInspectZip(String tpID, String sectionID)
			throws ServiceException
	{
		// String decryptFileSystemPath = "D:/fileEbid-fileTb_decrypt/";
		String decryptFileSystemPath = SystemParamUtils
				.getString(SysParamKey.EBIDKB_DECRYPTFILE_PATH_URL);
		Record<String, Object> bidSctionRecprd = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpID}")
				.setCondition("AND", "V_BID_SECTION_ID=#{sectionID}")
				.get(new RecordImpl<String, Object>().setColumn("tpID", tpID)
						.setColumn("sectionID", sectionID));
		if (CollectionUtils.isEmpty(bidSctionRecprd))
		{
			throw new ValidateException("", "找不到对应标段信息!");
		}
		String bidSectionCode = bidSctionRecprd.getString("V_BID_SECTION_CODE");
		// String kpFileSystemPath = "D:/fileEbid-kp/";
		String kpFileSystemPath = SystemParamUtils
				.getString(SysParamKey.EBIDKB_FILE_PATH_URL);
		String systemInspectPath = kpFileSystemPath + "systemInspect/"
				+ bidSectionCode;

		try
		{
			File systemInspectDir = new File(systemInspectPath);
			if (!systemInspectDir.exists())
			{
				systemInspectDir.mkdirs();
			}
			else
			{
				FileUtils.deleteDirectory(systemInspectDir);
				systemInspectDir.mkdirs();
			}

			List<Record<String, Object>> list = this.activeRecordDAO
					.auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID = #{tpID}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sectionID}")
					.setCondition("AND", "(N_ENVELOPE_9=1 OR N_ENVELOPE_9=2)")
					.list(new RecordImpl<String, Object>().setColumn("tpID",
							tpID).setColumn("sectionID", sectionID));

			// 控制价
			String controlPricePath = kpFileSystemPath + bidSectionCode
					+ "/控制价/控制价文件描述.json";
			copyControlPriceFile(controlPricePath, systemInspectPath,
					bidSectionCode);

			// 投标报价
			for (Record<String, Object> record : list)
			{
				String bidderCode = record.getString("V_BIDDER_ORG_CODE");
				String jsonPath = decryptFileSystemPath + bidSectionCode + "/"
						+ bidderCode + "/ZipFolder/0-目录描述.json";
				copyBidderFile(jsonPath, systemInspectPath,
						record.getString("V_BIDDER_NAME").trim());
			}

			// 压缩
			String systemInspectZip = systemInspectPath + ".zip";
			File systemInspectZipFile = new File(systemInspectZip);
			ArchiverUtils.zip(systemInspectZipFile,
					systemInspectDir.listFiles());
			String fileName = systemInspectZipFile.getName();
			// 集美接口总线导致海迈不能下载文件，因此需要把文件拷贝到指定目录，与对方进行同步。
			String zipSavePath = SystemParamUtils
					.getString(SysParamKey.SYSTEM_INSPECT_FILE_PATH);
			if (StringUtils.isEmpty(zipSavePath))
			{
				zipSavePath = "D:\\evaluation";
			}
			File zipDir = new File(zipSavePath);
			if (!zipDir.exists())
			{
				zipDir.mkdirs();
			}
			FileUtils
					.copyFile(systemInspectZipFile, new File(zipDir, fileName));
			return fileName;
		}
		catch (ServiceException e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			throw new ValidateException("", e.getMessage(), e);
		}
	}

	/**
	 * 拷贝投标人文件<br/>
	 * 
	 * @param jsonPath
	 * @param systemInspectPath
	 * @param bidderName
	 * @throws ValidateException
	 */
	private void copyBidderFile(String jsonPath, String systemInspectPath,
			String bidderName) throws ValidateException
	{
		try
		{
			File jsonFile = new File(jsonPath);
			if (!jsonFile.exists())
			{
				throw new RuntimeException("文件找不到！");
			}
			String jsonStr = FileUtils.readFileToString(jsonFile, "UTF-8");
			JSONArray jsonArray = JSON.parseArray(jsonStr);
			List<File> fileList = new ArrayList<File>();
			getBidderFileList(jsonArray, fileList);
			File bidderDir = new File(systemInspectPath, "投标报价/" + bidderName);
			if (!bidderDir.exists())
			{
				bidderDir.mkdirs();
			}
			for (File file : fileList)
			{
				File saveFile = new File(bidderDir, file.getName());
				FileUtils.copyFile(file, saveFile);
			}
		}
		catch (IOException e)
		{
			throw new ValidateException("", "拷贝[" + bidderName + "]信息出错,"
					+ e.getMessage(), e);
		}
	}

	/**
	 * 获取投标人的可拷贝的文件<br/>
	 * 
	 * @param jsonArray
	 * @param fileList
	 */
	private void getBidderFileList(JSONArray jsonArray, List<File> fileList)
	{
		if (null == jsonArray || jsonArray.size() < 1)
		{
			return;
		}
		for (int i = 0; i < jsonArray.size(); i++)
		{
			JSONObject object = jsonArray.getJSONObject(i);
			String type = object.getString("TYPE");
			if ("dir".equals(type))
			{
				getBidderFileList(object.getJSONArray("CHILDREN"), fileList);
			}
			else if ("file".equals(type))
			{
				String path = object.getString("PATH");
				if (StringUtils.contains(path, "ScalarCheckList"))
				{
					fileList.add(new File(object.getString("PATH")));
				}
			}
		}
	}

	/**
	 * 拷贝控制价文件<br/>
	 * 
	 * @param controlPricePath
	 * @param systemInspectPath
	 * @param bidSectionCode
	 * @throws ValidateException
	 */
	private void copyControlPriceFile(String controlPricePath,
			String systemInspectPath, String bidSectionCode)
			throws ValidateException
	{
		File controlPriceDir = new File(systemInspectPath, "招标控制价");
		if (!controlPriceDir.exists())
		{
			controlPriceDir.mkdirs();
		}

		try
		{
			File jsonFile = new File(controlPricePath);
			if (!jsonFile.exists())
			{
				throw new FileNotFoundException("文件路径=" + controlPricePath
						+ "---文件不存在!");
			}
			String jsonStr = FileUtils.readFileToString(jsonFile, "UTF-8");
			JSONArray array = JSON.parseArray(jsonStr);
			List<File> fileList = new ArrayList<File>();
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
						fileList.add(new File(object2.getString("FILE_PATH")));
					}
				}
			}
			for (File file : fileList)
			{
				File saveFile = new File(controlPriceDir, file.getName());
				FileUtils.copyFile(file, saveFile);
			}
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			throw new ValidateException("", "招标控制价文件处理失败," + e.getMessage(), e);
		}
	}

	/**
	 * 获取控制价，暂列金额总和，专业工程暂估价总和<br/>
	 * 
	 * @return Record<String,Object> <br/>
	 *         {ZYGCZGJ=0.0, ZLJE=401050.0, GCZJ=1.825235718E7} <br/>
	 *         专业工程暂估价ZYGCZGJ,暂列金额ZLJE,获取控制价GCZJ
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "/haimai/price", desc = "获取控制价，暂列金额总和，专业工程暂估价总和")
	@Service
	@Deprecated
	public Record<String, Object> getPrices() throws ServiceException
	{
		String tpID = SessionUtils.getTPID();
		ActiveRecordDAO dao = getActiveRecordDAO();
		Record<String, Object> bidSectionRecord = dao
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.get(new RecordImpl<String, Object>().setColumn("V_TPID", tpID));
		if (CollectionUtils.isEmpty(bidSectionRecord))
		{
			throw new ValidateException("", "找不到标段信息!");
		}
		String bidSectionCode = bidSectionRecord
				.getString("V_BID_SECTION_CODE");
		String kpFileSystemPath = SystemParamUtils
				.getString(SysParamKey.EBIDKB_FILE_PATH_URL);
		String controlPricePath = kpFileSystemPath + bidSectionCode
				+ "/控制价/控制价文件描述.json";
		String xmlFilePath = getControlPriceXmlFile(controlPricePath,
				bidSectionCode);
		Map<String, Double> prices = BaValidationKit.getPrices(xmlFilePath);
		RecordImpl<String, Object> result = new RecordImpl<String, Object>();
		result.putAll(prices);
		return result;
	}

	/**
	 * 拷贝控制价文件<br/>
	 * 
	 * @param controlPricePath
	 * @param systemInspectPath
	 * @param bidSectionCode
	 * @throws ValidateException
	 */
	private String getControlPriceXmlFile(String controlPricePath,
			String bidSectionCode) throws ValidateException
	{
		try
		{
			File jsonFile = new File(controlPricePath);
			if (!jsonFile.exists())
			{
				throw new FileNotFoundException("文件不存在!");
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
			logger.error(e.getMessage(), e);
			throw new ValidateException("", "招标控制价文件处理失败," + e.getMessage(), e);
		}
		throw new ValidateException("", "找不到对应控制价XML文件");
	}

	/**
	 * 
	 * 获取预清标状态<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/haimai/getProjectStateUrl", desc = "获取预清标状态")
	@Service
	@Deprecated
	public ResultVO<Record<String, Object>> getProjectState(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取预清标状态", data));
		String tpID = SessionUtils.getTPID();
		Record<String, Object> bidSectionRecord = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.get(new RecordImpl<String, Object>().setColumn("V_TPID", tpID));
		if (CollectionUtils.isEmpty(bidSectionRecord))
		{
			throw new ValidateException("", "找不到标段信息!");
		}
		String sectionID = bidSectionRecord.getString("V_BID_SECTION_ID");
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("bidId", sectionID);
		record.setColumn("projectId", tpID);
		record.setColumn("reviewId", sectionID + "-1");
		Record<String, String> dataJson = new RecordImpl<String, String>();
		dataJson.setColumn("dataJson", JSON.toJSONString(record));

		String haimaiUrl = SystemParamUtils
				.getString("com.sozone.ekb.haimaiUrl");
		String haimaiUrlPath = "/rest/getProjectState";
		try
		{
			logger.error(LogUtils.format("获取预清标状态", haimaiUrl, dataJson));
			String result = "";
			if (haimaiUrl.toLowerCase().startsWith(UtilCollaGEN.PROTOCOL))
			{
				result = UtilCollaGEN.post(haimaiUrl, haimaiUrlPath, dataJson);
			}
			else
			{
				result = HttpClientUtils.doPost(haimaiUrl + haimaiUrlPath,
						dataJson);
			}
			logger.error(LogUtils.format("获取预清标状态 结果", result));
			JSONObject resultObject = JSON.parseObject(result);
			Boolean resultBoolean = resultObject.getBoolean("result");
			if (resultBoolean == null || !resultBoolean)
			{
				throw new ValidateException("", "获取预清标状态:"
						+ resultObject.getString("message"));
			}

			JSONObject dataObj = resultObject.getJSONObject("data");
			Boolean isCompletedObj = dataObj.getBoolean("isCompleted");
			if (isCompletedObj == null || !isCompletedObj)
			{
				StringBuilder builder = new StringBuilder();
				// 预清标失败信息
				String errorMsg = dataObj.getString("errorMsg");
				builder.append("预清标失败信息").append(errorMsg);
				JSONArray bidderErrorMsgs = dataObj
						.getJSONArray("bidderErrorMsg");
				if (null != bidderErrorMsgs)
				{
					for (int i = 0; i < bidderErrorMsgs.size(); i++)
					{
						JSONObject bidderErrorMsg = bidderErrorMsgs
								.getJSONObject(i);
						builder.append("[")
								.append(bidderErrorMsg.getString("biddername"))
								.append("]")
								.append(bidderErrorMsg.getString("errorMsg"));
					}
				}
				throw new ValidateException("", builder.toString());
			}
		}
		catch (FacadeException e)
		{
			logger.error("获取预清标状态 结果异常" + e.getErrorDesc(), e);
			throw e;
		}
		catch (Exception e)
		{
			logger.error("获取预清标状态 结果异常" + e.getMessage(), e);
			throw new ValidateException("", e.getMessage(), e);
		}
		return new ResultVO<Record<String, Object>>(true);
	}

}
