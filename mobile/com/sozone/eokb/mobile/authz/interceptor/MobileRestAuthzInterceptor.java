/**
 * 包名：com.sozone.eokb.mobile.authz.interceptor
 * 文件名：MobileRestAuthzInterceptor.java<br/>
 * 创建时间：2018-11-16 下午12:06:25<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.authz.interceptor;

import java.text.ParseException;
import java.util.Date;

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
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.AeolusException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.interceptor.Interceptor;
import com.sozone.aeolus.interceptor.InterceptorChain;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.mobile.authz.token.MobileRestToken;
import com.sozone.eokb.mobile.common.MobileUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 手机Rest认证<br/>
 * <p>
 * 手机Rest认证<br/>
 * </p>
 * Time：2018-11-16 下午12:06:25<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class MobileRestAuthzInterceptor implements Interceptor
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(MobileRestAuthzInterceptor.class);

	/**
	 * TOKEN key
	 */
	private static final String AUTHZ_HEAD_KEY = "Authorization";

	/**
	 * basic
	 */
	private static final String AUTHZ_HEADER_NAME = "EOKB";

	/**
	 * 招标项目ID
	 */
	private static final String EOKB_TENDER_PROJECT_ID = "EOKB-TPID";

	/**
	 * 当前用户类型,0招标人,1投标人
	 */
	private static final String EOKB_USER_TYPE = "EOKB-USER-TYPE";

	/**
	 * 客户端类型
	 */
	private static final String EOKB_CLIENT_TYPE = "EOKB-CLIENT-TYPE";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sozone.aeolus.interceptor.Interceptor#intercept(com.sozone.aeolus
	 * .data.AeolusData, com.sozone.aeolus.interceptor.InterceptorChain)
	 */
	@Override
	public void intercept(AeolusData data, InterceptorChain chain)
			throws AeolusException
	{
		logger.debug(LogUtils.format("手机Rest Token认证拦截器!", data, chain));
		this.login(data);
		// 继续往下执行
		chain.intercept(data);
	}

	/**
	 * 处理登录<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws AeolusException
	 *             AeolusException
	 */
	private void login(AeolusData data) throws AeolusException
	{
		// 获取头信息中的认证TOKEN
		String token = data.getHeader(AUTHZ_HEAD_KEY);
		logger.info(LogUtils.format("从请求中拿到的Token信息!", token));
		boolean restRequest = isRestRequest(token);
		// 如果是Rest请求
		if (restRequest)
		{
			token = getTokenValue(token);
			// 解析Token
			Record<String, Object> tokenInfo = parseToken(token);
			String userID = tokenInfo.getString("USER_ID");
			long timeStamp = tokenInfo.getLong("TIME_STAMP");
			// 如果当前用户已经登录过了
			if (ApacheShiroUtils.isAuthenticated())
			{
				// 如果当前用户与请求的用户一直
				if (StringUtils.equals(ApacheShiroUtils.getCurrentUserID(),
						userID))
				{
					// 获取选择的招标项目
					String tpid = data.getHeader(EOKB_TENDER_PROJECT_ID);
					// 如果招标项目ID不为空,且SESSION中的招标项目信息和用户请求的不一致
					if (StringUtils.isNotEmpty(tpid)
							&& (CollectionUtils.isEmpty(SessionUtils
									.getTenderProjectInfo()) || !StringUtils
									.equals(tpid, SessionUtils.getTPID())))
					{
						// 设置招标项目信息
						setTPInfo(tpid);
					}
					// 用户类型
					String userType = data.getHeader(EOKB_USER_TYPE);
					SessionUtils.setAttribute("roleCode", userType);
					// 客户端类型
					String clientType = data.getHeader(EOKB_CLIENT_TYPE);
					SessionUtils.setAttribute("EOKB_CLIENT_TYPE", clientType);
					// 跳过
					return;
				}
				// 退出重新登录
				SecurityUtils.getSubject().logout();
			}

			// 大于有效期
			if (System.currentTimeMillis() - timeStamp > MobileUtils.TOKEN_EXPIRY_DATE)
			{
				logger.error(LogUtils.format("Token超过有效期!", timeStamp));
				throw new ServiceException("EOKB-TOKEN-0001", "Token超过有效期");
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
				throw new ValidateException("EOKB-TOKEN-0002");
			}
			// 获取选择的招标项目
			String tpid = data.getHeader(EOKB_TENDER_PROJECT_ID);
			// 如果招标项目ID不为空
			if (StringUtils.isNotEmpty(tpid))
			{
				setTPInfo(tpid);
			}
			// 用户类型
			String userType = data.getHeader(EOKB_USER_TYPE);
			SessionUtils.setAttribute("roleCode", userType);
			// 客户端类型
			String clientType = data.getHeader(EOKB_CLIENT_TYPE);
			SessionUtils.setAttribute("EOKB_CLIENT_TYPE", clientType);
		}
	}

	/**
	 * 设置招标项目信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 * @throws ServiceException
	 */
	private void setTPInfo(String tpid) throws ServiceException
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
			throws ServiceException
	{
		// 获取招标项目流程信息
		String bemJson = tenderProject.getString("V_BEM_INFO_JSON");
		if (StringUtils.isEmpty(bemJson))
		{
			// 获取不到评标办法
			throw new ValidateException("", "无法获取招标项目对应的评标办法信息");
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
			throws ServiceException
	{
		// 解密Token
		token = MobileUtils.decryptToken(token);
		String[] strs = StringUtils.split(token, "|");
		// 如果token长度不正确
		if (null == strs || 2 != strs.length)
		{
			logger.error(LogUtils.format("无效的Token信息!", (Object) strs));
			throw new ServiceException("EOKB-TOKEN-0003", "无效的Token信息!");
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
			throw new ServiceException("", "解析Token时间戳发生异常!", e);
		}
		return result;
	}

	private boolean isRestRequest(String token)
	{
		//
		if (StringUtils.isNotEmpty(token))
		{
			String[] ss = StringUtils.split(token, " ");
			// 必须要是Basic 开头的
			return StringUtils.equals(AUTHZ_HEADER_NAME, ss[0]);
		}
		return false;
	}

	private String getTokenValue(String token) throws AeolusException
	{
		String[] ss = StringUtils.split(token, " ");
		if (null == ss || 2 > ss.length
				|| !StringUtils.equals(AUTHZ_HEADER_NAME, ss[0]))
		{
			throw new ValidateException("EOKB-TOKEN-0003", "无效的Token信息!");
		}
		return ss[1];
	}

}
