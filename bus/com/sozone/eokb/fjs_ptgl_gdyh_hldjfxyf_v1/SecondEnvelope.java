/**
 * 包名：com.sozone.eokb.fjs_gsgl_ljsg_hldjf_v1
 * 文件名：SecondEnvelope.java<br/>
 * 创建时间：2017-8-29 下午2:05:22<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_ptgl_gdyh_hldjfxyf_v1;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import net.sf.jxls.transformer.XLSTransformer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.download.util.AeolusDownloadUtils;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.ClassLoaderUtils;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.LogFormatUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.bus.createFile.CreateFilePTGL;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_ptgl.common.PtglUtils;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 第二信封解密服务接口<br/>
 * <p>
 * 第二信封解密服务接口<br/>
 * </p>
 * Time：2017-8-29 下午2:05:22<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fjs_ptgl_gdyh_hldjfxyf_v1/secondenvelope", desc = "第二信封解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class SecondEnvelope extends BaseAction
{

	private static final NumberFormat FMT_D = new DecimalFormat("###,##0.00",
			new DecimalFormatSymbols());

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(SecondEnvelope.class);

	private static String sqlName = ConstantEOKB.EOKBBemCode.FJS_PTGL_GDYH_HLDJFXYF_V1
			+ "_secondenvelope";

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
	 * 获取摇球结果<br/>
	 * <p>
	 * 获取摇球结果
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
	@Path(value = "/rollbal/flow/{tpnid}", desc = "获取摇球结果")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getRollBal(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取摇球结果", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", SessionUtils.getTPID());
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 判断是否分配过球号
		boolean aleadySave = true;

		List<Record<String, Object>> bidders = this.activeRecordDAO.statement()
				.selectList(sqlName + ".getDecryptValid", param);
		for (Record<String, Object> bidder : bidders)
		{
			// 分配过
			if (bidder.getColumn("V_BIDDER_NO") == null)
			{
				aleadySave = false;
				break;
			}
		}

		// 获取当前招标项目的所有标段组
		List<Record<String, Object>> groups = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.list(param);

		// 循环标段组
		for (Record<String, Object> group : groups)
		{
			param.put("tpid", SessionUtils.getTPID());
			param.setColumn("group",
					group.getString("V_BID_SECTION_GROUP_CODE"));
			// 标段编号
			group.setColumn("BID_SECTION_GROUP_CODE",
					group.getString("V_BID_SECTION_GROUP_CODE"));

			// 获得标段
			List<Record<String, Object>> sections = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
					.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
					.setCondition("AND", "V_BID_SECTION_GROUP_CODE = #{group}")
					.list(param);

			// 循环标段
			for (Record<String, Object> section : sections)
			{
				section.setColumn("VBID_SECTION_NAME",
						section.getColumn("V_BID_SECTION_NAME"));
				param.setColumn("sectionId",
						section.getColumn("V_BID_SECTION_ID"));
				List<Record<String, Object>> rollResult = this.activeRecordDAO
						.auto().table(TableName.EKB_T_DECRYPT_INFO)
						.setCondition("AND", "V_TPID = #{tpid}")
						.setCondition("AND", "V_BID_SECTION_ID = #{sectionId}")
						.setCondition("AND", "N_ENVELOPE_1 = 1")
						.addSortOrder("N_SORT_FILE_BID", "ASC").list(param);
				section.setColumn("BIDDERS", rollResult);
			}

			group.setColumn("TENDER_LIST", sections);
		}

		// model.putAll(getXypsModelMap());

		model.put("SECTION_LIST", groups);
		model.put("ALREADY_SAVE", aleadySave);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		// 投标人
		if (SessionUtils.isBidder())
		{
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/rollbal.tbr.html",
					model);
		}
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/rollbal.zbr.html",
				model);
	}

	/**
	 * 
	 * 获取摇球结果<br/>
	 * <p>
	 * 获取摇球结果
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
	@Path(value = "/rollresult/flow/{tpnid}", desc = "获取摇球结果")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getRollResult(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取摇球结果", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", SessionUtils.getTPID());
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 查询有效的标段组
		List<Record<String, Object>> rollResult = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.list(param);
		Record<String, Object> paramRoll = new RecordImpl<String, Object>();
		for (Record<String, Object> roll : rollResult)
		{
			paramRoll.clear();
			paramRoll.setColumn("tpid", SessionUtils.getTPID());
			paramRoll.setColumn("group",
					roll.getColumn("V_BID_SECTION_GROUP_CODE"));

			List<Record<String, Object>> section = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_GROUP_CODE = #{group}")
					.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
					.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
					.addSortOrder("V_BID_SECTION_NAME", "ASC").list(paramRoll);
			roll.setColumn("SECTIONS", section);
		}

		// 判断是否分配过标段
		boolean alreadyAllocation = true;

		// 查出投标人名单
		// List<Record<String, Object>> tenderList = this.activeRecordDAO
		// .statement().selectList(
		// SQLNAME_SECONDENVELOPE
		// + ".getsecbidderalreadyallocation", param);
		//
		// for (Record<String, Object> bidder : tenderList)
		// {
		// if (bidder.getInteger("NUM") > 1)
		// {
		// ALREADY_ALLOCATION = false;
		// }
		// }
		List<Record<String, Object>> tenderList = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		if (CollectionUtils.isEmpty(tenderList))
		{
			alreadyAllocation = false;
		}
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		// 已分配过
		if (alreadyAllocation)
		{
			// 查询是否已经发起过确认
			Record<String, Object> cm = this.activeRecordDAO
					.auto()
					.table(TableName.EKB_T_CHECK_MODEL)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND",
							"V_STATUS = '1' AND V_MODEL_TYPE = 'DYXF_electronics'")
					.get(param);
			boolean alreadyLaunched = true;
			if (CollectionUtils.isEmpty(cm))
			{
				alreadyLaunched = false;
			}

			// 是否发起确认标识
			model.put("alreadyLaunched", alreadyLaunched);

			// 分配标段结果视图
			model.put("SECTION_LIST", getROLLRSViewData(cm, alreadyLaunched));
			if (SessionUtils.isBidder())
			{
				return new ModelAndView(
						getTheme(data.getHttpServletRequest())
								+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/rollresult.tbr.html",
						model);

			}
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/rollresult.zbr.html",
					model);
		}

		if (SessionUtils.isBidder())
		{
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/rollresult.msg.html",
					model);
		}
		model.put("SECTION_LIST", rollResult);
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/rollresult.html",
				model);

	}

	/**
	 * 
	 * 保存分配的标段<br/>
	 * <p>
	 * 保存分配的标段
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "/saveSection", desc = "保存分配的标段")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void saveSection(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("获保存分配的标段球号", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		String jsonArray = data.getParam("info");
		// 非空才处理
		if (StringUtils.isNotEmpty(jsonArray))
		{
			JSONArray array = JSON.parseArray(jsonArray);
			JSONObject roll = null;
			// JSONObject json = null;
			// String sectionId = null;
			// String sectionCode = null;

			for (int i = 0; i < array.size(); i++)
			{
				param.clear();
				roll = array.getJSONObject(i);
				param.setColumn("tpid", SessionUtils.getTPID());
				param.setColumn("group", roll.getString("GROUPCODE"));
				param.setColumn("orgCode", roll.getString("ORGCODE"));
				List<Record<String, Object>> records = this.activeRecordDAO
						.auto()
						.table(TableName.EKB_T_DECRYPT_INFO)
						.setCondition("AND", "V_TPID=#{tpid}")
						.setCondition("AND",
								"V_BID_SECTION_GROUP_CODE=#{group}")
						.setCondition("AND", "V_BIDDER_ORG_CODE=#{orgCode}")
						.list(param);
				if (CollectionUtils.isEmpty(records))
				{
					throw new ServiceException("", "分配结果录入有误，请重新分配！");
				}
				// 循环查出来的投标人
				for (Record<String, Object> record : records)
				{
					if (StringUtils.equals(roll.getString("ID"),
							record.getString("ID")))
					{
						// 功能尚有缺陷，暂时先给用户友好提示
						try
						{
							record.setColumn("V_JSON_OBJ",
									record.getColumn("V_JSON_OBJ"));
							this.activeRecordDAO.auto()
									.table(TableName.EKB_T_TENDER_LIST)
									.save(record);
						}
						catch (Exception e)
						{
							throw new ServiceException("", "标段组"
									+ roll.getString("GROUPCODE") + "的标段"
									+ roll.getString("SECTIONNAME") + "重复录入球号"
									+ record.getString("V_BIDDER_NO"));
						}
					}
				}
			}

			// 获取当前招标项目的所有标段
			List<Record<String, Object>> sections = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
					.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
					.setCondition("GROUP BY", "V_BID_SECTION_NAME").list(param);

			int bidderNum;
			for (Record<String, Object> section : sections)
			{
				param.clear();
				param.setColumn("tpid", SessionUtils.getTPID());
				param.setColumn("sid", section.getColumn("V_BID_SECTION_ID"));
				bidderNum = this.activeRecordDAO.auto()
						.table(TableName.EKB_T_TENDER_LIST)
						.setCondition("AND", "V_TPID = #{tpid}")
						.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
						.count(param);
				// 投标人数量为0，作废标段
				if (bidderNum == 0)
				{
					section.setColumn("V_BID_OPEN_STATUS", "10");
					this.activeRecordDAO.auto()
							.table(TableName.EKB_T_SECTION_INFO)
							.modify(section);
				}
			}

		}
	}

	/**
	 * 
	 * 获取球号<br/>
	 * <p>
	 * 获取球号
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return List
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/rownumber", desc = "获取球号")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public List<Record<String, Object>> getRollNo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取球号", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());
		param.setColumn("V_BID_SECTION_ID", data.getParam("sectionID"));
		List<Record<String, Object>> records = this.activeRecordDAO.statement()
				.selectList(sqlName + ".getDecryptResult", param);
		return records;
	}

	/**
	 * 
	 * 获取企业名称<br/>
	 * <p>
	 * 获取企业名称
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return Record
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/biddername", desc = "获取企业名称")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public Record<String, Object> getBidderName(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取企业名称", data));
		String rollNum = data.getParam("rollNum");
		String sectionID = data.getParam("sectionID");
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());
		param.setColumn("rollNum", rollNum);
		param.setColumn("sectionID", sectionID);
		// String[] strs = num.split("\\(");
		Record<String, Object> record = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_DECRYPT_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BIDDER_NO=#{rollNum}")
				.setCondition("AND", "V_BID_SECTION_ID=#{sectionID}")
				.get(param);
		if (CollectionUtils.isEmpty(record))
		{
			throw new ServiceException("", "找不到对应的企业名称");
		}
		return record;
	}

	/**
	 * 
	 * 保存球号<br/>
	 * <p>
	 * 保存球号
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/rollNum", desc = "保存球号")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void saveRoll(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存球号", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		String jsonArray = data.getParam("info");
		// 非空才处理
		if (StringUtils.isNotEmpty(jsonArray))
		{
			JSONArray array = JSON.parseArray(jsonArray);
			JSONObject section = null;
			JSONObject bidders = null;
			// 标段ID
			String sectionID = null;
			String bidderID = null;
			String ballNO = null;
			for (int i = 0; i < array.size(); i++)
			{
				param.clear();
				// 标段
				section = array.getJSONObject(i);
				// 标段ID
				sectionID = section.getString("V_BID_SECTION_ID");
				bidders = section.getJSONObject("BIDDER_BALLS");
				if (null == bidders || bidders.isEmpty())
				{
					continue;
				}
				// 如果有录入球号
				for (Entry<String, Object> entry : bidders.entrySet())
				{
					bidderID = entry.getKey();
					ballNO = (String) entry.getValue();
					param.setColumn("ID", bidderID);
					param.setColumn("V_BIDDER_NO", ballNO);
					param.setColumn("sid", sectionID);
					this.activeRecordDAO.auto()
							.table(TableName.EKB_T_DECRYPT_INFO)
							.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
							.modify(param);
				}
			}
		}
	}

	/**
	 * 第二信封文件解密结果<br/>
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
	@Path(value = "/secondRc/flow/{tpnid}", desc = "第二信封文件解密结果")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSecondRc(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第二信封文件解密结果", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// model.putAll(getXypsModelMap());

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		param.put("tpid", SessionUtils.getTPID());
		// 查询是否已经发起过确认
		Record<String, Object> cm = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_CHECK_MODEL)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND",
						"V_STATUS = '1' AND V_MODEL_TYPE = 'DEXF_offer'")
				.get(param);
		boolean alreadyLaunched = true;
		if (CollectionUtils.isEmpty(cm))
		{
			alreadyLaunched = false;
		}

		// 是否发起确认标识
		model.put("alreadyLaunched", alreadyLaunched);
		// 第二数字信封解密视图
		String tpid = SessionUtils.getTPID();
		model.putAll(PtglUtils.getSecondEnvelopeDecryptSituation(tpid));

		// 招标人
		if (!SessionUtils.isBidder())
		{
			// 合理低价法+信用分
			if (StringUtils.equals("fjs_ptgl_gdyh_hldjfxyf_v1",
					SessionUtils.getTenderProjectTypeCode()))
			{
				return new ModelAndView(
						getTheme(data.getHttpServletRequest())
								+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/decrypt.zbr.html",
						model);
			}
			// 合理低价法
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_gdyh_hldjf_v1/secondEnvelope/decrypt.zbr.html",
					model);

		}
		// 合理低价法+信用分
		if (StringUtils.equals("fjs_ptgl_gdyh_hldjfxyf_v1",
				SessionUtils.getTenderProjectTypeCode()))
		{
			return new ModelAndView(
					getTheme(data.getHttpServletRequest())
							+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/decrypt.tbr.html",
					model);

		}
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/fjs_ptgl_gdyh_hldjf_v1/secondEnvelope/decrypt.zbr.html",
				model);
	}

	/**
	 * 
	 * 获取标段分配结果<br/>
	 * <p>
	 * 获取标段分配结果
	 * </p>
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private List<Record<String, Object>> getROLLRSViewData(
			Record<String, Object> cm, boolean alreadyLaunched)
			throws ServiceException
	{
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);

		// 获取当前招标项目的所有标段组
		List<Record<String, Object>> groups = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_GROUP_CODE")
				.list(param);

		// 循环标段组
		for (Record<String, Object> group : groups)
		{
			param.put("tpid", SessionUtils.getTPID());
			param.setColumn("group",
					group.getString("V_BID_SECTION_GROUP_CODE"));
			// 标段编号
			group.setColumn("BID_SECTION_GROUP_CODE",
					group.getString("V_BID_SECTION_GROUP_CODE"));

			// 获得标段
			List<Record<String, Object>> sections = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
					.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
					.setCondition("AND", "V_BID_SECTION_GROUP_CODE = #{group}")
					.list(param);

			// 循环标段
			for (Record<String, Object> section : sections)
			{
				param.clear();
				param.setColumn("tpid", SessionUtils.getTPID());
				section.setColumn("V_BID_SECTION_NAME",
						section.getColumn("V_BID_SECTION_NAME"));
				param.setColumn("sid", section.getColumn("V_BID_SECTION_ID"));
				param.setColumn("modelType", "DYXF_electronics");
				List<Record<String, Object>> rollResult = this.activeRecordDAO
						.statement().selectList(sqlName + ".getSecBidderInfo",
								param);
				section.setColumn("BIDDERS", rollResult);
				section.setColumn("ALREADY_LAUNCHED", alreadyLaunched);
			}

			group.setColumn("TENDER_LIST", sections);
		}
		return groups;
	}

	/**
	 * 
	 * 生成excel,无返回值<br/>
	 * <p>
	 * 生成excel,无返回值
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/export", desc = "生成excel,无返回值")
	public void exclAssets(AeolusData data) throws FacadeException
	{
		logger.debug(LogFormatUtils.formatOperateMessage("", "生成excel,无返回值",
				data));

		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);

		// 获取当前招标项目的所有标段
		List<Record<String, Object>> bidders = this.activeRecordDAO.statement()
				.selectList(sqlName + ".getExcelBidderInfo", param);

		// 投标报价强制转成小数点两位
		for (Record<String, Object> bidder : bidders)
		{
			bidder.setColumn("N_PRICE",
					FMT_D.format(bidder.getDouble("N_PRICE")));

		}
		String fileName = "投标报价";
		// 输出文件名
		String outFileName = fileName + ".xls";

		HttpServletResponse response = data.getHttpServletResponse();
		String mimeType = AeolusDownloadUtils.getMimeType(outFileName);
		response.setContentType(mimeType);

		response.setHeader("Content-Disposition", "attachment;filename="
				+ AeolusDownloadUtils.encodeFileName(data, outFileName));

		InputStream input = null;
		OutputStream out = null;

		param.setColumn("bidders", bidders);

		try
		{
			input = ClassLoaderUtils
					.getResourceAsStream(
							"com/sozone/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/bidder_info_template.xls",
							this.getClass());
			XLSTransformer transformer = new XLSTransformer();
			transformer.groupCollection("department.staff");
			Workbook resultWorkbook = transformer.transformXLS(input, param);
			// 获取输出流
			out = response.getOutputStream();
			resultWorkbook.write(out);
			out.flush();
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("导出投标人信息列表发生异常!"), e);
			throw new ServiceException("", "导出投标人信息列表发生异常!", e);
		}
		finally
		{
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * 开标记录表二<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/secondRecord", desc = "开标记录表二")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSecondRecord(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("开标记录表二", data));
		ModelMap model = new ModelMap();

		String tpid = SessionUtils.getTPID();
		model.putAll(PtglUtils.getSecondEnvelopeDecryptSituation(tpid));

		// 项目名称
		model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
		// 组别
		model.put("GROUP", "无标段组");
		// 开标时间
		String vTime = SessionUtils.getBidOpenTime();
		model.put("TIME", DateFormatUtils.format(
				DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
				"yyyy年MM月dd日 HH时mm分"));
		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/fjs_ptgl_gdyh_hldjfxyf_v1/secondEnvelope/second.record.view.html",
				model);
	}

	/**
	 * 
	 * 保存投标人备注<br/>
	 * <p>
	 * 保存投标人备注
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "/saveBidders", desc = "保存投标人备注")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void saveSign(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("保存投标人备注", data));

		String json = data.getParam("bidders");
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isNotEmpty(json))
		{
			PtglUtils.saveFirstRemark(data, json, tpid, "secondRemark",
					ConstantEOKB.SECOND_REMARK);
		}

		CreateFilePTGL ptgl = new CreateFilePTGL();
		ptgl.createDocFileForGcsgAndGdyh(data, tpid,
				"fjs_ptgl_gdyh_hldjfxyf_v1", "secondRecord");
	}
}
