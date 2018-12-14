/**
 * 包名：com.sozone.eokb.mobile.authz.token
 * 文件名：MobileRestToken.java<br/>
 * 创建时间：2018-11-19 下午2:56:18<br/>
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

/**
 * 手机Rest Token<br/>
 * <p>
 * 手机Rest Token<br/>
 * </p>
 * Time：2018-11-19 下午2:56:18<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class MobileRestToken implements AuthenticationToken
{

	/**
	 * 序列化版本号
	 */
	private static final long serialVersionUID = -4927831092007801515L;

	/**
	 * 
	 */
	private String userID = null;

	/**
	 * 构造函数
	 * 
	 * @param userID
	 *            用户ID
	 */
	public MobileRestToken(String userID)
	{
		this.userID = userID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.shiro.authc.AuthenticationToken#getCredentials()
	 */
	@Override
	public Object getCredentials()
	{
		return this.userID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.shiro.authc.AuthenticationToken#getPrincipal()
	 */
	@Override
	public Object getPrincipal()
	{
		return this.userID;
	}

}
