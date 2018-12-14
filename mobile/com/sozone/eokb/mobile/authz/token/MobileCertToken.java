/**
 * 包名：com.sozone.eokb.mobile.authz.token
 * 文件名：MobileCertToken.java<br/>
 * 创建时间：2018-11-15 上午11:49:55<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.authz.token;

import org.apache.shiro.authc.AuthenticationToken;

import com.sozone.aeolus.dao.data.Record;

/**
 * 手机证书登录Token<br/>
 * <p>
 * 手机证书登录Token<br/>
 * </p>
 * Time：2018-11-15 上午11:49:55<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class MobileCertToken implements AuthenticationToken
{

	/**
	 * 序列化版本号
	 */
	private static final long serialVersionUID = -3192263191859732723L;

	/**
	 * OKApp的用户信息
	 */
	private Record<String, Object> okappUserInfo = null;

	/**
	 * 构造函数
	 * 
	 * @param okappUserInfo
	 *            OKApp的用户信息
	 */
	public MobileCertToken(Record<String, Object> okappUserInfo)
	{
		this.okappUserInfo = okappUserInfo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.shiro.authc.AuthenticationToken#getCredentials()
	 */
	@Override
	public Object getCredentials()
	{
		return this.okappUserInfo.getString("V_LOGIN_NAME");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.shiro.authc.AuthenticationToken#getPrincipal()
	 */
	@Override
	public Object getPrincipal()
	{
		return this.okappUserInfo.getString("V_LOGIN_NAME");
	}

	/**
	 * okappUserInfo属性的get方法
	 * 
	 * @return the okappUserInfo
	 */
	public Record<String, Object> getOKAppUserInfo()
	{
		return okappUserInfo;
	}

}
