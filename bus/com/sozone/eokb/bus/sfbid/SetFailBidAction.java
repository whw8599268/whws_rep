/**
 * 包名：com.sozone.eokb.bus.sfbid
 * 文件名：SetFailBidAction.java<br/>
 * 创建时间：2017-11-1 上午11:00:47<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.sfbid;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.sievealgorithm.SieveAlgorithm;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 流标设置服务接口<br/>
 * <p>
 * 流标设置服务接口<br/>
 * </p>
 * Time：2017-11-1 上午11:00:47<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/sfbid", desc = "流标设置服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class SetFailBidAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(SetFailBidAction.class);

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
	 * 获取各标段解密情况<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 各标段解密情况
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "", desc = "获取各标段解密情况")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getSectionDecryptInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取各标段解密情况", data));
		String tpid = SessionUtils.getTPID();
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", tpid);
		boolean group = SessionUtils.isSectionGroup();
		// 如果是标段组的情况
		if (group)
		{
			return this.activeRecordDAO.statement().selectList(
					"SetFailBid.getSectionDecryptInfo_Group", params);
		}
		return this.activeRecordDAO.statement().selectList(
				"SetFailBid.getSectionDecryptInfo", params);

	}

	/**
	 * 获取各标段解密情况（电子摇号）<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 各标段解密情况
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "ele", desc = "获取各标段解密情况（电子摇号）")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getEleSectionDecryptInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取各标段解密情况（电子摇号）", data));
		String tpid = SessionUtils.getTPID();
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", tpid);
		return this.activeRecordDAO.statement().selectList(
				"SetFailBid.getSectionDecryptInfo", params);

	}

	/**
	 * 设置标段为流标<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "", desc = "设置标段为流标")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void setSectionFail(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("设置标段为流标", data));
		// 获取要设置流标的标段
		List<String> sectionIDs = data.getParams("V_BID_SECTION_IDS");
		List<String> sidList = new ArrayList<String>();
		if (CollectionUtils.isEmpty(sectionIDs))
		{
			setSectionStatusStart(sidList);
			return;
		}
		String tpid = SessionUtils.getTPID();
		Record<String, Object> section = new RecordImpl<String, Object>();
		section.setColumn("V_UPDATE_USER", ApacheShiroUtils.getCurrentUserID());
		section.setColumn("N_UPDATE_TIME", System.currentTimeMillis());
		section.setColumn("V_BID_OPEN_STATUS", "10-1");
		section.setColumn("tpid", tpid);
		String[] sids = null;
		for (String sectionID : sectionIDs)
		{
			// 用,号分割
			sids = StringUtils.split(sectionID, ",");
			if (null != sids && 0 < sids.length)
			{
				for (String sid : sids)
				{
					sidList.add(sid);
					section.setColumn("sid", sid);
					this.activeRecordDAO.auto()
							.table(TableName.EKB_T_SECTION_INFO)
							.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
							.setCondition("AND", "V_TPID = #{tpid}")
							.modify(section);
				}
			}
		}
		setSectionStatusStart(sidList);
	}

	/**
	 * 设置标段为流标（电子摇号）<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "ele", desc = "设置标段为流标（电子摇号）")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void setEleSectionFail(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("设置标段为流标", data));
		// 获取要设置流标的标段
		List<String> sectionIDs = data.getParams("V_BID_SECTION_IDS");
		List<String> sidList = new ArrayList<String>();
		if (CollectionUtils.isEmpty(sectionIDs))
		{
			setSectionStatusStart(sidList);
			return;
		}
		String tpid = SessionUtils.getTPID();
		Record<String, Object> section = new RecordImpl<String, Object>();
		section.setColumn("V_UPDATE_USER", ApacheShiroUtils.getCurrentUserID());
		section.setColumn("N_UPDATE_TIME", System.currentTimeMillis());
		section.setColumn("V_BID_OPEN_STATUS", "10-2");
		section.setColumn("tpid", tpid);
		String[] sids = null;
		for (String sectionID : sectionIDs)
		{
			// 用,号分割
			sids = StringUtils.split(sectionID, ",");
			if (null != sids && 0 < sids.length)
			{
				for (String sid : sids)
				{
					sidList.add(sid);
					section.setColumn("sid", sid);
					this.activeRecordDAO.auto()
							.table(TableName.EKB_T_SECTION_INFO)
							.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
							.setCondition("AND", "V_TPID = #{tpid}")
							.modify(section);
				}
			}
		}
		setEleSectionStatusStart(sidList);
	}

	/**
	 * 
	 * 废标的标段重新启动<br/>
	 * <p>
	 * 表单序列化的时候无法获取没有选中废标的标段，避免代理误操作将标段流标后又要启动标段
	 * </p>
	 * 
	 * @param sidList
	 * @throws FacadeException
	 */
	private void setSectionStatusStart(List<String> sidList)
			throws FacadeException
	{
		logger.debug(LogUtils.format("废标的标段重新启动", sidList));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());
		param.setColumn("status", "10-1");
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS=#{status}").list(param);

		param.setColumn("V_UPDATE_USER", ApacheShiroUtils.getCurrentUserID());
		param.setColumn("N_UPDATE_TIME", System.currentTimeMillis());
		param.setColumn("V_BID_OPEN_STATUS", "2-1");
		// 能够启动标志
		boolean canStart;
		for (Record<String, Object> section : sections)
		{
			canStart = true;
			// 排除代理设置流标的标段
			for (String sid : sidList)
			{
				if (StringUtils.equals(sid,
						section.getString("V_BID_SECTION_ID")))
				{
					canStart = false;
					break;
				}
			}

			if (canStart)
			{
				param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
				this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
						.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
						.setCondition("AND", "V_TPID = #{tpid}").modify(param);
			}
		}
	}
	
	/**
	 * 
	 * 废标的标段重新启动<br/>
	 * <p>
	 * 表单序列化的时候无法获取没有选中废标的标段，避免代理误操作将标段流标后又要启动标段
	 * </p>
	 * 
	 * @param sidList
	 * @throws FacadeException
	 */
	private void setEleSectionStatusStart(List<String> sidList)
			throws FacadeException
	{
		logger.debug(LogUtils.format("废标的标段重新启动", sidList));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());
		param.setColumn("status", "10-2");
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS=#{status}").list(param);

		param.setColumn("V_UPDATE_USER", ApacheShiroUtils.getCurrentUserID());
		param.setColumn("N_UPDATE_TIME", System.currentTimeMillis());
		param.setColumn("V_BID_OPEN_STATUS", "2-1");
		// 能够启动标志
		boolean canStart;
		for (Record<String, Object> section : sections)
		{
			canStart = true;
			// 排除代理设置流标的标段
			for (String sid : sidList)
			{
				if (StringUtils.equals(sid,
						section.getString("V_BID_SECTION_ID")))
				{
					canStart = false;
					break;
				}
			}

			if (canStart)
			{
				param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
				this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
						.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
						.setCondition("AND", "V_TPID = #{tpid}").modify(param);
			}
		}
	}
	

	/**
	 * 
	 * 获取投标人解密成功数量<br/>
	 * <p>
	 * 获取投标人解密成功数量
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 获取投标人解密成功数量
	 * @throws ServiceException
	 *             ServiceException
	 * @throws ParseException
	 *             ParseException
	 */
	@Path(value = "gdsn", desc = "获取投标人解密成功数量")
	@Service
	public ResultVO<Record<String, Object>> getDecryptSuccessNum(AeolusData data)
			throws ServiceException, ParseException
	{
		logger.debug(LogUtils.format("获取投标人解密成功数量", data));

		String tpid = SessionUtils.getTPID();

		String tableName = ConstantEOKB.TableName.EKB_T_TENDER_LIST;
		if (SessionUtils.isSectionGroup())
		{
			tableName = ConstantEOKB.TableName.EKB_T_DECRYPT_INFO;
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		List<Record<String, Object>> bidders = this.activeRecordDAO.auto()
				.table(tableName).setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("GROUP BY", "V_BIDDER_ORG_CODE").list(param);

		ResultVO<Record<String, Object>> result = new ResultVO<Record<String, Object>>(
				true);

		result.setTotal((long) bidders.size());
		// 大于500家需要筛选
		if (bidders.size() > 500)
		{
			SieveAlgorithm sa = new SieveAlgorithm();
			sa.doSieve(data);

			bidders = this.activeRecordDAO.auto().table(tableName)
					.setCondition("AND", "N_ENVELOPE_0=1")
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("GROUP BY", "V_BIDDER_ORG_CODE").list(param);
			result.setSuccess(false);
			result.setTotal((long) bidders.size());
			return result;
		}
		return result;
	}
}
