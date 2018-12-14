/**
 * 包名：com.sozone.eokb.mobile.authz.realm
 * 文件名：MobileCertAuthrizingRealm.java<br/>
 * 创建时间：2018-11-15 下午1:51:37<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.authz.realm;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.common.Constant;
import com.sozone.aeolus.authorize.realm.AeolusAccountPasswordAuthorizingRealm;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.DAOException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.ext.ExtConstant;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.mobile.authz.token.MobileCertToken;
import com.sozone.eokb.utils.HttpClientUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 手机证书登录认证Realm<br/>
 * <p>
 * 手机证书登录认证Realm<br/>
 * </p>
 * Time：2018-11-15 下午1:51:37<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class MobileCertAuthrizingRealm extends
		AeolusAccountPasswordAuthorizingRealm
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sozone.aeolus.authorize.realm.AeolusAccountPasswordAuthorizingRealm
	 * #supports(org.apache.shiro.authc.AuthenticationToken)
	 */
	@Override
	public boolean supports(AuthenticationToken token)
	{
		// 只支持手机证书登录方式
		return token instanceof MobileCertToken;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sozone.aeolus.authorize.realm.AeolusAccountPasswordAuthorizingRealm
	 * #buildAuthcInfo(com.sozone.aeolus.dao.data.Record)
	 */
	@Override
	protected AuthenticationInfo buildAuthcInfo(Record<String, Object> user)
	{
		return new SimpleAuthenticationInfo(user,
				user.getString("USER_ACCOUNT"), getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sozone.aeolus.authorize.realm.AeolusAccountPasswordAuthorizingRealm
	 * #getUser(org.apache.shiro.authc.AuthenticationToken)
	 */
	@Override
	protected Record<String, Object> getUser(AuthenticationToken authcToken)
			throws DAOException
	{
		MobileCertToken mct = (MobileCertToken) authcToken;
		// 传入的证书信息
		Record<String, Object> okAppUserInfo = mct.getOKAppUserInfo();
		// 获取RA的用户信息
		Record<String, Object> raCloudUserInfo = getUserInfo(okAppUserInfo);

		// 登录账号
		String account = (String) mct.getPrincipal();
		if (StringUtils.isEmpty(account))
		{
			throw new ValidateException("", "无效的证书名称!");
		}
		// 参数
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("account", account);
		// 获取用户基本信息
		Record<String, Object> userBase = this.activeRecordDAO.auto()
				.table(Constant.TableName.USER_BASE)
				.setCondition("AND", "USER_ACCOUNT=#{account}").get(param);
		if (CollectionUtils.isEmpty(userBase))
		{
			throw new ValidateException("", "不存在的用户信息!");
		}
		// 证书表的ID,即用户基本信息表的ID
		String userID = userBase.getString("USER_ID");
		// 查询扩展信息表,查看是否有该证书关联的扩展信息对象
		Record<String, Object> ext = this.activeRecordDAO.auto()
				.table(ExtConstant.TableName.USER_EXT_INFO).get(userID);
		// 如果有关联的用户
		if (!CollectionUtils.isEmpty(ext))
		{
			// 使用关联的用户登录
			userID = ext.getString("ID");
		}
		param.clear();
		param.setColumn("uid", userID);
		// 查询用户视图
		Record<String, Object> user = this.activeRecordDAO.auto()
				.table(ExtConstant.TableName.USER_VIEW)
				.setCondition("AND", "USER_ID = #{uid}").get(param);
		// 将服务器端返回的信息也放入SESSION
		user.putAll(raCloudUserInfo);
		return user;
	}

	/**
	 * 获取证书用户信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param certInfo
	 *            证书信息
	 * @throws DAOException
	 *             持久化异常
	 */
	private Record<String, Object> getUserInfo(Record<String, Object> certInfo)
			throws DAOException
	{
		// 表中不存在当前用户信息,此时就要到云盾去取了
		// 获取服务器端证书信息
		Record<String, Object> cert = getCertificate(certInfo);
		if (CollectionUtils.isEmpty(cert))
		{
			throw new ValidateException("", "无法获取到相应的证书信息!");
		}
		// 获取服务端用户信息
		Record<String, Object> user = getCertUserInfo(cert.getString("CA_ID"));
		// 保存用户信息
		String userID = null;
		if (!CollectionUtils.isEmpty(user))
		{
			// 先保存CA用户信息
			user = saveCAUserInfo(user);
			userID = user.getString("ID");
			// 再保存证书信息,这里注意一定要不要调换顺序,要保证user对象中的ID属性是证书表的ID
			cert = saveCertInfo(cert, userID);
			// 合并对象
			user.putAll(cert);
		}
		return user;
	}

	/**
	 * 保存证书信息<br/>
	 * <p>
	 * 保存从云盾端获取到的证书信息
	 * </p>
	 * 
	 * @param cert
	 *            从云盾端获取到的证书信息
	 * @param causerID
	 *            本地CA用户信息表中的主键
	 * @return 保存到本地的证书信息对象
	 * @throws DAOException
	 *             DAOException
	 */
	private Record<String, Object> saveCertInfo(Record<String, Object> cert,
			String causerID) throws DAOException
	{
		// 先保存用户基本信息,用于登录以及权限处理
		saveUserBaseInfo(cert);
		// 构造自己的对象
		Record<String, Object> certInfo = new RecordImpl<String, Object>();
		// 使用云盾端返回的证书主键的MD5值作为本地表的主键
		String id = DigestUtils.md5Hex(cert.getString("CA_ID"));
		certInfo.setColumn("ID", id);
		certInfo.setColumn("CERT_ID", id);
		// OPEN ID
		certInfo.setColumn("V_OPEN_ID", cert.getString("OPEN_CERT_ID"));
		certInfo.setColumn("V_OPEN_CERT_ID", cert.getString("OPEN_CERT_ID"));
		// 原样保存云盾端的主键,留作备用
		certInfo.setColumn("V_CA_ID", cert.getString("CA_ID"));
		certInfo.setColumn("V_SERIAL", cert.getString("SERIAL"));
		certInfo.setColumn("N_TYPE", cert.getString("N_TYPE"));
		certInfo.setColumn("V_CERT_BASE64", cert.getString("CERTDATA"));
		certInfo.setColumn("V_START_TIME", cert.getString("STARTTIME"));
		certInfo.setColumn("V_END_TIME", cert.getString("ENDTIME"));
		certInfo.setColumn("V_LOGIN_NAME", cert.getString("LOGINNAME"));
		certInfo.setColumn("V_KEY_NAME", cert.getString("KEYNAME"));
		// 关联本地用户信息表的主键
		certInfo.setColumn("V_CA_USER_ID", causerID);
		certInfo.setColumn("N_UPDATE_TIME", System.currentTimeMillis());
		// 判断证书是否存在
		int count = this.activeRecordDAO.auto().table(TableName.CERT_INFO)
				.count(new RecordImpl<String, Object>().setColumn("ID", id));
		// 如果不存在
		if (0 == count)
		{
			// 插入时间
			certInfo.setColumn("N_CREATE_TIME", System.currentTimeMillis());
			this.activeRecordDAO.auto().table(TableName.CERT_INFO)
					.save(certInfo);
			return certInfo;
		}
		// 如果存在修改
		this.activeRecordDAO.auto().table(TableName.CERT_INFO).modify(certInfo);
		return certInfo;
	}

	/**
	 * 保存CA用户信息<br/>
	 * <p>
	 * 保存从云盾端获取到的CA用户信息
	 * </p>
	 * 
	 * @param user
	 *            从云盾端获取到的CA用户信息
	 * @return 保存到本地的CA用户对象信息
	 * @throws DAOException
	 *             DAOException
	 */
	private Record<String, Object> saveCAUserInfo(Record<String, Object> user)
			throws DAOException
	{
		// 构造自己的对象
		Record<String, Object> userInfo = new RecordImpl<String, Object>();
		// 使用云盾端返回的SYS_ID作为本地表的主键
		String id = DigestUtils.md5Hex(user.getString("SYS_ID"));
		userInfo.setColumn("ID", id);
		userInfo.setColumn("CA_USER_ID", id);
		// Open ID
		userInfo.setColumn("V_OPEN_ID", user.getString("OPEN_USER_ID"));
		userInfo.setColumn("V_OPEN_USER_ID", userInfo.getString("OPEN_USER_ID"));
		//
		userInfo.setColumn("V_SYS_ID", user.getString("SYS_ID"));
		userInfo.setColumn("V_NAME", user.getString("NAME"));
		userInfo.setColumn("V_USER_TYPE", user.getString("USERTYPE"));
		// 去重前导与尾部空格
		userInfo.setColumn("V_SOCIALCREDIT_NO",
				StringUtils.trim(user.getString("SOCIALCREDITNO")));
		userInfo.setColumn("V_REG_NO", user.getString("REGNO"));
		userInfo.setColumn("V_LEGAL_NO", user.getString("LEGALNO"));
		// 去除前导与尾部空格,便于业务匹配
		userInfo.setColumn("V_COMPANY_CODE",
				StringUtils.trim(user.getString("COMPANYCODE")));
		userInfo.setColumn("V_TAX_NO", user.getString("TAXNO"));
		userInfo.setColumn("V_RENT_NO", user.getString("RENTNO"));
		userInfo.setColumn("V_SOCIAL_NO", user.getString("SOCIALNO"));
		userInfo.setColumn("V_OTHER_NO", user.getString("OTHERNO"));
		userInfo.setColumn("V_LEGAL_NAME", user.getString("LEGALNAME"));
		userInfo.setColumn("V_ID_NO", user.getString("IDNO"));
		userInfo.setColumn("V_UNI_TTEL", user.getString("UNITTEL"));
		userInfo.setColumn("V_REG_ADDRESS", user.getString("REGADDRESS"));
		userInfo.setColumn("N_UPDATE_TIME", System.currentTimeMillis());
		// 判断用户是否存在
		int count = this.activeRecordDAO.auto().table(TableName.CA_USER_INFO)
				.count(new RecordImpl<String, Object>().setColumn("ID", id));
		// 如果不存在
		if (0 == count)
		{
			// 插入时间
			userInfo.setColumn("N_CREATE_TIME", System.currentTimeMillis());
			this.activeRecordDAO.auto().table(TableName.CA_USER_INFO)
					.save(userInfo);
			return userInfo;
		}
		// 如果存在修改,这里为了避免云盾端的数据发生了更新
		this.activeRecordDAO.auto().table(TableName.CA_USER_INFO)
				.modify(userInfo);
		return userInfo;
	}

	/**
	 * 保存用户基本信息<br/>
	 * <p>
	 * 插入本地的用户基本信息表
	 * </p>
	 * 
	 * @param cert
	 *            证书信息
	 * @throws DAOException
	 *             DAOException
	 */
	private void saveUserBaseInfo(Record<String, Object> cert)
			throws DAOException
	{
		// 先查询用户是不是存在
		int count = this.activeRecordDAO.auto()
				.table(Constant.TableName.USER_BASE)
				.setCondition("AND", "USER_ACCOUNT=#{LOGINNAME}").count(cert);
		// 如果用户不存在
		if (0 == count)
		{
			// 用户ID,用户基本信息表的主键和证书信息表的主键是一样的都是使用云盾端返回的CA_ID的MD5值作为主键
			String userID = DigestUtils.md5Hex(cert.getString("CA_ID"));
			Record<String, Object> record = this.activeRecordDAO.auto()
					.table(Constant.TableName.USER_BASE).get(userID);
			// 如果用户不存在
			if (CollectionUtils.isEmpty(record))
			{
				record = new RecordImpl<String, Object>();
				// 散列两次
				String pwd = DigestUtils.md5Hex(DigestUtils
						.md5Hex(SystemParamUtils.getString(
								Constant.RESET_PASSWORD_KEY,
								Constant.RESET_PASSWORD)));
				// 用户ID
				record.setColumn("USER_ID", userID);
				// 用户登录名,使用证书信息的登录名作为账号
				record.setColumn("USER_ACCOUNT", cert.getString("LOGINNAME"));
				// 用户姓名
				record.setColumn("USER_NAME", cert.getString("KEYNAME"));
				// 密码
				record.setColumn("PASSWORD", pwd);
				// 是否为管理员
				record.setColumn("IS_ADMIN", 0);
				// 是否被冻结
				record.setColumn("IS_LOCK", 0);
				// 插入用户
				this.activeRecordDAO.auto().table(Constant.TableName.USER_BASE)
						.save(record);
				// -----------------------------------------------
				// 插入扩展表信息
				record.clear();
				// 扩展表主键与用户基础信息表注解以及证书信息表主键一致
				record.setColumn("ID", userID);
				record.setColumn("V_CERT_ID", userID);
				// 设置创建人创建时间
				record.setColumn("V_CREATE_USER", userID).setColumn(
						"N_CREATE_TIME", System.currentTimeMillis());
				// 设置修改人修改时间
				record.setColumn("N_UPDATE_TIME", System.currentTimeMillis())
						.setColumn("V_UPDATE_USER", userID);
				// 设置是否允许账号密码登录,不允许
				record.setColumn("N_ALLOW_AP_AUTH", 0);
				// 插入扩展信息
				this.activeRecordDAO.auto()
						.table(ExtConstant.TableName.USER_EXT_INFO)
						.save(record);
				return;
			}
			// 如果用户发生名称变更,即Key发生过名称变更
			// 用户ID
			record.setColumn("USER_ID", userID);
			// 用户登录名
			record.setColumn("USER_ACCOUNT", cert.getString("LOGINNAME"));
			// 用户姓名
			record.setColumn("USER_NAME", cert.getString("KEYNAME"));
			this.activeRecordDAO.auto().table(Constant.TableName.USER_BASE)
					.modify(record);
		}
	}

	/**
	 * 去云盾端获取当前的证书信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param certInfo
	 * @return
	 * @throws DAOException
	 */
	private Record<String, Object> getCertificate(
			Record<String, Object> certInfo) throws DAOException
	{
		Map<String, String> params = new HashMap<String, String>();
		params.put(
				"client_id",
				SystemParamUtils
						.getString(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_ID_KEY));
		params.put(
				"client_secret",
				SystemParamUtils
						.getString(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_SECRET_KEY));
		params.put("serial", certInfo.getString("V_SERIAL"));
		params.put("type", certInfo.getString("N_TYPE"));
		// params.put("issuer", certInfo.getString("ISSUER_TYPE"));
		try
		{
			String certjson = HttpClientUtils.doPost(SystemParamUtils
					.getProperty(SysParamKey.MOBILE_GET_CERT_INFO_URL_KEY, ""),
					params, "UTF-8");
			JSONObject certjobj = JSON.parseObject(certjson);
			Record<String, Object> cert = new RecordImpl<String, Object>();
			cert.putAll(certjobj);
			return cert;
		}
		catch (DAOException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DAOException("", "从云盾端获取当前证书信息时发生异常!", e);
		}
	}

	/**
	 * 从云盾获取证书对应的用户信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param caid
	 * @return
	 * @throws DAOException
	 */
	private Record<String, Object> getCertUserInfo(String caid)
			throws DAOException
	{
		Map<String, String> params = new HashMap<String, String>();
		params.put(
				"client_id",
				SystemParamUtils
						.getString(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_ID_KEY));
		params.put(
				"client_secret",
				SystemParamUtils
						.getString(com.sozone.auth2.client.OAuth2ClientConstant.SysParamKey.CLIENT_SECRET_KEY));
		params.put("caid", caid);
		try
		{
			String userjson = HttpClientUtils.doPost(SystemParamUtils
					.getProperty(SysParamKey.MOBILE_GET_USER_INFO_URL_KEY, ""),
					params, "UTF-8");
			JSONObject userjobj = JSON.parseObject(userjson);
			Record<String, Object> user = new RecordImpl<String, Object>();
			user.putAll(userjobj);
			return user;
		}
		catch (DAOException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DAOException("", "从云盾端获取当前证书对应的用户信息时发生异常!", e);
		}
	}

}
