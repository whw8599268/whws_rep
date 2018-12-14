/**
 * 包名：com.sozone.eokb.bus.decrypt
 * 文件名：BidderDocumentDecryptor.java<br/>
 * 创建时间：2018-4-10 下午3:57:17<br/>
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.Key;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.common.utils.JDKEncryptDecryptUtils;
import com.sozone.aeolus.common.utils.SecretKeyUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.utils.ArchiverUtils;

/**
 * 投标文件解密器<br/>
 * <p>
 * 只支持最新的stbx格式<br/>
 * </p>
 * Time：2018-4-10 下午3:57:17<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class BidderDocumentDecryptor
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(BidderDocumentDecryptor.class);

	/**
	 * 对称加密算法密码
	 */
	private static final String DEFAULT_PWD = DigestUtils
			.md5Hex("www.okap.com").toUpperCase();

	/**
	 * DES
	 */
	private static final int DES_ALGORITHM_TYPE = 10;

	/**
	 * DES 加密转换的名称
	 */
	private static final String DES_TRANSFORMATION = "DES/ECB/PKCS5Padding";

	/**
	 * 3DES
	 */
	private static final int THREEDES_ALGORITHM_TYPE = 20;

	/**
	 * 3DES 加密转换的名称
	 */
	private static final String THREEDES_TRANSFORMATION = "DESede/ECB/PKCS5Padding";

	/**
	 * AES
	 */
	private static final int AES_ALGORITHM_TYPE = 30;

	/**
	 * AES 加密转换的名称
	 */
	private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";

	/**
	 * 投标文件扩展
	 */
	private static final String BIDDER_DOC_EXT_NAME = "stbx";

	/**
	 * 加密后的文件块后缀
	 */
	private static final String ENCRYPTED_FILE_BLOCK_EXT = ".efbx";

	/**
	 * 缓冲区大小
	 */
	private static final int BUFF_SIZE = 4096;

	/**
	 * 获取投标文件的描述JSON信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param stbx
	 *            投标文件
	 * @return 
	 *         解析后的内容,TB_FILE_JSON:JSON内容,V_EBF_FILE_PATH:加密块文件File对象,ALGORITHM_TYPE
	 *         :加密算法类型
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> getBidderDocumentJsonInfo(File stbx)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取投标文件的描述JSON信息", stbx));
		if (!stbx.exists())
		{
			logger.error(LogUtils.format("投标文件不存在!", stbx));
			throw new ServiceException("", "投标文件不存在!");
		}
		// 获取文件扩展名
		String extName = FilenameUtils.getExtension(stbx.getName());
		// 如果扩展名不对
		if (!StringUtils.endsWithIgnoreCase(extName, BIDDER_DOC_EXT_NAME))
		{
			logger.error(LogUtils.format("无效的投标文件扩展名!", stbx));
			throw new ServiceException("", "无效的投标文件!");
		}
		RandomAccessFile raf = null;
		try
		{
			Record<String, Object> result = new RecordImpl<String, Object>();
			logger.debug(LogUtils.format("投标文件是否可读", stbx.canRead()));
			logger.debug(LogUtils.format("投标文件是否可写", stbx.canWrite()));
			logger.debug(LogUtils.format("投标文件是否可执行", stbx.canExecute()));
			logger.debug(LogUtils.format("开始创建随机文件输入流", stbx));
			raf = new RandomAccessFile(stbx, "r");
			logger.debug(LogUtils.format("随机文件输入流创建完成", raf));
			// ----------------------处理前半段-----------------------------------
			logger.debug(LogUtils.format("解析对称加密算法"));
			// 第一块的总长度
			int firstBlockLength = 2;
			// 第一块的字符串
			byte[] bo1 = new byte[2];
			// 读取第一块
			int l = raf.read(bo1);
			if (l != firstBlockLength)
			{
				logger.error(LogUtils.format("读取加密算法部分失败,读取到的长度不对!"));
				throw new ServiceException("", "无效的投标文件!");
			}
			// 加密算法类型
			int algorithmType = Integer.valueOf(new String(bo1,
					ConstantEOKB.DEFAULT_CHARSET));
			logger.debug(LogUtils.format("对称加密算法类型", algorithmType));
			result.setColumn("ALGORITHM_TYPE", algorithmType);
			// ----------------------------------------------------
			// 读取B段的长度
			logger.debug(LogUtils.format("解析JSON字符串长度"));
			int jsonLength = 10;
			byte[] jb = new byte[10];
			// 读取第二块
			l = raf.read(jb);
			if (l != jsonLength)
			{
				logger.error(LogUtils.format("读取JSON字符串长度部分失败,读取到的长度不对!"));
				throw new ServiceException("", "无效的投标文件!");
			}
			int jsonDataLength = Integer.valueOf(new String(jb,
					ConstantEOKB.DEFAULT_CHARSET));
			logger.debug(LogUtils.format("JSON字符串长度", jsonDataLength));
			// ------------------------------------------------------
			logger.debug(LogUtils.format("解析JSON字符串"));
			byte[] jsb = new byte[jsonDataLength];
			l = raf.read(jsb);
			if (l != jsonDataLength)
			{
				logger.error(LogUtils.format("读取JSON字符串部分失败,读取到的长度不对!"));
				throw new ServiceException("", "无效的投标文件!");
			}
			// 加密前的JSON
			String ecodeJson = new String(jsb, ConstantEOKB.DEFAULT_CHARSET);
			// 使用默认密码解密
			String json = decrypt(ecodeJson, algorithmType, DEFAULT_PWD);
			logger.debug(LogUtils.format("JSON字符串内容", json));
			// 解析成JSON
			JSONObject tbInfo = JSON.parseObject(json);
			// 获取DATA Base64字符串
			String data = tbInfo.getString("DATA");
			byte[] temp = Base64.decodeBase64(data);
			// 获取真实的JSON字符串
			data = new String(temp, ConstantEOKB.DEFAULT_CHARSET);
			logger.debug(LogUtils.format("真实的DATA JSON字符串内容为:", data));
			JSONObject jobj = JSON.parseObject(data);
			// 将DATA 这个put进来
			tbInfo.putAll(jobj);
			result.setColumn("TB_FILE_JSON", tbInfo);
			return result;
		}
		catch (ServiceException e)
		{
			logger.error(LogUtils.format("获取投标文件描述JSON信息发生异常!", stbx), e);
			throw e;
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("获取投标文件描述JSON信息发生异常!", stbx), e);
			throw new ServiceException("", "获取投标文件描述JSON信息发生异常!", e);
		}
		finally
		{
			IOUtils.closeQuietly(raf);
		}
	}

	/**
	 * 获取投标文件解析临时文件<br/>
	 * <p>
	 * 必须是stbx
	 * </p>
	 * 
	 * @param stbx
	 *            投标文件
	 * @param tempDir
	 *            临时文件目录
	 * @return 临时文件efbx File对象
	 * @throws ServiceException
	 *             服务异常
	 */
	public static File getEfbxTempFile(File stbx, File tempDir)
			throws ServiceException
	{
		logger.debug(LogUtils.format("解析加密的投标文件,理出非对称加密内容,以及真正的文件加密块", stbx,
				tempDir));
		if (!stbx.exists())
		{
			logger.error(LogUtils.format("投标文件不存在!", stbx, tempDir));
			throw new ServiceException("", "投标文件不存在!");
		}
		// 获取文件扩展名
		String extName = FilenameUtils.getExtension(stbx.getName());
		// 如果扩展名不对
		if (!StringUtils.endsWithIgnoreCase(extName, BIDDER_DOC_EXT_NAME))
		{
			logger.error(LogUtils.format("无效的投标文件扩展名!", stbx, tempDir));
			throw new ServiceException("", "无效的投标文件!");
		}
		RandomAccessFile raf = null;
		try
		{
			// 临时文件目录
			if (!tempDir.exists())
			{
				tempDir.mkdirs();
			}
			logger.debug(LogUtils.format("临时文件夹是否可读", tempDir.canRead()));
			logger.debug(LogUtils.format("临时文件夹是否可写", tempDir.canWrite()));
			logger.debug(LogUtils.format("临时文件夹是否可执行", tempDir.canExecute()));
			logger.debug(LogUtils.format("投标文件是否可读", stbx.canRead()));
			logger.debug(LogUtils.format("投标文件是否可写", stbx.canWrite()));
			logger.debug(LogUtils.format("投标文件是否可执行", stbx.canExecute()));
			logger.debug(LogUtils.format("开始创建随机文件输入流", stbx));
			raf = new RandomAccessFile(stbx, "r");
			logger.debug(LogUtils.format("随机文件输入流创建完成", raf));
			// ----------------------处理前半段-----------------------------------
			logger.debug(LogUtils.format("跳过算法长度"));
			// 第一块的总长度
			int firstBlockLength = 2;
			raf.seek(firstBlockLength);
			// 读取B段的长度
			logger.debug(LogUtils.format("解析JSON字符串长度"));
			int jsonLength = 10;
			byte[] jb = new byte[10];
			// 读取第二块
			int l = raf.read(jb);
			if (l != jsonLength)
			{
				logger.error(LogUtils.format("读取JSON字符串长度部分失败,读取到的长度不对!"));
				throw new ServiceException("", "无效的投标文件!");
			}
			int jsonDataLength = Integer.valueOf(new String(jb,
					ConstantEOKB.DEFAULT_CHARSET));
			logger.debug(LogUtils.format("JSON字符串长度", jsonDataLength));
			// ------------------------------------------------
			logger.debug(LogUtils.format("解析临时文件"));
			// 实际的加密部分内容
			File efbx = new File(tempDir, FilenameUtils.getBaseName(stbx
					.getName()) + ENCRYPTED_FILE_BLOCK_EXT);
			// 实际内容开始位置
			long start = 12 + jsonDataLength;
			// 保存临时文件
			saveTempFile(raf, efbx, start);
			return efbx;
		}
		catch (ServiceException e)
		{
			logger.error(LogUtils.format("获取解密临时文件发生异常!", stbx, tempDir), e);
			throw e;
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("获取解密临时文件发生异常!", stbx, tempDir), e);
			throw new ServiceException("", "获取解密临时文件发生异常!", e);
		}
		finally
		{
			IOUtils.closeQuietly(raf);
		}
	}

	/**
	 * 解析加密的投标文件<br/>
	 * <p>
	 * 必须是stbx
	 * </p>
	 * 
	 * @param stbx
	 *            投标文件
	 * @param tempDir
	 *            临时文件目录
	 * @return 
	 *         解析后的内容,TB_FILE_JSON:JSON内容,V_EBF_FILE_PATH:加密块文件File对象,ALGORITHM_TYPE
	 *         :加密算法类型
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> parseBidderDocument(File stbx,
			File tempDir) throws ServiceException
	{
		logger.debug(LogUtils.format("解析加密的投标文件,理出非对称加密内容,以及真正的文件加密块", stbx,
				tempDir));
		if (!stbx.exists())
		{
			logger.error(LogUtils.format("投标文件不存在!", stbx, tempDir));
			throw new ServiceException("", "投标文件不存在!");
		}
		// 获取文件扩展名
		String extName = FilenameUtils.getExtension(stbx.getName());
		// 如果扩展名不对
		if (!StringUtils.endsWithIgnoreCase(extName, BIDDER_DOC_EXT_NAME))
		{
			logger.error(LogUtils.format("无效的投标文件扩展名!", stbx, tempDir));
			throw new ServiceException("", "无效的投标文件!");
		}

		RandomAccessFile raf = null;
		try
		{
			Record<String, Object> result = new RecordImpl<String, Object>();
			// 临时文件目录
			if (!tempDir.exists())
			{
				tempDir.mkdirs();
			}
			logger.debug(LogUtils.format("临时文件夹是否可读", tempDir.canRead()));
			logger.debug(LogUtils.format("临时文件夹是否可写", tempDir.canWrite()));
			logger.debug(LogUtils.format("临时文件夹是否可执行", tempDir.canExecute()));
			logger.debug(LogUtils.format("投标文件是否可读", stbx.canRead()));
			logger.debug(LogUtils.format("投标文件是否可写", stbx.canWrite()));
			logger.debug(LogUtils.format("投标文件是否可执行", stbx.canExecute()));
			logger.debug(LogUtils.format("开始创建随机文件输入流", stbx));
			raf = new RandomAccessFile(stbx, "r");
			logger.debug(LogUtils.format("随机文件输入流创建完成", raf));
			// ----------------------处理前半段-----------------------------------
			logger.debug(LogUtils.format("解析对称加密算法"));
			// 第一块的总长度
			int firstBlockLength = 2;
			// 第一块的字符串
			byte[] bo1 = new byte[2];
			// 读取第一块
			int l = raf.read(bo1);
			if (l != firstBlockLength)
			{
				logger.error(LogUtils.format("读取加密算法部分失败,读取到的长度不对!"));
				throw new ServiceException("", "无效的投标文件!");
			}
			// 加密算法类型
			int algorithmType = Integer.valueOf(new String(bo1,
					ConstantEOKB.DEFAULT_CHARSET));
			logger.debug(LogUtils.format("对称加密算法类型", algorithmType));
			result.setColumn("ALGORITHM_TYPE", algorithmType);
			// ----------------------------------------------------
			// 读取B段的长度
			logger.debug(LogUtils.format("解析JSON字符串长度"));
			int jsonLength = 10;
			byte[] jb = new byte[10];
			// 读取第二块
			l = raf.read(jb);
			if (l != jsonLength)
			{
				logger.error(LogUtils.format("读取JSON字符串长度部分失败,读取到的长度不对!"));
				throw new ServiceException("", "无效的投标文件!");
			}
			int jsonDataLength = Integer.valueOf(new String(jb,
					ConstantEOKB.DEFAULT_CHARSET));
			logger.debug(LogUtils.format("JSON字符串长度", jsonDataLength));
			// ------------------------------------------------------
			logger.debug(LogUtils.format("解析JSON字符串"));
			byte[] jsb = new byte[jsonDataLength];
			l = raf.read(jsb);
			if (l != jsonDataLength)
			{
				logger.error(LogUtils.format("读取JSON字符串部分失败,读取到的长度不对!"));
				throw new ServiceException("", "无效的投标文件!");
			}
			// 加密前的JSON
			String ecodeJson = new String(jsb, ConstantEOKB.DEFAULT_CHARSET);
			// 使用默认密码解密
			String json = decrypt(ecodeJson, algorithmType, DEFAULT_PWD);
			logger.debug(LogUtils.format("JSON字符串内容", json));
			// 解析成JSON
			JSONObject tbInfo = JSON.parseObject(json);
			// 获取DATA Base64字符串
			String data = tbInfo.getString("DATA");
			byte[] temp = Base64.decodeBase64(data);
			// 获取真实的JSON字符串
			data = new String(temp, ConstantEOKB.DEFAULT_CHARSET);
			logger.debug(LogUtils.format("真实的DATA JSON字符串内容为:", data));
			JSONObject jobj = JSON.parseObject(data);
			// 将DATA 这个put进来
			tbInfo.putAll(jobj);
			result.setColumn("TB_FILE_JSON", tbInfo);
			// ------------------------------------------------
			logger.debug(LogUtils.format("保存临时文件"));
			// 实际的加密部分内容
			File efbx = new File(tempDir, FilenameUtils.getBaseName(stbx
					.getName()) + ENCRYPTED_FILE_BLOCK_EXT);
			// 实际内容开始位置
			long start = 12 + jsonDataLength;
			// 保存临时文件
			saveTempFile(raf, efbx, start);
			result.setColumn("V_EFB_FILE_PATH",
					FilenameUtils.separatorsToUnix(efbx.getAbsolutePath()));
			return result;
		}
		catch (ServiceException e)
		{
			logger.error(LogUtils.format("解析投标文件发生异常!", stbx, tempDir), e);
			throw e;
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("解析投标文件发生异常!", stbx, tempDir), e);
			throw new ServiceException("", "解析投标文件发生异常!", e);
		}
		finally
		{
			IOUtils.closeQuietly(raf);
		}
	}

	/**
	 * 保存临时文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param raf
	 * @param temp
	 * @param start
	 * @param end
	 * @throws IOException
	 */
	private static void saveTempFile(RandomAccessFile raf, File temp, long start)
			throws IOException
	{
		logger.debug(LogUtils.format("保存加密块", temp, start));
		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(temp);
			raf.seek(start);
			// 缓冲区
			byte[] buffer = new byte[BUFF_SIZE];
			// 读满缓冲区
			int n = raf.read(buffer, 0, buffer.length);
			long rl = 0;
			while (-1 != n)
			{
				rl += n;
				// 写出
				out.write(buffer, 0, n);
				n = raf.read(buffer, 0, buffer.length);
			}
			logger.debug(LogUtils.format("保存加密块总长度", rl));
			out.flush();
			IOUtils.closeQuietly(out);
		}
		finally
		{
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * 解密.efbx文件,即解密临时文件获取真实的ZIP文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param efbx
	 *            机密临时文件
	 * @param algorithmType
	 *            对称加密算法类型
	 * @param pwd
	 *            明文密码
	 * @param outDir
	 *            输出目录
	 * @return 解密临时文件耗时,单位毫秒
	 * @throws ServiceException
	 *             服务异常
	 */
	public static long decryptEfbxFile(File efbx, int algorithmType,
			String pwd, File outDir) throws ServiceException
	{
		long start = System.currentTimeMillis();
		//
		String transformation = null;
		// 密钥
		Key key = null;
		try
		{
			// 如果是des
			if (algorithmType == DES_ALGORITHM_TYPE)
			{
				key = SecretKeyUtils.generateDESKey(pwd);
				transformation = DES_TRANSFORMATION;
			}
			else if (algorithmType == THREEDES_ALGORITHM_TYPE)
			{
				key = SecretKeyUtils.get3DESKey(pwd);
				transformation = THREEDES_TRANSFORMATION;
			}
			else if (algorithmType == AES_ALGORITHM_TYPE)
			{
				key = SecretKeyUtils.getAESKey(pwd);
				transformation = AES_TRANSFORMATION;
			}
			else
			{
				throw new ServiceException("", "不支持的对称加密算法!");
			}
			Cipher cipher = Cipher.getInstance(transformation);
			cipher.init(Cipher.DECRYPT_MODE, key);
			if (!outDir.exists())
			{
				outDir.mkdirs();
			}
			// zip包存放路径
			File zipFolder = new File(outDir, "ZipFolder");
			if (!zipFolder.exists())
			{
				zipFolder.mkdirs();
			}
			// zip zip文件路径
			File zip = new File(zipFolder, FilenameUtils.getBaseName(efbx
					.getName()) + ".zip");
			JDKEncryptDecryptUtils.decrypt(efbx, zip, cipher);
		}
		catch (ServiceException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new ServiceException("", "解密临时文件时发生异常!", e);
		}
		long end = System.currentTimeMillis();
		return end - start;
	}

	/**
	 * 解密字符串<br/>
	 * 
	 * @param ciphertext
	 *            要解密Base64字符串
	 * @param algorithmType
	 *            对称加密算法类型
	 * @param pwd
	 *            明文密码
	 * @return 明文
	 * @throws ServiceException
	 *             服务异常
	 */
	public static String decrypt(String ciphertext, int algorithmType,
			String pwd) throws ServiceException
	{
		//
		String transformation = null;
		// 密钥
		Key key = null;
		try
		{
			// 如果是des
			if (algorithmType == DES_ALGORITHM_TYPE)
			{
				key = SecretKeyUtils.generateDESKey(pwd);
				transformation = DES_TRANSFORMATION;
			}
			else if (algorithmType == THREEDES_ALGORITHM_TYPE)
			{
				key = SecretKeyUtils.get3DESKey(pwd);
				transformation = THREEDES_TRANSFORMATION;
			}
			else if (algorithmType == AES_ALGORITHM_TYPE)
			{
				key = SecretKeyUtils.getAESKey(pwd);
				transformation = AES_TRANSFORMATION;
			}
			else
			{
				throw new ServiceException("", "不支持的对称加密算法!");
			}
			Cipher cipher = Cipher.getInstance(transformation);
			cipher.init(Cipher.DECRYPT_MODE, key);
			return JDKEncryptDecryptUtils.decrypt(ciphertext, cipher);
		}
		catch (ServiceException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new ServiceException("", "解密投标信息摘要时发生异常!", e);
		}
	}

	/**
	 * 解压投标文件<br/>
	 * 
	 * @param zip
	 *            投标文件Zip文件
	 * @param outDir
	 *            解压输出目录
	 * @param useCase
	 *            投标文件类型
	 * @return 耗时
	 * @throws ServiceException
	 *             服务异常
	 */
	public static long unpackBidderDocumentFile(File zip, File outDir,
			int useCase) throws ServiceException
	{
		long start = System.currentTimeMillis();
		// zip包存放路径
		File zipFolder = new File(outDir, "ZipFolder");
		// zip包解压路径
		File unZipDir = new File(zipFolder, FilenameUtils.getBaseName(zip
				.getName()));
		if (!unZipDir.exists())
		{
			unZipDir.mkdirs();
		}

		try
		{
			// 解压内容
			ArchiverUtils.unZip(zip, unZipDir);
			// 目录描述文件
			File src = new File(unZipDir, "投标文件目录描述信息.json");
			if (!src.exists())
			{
				throw new ServiceException("", "在投标文件中无法获取到目录描述信息文件!");
			}

			// 特殊处理信封标识
			if (useCase < 0)
			{
				useCase = 0;
			}
			File catalog = new File(zipFolder, useCase + "-目录描述.json");
			// 拷贝文件
			FileUtils.copyFile(src, catalog);
			// 解析文件路径
			parseBidderDocFilePath(catalog, unZipDir);
		}
		catch (IOException e)
		{
			throw new ServiceException("", "解压ZIP文件时发生异常!", e);
		}
		long end = System.currentTimeMillis();
		return end - start;
	}

	/**
	 * 解析投标文件描述路径<br/>
	 * <p>
	 * </p>
	 * 
	 * @param jsonFile
	 * @param root
	 * @throws IOException
	 */
	private static void parseBidderDocFilePath(File jsonFile, File root)
			throws IOException
	{
		// 读取文件内容
		String json = FileUtils.readFileToString(jsonFile,
				ConstantEOKB.DEFAULT_CHARSET);
		JSONArray elements = JSON.parseArray(json);
		paraCatalogDescJson(elements, root);
		FileUtils.writeStringToFile(jsonFile, elements.toJSONString(),
				ConstantEOKB.DEFAULT_CHARSET);
	}

	/**
	 * 递归解析目录描述JSON<br/>
	 * <p>
	 * </p>
	 * 
	 * @param elements
	 * @param root
	 */
	private static void paraCatalogDescJson(JSONArray elements, File root)
	{
		JSONObject jobj = null;
		File temp = null;
		String path = null;
		JSONArray children = null;
		for (int i = 0; i < elements.size(); i++)
		{
			jobj = elements.getJSONObject(i);
			path = jobj.getString("PATH");
			if (StringUtils.isNotEmpty(path))
			{
				temp = new File(root, path);
				jobj.put("PATH",
						FilenameUtils.separatorsToUnix(temp.getAbsolutePath()));
			}
			// 如果子节点存在
			children = jobj.getJSONArray("CHILDREN");
			if (null != children && !children.isEmpty())
			{
				paraCatalogDescJson(children, root);
			}
		}
	}

	// public static void main(String[] args)
	// {
	// try
	// {
	// long start = System.currentTimeMillis();
	// // Record<String, Object> record = parseBidderDocument(new File(
	// // "I:/最新最新.stbx"), new File("I:/stbx_test"));
	// Record<String, Object> record = getBidderDocumentJsonInfo(new File(
	// "I:/最新最新.stbx"));
	// long end = System.currentTimeMillis();
	// System.err.println("解析投标文件耗时：" + (end - start) + "毫秒");
	// System.err.println(record);
	//
	// // 解密算法
	// int algorithmType = record.getInteger("ALGORITHM_TYPE");
	// // 信息
	// JSONObject jobj = record.getJSONObject("TB_FILE_JSON");
	// // 获取JSON
	// JSONArray pwds = jobj.getJSONArray("PWDS");
	//
	// System.err.println(algorithmType);
	// System.err.println(pwds);
	//
	// String pwd = "614990d08e6106dbb4249de0461f9a63";
	//
	// // 投标信息摘要密文
	// String ciphertext = jobj.getString("ELEMENT");
	// System.err.println(ciphertext);
	// String element = decrypt(ciphertext, algorithmType, pwd);
	// System.err.println("投标信息摘要明文:" + element);
	//
	// File efbx = getEfbxTempFile(new File("I:/最新最新.stbx"), new File(
	// "I:/stbx_test"));
	//
	// // 解密
	// long time = decryptEfbxFile(efbx, algorithmType, pwd, new File(
	// "I:/stbx_test/out"));
	// System.err.println("解密投标文件耗时：" + time + "毫秒");
	//
	// // 投标文件类型
	// int useCase = jobj.getIntValue("USE_CASE");
	// System.err.println("投标文件类型：" + useCase);
	// time = unpackBidderDocumentFile(new File(
	// "I:/stbx_test/out/最新最新.zip"), new File("I:/stbx_test/out"),
	// useCase);
	// System.err.println("解压投标文件耗时：" + time + "毫秒");
	//
	// //
	// // // //
	// // //
	// //
	// ---------------------------------------------------------------------------
	// // start = System.currentTimeMillis();
	// // decryptAndUnpackBidderDocument(new File("I:/投标文件测试/投标文件A.efb"),
	// // "ac5608-f751-4327-bd1f-3ae53f4559", new File(
	// // "I:/投标文件测试/正式目录"), new File("I:/投标文件测试/temp"),
	// // "113", "");
	// // end = System.currentTimeMillis();
	// // System.err.println("解密投标文件耗时：" + (end - start) + "毫秒");
	//
	// }
	// catch (ServiceException e)
	// {
	// }
	// }

}
