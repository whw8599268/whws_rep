/**
 * 包名：com.sozone.eokb.bus.decrypt.action
 * 文件名：ThreePartsDecryptAction.java<br/>
 * 创建时间：2018-5-30 上午11:16:19<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt.action;

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
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.auth2.client.utils.RSCallUtils;
import com.sozone.auth2.client.utils.RSCallUtils.ContentType;
import com.sozone.eokb.bus.decrypt.BidderDocumentDecryptor;
import com.sozone.eokb.bus.decrypt.DecryptStatus;
import com.sozone.eokb.bus.decrypt.service.BidderDocumentService;
import com.sozone.eokb.bus.decrypt.service.impl.BidderDocumentServiceFactory;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.MsgUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 三层解密服务类<br/>
 * <p>
 * 三层解密服务类<br/>
 * </p>
 * Time：2018-5-30 上午11:16:19<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "3dv3", desc = "三层解密服务类")
// 登录即可访问
@Permission(Level.Authenticated)
public class ThreePartsDecryptAction
{

	/**
	 * 项目解密锁
	 */
	private static final Map<String, ReentrantLock> PROJECT_DECRYPT_LOCK = new ConcurrentHashMap<String, ReentrantLock>();

	/**
	 * 证书序列号
	 */
	private static final String SOZONE_FIELD_CER_SERIAL = "989EFE";

	/**
	 * 证书Base64
	 */
	private static final String SOZONE_FIELD_CER_DATA = "MIICvDCCAiWgAwIBAgIEAJie-jANBgkqhkiG9w0BAQUFADB7MQswCQYDVQQ"
			+ "GEwJDTjESMBAGA1UECBMJR3Vhbmdkb25nMREwDwYDVQQHEwhTaG"
			+ "VuemhlbjEnMCUGA1UEChMeU2hlblpoZW4gQ2VydGlmaWNhdGUgQXV0aG9yaXR5MQ0wCwYDVQQLEwRzemNhMQ0wCwYDVQQDEwRTWkN"
			+ "BMB4XDTE2MTAyNzA2NTkzM1oXDTE5MTAyNzA2NTkzM1owgckxLTArBgNVBAMMJOemj-W7uummluS8l-S_oeaBr-enkeaKgOaciemZ"
			+ "kOWFrOWPuDETMBEGA1UECwwKMzEwNzgzOTYtMDFOMEwGA1UECgxF56aP5bu66aaW5LyX5L-h5oGv56eR5oqA5pyJ6ZmQ5YWs5Y-47"
			+ "7yI5oub5oqV5qCH6aKG5Z-f5LiT55So6K-B5Lmm77yJMRIwEAYDVQQHDAnnpo_lt57luIIxEjAQBgNVBAgMCeemj-W7uuecgTELMA"
			+ "kGA1UEBhMCQ04wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAJkS29WOXSAD6HP-KuJK8GRnt7AmhfEBfrZBtr8WKBz_goVa-Mo"
			+ "M1H9lkmQNzQ60Chd7SlcroAnJkC88SSAyMZuQbI5ex62_gNlDViBH14AlEznCjkCVwrqbyIFg_VEyNJLrulQM2Zip4C60oNCPQ0pB"
			+ "wdq9dkjflX7Lb0zMC597AgMBAAEwDQYJKoZIhvcNAQEFBQADgYEApA4TR1fPp83WKqSO3VSF7OR4sUHVYNMTEwqFfXVirWIGMTicG"
			+ "-o6UvIPAzLwDLaRL0ccxuFhdkyeE9mMQTLe2wdrUWNmGd35hziJcULiyrSH1BLO6M0yg1m0ITG67igPAa73zriZ8jAsZMv5tE-5Do9"
			+ "naTbYY7FFdPeMkCBOObU";
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(ThreePartsDecryptAction.class);

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

	// ------------------------------------------------------------------------------------------
	/**
	 * <p>
	 * </p>
	 * 
	 * @return BidderDocumentService
	 * @throws ServiceException
	 *             服务异常
	 */
	protected BidderDocumentService getBidderDocumentService()
			throws ServiceException
	{
		// 根据招标文件中的评标办法编码获取具体的解析类
		String bemCode = SessionUtils.getTenderProjectTypeCode();
		if (StringUtils.isEmpty(bemCode))
		{
			throw new ServiceException("", "无法获取评标办法编码");
		}
		String tpType = SessionUtils.getTenderProjectType();
		// 服务
		return BidderDocumentServiceFactory.getServiceInstance(bemCode, tpType);
	}

	// ---------------------------------------------------------------------------

	/**
	 * 
	 * 获取解密情况<br/>
	 * <p>
	 * 获取解密情况
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 投标人获取解密情况
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "", desc = "获取投标人获取解密情况")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getBidderDecryptInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取投标人获取解密情况", data));
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
		return this.activeRecordDAO.statement().selectPage(
				"DecryptV3.getBidderDecrypts", data.getPageRequest(), params);
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
	@Path(value = "status/{tpnid}", desc = "获取三方解密状态")
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
		param.setColumn("flag", ConstantEOKB.THREE_PARTS_DECRYPT_V3);
		// 获取流程节点
		Record<String, Object> tpfnData = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_TPFN_ID = #{tpnid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").get(param);
		ResultVO<Object> result = new ResultVO<Object>(true);
		if (!CollectionUtils.isEmpty(tpfnData))
		{
			result.setResult(tpfnData.getJSONArray("V_JSON_OBJ"));
		}
		return result;
	}

	/**
	 * 获取需要解析的投标文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @return 投递文件列表
	 * @throws ServiceException
	 *             服务异常
	 */
	private List<Record<String, Object>> getDeliveryDocuments()
			throws ServiceException
	{
		String tpid = SessionUtils.getTPID();
		List<Record<String, Object>> result = new ArrayList<Record<String, Object>>();
		// 获取投标人投递信息
		List<Record<String, Object>> docs = getBidderDocumentService()
				.getDeliveryDocuments(tpid, null, null);
		if (CollectionUtils.isEmpty(docs))
		{
			throw new ServiceException("DECRYPT-0001", "找不到需要解析的投标文件信息!");
		}
		File tempDir = null;
		File targetDir = null;
		String orgCode = null;
		for (Record<String, Object> doc : docs)
		{
			orgCode = doc.getString("V_ORG_CODE");
			String bidSectionCode = doc.getString("V_BID_SECTION_CODE");
			tempDir = new File(
					SystemParamUtils
							.getString(SysParamKey.EBIDKB_DECRYPTFILE_TEMP_PATH_URL)
							+ bidSectionCode + "/" + orgCode);
			targetDir = new File(
					SystemParamUtils
							.getString(SysParamKey.EBIDKB_DECRYPTFILE_PATH_URL)
							+ bidSectionCode + "/" + orgCode);
			doc.setColumn(
					"V_STBX_FILE_PATH",
					new File(doc.getString("V_FOLDERPATHALL"), doc
							.getString("V_FILEADDR")));
			// 临时目录
			doc.setColumn("V_TEMP_DIR_PATH", tempDir);
			// 投标文件（解密后）D:\fileEbid-fileTb_decrypt\标段包编号\当前投标人组织机构代码号
			doc.setColumn("V_TARGET_DIR_PATH", targetDir);
			result.add(doc);
		}
		return result;
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
		// 当前操作人
		String userID = ApacheShiroUtils.getCurrentUserID();
		String tpid = SessionUtils.getTPID();
		String funName = "disposeParseProgress";
		// 消息flag 0开始,200结束,1正常进行,-1错误,500内部错误,-99被占用
		Record<String, Object> msg = new RecordImpl<String, Object>();
		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPDecryptLock(tpid);
		// 尝试获取锁，如果已经被其他线程占用
		if (!lock.tryLock())
		{
			logger.error(LogUtils.format("已经存在解密线程,用户重复刷新!", tpName, userID,
					tpid, data));
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
			// 招标项目ID
			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("tpid", tpid);
			// 还要解析多少个
			int count = this.activeRecordDAO.statement().getOne(
					"3DecryptV3.getNotParseDeliveryCount", param);
			logger.info(LogUtils.format("要解析" + count + "个投标文件!"));
			msg.clear();
			msg.setColumn("FLAG", 0);
			//
			if (count > 0)
			{
				// 获取需要解密的临时信息
				List<Record<String, Object>> docs = null;
				// 一共要解析的投标文件数量
				int total = 0;
				try
				{
					// 获取需要解密的临时信息
					docs = getDeliveryDocuments();
					// 一共要解析的投标文件数量
					total = this.activeRecordDAO.auto()
							.table(TableName.EKB_T_TBIMPORTBIDDING)
							.setCondition("AND", "V_TPID = #{tpid}")
							.count(param);
				}
				catch (ServiceException e)
				{
					MsgUtils.send("开标解析投标文件描述JSON信息：失败，招标项目[" + tpName
							+ "]，请处理!");
					msg.clear();
					msg.setColumn("FLAG", 500);
					msg.setColumn("MSG", e.getErrorDesc());
					writeParseResponse(data, funName, msg);
					logger.error(LogUtils.format("投标文件描述JSON信息发生异常!"), e);
					throw e;
				}
				// 发送开始
				writeParseResponse(data, funName, msg);
				// 源文件
				File source = null;
				// 临时目录
				File tempDir = null;
				// 目标目录
				File targetDir = null;
				// 临时
				Record<String, Object> temp = null;
				String tbid = null;
				// 已经解析的个数
				int index = total - count;
				for (Record<String, Object> doc : docs)
				{
					// 直接用投递表的主键作为该表主键
					tbid = doc.getString("ID");
					doc.setColumn("V_TBID", tbid);
					source = doc.getColumn("V_STBX_FILE_PATH");
					doc.setColumn("V_STBX_FILE_PATH", FilenameUtils
							.separatorsToUnix(source.getAbsolutePath()));
					tempDir = doc.getColumn("V_TEMP_DIR_PATH");
					doc.setColumn("V_TEMP_DIR_PATH", FilenameUtils
							.separatorsToUnix(tempDir.getAbsolutePath()));
					targetDir = doc.getColumn("V_TARGET_DIR_PATH");
					doc.setColumn("V_TARGET_DIR_PATH", FilenameUtils
							.separatorsToUnix(targetDir.getAbsolutePath()));
					doc.setColumn("ID", tbid);
					doc.setColumn("V_FTID", doc.getString("V_FTID"));
					doc.setColumn("N_CREATE_TIME", System.currentTimeMillis());
					doc.setColumn("V_CREATE_USER", userID);
					doc.setColumn("V_TPID", tpid);
					// --------------------------获取投标文件描述JSON--------------------------------------------
					try
					{
						temp = BidderDocumentDecryptor
								.getBidderDocumentJsonInfo(source);
						// 解密算法
						int algorithmType = temp.getInteger("ALGORITHM_TYPE");
						// 信息
						JSONObject jobj = temp.getJSONObject("TB_FILE_JSON");
						// 投标文件类型
						int useCase = jobj.getIntValue("USE_CASE");
						doc.setColumn("N_USE_CASE", useCase);
						doc.setColumn("N_ALGORITHM_TYPE", algorithmType);
						doc.setColumn("V_FILE_INFO_JSON", jobj);
						doc.setColumn("N_STATUS",
								DecryptStatus.ParseJsonInfoSuccess.getStatus());
					}
					catch (Exception e)
					{
						// 设置失败状态
						doc.setColumn("N_STATUS",
								DecryptStatus.ParseJsonInfoFail.getStatus());
						MsgUtils.send("开标解析投标文件描述JSON信息：失败，招标项目[" + tpName
								+ "]中投标人[" + doc.getString("V_BIDDER_NAME")
								+ "]，请处理!");
						msg.clear();
						msg.setColumn("FLAG", -1);
						msg.setColumn("MSG", "解析[" + source.getAbsolutePath()
								+ "]文件描述JSON信息失败!");
						writeParseResponse(data, funName, msg);
						logger.error(LogUtils.format("投标文件描述JSON信息发生异常!", doc,
								source), e);
					}
					finally
					{
						saveDecryptTempInfo(doc);
					}
					index++;
					// 输出进度情况
					DecimalFormat df = new DecimalFormat("0.00");
					String num = df.format((float) index / total);
					msg.clear();
					msg.setColumn("FLAG", 1);
					msg.setColumn("TEXT", index + "/" + total);
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
	 * 保存解密临时信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param temp
	 *            保存解密临时信息
	 */
	private void saveDecryptTempInfo(Record<String, Object> temp)
	{
		StatefulDAO statefulDAO = null;
		try
		{
			statefulDAO = new StatefulDAOImpl();
			statefulDAO.auto().table(TableName.DECRYPT_TEMP_DATA)
					.remove(temp.getString("ID"));
			statefulDAO.auto().table(TableName.DECRYPT_TEMP_DATA).save(temp);
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
	 * 写出解析响应<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param funName
	 *            JS函数名
	 * @param msg
	 *            数据
	 */
	protected void writeParseResponse(AeolusData data, String funName,
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
	 * 校验证书的有效性<br/>
	 * <p>
	 * </p>
	 * 
	 * @param type
	 *            类型,0:招标人/招标代理,1:交易中心,2:公证,3公证或者交易中心
	 * @param data
	 *            AeolusData
	 * @return 返回具体的证书使用者,即0:招标人/招标代理,1:交易中心,2:公证
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
		certData = StringUtils.replace(certData, "\r", "");
		certData = StringUtils.replace(certData, "\n", "");
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
		type = verifyCert(type, cers, certData, certSerial);
		result.setResult(type);
		return result;
	}

	/**
	 * 验证证书有效性<br/>
	 * <p>
	 * </p>
	 * 
	 * @param type
	 * @param cers
	 * @param certData
	 * @param certSerial
	 * @return 返回具体的证书使用者,即0:招标人/招标代理,1:交易中心,2:公证
	 * @throws ServiceException
	 */
	private int verifyCert(int type, JSONObject cers, String certData,
			String certSerial) throws ServiceException
	{

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
			return 0;
		}
		// 交易中心
		if (1 == type)
		{
			JSONArray jyzxCers = cers.getJSONArray("TRADING_CENTER_CERS");
			if (!verify(certData, certSerial, jyzxCers))
			{
				logger.error(LogUtils.format("无效的交易中心证书!"));
				throw new ServiceException("", "无效的交易中心证书!");
			}
			return 1;
		}
		// 公证
		if (2 == type)
		{
			JSONArray gzCers = cers.getJSONArray("NOTARIZATION_CERS");
			if (!verify(certData, certSerial, gzCers))
			{
				logger.error(LogUtils.format("无效的公证证书!"));
				throw new ServiceException("", "无效的公证证书!");
			}
			return 2;
		}
		if (3 == type)
		{
			// 交易中心的证书
			JSONArray jyzxCers = cers.getJSONArray("TRADING_CENTER_CERS");
			// 公证的证书
			JSONArray gzCers = cers.getJSONArray("NOTARIZATION_CERS");
			// 如果是交易中心
			if (verify(certData, certSerial, jyzxCers))
			{
				return 1;
			}
			// 如果是公证
			if (verify(certData, certSerial, gzCers))
			{
				return 2;
			}
			logger.error(LogUtils.format("无效的交易中心或公证证书!"));
			throw new ServiceException("", "无效的交易中心或公证证书!");
		}
		logger.error(LogUtils.format("无效的验证类型!"));
		throw new ServiceException("", "无效的验证类型!");
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
		if (CollectionUtils.isEmpty(cers))
		{
			logger.error(LogUtils.format("证书列表为空!"));
			return false;
		}
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
		logger.info(LogUtils.format("调用开放式授权接口获取三方证书", SystemParamUtils
				.getProperty(SysParamKey.AS_GET_THREE_PARTY_CERT_URL_KEY, ""),
				accessToken, params));
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
		// -----------------------------------------------------
		try
		{
			Record<String, Object> params = new RecordImpl<String, Object>();
			params.setColumn("tpid", tpid);
			// 查询要解密的数量
			Record<String, Object> rcts = this.activeRecordDAO.statement()
					.selectOne("3DecryptV3.getBeToDecryptCount", params);
			// 需要解密的数量
			int count = rcts.getInteger("N_BE_COUNT");
			// 所有要解密的数量
			int total = rcts.getInteger("N_TOTAL");
			int index = total - count;
			msg.clear();
			msg.setColumn("FLAG", 0);
			writeParseResponse(data, funName, msg);
			//
			int tc = 0;
			if (count > 0)
			{

				// 获取要解密的投标人
				List<Record<String, Object>> bidders = this.activeRecordDAO
						.statement().selectList(
								"3DecryptV3.getBeToDecryptBidders", tpid);
				for (Record<String, Object> bidder : bidders)
				{
					logger.info(LogUtils.format("开始解密投标信息摘要!", bidder));
					try
					{
						tc = doDecrypt(tpid, bidder.getString("V_ORG_CODE"));
					}
					catch (ServiceException e)
					{
						MsgUtils.send("开标解密摘要信息：失败，招标项目[" + tpName + "]中投标人["
								+ bidder.getString("V_BIDDER_NAME") + "]，请处理!");
						msg.clear();
						msg.setColumn("FLAG", -1);
						msg.setColumn("MSG",
								"解密[" + bidder.getString("V_BIDDER_NAME")
										+ "]投标信息摘要失败!");
						writeParseResponse(data, funName, msg);
						logger.error(LogUtils.format("解密投标摘要信息发生异常!", bidder),
								e);
					}
					catch (Exception e)
					{
						MsgUtils.send("开标解密摘要信息：失败，招标项目[" + tpName + "]中投标人["
								+ bidder.getString("V_BIDDER_NAME") + "]，请处理!");
						msg.clear();
						msg.setColumn("FLAG", -1);
						msg.setColumn("MSG",
								"解密[" + bidder.getString("V_BIDDER_NAME")
										+ "]投标信息摘要失败!");
						writeParseResponse(data, funName, msg);
						logger.error(LogUtils.format("解密投标摘要信息发生异常!", bidder),
								e);
					}
					index += tc;
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
	 * 执行解密<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 * @param orgCode
	 * @return 解密的文件个数
	 * @throws ServiceException
	 */
	private int doDecrypt(String tpid, String orgCode) throws ServiceException
	{
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", tpid).setColumn("org_code", orgCode);
		List<Record<String, Object>> temps = this.activeRecordDAO.auto()
				.table(TableName.DECRYPT_TEMP_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_ORG_CODE = #{org_code}").list(params);
		// 解密的文件个数
		int count = 0;
		// 文件
		int status = 0;
		// 源文件
		File source = null;
		Record<String, Object> jifo = null;
		for (Record<String, Object> temp : temps)
		{
			// 获取当前记录状态
			status = temp.getInteger("N_STATUS");
			// 如果摘要已经解密
			if (DecryptStatus.DecryptSummarySuccess.getStatus() <= status)
			{
				continue;
			}
			// 如果解析JSON失败状态,再解析一次
			if (DecryptStatus.ParseJsonInfoFail.getStatus() == status)
			{
				source = new File(temp.getString("V_STBX_FILE_PATH"));
				jifo = BidderDocumentDecryptor
						.getBidderDocumentJsonInfo(source);
				// 解密算法
				int algorithmType = jifo.getInteger("ALGORITHM_TYPE");
				// 信息
				JSONObject jobj = jifo.getJSONObject("TB_FILE_JSON");
				// 投标文件类型
				int useCase = jobj.getIntValue("USE_CASE");
				temp.setColumn("N_USE_CASE", useCase);
				temp.setColumn("N_ALGORITHM_TYPE", algorithmType);
				temp.setColumn("V_FILE_INFO_JSON", jobj);
				temp.setColumn("N_STATUS",
						DecryptStatus.ParseJsonInfoSuccess.getStatus());
			}
			try
			{
				// 如果还没有解密过密码
				if ((DecryptStatus.DecryptPwdSuccess.getStatus() > status && DecryptStatus.ParseJsonInfoSuccess
						.getStatus() <= status)
						|| DecryptStatus.DecryptPwdFail.getStatus() == status)
				{
					// 先解密加密密码
					temp = decryptPwd(temp);
				}
				// 再解摘要
				decryptBidderElementInfo(temp);
			}
			finally
			{
				modifyDecryptTempInfo(temp);
				// TODO 纯粹为了对付隔离级别
				this.activeRecordDAO.auto().table(TableName.DECRYPT_TEMP_DATA)
						.modify(temp);
			}
			count++;
		}
		// 只有全部解密正确了才能发起解析
		try
		{
			// 解析投标文件
			getBidderDocumentService()
					.parseDecryptTempData(tpid, null, orgCode);
		}
		catch (ServiceException e)
		{
			logger.error(LogUtils.format("解析投标文件解密数据失败!", tpid, null, orgCode),
					e);
			throw e;
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("解析投标文件解密数据失败!", tpid, null, orgCode),
					e);
			throw new ServiceException("", "解析投标文件解密数据失败!", e);
		}
		return count;
	}

	/**
	 * 获取密文<br/>
	 * <p>
	 * </p>
	 * 
	 * @param jobj
	 * @param issuerType
	 * @param certSerial
	 * @param certData
	 * @return
	 * @throws ServiceException
	 */
	private String getCiphertext(JSONObject jobj, String issuerType,
			String certSerial, String certData) throws ServiceException
	{
		if (null == jobj || jobj.isEmpty())
		{
			throw new ServiceException("", "无效的投标文件描述信息!");
		}
		JSONArray pwds = jobj.getJSONArray("PWDS");
		if (null == pwds || pwds.isEmpty())
		{
			throw new ServiceException("", "无法获取到投标文件加密信息!");
		}
		JSONObject pwdInfo = null;
		String cdt = null;
		for (int i = 0; i < pwds.size(); i++)
		{
			pwdInfo = pwds.getJSONObject(i);
			// 干掉换行符
			cdt = pwdInfo.getString("CER_DATA");
			cdt = StringUtils.replace(cdt, "\r", "");
			cdt = StringUtils.replace(cdt, "\n", "");
			// 如果证书序列号与证书内容都一样
			if (StringUtils.equals(certSerial,
					pwdInfo.getString("CER_SERIAL_NUM"))
					&& StringUtils.equals(certData, cdt))
			{
				return pwdInfo.getString("PWD");
			}
		}
		throw new ServiceException("", "无法获取到证书对应的投标文件加密信息,请检查证书是否正确!");
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
		OAuthResourceResponse response = null;
		try
		{
			response = RSCallUtils.doPost(SystemParamUtils.getProperty(
					SysParamKey.AS_VALIDATE_FIELD_CERT_URL_KEY, ""),
					accessToken, ContentType.APPLICATION_FORM_URLENCODED,
					params);
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("请求云盾解密时发生异常!"), e);
			return SozoneCipher.j(ciphertext);
		}
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
	 * 解密加密密码<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tempDatas
	 * @return
	 * @throws ServiceException
	 */
	private Record<String, Object> decryptPwd(Record<String, Object> tempData)
			throws ServiceException
	{
		logger.info(LogUtils.format("解密加密密码", tempData));
		// 证书颁发机构
		String issuerType = "-1";
		// 获取文件描述JSON
		JSONObject obj = tempData.getJSONObject("V_FILE_INFO_JSON");
		try
		{
			// 获取密码
			String ciphertext = getCiphertext(obj, issuerType,
					SOZONE_FIELD_CER_SERIAL, SOZONE_FIELD_CER_DATA);
			// 调用开放式授权进行解密
			String plaintext = decrypt(ciphertext);
			logger.info(LogUtils.format("解密后的明文", tempData));
			// 设置密文
			tempData.setColumn("V_PWD", plaintext);
			// 设置状态
			tempData.setColumn("N_STATUS",
					DecryptStatus.DecryptPwdSuccess.getStatus());
			return tempData;
		}
		catch (ServiceException e)
		{
			// 设置状态
			tempData.setColumn("N_STATUS",
					DecryptStatus.DecryptPwdFail.getStatus());
			logger.error(LogUtils.format("解密加密密码发生异常", tempData), e);
			throw e;
		}
		catch (Exception e)
		{
			// 设置状态
			tempData.setColumn("N_STATUS",
					DecryptStatus.DecryptPwdFail.getStatus());
			logger.error(LogUtils.format("解密加密密码发生异常", tempData), e);
			throw new ServiceException("", "解密加密密码失败!");
		}
	}

	/**
	 * 解密投标信息摘要<br/>
	 * <p>
	 * </p>
	 * 
	 * @param temp
	 *            临时信息
	 * @return 解密信息
	 * @throws ServiceException
	 */
	private Record<String, Object> decryptBidderElementInfo(
			Record<String, Object> temp) throws ServiceException
	{
		JSONObject jobj = null;
		try
		{
			// 加密密码
			String plaintext = temp.getString("V_PWD");
			// ------------------------------------
			// 开始准备解密摘要
			long start = System.currentTimeMillis();
			jobj = temp.getJSONObject("V_FILE_INFO_JSON");
			if (null == jobj || jobj.isEmpty())
			{
				throw new ServiceException("", "无效的投标文件描述信息!");
			}
			logger.debug(LogUtils.format("解密的密码：", plaintext));
			String eleB64 = jobj.getString("ELEMENT");
			logger.debug(LogUtils.format("解密前的投标信息摘要：", eleB64));
			// 这里只解密投标信息摘要
			String eleJson = BidderDocumentDecryptor.decrypt(eleB64,
					temp.getInteger("N_ALGORITHM_TYPE"), plaintext);
			logger.debug(LogUtils.format("解密后的投标信息摘要：", eleJson));
			// 投标信息摘要
			JSONArray element = JSON.parseArray(eleJson);
			// 投标信息摘要解密完成
			temp.setColumn("N_STATUS",
					DecryptStatus.DecryptSummarySuccess.getStatus());
			// 设置投标信息摘要
			temp.setColumn("V_ELEMENT_JSON", element);
			long end = System.currentTimeMillis();
			// 设置消耗时间
			temp.setColumn("N_DECRYPT_SUMMARY_CONSUMING", end - start);
			return temp;
		}
		catch (ServiceException e)
		{
			// 设置状态
			temp.setColumn("N_STATUS",
					DecryptStatus.DecryptSummaryFail.getStatus());
			logger.error(LogUtils.format("解密投标信息摘要失败!", jobj, temp), e);
			throw e;
		}
		catch (Exception e)
		{
			// 设置状态
			temp.setColumn("N_STATUS",
					DecryptStatus.DecryptSummaryFail.getStatus());
			logger.error(LogUtils.format("解密投标信息摘要失败!", jobj, temp), e);
			throw new ServiceException("", "解密投标信息摘要失败!", e);
		}
	}

	/**
	 * 修改解密临时信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param temp
	 *            解密临时信息
	 */
	private void modifyDecryptTempInfo(Record<String, Object> temp)
	{
		StatefulDAO statefulDAO = null;
		try
		{
			statefulDAO = new StatefulDAOImpl();
			statefulDAO.auto().table(TableName.DECRYPT_TEMP_DATA).modify(temp);
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
	 * 标记三方解密的各自解密状态<br/>
	 * <p>
	 * dothdecrypt
	 * </p>
	 * 
	 * @param tpnid
	 *            招标项目流程节点ID
	 * @param type
	 *            类型,0:招标人/招标代理,1:交易中心,2:公证,3公证或者交易中心
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             Facade异常
	 */
	// 定义路径
	@Path(value = "doflag/{tpnid}/{type}", desc = "标记三方解密的各自解密状态")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void doFlagDecryptStatus(@PathParam("tpnid") String tpnid,
			@PathParam("type") int type, AeolusData data)
			throws FacadeException
	{
		logger.info(LogUtils.format("标记三方解密的各自解密状态", tpnid, type, data));
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		// 解密情况
		JSONObject json = null;
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("tpnid", tpnid);
		param.setColumn("flag", ConstantEOKB.THREE_PARTS_DECRYPT_V3);
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
					ConstantEOKB.THREE_PARTS_DECRYPT_V3);
			tpfnData.setColumn("V_TPID", tpid);
			tpfnData.setColumn("V_TPFN_ID", tpnid);
			tpfnData.setColumn("V_JSON_OBJ", array);
			tpfnData.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA).save(tpfnData);
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
		rd.setColumn("V_JSON_OBJ", array);
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.modify(rd);
	}

}
