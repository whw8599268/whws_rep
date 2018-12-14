/**
 * 包名：com.sozone.eokb.bus.notice
 * 文件名：Notice.java<br/>
 * 创建时间：2017-8-29 下午2:02:29<br/>
 * 创建者：LDH<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.notice;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.common.Constant;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.eokb.bus.signin.Signin;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 系统公告接口<br/>
 * <p>
 * 系统公告接口<br/>
 * </p>
 * Time：2017-8-29 下午2:02:29<br/>
 * 
 * @author LDH
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/notice", desc = "系统公告接口")
@Permission(Level.Authenticated)
public class Notice
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Signin.class);

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
	 * 系统时间<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return
	 * @throws FacadeException
	 *             FacadeException
	 * @throws IOException
	 */
	// 定义路径
	@Path(value = "/getNowTime", desc = "系统时间")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void getNowTime(AeolusData data) throws FacadeException, IOException
	{
		logger.debug(LogUtils.format("系统时间", data));
		// 设置输出的格式
		JSONObject object = new JSONObject();
		HttpServletResponse response = data.getHttpServletResponse();
		response.reset();
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = data.getHttpServletResponse().getWriter();
		object.put("falg", true);
		object.put("nowTime", System.currentTimeMillis());
		out.print(object);
	}

	/**
	 * 系统公告信息<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 * @throws IOException
	 */
	// 定义路径
	@Path(value = "/htmlInfo", desc = "系统公告信息")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void htmlInfo(AeolusData data) throws FacadeException, IOException
	{
		logger.debug(LogUtils.format("系统公告信息", data));
		// 设置输出的格式
		JSONObject object = new JSONObject();
		String TPID = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", TPID);
		List<Record<String, Object>> list = activeRecordDAO.auto()
				.table("EKB_T_NOTICE").setCondition("AND", "V_TPID=#{tpid}")
				.addSortOrder("N_CREATE_TIME", "DESC").list(param);
		String noticeInfoTitle = "";
		String noticeInfo = "";
		if (list.size() > 0 && list != null)
		{
			for (int i = 0; i < list.size(); i++)
			{
				Record<String, Object> r = list.get(i);
				if (i == 0)
				{
					noticeInfoTitle = StringUtils.defaultIfEmpty(
							r.getString("V_MSG"), "");
				}
				else
				{
					noticeInfo += "<p class=\"zcd\" id=\"zcd"
							+ i
							+ "\">"
							+ StringUtils.defaultIfEmpty(r.getString("V_MSG"),
									"")
							+ "</p><hr style=\"border:1px dashed #F2F1F1;\">";
				}
				if (i == 7)
				{
					break;
				}
			}
		}
		HttpServletResponse response = data.getHttpServletResponse();
		response.reset();
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = data.getHttpServletResponse().getWriter();
		object.put("falg", true);
		object.put("noticeInfoTitle", noticeInfoTitle);
		object.put("noticeInfo", noticeInfo);
		out.print(object);
	}

	/**
	 * 获取系统公告列表<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 系统公告列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "", desc = "获取系统公告列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> loadNotices(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取系统公告列表", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());
		return this.activeRecordDAO.auto().table("EKB_T_NOTICE")
				.setCondition("AND", "V_TPID=#{tpid}")
				.addSortOrder("N_CREATE_TIME", "DESC").list(param);
	}

	/**
	 * 系统公告信息(添加)<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 * @throws IOException
	 */
	// 定义路径
	@Path(value = "/add", desc = "系统公告信息(添加)")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void addNotice(AeolusData data) throws FacadeException, IOException
	{
		logger.debug(LogUtils.format("系统公告信息(添加)", data));
		// 设置输出的格式
		JSONObject object = new JSONObject();
		object.put("success", false);
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("ID", Random.generateUUID());
		String msg = data.getParam("v_msg");
		if (StringUtils.isNotEmpty(msg))
		{
			Map<Object, Object> params = new HashMap<Object, Object>();
			// 日期
			params.put("date", DateFormatUtils.format(new Date(),
					Constant.DATE_YYYY_MM_DD_HH_MM_SS));
			params.putAll(SystemParamUtils.getProperties());
			// 表达式解析器,创建一个#{}的表达式解析器
			StrSubstitutor strs = new StrSubstitutor(params, "#{", "}");
			msg = strs.replace(msg);
		}
		record.setColumn("V_MSG", msg);
		record.setColumn("V_TYPE", data.getParam("v_type"));
		record.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		record.setColumn("V_TPID", SessionUtils.getTPID());
		this.activeRecordDAO.auto().table(ConstantEOKB.TableName.EKB_T_NOTICE)
				.save(record);
	}
}
