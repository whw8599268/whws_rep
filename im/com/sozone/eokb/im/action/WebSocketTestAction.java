/**
 * 包名：com.sozone.eokb.im.action
 * 文件名：WebSocketTestAction.java<br/>
 * 创建时间：2018-7-31 下午4:17:20<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.im.action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.Version;

/**
 * WS协议测试Action<br/>
 * <p>
 * WS协议测试Action<br/>
 * </p>
 * Time：2018-7-31 下午4:17:20<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/wstest", desc = "WS协议测试")
@Permission(Level.Authenticated)
public class WebSocketTestAction extends BaseAction
{
	/**
	 * ws协议
	 */
	private static final String WEB_SOCKET_SCHEME = "ws://";

	/**
	 * wss协议
	 */
	private static final String WEB_SOCKET_SSL_SCHEME = "wss://";

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(WebSocketTestAction.class);

	/**
	 * 打开测试视图<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 测试视图
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "view", desc = "打开WebSocket协议测试视图")
	@Service
	public ModelAndView openTestView(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("打开WebSocket协议测试视图", data));
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("CURRENT_USER", ApacheShiroUtils.getCurrentUser());
		// 判断当前使用的是什么协议
		String scheme = data.getHttpServletRequest().getScheme();
		String wsScheme = WEB_SOCKET_SCHEME;
		// 如果使用的是https协议
		if (StringUtils.equalsIgnoreCase("https", scheme))
		{
			wsScheme = WEB_SOCKET_SSL_SCHEME;
		}
		// 头信息
		String userAgent = data.getHeader("User-Agent");
		params.setColumn("USER_AGENT", userAgent);
		params.setColumn("BROWSER", getBrowserInfo(userAgent));
		params.setColumn("PROTOCOL", wsScheme);
		// websocket路径
		String wsPath = wsScheme + data.getHttpServletRequest().getServerName()
				+ ":" + data.getHttpServletRequest().getServerPort()
				+ data.getHttpServletRequest().getContextPath();
		params.setColumn("WEBSOCKET_URL", wsPath);
		// 用于解决Flash不能主动携带SESSION_ID问题
		params.setColumn("SESSION_ID", data.getHttpServletRequest()
				.getSession().getId());

		// xmlsocket 路径
		String xsPath = "xmlsocket://"
				+ data.getHttpServletRequest().getServerName()
				+ ":"
				+ SystemParamUtils.getProperty(
						SysParamKey.FLASH_SOCKET_SECURITY_POLICY_FILE_PORT,
						"843");
		params.setColumn("XMLSOCKET_URL", xsPath);
		String url = "/im/test.html";
		return new ModelAndView(getTheme(data.getHttpServletRequest()) + url,
				params);
	}

	private Record<String, Object> getBrowserInfo(String userAgent)
	{
		Record<String, Object> result = new RecordImpl<String, Object>();
		// 获取浏览器信息
		Browser browser = UserAgent.parseUserAgentString(userAgent)
				.getBrowser();
		// 获取浏览器版本号
		Version version = browser.getVersion(userAgent);

		result.setColumn("NAME", browser.getName());
		result.setColumn("VERSION", version.getVersion());
		result.setColumn("SUPPORT", true);
		result.setColumn("SUPPORT_WS", true);
		// 如果是IE浏览器
		if (Browser.IE.isInUserAgentString(userAgent))
		{
			// 是IE浏览器
			result.setColumn("IS_IE", true);
			int ver = 0;
			try
			{
				ver = Float.valueOf(version.getVersion()).intValue();
				// 是否支持WebSocket
				result.setColumn("SUPPORT_WS", ver >= 10);
				// 是否支持的浏览器
				result.setColumn("SUPPORT", ver >= 8);
			}
			catch (Exception e)
			{
				logger.debug(LogUtils.format("获取IE版本发生异常"), e);
			}
		}

		result.setColumn("SUPPORT_CN", result.getBoolean("SUPPORT"));
		result.setColumn("SUPPORT_WS_CN", result.getBoolean("SUPPORT_WS"));
		return result;
	}
}
