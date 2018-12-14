/**
 * 包名：com.sozone.eokb.common.bus.decrypt
 * 文件名：DecryptBidder.java<br/>
 * 创建时间：2017-8-27 下午05:56:51<br/>
 * 创建者：huangym<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.security.Key;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.common.utils.JDKEncryptDecryptUtils;
import com.sozone.aeolus.common.utils.SecretKeyUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.utils.ArchiverUtils;

/**
 * 解密文件，投标人自行解密<br/>
 * <p>
 * 解密文件，投标人自行解密<br/>
 * </p>
 * Time：2017-8-27 下午05:56:51<br/>
 * 
 * @author huangym
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
public class DecryptBidder
{

	/**
	 * 缓冲区大小
	 */
	private static final int BUFF_SIZE = 4096;

	/**
	 * 字符集
	 */
	private static final String CHARSET_NAME = "GBK";

	/**
	 * 分隔符
	 */
	private static final String SEPARATOR_ONE = "#####";

	/**
	 * 分隔符
	 */
	private static final String SEPARATOR_TWO = "##";

	/**
	 * 分隔符
	 */
	private static final String SEPARATOR_THREE = ":";

	/**
	 * 要替换的字符
	 */
	private static final String REPLACE_ONE = "~";

	/**
	 * 要替换的字符
	 */
	private static final String REPLACE_TWO = ":";

	/**
	 * 软证加密字符串
	 */
	private static final String PWD_KEY = "PWDX";

	/**
	 * 加密算法模式
	 */
	private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

	/**
	 * 加密后的文件块后缀
	 */
	private static final String ENCRYPTED_FILE_BLOCK_EXT = ".efb";

	/**
	 * 解密后的文件块后缀
	 */
	private static final String DECRYPTED_FILE_BLOCK_EXT = ".dfb";

	/**
	 * 数组信封
	 */
	// private static final String[] DIR_NAMES = new String[] { "第一数字信封",
	// "第二数字信封" };

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(DecryptBidder.class);

	/**
	 * 解密投标文件<br/>
	 * <p>
	 * 必须是.efb文件
	 * </p>
	 * 
	 * @param efb
	 *            加密后的文件块
	 * @param pwd
	 *            对称加密算法密码,必须是32位的
	 * @return 解密后的临时文件
	 * @throws ServiceException
	 *             服务异常
	 */
	private static File decryptBidderDocument(File efb, String pwd)
			throws ServiceException
	{
		if (null == efb || !efb.exists())
		{
			throw new ServiceException("", "要解密的文件不存在!");
		}
		if (!StringUtils.equals("efb",
				FilenameUtils.getExtension(efb.getName())))
		{
			throw new ServiceException("", "不支持的文件,无法完成解密动作!");
		}
		try
		{
			logger.debug(LogUtils.format("使用对称加解密算法解密文件块", efb, TRANSFORMATION,
					pwd));
			// 获取父目录
			File dir = efb.getParentFile();
			// 解密后的文件块
			File dfb = new File(dir, FilenameUtils.getBaseName(efb.getName())
					+ DECRYPTED_FILE_BLOCK_EXT);
			Key key = SecretKeyUtils.getAESKey(pwd);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key);
			JDKEncryptDecryptUtils.decrypt(efb, dfb, cipher);
			return dfb;
		}
		catch (Exception e)
		{
			throw new ServiceException("", "解密投标文件发生异常!", e);
		}
	}

	/**
	 * 解析解密后的文件块<br/>
	 * <p>
	 * </p>
	 * 
	 * @param dfb
	 *            解密后的文件块
	 * @param targetDir
	 *            目标文件目录
	 * @param tempDir
	 *            临时文件目录
	 * @return 一个比较特殊的zip文件
	 * @throws ServiceException
	 *             服务异常
	 */
	private static File parseDecryptedFileBlock(File dfb, File targetDir,
			File tempDir) throws ServiceException
	{
		logger.debug(LogUtils.format("解析解密后的文件块", dfb, targetDir, tempDir));
		RandomAccessFile raf = null;
		FileOutputStream out = null;
		try
		{
			// 临时文件目录
			if (!tempDir.exists())
			{
				tempDir.mkdirs();
			}

			// 创建PublicInfomation目录
			File publicinfomation = new File(targetDir, "PublicInfomation");
			if (!publicinfomation.exists())
			{
				publicinfomation.mkdirs();
			}

			File zipx = new File(tempDir, FilenameUtils.getBaseName(dfb
					.getName()) + ".zipx");
			raf = new RandomAccessFile(dfb, "r");
			// ----------------------处理前半段-----------------------------------
			logger.debug(LogUtils.format("解析前半段"));
			// 第一块的总长度
			int firstBlockLength = 2;
			// 第一块的字符串
			byte[] bo1 = new byte[2];
			// 读取第一块
			raf.read(bo1);
			// 获取第二块的长度
			int bo2Length = Integer.valueOf(new String(bo1, CHARSET_NAME));
			logger.debug(LogUtils.format("解析前半段--第二块长度", bo2Length));
			firstBlockLength += bo2Length;
			byte[] bo2 = new byte[bo2Length];
			// 读取第二块
			raf.read(bo2);
			// 读取XML长度
			int xmlLength = Integer.valueOf(new String(bo2, CHARSET_NAME));
			logger.debug(LogUtils.format("解析前半段--PublicInfomation.xml长度",
					xmlLength));
			firstBlockLength += xmlLength;
			logger.debug(LogUtils.format("解析前半段总长度", firstBlockLength));
			// PublicInfomation.xml,特别注意这里的字符集是UTF-8不是GBK
			byte[] xmldata = new byte[xmlLength];
			raf.read(xmldata);
			String xml = new String(xmldata, ConstantEOKB.DEFAULT_CHARSET);
			File xmlFile = new File(publicinfomation,
					FilenameUtils.getBaseName(dfb.getName()) + ".xml");
			logger.debug(LogUtils.format("解析前半段--写入PublicInfomation.xml内容",
					xml, xmlFile));
			FileUtils.write(xmlFile, xml, ConstantEOKB.DEFAULT_CHARSET);

			// ------------------------处理包含乱七八糟头的一个zip文件,该zip文件必须特殊处理之后才能解压-----------------------------------------
			logger.debug(LogUtils.format("解析后半段ZIP"));
			// 直接读取剩下的内容倒置就可以了
			// 跳过一个标记为字符
			firstBlockLength += 1;
			// 索引位置
			long index = dfb.length();
			// 能够读的总长度
			long canRead = dfb.length() - firstBlockLength;
			logger.debug(LogUtils.format("解析后半段ZIP--后半段总长", canRead,
					dfb.length(), firstBlockLength));
			out = new FileOutputStream(zipx);
			// 已读
			long rl = -1;
			int t = -1;
			// 缓冲区
			byte[] buff = new byte[BUFF_SIZE];
			int r = -1;
			while (rl < canRead)
			{
				// 如果能读的总数大于缓冲区
				if (canRead - rl > BUFF_SIZE)
				{
					r = BUFF_SIZE;
				}
				else
				{
					// 否则
					r = (int) (canRead - rl);
				}
				index -= r;
				logger.debug(LogUtils.format("解析后半段ZIP--当前读取开始索引", index, r));
				raf.seek(index);
				t = raf.read(buff, 0, r);
				if (-1 == t)
				{
					break;
				}
				if (t != r)
				{
					throw new IOException("未读取到指定长度的内容,导致内容错乱!");
				}
				// 写入倒置后的结果
				out.write(invertByteArray(buff, 0, t));
				rl += t;
			}

			// 不使用缓冲区的写法,这种方法非常慢
			// for (index -= 1; index >= firstBlockLength; index--)
			// {
			// raf.seek(index);
			// t = raf.read();
			// if (-1 == t)
			// {
			// break;
			// }
			// out.write(t);
			// }
			out.flush();
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(raf);
			return zipx;
		}
		catch (IOException e)
		{
			throw new ServiceException("", "解析投标文件发生异常!", e);
		}
		finally
		{
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(raf);
		}
	}

	/**
	 * 倒置byte数组<br/>
	 * <p>
	 * </p>
	 * 
	 * @param input
	 * @param off
	 * @param len
	 * @return
	 */
	private static byte[] invertByteArray(byte[] input, int off, int len)
	{
		byte[] temp = new byte[len];
		int index = 0;
		for (int i = (off + len - 1); i >= off; i--)
		{
			temp[index] = input[i];
			index++;
		}
		return temp;
	}

	/***
	 * 解析加密的投标文件<br/>
	 * <p>
	 * 把密文和正在的加密文件拆开来
	 * </p>
	 * 
	 * @param bidderDoc
	 *            投标文件
	 * @param tempDir
	 *            临时文件目录
	 * @return 解析后的内容,V_CIPHERTEXT_JSON:密码图,V_EBF_FILE_PATH:加密块文件File对象
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Record<String, Object> parseBidderDocument(File bidderDoc,
			File tempDir) throws ServiceException
	{
		logger.debug(LogUtils.format("解析加密的投标文件,理出非对称加密内容,以及真正的文件加密块",
				bidderDoc, tempDir));
		if (!bidderDoc.exists())
		{
			logger.error(LogUtils.format("投标文件不存在!", bidderDoc, tempDir));
			throw new ServiceException("", "投标文件不存在!");
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
			logger.debug(LogUtils.format("投标文件是否可读", bidderDoc.canRead()));
			logger.debug(LogUtils.format("投标文件是否可写", bidderDoc.canWrite()));
			logger.debug(LogUtils.format("投标文件是否可执行", bidderDoc.canExecute()));
			logger.debug(LogUtils.format("开始创建随机文件输入流", bidderDoc));
			raf = new RandomAccessFile(bidderDoc, "r");
			logger.debug(LogUtils.format("随机文件输入流创建完成", raf));
			// ----------------------处理前半段-----------------------------------
			logger.debug(LogUtils.format("解析前半段"));
			// 第一块的总长度
			int firstBlockLength = 2;
			// 第一块的字符串
			byte[] bo1 = new byte[2];
			// 读取第一块
			raf.read(bo1);
			// 获取第二块的长度
			int bo2Length = Integer.valueOf(new String(bo1, CHARSET_NAME));
			logger.debug(LogUtils.format("解析前半段--第二块长度", bo2Length));
			firstBlockLength += bo2Length;
			byte[] bo2 = new byte[bo2Length];
			// 读取第二块
			raf.read(bo2);
			int pwdStringLength = Integer
					.valueOf(new String(bo2, CHARSET_NAME));
			logger.debug(LogUtils.format("解析前半段--介质证书密文长度", pwdStringLength));
			firstBlockLength += pwdStringLength;
			byte[] pbs = new byte[pwdStringLength];
			raf.read(pbs);
			// 得到文件内容的签名及证书#####证书1：密文##证书2:密文##证书3:密文
			String psc = new String(pbs, CHARSET_NAME);
			logger.debug(LogUtils.format("解析前半段--介质证书密文内容", psc));
			// 这里千万不能用StringUtils.split(psc, SEPARATOR_ONE)
			String[] pscs = psc.split(SEPARATOR_ONE);
			// 如果长度不足,即格式不对
			if (null == pscs || 2 > pscs.length)
			{
				throw new ServiceException("", "无法从投标文件中获取到非对称加密密码!");
			}

			String pwds = pscs[1];
			String[] pwdss = StringUtils.split(pwds, SEPARATOR_TWO);
			Record<String, Object> cerPwds = new RecordImpl<String, Object>();
			String tempKey = null;
			for (String pwd : pwdss)
			{
				String[] ss = StringUtils.split(pwd, SEPARATOR_THREE);
				// 这边的是证书base64里面可能用\r\n
				tempKey = ss[0];
				// 干掉换行符
				tempKey = StringUtils.replace(tempKey, "\r", "");
				tempKey = StringUtils.replace(tempKey, "\n", "");
				// 福建CA这个SB生成东西要特殊处理才能解密
				cerPwds.setColumn(tempKey,
						StringUtils.replace(ss[1], REPLACE_ONE, REPLACE_TWO));
			}
			// ----------------------------处理后半段----------------------------------------
			logger.debug(LogUtils.format("解析后半段XML内容"));
			// 后半段总长度
			int thirdBlockLength = 14;
			byte[] tb1 = new byte[12];
			raf.seek(bidderDoc.length() - 14);
			raf.read(tb1);
			int tb2length = Integer.valueOf(new String(tb1, CHARSET_NAME));
			logger.debug(LogUtils.format("解析后半段XML内容--xml总长度", tb2length));
			thirdBlockLength += tb2length;
			byte[] tb2 = new byte[tb2length];
			raf.seek(bidderDoc.length() - thirdBlockLength);
			raf.read(tb2);
			// 第三段的xml内容
			String xml = new String(tb2, CHARSET_NAME);
			logger.debug(LogUtils.format("解析后半段XML内容--xml内容", xml));
			String json = parseXml(xml);
			logger.debug(LogUtils.format("解析出来的原始json", json));
			JSONArray array = JSON.parseArray(json);
			JSONObject obj;
			for (int i = 0; i < array.size(); i++)
			{
				obj = array.getJSONObject(i);
				cerPwds.setColumn(obj.getString("SERIAL"),
						obj.getString("CIPHERTEXT"));
			}

			// File pwdJson = new File(tempDir,
			// FilenameUtils.getBaseName(bidderDoc.getName()) + ".json");
			// FileUtils.writeStringToFile(pwdJson, JSON.toJSONString(cerPwds),
			// ConstantEOKB.DEFAULT_CHARSET);
			// logger.debug(LogUtils.format("解析后半段XML内容--写入非对称密码JSON", cerPwds,
			// pwdJson));

			Record<String, Object> result = new RecordImpl<String, Object>();
			result.setColumn("V_CIPHERTEXT_JSON", JSON.toJSONString(cerPwds));
			// ----------------------------------解析中间的那一段-------------------------------
			logger.debug(LogUtils.format("文件总长度" + bidderDoc.length() + ",前端长度"
					+ firstBlockLength + ",尾端长度" + thirdBlockLength));
			// 中间块的开始位置
			long start = firstBlockLength;
			// 结束位置
			long end = bidderDoc.length() - thirdBlockLength;
			logger.debug(LogUtils.format("开始掐头去尾取中间的加密文件块", start, end));
			File efb = new File(tempDir, FilenameUtils.getBaseName(bidderDoc
					.getName()) + ENCRYPTED_FILE_BLOCK_EXT);
			saveTempFile(raf, efb, start, end);
			result.setColumn("V_EFB_FILE_PATH",
					FilenameUtils.separatorsToUnix(efb.getAbsolutePath()));
			return result;
		}
		catch (ServiceException e)
		{
			logger.error(LogUtils.format("解析投标文件发生异常!", bidderDoc, tempDir), e);
			throw e;
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("解析投标文件发生异常!", bidderDoc, tempDir), e);
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
	private static void saveTempFile(RandomAccessFile raf, File temp,
			long start, long end) throws IOException
	{
		logger.debug(LogUtils.format("保存加密块", temp, start, end));
		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(temp);
			raf.seek(start);
			// 缓冲区
			byte[] buffer = new byte[BUFF_SIZE];
			// 实际长度
			long length = end - start;
			// 已经读取的长度
			long readLength = 0;
			// 还能够读取的长度
			long canRead = length - readLength;
			int n = 0;
			int len = 0;
			while (0 < canRead)
			{
				// 如果能够读取的总长大于缓冲区
				if (canRead > BUFF_SIZE)
				{
					len = BUFF_SIZE;
				}
				else
				{
					// 如果能够读的总长已经小于缓冲区
					len = (int) canRead;
				}
				n = raf.read(buffer, 0, len);
				if (-1 == n)
				{
					break;
				}
				// 已读
				readLength += n;
				// 还能读
				canRead = length - readLength;
				// 写出
				out.write(buffer, 0, n);
			}
			logger.debug(LogUtils.format("保存加密块总长度", readLength));
			out.flush();
			IOUtils.closeQuietly(out);
		}
		finally
		{
			IOUtils.closeQuietly(out);
		}

	}

	/**
	 * 解析XML<br/>
	 * <p>
	 * </p>
	 * 
	 * @param xmlContent
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private static String parseXml(String xmlContent) throws Exception
	{
		logger.debug(LogUtils.format("读取后半段XML内容"));
		SAXReader sax = new SAXReader();
		Document document = sax.read(new StringReader(xmlContent));
		Element root = document.getRootElement();
		List<Element> infos = root.elements("Info");
		Element id = null;
		Element value = null;
		for (Element info : infos)
		{
			id = info.element("id");
			if (StringUtils.equals(PWD_KEY, id.getText()))
			{
				value = info.element("value");
				String val = value.getText();
				return new String(Base64.decodeBase64(val));
			}
		}
		return "[]";
	}

	/**
	 * 投标制作系统做出来的Zip包不是标准zip包,该文件头部包含一些xml内容,必须把xml内容剔除<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tdz
	 * @param zipDir
	 *            zip文件保存目录
	 * @param unpackDir
	 *            解压目录
	 * @throws Exception
	 */
	private static void unpackBidderDocZip(File tdz, File zipDir, File unpackDir)
			throws Exception
	{
		File zip = new File(zipDir, FilenameUtils.getBaseName(tdz.getName())
				+ ".zip");
		RandomAccessFile raf = null;
		OutputStream os = null;
		try
		{
			// ---------------------------从此处开始干掉xml内容----------------------------
			// 开始创建临时文件
			raf = new RandomAccessFile(tdz, "r");
			os = new BufferedOutputStream(new FileOutputStream(zip));
			byte[] tempByte = new byte[5000];
			raf.read(tempByte);
			String str = bytesToHexString(tempByte);
			int begin = str.indexOf("504B0304") / 2;
			raf.seek(begin);
			byte[] buf = new byte[102400];
			int readLen;
			while ((readLen = raf.read(buf)) > 0)
			{
				os.write(buf, 0, readLen);
			}
			os.flush(); // 此操作必须的
			// 关闭输入输入流
			IOUtils.closeQuietly(raf);
			IOUtils.closeQuietly(os);
			// ------------------------结束-------------------------------
			// 解压
			ArchiverUtils.unZip(zip, unpackDir);
		}
		finally
		{
			IOUtils.closeQuietly(raf);
			IOUtils.closeQuietly(os);
		}
	}

	/**
	 * 把字符数据转换为16进制字符串
	 * 
	 * @param src
	 * @return
	 */
	private static String bytesToHexString(byte[] src)
	{
		StringBuilder stringBuilder = new StringBuilder();
		if (src == null || src.length <= 0)
		{
			return null;
		}
		for (int i = 0; i < src.length; i++)
		{
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2)
			{
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString().toUpperCase();
	}

	/**
	 * 解析信封文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param path
	 *            信封路径
	 * @param index
	 *            信封编号
	 * @throws Exception
	 *             异常
	 */
	private static void parseEnvelopeFile(File path, int index)
			throws Exception
	{

		// 获取目录
		File[] dirs = path.listFiles(new FileFilter()
		{
			/**
			 * 
			 */
			@Override
			public boolean accept(File path)
			{
				String fileName = path.getName();
				if (StringUtils.equals("ScalarCheckList", fileName))
				{
					return true;
				}
				// 如果是其他目录
				File tviXml = new File(path, "TreeViewInfo.xml");
				return tviXml.exists();
			}
		});
		List<Record<String, Object>> fileInfo = new LinkedList<Record<String, Object>>();
		List<Record<String, Object>> fileList = null;
		for (File dir : dirs)
		{
			String fileName = dir.getName();
			// 如果是工程量清单目录
			if (StringUtils.equals("ScalarCheckList", fileName))
			{
				Record<String, Object> record = getScalarCheckFile(dir);
				if (!CollectionUtils.isEmpty(record))
				{
					fileInfo.add(record);
				}
				continue;
			}
			File tviXml = new File(dir, "TreeViewInfo.xml");
			// 解析TreeViewInfo.xml
			fileList = BidderDocumentCatalogUtils.readTreeViewXml(tviXml);
			fileInfo.addAll(fileList);
		}
		FileUtils.write(new File(path.getParentFile(), index + "-目录描述.json"),
				JSON.toJSONString(fileInfo), "UTF-8");
	}

	/**
	 * 解析工程量清单<br/>
	 * <p>
	 * </p>
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	private static Record<String, Object> getScalarCheckFile(File path)
			throws Exception
	{
		// File zip = new File(path, "ScalarCheckList");
		// if (!zip.exists())
		// {
		// return null;
		// }
		File[] flist = path.listFiles();
		if (null == flist || 1 > flist.length)
		{
			return null;
		}
		Record<String, Object> node = new RecordImpl<String, Object>();
		String id = Random.generateUUID();
		node.setColumn("ID", id);
		node.setColumn("NAME", "工程量清单");
		node.setColumn("TYPE", "dir");
		List<Record<String, Object>> children = new LinkedList<Record<String, Object>>();
		node.setColumn("CHILDREN", children);
		Record<String, Object> c = null;
		for (File f : flist)
		{
			c = new RecordImpl<String, Object>();
			c.setColumn("ID", Random.generateUUID());
			c.setColumn("NAME", FilenameUtils.getBaseName(f.getName()));
			c.setColumn("TYPE", "file");
			c.setColumn("PATH",
					FilenameUtils.separatorsToUnix(f.getAbsolutePath()));
			children.add(c);
		}
		return node;
	}

	/**
	 * 解密解压投标文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param efb
	 *            加密块临时文件
	 * @param pwd
	 *            对称加密密码
	 * @param targetDir
	 *            目标目录
	 * @param tempDir
	 *            临时目录
	 * @param tfid
	 *            文件ID
	 * @param bemCode
	 *            评标办法编码
	 * @throws ServiceException
	 *             服务异常
	 */
	public static void decryptAndUnpackBidderDocument(File efb, String pwd,
			File targetDir, File tempDir, String tfid, String bemCode)
			throws ServiceException
	{
		File dfb = decryptBidderDocument(efb, pwd);
		File zipx = parseDecryptedFileBlock(dfb, targetDir, tempDir);
		// 解压特殊zip
		try
		{
			// zip包存放路径
			File zipFolder = new File(targetDir, "ZipFolder");
			// zip包解压路径
			File unZipDir = new File(zipFolder, FilenameUtils.getBaseName(efb
					.getName()));
			if (!unZipDir.exists())
			{
				unZipDir.mkdirs();
			}
			// 写出zip包,并解压内容
			unpackBidderDocZip(zipx, zipFolder, unZipDir);
			// 这是一个特殊的地方-------------------------------
			int index = 0;
			// 第一信封
			if (StringUtils.equals(ConstantEOKB.FIRST_ENVELOPE_TAG, tfid)
					|| StringUtils.equals(
							ConstantEOKB.SHARE_FIRST_ENVELOPE_TAG, tfid)
					|| StringUtils.equals(ConstantEOKB.BIDDER_DOC_ENVELOPE_TAG,
							tfid)
					|| StringUtils.equals(ConstantEOKB.XM_FJSZ_BIDDER_DOC_TAG,
							tfid))
			{
				index = 0;
				parseEnvelopeFile(unZipDir, index);
				return;
			}
			// 第二信封
			if (StringUtils.equals(ConstantEOKB.SECOND_ENVELOPE_TAG, tfid))
			{
				index = 1;
				parseEnvelopeFile(unZipDir, index);
				return;
			}
			// 如果是第三信封
			if (StringUtils.equals(ConstantEOKB.THIRD_ENVELOPE_TAG, tfid))
			{
				index = 2;
				parseEnvelopeFile(unZipDir, index);
				return;
			}

		}
		catch (Exception e)
		{
			throw new ServiceException("", "解压投标文件失败!", e);
		}
	}

	// -------------------------------三层解密解压专用------------------------------------------

	/**
	 * 解密并解析临时文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param efb
	 *            加密块临时文件
	 * @param pwd
	 *            对称加密密码
	 * @param targetDir
	 *            目标目录
	 * @param tempDir
	 *            临时目录
	 * @return ZIPX文件全路径
	 * @throws ServiceException
	 *             服务异常
	 */
	public static String decryptAndParseTempFile(File efb, String pwd,
			File targetDir, File tempDir) throws ServiceException
	{
		File dfb = decryptBidderDocument(efb, pwd);
		File zipx = parseDecryptedFileBlock(dfb, targetDir, tempDir);
		return FilenameUtils.separatorsToUnix(zipx.getAbsolutePath());
	}

	/**
	 * 解压Zipx文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tfid
	 *            文件ID
	 * @param zipx
	 *            Zipx文件
	 * @param targetDir
	 *            目标
	 * @throws ServiceException
	 */
	public static void unpackZipxFile(String tfid, File zipx, File targetDir)
			throws ServiceException
	{
		// 解压特殊zip
		try
		{
			// zip包存放路径
			File zipFolder = new File(targetDir, "ZipFolder");
			// zip包解压路径
			File unZipDir = new File(zipFolder, FilenameUtils.getBaseName(zipx
					.getName()));
			if (!unZipDir.exists())
			{
				unZipDir.mkdirs();
			}
			// 写出zip包,并解压内容
			unpackBidderDocZip(zipx, zipFolder, unZipDir);

			// 这是一个特殊的地方-------------------------------
			int index = 0;
			// 目前房建市政只有一个信封
			parseEnvelopeFile(unZipDir, index);
			// // 第一信封
			// if (StringUtils.equals(ConstantEOKB.FIRST_ENVELOPE_TAG, tfid)
			// || StringUtils.equals(
			// ConstantEOKB.SHARE_FIRST_ENVELOPE_TAG, tfid)
			// || StringUtils.equals(ConstantEOKB.BIDDER_DOC_ENVELOPE_TAG,
			// tfid)
			// || StringUtils.equals(ConstantEOKB.XM_FJSZ_BIDDER_DOC_TAG,
			// tfid))
			// {
			// index = 0;
			// parseEnvelopeFile(unZipDir, index);
			// return;
			// }
			// // 第二信封
			// if (StringUtils.equals(ConstantEOKB.SECOND_ENVELOPE_TAG, tfid))
			// {
			// index = 1;
			// parseEnvelopeFile(unZipDir, index);
			// return;
			// }
			// // 如果是第三信封
			// if (StringUtils.equals(ConstantEOKB.THIRD_ENVELOPE_TAG, tfid))
			// {
			// index = 2;
			// parseEnvelopeFile(unZipDir, index);
			// return;
			// }
		}
		catch (Exception e)
		{
			throw new ServiceException("", "解压投标文件失败!", e);
		}
	}

	public static void main(String[] args)
	{
		try
		{
			long start = System.currentTimeMillis();
			Record<String, Object> record = parseBidderDocument(new File(
					"I:/2/第二数字信封E.stb"), new File("I:/2/投标文件测试"));
			long end = System.currentTimeMillis();
			System.out.println("解析投标文件耗时：" + (end - start) + "毫秒");
			System.out.println(record);
			//
			// // //
			// //
			// ---------------------------------------------------------------------------
			// start = System.currentTimeMillis();
			// decryptAndUnpackBidderDocument(new File("I:/投标文件测试/投标文件A.efb"),
			// "ac5608-f751-4327-bd1f-3ae53f4559", new File(
			// "I:/投标文件测试/正式目录"), new File("I:/投标文件测试/temp"),
			// "113", "");
			// end = System.currentTimeMillis();
			// System.out.println("解密投标文件耗时：" + (end - start) + "毫秒");

		}
		catch (ServiceException e)
		{
			e.printStackTrace();
		}
	}

}
