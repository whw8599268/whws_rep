/**
 * 包名：com.sozone.eokb.mobile.authz.filter
 * 文件名：WebSocketAuthzFilter.java<br/>
 * 创建时间：2018-11-22 上午9:16:35<br/>
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
import java.text.ParseException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.mobile.authz.token.MobileRestToken;
import com.sozone.eokb.mobile.common.MobileUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * WebSocket 认证过滤器<br/>
 * <p>
 * WebSocket 认证过滤器<br/>
 * </p>
 * Time：2018-11-22 上午9:16:35<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class WebSocketAuthzFilter implements Filter
{

	/**
	 * token 参数名
	 */
	private static final String TOKEN_PARAM_NAME = "token";

	/**
	 * 招标项目ID 参数名
	 */
	private static final String TPID_PARAM_NAME = "tpid";

	/**
	 * 用户类型 参数名
	 */
	private static final String USER_TYPE_PARAM_NAME = "utype";

	/**
	 * 客户端类型 参数名
	 */
	private static final String CLIENT_TYPE_PARAM_NAME = "ct";

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(WebSocketAuthzFilter.class);

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
		logger.info(LogUtils.format("处理WebSocket认证!", req, res));
		HttpServletRequest request = (HttpServletRequest) req;
		// 如果没有登陆过,登录
		if (!ApacheShiroUtils.isAuthenticated())
		{
			login(request);
		}
		// 继续
		chain.doFilter(req, res);
	}

	/**
	 * 登录<br/>
	 * 
	 * @param request
	 *            HttpServletRequest
	 */
	private void login(HttpServletRequest request)
	{
		logger.info(LogUtils.format("准备登陆!", request));
		// 获取token
		String token = request.getParameter(TOKEN_PARAM_NAME);
		// 获取招标项目ID
		String tpid = request.getParameter(TPID_PARAM_NAME);
		// 获取用户类型
		String userType = request.getParameter(USER_TYPE_PARAM_NAME);
		if (StringUtils.isEmpty(token))
		{
			logger.error(LogUtils.format("认证Token不允许为空!", request));
			throw new UnsupportedOperationException("认证Token不允许为空!");
		}
		if (StringUtils.isEmpty(tpid))
		{
			logger.error(LogUtils.format("招标项目ID不允许为空!", request));
			throw new UnsupportedOperationException("招标项目ID不允许为空!");
		}
		if (StringUtils.isEmpty(userType))
		{
			logger.error(LogUtils.format("用户类型不允许为空!", request));
			throw new UnsupportedOperationException("用户类型不允许为空!");
		}
		// 解析Token
		Record<String, Object> tokenInfo = parseToken(token);
		String userID = tokenInfo.getString("USER_ID");
		long timeStamp = tokenInfo.getLong("TIME_STAMP");

		// 大于有效期
		if (System.currentTimeMillis() - timeStamp > MobileUtils.TOKEN_EXPIRY_DATE)
		{
			logger.error(LogUtils.format("Token超过有效期!", timeStamp));
			throw new UnsupportedOperationException("Token超过有效期");
		}

		try
		{
			// 登录
			SecurityUtils.getSubject().login(new MobileRestToken(userID));
		}
		// 其他
		catch (Exception e)
		{
			logger.error(LogUtils.format(userID + "登录失败"), e);
			throw new UnsupportedOperationException("EOKB-TOKEN-0002");
		}
		// 设置招标项目信息
		setTPInfo(tpid);
		// 用户类型
		SessionUtils.setAttribute("roleCode", userType);

		// 客户端类型
		String clientType = request.getParameter(CLIENT_TYPE_PARAM_NAME);
		SessionUtils.setAttribute("EOKB_CLIENT_TYPE", clientType);
	}

	/**
	 * 设置招标项目信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 */
	private void setTPInfo(String tpid)
	{

		StatefulDAO statefulDAO = null;
		try
		{
			statefulDAO = new StatefulDAOImpl();
			Record<String, Object> tenderProject = statefulDAO.auto()
					.table(ConstantEOKB.TableName.EKB_T_TENDER_PROJECT_INFO)
					.get(tpid);
			if (CollectionUtils.isEmpty(tenderProject))
			{
				// 获取不到项目信息
				throw new ValidateException("E-1003");
			}
			// 获取开标时间
			String openTime = tenderProject.getString("V_BIDOPEN_TIME");
			if (StringUtils.isEmpty(openTime))
			{
				throw new ServiceException("", "招标项目开标时间为空!");
			}
			Date opTime = DateUtils.parseDate(openTime,
					new String[] { "yyyy-MM-dd HH:mm:ss" });
			if (opTime.getTime() > System.currentTimeMillis())
			{
				throw new ServiceException("", "招标项目开标时间尚未开始,请耐心等待!");
			}
			// 清空session 信息
			ApacheShiroUtils.getSession().removeAttribute(
					ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY);
			// 设置SESSION信息
			SessionUtils
					.setAttribute(ConstantEOKB.TENDER_PROJECT_INFO_SESSION_KEY,
							tenderProject);
			// 设置流程信息
			setFlowInfo(tenderProject);
			// 提交事务
			statefulDAO.commit();
		}
		catch (Exception e)
		{
			// 回滚事务
			if (null != statefulDAO)
			{
				statefulDAO.rollback();
			}
			logger.error(LogUtils.format("设置招标项目信息时发生异常!"), e);
		}
		finally
		{
			// 关闭事务
			if (null != statefulDAO)
			{
				statefulDAO.close();
			}
		}

	}

	/**
	 * 设置流程信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProject
	 *            招标项目
	 */
	private void setFlowInfo(Record<String, Object> tenderProject)
	{
		// 获取招标项目流程信息
		String bemJson = tenderProject.getString("V_BEM_INFO_JSON");
		if (StringUtils.isEmpty(bemJson))
		{
			// 获取不到评标办法
			throw new UnsupportedOperationException("无法获取招标项目对应的评标办法信息");
		}
		JSONObject bem = JSON.parseObject(bemJson);
		String code = bem.getString("V_CODE");
		// 判断是否有标段组
		Integer flag = tenderProject.getInteger("N_IS_SECTION_GROUP");
		if (null == flag)
		{
			flag = 0;
		}
		Record<String, Object> ps = new RecordImpl<String, Object>();
		ps.setColumn("bemcode", code);
		// 没标段组
		ps.setColumn("group", 0);
		// 有标段组
		if (1 == flag)
		{
			ps.setColumn("group", 1);
		}
		// 查询流程
		StatefulDAO statefulDAO = null;
		try
		{
			statefulDAO = new StatefulDAOImpl();
			Record<String, Object> flow = statefulDAO.auto()
					.table(TableName.FLOW_INFO)
					.setCondition("AND", "V_BEM_CODE = #{bemcode}")
					.setCondition("AND", "N_IS_SECTION_GROUP = #{group}")
					.get(ps);
			if (CollectionUtils.isEmpty(flow))
			{
				throw new ValidateException("", "无法获取招标项目对应的流程信息");
			}
			SessionUtils.setAttribute(
					ConstantEOKB.TENDER_PROJECT_FLOW_INFO_SESSION_KEY, flow);
			// 提交事务
			statefulDAO.commit();
		}
		catch (Exception e)
		{
			// 回滚事务
			if (null != statefulDAO)
			{
				statefulDAO.rollback();
			}
			logger.error(LogUtils.format("设置流程信息时发生异常!"), e);
		}
		finally
		{
			// 关闭事务
			if (null != statefulDAO)
			{
				statefulDAO.close();
			}
		}
	}

	private Record<String, Object> parseToken(String token)
	{
		// 解密Token
		try
		{
			token = MobileUtils.decryptToken(token);
		}
		catch (ServiceException e)
		{
			logger.error(LogUtils.format("解密Token时发生异常!"), e);
			throw new RuntimeException("解密Token时发生异常", e);
		}
		String[] strs = StringUtils.split(token, "|");
		// 如果token长度不正确
		if (null == strs || 2 != strs.length)
		{
			logger.error(LogUtils.format("无效的Token信息!", (Object) strs));
			throw new UnsupportedOperationException("无效的Token信息!");
		}

		Record<String, Object> result = new RecordImpl<String, Object>();
		result.setColumn("USER_ID", strs[0]);
		try
		{
			result.setColumn(
					"TIME_STAMP",
					DateUtils.parseDate(strs[1],
							new String[] { "yyyy-MM-dd HH:mm:ss" }).getTime());
		}
		catch (ParseException e)
		{
			throw new UnsupportedOperationException("解析Token时间戳发生异常!", e);
		}
		return result;
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
