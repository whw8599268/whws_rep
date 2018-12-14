package com.sozone.eokb.bus.decrypt;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import net.sf.jxls.transformer.XLSTransformer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.download.util.AeolusDownloadUtils;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.ClassLoaderUtils;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.TimestampUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.flow.common.NodeStatus;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 
 * 投标人解密服务接口<br/>
 * <p>
 * 投标人解密服务接口<br/>
 * </p>
 * Time：2017-8-29 下午4:06:11<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
@Path(value = "/bus/decrypt", desc = "投标人解密业务服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class DecryptBus
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(DecryptBus.class);

	/**
	 * 保函EXCEL模板路径
	 */
	private static String lgExcelPath = "com/sozone/eokb/bus/decrypt/lg_bidder_info_template.xls";

	/**
	 * 开标EXCEL模板路径
	 */
	private static String eokbExcelPath = "com/sozone/eokb/bus/decrypt/bidder_info_template.xls";

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
	 * 获取解密情况<br/>
	 * <p>
	 * 获取解密情况
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 投标人列表
	 * @throws FacadeException
	 *             facade异常
	 */
	@Path(value = "/info", desc = "投标人获取解密情况")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getBidderDecryptInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("投标人获取解密情况", data));
		Record<String, Object> params = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		String orgCode = SessionUtils.getCompanyCode();
		params.setColumn("tpid", tpid);
		// 判断是否为投标人
		if (SessionUtils.isBidder())
		{
			params.setColumn("org_code", orgCode);
		}
		// 获取解密情况
		return this.activeRecordDAO.statement().selectList(
				"decrypt.getBidderDecryptInfo", params);
	}

	/**
	 * 
	 * 获取解密环节状态<br/>
	 * <p>
	 * </p>
	 * 
	 * @param nodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 返回结果
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/status/{nid}", desc = "获取解密环节状态")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Record<String, Object> getDecryptLinkStatus(
			@PathParam("nid") String nodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取解密环节状态", data));
		if (StringUtils.isEmpty(nodeID))
		{
			throw new ServiceException("", "找不到当前节点ID!");
		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		// 状态：-1：已解密,0:尚未开始,1：进行中,2：解密时间结束,3解密环节结束
		int status = 0;

		// 如果是投标人,且解密过
		if (SessionUtils.isBidder() && isDecrypted())
		{
			result.setColumn("DECRYPT_STATUS", -1);
			return result;
		}
		// 判断解密环节是否已经完成
		Record<String, Object> noedInfo = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE).get(nodeID);
		if (CollectionUtils.isEmpty(noedInfo))
		{
			throw new ServiceException("", "找不到当前节点信息!");
		}
		if (NodeStatus.HaveClosed.getStatus() == noedInfo
				.getInteger("N_STATUS"))
		{
			// 解密环节已结束
			result.setColumn("DECRYPT_STATUS", 3);
			return result;
		}
		// 获取开标时间
		String bidOpenTime = SessionUtils.getBidOpenTime();
		if (StringUtils.isEmpty(bidOpenTime))
		{
			throw new ServiceException("", "开标时间为空!");
		}
		// 开标时间戳
		long bidOpenTimestamp = TimestampUtils.getTimestamp(bidOpenTime,
				"yyyy-MM-dd HH:mm:ss");

		// 解密结束时间戳
		long decryptEndTime = getDecryptEndTimestamp(nodeID);
		// 当前时间戳
		long currentTime = System.currentTimeMillis();
		// 未开始解密
		if (currentTime < bidOpenTimestamp)
		{
			// 未开始
			status = 0;
		}
		// 正在进行中
		else if (currentTime < decryptEndTime)
		{
			// 进行中
			status = 1;
		}
		else if (currentTime >= decryptEndTime)
		{
			// 解密时间已结束
			status = 2;
		}

		result.setColumn("CURRENT_TIME", currentTime);
		result.setColumn("DECRYPT_END_TIME", decryptEndTime);
		result.setColumn("DECRYPT_STATUS", status);
		// 高速的需要在页面提示信封开启状态
		if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_GSGL_TYPE,
				SessionUtils.getTenderProjectType()))
		{
			result.setColumn("BID_OPEN_MSG", getBidOpenMsg());
		}
		return result;
	}

	/**
	 * 
	 * 获取开标状态<br/>
	 * <p>
	 * 获取开标状态
	 * </p>
	 * 
	 * @return
	 */
	private String getBidOpenMsg() throws FacadeException
	{
		logger.debug(LogUtils.format("获取开标状态"));

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());

		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		// 0：第一信封未开启，1：第一信封已开启，2：第二信封已开启
		int bidOpenStatus = 0;
		for (Record<String, Object> section : sections)
		{
			// 第一信封已开启
			if (StringUtils.equals("2-1",
					section.getString("V_BID_OPEN_STATUS"))
					&& bidOpenStatus < 1)
			{
				bidOpenStatus = 1;
			}
			// 第二信封已开启或开标结束
			if ((StringUtils.equals("2-2",
					section.getString("V_BID_OPEN_STATUS")) || StringUtils
					.equals("2", section.getString("V_BID_OPEN_STATUS")))
					&& bidOpenStatus < 2)
			{
				bidOpenStatus = 2;
			}
		}

		if (1 == bidOpenStatus)
		{
			return "第一数字信封已开启，第二数字信封未开启";
		}
		if (2 == bidOpenStatus)
		{
			return "第一数字信封已开启，第二数字信封已开启";
		}

		return "第一信封未开启，第二数字信封未开启";
	}

	/**
	 * 
	 * 根据投标人是否解密过<br/>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private boolean isDecrypted() throws ServiceException
	{
		boolean group = SessionUtils.isSectionGroup();
		String tpid = SessionUtils.getTPID();
		int count = 0;
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("orgcode", SessionUtils.getCompanyCode());
		// 有标段组的情况下查解密信息表
		if (group)
		{
			count = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_DECRYPT_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BIDDER_ORG_CODE = #{orgcode}")
					.count(param);
			return 0 < count;
		}
		// 非标段组情况下直接查投标人列表
		count = this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BIDDER_ORG_CODE = #{orgcode}")
				.count(param);
		return 0 < count;
	}

	/**
	 * 获取解密提示信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param nodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 返回结果
	 * @throws FacadeException
	 *             服务异常
	 */
	@Path(value = "/tip/{nid}", desc = "获取解密提示信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ResultVO<String> getDecryptTipInfo(@PathParam("nid") String nodeID,
			AeolusData data) throws FacadeException
	{
		ResultVO<String> result = new ResultVO<String>(true);
		String tpid = SessionUtils.getTPID();
		long decryptEndTime = getDecryptEndTimestamp(nodeID);
		long currentTime = System.currentTimeMillis();
		if (decryptEndTime < currentTime)
		{
			return result;
		}
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", tpid);
		// // 投递家数
		// int deliverCount = this.activeRecordDAO.statement().getOne(
		// "decrypt.getDeliveryCount", params);
		// // 解密成功家数
		// int decryptCount = this.activeRecordDAO.statement().getOne(
		// "decrypt.getDecryptSuccessCount", params);
		// result.setResult("投标人投递家数：[" + deliverCount + "]，已完成解密家数：["
		// + decryptCount + "]，是否马上进入下一环节？");
		result.setResult("当前解密时间尚未结束，是否马上进入下一环节？");
		result.setSuccess(false);
		return result;
	}

	/**
	 * 
	 * 获取解密结束时间戳<br/>
	 * <p>
	 * 获取解密结束时间戳
	 * </p>
	 * 
	 * @param nodeId
	 *            流程节点ID
	 * @return 解密结束时间戳
	 * @throws ServiceException
	 *             服务异常
	 */
	private long getDecryptEndTimestamp(String nodeID) throws ServiceException
	{
		String bidOpenTime = SessionUtils.getBidOpenTime();
		if (StringUtils.isEmpty(bidOpenTime))
		{
			throw new ServiceException("", "开标时间为空!");
		}
		// 开标时间
		long begin = TimestampUtils.getTimestamp(bidOpenTime,
				"yyyy-MM-dd HH:mm:ss");
		String tpType = SessionUtils.getTenderProjectType();
		// 如果是高速公路
		if (StringUtils.equals(
				ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_GSGL_TYPE, tpType))
		{

			String sql = "SELECT N_START_TIME FROM "
					+ TableName.TENDER_PROJECT_NODE_STATUS_TIME
					+ " WHERE ID = #{nid} AND N_STATUS = #{status}";
			Record<String, Object> rs = this.activeRecordDAO.sql(sql)
					.setParam("nid", nodeID)
					.setParam("status", NodeStatus.InProgress.getStatus())
					.get();
			if (CollectionUtils.isEmpty(rs))
			{
				throw new ServiceException("", "无法获取解密环节开始时间!");
			}
			begin = rs.getLong("N_START_TIME");
		}
		// 如果代理有设置解密时间，优先读取项目表上的时间
		String tpid = SessionUtils.getTPID();
		Record<String, Object> project = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO).get(tpid);

		JSONObject jsonObj = project.getJSONObject("V_JSON_OBJ");

		// 解密时长
		long time;
		if (!CollectionUtils.isEmpty(jsonObj)
				&& !StringUtils.isEmpty(jsonObj.getString("decrypt_time")))
		{
			// 分钟转换为毫秒
			time = jsonObj.getInteger("decrypt_time") * 60 * 1000;
			// 开标解密结束时间=开标时间+解密时长
			return begin + time;
		}
		// 获取评标办法编码
		String bemCode = SessionUtils.getTenderProjectTypeCode();
		// 系统运行参数
		String paramKey = SysParamKey.DECRYPT_TIME_KEY_PREFIX + bemCode;
		// 缺省一小时
		time = SystemParamUtils.getLong(paramKey,
				ConstantEOKB.DEFAULT_DECRYPT_TIME);
		// 开标解密结束时间=开标时间+解密时长
		return begin + time;
	}

	/**
	 * 
	 * 导出投标人信息列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/export_bidders", desc = "导出投标人信息列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void exportBidderList(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("导出投标人信息列表", data));
		HttpServletResponse response = data.getHttpServletResponse();
		String fileName = "投标人信息列表.xls";
		String mimeType = AeolusDownloadUtils.getMimeType(fileName);
		response.setContentType(mimeType);
		response.setHeader("Content-Disposition", "attachment;filename="
				+ AeolusDownloadUtils.encodeFileName(data, fileName));
		String tpid = SessionUtils.getTPID();
		boolean group = SessionUtils.isSectionGroup();
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", tpid);
		// 非标段组情况下直接查投标人列表
		params.setColumn("tableName", TableName.EKB_T_TENDER_LIST);
		// 有标段组的情况下查解密信息表
		if (group)
		{
			params.setColumn("tableName", TableName.EKB_T_DECRYPT_INFO);
		}
		// 投标人信息列表
		List<Record<String, Object>> bidders = this.activeRecordDAO.statement()
				.selectList("decrypt.getBidderInfo", params);

		// 联合体投标列表
		List<Record<String, Object>> unionContactList = new ArrayList<Record<String, Object>>();
		// union_enterprise_name 判断该唱标字段是否有值
		JSONObject jobj = null;
		JSONArray jarr = null;
		for (Record<String, Object> bidder : bidders)
		{
			jobj = bidder.getJSONObject("V_JSON_OBJ");
			if (!CollectionUtils.isEmpty(jobj))
			{
				jarr = jobj.getJSONArray("objSing");
				for (int i = 0; i < jarr.size(); i++)
				{
					jobj = jarr.getJSONObject(i);
					if (StringUtils.isNotBlank(jobj
							.getString("union_enterprise_name"))
							&& jobj.getString("union_enterprise_name").trim()
									.length() > 2)
					{
						unionContactList
								.add(new RecordImpl<String, Object>().setColumn(
										"V_BIDDER_NAME",
										jobj.getString("union_enterprise_name")));
						break;
					}
				}
			}
		}
		// 合并联合体投标
		bidders.addAll(unionContactList);
		params.setColumn("bidders", bidders);

		InputStream input = null;
		OutputStream out = null;
		try
		{
			input = ClassLoaderUtils.getResourceAsStream(eokbExcelPath,
					this.getClass());
			XLSTransformer transformer = new XLSTransformer();
			transformer.groupCollection("department.staff");
			Workbook resultWorkbook = transformer.transformXLS(input, params);
			// 获取输出流
			out = response.getOutputStream();
			resultWorkbook.write(out);
			out.flush();
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("导出投标人信息列表发生异常!"), e);
			throw new ServiceException("", "导出投标人信息列表发生异常!", e);
		}
		finally
		{
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * 
	 * 导出投标人信息列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/elgbl", desc = "导出保函投标人信息列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void exportLgBidderList(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("导出保函投标人信息列表", data));
		HttpServletResponse response = data.getHttpServletResponse();
		String fileName = "保函投标人信息列表.xls";
		String mimeType = AeolusDownloadUtils.getMimeType(fileName);
		response.setContentType(mimeType);
		response.setHeader("Content-Disposition", "attachment;filename="
				+ AeolusDownloadUtils.encodeFileName(data, fileName));
		String tpid = SessionUtils.getTPID();
		boolean group = SessionUtils.isSectionGroup();
		String tableName = TableName.EKB_T_TENDER_LIST;
		if (group)
		{
			tableName = TableName.EKB_T_DECRYPT_INFO;
		}

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		// 获取所有的标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.list(param);

		if (CollectionUtils.isEmpty(sections))
		{
			throw new FacadeException("", "获取标段信息失败");
		}

		List<List<Record<String, Object>>> sheetDatas = new LinkedList<List<Record<String, Object>>>();
		List<String> sheetNames = new LinkedList<String>();

		List<Record<String, Object>> bidders = this.activeRecordDAO.auto()
				.table(tableName).setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("GROUP BY", "V_BIDDER_ORG_CODE")
				.addSortOrder("V_BIDDER_NO", "ASC").list(param);
		// 第一个工作簿添加所有的投标人信息
		sheetDatas.add(bidders);
		sheetNames.add("投标人信息");
		// 其他工作簿添加所有的标段对应的投标人信息
		// 循环获取所有的标段对应的投标人信息
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			bidders = this.activeRecordDAO.auto().table(tableName)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			sheetDatas.add(bidders);
			sheetNames.add(section.getString("V_BID_SECTION_NAME"));
		}

		InputStream input = null;
		OutputStream out = null;
		try
		{
			input = ClassLoaderUtils.getResourceAsStream(lgExcelPath,
					this.getClass());
			XLSTransformer transformer = new XLSTransformer();
			transformer.groupCollection("department.staff");
			Workbook resultWorkbook = transformer.transformMultipleSheetsList(
					input, sheetDatas, sheetNames, "bidders",
					new HashMap<Object, Object>(), 0);
			// 获取输出流
			out = response.getOutputStream();
			resultWorkbook.write(out);
			out.flush();
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("导出投标人信息列表发生异常!"), e);
			throw new ServiceException("", "导出投标人信息列表发生异常!", e);
		}
		finally
		{
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * 
	 * 获取解密汇总信息<br/>
	 * <p>
	 * 获取解密汇总信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 获取解密汇总信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/summary", desc = "获取解密汇总信息")
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getDecryptSummaryInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取解密汇总信息", data));
		String tpid = SessionUtils.getTPID();
		boolean group = SessionUtils.isSectionGroup();
		// 如果是标段组的情况
		if (group)
		{
			// 个标段组投递情况汇总
			return this.activeRecordDAO.statement().selectList(
					"decrypt.getDecryptSummary_Group", tpid);
		}
		// 非标段组情况下
		return this.activeRecordDAO.statement().selectList(
				"decrypt.getDecryptSummary", tpid);
	}
}
