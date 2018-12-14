/**
 * 包名：com.sozone.eokb.mobile.common
 * 文件名：MobileUtils.java<br/>
 * 创建时间：2018-11-15 上午9:54:48<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.common;

import java.security.Key;
import java.util.Date;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import com.sozone.aeolus.common.utils.JDKEncryptDecryptUtils;
import com.sozone.aeolus.common.utils.SecretKeyUtils;
import com.sozone.aeolus.exception.ServiceException;

/**
 * 手机工具类<br/>
 * <p>
 * 手机工具类<br/>
 * </p>
 * Time：2018-11-15 上午9:54:48<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MobileUtils
{

	/**
	 * Token 有效时间30天
	 */
	public static final long TOKEN_EXPIRY_DATE = 1000L * 60L * 60L * 24L * 30L;

	/**
	 * 对称加密算法密码
	 */
	private static final String DEFAULT_PWD = DigestUtils.md5Hex("sz-eokb.com")
			.toUpperCase();

	private MobileUtils()
	{
	}

	/**
	 * 加密Token<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tk
	 *            token 明文
	 * @return token 密文
	 * @throws ServiceException
	 *             服务异常
	 */
	public static String encryptToken(String tk) throws ServiceException
	{
		try
		{
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			Key key = SecretKeyUtils.getAESKey(DEFAULT_PWD);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return JDKEncryptDecryptUtils.encrypt(tk, cipher);
		}
		catch (Exception e)
		{
			throw new ServiceException("", "加密Token时发生异常!", e);
		}
	}

	/**
	 * 解密Token<br/>
	 * <p>
	 * </p>
	 * 
	 * @param token
	 *            token密文
	 * @return Token明文
	 * @throws ServiceException
	 *             服务异常
	 */
	public static String decryptToken(String token) throws ServiceException
	{
		try
		{
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			Key key = SecretKeyUtils.getAESKey(DEFAULT_PWD);
			cipher.init(Cipher.DECRYPT_MODE, key);
			return JDKEncryptDecryptUtils.decrypt(token, cipher);
		}
		catch (Exception e)
		{
			throw new ServiceException("", "加密Token时发生异常!", e);
		}
	}

	public static void main(String[] args)
	{
		// 生成一个测试的认证Token
		String userID = "539d39fc42ce30fe9be3bc4b1675cd96";
		// 获取当前时间戳
		String time = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
		StringBuilder tokensb = new StringBuilder();
		tokensb.append(userID).append("|").append(time);
		// 生成Token
		try
		{
			String token = MobileUtils.encryptToken(tokensb.toString());
			token = Base64
					.encodeBase64URLSafeString(Base64.decodeBase64(token));
			System.out.println("TOKEN:\t" + token);
		}
		catch (ServiceException e)
		{
			e.printStackTrace();
		}
	}

}
