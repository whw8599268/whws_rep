/**
 * 包名：com.sozone.eokb.mobile.authz.realm
 * 文件名：MobileRestAuthrizingRealm.java<br/>
 * 创建时间：2018-11-19 下午3:01:06<br/>
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

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;

import com.sozone.aeolus.authorize.common.Constant;
import com.sozone.aeolus.authorize.realm.AeolusAccountPasswordAuthorizingRealm;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.DAOException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.ext.ExtConstant;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.mobile.authz.token.MobileRestToken;

/**
 * 手机Rest Token方式登录认证Realm<br/>
 * <p>
 * 手机Rest Token方式登录认证Realm<br/>
 * </p>
 * Time：2018-11-19 下午3:01:06<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class MobileRestAuthrizingRealm extends
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
		return token instanceof MobileRestToken;
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
		return new SimpleAuthenticationInfo(user, user.getString("USER_ID"),
				getName());
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
		MobileRestToken token = (MobileRestToken) authcToken;
		// 登录账号
		String userID = (String) token.getPrincipal();
		// 获取用户基本信息
		Record<String, Object> userBase = this.activeRecordDAO.auto()
				.table(Constant.TableName.USER_BASE).get(userID);
		if (CollectionUtils.isEmpty(userBase))
		{
			throw new ValidateException("", "未找到对应的用户基本信息");
		}
		// 查询扩展信息表,查看是否有该证书关联的扩展信息对象
		Record<String, Object> ext = this.activeRecordDAO.auto()
				.table(ExtConstant.TableName.USER_EXT_INFO).get(userID);
		// 如果有关联的用户
		if (!CollectionUtils.isEmpty(ext))
		{
			// 使用关联的用户登录
			userID = ext.getString("ID");
		}
		// 参数
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("uid", userID);
		// 查询用户视图
		Record<String, Object> user = this.activeRecordDAO.auto()
				.table(ExtConstant.TableName.USER_VIEW)
				.setCondition("AND", "USER_ID = #{uid}").get(param);

		// 获取证书对应的用户信息
		Record<String, Object> certUserInfo = this.activeRecordDAO.auto()
				.table(ExtConstant.TableName.CERT_CA_USER_VIEW)
				.setCondition("AND", "V_CERT_ID = #{uid}").get(param);
		user.putAll(certUserInfo);
		return user;
	}

}
