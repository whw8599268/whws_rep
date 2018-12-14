/**
 * 包名：com.sozone.eokb.handler
 * 文件名：OperationLogHandler.java<br/>
 * 创建时间：2017-10-26 下午5:14:19<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.handler;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.handler.Handler;
import com.sozone.aeolus.handler.HandlerChain;
import com.sozone.aeolus.handler.Target;
import com.sozone.eokb.common.ConstantEOKB.TableName;

/**
 * 操作日志记录处理器<br/>
 * <p>
 * 操作日志记录处理器<br/>
 * </p>
 * Time：2017-10-26 下午5:14:19<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class OperationLogHandler implements Handler
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(OperationLogHandler.class);

	/**
	 * 构建操作日志<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param methodName
	 *            方法名
	 * @param methodDesc
	 *            方法描述
	 * @return 日志对象
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static Record<String, Object> buildOperatingLog(AeolusData data,
			String methodName, String methodDesc) throws ServiceException
	{
		Record<String, Object> log = new RecordImpl<String, Object>();
		log.setColumn("ID", Random.generateUUID());
		log.setColumn("V_CALL_METHOD_NAME", methodName);
		log.setColumn("V_CALL_METHOD_DESC", methodDesc);
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("HEAD_PARAMS", data.getHeadersMap());
		params.setColumn("OTHER_PARAMS", data.getParamsMap());
		params.setColumn("PATH_PARAMS", data.getPathParamsMap());
		params.setColumn("IP_ADDRESS", data.getClientIPAddress());
		params.setColumn("REQUEST_URL", data.getHttpServletRequest()
				.getRequestURL().toString());
		log.setColumn("V_CALL_PARAMS", JSON.toJSONString(params));
		log.setColumn("N_OPERAT_TIME", System.currentTimeMillis());
		log.setColumn("V_OPERATOR_ID", ApacheShiroUtils.getCurrentUserID());
		return log;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sozone.aeolus.handler.Handler#doHandler(com.sozone.aeolus.handler
	 * .HandlerChain, com.sozone.aeolus.handler.Target)
	 */
	@Override
	public Object doHandler(HandlerChain chain, Target target) throws Exception
	{
		// 参数
		AeolusData data = target.getAeolusData();
		// 目标类
		Class<?> tc = target.getCallClass();
		// 目标方法
		Method tm = target.getCallMethod();
		writeOperationLog(data, tc, tm);
		return chain.doHandler(target);
	}

	/**
	 * 写入操作日志<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param targetClass
	 *            目标类
	 * @param targetMethod
	 *            Method
	 */
	protected void writeOperationLog(AeolusData data, Class<?> targetClass,
			Method targetMethod)
	{
		StatefulDAO statefulDAO = null;
		try
		{
			statefulDAO = new StatefulDAOImpl();
			String className = targetClass.getCanonicalName();
			String methodName = targetMethod.getName();
			methodName = className + "." + methodName + "()";
			// 获取服务请求路径
			Path path = targetMethod.getAnnotation(Path.class);
			String methodDesc = null;
			if (null != path)
			{
				methodDesc = path.desc();
			}
			Record<String, Object> log = buildOperatingLog(data, methodName,
					methodDesc);
			statefulDAO.auto().table(TableName.OPERATION_LOG).save(log);
			// 提交事务
			statefulDAO.commit();
		}
		catch (Throwable e)
		{
			logger.error(LogUtils.format("记录日志发生异常!"), e);
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
}
