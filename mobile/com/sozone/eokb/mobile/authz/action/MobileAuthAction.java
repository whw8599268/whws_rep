/**
 * 包名：com.sozone.eokb.mobile.authz.action
 * 文件名：MobileAuthAction.java<br/>
 * 创建时间：2018-11-15 上午10:33:33<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.authz.action;

import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.ShiroException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.mobile.authz.token.MobileCertToken;
import com.sozone.eokb.mobile.common.MobileUtils;

/**
 * 手机认证接口<br/>
 * <p>
 * 手机认证接口<br/>
 * </p>
 * Time：2018-11-15 上午10:33:33<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
// 任意可访问
@Permission(Level.Guest)
@Path(value = "mobile/", desc = "手机认证接口")
public class MobileAuthAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(MobileAuthAction.class);

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
	 * 认证<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return token
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	@Path(value = "/auth", desc = "手机证书用户认证")
	@Service
	public ResultVO<Record<String, Object>> authentication(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("手机证书用户认证", data));
		// 获取证书请求信息
		Record<String, Object> okappUserInfo = data.getRecord();
		if (CollectionUtils.isEmpty(okappUserInfo))
		{
			logger.error(LogUtils.format("登录失败,无效的认证参数!", okappUserInfo));
			throw new ValidateException("", "登录失败,无效的认证参数!");
		}
		try
		{
			// 登录
			SecurityUtils.getSubject()
					.login(new MobileCertToken(okappUserInfo));
		}
		// 账号不存在
		catch (UnknownAccountException e)
		{
			logger.error(LogUtils.format("登录失败,账号不存在!", okappUserInfo));
			throw new ValidateException("AUTH-1010");
		}
		// 密码错误
		catch (IncorrectCredentialsException e)
		{
			logger.error(LogUtils.format("登录失败,密码不正确!", okappUserInfo));
			throw new ValidateException("AUTH-1011");
		}
		// 账号被冻结
		catch (LockedAccountException e)
		{
			logger.error(LogUtils.format("登录失败,账号被冻结!", okappUserInfo));
			throw new ValidateException("AUTH-1012");
		}
		// 其他
		catch (ShiroException e)
		{
			logger.error(LogUtils.format("登录失败", okappUserInfo), e);
			throw new ValidateException("AUTH-1013");
		}

		// 获取当前用户ID
		String userID = ApacheShiroUtils.getCurrentUserID();
		// 获取当前时间戳
		String time = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
		StringBuilder tokensb = new StringBuilder();
		tokensb.append(userID).append("|").append(time);
		// 生成Token
		String token = MobileUtils.encryptToken(tokensb.toString());
		// 处理成URL安全的Base64字符串
		token = Base64.encodeBase64URLSafeString(Base64.decodeBase64(token));
		// 响应
		Record<String, Object> result = new RecordImpl<String, Object>();
		result.setColumn("AUTH_TOKEN", token);
		// 当前用户信息
		Record<String, Object> userInfo = new RecordImpl<String, Object>();
		userInfo.putAll(ApacheShiroUtils.getCurrentUser());
		userInfo.remove("PERMITTED_" + HttpMethod.GET + "_URIS");
		userInfo.remove("PERMITTED_" + HttpMethod.DELETE + "_URIS");
		userInfo.remove("PERMITTED_" + HttpMethod.POST + "_URIS");
		userInfo.remove("PERMITTED_" + HttpMethod.PUT + "_URIS");
		userInfo.remove("PERMITTED_" + HttpMethod.OPTIONS + "_URIS");
		userInfo.remove("PERMITTED_" + HttpMethod.HEAD + "_URIS");
		userInfo.remove("PERMITTED_SERVICE_URIS");
		result.setColumn("CURRENT_USER", userInfo);
		ResultVO<Record<String, Object>> rs = new ResultVO<Record<String, Object>>(
				true);
		rs.setResult(result);
		return rs;
	}

}
