/**
 * 包名：com.sozone.eokb.bus.decrypt
 * 文件名：ThreePartsDecrypt.java<br/>
 * 创建时间：2017-11-28 下午2:00:20<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.MediaType;
import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.TimestampUtils;
import com.sozone.auth2.client.utils.RSCallUtils;
import com.sozone.auth2.client.utils.RSCallUtils.ContentType;
import com.sozone.eokb.bus.decrypt.service.BidderDocumentParseService;
import com.sozone.eokb.bus.decrypt.service.BidderDocumentParseServiceFactory;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.flow.common.NodeStatus;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.MsgUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 三层解密接口实现<br/>
 * <p>
 * 三层解密接口实现<br/>
 * </p>
 * Time：2017-11-28 下午2:00:20<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
@Path(value = "/bus/thpdecrypt", desc = "三层解密接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class ThreePartsDecrypt
{

	/**
	 * 项目解密锁
	 */
	private static final Map<String, ReentrantLock> PROJECT_DECRYPT_LOCK = new ConcurrentHashMap<String, ReentrantLock>();

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(ThreePartsDecrypt.class);

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
	 * 获取招标项目解密锁<br/>
	 * <p>
	 * 为了防止同一个项目在同一个解密时间段内重复解密,所有为每一个项目加了一个同步锁,该方法用于创建项目单例互斥锁
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 互斥锁锁对象
	 */
	private static synchronized ReentrantLock getTPDecryptLock(String tpid)
	{
		// 获取项目锁
		ReentrantLock lock = PROJECT_DECRYPT_LOCK.get(tpid);
		// 如果锁不存在
		if (null == lock)
		{
			// 构建互斥锁
			lock = new ReentrantLock();
			PROJECT_DECRYPT_LOCK.put(tpid, lock);
		}
		return lock;
	}

	/**
	 * <p>
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private BidderDocumentParseService getBidderDocumentParseService()
			throws ServiceException
	{
		return BidderDocumentParseServiceFactory
				.getBidderDocumentParseServiceInstance();
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
	 *             Facade异常
	 */
	@Path(value = "/bidders", desc = "投标人获取解密情况")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getBidderDecryptPage(AeolusData data)
			throws FacadeException
	{
		logger.info(LogUtils.format("投标人获取解密情况", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> params = data.getRecord();
		String tpid = SessionUtils.getTPID();
		String orgCode = SessionUtils.getCompanyCode();
		params.setColumn("tpid", tpid);
		// 判断是否为投标人
		if (SessionUtils.isBidder())
		{
			params.setColumn("org_code", orgCode);
		}
		// 获取解密情况
		return this.activeRecordDAO.statement().selectPage(
				"decrypt.getBidderDecryptInfo", pageable, params);
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
		// 获取评标办法编码
		String bemCode = SessionUtils.getTenderProjectTypeCode();
		// 系统运行参数
		String paramKey = SysParamKey.DECRYPT_TIME_KEY_PREFIX + bemCode;
		// 缺省一小时
		long time = SystemParamUtils.getLong(paramKey,
				ConstantEOKB.DEFAULT_DECRYPT_TIME);
		// 开标解密结束时间=开标时间+解密时长
		return begin + time;
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
		logger.info(LogUtils.format("获取解密环节状态", data));
		if (StringUtils.isEmpty(nodeID))
		{
			throw new ServiceException("", "找不到当前节点ID!");
		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		// 状态：-1：已解密,0:尚未开始,1：进行中,2：解密时间结束,3解密环节结束,4尚未解析结束
		int status = 0;
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

		// 获取解析状态
		Boolean pbfs = SessionUtils.getAttribute("$_PARSE_BIDDER_FINISH");
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
		// 如果没有解析完成
		if (null == pbfs || !pbfs)
		{
			status = 4;
		}
		// 未开始解密
		else if (currentTime < bidOpenTimestamp)
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
		return result;
	}

	/**
	 * 获取投标源文件方法<br/>
	 * <p>
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private List<Record<String, Object>> getBidderDocList()
			throws ServiceException
	{
		String tpid = SessionUtils.getTPID();
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", tpid);
		String orgcode = null;
		List<Record<String, Object>> result = new ArrayList<Record<String, Object>>();
		// 获取投标人投递信息
		List<Record<String, Object>> docs = this.activeRecordDAO.statement()
				.selectList("ThreePartsDecrypt.getBidderDeliveryInfo", params);
		if (CollectionUtils.isEmpty(docs))
		{
			throw new ServiceException("", "找不到对应投标文件！");
		}
		for (Record<String, Object> doc : docs)
		{
			orgcode = doc.getString("V_ORG_CODE");
			Record<String, Object> record = new RecordImpl<String, Object>();
			record.setColumn("ID", doc.getString("ID"));
			record.setColumn("V_FTID", doc.getString("V_FTID"));
			String bidSectionCode = doc.getString("V_BID_SECTION_CODE");
			record.setColumn(
					"V_SOURCE_PATH",
					new File(doc.getString("V_FOLDERPATHALL"), doc
							.getString("V_FILEADDR")));
			record.setColumn(
					"V_TEMP_DIR_PATH",
					new File(
							SystemParamUtils
									.getString(SysParamKey.EBIDKB_DECRYPTFILE_TEMP_PATH_URL)
									+ bidSectionCode + "/" + orgcode));
			// 投标文件（解密后）D:\fileEbid-fileTb_decrypt\标段包编号\当前投标人组织机构代码号
			record.setColumn(
					"V_TARGET_DIR_PATH",
					new File(SystemParamUtils
							.getString(SysParamKey.EBIDKB_DECRYPTFILE_PATH_URL)
							+ bidSectionCode + "/" + orgcode));
			result.add(record);
		}
		return result;
	}

	/**
	 * 写出解析响应<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 * @param msg
	 */
	private void writeParseResponse(AeolusData data, String funName,
			Record<String, Object> msg)
	{
		HttpServletResponse response = data.getHttpServletResponse();
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		PrintWriter out = null;
		StringBuilder sb = new StringBuilder();
		sb.append("<script type=\"text/javascript\">\r\n");
		String paramName = "vjs_" + Random.generateUUID();
		sb.append("var ").append(paramName).append(" = ")
				.append(JSON.toJSONString(msg)).append(";\r\n");
		sb.append("parent.").append(funName).append("(").append(paramName)
				.append(");\r\n</script>");
		logger.info(LogUtils.format("发送客户端消息", sb));
		try
		{
			out = response.getWriter();
			out.write(sb.toString());
			out.flush();
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("写出解析响应时发生异常!"), e);
		}
	}

	/**
	 * 解析投标文件<br/>
	 * <p>
	 * 解析投标文件
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             Facade异常
	 */
	// 定义路径
	@Path(value = "parse", desc = "解析投标文件")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void parseBidderDocument(AeolusData data) throws FacadeException
	{
		logger.info(LogUtils.format("解析投标文件", data));
		// 招标项目名称
		String tpName = SessionUtils.getTenderProjectInfo().getString(
				"V_TENDER_PROJECT_NAME");
		// 评标办法编码
		String code = SessionUtils.getTenderProjectTypeCode();
		String userID = ApacheShiroUtils.getCurrentUserID();
		String tpid = SessionUtils.getTPID();
		String funName = "disposeParseProgress";
		// 消息flag 0开始,200结束,1正常进行,-1错误,-99被占用
		Record<String, Object> msg = new RecordImpl<String, Object>();
		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPDecryptLock(tpid);
		// 尝试获取锁，如果已经被其他线程占用
		if (!lock.tryLock())
		{
			logger.error(LogUtils.format("已经存在解密线程,用户重复刷新!", tpName, code,
					userID, tpid, data));
			msg.clear();
			msg.setColumn("FLAG", -99);
			msg.setColumn("MSG", "当前招标项目正在被解密中,请勿重复刷新!");
			writeParseResponse(data, funName, msg);
			// 获取锁,这一步其实就是为了让线程挂起
			lock.lock();
		}
		// -----------------------------------------------------
		try
		{
			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("tpid", tpid);
			// 获取未解析的数量
			int count = this.activeRecordDAO.statement().getOne(
					"ThreePartsDecrypt.getNotParseDeliveryCount", param);
			logger.info(LogUtils.format("总共要解析" + count + "个投标文件!"));
			// 消息flag 0开始,200结束,1正常进行,-1错误
			msg.clear();
			msg.setColumn("FLAG", 0);
			writeParseResponse(data, funName, msg);
			//
			if (count > 0)
			{
				List<Record<String, Object>> docs = getBidderDocList();
				// 源文件
				File source = null;
				// 临时目录
				File tempDir = null;
				// 目标目录
				File targetDir = null;
				int index = 0;
				// 临时
				Record<String, Object> temp = null;
				for (Record<String, Object> doc : docs)
				{
					// 设置投递表ID,一定要在设置主键之前做
					doc.setColumn("V_TBID", doc.getString("ID"));
					source = doc.getColumn("V_SOURCE_PATH");
					logger.info(LogUtils.format("开始解析投标文件["
							+ source.getAbsolutePath() + "]"));
					doc.setColumn("V_SOURCE_PATH", FilenameUtils
							.separatorsToUnix(source.getAbsolutePath()));
					tempDir = doc.getColumn("V_TEMP_DIR_PATH");
					doc.setColumn("V_TEMP_DIR_PATH", FilenameUtils
							.separatorsToUnix(tempDir.getAbsolutePath()));
					targetDir = doc.getColumn("V_TARGET_DIR_PATH");
					doc.setColumn("V_TARGET_DIR_PATH", FilenameUtils
							.separatorsToUnix(targetDir.getAbsolutePath()));
					// 解析投标文件
					try
					{
						temp = DecryptBidder.parseBidderDocument(source,
								tempDir);
						doc.putAll(temp);
						// 这边不要重新设置了,直接用投递表的ID即可，解决临时表重复插入问题2018-6-7 11:24:32
						// doc.setColumn("ID", Random.generateUUID());
						doc.setColumn("V_FTID", doc.getString("V_FTID"));
						doc.setColumn("V_BEM_CODE", code);
						doc.setColumn("N_CREATE_TIME",
								System.currentTimeMillis());
						doc.setColumn("V_CREATE_USER", userID);
						doc.setColumn("V_TPID", tpid);
						doc.setColumn("N_STATUS", 0);
						// 先删除后插入
						this.activeRecordDAO.auto()
								.table(TableName.DECRYPT_TEMP)
								.remove(doc.getString("ID"));
						this.activeRecordDAO.auto()
								.table(TableName.DECRYPT_TEMP).save(doc);
					}
					catch (Exception e)
					{
						MsgUtils.send("开标投标解析：失败，招标项目[" + tpName + "]中投标人["
								+ doc.getString("V_BIDDER_NAME") + "]，请处理!");
						msg.clear();
						msg.setColumn("FLAG", -1);
						msg.setColumn("MSG", "解析[" + source.getAbsolutePath()
								+ "]文件失败!");
						writeParseResponse(data, funName, msg);
						logger.error(
								LogUtils.format("解析投标文件发生异常!", doc, source), e);
					}
					index++;
					// 输出进度情况
					DecimalFormat df = new DecimalFormat("0.00");
					String num = df.format((float) index / count);
					msg.clear();
					msg.setColumn("FLAG", 1);
					msg.setColumn("TEXT", index + "/" + count);
					msg.setColumn("VALUE", num);
					writeParseResponse(data, funName, msg);
				}
			}
			SessionUtils.setAttribute("$_PARSE_BIDDER_FINISH", true);
			msg.clear();
			msg.setColumn("FLAG", 200);
			writeParseResponse(data, funName, msg);
		}
		finally
		{
			// 如果已经没有线程等待获取锁
			if (!lock.hasQueuedThreads())
			{
				// 干掉锁
				PROJECT_DECRYPT_LOCK.remove(tpid);
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	/**
	 * 校验证书的有效性<br/>
	 * <p>
	 * </p>
	 * 
	 * @param type
	 *            类型,0:招标人/招标代理,1:交易中心,2:公证,3公证或者交易中心
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             Facade异常
	 */
	// 定义路径
	@Path(value = "verify/{type}", desc = "校验证书的有效性")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public ResultVO<Integer> validateCertificate(@PathParam("type") int type,
			AeolusData data) throws FacadeException
	{
		logger.info(LogUtils.format("校验证书的有效性", type, data));
		ResultVO<Integer> result = new ResultVO<Integer>(true);
		// 获取招标项目信息
		Record<String, Object> tp = SessionUtils.getTenderProjectInfo();
		if (CollectionUtils.isEmpty(tp))
		{
			logger.error(LogUtils.format("无法从SESSION中获取招标项目信息!"));
			throw new ServiceException("", "无法从SESSION中获取招标项目信息!");
		}
		// 证书base64
		String certData = data.getParam("CERT_DATA");
		// 证书序列号
		String certSerial = data.getParam("CERT_SERIAL");

		if (StringUtils.isEmpty(certSerial) || StringUtils.isEmpty(certData))
		{
			logger.error(LogUtils.format("无法从请求参数中获取证书内容或证书序列号!"));
			throw new ServiceException("", "无效的证书!");
		}
		// 获取三方证书信息
		JSONObject cers = getThreePartyCertificates(
				tp.getString("V_TENDERER_CODE"),
				tp.getString("V_TENDER_AGENCY_CODE"), tp.getString("V_JYCS"));
		logger.info(LogUtils.format("获取到的三方证书内容为：", cers));
		// 招标人/招标代理
		if (0 == type)
		{
			// 招标人
			JSONArray zbrCers = cers.getJSONArray("TENDER_CERS");
			// 招标代理
			JSONArray zbdlCers = cers.getJSONArray("TENDER_AGENCY_CERS");

			// 如果同时为空
			if (CollectionUtils.isEmpty(zbrCers)
					&& CollectionUtils.isEmpty(zbdlCers))
			{
				logger.error(LogUtils.format("从云盾未获取到任何的招标人/代理证书信息!"));
				throw new ServiceException("", "无效的招标人/代理证书!");
			}
			JSONArray all = new JSONArray();
			all.addAll(zbdlCers);
			all.addAll(zbrCers);
			if (!verify(certData, certSerial, all))
			{
				logger.error(LogUtils.format("无效的招标人/代理证书!"));
				throw new ServiceException("", "无效的招标人/代理证书!");
			}
		}
		// 交易中心
		else if (1 == type)
		{
			JSONArray jyzxCers = cers.getJSONArray("TRADING_CENTER_CERS");
			// 如果同时为空
			if (CollectionUtils.isEmpty(jyzxCers))
			{
				logger.error(LogUtils.format("从云盾未获取到任何的交易中心证书信息!"));
				throw new ServiceException("", "无效的交易中心证书!");
			}
			if (!verify(certData, certSerial, jyzxCers))
			{
				logger.error(LogUtils.format("无效的交易中心证书!"));
				throw new ServiceException("", "无效的交易中心证书!");
			}
		}
		// 公证
		else if (2 == type)
		{
			JSONArray gzCers = cers.getJSONArray("NOTARIZATION_CERS");
			// 如果同时为空
			if (CollectionUtils.isEmpty(gzCers))
			{
				logger.error(LogUtils.format("从云盾未获取到任何的公证证书信息!"));
				throw new ServiceException("", "无效的公证证书!");
			}
			if (!verify(certData, certSerial, gzCers))
			{
				logger.error(LogUtils.format("无效的公证证书!"));
				throw new ServiceException("", "无效的公证证书!");
			}
		}
		else if (3 == type)
		{
			// 交易中心的证书
			JSONArray jyzxCers = cers.getJSONArray("TRADING_CENTER_CERS");
			// 公证的证书
			JSONArray gzCers = cers.getJSONArray("NOTARIZATION_CERS");
			// 如果同时为空
			if (CollectionUtils.isEmpty(jyzxCers)
					&& CollectionUtils.isEmpty(gzCers))
			{
				logger.error(LogUtils.format("从云盾未获取到任何的交易中心证书信息与公证证书信息!"));
				throw new ServiceException("", "无效的交易中心证书!");
			}
			// 如果是交易中心
			if (!CollectionUtils.isEmpty(jyzxCers)
					&& verify(certData, certSerial, jyzxCers))
			{
				type = 1;
			}
			// 如果是公证
			else if (!CollectionUtils.isEmpty(gzCers)
					&& verify(certData, certSerial, gzCers))
			{
				type = 2;
			}
			else
			{
				logger.error(LogUtils.format("无效的交易中心或公证证书!"));
				throw new ServiceException("", "无效的交易中心或公证证书!");
			}
		}
		else
		{
			logger.error(LogUtils.format("无效的验证类型!"));
			throw new ServiceException("", "无效的验证类型!");
		}
		result.setResult(type);
		return result;
	}

	/**
	 * 校验证书有效性<br/>
	 * <p>
	 * </p>
	 * 
	 * @param certData
	 * @param certSerial
	 * @param cers
	 * @return
	 */
	private boolean verify(String certData, String certSerial, JSONArray cers)
	{
		certData = StringUtils.replace(certData, "\r", "");
		certData = StringUtils.replace(certData, "\n", "");
		// 序列号
		String serial = null;
		// 证书数据
		String cdata = null;
		JSONObject jobj = null;
		for (int i = 0; i < cers.size(); i++)
		{
			jobj = cers.getJSONObject(i);
			serial = jobj.getString("SERIAL");
			if (StringUtils.equals(certSerial, serial))
			{
				cdata = jobj.getString("CERTDATA");
				return StringUtils.equals(cdata, certData);
			}
		}
		return false;
	}

	/**
	 * 获取三方证书<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderCode
	 * @param tenderAgencyCode
	 * @param tradingPlaceCode
	 * @return
	 * @throws ServiceException
	 */
	private JSONObject getThreePartyCertificates(String tenderCode,
			String tenderAgencyCode, String tradingPlaceCode)
			throws ServiceException
	{
		logger.info(LogUtils.format("获取三方证书", tenderCode, tenderAgencyCode,
				tradingPlaceCode));
		StringBuilder key = new StringBuilder();
		key.append("$_THREE_PARTY_CERTIFICATES_$").append(tenderCode)
				.append("#").append(tenderAgencyCode).append("#")
				.append(tradingPlaceCode);

		// 获取session中的三方证书信息
		JSONObject cers = SessionUtils.getAttribute(key.toString());
		if (null != cers)
		{
			return cers;
		}
		// 获取AccessToken
		String accessToken = ApacheShiroUtils.getCurrentUser().getColumn(
				"ACCESS_TOKEN");
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("TENDER_CODE", tenderCode);
		params.setColumn("TENDER_AGENCY_CODE", tenderAgencyCode);
		params.setColumn("TRADING_PLACE_CODE", tradingPlaceCode);
		OAuthResourceResponse response = RSCallUtils.doGet(SystemParamUtils
				.getProperty(SysParamKey.AS_GET_THREE_PARTY_CERT_URL_KEY, ""),
				accessToken, params);
		// 请求类型
		MediaType contentType = MediaType.valueOf(response.getContentType());
		// 如果响应成功
		if (200 == response.getResponseCode()
				&& (MediaType.APPLICATION_JSON_TYPE.isCompatible(contentType) || MediaType.TEXT_JSON_TYPE
						.isCompatible(contentType)))
		{
			// 服务器端返回的证书信息JSON字符串
			String json = response.getBody();
			logger.info(LogUtils.format("三方证书返回值", json));
			if (StringUtils.isEmpty(json))
			{
				logger.error(LogUtils
						.format("获取三方证书信息失败,响应为空!", json, response));
				throw new ServiceException("", "获取三方证书信息失败,响应为空!");
			}
			JSONObject obj = JSON.parseObject(json);
			if (!obj.getBooleanValue("success"))
			{
				throw new ServiceException(obj.getString("errorCode"),
						obj.getString("errorDesc"));
			}
			cers = obj.getJSONObject("result");
			SessionUtils.setAttribute(key.toString(), cers);
			return cers;
		}
		logger.error(LogUtils.format("获取三方证书信息失败!", contentType,
				response.getBody()));
		// 响应失败
		throw new ServiceException("", "获取三方证书信息失败!");
	}

	/**
	 * 获取三方解密状态<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            招标项目流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 解密状态
	 * @throws FacadeException
	 *             Facade异常
	 */
	// 定义路径
	@Path(value = "tpdstatus/{tpnid}", desc = "获取三方解密状态")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ResultVO<Object> getThreePartsDecryptStatus(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.info(LogUtils.format("获取三方解密状态", tenderProjectNodeID, data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("tpnid", tenderProjectNodeID);
		param.setColumn("flag", ConstantEOKB.THREE_PARTS_DECRYPT);
		// 获取流程节点
		Record<String, Object> tpfnData = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_TPFN_ID = #{tpnid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").get(param);
		ResultVO<Object> result = new ResultVO<Object>(true);
		if (!CollectionUtils.isEmpty(tpfnData))
		{
			result.setResult(JSON.parseArray(tpfnData.getString("V_JSON_OBJ")));
		}
		return result;
	}

	/**
	 * 获取解密临时信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private List<Record<String, Object>> getDecryptTempInfos()
			throws ServiceException
	{
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		// 查询
		return this.activeRecordDAO.statement().selectList(
				"ThreePartsDecrypt.getDecryptTempInfos", param);
	}

	/**
	 * 合并解密<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             Facade异常
	 */
	// 定义路径
	@Path(value = "dofinal", desc = "合并解密")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void decrypt(AeolusData data) throws FacadeException
	{
		logger.info(LogUtils.format("合并解密", data));
		String tpid = SessionUtils.getTPID();
		// 招标项目名称
		String tpName = SessionUtils.getTenderProjectInfo().getString(
				"V_TENDER_PROJECT_NAME");
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		// 获取所有需要解密的列表
		List<Record<String, Object>> tempInfos = getDecryptTempInfos();
		String funName = "disposeDecryptProgress";
		// 消息flag 0开始,200结束,1正常进行,-1错误,-99被占用
		Record<String, Object> msg = new RecordImpl<String, Object>();

		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPDecryptLock(tpid);
		// 尝试获取锁，如果已经被其他线程占用
		if (!lock.tryLock())
		{
			logger.error(LogUtils
					.format("已经存在解密线程,用户重复刷新!", tpName, tpid, data));
			msg.clear();
			msg.setColumn("FLAG", -99);
			msg.setColumn("MSG", "当前招标项目正在被解密中,请勿重复刷新!");
			writeParseResponse(data, funName, msg);
			// 获取锁,这一步其实就是为了让线程挂起
			lock.lock();
		}
		// ------------------------------------------------
		try
		{
			msg.setColumn("FLAG", 0);
			writeParseResponse(data, funName, msg);
			JSONObject jobj = null;
			String ciphertext = null;
			String plaintext = null;
			File efb = null;
			// 临时目录
			File tempDir = null;
			// 目标目录
			File targetDir = null;
			int count = tempInfos.size();
			int index = 0;
			// ZIPX路径
			String zipxPath = null;
			// 解压记录
			Record<String, Object> unpackRecord = new RecordImpl<String, Object>();
			// 迭代获取信息解密
			for (Record<String, Object> tempInfo : tempInfos)
			{
				logger.info(LogUtils.format("解密文件", tempInfo));
				try
				{
					jobj = JSONObject.parseObject(tempInfo
							.getString("V_CIPHERTEXT_JSON"));
					ciphertext = jobj.getString("SOZONE-BID-FIELD");
					// 调用软证解密
					plaintext = decrypt(ciphertext);
					efb = new File(tempInfo.getString("V_EFB_FILE_PATH"));
					tempDir = new File(tempInfo.getString("V_TEMP_DIR_PATH"));
					targetDir = new File(
							tempInfo.getString("V_TARGET_DIR_PATH"));

					// 三层解密这边比较特殊
					// 第一步只解密
					zipxPath = DecryptBidder.decryptAndParseTempFile(efb,
							plaintext, targetDir, tempDir);
					// 保存解压记录
					unpackRecord.clear();
					// ID
					unpackRecord.setColumn("ID", Random.generateUUID());
					unpackRecord.setColumn("V_TPID", tpid);
					unpackRecord.setColumn("V_TENDER_PROJECT_NAME", tpName);
					unpackRecord.setColumn("V_TBID",
							tempInfo.getString("V_TBID"));
					unpackRecord.setColumn("V_FTID",
							tempInfo.getString("V_FTID"));
					unpackRecord.setColumn("V_BIDDER_NAME",
							tempInfo.getString("V_BIDDER_NAME"));
					unpackRecord.setColumn("V_ORG_CODE",
							tempInfo.getString("V_ORG_CODE"));
					unpackRecord.setColumn("V_UNIFY_CODE",
							tempInfo.getString("V_UNIFY_CODE"));
					unpackRecord.setColumn("V_ZIPX_FILE_PATH", zipxPath);
					unpackRecord.setColumn("V_TARGET_DIR_PATH",
							tempInfo.getString("V_TARGET_DIR_PATH"));
					unpackRecord.setColumn("N_STATUS", 0);
					unpackRecord.setColumn("N_CREATE_TIME",
							System.currentTimeMillis());
					// 保存解压日志
					save(unpackRecord, TableName.UNPACK_RECORD);
					// DecryptBidder.decryptAndUnpackBidderDocument(efb,
					// plaintext,
					// targetDir, tempDir, tempInfo.getString("V_FTID"),
					// tempInfo.getString("V_BEM_CODE"));
					// 解析投标文件
					getBidderDocumentParseService().parseBidderDocument(tpid,
							tempInfo.getString("V_UNIFY_CODE"),
							tempInfo.getString("V_ORG_CODE"));
					// 修改解密状态
					modifyDecryptStatus(tempInfo.getString("ID"));
				}
				catch (Exception e)
				{
					MsgUtils.send("开标投标解密：失败，招标项目[" + tpName + "]中投标人["
							+ tempInfo.getString("V_BIDDER_NAME") + "]，请处理!");
					msg.clear();
					msg.setColumn("FLAG", -1);
					msg.setColumn("MSG",
							"解密[" + tempInfo.getString("V_EFB_FILE_PATH")
									+ "]文件失败!");
					writeParseResponse(data, funName, msg);
					logger.error(LogUtils.format("解析投标文件发生异常!", tempInfo), e);
				}
				index++;
				// 输出进度情况
				DecimalFormat df = new DecimalFormat("0.00");
				String num = df.format((float) index / count);
				msg.clear();
				msg.setColumn("FLAG", 1);
				msg.setColumn("TEXT", index + "/" + count);
				msg.setColumn("VALUE", num);
				writeParseResponse(data, funName, msg);
			}
			msg.clear();
			msg.setColumn("FLAG", 200);
			writeParseResponse(data, funName, msg);
			if (count > 0)
			{
				// 启动后台解压任务
				UnpackServiceUtils.startUnpackTask(tpid, tpName);
			}
		}
		finally
		{
			// 如果已经没有线程等待获取锁
			if (!lock.hasQueuedThreads())
			{
				// 干掉锁
				PROJECT_DECRYPT_LOCK.remove(tpid);
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	/**
	 * 保存记录<br/>
	 * <p>
	 * </p>
	 * 
	 * @param record
	 * @param tableName
	 */
	private void save(Record<String, Object> record, String tableName)
	{
		StatefulDAO statefulDAO = null;
		try
		{
			statefulDAO = new StatefulDAOImpl();
			statefulDAO.auto().table(tableName).save(record);
			// 提交事务
			statefulDAO.commit();
		}
		catch (Exception e)
		{
			// 回滚事务
			if (null != statefulDAO)
			{
				statefulDAO.rollback();
			}
		}
		finally
		{
			// 关闭事务
			if (null != statefulDAO)
			{
				statefulDAO.close();
			}
		}
	}

	/**
	 * 修改解密状态<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tempID
	 */
	private void modifyDecryptStatus(String tempID)
	{
		StatefulDAO statefulDAO = null;
		try
		{
			statefulDAO = new StatefulDAOImpl();
			Record<String, Object> temp = new RecordImpl<String, Object>();
			// 设置ID
			temp.setColumn("ID", tempID);
			// 设置时间
			temp.setColumn("N_STATUS", 1);
			statefulDAO.auto().table(TableName.DECRYPT_TEMP).modify(temp);
			// 提交事务
			statefulDAO.commit();
		}
		catch (Exception e)
		{
			// 回滚事务
			if (null != statefulDAO)
			{
				statefulDAO.rollback();
			}
		}
		finally
		{
			// 关闭事务
			if (null != statefulDAO)
			{
				statefulDAO.close();
			}
		}
	}

	/**
	 * 解密<br/>
	 * <p>
	 * </p>
	 * 
	 * @param ciphertext
	 * @return
	 * @throws ServiceException
	 */
	private String decrypt(String ciphertext) throws ServiceException
	{
		logger.info(LogUtils.format("解密", ciphertext));
		// 获取AccessToken
		String accessToken = ApacheShiroUtils.getCurrentUser().getColumn(
				"ACCESS_TOKEN");
		Map<String, String> params = new HashMap<String, String>();
		params.put("CIPHERTEXT", ciphertext);
		OAuthResourceResponse response = RSCallUtils.doPost(SystemParamUtils
				.getProperty(SysParamKey.AS_VALIDATE_FIELD_CERT_URL_KEY, ""),
				accessToken, ContentType.APPLICATION_FORM_URLENCODED, params);
		// 请求类型
		MediaType contentType = MediaType.valueOf(response.getContentType());
		// 如果响应成功
		if (200 == response.getResponseCode()
				&& (MediaType.APPLICATION_JSON_TYPE.isCompatible(contentType) || MediaType.TEXT_JSON_TYPE
						.isCompatible(contentType)))
		{
			// 服务器端返回的证书信息JSON字符串
			String json = response.getBody();
			logger.info(LogUtils.format("解密结果", json));
			if (StringUtils.isEmpty(json))
			{
				logger.error(LogUtils.format("解密失败,响应为空!", json, response));
				throw new ServiceException("", "解密失败,响应为空!");
			}
			JSONObject obj = JSON.parseObject(json);
			return obj.getString("PLAINTEXT");
		}
		logger.error(LogUtils.format("解密失败!", contentType, response.getBody()));
		// 响应失败
		throw new ServiceException("", "解密失败!");
	}

	/**
	 * 三方解密<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            招标项目流程节点ID
	 * @param type
	 *            类型,0:招标人/招标代理,1:交易中心,2:公证,3公证或者交易中心
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             Facade异常
	 */
	// 定义路径
	@Path(value = "dothdecrypt/{tpnid}/{type}", desc = "三方解密")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void doThreePartsDecrypt(
			@PathParam("tpnid") String tenderProjectNodeID,
			@PathParam("type") int type, AeolusData data)
			throws FacadeException
	{
		logger.info(LogUtils.format("三方解密", tenderProjectNodeID, type, data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		// 解密情况
		JSONObject json = null;
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("tpnid", tenderProjectNodeID);
		param.setColumn("flag", ConstantEOKB.THREE_PARTS_DECRYPT);
		// 获取流程节点
		Record<String, Object> tpfnData = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_TPFN_ID = #{tpnid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").get(param);
		JSONArray array = null;
		// 获取数据
		Record<String, Object> inputParam = data.getRecord();
		// 如果数据为空
		if (CollectionUtils.isEmpty(tpfnData))
		{
			json = new JSONObject();
			json.put("DECRYPTION_TIME", System.currentTimeMillis());
			json.put("DECIPHER", type);
			json.putAll(inputParam);
			array = new JSONArray();
			array.add(json);
			tpfnData = new RecordImpl<String, Object>();
			tpfnData.setColumn("ID", Random.generateUUID());
			tpfnData.setColumn("V_BUS_FLAG_TYPE",
					ConstantEOKB.THREE_PARTS_DECRYPT);
			tpfnData.setColumn("V_TPID", tpid);
			tpfnData.setColumn("V_TPFN_ID", tenderProjectNodeID);
			tpfnData.setColumn("V_JSON_OBJ", JSON.toJSONString(array));
			tpfnData.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(tpfnData);
			sleep(tpid);
			return;
		}

		// 如果数据不为空
		array = JSON.parseArray(tpfnData.getString("V_JSON_OBJ"));
		for (int i = 0; i < array.size(); i++)
		{
			json = array.getJSONObject(i);
			// 如果已经解密过了
			if (json.getInteger("DECIPHER") == type)
			{
				return;
			}
		}
		json = new JSONObject();
		json.put("DECRYPTION_TIME", System.currentTimeMillis());
		json.put("DECIPHER", type);
		json.putAll(inputParam);
		array.add(json);
		Record<String, Object> rd = new RecordImpl<String, Object>();
		rd.setColumn("ID", tpfnData.getString("ID"));
		rd.setColumn("V_JSON_OBJ", JSON.toJSONString(array));
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.modify(rd);
		sleep(tpid);
	}

	private void sleep(String tpid) throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		// 获取未解析的数量
		int count = this.activeRecordDAO.auto().table(TableName.DECRYPT_TEMP)
				.setCondition("AND", "V_TPID = #{tpid}").count(param);
		if (count > 0)
		{
			try
			{
				Thread.sleep(count * 10);
			}
			catch (InterruptedException e)
			{
				logger.info(LogUtils.format("睡眠发生异常!"), e);
			}
		}
	}

}
