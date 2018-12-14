/**
 * 包名：com.sozone.eokb.bus.decrypt.action
 * 文件名：SozoneCipher.java<br/>
 * 创建时间：2018-10-16 上午11:55:18<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt.action;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.common.utils.CertificateUtils;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.ClassLoaderUtils;

/**
 * 工具类<br/>
 * <p>
 * 工具类<br/>
 * </p>
 * Time：2018-10-16 上午11:55:18<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SozoneCipher
{

	/**
	 * zhi
	 */
	private static final String KEY = "moc.pako.www";

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(SozoneCipher.class);

	private static KeyStore root = null;

	private static X509Certificate rootCer = null;

	private static PrivateKey privateKey = null;

	private static String p()
	{
		StringBuilder sb = new StringBuilder();
		char[] t = KEY.toCharArray();
		for (int i = t.length - 1; i >= 0; i--)
		{
			sb.append(t[i]);
		}
		return sb.toString();
	}

	static
	{
		InputStream input = ClassLoaderUtils.getResourceAsStream(
				"com/sozone/eokb/bus/decrypt/action/eokb.root.com.pfx",
				SozoneCipher.class);
		try
		{
			root = CertificateUtils.getPfx(input, p());
			rootCer = CertificateUtils.getX509Certificate(root);
			privateKey = CertificateUtils.getPrivateKey(root, "111111");
		}
		catch (Exception e)
		{
			logger.debug(LogUtils.format("加载异常"), e);
		}
	}

	/**
	 * <p>
	 * </p>
	 * @param m
	 * @return
	 * @throws ServiceException
	 */
	public static String j(String m) throws ServiceException
	{
		try
		{
			return CertificateUtils.decrypt(rootCer, privateKey, m);
		}
		catch (Exception e)
		{
			logger.debug(LogUtils.format("解密异常", m), e);
			throw new ServiceException("", "解密失败!", e);
		}
	}
}
