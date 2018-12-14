/**
 * 包名：com.sozone.eokb.fjs_gsgl_ljsg_hldjf_v1
 * 文件名：FirstEnvelope.java<br/>
 * 创建时间：2017-8-28 下午2:13:39<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.xms_szgc_jl_jypbf_v2;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.xms_fjsz.common.FjszUtils;

/**
 * 评标前准备服务接口<br/>
 * <p>
 * 评标前准备服务接口<br/>
 * </p>
 * Time：2017-8-28 下午2:13:39<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/xms_szgc_jl_jypbf_v2/prebid", desc = "评标前准备服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class Prebid extends BaseAction
{

	private static final NumberFormat FMT_D = new DecimalFormat("###,##0.00",
			new DecimalFormatSymbols());

	/**
	 * 项目CODE
	 */
	private static String projectCode = ConstantEOKB.EOKBBemCode.XMS_SZGC_JL_JYPBF_V2;

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Prebid.class);

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
	 * 打开抽取投标人页面<br/>
	 * <p>
	 * 打开抽取投标人页面
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/preTenderList/flow/{tpnid}", desc = "打开抽取投标人页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView preTenderList(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开抽取投标人页面", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);
		for (Record<String, Object> section : sections)
		{
			String sectionId = section.getString("V_BID_SECTION_ID");
			param.setColumn("sid", sectionId);
			// 查询
			Record<String, Object> tpDate = this.activeRecordDAO.auto()
					.table(TableName.TENDER_PROJECT_NODE_DATA)
					.setCondition("AND", "V_TPFN_ID=#{tpnoid}")
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BUS_ID=#{sid}").get(param);

			if (CollectionUtils.isEmpty(tpDate))
			{
				tpDate = extract(tpid, tenderProjectNodeID, sectionId);
			}

			int method = FjszUtils.chouMethod(tpid, sectionId, "4");

			// 抽取方法
			section.setColumn("METHOD", method);
			section.setColumn("STATUS", tpDate.getString("V_BUS_FLAG_TYPE"));
		}
		model.put("SECTIONS", sections);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/prebid/preTenderList.index.html",
				model);
	}

	/**
	 * 
	 * 获取入围投标人<br/>
	 * <p>
	 * 获取入围投标人
	 * </p>
	 * 
	 * @param sid
	 *            标段主键
	 * @param status
	 *            入围状态
	 * @param data
	 *            AeolusData
	 * @return Page
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/getPreTenderList/{sid}/{status}", desc = "获取入围投标人")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getPreTenderList(
			@PathParam("sid") String sid, @PathParam("status") String status,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取符合搜索条件的入围投标人列表分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目");
		}

		Page<Record<String, Object>> page = null;
		param.put("tpid", tpid);
		param.put("sid", sid);
		param.put("status", status);
		param.put("V_BIDDER_NO", data.getParam("V_BIDDER_NO"));
		if (StringUtils.equals("0", status))
		{
			page = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.setCondition("AND", "N_ENVELOPE_9 IN (0,1)")
					.addSortOrder("N_CREDITSCORE", "DESC")
					.addSortOrder("V_BIDDER_NO", "ASC").page(pageable, param);
		}
		else
		{
			page = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.setCondition("AND", "N_ENVELOPE_9=1")
					.addSortOrder("N_CREDITSCORE", "DESC")
					.addSortOrder("V_BIDDER_NO", "ASC").page(pageable, param);
		}

		// 保留小数点后两位
		for (Record<String, Object> record : page.getContent())
		{
			record.setColumn("N_PRICE",
					FMT_D.format(record.getDouble("N_PRICE")));
		}

		return page;
	}

	/**
	 * 
	 * 修改入围状态<br/>
	 * <p>
	 * 修改入围状态
	 * </p>
	 * 
	 * @param id
	 *            投标人主键
	 * @param status
	 *            入围状态
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "beSelected/{id}/{status}", desc = "修改入围状态")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyBidder(@PathParam("id") String id,
			@PathParam("status") String status, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("修改入围状态", status, data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("ID", id);
		param.setColumn("N_ENVELOPE_9", status);
		this.activeRecordDAO
				.sql("UPDATE EKB_T_TENDER_LIST SET N_ENVELOPE_9 = #{N_ENVELOPE_9} WHERE ID=#{ID}")
				.build(param).update();
	}

	/**
	 * 
	 * 获取入围数量<br/>
	 * <p>
	 * 获取入围数量
	 * </p>
	 * 
	 * @param sid
	 *            标段主键
	 * @param status
	 *            入围状态
	 * @param data
	 *            AeolusData
	 * @return 入围数量
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "count/{sid}/{status}", desc = "获取入围数量")
	// PUT访问方式
	@HttpMethod(HttpMethod.GET)
	public int getCount(@PathParam("sid") String sid,
			@PathParam("status") String status, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取入围数量", data));
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("sid", sid);
		param.setColumn("status", status);
		int n = this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "N_ENVELOPE_9=#{status}")
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_SECTION_ID=#{sid}").count(param);
		return n;
	}

	/**
	 * 
	 * 初始化抽取状态<br/>
	 * <p>
	 * 初始化抽取状态
	 * </p>
	 * 
	 * @param tpid
	 * @param tenderProjectNodeID
	 * @param sid
	 * @return
	 * @throws ServiceException
	 */
	private Record<String, Object> extract(String tpid,
			String tenderProjectNodeID, String sid) throws ServiceException
	{
		Record<String, Object> record = new RecordImpl<String, Object>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("sid", sid);
		param.setColumn("tpid", tpid);
		// 有效获取投标人家数
		int count = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "N_ENVELOPE_0 = 1")
				.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
				.setCondition("AND", "V_TPID = #{tpid}").count(param);

		// 小于等于10家全部入围
		if (count <= 10)
		{
			param.setColumn("status", 1);
			this.activeRecordDAO
					.sql("UPDATE EKB_T_TENDER_LIST SET N_ENVELOPE_9=#{status} WHERE V_TPID=#{tpid} AND V_BID_SECTION_ID=#{sid} AND N_ENVELOPE_0=1")
					.build(param).update();

			record.setColumn("V_BUS_FLAG_TYPE", "1");
		}
		// 大于10家进行抽取
		else
		{
			param.setColumn("status", 0);
			this.activeRecordDAO
					.sql("UPDATE EKB_T_TENDER_LIST SET N_ENVELOPE_9=#{status} WHERE V_TPID=#{tpid} AND V_BID_SECTION_ID=#{sid} AND N_ENVELOPE_0=1")
					.build(param).update();

			record.setColumn("V_BUS_FLAG_TYPE", "0");
		}
		record.setColumn("ID", Random.generateUUID());
		record.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
				.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		record.setColumn("V_TPID", tpid);
		record.setColumn("V_TPFN_ID", tenderProjectNodeID);
		record.setColumn("V_BUS_ID", sid);
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.save(record);
		return record;
	}

	/**
	 * 
	 * 修改抽取状态<br/>
	 * <p>
	 * 修改抽取状态
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param status
	 *            入围状态
	 * @param sid
	 *            标段主键
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "extract/{tenderProjectNodeID}/{sid}/{status}", desc = "修改抽取状态")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyExtract(
			@PathParam("tenderProjectNodeID") String tenderProjectNodeID,
			@PathParam("status") String status, @PathParam("sid") String sid,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("修改抽取状态", status, data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("V_TPFN_ID", tenderProjectNodeID);
		param.setColumn("V_TPID", tpid);
		param.setColumn("V_BUS_ID", sid);
		param.setColumn("V_BUS_FLAG_TYPE", status);
		this.activeRecordDAO
				.sql("UPDATE EKB_T_TPFN_DATA_INFO SET V_BUS_FLAG_TYPE = #{V_BUS_FLAG_TYPE} WHERE V_TPFN_ID=#{V_TPFN_ID} AND V_TPID=#{V_TPID} AND V_BUS_ID=#{V_BUS_ID}")
				.build(param).update();
	}

	/**
	 * 
	 * 打开入围投标人页面<br/>
	 * <p>
	 * 打开入围投标人页面
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/preTenderListResult/flow/{tpnid}", desc = "打开入围投标人页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView preTenderListResult(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("打开入围投标人页面", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);

		model.put("SECTIONS", sections);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/prebid/preTenderList.result.html",
				model);
	}
}
