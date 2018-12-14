/**
 * 包名：com.sozone.eokb.mobile.bus.benchmark
 * 文件名：BenchmarkAction.java<br/>
 * 创建时间：2018-11-26 下午4:04:23<br/>
 * 创建者：wengdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.bus.benchmark;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.benchmark.BenchmarkUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 评标基准价<br/>
 * <p>
 * 评标基准价
 * </p>
 * Time：2018-11-26 下午4:04:23<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "mobile/benchmark")
@Permission(Level.Authenticated)
public class BenchmarkAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(BenchmarkAction.class);
	/**
	 * 金额格式化
	 */
	private static final NumberFormat FMT_D = new DecimalFormat("###,##0",
			new DecimalFormatSymbols());
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
	 * 各个标段的评标基准价信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 评标基准价信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "list", desc = "各个标段的评标基准价信息")
	@Service
	public Record<String, Object> getBenchmark(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("各个标段的评标基准价信息", data));
		String tpID = data.getParam("tpID");

		Record<String, Object> project = this.activeRecordDAO.pandora()
				.SELECT("V_BEM_INFO_JSON")
				.FROM(TableName.EKB_T_TENDER_PROJECT_INFO).EQUAL("ID", tpID)
				.get();

		if (CollectionUtils.isEmpty(project))
		{
			throw new FacadeException("", "未获取到相关的项目信息");
		}

		// 获取没有流标和已经启动标段列表
		List<Record<String, Object>> sections = this.activeRecordDAO
				.pandora()
				.SELECT("ID,V_BID_SECTION_ID,V_BID_SECTION_NAME,N_CONTROL_PRICE,N_EVALUATION_PRICE")
				.FROM(TableName.EKB_T_SECTION_INFO).EQUAL("V_TPID", tpID)
				.NOT_EQUAL("V_BID_OPEN_STATUS", "0")
				.WHERE("V_BID_OPEN_STATUS NOT LIKE '10%'").list();

		if (CollectionUtils.isEmpty(sections))
		{
			throw new FacadeException("", "为获取到相关信息标段");
		}
		// 获取每个标段下面的评标基准价信息
		boolean isCalculated;
		for (Record<String, Object> section : sections)
		{

			isCalculated = false;
			List<Record<String, Object>> list = new LinkedList<Record<String, Object>>();
			Record<String, Object> record = new RecordImpl<String, Object>();
			record.setColumn("label", "最高限价（元）");
			record.setColumn("value",
					FMT_D.format(section.getDouble("N_CONTROL_PRICE")));
			list.add(record);

			record = new RecordImpl<String, Object>();
			record.setColumn("label", "评标基准价（元）");
			record.setColumn("value", "未计算");
			if (StringUtils.isNotEmpty(section.getString("N_EVALUATION_PRICE")))
			{
				isCalculated = true;
				record.setColumn("value",
						FMT_D.format(section.getDouble("N_EVALUATION_PRICE")));
			}
			list.add(record);

			section.setColumn("INFOS", list);
			section.setColumn("IS_CALCULATED", isCalculated);

			List<Record<String, Object>> bidders = this.activeRecordDAO
					.pandora()
					.SELECT("ID,V_BIDDER_NAME,V_BIDDER_ORG_CODE")
					.FROM(TableName.EKB_T_TENDER_LIST)
					.EQUAL("V_TPID", tpID)
					.EQUAL("N_ENVELOPE_1", 1)
					.EQUAL("V_BID_SECTION_ID",
							section.getString("V_BID_SECTION_ID")).list();
			isCurrentProjectBidder(section, bidders, tpID, "DEXF_price");

		}
		Record<String, Object> result = new RecordImpl<String, Object>();
		result.setColumn("BENCHMARKS", sections);
		String pbCode = project.getJSONObject("V_BEM_INFO_JSON").getString(
				"V_CODE");
		result.setColumn("IS_HLDJF", StringUtils.contains(pbCode, "hldjf")
				|| StringUtils.contains(pbCode, "hldfj"));
		return result;
	}

	/**
	 * 
	 * 判断该用户是否是本项目的投标人<br/>
	 * <p>
	 * 判断该用户是否是本项目的投标人
	 * </p>
	 * 
	 * @param record
	 *            业务记录
	 * @param bidders
	 *            投标人列表
	 * @param tpID
	 *            招标项目主键
	 * @param modelType
	 *            确认模块
	 * @throws FacadeException
	 *             FacadeException
	 */
	private void isCurrentProjectBidder(Record<String, Object> record,
			List<Record<String, Object>> bidders, String tpID, String modelType)
			throws FacadeException
	{
		logger.debug(LogUtils.format("判断该用户是否是本项目的投标人", record, bidders));
		// 用户组织机构号码
		String orgCode = SessionUtils.getCompanyCode();
		record.setColumn("IS_CURRENT_PROJECT_BIDDER", false);
		for (Record<String, Object> bidder : bidders)
		{
			// 是本项目的投标人
			if (StringUtils.equals(orgCode,
					bidder.getString("V_BIDDER_ORG_CODE")))
			{
				record.setColumn("IS_CURRENT_PROJECT_BIDDER", true);
				record.setColumn("BIDDER_ID", bidder.getString("ID"));
				// 在获取用户确认信息
				Record<String, Object> confirm = this.activeRecordDAO.pandora()
						.SELECT("V_STATUS,V_REMARK")
						.FROM(TableName.EKB_T_CHECK_DATA).EQUAL("V_TPID", tpID)
						.EQUAL("V_BUSID", bidder.getString("ID"))
						.EQUAL("V_BUSNAME", modelType).get();
				record.setColumn("HAS_CONFIRM", false);
				// 查询到记录，已确认过
				if (!CollectionUtils.isEmpty(confirm))
				{
					record.setColumn("HAS_CONFIRM", true);
					record.setColumn("CONFIRM", confirm);
				}
				break;
			}
		}
	}

	/**
	 * 
	 * 获取标段的评标基准价信息过程<br/>
	 * <p>
	 * 获取标段的评标基准价信息过程
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 评标基准价信息过程
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "info", desc = "获取标段的评标基准价信息过程")
	@Service
	public Record<String, Object> getBenchmarkInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取标段的评标基准价信息过程", data));
		String tpID = data.getParam("tpID");
		String sectionID = data.getParam("sectionID");

		Record<String, Object> section = this.activeRecordDAO.pandora()
				.SELECT_ALL_FROM(TableName.EKB_T_SECTION_INFO)
				.EQUAL("V_TPID", tpID).EQUAL("V_BID_SECTION_ID", sectionID)
				.get();

		if (CollectionUtils.isEmpty(section))
		{
			throw new FacadeException("", "未获取到标段信息");
		}

		BenchmarkUtils.setBenchmarkInfo(section, tpID,
				section.getString("V_BID_SECTION_ID"));
		return section;
	}
}
