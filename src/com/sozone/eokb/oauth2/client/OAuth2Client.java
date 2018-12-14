/**
 * 包名：com.sozone.eokb.oauth2.client
 * 文件名：OAuth2Client.java<br/>
 * 创建时间：2017-7-5 下午5:06:38<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.oauth2.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.ext.ExtConstant;
import com.sozone.aeolus.ext.client.RACloudBaseOAuth2Client;
import com.sozone.auth2.client.OAuth2ClientConstant;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 开放式授权客户端实现<br/>
 * <p>
 * 开放式授权客户端实现<br/>
 * </p>
 * Time：2017-7-5 下午5:06:38<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "o2c", desc = "开放式授权客户端实现")
public class OAuth2Client extends RACloudBaseOAuth2Client
{

	/**
	 * 运维管理员类型
	 */
	private static final String PM_TYPE = "9527";

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(OAuth2Client.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sozone.auth2.client.facade.ClientEntry#getLoginUrl(com.sozone.aeolus
	 * .data.AeolusData)
	 */
	@Override
	protected String getLoginUrl(AeolusData data)
	{
		logger.debug(LogUtils.format("获取跳转登录页", data));
		// 获取用户类型
		String type = data.getParam(ExtConstant.USER_TYPE_PARAM_KEY);
		// 网络类型,1为内网,空或者0位外网，即缺省外网
		String netType = data.getParam("net_type");
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put(ExtConstant.USER_TYPE_PARAM_KEY, type);

		// 缺省的客户端ID
		String clientID = SystemParamUtils
				.getString(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_ID_KEY);
		// 缺省的客户端密钥
		String clientSecret = SystemParamUtils
				.getString(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_SECRET_KEY);
		// 缺省的回调地址
		String redirectUrl = SystemParamUtils
				.getString(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.REDIRECT_URI_KEY);

		// 这里只供测试使用 不允许提交
		// // 客户端ID
		// SystemParamUtils.setProperty(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_ID_KEY,
		// "");
		// // 客户端密钥
		// SystemParamUtils.setProperty(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_SECRET_KEY,
		// "");
		// // 客户端回调地址
		// SystemParamUtils.setProperty(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.REDIRECT_URI_KEY,
		// "");
		params.putAll(SystemParamUtils.getProperties());
		// 下面要额外处理内网环境
		// 如果是内网环境
		if (StringUtils.equals(netType, "1"))
		{
			// 全部换成内网参数
			// 客户端ID
			clientID = SystemParamUtils.getString(SysParamKey.CLIENT_ID_KEY);
			params.put(
					com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_ID_KEY,
					clientID);
			// 客户端密钥
			clientSecret = SystemParamUtils
					.getString(SysParamKey.CLIENT_SECRET_KEY);
			params.put(
					com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_SECRET_KEY,
					clientSecret);
			// 客户端回调地址
			redirectUrl = SystemParamUtils
					.getString(SysParamKey.REDIRECT_URI_KEY);
			params.put(
					com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.REDIRECT_URI_KEY,
					redirectUrl);
		}

		// 设置session
		SessionUtils
				.setAttribute(
						com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_ID_KEY,
						clientID);
		SessionUtils
				.setAttribute(
						com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_SECRET_KEY,
						clientSecret);
		SessionUtils
				.setAttribute(
						com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.REDIRECT_URI_KEY,
						redirectUrl);

		// 表达式解析器
		StrSubstitutor strs = new StrSubstitutor(params);
		// 服务器端授权页面
		String loginUrl = SystemParamUtils
				.getProperty(OAuth2ClientConstant.SysParamKey.AS_AUTH_URL_KEY);
		loginUrl = strs.replace(loginUrl);

		// TODO 纯粹为了解决老证书问题,到时候要删除掉
		String key = data.getParam("key");
		if (StringUtils.equals("0", key))
		{
			loginUrl = StringUtils.replace(loginUrl, "&auth_page_type=0", "");
		}
		return loginUrl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sozone.auth2.client.facade.ClientEntry#buildOAuthClientRequest(java
	 * .lang.String)
	 */
	@Override
	protected OAuthClientRequest buildOAuthClientRequest(String authCode)
			throws OAuthSystemException
	{
		logger.debug(LogUtils.format("构建OAuth2 获取Access Token请求对象", authCode));
		// 由于内网环境所以这里需要特殊处理
		return OAuthClientRequest
				.tokenLocation(
						SystemParamUtils
								.getProperty(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.AS_TOKEN_URL_KEY))
				.setGrantType(GrantType.AUTHORIZATION_CODE)
				.setClientId(
						SessionUtils
								.<String> getAttribute(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_ID_KEY))
				.setClientSecret(
						SessionUtils
								.<String> getAttribute(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_SECRET_KEY))
				.setCode(authCode)
				.setRedirectURI(
						SessionUtils
								.<String> getAttribute(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.REDIRECT_URI_KEY))
				.buildQueryMessage();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sozone.aeolus.ext.client.RACloudBaseOAuth2Client#getSuccessPage(com
	 * .sozone.aeolus.data.AeolusData)
	 */
	@Override
	protected String getSuccessPage(AeolusData data)
	{
		logger.debug(LogUtils.format("获取登录成功跳转页", data));
		// 获取用户的类型
		String userType = data.getParam(OAuth.OAUTH_STATE);
		SessionUtils.setAttribute("roleCode", userType);
		// 首页
		String successUrl = SystemParamUtils.getProperty(
				SysParamKey.MAIN_PAGE_KEY, "");
		String basePath = SystemParamUtils.getProperty(
				SysParamKey.MAIN_FORWARD_URL_KEY, data.getBasePath());
		successUrl = basePath + "/" + successUrl + "?user_type=" + userType;
		// 如果是运维登录且是运维管理员
		if (StringUtils.equalsIgnoreCase(userType, PM_TYPE)
				&& SecurityUtils.getSubject().hasRole(
						SystemParamUtils.getProperty(SysParamKey.PM_ADMIN_ID)))
		{
			successUrl = basePath + "/authorize/view/manage/index.html";
		}
		return successUrl;
	}

}
