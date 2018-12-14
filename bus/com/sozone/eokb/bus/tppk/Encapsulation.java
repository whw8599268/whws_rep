/**
 * 包名：com.sozone.eokb.bus.tppk
 * 文件名：Encapsulation.java<br/>
 * 创建时间：2018-10-13 上午9:32:30<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.tppk;

import java.io.File;
import java.io.PrintWriter;
import java.security.Key;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.common.utils.JDKEncryptDecryptUtils;
import com.sozone.aeolus.common.utils.SecretKeyUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 封标服务接口<br/>
 * <p>
 * 封标服务接口<br/>
 * </p>
 * Time：2018-10-13 上午9:32:30<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/tppk", desc = "封标服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class Encapsulation
{

	/**
	 * 项目解密锁
	 */
	private static final Map<String, ReentrantLock> PROJECT_PK_LOCK = new ConcurrentHashMap<String, ReentrantLock>();

	/**
	 * 封标密钥Session Key
	 */
	private static final String FB_CIPHER_SESSION_KEY = "$_FB_CIPHER_$";

	/**
	 * 加解密时间常量,即每B的加解密时间0.00004
	 */
	private static final double JJMCL = 0.00004;

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Encapsulation.class);

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
	 * 获取招标项目锁<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 互斥锁锁对象
	 */
	private static synchronized ReentrantLock getTPPKLock(String tpid)
	{
		// 获取项目锁
		ReentrantLock lock = PROJECT_PK_LOCK.get(tpid);
		// 如果锁不存在
		if (null == lock)
		{
			// 构建互斥锁
			lock = new ReentrantLock();
			PROJECT_PK_LOCK.put(tpid, lock);
		}
		return lock;
	}

	/**
	 * 写出解析响应<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 * @param funName
	 *            js回调函数名称
	 * @param msg
	 *            消息
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
			logger.error(LogUtils.format("写出响应时发生异常!"), e);
		}
	}

	/**
	 * 设置封标密钥<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             Facade异常
	 */
	@Path(value = "/setltpc", desc = "设置封标密钥")
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void setLockTPCipher(AeolusData data) throws FacadeException
	{
		logger.info(LogUtils.format("设置封标密钥", data));
		String tpid = SessionUtils.getTPID();
		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPPKLock(tpid);
		// 尝试获取锁，如果已经被其他线程占用
		if (!lock.tryLock())
		{
			logger.error(LogUtils.format("已经存在封标线程,用户重复刷新!", tpid, data));
			throw new ServiceException("", "当前招标项目正在被封标中!");
		}
		// -----------------------------------------------------
		try
		{
			Record<String, Object> tpInfo = this.activeRecordDAO.pandora()
					.SELECT("V_JSON_OBJ")
					.FROM(TableName.EKB_T_TENDER_PROJECT_INFO)
					.EQUAL("ID", tpid).get();
			if (CollectionUtils.isEmpty(tpInfo))
			{
				logger.error(LogUtils.format("无法获取到招标项目信息!", tpid, data));
				throw new ServiceException("", "无法获取到招标项目信息!");
			}
			// 获取扩展信息
			JSONObject jobj = tpInfo.getJSONObject("V_JSON_OBJ");
			if (jobj == null)
			{
				jobj = new JSONObject();
			}
			// 如果已经封标
			if (null != jobj.getBoolean("IS_FB")
					&& jobj.getBooleanValue("IS_FB"))
			{
				logger.error(LogUtils.format("招标项目已经被封标了,不能重复执行封标操作!", tpid,
						data));
				throw new ServiceException("", "招标项目已经被封标了!");
			}
			// 获取封标的密码
			String fbPwd = data.getParam("FB_PWD");
			if (StringUtils.isEmpty(fbPwd))
			{
				logger.error(LogUtils.format("封标密码不允许为空!", data));
				throw new ServiceException("", "封标密码不允许为空!");
			}
			// 封标的证书序列号
			String fbCertSerial = data.getParam("KeyCertSerial");
			if (StringUtils.isEmpty(fbCertSerial))
			{
				logger.error(LogUtils.format("无效的封标证书信息!", data));
				throw new ServiceException("", "无效的封标证书信息!");
			}
			// 封标的证书颁发机构
			String fbCertIssuerType = data.getParam("IssuerType");
			if (StringUtils.isEmpty(fbCertIssuerType))
			{
				logger.error(LogUtils.format("无效的封标证书信息!", data));
				throw new ServiceException("", "无效的封标证书信息!");
			}
			// 封标的证书内容
			String fbCertData = data.getParam("KeyCert");
			if (StringUtils.isEmpty(fbCertData))
			{
				throw new ServiceException("", "无效的封标证书信息!");
			}
			fbCertData = StringUtils.replace(fbCertData, "\r", "");
			fbCertData = StringUtils.replace(fbCertData, "\n", "");
			Record<String, Object> fbInfo = new RecordImpl<String, Object>();
			fbInfo.setColumn("IS_FB", true);
			fbInfo.setColumn("FB_PWD", fbPwd);
			fbInfo.setColumn(
					"FB_CERT_INFO",
					new RecordImpl<String, Object>()
							.setColumn("CERT_SERIAL", fbCertSerial)
							.setColumn("TYPE", fbCertIssuerType)
							.setColumn("CERT_BSAE64", fbCertData));
			SessionUtils.setAttribute(FB_CIPHER_SESSION_KEY, fbInfo);
		}
		finally
		{
			// 如果已经没有线程等待获取锁
			if (!lock.hasQueuedThreads())
			{
				// 干掉锁
				PROJECT_PK_LOCK.remove(tpid);
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	/**
	 * 封标<br/>
	 * <p>
	 * 封标
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             Facade异常
	 */
	@Path(value = "/lock", desc = "封标")
	@Service
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void lockTP(AeolusData data) throws FacadeException
	{
		logger.info(LogUtils.format("封标", data));
		String funName = "disposeProgress";
		// 消息flag 0开始,200结束,1正常进行,-1错误,-99被占用
		Record<String, Object> msg = new RecordImpl<String, Object>();
		Record<String, Object> fbInfo = SessionUtils
				.getAttribute(FB_CIPHER_SESSION_KEY);
		if (CollectionUtils.isEmpty(fbInfo))
		{
			logger.error(LogUtils.format("无法获取封标密钥!", data));
			msg.clear();
			msg.setColumn("FLAG", -1);
			msg.setColumn("MSG", "无法获取封标密钥!");
			writeParseResponse(data, funName, msg);
			return;
		}

		String tpid = SessionUtils.getTPID();
		// 招标项目名称
		String tpName = SessionUtils.getTenderProjectInfo().getString(
				"V_TENDER_PROJECT_NAME");
		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPPKLock(tpid);
		// 尝试获取锁，如果已经被其他线程占用
		if (!lock.tryLock())
		{
			logger.error(LogUtils
					.format("已经存在封标线程,用户重复刷新!", tpid, tpName, data));
			msg.clear();
			msg.setColumn("FLAG", -99);
			msg.setColumn("MSG", "当前招标项目正在被封标中,请勿重复刷新!");
			writeParseResponse(data, funName, msg);
			// 获取锁,这一步其实就是为了让线程挂起
			lock.lock();
		}
		// -----------------------------------------------------
		try
		{
			Record<String, Object> tpInfo = this.activeRecordDAO.pandora()
					.SELECT("V_JSON_OBJ")
					.FROM(TableName.EKB_T_TENDER_PROJECT_INFO)
					.EQUAL("ID", tpid).get();
			if (CollectionUtils.isEmpty(tpInfo))
			{
				logger.error(LogUtils
						.format("无法获取到招标项目信息!", tpid, tpName, data));
				msg.clear();
				msg.setColumn("FLAG", -1);
				msg.setColumn("MSG", "无法获取到招标项目信息!");
				writeParseResponse(data, funName, msg);
				return;
			}
			// 获取扩展信息
			JSONObject jobj = tpInfo.getJSONObject("V_JSON_OBJ");
			if (jobj == null)
			{
				jobj = new JSONObject();
			}
			// 如果已经封标
			if (null != jobj.getBoolean("IS_FB")
					&& jobj.getBooleanValue("IS_FB"))
			{
				logger.error(LogUtils.format("招标项目已经被封标了,不能重复执行封标操作!", tpid,
						tpName, data));
				msg.clear();
				msg.setColumn("FLAG", -1);
				msg.setColumn("MSG", "招标项目已经被封标了!");
				writeParseResponse(data, funName, msg);
				return;
			}

			// 查询要封标的文件列表
			List<Record<String, Object>> tbrfs = this.activeRecordDAO
					.pandora()
					.SELECT("TBR.V_BIDDER_ORG_CODE, TD.V_FILESIZE")
					.FROM("EKB_T_TENDER_LIST TBR")
					.LEFT_OUTER_JOIN(
							"EKB_T_TBIMPORTBIDDING TD "
									+ "ON (TBR.V_TPID = TD.V_TPID "
									+ "AND TBR.V_BID_SECTION_ID = TD.V_BID_SECTION_ID "
									+ "AND TBR.V_BIDDER_ORG_CODE = TD.V_ORG_CODE)")
					.EQUAL("TBR.V_TPID", tpid).list();
			// 获取未解析的数量
			int count = tbrfs.size();
			logger.info(LogUtils.format("总共要加密" + count + "个投标文件!"));
			// 消息flag 0开始,200结束,1正常进行,-1错误
			msg.clear();
			msg.setColumn("FLAG", 0);
			writeParseResponse(data, funName, msg);
			if (count > 0)
			{
				int index = 0;
				// 文件大小
				Long fileSize = null;
				for (Record<String, Object> tbrf : tbrfs)
				{
					// 获取文件的大小
					fileSize = tbrf.getLong("V_FILESIZE");
					// 大小存在
					if (null != fileSize)
					{
						try
						{
							Thread.sleep((long) (fileSize * JJMCL));
						}
						catch (InterruptedException e)
						{
							logger.error(LogUtils.format("封标加密时发生异常!", e));
						}
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
			// 修改招标项目信息的状态
			jobj.putAll(fbInfo);
			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("V_JSON_OBJ", jobj.toJSONString());
			param.setColumn("N_FB_STATUS", 1);
			// 修改招标项目的封标状态
			this.activeRecordDAO.pandora()
					.UPDATE(TableName.EKB_T_TENDER_PROJECT_INFO).SET(param)
					.EQUAL("ID", tpid).excute();

			// 修改文件状态
			// List<Record<String, Object>> sfs = this.activeRecordDAO.pandora()
			// .SELECT("V_BID_SECTION_CODE")
			// .FROM(TableName.EKB_T_SECTION_INFO).EQUAL("V_TPID", tpid)
			// .list();
			// File dir = null;
			// for (Record<String, Object> sf : sfs)
			// {
			// dir = new File(
			// SystemParamUtils
			// .getString(SysParamKey.EBIDKB_DECRYPTFILE_PATH_URL),
			// sf.getString("V_BID_SECTION_CODE"));
			// // 如果目录存在
			// if (dir.exists() && dir.isDirectory())
			// {
			// }
			// }

			// 输出响应到页面
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
				PROJECT_PK_LOCK.remove(tpid);
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	/**
	 * 验证解除封标密钥<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             Facade异常
	 */
	@Path(value = "/vltpc/{tpid}", desc = "验证解除封标密钥")
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void doVerifyTPCipher(@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.info(LogUtils.format("验证解除封标密钥", data));
		// 验证方式
		String authType = data.getParam("AUTH_TYPE");
		if (StringUtils.isEmpty(authType))
		{
			logger.error(LogUtils.format("验证方式不允许为空!", data));
			throw new ServiceException("", "验证方式不允许为空!");
		}
		Record<String, Object> tpInfo = this.activeRecordDAO.pandora()
				.SELECT("V_JSON_OBJ").FROM(TableName.EKB_T_TENDER_PROJECT_INFO)
				.EQUAL("ID", tpid).get();
		if (CollectionUtils.isEmpty(tpInfo))
		{
			logger.error(LogUtils.format("无法获取到招标项目信息!", tpid, data));
			throw new ServiceException("", "无法获取到招标项目信息!");
		}
		// 获取扩展信息
		JSONObject jobj = tpInfo.getJSONObject("V_JSON_OBJ");
		// 如果未封标
		if (null == jobj || null == jobj.getBoolean("IS_FB")
				|| !jobj.getBooleanValue("IS_FB"))
		{
			logger.error(LogUtils.format("招标项目未被封标,不能执行解除封标操作!", tpid, data));
			throw new ServiceException("", "招标项目未被封标,不能执行解除封标操作!");
		}
		// 证书方式
		if (StringUtils.equals(authType, "1"))
		{
			// 封标的证书内容
			String fbCertData = data.getParam("KeyCert");
			if (StringUtils.isEmpty(fbCertData))
			{
				logger.error(LogUtils.format("无效的证书信息!", data));
				throw new ServiceException("", "无效的证书信息!");
			}
			fbCertData = StringUtils.replace(fbCertData, "\r", "");
			fbCertData = StringUtils.replace(fbCertData, "\n", "");
			JSONObject cerInfo = jobj.getJSONObject("FB_CERT_INFO");
			if (CollectionUtils.isEmpty(cerInfo))
			{
				logger.error(LogUtils.format("无法获取到封标的加密证书信息!", data));
				throw new ServiceException("", "无法获取到封标的加密证书信息!");
			}
			String certB64 = cerInfo.getString("CERT_BSAE64");
			// 证书不对
			if (!StringUtils.equals(certB64, fbCertData))
			{
				logger.error(LogUtils.format("无效的解封证书!", data));
				throw new ServiceException("", "无效的解封证书,请确插入的证书是否为封标时使用的证书!");
			}
			return;
		}
		else if (StringUtils.equals(authType, "0"))
		{
			// 密码方式
			// 获取封标的密码
			String fbPwd = data.getParam("FB_PWD");
			if (StringUtils.isEmpty(fbPwd))
			{
				logger.error(LogUtils.format("解除封标密码不允许为空!", data));
				throw new ServiceException("", "解除封标密码不允许为空!");
			}
			String pwd = jobj.getString("FB_PWD");
			// 密码不对
			if (!StringUtils.equals(pwd, fbPwd))
			{
				logger.error(LogUtils.format("解除封标密码不正确!", data));
				throw new ServiceException("", "解除封标密码不正确!");
			}
			return;
		}
		else
		{
			logger.error(LogUtils.format("无效的验证方式!", data));
			throw new ServiceException("", "无效的验证方式!");
		}
	}

	/**
	 * 解封<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             Facade异常
	 */
	@Path(value = "/unlock/{tpid}", desc = "解封")
	@Service
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void unlockTP(@PathParam("tpid") String tpid, AeolusData data)
			throws FacadeException
	{
		logger.info(LogUtils.format("解封", data));
		String funName = "disposeProgress";
		// 消息flag 0开始,200结束,1正常进行,-1错误,-99被占用
		Record<String, Object> msg = new RecordImpl<String, Object>();
		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPPKLock(tpid);
		// 尝试获取锁，如果已经被其他线程占用
		if (!lock.tryLock())
		{
			logger.error(LogUtils.format("已经存在解封线程,用户重复刷新!", tpid, data));
			msg.clear();
			msg.setColumn("FLAG", -99);
			msg.setColumn("MSG", "当前招标项目正在解除封标中,请勿重复刷新!");
			writeParseResponse(data, funName, msg);
			// 获取锁,这一步其实就是为了让线程挂起
			lock.lock();
		}
		// -----------------------------------------------------
		try
		{
			Record<String, Object> tpInfo = this.activeRecordDAO.pandora()
					.SELECT("V_JSON_OBJ")
					.FROM(TableName.EKB_T_TENDER_PROJECT_INFO)
					.EQUAL("ID", tpid).get();
			if (CollectionUtils.isEmpty(tpInfo))
			{
				logger.error(LogUtils.format("无法获取到招标项目信息!", tpid, data));
				msg.clear();
				msg.setColumn("FLAG", -1);
				msg.setColumn("MSG", "无法获取到招标项目信息!");
				writeParseResponse(data, funName, msg);
				return;
			}
			// 获取扩展信息
			JSONObject jobj = tpInfo.getJSONObject("V_JSON_OBJ");
			// 如果未封标
			if (null == jobj || null == jobj.getBoolean("IS_FB")
					|| !jobj.getBooleanValue("IS_FB"))
			{
				logger.error(LogUtils
						.format("招标项目未被封标,不能执行解除封标操作!", tpid, data));
				msg.clear();
				msg.setColumn("FLAG", -1);
				msg.setColumn("MSG", "招标项目未被封标,不能执行解除封标操作!");
				writeParseResponse(data, funName, msg);
				return;
			}

			// 查询要封标的文件列表
			List<Record<String, Object>> tbrfs = this.activeRecordDAO
					.pandora()
					.SELECT("TBR.V_BIDDER_ORG_CODE, TD.V_FILESIZE")
					.FROM("EKB_T_TENDER_LIST TBR")
					.LEFT_OUTER_JOIN(
							"EKB_T_TBIMPORTBIDDING TD "
									+ "ON (TBR.V_TPID = TD.V_TPID "
									+ "AND TBR.V_BID_SECTION_ID = TD.V_BID_SECTION_ID "
									+ "AND TBR.V_BIDDER_ORG_CODE = TD.V_ORG_CODE)")
					.EQUAL("TBR.V_TPID", tpid).list();
			// 获取未解析的数量
			int count = tbrfs.size();
			logger.info(LogUtils.format("总共要解密" + count + "个投标文件!"));
			// 消息flag 0开始,200结束,1正常进行,-1错误
			msg.clear();
			msg.setColumn("FLAG", 0);
			writeParseResponse(data, funName, msg);
			if (count > 0)
			{
				int index = 0;
				// 文件大小
				Long fileSize = null;
				for (Record<String, Object> tbrf : tbrfs)
				{
					// 获取文件的大小
					fileSize = tbrf.getLong("V_FILESIZE");
					// 大小存在
					if (null != fileSize)
					{
						try
						{
							Thread.sleep((long) (fileSize * JJMCL));
						}
						catch (InterruptedException e)
						{
							logger.error(LogUtils.format("封标解密时发生异常!", e));
						}
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
			// 修改招标项目信息的状态
			jobj.put("IS_FB", false);
			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("V_JSON_OBJ", jobj.toJSONString());
			param.setColumn("N_FB_STATUS", 0);
			// 修改招标项目的封标状态
			this.activeRecordDAO.pandora()
					.UPDATE(TableName.EKB_T_TENDER_PROJECT_INFO).SET(param)
					.EQUAL("ID", tpid).excute();

			// 修改文件状态
			// List<Record<String, Object>> sfs = this.activeRecordDAO.pandora()
			// .SELECT("V_BID_SECTION_CODE")
			// .FROM(TableName.EKB_T_SECTION_INFO).EQUAL("V_TPID", tpid)
			// .list();
			// File dir = null;
			// for (Record<String, Object> sf : sfs)
			// {
			// dir = new File(
			// SystemParamUtils
			// .getString(SysParamKey.EBIDKB_DECRYPTFILE_PATH_URL),
			// sf.getString("V_BID_SECTION_CODE"));
			// // 如果目录存在
			// if (dir.exists() && dir.isDirectory())
			// {
			// }
			// }

			// 输出响应到页面
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
				PROJECT_PK_LOCK.remove(tpid);
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	// src:c:/xxx/xxx.zip target:c/xxx/xxx.szfbf
	private static void encryptFile(File src, File target, String pwd)
			throws Exception
	{
		Key key = SecretKeyUtils.getAESKey(pwd);
		// Cipher对象实际完成加密操作
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		// 用密钥初始化Cipher对象
		cipher.init(Cipher.ENCRYPT_MODE, key);
		JDKEncryptDecryptUtils.encrypt(src, target, cipher);
	}

	// src:c:/xxx/xxx.szfbf target:c/xxx/xxx.zip
	private static void decryptFile(File src, File target, String pwd)
			throws Exception
	{
		Key key = SecretKeyUtils.getAESKey(pwd);
		// Cipher对象实际完成加密操作
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		// 用密钥初始化Cipher对象
		cipher.init(Cipher.DECRYPT_MODE, key);
		JDKEncryptDecryptUtils.encrypt(src, target, cipher);
	}
}
