/**
 * 包名：com.sozone.eokb.bus.decrypt
 * 文件名：DecryptBidderAction.java<br/>
 * 创建时间：2017-8-29 下午3:52:02<br/>
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.codec.digest.DigestUtils;
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
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.auth2.client.utils.RSCallUtils;
import com.sozone.auth2.client.utils.RSCallUtils.ContentType;
import com.sozone.eokb.bus.decrypt.service.BidderDocumentParseService;
import com.sozone.eokb.bus.decrypt.service.BidderDocumentParseServiceFactory;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.MsgUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 投标文件在线解密服务<br/>
 * <p>
 * </p>
 * Time：2017-8-29 下午3:52:02<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
@Path(value = "decrypt", desc = "投标文件在线解密服务")
@Permission(Level.Authenticated)
public class DecryptBidderAction
{

	/**
	 * 解密队列
	 */
	private static final BlockingQueue<String> DECRYPT_QUEUE = new LinkedBlockingQueue<String>(
			5);

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(DecryptBidderAction.class);

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
		String unitfy = SessionUtils.getSocialcreditNO();
		String orgcode = SessionUtils.getCompanyCode();
		// String companyName = SessionUtils.getCompanyName();
		List<Record<String, Object>> result = new ArrayList<Record<String, Object>>();
		// 获取投标人投递信息
		List<Record<String, Object>> docs = getBidderDocumentParseService()
				.getBidderDeliveryInfo(tpid, unitfy, orgcode);
		if (CollectionUtils.isEmpty(docs))
		{
			throw new ServiceException("DECRYPT-0001",
					"请检查是否插入了正确的数字证书,系统找不到当前数字证书对应的投标文件!");
		}
		for (Record<String, Object> doc : docs)
		{
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
		// 评标办法编码
		String code = SessionUtils.getTenderProjectTypeCode();
		String userID = ApacheShiroUtils.getCurrentUserID();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("userID", userID);
		int count = this.activeRecordDAO
				.auto()
				.table(TableName.DECRYPT_TEMP)
				.setCondition("AND",
						"V_TPID = #{tpid} AND V_CREATE_USER = #{userID}")
				.count(param);
		// 如果没有初始化
		if (count <= 0)
		{
			List<Record<String, Object>> docs = getBidderDocList();
			// 源文件
			File source = null;
			// 临时目录
			File tempDir = null;
			// 目标目录
			File targetDir = null;
			// 临时
			Record<String, Object> temp = null;
			for (Record<String, Object> doc : docs)
			{
				// 设置投递表ID,一定要在设置主键之前做
				doc.setColumn("V_TBID", doc.getString("ID"));
				source = doc.getColumn("V_SOURCE_PATH");
				doc.setColumn("V_SOURCE_PATH", FilenameUtils
						.separatorsToUnix(source.getAbsolutePath()));
				tempDir = doc.getColumn("V_TEMP_DIR_PATH");
				doc.setColumn("V_TEMP_DIR_PATH", FilenameUtils
						.separatorsToUnix(tempDir.getAbsolutePath()));
				targetDir = doc.getColumn("V_TARGET_DIR_PATH");
				doc.setColumn("V_TARGET_DIR_PATH", FilenameUtils
						.separatorsToUnix(targetDir.getAbsolutePath()));
				// 解析投标文件
				temp = DecryptBidder.parseBidderDocument(source, tempDir);
				doc.putAll(temp);
				// 这边不要重新设置了,直接用投递表的ID即可，解决临时表重复插入问题2018-6-7 11:24:32
				// doc.setColumn("ID", Random.generateUUID());
				doc.setColumn("V_FTID", doc.getString("V_FTID"));
				doc.setColumn("V_BEM_CODE", code);
				doc.setColumn("N_CREATE_TIME", System.currentTimeMillis());
				doc.setColumn("V_CREATE_USER", userID);
				doc.setColumn("V_TPID", tpid);
				doc.setColumn("N_STATUS", 0);
				// 先删除后插入
				this.activeRecordDAO.auto().table(TableName.DECRYPT_TEMP)
						.remove(doc.getString("ID"));
				this.activeRecordDAO.auto().table(TableName.DECRYPT_TEMP)
						.save(doc);
			}
			return docs;
		}
		// 查询
		return this.activeRecordDAO
				.auto()
				.table(TableName.DECRYPT_TEMP)
				.setCondition("AND",
						"V_TPID = #{tpid} AND V_CREATE_USER = #{userID}")
				.list(param);

	}

	/**
	 * 获取招标项目中对应的介质证书要解密的所有非对称密钥列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectID
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @return 获取招标项目中对应的介质证书要解密的所有非对称密钥列表
	 * @throws FacadeException
	 *             异常
	 */
	// 定义路径
	@Path(value = "ciphertext", desc = "获取招标项目中对应的介质证书要解密的所有非对称密钥列表")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public ResultVO<List<Record<String, Object>>> getCertPassword(
			@PathParam("tpid") String tenderProjectID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取招标项目中对应的介质证书要解密的所有非对称密钥列表", data));
		// 证书base64
		String certData = data.getParam("CERT_DATA");
		// 干掉换行符
		certData = StringUtils.replace(certData, "\r", "");
		certData = StringUtils.replace(certData, "\n", "");
		// 证书序列号
		// String certSerial = data.getParam("CERT_SERIAL");

		// 获取所有需要解密的列表
		List<Record<String, Object>> tempInfos = getDecryptTempInfos();
		// 要解密的密文列表
		List<Record<String, Object>> rs = new LinkedList<Record<String, Object>>();
		Record<String, Object> record = null;
		JSONObject jobj = null;
		String ct = null;
		// 迭代获取信息
		for (Record<String, Object> tif : tempInfos)
		{
			record = new RecordImpl<String, Object>();
			record.setColumn("ID", tif.getString("ID"));
			jobj = JSONObject.parseObject(tif.getString("V_CIPHERTEXT_JSON"));
			ct = jobj.getString(certData);
			if (StringUtils.isNotEmpty(ct))
			{
				record.setColumn("CIPHERTEXT", ct);
				rs.add(record);
			}
		}
		// 如果要解密的数量与当前证书要解的不一致
		if (tempInfos.size() != rs.size())
		{
			rs.clear();
		}
		if (CollectionUtils.isEmpty(rs))
		{
			throw new ServiceException("DECRYPT-0001",
					"请检查是否插入了正确的数字证书,系统找不到当前数字证书对应的投标文件!");
		}
		ResultVO<List<Record<String, Object>>> result = new ResultVO<List<Record<String, Object>>>(
				true);
		result.setResult(rs);
		return result;
	}

	/**
	 * 使用CA客户端得到的密钥解密临时文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             异常
	 */
	// 定义路径
	@Path(value = "ocx", desc = "使用CA客户端得到的密钥解密临时文件")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void decryptTempFileByOcx(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("使用CA客户端得到的密钥解密临时文件", data));
		// 当前线程的ID
		long threadID = Thread.currentThread().getId();
		// 当前线程的名称
		String threadName = Thread.currentThread().getName();
		threadName = threadID + "-" + threadName;
		try
		{
			// 排队进入队列,如果此时队列是尚有空闲,直接进入,如果没有将会被挂起等待。
			DECRYPT_QUEUE.put(threadName);
			String plaintextJson = data.getParam("PLAINTEXTS");
			if (StringUtils.isEmpty(plaintextJson))
			{
				logger.error(LogUtils.format("解密招标文件失败,客户端返回的对称解密密钥为空!"));
				throw new ServiceException("", "解密招标文件失败,无法获取到客户端返回密钥!");
			}
			JSONArray array = JSON.parseArray(plaintextJson);
			String id = null;
			String plaintext = null;
			JSONObject obj = null;
			Record<String, Object> tempInfo = null;
			File efb = null;
			// 临时目录
			File tempDir = null;
			// 目标目录
			File targetDir = null;
			for (int i = 0; i < array.size(); i++)
			{
				obj = array.getJSONObject(i);
				id = obj.getString("ID");
				plaintext = obj.getString("PLAINTEXT");
				tempInfo = this.activeRecordDAO.auto()
						.table(TableName.DECRYPT_TEMP).get(id);
				// 如果存在要解密的内容
				if (!CollectionUtils.isEmpty(tempInfo))
				{
					efb = new File(tempInfo.getString("V_EFB_FILE_PATH"));
					tempDir = new File(tempInfo.getString("V_TEMP_DIR_PATH"));
					targetDir = new File(
							tempInfo.getString("V_TARGET_DIR_PATH"));
					DecryptBidder.decryptAndUnpackBidderDocument(efb,
							plaintext, targetDir, tempDir,
							tempInfo.getString("V_FTID"),
							tempInfo.getString("V_BEM_CODE"));
					// 修改解密状态
					modifyDecryptStatus(tempInfo.getString("ID"));
				}
			}
			// 解析投标文件
			getBidderDocumentParseService().parseBidderDocument();
		}
		catch (ServiceException e)
		{
			// 招标项目名称
			String tpName = SessionUtils.getBidProjectName();
			StringBuilder sb = new StringBuilder();
			sb.append("开标投标解密：失败，招标项目:[").append(tpName).append("]中投标人[")
					.append(SessionUtils.getCompanyName()).append("]，请处理!");
			MsgUtils.send(sb.toString());
			logger.error(LogUtils.format("解密解压失败!", data), e);
			throw e;
		}
		catch (Exception e)
		{
			// 招标项目名称
			String tpName = SessionUtils.getBidProjectName();
			StringBuilder sb = new StringBuilder();
			sb.append("开标投标解密：失败，招标项目:[").append(tpName).append("]中投标人[")
					.append(SessionUtils.getCompanyName()).append("]，请处理!");
			MsgUtils.send(sb.toString());
			logger.error(LogUtils.format("解密解压失败!", data), e);
			throw new ServiceException("", "解密解压失败!", e);
		}
		finally
		{
			// 最后一定要离开队列
			DECRYPT_QUEUE.remove(threadName);
		}
	}

	// --------------------------------------------------------------------

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
	@Path(value = "sm9", desc = "使用软证进行解密")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void decryptBidderDocBySoftCert(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("使用软证进行解密", data));
		// 当前线程的ID
		long threadID = Thread.currentThread().getId();
		// 当前线程的名称
		String threadName = Thread.currentThread().getName();
		threadName = threadID + "-" + threadName;
		try
		{
			// 排队进入队列,如果此时队列是尚有空闲,直接进入,如果没有将会被挂起等待。
			DECRYPT_QUEUE.put(threadName);
			// 获取所有需要解密的列表
			List<Record<String, Object>> tempInfos = getDecryptTempInfos();
			JSONObject jobj = null;
			// 判断当前用户类型
			int type = ApacheShiroUtils.getCurrentUser().getInteger("N_TYPE");
			String ciphertext = null;
			String plaintext = null;
			File efb = null;
			// 临时目录
			File tempDir = null;
			// 目标目录
			File targetDir = null;
			// 如果是软证用户
			if (type == 1)
			{
				logger.debug(LogUtils.format("软证用户解密", type, tempInfos));
				// 证书序列号
				String certSerial = ApacheShiroUtils.getCurrentUser()
						.getString("V_SERIAL");
				try
				{
					// 迭代获取信息解密
					for (Record<String, Object> tempInfo : tempInfos)
					{
						jobj = JSONObject.parseObject(tempInfo
								.getString("V_CIPHERTEXT_JSON"));
						ciphertext = jobj.getString(certSerial);
						// 调用软证解密
						plaintext = decryptByCurrentCert(ciphertext);

						efb = new File(tempInfo.getString("V_EFB_FILE_PATH"));
						tempDir = new File(
								tempInfo.getString("V_TEMP_DIR_PATH"));
						targetDir = new File(
								tempInfo.getString("V_TARGET_DIR_PATH"));
						DecryptBidder.decryptAndUnpackBidderDocument(efb,
								plaintext, targetDir, tempDir,
								tempInfo.getString("V_FTID"),
								tempInfo.getString("V_BEM_CODE"));
						// 修改解密状态
						modifyDecryptStatus(tempInfo.getString("ID"));
					}
					// 解析投标文件
					getBidderDocumentParseService().parseBidderDocument();
					return;
				}
				catch (ServiceException e)
				{
					logger.error(LogUtils.format("使用当前软证书进行解密失败!"), e);
				}
			}
			// 使用领域证书解密
			// 迭代获取信息解密
			for (Record<String, Object> tempInfo : tempInfos)
			{
				jobj = JSONObject.parseObject(tempInfo
						.getString("V_CIPHERTEXT_JSON"));
				ciphertext = jobj.getString("BID-FIELD");
				// 调用软证解密
				plaintext = decryptByFieldCert(ciphertext);
				efb = new File(tempInfo.getString("V_EFB_FILE_PATH"));
				tempDir = new File(tempInfo.getString("V_TEMP_DIR_PATH"));
				targetDir = new File(tempInfo.getString("V_TARGET_DIR_PATH"));
				DecryptBidder.decryptAndUnpackBidderDocument(efb, plaintext,
						targetDir, tempDir, tempInfo.getString("V_FTID"),
						tempInfo.getString("V_BEM_CODE"));
				// 修改解密状态
				modifyDecryptStatus(tempInfo.getString("ID"));
			}

			// 解析投标文件
			getBidderDocumentParseService().parseBidderDocument();
		}
		catch (ServiceException e)
		{
			// 招标项目名称
			String tpName = SessionUtils.getBidProjectName();
			StringBuilder sb = new StringBuilder();
			sb.append("开标投标解密：失败，招标项目:[").append(tpName).append("]中投标人[")
					.append(SessionUtils.getCompanyName()).append("]，请处理!");
			MsgUtils.send(sb.toString());
			logger.error(LogUtils.format("解密解压失败!", data), e);
			throw e;
		}
		catch (Exception e)
		{
			// 招标项目名称
			String tpName = SessionUtils.getBidProjectName();
			StringBuilder sb = new StringBuilder();
			sb.append("开标投标解密：失败，招标项目:[").append(tpName).append("]中投标人[")
					.append(SessionUtils.getCompanyName()).append("]，请处理!");
			MsgUtils.send(sb.toString());
			logger.error(LogUtils.format("解密解压失败!", data), e);
			throw new ServiceException("", "解密解压失败!", e);
		}
		finally
		{
			// 最后一定要离开队列
			DECRYPT_QUEUE.remove(threadName);
		}
	}

	/**
	 * 使用领域证书进行解密<br/>
	 * <p>
	 * </p>
	 * 
	 * @param ciphertext
	 * @return
	 * @throws ServiceException
	 */
	private String decryptByFieldCert(String ciphertext)
			throws ServiceException
	{
		logger.debug(LogUtils.format("使用领域证书进行解密", ciphertext));
		// 获取AccessToken
		String accessToken = ApacheShiroUtils.getCurrentUser().getColumn(
				"ACCESS_TOKEN");
		Map<String, String> params = new HashMap<String, String>();
		params.put("CIPHERTEXT", ciphertext);
		logger.debug(LogUtils.format("请求参数", SystemParamUtils.getProperty(
				SysParamKey.AS_FIELD_CERT_DECRYPT_URL_KEY, ""), accessToken,
				ContentType.APPLICATION_FORM_URLENCODED, params));
		OAuthResourceResponse response = RSCallUtils.doPost(SystemParamUtils
				.getProperty(SysParamKey.AS_FIELD_CERT_DECRYPT_URL_KEY, ""),
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
			logger.debug(LogUtils.format("解密结果", json));
			if (StringUtils.isEmpty(json))
			{
				logger.error(LogUtils.format("使用领域证书进行解密失败,响应为空!", json,
						response));
				throw new ServiceException("", "使用领域证书进行解密失败,响应为空!");
			}
			JSONObject obj = JSON.parseObject(json);
			return obj.getString("PLAINTEXT");
		}
		logger.error(LogUtils.format("使用领域证书进行解密失败!", contentType,
				response.getBody()));
		// 响应失败
		throw new ServiceException("", "使用领域证书进行解密失败!");
	}

	/**
	 * 使用当前软证书进行解密<br/>
	 * <p>
	 * </p>
	 * 
	 * @param ciphertext
	 * @return
	 * @throws ServiceException
	 */
	private String decryptByCurrentCert(String ciphertext)
			throws ServiceException
	{
		logger.debug(LogUtils.format("使用软证书进行解密", ciphertext));
		// 获取AccessToken
		String accessToken = ApacheShiroUtils.getCurrentUser().getColumn(
				"ACCESS_TOKEN");
		Map<String, String> params = new HashMap<String, String>();
		params.put("CIPHERTEXT", ciphertext);
		params.put("PK_PWD", DigestUtils.md5Hex("111111"));
		OAuthResourceResponse response = RSCallUtils.doPost(SystemParamUtils
				.getProperty(SysParamKey.AS_CURRENT_CERT_DECRYPT_URL_KEY, ""),
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
			logger.debug(LogUtils.format("解密结果", json));
			if (StringUtils.isEmpty(json))
			{
				logger.error(LogUtils.format("使用软证书进行解密失败,响应为空!", json,
						response));
				throw new ServiceException("", "使用软证书进行解密失败,响应为空!");
			}
			JSONObject obj = JSON.parseObject(json);
			return obj.getString("PLAINTEXT");
		}
		logger.error(LogUtils.format("使用软证书进行解密失败!", contentType,
				response.getBody()));
		// 响应失败
		throw new ServiceException("", "使用软证书进行解密失败!");
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

}
