/**
 * 包名：com.sozone.ede.rest.token
 * 文件名：TokenUtils.java<br/>
 * 创建时间：2017-7-3 下午1:39:48<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.utils;

import java.text.ParseException;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;

import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;

/**
 * Token 工具类<br/>
 * <p>
 * Token 工具类<br/>
 * </p>
 * Time：2017-7-3 下午1:39:48<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class TokenUtils
{

	/**
	 * 最大有效时间 5分钟
	 */
	private static final long MAX_EXPIRY = 5 * 60 * 1000;

	/**
	 * 生成Token<br/>
	 * <p>
	 * </p>
	 * 
	 * @param userName
	 *            用户名称
	 * @param key
	 *            密钥
	 * @return token值
	 * @throws ServiceException
	 *             服务异常
	 */
	public static String generateToken(String userName, String key)
			throws ServiceException
	{
		StringBuilder sb = new StringBuilder();
		sb.append(userName)
				.append(DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"))
				.append(DigestUtils.md5Hex(userName).toUpperCase());
		try
		{
			Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("UTF-8"));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
			IvParameterSpec iv = new IvParameterSpec(key.getBytes("UTF-8"));
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
			byte[] ss = cipher.doFinal(sb.toString().getBytes("UTF-8"));
			return Base64.encodeBase64String(ss);
		}
		catch (Exception e)
		{
			throw new ServiceException("R-0001", e);
		}
	}

	/**
	 * 解析Token<br/>
	 * <p>
	 * </p>
	 * 
	 * @param token
	 *            Token
	 * @param key
	 *            key
	 * @return 值
	 * @throws ServiceException
	 *             异常
	 */
	public static Record<String, Object> decryptToken(String token, String key)
			throws ServiceException
	{
		String source = null;
		try
		{
			Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("UTF-8"));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
			IvParameterSpec iv = new IvParameterSpec(key.getBytes("UTF-8"));
			cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
			byte[] ss = cipher.doFinal(Base64.decodeBase64(token));
			source = new String(ss);
		}
		catch (Exception e)
		{
			throw new ServiceException("R-0002", e);
		}
		int length = source.length();
		if (length < 46)
		{
			throw new ServiceException("R-0003");
		}
		String md5 = StringUtils.substring(source, length - 32);
		String time = StringUtils.substring(source, length - 46, length - 32);
		validateTime(time);
		String userName = StringUtils.substring(source, 0, length - 46);
		if (!StringUtils
				.equals(md5, DigestUtils.md5Hex(userName).toUpperCase()))
		{
			throw new ServiceException("R-0004");
		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		result.setColumn("PLAT_CODE", userName);
		result.setColumn("PLAT_CODE_MD5", md5);
		result.setColumn("TIME_STAMP", time);
		return result;
	}

	private static void validateTime(String time) throws ServiceException
	{
		Date date = null;
		try
		{
			date = DateUtils.parseDate(time, new String[] { "yyyyMMddHHmmss" });
		}
		catch (ParseException e)
		{
			throw new ServiceException("R-0005");
		}
		long got = date.getTime();
		long now = System.currentTimeMillis();
		if (now - got > MAX_EXPIRY)
		{
			throw new ServiceException("R-0006");
		}
	}

}
