/**
 * 包名：com.sozone.eokb.mobile.authz.filter
 * 文件名：CORSFileter.java<br/>
 * 创建时间：2018-11-21 上午11:06:59<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.authz.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.authorize.utlis.LogUtils;

/**
 * 跨域请求处理拦截器<br/>
 * <p>
 * 跨域请求处理拦截器<br/>
 * </p>
 * Time：2018-11-21 上午11:06:59<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class CORSFileter implements Filter
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(CORSFileter.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy()
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 * javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException
	{
		logger.info(LogUtils.format("设置服务端跨域请求!", req, res));
		// HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		// 允许跨域的主机地址
		response.setHeader("Access-Control-Allow-Origin", "*");
		// 允许跨域的请求方法GET, POST, HEAD 等
		response.setHeader("Access-Control-Allow-Methods", "*");
		// 重新预检验跨域的缓存时间 (s)
		response.setHeader("Access-Control-Max-Age", "3600");
		// 允许跨域的请求头
		response.setHeader("Access-Control-Allow-Headers", "*");
		// 是否携带cookie
		response.setHeader("Access-Control-Allow-Credentials", "true");
		// 继续
		chain.doFilter(req, res);
		//
		// Collection<String> names = response.getHeaderNames();
		// for (String name : names)
		// {
		// System.err.println(name + ":\t" + response.getHeader(name));
		// }

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig config) throws ServletException
	{

	}

}
