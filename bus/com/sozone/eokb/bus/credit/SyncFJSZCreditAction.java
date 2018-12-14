/**
 * 包名：com.sozone.eokb.bus.credit
 * 文件名：SyncFJSZCreditAction.java<br/>
 * 创建时间：2018-10-11 下午2:01:27<br/>
 * 创建者：wengdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.credit;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.job.GetFJSZCreditScoreUtils;
import com.sozone.eokb.job.GetGCJLCreditScoreUtils;
import com.sozone.eokb.utils.NumberToCharUtils;
import com.ws.client.CreditScoreUtils;

/**
 * 同步房建市政信用分服务类<br/>
 * <p>
 * 同步房建市政信用分服务类<br/>
 * </p>
 * Time：2018-10-11 下午2:01:27<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/syncfjszcredit", desc = "同步房建市政信用分")
// 登录即可访问
@Permission(Level.Authenticated)
public class SyncFJSZCreditAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(SyncFJSZCreditAction.class);

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
	 * 
	 * 获取行业信用分或者平均分信息<br/>
	 * <p>
	 * 获取行业信用分或者平均分信息
	 * </p>
	 * 
	 * @param data
	 *            请求
	 * @return 分页信息
	 * @throws FacadeException
	 *             服务异常
	 */
	@Path(value = "", desc = "获取分页信息")
	@Service
	public Page<Record<String, Object>> getBidderCreditPage(AeolusData data)
			throws FacadeException
	{
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = data.getRecord();
		String tableName = TableName.INDUSTRY_AVG_CREDIT_SCORE;

		String tableFlag = data.getParam("tableFlag");
		if (StringUtils.isEmpty(tableFlag))
		{
			tableName = TableName.COMPANY_CREDIT_SCORE;
		}
		Page<Record<String, Object>> page = this.activeRecordDAO.auto()
				.table(tableName).addSortOrder("V_YEAR", "DESC")
				.page(pageable, param);
		List<Record<String, Object>> list = page.getContent();
		for (Record<String, Object> record : list)
		{
			if (NumberToCharUtils.isNumeric(record.getString("V_QUARTER")))
			{
				record.setColumn("V_QUARTER",
						getQuarter("", record.getString("V_QUARTER")));
			}
		}
		return page;
	}

	/**
	 * 
	 * 同步信用分<br/>
	 * <p>
	 * 同步信用分
	 * </p>
	 * 
	 * @param data
	 *            请求
	 * @throws Exception
	 *             Exception
	 */
	@Path(value = "sync", desc = "同步信用分")
	@Service
	public void doSync(AeolusData data) throws Exception
	{
		String type = data.getParam("V_TYPE");
		String year = data.getParam("V_YEAR");
		String quarter = data.getParam("V_QUARTER");
		quarter = getQuarter(type, quarter);
		String[] types = new String[1];
		types[0] = type;
		if (NumberToCharUtils.isNumeric(quarter))
		{
			GetGCJLCreditScoreUtils.getData(activeRecordDAO, types, year,
					quarter);
			return;
		}
		GetFJSZCreditScoreUtils.getData(activeRecordDAO, types, year, quarter);
	}

	private String getQuarter(String type, String quarter)
	{
		if (!StringUtils.equals("工程监理企业信用评价", type))
		{
			Integer q = Integer.valueOf(quarter);
			switch (q)
			{
				case 1:
					return "第一季度";
				case 2:
					return "第二季度";
				case 3:
					return "第三季度";
				case 4:
					return "第四季度";
				default:
					break;
			}
			return quarter;
		}
		return quarter;
	}

	/**
	 * 
	 * 获取当前年份和季度<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 年份和季度
	 * @throws FacadeException
	 *             Facade异常
	 */
	@Path(value = "gyaq", desc = "获取当前年份和季度")
	@Service
	public Record<String, Object> getYearAndQuarter(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取当前年份和季度", data));

		return CreditScoreUtils.getYearAndQuarter();
	}
}
