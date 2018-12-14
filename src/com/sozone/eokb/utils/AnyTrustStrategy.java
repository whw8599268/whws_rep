package com.sozone.eokb.utils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.ssl.TrustStrategy;

/**
 * 
 * HTTP<br/>
 * <p>
 * HTTP<br/>
 * </p>
 * Time：2016年12月5日 下午3:32:39<br/>
 * 
 * @author lhj
 * @version 1.0.0
 * @since 1.0.0
 */
public class AnyTrustStrategy implements TrustStrategy
{

	@Override
	public boolean isTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException
	{
		return true;
	}

}
