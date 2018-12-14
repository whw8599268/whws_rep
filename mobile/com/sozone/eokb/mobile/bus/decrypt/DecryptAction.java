/**
 * 包名：com.sozone.eokb.mobile.bus.decrypt
 * 文件名：DecryptAction.java<br/>
 * 创建时间：2018-12-4 上午11:24:37<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.bus.decrypt;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FilenameUtils;
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
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.common.utils.CertificateUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.TimestampUtils;
import com.sozone.eokb.bus.decrypt.BidderDocumentDecryptor;
import com.sozone.eokb.bus.decrypt.DecryptStatus;
import com.sozone.eokb.bus.decrypt.action.SozoneCipher;
import com.sozone.eokb.bus.decrypt.service.BidderDocumentService;
import com.sozone.eokb.bus.decrypt.service.impl.BidderDocumentServiceFactory;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.flow.common.NodeStatus;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.MsgUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 手机解密服务接口<br/>
 * <p>
 * 手机解密只有自解密,不存在三层解密这一说<br/>
 * </p>
 * Time：2018-12-4 上午11:24:37<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/mobile/decrypt", desc = "手机解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class DecryptAction
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
	 * CN Key
	 */
	private static final String CN_KEY = "CN=";

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
	private static Logger logger = LoggerFactory.getLogger(DecryptAction.class);

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
	 * @return 互斥锁锁对象
	 * @throws ValidateException
	 *             校验异常
	 */
	private static synchronized ReentrantLock getTPDecryptLock()
			throws ValidateException
	{
		String tpid = SessionUtils.getTPID();
		String unitfy = SessionUtils.getSocialcreditNO();
		String orgCode = SessionUtils.getCompanyCode();
		StringBuilder sb = new StringBuilder();
		sb.append(tpid).append("|")
				.append(StringUtils.defaultIfEmpty(orgCode, "")).append("|")
				.append(StringUtils.defaultIfEmpty(unitfy, ""));
		String key = sb.toString();
		// 获取项目锁
		ReentrantLock lock = PROJECT_DECRYPT_LOCK.get(key);
		// 如果锁不存在
		if (null == lock)
		{
			// 构建互斥锁
			lock = new ReentrantLock();
			PROJECT_DECRYPT_LOCK.put(key, lock);
		}
		return lock;
	}

	/**
	 * 删除项目解密锁<br/>
	 * <p>
	 * </p>
	 * 
	 * @throws ValidateException
	 *             校验异常
	 */
	private static void removeTPDecryptLock() throws ValidateException
	{
		String tpid = SessionUtils.getTPID();
		String unitfy = SessionUtils.getSocialcreditNO();
		String orgCode = SessionUtils.getCompanyCode();
		StringBuilder sb = new StringBuilder();
		sb.append(tpid).append("|")
				.append(StringUtils.defaultIfEmpty(orgCode, "")).append("|")
				.append(StringUtils.defaultIfEmpty(unitfy, ""));
		String key = sb.toString();
		PROJECT_DECRYPT_LOCK.remove(key);
	}

	/**
	 * 
	 * 招标代理或投标人获取各标段开标设置信息<br/>
	 * <p>
	 * 招标代理或投标人获取各标段开标设置信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 列表信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/sections", desc = "招标代理或投标人获取各标段开标设置信息")
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getSections(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("招标代理或投标人获取各标段开标设置信息", data));
		String tpid = SessionUtils.getTPID();
		boolean group = SessionUtils.isSectionGroup();
		// 如果是标段组的情况
		if (group)
		{
			return this.activeRecordDAO.statement().selectList(
					"StartUp.getBidOpenList_Group", tpid);
		}
		// 非标段组情况下
		return this.activeRecordDAO.statement().selectList(
				"StartUp.getBidOpenList", tpid);
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
	 * @return 投标人获取解密情况
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "", desc = "获取投标人获取解密情况")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getBidderDecryptInfo(AeolusData data)
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
		return this.activeRecordDAO.statement().selectList(
				"DecryptV3.getBidderDecrypts", params);
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

		return "第一数字信封未开启，第二数字信封未开启";
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
	 * 解析投标文件<br/>
	 * <p>
	 * 获取证书要解密的密文列表
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 证书要解密的密文列表
	 * @throws FacadeException
	 *             Facade异常
	 */
	// 定义路径
	@Path(value = "parse", desc = "获取证书要解密的密文列表")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public List<Record<String, Object>> parseBidderDocument(AeolusData data)
			throws FacadeException
	{
		logger.info(LogUtils.format("获取招标项目中对应的介质证书要解密的所有非对称密钥列表", data));
		// 证书base64
		String certData = data.getParam("CERT_DATA");
		// 干掉换行符
		certData = StringUtils.replace(certData, "\r", "");
		certData = StringUtils.replace(certData, "\n", "");
		// 证书序列号
		String certSerial = data.getParam("CERT_SERIAL");
		// 证书颁发机构
		String issuerType = data.getParam("ISSUER_TYPE");
		//
		if (StringUtils.isEmpty(certSerial))
		{
			logger.error(LogUtils.format("证书序列号为空!", data));
			throw new ServiceException("", "无效的证书序列号");
		}
		if (StringUtils.isEmpty(issuerType))
		{
			logger.error(LogUtils.format("证书颁发机构为空!", data));
			throw new ServiceException("", "无效的证书颁发机构");
		}
		// 获取需要解密的临时信息
		List<Record<String, Object>> tempDatas = getBidderDocumentJsonDatas();
		logger.info(LogUtils.format("要解密的临时数据列表", tempDatas));
		// 密码
		List<Record<String, Object>> ciphertexts = new LinkedList<Record<String, Object>>();
		JSONObject obj = null;
		// 迭代列表
		for (Record<String, Object> td : tempDatas)
		{
			// 获取文件描述JSON
			obj = td.getJSONObject("V_FILE_INFO_JSON");
			// 校验证书是否有效
			verifyCert(obj, issuerType, certSerial, certData);
		}
		// Record<String, Object> ciphertext = null;
		// JSONObject obj = null;
		// // 迭代列表
		// for (Record<String, Object> td : tempDatas)
		// {
		// ciphertext = new RecordImpl<String, Object>();
		// ciphertext.setColumn("ID", td.getString("ID"));
		// // 获取文件描述JSON
		// obj = td.getJSONObject("V_FILE_INFO_JSON");
		// // 获取密码
		// ciphertext.setColumn("CIPHERTEXT",
		// getCiphertext(obj, issuerType, certSerial, certData));
		// ciphertexts.add(ciphertext);
		// }
		// logger.info(LogUtils.format("客户端需要解密的密码列表", ciphertexts));
		return ciphertexts;
	}

	/**
	 * 判断当前证书是否为有效的解密证书<br/>
	 * <p>
	 * </p>
	 * 
	 * @param jobj
	 * @param issuerType
	 * @param certSerial
	 * @param certData
	 * @throws ServiceException
	 */
	private void verifyCert(JSONObject jobj, String issuerType,
			String certSerial, String certData) throws ServiceException
	{
		logger.info(LogUtils.format("要解密的投标文件信息!", jobj));
		if (null == jobj || jobj.isEmpty())
		{
			logger.error(LogUtils.format("无效的投标文件描述信息!", certData, jobj));
			throw new ServiceException("", "无效的投标文件描述信息,解析投标文件时获取不到文件的描述信息!");
		}
		JSONArray pwds = jobj.getJSONArray("PWDS");
		if (null == pwds || pwds.isEmpty())
		{
			logger.error(LogUtils.format("无法获取到投标文件加密信息!", certData, jobj));
			throw new ServiceException("", "无法获取到投标文件加密信息,解析投标文件时获取不到文件的加密密码!");
		}
		JSONObject pwdInfo = null;
		String cdt = null;
		List<String> certNames = new LinkedList<String>();
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
				return;
			}
			// 是否为同一家的证书（专门处理延期的情况）
			if (isSameCompany(certData, cdt))
			{
				return;
			}
			// 介质证书
			if (0 == pwdInfo.getIntValue("CER_TYPE"))
			{
				certNames.add(getSubjectCNValue(cdt));
			}
		}
		logger.error(LogUtils.format("无法获取到证书对应的投标文件加密信息,请检查证书是否正确!", certData,
				jobj));
		String tpName = jobj.getString("TP_NAME");
		String sectionName = jobj.getString("SECTION_NAME");
		String envelope = jobj.getString("USE_CASE");
		StringBuilder sb = new StringBuilder();
		sb.append("招标项目名称：").append(tpName).append("<br>");
		sb.append("标段名称：").append(sectionName).append("<br>");
		sb.append("信封编号：").append(envelope).append("<br>");
		sb.append("加密的证书名称列表：").append(certNames.toString()).append("<br>");
		sb.append("当前证书名称：").append(getSubjectCNValue(certData));
		throw new ServiceException("DECRYPT-0001",
				"无法获取到证书对应的投标文件加密信息,请检查证书是否正确!<br>" + sb.toString());
	}

	/**
	 * 获取证书对应的密文<br/>
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
		logger.info(LogUtils.format("要解密的投标文件信息!", jobj));
		if (null == jobj || jobj.isEmpty())
		{
			logger.error(LogUtils.format("无效的投标文件描述信息!", certData, jobj));
			throw new ServiceException("", "无效的投标文件描述信息,解析投标文件时获取不到文件的描述信息!");
		}
		JSONArray pwds = jobj.getJSONArray("PWDS");
		if (null == pwds || pwds.isEmpty())
		{
			logger.error(LogUtils.format("无法获取到投标文件加密信息!", certData, jobj));
			throw new ServiceException("", "无法获取到投标文件加密信息,解析投标文件时获取不到文件的加密密码!");
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
		logger.error(LogUtils.format("无法获取到证书对应的投标文件加密信息,请检查证书是否正确!", certData,
				jobj));
		throw new ServiceException("DECRYPT-0001",
				"无法获取到证书对应的投标文件加密信息,请检查证书是否正确!");
	}

	/**
	 * 比较两个证书是否为同一家的证书<br/>
	 * <p>
	 * </p>
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	private static boolean isSameCompany(String c1, String c2)
	{
		try
		{
			X509Certificate cert1 = CertificateUtils.getX509Certificate(c1);
			X509Certificate cert2 = CertificateUtils.getX509Certificate(c2);
			String cn1 = getSubjectName(cert1);
			if (StringUtils.isEmpty(cn1))
			{
				return false;
			}
			String cn2 = getSubjectName(cert2);
			if (StringUtils.isEmpty(cn2))
			{
				return false;
			}
			return StringUtils.equals(cn1, cn2);
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("比对证书使用者名称时发生异常!", c1, c2), e);
			return false;
		}
	}

	/**
	 * 获取投标文件描述JSON信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @return 投标文件描述JSON信息列表
	 * @throws ServiceException
	 *             服务异常
	 */
	protected List<Record<String, Object>> getBidderDocumentJsonDatas()
			throws ServiceException
	{
		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPDecryptLock();
		// -----------------------------------------------------
		try
		{
			// 获取锁
			lock.lock();
			String userID = ApacheShiroUtils.getCurrentUserID();
			String tpid = SessionUtils.getTPID();
			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("tpid", tpid);
			param.setColumn("userID", userID);
			// 源投递文件
			List<Record<String, Object>> docs = getDeliveryDocuments();
			// 判断下是否已经解析过
			int count = this.activeRecordDAO
					.auto()
					.table(TableName.DECRYPT_TEMP_DATA)
					.setCondition("AND",
							"V_TPID = #{tpid} AND V_CREATE_USER = #{userID}")
					.count(param);
			// 如果没有初始化,或者数量不对
			if (count <= 0 || count != docs.size())
			{
				// 源文件
				File source = null;
				// 临时目录
				File tempDir = null;
				// 目标目录
				File targetDir = null;
				// 临时
				Record<String, Object> temp = null;
				String tbid = null;
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
					// --------------------------获取投标文件描述JSON--------------------------------------------
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
					doc.setColumn("ID", tbid);
					doc.setColumn("V_FTID", doc.getString("V_FTID"));
					doc.setColumn("N_CREATE_TIME", System.currentTimeMillis());
					doc.setColumn("V_CREATE_USER", userID);
					doc.setColumn("V_TPID", tpid);
					doc.setColumn("N_STATUS",
							DecryptStatus.ParseJsonInfoSuccess.getStatus());
					// 先删除再增加
					this.activeRecordDAO.auto()
							.table(TableName.DECRYPT_TEMP_DATA).remove(tbid);
					this.activeRecordDAO.auto()
							.table(TableName.DECRYPT_TEMP_DATA).save(doc);
				}
				return docs;
			}
			param.setColumn("status",
					DecryptStatus.ParseJsonInfoSuccess.getStatus());
			// 查询
			return this.activeRecordDAO
					.auto()
					.table(TableName.DECRYPT_TEMP_DATA)
					.setCondition("AND",
							"V_TPID = #{tpid} AND V_CREATE_USER = #{userID} AND N_STATUS <= #{status}")
					.list(param);
		}
		finally
		{
			// 如果已经没有线程等待获取锁
			if (!lock.hasQueuedThreads())
			{
				// 干掉锁
				removeTPDecryptLock();
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	/**
	 * 获取投标源文件方法<br/>
	 * <p>
	 * </p>
	 * 
	 * @return 投递文件列表
	 * @throws ServiceException
	 *             服务异常
	 */
	protected List<Record<String, Object>> getDeliveryDocuments()
			throws ServiceException
	{
		String tpid = SessionUtils.getTPID();
		String unitfy = SessionUtils.getSocialcreditNO();
		String orgCode = SessionUtils.getCompanyCode();
		// String companyName = SessionUtils.getCompanyName();
		List<Record<String, Object>> result = new ArrayList<Record<String, Object>>();
		// 获取投标人投递信息
		List<Record<String, Object>> docs = getBidderDocumentService()
				.getDeliveryDocuments(tpid, unitfy, orgCode);
		if (CollectionUtils.isEmpty(docs))
		{
			throw new ServiceException("DECRYPT-0001",
					"请检查当前登录的用户是否正确,系统找不到当前用户对应的投标文件!");
		}
		File tempDir = null;
		File targetDir = null;
		for (Record<String, Object> doc : docs)
		{
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

	private static String getSubjectCNValue(String cd)
	{
		try
		{
			String cn = getSubjectCN(CertificateUtils.getX509Certificate(cd));
			return StringUtils.substring(cn, CN_KEY.length());
		}
		catch (Exception e)
		{
			logger.debug(LogUtils.format("获取证书的使用者名称发生异常"), e);
			return null;
		}
	}

	private static String getSubjectCN(X509Certificate cer)
	{
		String dn = cer.getSubjectX500Principal().getName();
		String[] infos = StringUtils.split(dn, ",");
		if (null == infos || 0 == infos.length)
		{
			return null;
		}
		for (String info : infos)
		{
			if (StringUtils.startsWith(info, CN_KEY))
			{
				return info;
			}
		}
		return null;
	}

	private static String getSubjectName(X509Certificate cer)
	{
		String cn = getSubjectCN(cer);
		if (StringUtils.isNotEmpty(cn))
		{
			int end = StringUtils.indexOf(cn, '（');
			if (-1 == end)
			{
				return StringUtils.substring(cn, CN_KEY.length());
			}
			return StringUtils.substring(cn, CN_KEY.length(), end);
		}
		return null;
	}

	/**
	 * 使用软证进行解密<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             异常
	 */
	// 定义路径
	@Path(value = "excute", desc = "使用软证进行解密")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void decryptBidderDocument(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("使用软证进行解密", data));
		// 文件密码对
		JSONArray plaintexts = null;
		// 获取需要解密的临时信息
		List<Record<String, Object>> tempDatas = getBidderDocumentJsonDatas();
		try
		{
			// 使用领域证书解密
			plaintexts = getFieldCertCiphertexts(tempDatas);
		}
		catch (ServiceException e)
		{
			// 招标项目名称
			String tpName = SessionUtils.getBidProjectName();
			StringBuilder sb = new StringBuilder();
			sb.append("开标投标信息摘要解密：失败，招标项目:[").append(tpName).append("]中投标人[")
					.append(SessionUtils.getCompanyName()).append("]，请处理!");
			MsgUtils.send(sb.toString());
			logger.error(LogUtils.format("调用开放式授权解密密码失败!", data), e);
			throw e;
		}
		catch (Exception e)
		{
			// 招标项目名称
			String tpName = SessionUtils.getBidProjectName();
			StringBuilder sb = new StringBuilder();
			sb.append("开标投标信息摘要解密：失败，招标项目:[").append(tpName).append("]中投标人[")
					.append(SessionUtils.getCompanyName()).append("]，请处理!");
			MsgUtils.send(sb.toString());
			logger.error(LogUtils.format("调用开放式授权解密密码失败!", data), e);
			throw new ServiceException("", "解密失败!", e);
		}
		// 解密投标信息摘要
		doDecryptBidderElement(plaintexts);
	}

	/**
	 * 获取领域证书密码明文<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tempDatas
	 * @return
	 * @throws ServiceException
	 */
	private JSONArray getFieldCertCiphertexts(
			List<Record<String, Object>> tempDatas) throws ServiceException
	{
		// 证书颁发机构
		String issuerType = "-1";
		// 文件密码对
		JSONArray plaintexts = new JSONArray();
		JSONObject obj = null;
		// 密文
		String ciphertext = null;
		// 明文
		String plaintext = null;
		// 密码信息
		JSONObject pwdInfo = null;
		// 迭代列表
		for (Record<String, Object> td : tempDatas)
		{
			// 获取文件描述JSON
			obj = td.getJSONObject("V_FILE_INFO_JSON");
			// 获取密码
			ciphertext = getCiphertext(obj, issuerType,
					SOZONE_FIELD_CER_SERIAL, SOZONE_FIELD_CER_DATA);
			// 调用开放式授权进行解密
			plaintext = SozoneCipher.j(ciphertext);
			pwdInfo = new JSONObject();
			pwdInfo.put("ID", td.getString("ID"));
			pwdInfo.put("PLAINTEXT", plaintext);
			plaintexts.add(pwdInfo);
		}
		return plaintexts;
	}

	/**
	 * 解密投标信息摘要<br/>
	 * <p>
	 * </p>
	 * 
	 * @param plaintexts
	 * @throws ServiceException
	 */
	private void doDecryptBidderElement(JSONArray plaintexts)
			throws ServiceException
	{
		logger.info(LogUtils.format("解密投标信息摘要", plaintexts));
		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPDecryptLock();
		// -----------------------------------------------------
		try
		{
			// 获取锁
			lock.lock();
			JSONObject obj = null;
			Record<String, Object> tempInfo = null;
			for (int i = 0; i < plaintexts.size(); i++)
			{
				obj = plaintexts.getJSONObject(i);
				try
				{
					tempInfo = decryptBidderElementInfo(obj);
					logger.info(LogUtils.format("摘要信息", tempInfo));
				}
				catch (ServiceException e)
				{
					tempInfo = new RecordImpl<String, Object>();
					tempInfo.setColumn("ID", obj.getString("ID"));
					// 投标信息摘要解密失败
					tempInfo.setColumn("N_STATUS",
							DecryptStatus.DecryptSummaryFail.getStatus());
					throw e;
				}
				finally
				{
					if (null != tempInfo)
					{
						modifyDecryptTempInfo(tempInfo);
						this.activeRecordDAO.auto()
								.table(TableName.DECRYPT_TEMP_DATA)
								.modify(tempInfo);
					}
				}
			}
			String tpid = SessionUtils.getTPID();
			String unitfy = SessionUtils.getSocialcreditNO();
			String orgCode = SessionUtils.getCompanyCode();
			try
			{
				// 解析投标文件
				getBidderDocumentService().parseDecryptTempData(tpid, unitfy,
						orgCode);
			}
			catch (ServiceException e)
			{
				// 招标项目名称
				String tpName = SessionUtils.getBidProjectName();
				StringBuilder sb = new StringBuilder();
				sb.append("开标解析投标文件解密数据：失败，招标项目:[").append(tpName)
						.append("]中投标人[").append(SessionUtils.getCompanyName())
						.append("]，请处理!");
				MsgUtils.send(sb.toString());
				logger.error(
						LogUtils.format("解析投标文件解密数据失败!", tpid, unitfy, orgCode),
						e);
				throw e;
			}
			catch (Exception e)
			{
				// 招标项目名称
				String tpName = SessionUtils.getBidProjectName();
				StringBuilder sb = new StringBuilder();
				sb.append("开标解析投标文件解密数据：失败，招标项目:[").append(tpName)
						.append("]中投标人[").append(SessionUtils.getCompanyName())
						.append("]，请处理!");
				MsgUtils.send(sb.toString());
				logger.error(
						LogUtils.format("解析投标文件解密数据失败!", tpid, unitfy, orgCode),
						e);
				throw new ServiceException("", "解密失败!", e);
			}
		}
		finally
		{
			// 如果已经没有线程等待获取锁
			if (!lock.hasQueuedThreads())
			{
				// 干掉锁
				removeTPDecryptLock();
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	/**
	 * 解密投标信息摘要<br/>
	 * <p>
	 * </p>
	 * 
	 * @param jobj
	 * @return 解密信息
	 * @throws ServiceException
	 */
	private Record<String, Object> decryptBidderElementInfo(JSONObject jobj)
			throws ServiceException
	{
		logger.debug(LogUtils.format("解密入参", jobj));
		String id = jobj.getString("ID");
		String plaintext = jobj.getString("PLAINTEXT");
		// 获取临时对象
		Record<String, Object> tempInfo = this.activeRecordDAO.auto()
				.table(TableName.DECRYPT_TEMP_DATA).get(id);
		logger.debug(LogUtils.format("解密临时记录信息", tempInfo));
		Record<String, Object> temp = null;
		try
		{
			// 如果存在要解密的内容
			if (!CollectionUtils.isEmpty(tempInfo))
			{
				long start = System.currentTimeMillis();
				jobj = tempInfo.getJSONObject("V_FILE_INFO_JSON");
				logger.debug(LogUtils.format("投标文件信息描述JSON：", jobj));
				if (null == jobj || jobj.isEmpty())
				{
					throw new ServiceException("", "无效的投标文件描述信息!");
				}
				logger.debug(LogUtils.format("解密的密码：", plaintext));
				String eleB64 = jobj.getString("ELEMENT");
				logger.debug(LogUtils.format("解密前的投标信息摘要：", eleB64));
				// 这里只解密投标信息摘要
				String eleJson = BidderDocumentDecryptor.decrypt(eleB64,
						tempInfo.getInteger("N_ALGORITHM_TYPE"), plaintext);
				logger.debug(LogUtils.format("解密后的投标信息摘要：", eleJson));
				// 投标信息摘要
				JSONArray element = JSON.parseArray(eleJson);
				// 修改临时信息
				// 这里为了避免一级缓存被修改所以不直接操作临时对象
				temp = new RecordImpl<String, Object>();
				temp.setColumn("ID", id);
				// 投标信息摘要解密完成
				temp.setColumn("N_STATUS",
						DecryptStatus.DecryptSummarySuccess.getStatus());
				// 设置投标信息摘要
				temp.setColumn("V_ELEMENT_JSON", element);
				// 设置明文密码
				temp.setColumn("V_PWD", plaintext);
				long end = System.currentTimeMillis();
				// 设置消耗时间
				temp.setColumn("N_DECRYPT_SUMMARY_CONSUMING", end - start);
				return temp;
			}
		}
		catch (ServiceException e)
		{
			// 招标项目名称
			String tpName = SessionUtils.getBidProjectName();
			StringBuilder sb = new StringBuilder();
			sb.append("开标投标信息摘要解密：失败，招标项目:[").append(tpName).append("]中投标人[")
					.append(SessionUtils.getCompanyName()).append("]，请处理!");
			MsgUtils.send(sb.toString());
			logger.error(LogUtils.format("解密投标信息摘要失败!", jobj, tempInfo), e);
			throw e;
		}
		catch (Exception e)
		{
			// 招标项目名称
			String tpName = SessionUtils.getBidProjectName();
			StringBuilder sb = new StringBuilder();
			sb.append("开标投标信息摘要解密：失败，招标项目:[").append(tpName).append("]中投标人[")
					.append(SessionUtils.getCompanyName()).append("]，请处理!");
			MsgUtils.send(sb.toString());
			logger.error(LogUtils.format("解密投标信息摘要失败!", jobj, tempInfo), e);
			throw new ServiceException("", "解密失败!", e);
		}
		return null;
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

}
