package com.sozone.eokb.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;

/**
 * HTTP 工具类<br/>
 * <p>
 * HTTP 工具类<br/>
 * </p>
 * 
 */
public final class HttpClientUtils
{

	/**
	 * 字符集
	 */
	public static final String CHARSET = "UTF-8";

	/**
	 * 字符集
	 */
	public static final String CHARSET_GBK = "GBK";

	/**
	 * 证书密码 无法获取证书密码是默认使用
	 */
	public static final String SSLPASSWORD = "password";

	private static final Map<String, String> MIME_TYPE = new HashMap<String, String>();

	/**
	 * httpClient实例
	 */
	private static CloseableHttpClient httpClient;

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(HttpClientUtils.class);

	private HttpClientUtils()
	{
	}

	static
	{
		MIME_TYPE.put("audio/mp4a-latm", "m4a");
		MIME_TYPE.put("application/srgs+xml", "grxml");
		MIME_TYPE.put("video/mpeg", "mpg");
		MIME_TYPE.put("text/vnd.wap.wmlscript", "wmls");
		MIME_TYPE.put("image/tiff", "tif");
		MIME_TYPE.put("video/mpeg", "mpe");
		MIME_TYPE.put("video/ogv", "ogv");
		MIME_TYPE.put("application/x-shockwave-flash", "swf");
		MIME_TYPE.put("application/x-shar", "shar");
		MIME_TYPE
				.put("application/vnd.openxmlformats-officedocument.presentationml.presentation",
						"pptx");
		MIME_TYPE.put("video/3gpp", "3gp");
		MIME_TYPE.put("video/webm", "webm");
		MIME_TYPE.put("application/x-futuresplash", "spl");
		MIME_TYPE.put("audio/x-mpegurl", "m3u");
		MIME_TYPE.put("application/vnd.wap.wmlc", "wmlc");
		MIME_TYPE.put("text/vnd.wap.wml", "wml");
		MIME_TYPE.put("application/ogg", "ogg");
		MIME_TYPE.put("audio/mpeg", "mpga");
		MIME_TYPE.put("application/octet-stream", "lha");
		MIME_TYPE.put("image/tiff", "tiff");
		MIME_TYPE.put("audio/midi", "kar");
		MIME_TYPE.put("video/x-ms-wmv", "wmv");
		MIME_TYPE.put("image/vnd.djvu", "djvu");
		MIME_TYPE.put("video/mpeg", "mpeg");
		MIME_TYPE.put("application/x-gzip", "gz");
		MIME_TYPE.put("application/x-texinfo", "texinfo");
		MIME_TYPE.put("image/x-rgb", "rgb");
		MIME_TYPE.put("application/vnd.ms-powerpoint", "pps");
		MIME_TYPE.put("application/vnd.ms-powerpoint", "ppt");
		MIME_TYPE.put("application/x-director", "dxr");
		MIME_TYPE.put("audio/mp4a-latm", "m4p");
		MIME_TYPE.put("video/quicktime", "mov");
		MIME_TYPE.put("text/sgml", "sgm");
		MIME_TYPE.put("image/x-quicktime", "qtif");
		MIME_TYPE.put("text/plain", "asc");
		MIME_TYPE.put("image/x-portable-pixmap", "ppm");
		MIME_TYPE.put("video/x-m4v", "m4v");
		MIME_TYPE.put("video/vnd.mpegurl", "m4u");
		MIME_TYPE.put("text/x-setext", "etx");
		MIME_TYPE.put("image/jp2", "jp2");
		MIME_TYPE.put("application/x-tar", "tar");
		MIME_TYPE.put("application/x-chess-pgn", "pgn");
		MIME_TYPE.put("image/x-portable-graymap", "pgm");
		MIME_TYPE.put("application/octet-stream", "bin");
		MIME_TYPE.put("image/gif", "gif");
		MIME_TYPE.put("application/x-netcdf", "cdf");
		MIME_TYPE.put("text/css", "css");
		MIME_TYPE.put("chemical/x-xyz", "xyz");
		MIME_TYPE.put("video/x-dv", "dv");
		MIME_TYPE.put("application/x-tcl", "tcl");
		MIME_TYPE.put("application/xhtml+xml", "xhtml");
		MIME_TYPE.put("application/octet-stream", "dms");
		MIME_TYPE.put("audio/x-aiff", "aif");
		MIME_TYPE.put("image/x-macpaint", "mac");
		MIME_TYPE.put("image/jpeg", "jpe");
		MIME_TYPE.put("image/pict", "pict");
		MIME_TYPE.put("audio/midi", "midi");
		MIME_TYPE.put("model/iges", "igs");
		MIME_TYPE.put("text/tab-separated-values", "tsv");
		MIME_TYPE.put("application/x-troff-me", "me");
		MIME_TYPE.put("application/x-wais-source", "src");
		MIME_TYPE.put("text/sgml", "sgml");
		MIME_TYPE.put("model/iges", "iges");
		MIME_TYPE.put("application/x-troff-man", "man");
		MIME_TYPE.put("application/vnd.android.package-archive", "apk");
		MIME_TYPE.put("application/xml", "xsl");
		MIME_TYPE.put("application/x-dvi", "dvi");
		MIME_TYPE.put("application/andrew-inset", "ez");
		MIME_TYPE.put("application/x-troff-ms", "ms");
		MIME_TYPE.put("video/x-sgi-movie", "movie");
		MIME_TYPE.put("image/x-macpaint", "pnt");
		MIME_TYPE.put("model/mesh", "silo");
		MIME_TYPE.put("application/x-stuffit", "sit");
		MIME_TYPE.put("image/jpeg", "jpg");
		MIME_TYPE.put("model/vrml", "vrml");
		MIME_TYPE.put("application/x-troff", "t");
		MIME_TYPE.put("image/x-portable-anymap", "pnm");
		MIME_TYPE.put("application/xslt+xml", "xslt");
		MIME_TYPE.put("image/png", "png");
		MIME_TYPE.put("application/x-netcdf", "nc");
		MIME_TYPE.put("application/msword", "doc");
		MIME_TYPE.put("application/x-latex", "latex");
		MIME_TYPE.put("application/vnd.mif", "mif");
		MIME_TYPE.put("audio/midi", "mid");
		MIME_TYPE.put("application/voicexml+xml", "vxml");
		MIME_TYPE.put("application/atom+xml", "atom");
		MIME_TYPE.put("application/octet-stream", "lzh");
		MIME_TYPE.put("application/vnd.ms-excel", "xls");
		MIME_TYPE
				.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
						"xlsx");
		MIME_TYPE.put("image/jpeg", "jpeg");
		MIME_TYPE.put("application/x-troff", "tr");
		MIME_TYPE.put("application/smil", "smil");
		MIME_TYPE.put("application/pdf", "pdf");
		MIME_TYPE.put("application/x-javascript", "js");
		MIME_TYPE.put("application/x-bcpio", "bcpio");
		MIME_TYPE.put("application/x-sv4cpio", "sv4cpio");
		MIME_TYPE.put("application/x-sv4crc", "sv4crc");
		MIME_TYPE.put("image/ief", "ief");
		MIME_TYPE.put("application/mathml+xml", "mathml");
		MIME_TYPE.put("image/vnd.wap.wbmp", "wbmp");
		MIME_TYPE.put("image/pict", "pct");
		MIME_TYPE.put("application/rdf+xml", "rdf");
		MIME_TYPE.put("image/cgm", "cgm");
		MIME_TYPE.put("video/x-msvideo", "avi");
		MIME_TYPE.put("application/xml-dtd", "dtd");
		MIME_TYPE.put("chemical/x-pdb", "pdb");
		MIME_TYPE.put("text/html", "htm");
		MIME_TYPE.put("model/mesh", "mesh");
		MIME_TYPE.put("application/vnd.mozilla.xul+xml", "xul");
		MIME_TYPE.put("application/octet-stream", "dmg");
		MIME_TYPE.put("application/x-koan", "skm");
		MIME_TYPE.put("application/octet-stream", "so");
		MIME_TYPE.put("audio/x-aiff", "aifc");
		MIME_TYPE.put("application/mac-compactpro", "cpt");
		MIME_TYPE.put("application/x-koan", "skp");
		MIME_TYPE.put("application/octet-stream", "class");
		MIME_TYPE.put("application/xml", "xml");
		MIME_TYPE.put("audio/x-aiff", "aiff");
		MIME_TYPE.put("application/x-sh", "sh");
		MIME_TYPE.put("text/rtf", "rtf");
		MIME_TYPE.put("application/x-texinfo", "texi");
		MIME_TYPE.put("application/x-koan", "skt");
		MIME_TYPE.put("model/mesh", "msh");
		MIME_TYPE.put("image/x-xbitmap", "xbm");
		MIME_TYPE.put("image/bmp", "bmp");
		MIME_TYPE.put("application/x-cdlink", "vcd");
		MIME_TYPE.put("application/postscript", "eps");
		MIME_TYPE.put("video/x-flv", "flv");
		MIME_TYPE.put("application/x-tex", "tex");
		MIME_TYPE.put("model/vrml", "wrl");
		MIME_TYPE.put("application/octet-stream", "dll");
		MIME_TYPE.put("image/x-quicktime", "qti");
		MIME_TYPE.put("application/x-koan", "skd");
		MIME_TYPE.put("application/vnd.wap.wmlscriptc", "wmlsc");
		MIME_TYPE.put("application/octet-stream", "exe");
		MIME_TYPE.put("text/calendar", "ifb");
		MIME_TYPE.put("text/richtext", "rtx");
		MIME_TYPE.put("application/oda", "oda");
		MIME_TYPE.put("application/vnd.rn-realmedia", "rm");
		MIME_TYPE.put("text/calendar", "ics");
		MIME_TYPE.put("video/mp4", "mp4");
		MIME_TYPE.put("image/x-icon", "ico");
		MIME_TYPE.put("audio/mpeg", "mp3");
		MIME_TYPE.put("audio/x-wav", "wav");
		MIME_TYPE.put("image/x-portable-bitmap", "pbm");
		MIME_TYPE.put("image/x-cmu-raster", "ras");
		MIME_TYPE.put("image/x-xpixmap", "xpm");
		MIME_TYPE.put("text/plain", "txt");
		MIME_TYPE.put("application/x-java-jnlp-file", "jnlp");
		MIME_TYPE.put("application/x-cpio", "cpio");
		MIME_TYPE.put("audio/x-pn-realaudio", "ra");
		MIME_TYPE.put("application/x-director", "dir");
		MIME_TYPE.put("x-conference/x-cooltalk", "ice");
		MIME_TYPE.put("audio/mpeg", "mp2");
		MIME_TYPE.put("video/x-dv", "dif");
		MIME_TYPE.put("application/srgs", "gram");
		MIME_TYPE.put("application/x-gtar", "gtar");
		MIME_TYPE.put("application/vnd.wap.wbxml", "wbxml");
		MIME_TYPE.put("video/quicktime", "qt");
		MIME_TYPE.put("application/x-troff", "roff");
		MIME_TYPE.put("audio/basic", "snd");
		MIME_TYPE.put("application/zip", "zip");
		MIME_TYPE
				.put("application/vnd.openxmlformats-officedocument.presentationml.slideshow",
						"ppsx");
		MIME_TYPE.put("video/vnd.mpegurl", "mxu");
		MIME_TYPE.put("image/svg+xml", "svg");
		MIME_TYPE.put("image/pict", "pic");
		MIME_TYPE.put("image/vnd.djvu", "djv");
		MIME_TYPE.put("image/x-xwindowdump", "xwd");
		MIME_TYPE.put("image/x-macpaint", "pntg");
		MIME_TYPE.put("audio/basic", "au");
		MIME_TYPE.put("application/x-director", "dcr");
		MIME_TYPE.put("application/postscript", "ai");
		MIME_TYPE.put("audio/x-pn-realaudio", "ram");
		MIME_TYPE.put("application/mac-binhex40", "hqx");
		MIME_TYPE.put("application/x-ustar", "ustar");
		MIME_TYPE.put("application/x-hdf", "hdf");
		MIME_TYPE.put("text/html", "html");
		MIME_TYPE
				.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
						"docx");
		MIME_TYPE.put("application/smil", "smi");
		MIME_TYPE.put("application/x-csh", "csh");
		MIME_TYPE.put("application/xhtml+xml", "xht");
		MIME_TYPE.put("application/postscript", "ps");

		// 配置文件获取httpclient的类型 是 http还是https
		String httpClientType = "HTTP";

		RequestConfig defaultHttpsConfig = RequestConfig
				.custom()
				.setCookieSpec(CookieSpecs.STANDARD_STRICT)
				.setExpectContinueEnabled(true)
				.setTargetPreferredAuthSchemes(
						Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
				.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
				.build();
		// 超时3分钟
		RequestConfig httpConfig = RequestConfig.copy(defaultHttpsConfig)
				.setConnectionRequestTimeout(60000 * 3)
				.setConnectTimeout(60000 * 3).setSocketTimeout(60000 * 3)
				.build();

		// HTTPS
		if (httpClientType != null && "HTTPS".equalsIgnoreCase(httpClientType))
		{

			PoolingHttpClientConnectionManager connManager = null;

			RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder
					.<ConnectionSocketFactory> create();
			FileInputStream serverfis = null;
			FileInputStream trustfis = null;
			try
			{
				// 获得密匙库
				KeyStore serverstore = KeyStore.getInstance("PKCS12");
				KeyStore truststore = KeyStore.getInstance("JKS");
				// 配置文件获取证书根路径
				String path = "F:\\cer";

				// 配置文件获取客户端证书名称
				String server = "custom.p12";
				// 配置文件获取服务端信任证书
				String trust = "custom.truststore";

				// 配置文件获取客户端证书密码
				String serverPassword = "password";
				// 配置文件获取服务端信任证书密码
				String trustPassword = "password";

				// 密码为空
				serverPassword = (serverPassword == null || ""
						.equals(serverPassword)) ? SSLPASSWORD : serverPassword;
				trustPassword = (trustPassword == null || ""
						.equals(trustPassword)) ? SSLPASSWORD : trustPassword;

				File serverfile = new File(path, server);
				serverfis = new FileInputStream(serverfile);

				File trustfile = new File(path, trust);
				trustfis = new FileInputStream(trustfile);

				// 密匙库的密码
				serverstore.load(serverfis, serverPassword.toCharArray());
				truststore.load(trustfis, trustPassword.toCharArray());

				// 关闭流
				serverfis.close();
				trustfis.close();

				// 设置连接
				SSLContext sslContext = SSLContextBuilder
						.create()
						.loadKeyMaterial(serverstore, SSLPASSWORD.toCharArray())
						.loadTrustMaterial(truststore, new AnyTrustStrategy())
						.build();
				LayeredConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(
						sslContext);

				registryBuilder.register("https", sslSF);
				Registry<ConnectionSocketFactory> registry = registryBuilder
						.build();

				connManager = new PoolingHttpClientConnectionManager(registry);
				connManager.setMaxTotal(1000);// 最大连接数
				connManager.setDefaultMaxPerRoute(500);// 最大并行连接数

			}
			catch (Exception e)
			{
				logger.error(LogUtils.format("HttpClientUtils初始化发生异常!"), e);
			}
			finally
			{

				IOUtils.closeQuietly(serverfis);
				IOUtils.closeQuietly(trustfis);
			}

			if (connManager != null)
			{

				httpClient = HttpClients.custom()
						.setConnectionManager(connManager)
						.setDefaultRequestConfig(httpConfig).build();
			}

		}

		// HTTP
		if (httpClient == null)
		{

			RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder
					.<ConnectionSocketFactory> create();
			registryBuilder.register("http",
					PlainConnectionSocketFactory.getSocketFactory());

			PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
					registryBuilder.build());
			connManager.setMaxTotal(1000);// 最大连接数
			connManager.setDefaultMaxPerRoute(500);// 最大并行连接数

			httpClient = HttpClientBuilder.create()
					.setConnectionManager(connManager)
					.setDefaultRequestConfig(httpConfig).build();
		}

	}

	/**
	 * 发送get请求。<br/>
	 * 
	 * @param url
	 *            请求URL
	 * @param params
	 *            参数
	 * @return 结果
	 * @throws Exception
	 *             Exception
	 */
	public static String doGet(String url, Map<String, String> params)
			throws Exception
	{
		return doGet(url, params, CHARSET);
	}

	/**
	 * 发送Post请求<br/>
	 * 
	 * @param url
	 *            请求URL
	 * @param params
	 *            参数
	 * @return 结果
	 * @throws Exception
	 *             Exception
	 */
	public static String doPost(String url, Map<String, String> params)
			throws Exception
	{
		return doPost(url, params, CHARSET);
	}

	/**
	 * HTTP Get 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @param charset
	 *            编码格式
	 * @return 页面内容
	 * @throws Exception
	 *             异常
	 */
	public static String doGet(String url, Map<String, String> params,
			String charset) throws Exception
	{
		return doGet(url, params, null, charset);
	}

	/**
	 * HTTP Get 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @param headMap
	 *            请求头的参数
	 * @param charset
	 *            编码格式
	 * @return 页面内容
	 * @throws Exception
	 *             异常
	 */
	public static String doGet(String url, Map<String, String> params,
			Map<String, String> headMap, String charset) throws Exception
	{
		if (StringUtils.isBlank(url))
		{
			return null;
		}
		CloseableHttpResponse response = null;
		try
		{
			if (params != null && !params.isEmpty())
			{
				List<NameValuePair> pairs = new ArrayList<NameValuePair>(
						params.size());
				for (Map.Entry<String, String> entry : params.entrySet())
				{
					String value = entry.getValue();
					if (value != null)
					{
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
				url += "?"
						+ EntityUtils.toString(new UrlEncodedFormEntity(pairs,
								charset));
			}
			HttpGet httpGet = new HttpGet(url);

			if (null != headMap && !headMap.isEmpty())
			{
				Set<Entry<String, String>> entrySet = headMap.entrySet();
				for (Entry<String, String> entry : entrySet)
				{
					String value = entry.getValue();
					if (value != null)
					{
						httpGet.setHeader(entry.getKey(), value);
					}

				}
			}
			response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200)
			{
				try
				{
					dealError(response, charset);
				}
				finally
				{
					httpGet.abort();
				}
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null)
			{
				result = EntityUtils.toString(entity, charset);
			}
			EntityUtils.consume(entity);

			return result;

		}
		finally
		{
			IOUtils.closeQuietly(response);

		}
	}

	/**
	 * 发送json数据post请求<br/>
	 * 
	 * @param url
	 *            请求的url地址
	 * @param json
	 *            json字符串
	 * @param charset
	 *            编码字符集
	 * @return 响应内容
	 * @throws Exception
	 *             Exception
	 */
	public static String sendJsonPostRequest(String url, String json,
			String charset) throws Exception
	{
		return sendJsonPostRequest(url, json, null, charset);
	}

	/**
	 * 发送json数据post请求<br/>
	 * 
	 * @param url
	 *            请求的url地址
	 * @param json
	 *            json字符串
	 * @param headMap
	 *            请求头参数
	 * @param charset
	 *            编码字符集
	 * @return 响应内容
	 * @throws Exception
	 *             Exception
	 */
	public static String sendJsonPostRequest(String url, String json,
			Map<String, String> headMap, String charset) throws Exception
	{
		if (StringUtils.isBlank(url))
		{
			return null;
		}
		CloseableHttpResponse response = null;
		try
		{
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-type",
					"application/json; charset=utf-8");
			httpPost.setHeader("Accept", "application/json");
			httpPost.setEntity(new StringEntity(json, Charset.forName("UTF-8")));

			if (null != headMap && !headMap.isEmpty())
			{
				Set<Entry<String, String>> entrySet = headMap.entrySet();
				for (Entry<String, String> entry : entrySet)
				{
					String value = entry.getValue();
					if (value != null)
					{
						httpPost.setHeader(entry.getKey(), value);
					}
				}
			}

			response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200)
			{
				try
				{
					dealError(response, charset);
				}
				finally
				{
					httpPost.abort();
				}
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null)
			{
				result = EntityUtils.toString(entity, charset);
			}
			EntityUtils.consume(entity);
			return result;
		}
		finally
		{
			IOUtils.closeQuietly(response);
		}
	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @param charset
	 *            编码格式
	 * @return 页面内容
	 * @throws Exception
	 *             异常
	 */
	public static String doPost(String url, Map<String, String> params,
			String charset) throws Exception
	{
		return doPost(url, params, null, charset);
	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @param headMap
	 *            请求头的参数
	 * @param charset
	 *            编码格式
	 * @return 页面内容
	 * @throws Exception
	 *             异常
	 */
	public static String doPost(String url, Map<String, String> params,
			Map<String, String> headMap, String charset) throws Exception
	{
		if (StringUtils.isBlank(url))
		{
			return null;
		}

		CloseableHttpResponse response = null;

		try
		{
			List<NameValuePair> pairs = null;
			if (params != null && !params.isEmpty())
			{
				pairs = new ArrayList<NameValuePair>(params.size());
				for (Map.Entry<String, String> entry : params.entrySet())
				{
					String value = entry.getValue();
					if (value != null)
					{
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
			}
			HttpPost httpPost = new HttpPost(url);
			if (pairs != null && pairs.size() > 0)
			{
				httpPost.setEntity(new UrlEncodedFormEntity(pairs, charset));
			}

			if (null != headMap && !headMap.isEmpty())
			{
				Set<Entry<String, String>> entrySet = headMap.entrySet();
				for (Entry<String, String> entry : entrySet)
				{
					String value = entry.getValue();
					if (value != null)
					{
						httpPost.setHeader(entry.getKey(), value);
					}

				}
			}

			response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200)
			{
				try
				{
					dealError(response, charset);
				}
				finally
				{
					httpPost.abort();
				}
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null)
			{
				result = EntityUtils.toString(entity, charset);
			}
			EntityUtils.consume(entity);

			return result;

		}
		finally
		{
			IOUtils.closeQuietly(response);
		}
	}

	/**
	 * URL转码<br/>
	 * 
	 * @param baseURL
	 *            基础URL
	 * @param params
	 *            参数图
	 * @param charset
	 *            编码格式
	 * @return 完整URL
	 * @throws Exception
	 *             Exception
	 */
	public static String encodeURL(String baseURL, Map<String, String> params,
			String charset) throws Exception
	{
		if (params != null && !params.isEmpty())
		{
			List<NameValuePair> pairs = new ArrayList<NameValuePair>(
					params.size());
			for (Map.Entry<String, String> entry : params.entrySet())
			{
				String value = entry.getValue();
				if (value != null)
				{
					pairs.add(new BasicNameValuePair(entry.getKey(), value));
				}
			}
			baseURL += "?"
					+ EntityUtils.toString(new UrlEncodedFormEntity(pairs,
							charset));
		}
		return baseURL;
	}

	/**
	 * 描述：附件上传
	 * 
	 * @param url
	 *            地址
	 * @param map
	 *            参数
	 * @return String
	 * @throws Exception
	 *             Exception
	 */
	public static String doFileUpload(String url, Map<String, Object> map)
			throws Exception
	{
		if (StringUtils.isEmpty(url))
		{
			return null;
		}

		CloseableHttpResponse httpResponse = null;

		try
		{
			MultipartEntityBuilder mulipartEntity = MultipartEntityBuilder
					.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
					.setCharset(CharsetUtils.get(CHARSET));
			if (map != null && map.size() > 0)
			{
				for (String key : map.keySet())
				{
					Object obj = map.get(key);
					if (obj instanceof File)
					{
						FileBody fileBody = new FileBody((File) obj);
						mulipartEntity.addPart(key, fileBody);
					}
					else if (obj instanceof InputStream)
					{
						InputStreamBody inputStreamBody = new InputStreamBody(
								(InputStream) obj, "");
						mulipartEntity.addPart(key, inputStreamBody);
					}
					else
					{
						StringBody stringBody = new StringBody((String) obj,
								ContentType.TEXT_PLAIN);
						mulipartEntity.addPart(key, stringBody);
					}
				}
			}
			HttpEntity entity = mulipartEntity.build();
			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(entity);

			httpResponse = httpClient.execute(httpPost);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode != 200)
			{
				try
				{
					dealError(httpResponse, CHARSET);
				}
				finally
				{
					httpPost.abort();
				}
			}

			HttpEntity httpEntity = httpResponse.getEntity();

			String result = null;

			if (httpEntity != null)
			{
				result = EntityUtils.toString(httpEntity, CHARSET);
			}
			EntityUtils.consume(httpEntity);

			return result;

		}
		finally
		{
			IOUtils.closeQuietly(httpResponse);
		}
	}

	/**
	 * 描述：附件下载
	 * 
	 * @param url
	 *            地址
	 * @param params
	 *            参数
	 * @param savePath
	 *            保存路径
	 * @param fileName
	 *            文件名称
	 * @return Map<String, Object>
	 * @throws Exception
	 *             Exception
	 */
	public static File doFileDownLoad(String url, Map<String, String> params,
			String savePath, String fileName) throws Exception
	{
		return doFileDownLoad(url, params, null, savePath, fileName);
	}

	/**
	 * 描述：附件下载
	 * 
	 * @param url
	 *            地址
	 * @param params
	 *            参数
	 * @param headMap
	 *            请求头参数
	 * @param savePath
	 *            保存路径
	 * @param fileName
	 *            文件名称
	 * @return Map<String, Object>
	 * @throws Exception
	 *             Exception
	 */
	public static File doFileDownLoad(String url, Map<String, String> params,
			Map<String, String> headMap, String savePath, String fileName)
			throws Exception
	{
		if (StringUtils.isEmpty(url))
		{
			return null;
		}
		CloseableHttpResponse httpResponse = null;
		InputStream is = null;
		FileOutputStream fos = null;
		try
		{
			List<NameValuePair> list = null;
			if (params != null && params.size() > 0)
			{
				list = new ArrayList<NameValuePair>(params.size());
				for (Entry<String, String> entry : params.entrySet())
				{
					NameValuePair nameValuePair = new BasicNameValuePair(
							entry.getKey(), entry.getValue());
					list.add(nameValuePair);
				}
				url += "?"
						+ EntityUtils.toString(new UrlEncodedFormEntity(list,
								CHARSET));
			}

			HttpGet httpGet = new HttpGet(url);
			if (null != headMap && !headMap.isEmpty())
			{
				Set<Entry<String, String>> entrySet = headMap.entrySet();
				for (Entry<String, String> entry : entrySet)
				{
					String value = entry.getValue();
					if (value != null)
					{
						httpGet.setHeader(entry.getKey(), value);
					}

				}
			}
			httpResponse = httpClient.execute(httpGet);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode != 200)
			{
				dealError(httpResponse, CHARSET);
			}
			HttpEntity httpEntity = httpResponse.getEntity();
			if (httpEntity != null)
			{
				is = httpEntity.getContent();
				File dir = new File(savePath);
				if (!dir.exists())
				{
					// 创建目录
					dir.mkdirs();
				}

				if (StringUtils.isEmpty(fileName))
				{
					fileName = getFileName(httpResponse);
				}
				File target = new File(dir, fileName);
				fos = new FileOutputStream(target);
				IOUtils.copy(is, fos);
				return target;
			}

			EntityUtils.consume(httpEntity);
			return null;

		}
		finally
		{
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(fos);
			IOUtils.closeQuietly(httpResponse);
		}

	}

	/**
	 * 获取response header中Content-Disposition中的filename值
	 * 
	 * @param response
	 * @return
	 */
	private static String getFileName(HttpResponse response) throws Exception
	{

		Header contentHeader = response.getFirstHeader("Content-Disposition");
		if (contentHeader != null)
		{
			HeaderElement[] values = contentHeader.getElements();
			if (values.length == 1)
			{
				NameValuePair param = values[0].getParameterByName("filename");
				if (param != null)
				{
					// filename = new
					// String(param.getValue().toString().getBytes(),
					// "utf-8");
					// filename=URLDecoder.decode(param.getValue(),"utf-8");
					return param.getValue();
				}
			}
		}
		Header contentType = response.getFirstHeader("Content-type");
		if (null != contentType)
		{
			String ct = contentType.getValue();
			String ext = MIME_TYPE.get(ct);
			if (StringUtils.isNotEmpty(ext))
			{
				return UUID.randomUUID().toString() + "." + ext;
			}
		}
		return null;
	}

	/**
	 * 描述：附件下载
	 * 
	 * @param url
	 *            地址
	 * @param params
	 *            参数
	 * @param os
	 *            输出流
	 * @throws Exception
	 *             Exception
	 */
	public static void doFileDownLoad(String url, Map<String, String> params,
			OutputStream os) throws Exception
	{
		if (StringUtils.isEmpty(url))
		{
			return;
		}
		CloseableHttpResponse httpResponse = null;
		InputStream is = null;
		BufferedOutputStream fos = null;
		try
		{
			List<NameValuePair> list = null;
			if (params != null && params.size() > 0)
			{
				list = new ArrayList<NameValuePair>(params.size());
				for (Entry<String, String> entry : params.entrySet())
				{
					NameValuePair nameValuePair = new BasicNameValuePair(
							entry.getKey(), entry.getValue());
					list.add(nameValuePair);
				}
				url += "?"
						+ EntityUtils.toString(new UrlEncodedFormEntity(list,
								CHARSET));
			}

			HttpGet httpGet = new HttpGet(url);

			httpResponse = httpClient.execute(httpGet);
			int statusCode = httpResponse.getStatusLine().getStatusCode();

			if (statusCode != 200)
			{
				throw new Exception("HttpClient,error status code :"
						+ statusCode);
			}

			HttpEntity httpEntity = httpResponse.getEntity();

			// 判断附件是否存在
			Header successHeader = httpResponse.getFirstHeader("isSuccess");
			String isSuccess = "1";
			if (successHeader != null)
			{
				isSuccess = successHeader.getValue();
				Header errorMsg = httpResponse.getFirstHeader("errorMsg");
				String emsg = errorMsg == null ? "" : URLDecoder.decode(
						errorMsg.getValue(), "UTF-8");
				throw new Exception(emsg);
			}

			if (!StringUtils.equals(isSuccess, "0") && httpEntity != null)
			{
				is = httpEntity.getContent();
				fos = new BufferedOutputStream(os);
				IOUtils.copy(is, fos);
			}
			EntityUtils.consume(httpEntity);
		}
		finally
		{
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(fos);
			IOUtils.closeQuietly(httpResponse);
		}
	}

	/**
	 * @param response
	 */
	private static void dealError(HttpResponse response, String charset)
			throws ValidateException
	{
		HttpEntity entity = response.getEntity();
		String result = null;
		int statusCode = response.getStatusLine().getStatusCode();
		if (entity != null)
		{
			try
			{
				result = EntityUtils.toString(entity, charset);
				JSONObject jsonObj = (JSONObject) JSON.parse(result);
				Boolean success = jsonObj.getBoolean("success");
				if (!success)
				{
					throw new ValidateException(jsonObj.getString("errorCode"),
							jsonObj.getString("errorDesc"));
				}
				throw new ValidateException("",
						"HttpClient,error status code :" + statusCode);
			}
			catch (ParseException e)
			{
				throw new ValidateException("",
						"HttpClient,error status code :" + statusCode);
			}
			catch (IOException e)
			{
				throw new ValidateException("",
						"HttpClient,error status code :" + statusCode);
			}

		}
	}

	/**
	 * @return
	 * @throws ServiceException
	 */
	public static Record<String, String> getHeadMapOfToken()
			throws ServiceException
	{
		Record<String, String> headMap = new RecordImpl<String, String>();

		String edeUserIdKey = SystemParamUtils
				.getString(SysParamKey.EDE_USER_ID_KEY);
		String edeUserWpdKey = SystemParamUtils
				.getString(SysParamKey.EDE_USER_PWD_KEY);
		headMap.setColumn("Platcode", edeUserIdKey);
		headMap.setColumn(
				"Authorization",
				"Basic "
						+ TokenUtils.generateToken(edeUserIdKey, edeUserWpdKey));
		return headMap;
	}
}
