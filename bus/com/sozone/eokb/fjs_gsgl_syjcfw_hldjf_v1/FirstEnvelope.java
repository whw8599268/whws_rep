/**
 * 包名：com.sozone.eokb.fjs_gsgl_jdjl_hldfj_v1
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
package com.sozone.eokb.fjs_gsgl_syjcfw_hldjf_v1;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
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
import com.sozone.eokb.common.ConstantEOKB.EOKBFlowCode;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_gsgl.common.GsglUtils;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.AllocateSectionUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 第一信封解密服务接口<br/>
 * <p>
 * 第一信封解密服务接口<br/>
 * </p>
 * Time：2017-8-28 下午2:13:39<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fjs_gsgl_syjcfw_hldjf_v1/firstenvelope", desc = "第一信封解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class FirstEnvelope extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FirstEnvelope.class);

	/**
	 * 项目CODE
	 */
	private static String projectCode = ConstantEOKB.EOKBBemCode.FJS_GSGL_SYJCFW_HLDJF_V1;

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
	 * 获取信用等级信息<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/credit/flow/{tpnid}", desc = "获取信用等级信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstCredit(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取信用等级信息", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		String tpid = SessionUtils.getTPID();
		// 解密成功家数
		model.putAll(GsglUtils.getDecryptSuccessCount(tpid));

		param.put("tpid", tpid);
		param.put("modelType", EOKBFlowCode.DYXF_CREDIT);
		// 查询是否已经发起过确认
		Record<String, Object> cm = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_CHECK_MODEL)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND",
						"V_STATUS = '1' AND V_MODEL_TYPE = #{modelType}")
				.get(param);

		boolean alreadyLaunched = true;
		if (CollectionUtils.isEmpty(cm))
		{
			alreadyLaunched = false;
		}

		// 信用等级视图
		model.putAll(GsglUtils.getFirstEnvelopeDecryptSituation(tpid,
				alreadyLaunched, EOKBFlowCode.DYXF_CREDIT));

		// 是否发起确认标识
		model.put("alreadyLaunched", alreadyLaunched);

		logger.debug(LogUtils.format("成功获取信用等级信息"));
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/"
					+ EOKBFlowCode.FIRST_ENVELOPE + "/credit.zbr.html", model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.FIRST_ENVELOPE
				+ "/credit.tbr.html", model);
	}

	/**
	 * 修改投标人信用等级<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/mbcr", desc = "修改投标人信用等级")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void modifyBidderCreditRating(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("修改投标人信用等级", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		String bidderCreditRatings = data.getParam("BIDDER_CREDIT_RATINGS");
		param.setColumn("type", 2);
		String tpid = SessionUtils.getTPID();

		// 从字典表中获取等级列表
		List<Record<String, Object>> levels = this.activeRecordDAO.auto()
				.table(TableName.T_SYS_DICT)
				.setCondition("AND", "DICT_TYPE=#{type}").list(param);

		// 非空才处理
		if (StringUtils.isNotEmpty(bidderCreditRatings))
		{
			GsglUtils.modifyBidderCreditRating(levels, tpid,
					bidderCreditRatings);
		}
		logger.debug(LogUtils.format("修改投标人信用等级成功"));
	}

	/**
	 * 修改投标人信用等级<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/mbcrNoGroup", desc = "修改投标人信用等级")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void modifyBidderCreditRatingNoGroup(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("修改投标人信用等级", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		String bidderCreditRatings = data.getParam("BIDDER_CREDIT_RATINGS");
		param.setColumn("type", 2);
		String tpid = SessionUtils.getTPID();

		// 从字典表中获取等级列表
		List<Record<String, Object>> levels = this.activeRecordDAO.auto()
				.table(TableName.T_SYS_DICT)
				.setCondition("AND", "DICT_TYPE=#{type}").list(param);

		// 非空才处理
		if (StringUtils.isNotEmpty(bidderCreditRatings))
		{
			GsglUtils.modifyBidderCreditRatingNoGroup(levels, tpid,
					bidderCreditRatings);
		}
		logger.debug(LogUtils.format("修改投标人信用等级成功"));
	}

	/**
	 * 
	 * 获取关联企业信息<br/>
	 * <p>
	 * 获取关联企业信息
	 * </p>
	 * 
	 * @param orgCode
	 *            组织机构号
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/credit/modifyByName/{orgCode}", desc = "获取关联企业信息")
	@HttpMethod(HttpMethod.GET)
	public ModelAndView searchByName(@PathParam("orgCode") String orgCode,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("换取关联企业信息", data));

		ModelMap model = new ModelMap();

		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = data.getRecord();
		param.setColumn("tpid", tpid);
		// 获取点击的企业信息
		Record<String, Object> company = this.activeRecordDAO
				.sql("SELECT * FROM EKB_T_CORRELATE_ENTER WHERE V_ORG_CODE=#{orgcode} AND V_TPID=#{tpid}")
				.setParam("orgcode", orgCode).setParam("tpid", tpid).get();

		if (CollectionUtils.isEmpty(company))
		{
			throw new FacadeException("", "未获取到企业信息！");
		}

		// 获取所有的企业
		List<Record<String, Object>> companys = GsglUtils
				.parseBidderAssociatedEnt(this.activeRecordDAO
						.sql("SELECT * FROM EKB_T_CORRELATE_ENTER WHERE V_TPID=#{tpid} AND V_ORG_CODE IN (SELECT V_ORG_CODE FROM EKB_T_SIGN_IN WHERE V_TPID=#{tpid}) ORDER BY V_CORRELATE_CODE")
						.setParam("tpid", tpid).list());

		model.put("company", company);
		model.put("companys", companys);

		logger.debug(LogUtils.format("成功换取关联企业信息"));

		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.FIRST_ENVELOPE
				+ "/credit.modify.html", model);
	}

	/**
	 * 搜索企业关联标号<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @param name
	 *            企业名称
	 * @return List
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/credit/searchByName/{name}", desc = "搜索企业关联编号")
	@Service
	public List<Record<String, Object>> searchName(AeolusData data,
			@PathParam("name") String name) throws FacadeException
	{

		logger.debug(LogUtils.format("搜索企业关联标号", data));

		String tpid = SessionUtils.getTPID();
		// 按企业名称获取
		List<Record<String, Object>> bidders = this.activeRecordDAO
				.sql("SELECT * FROM EKB_T_CORRELATE_ENTER WHERE V_TPID=#{aid} AND V_ENTERPRIS_NAME LIKE CONCAT('%', #{name}, '%') AND V_ORG_CODE IN (SELECT V_ORG_CODE FROM EKB_T_SIGN_IN WHERE V_TPID=#{tpid}) ORDER BY V_CORRELATE_CODE")
				.setParam("name", name).setParam("aid", tpid).list();
		bidders = GsglUtils.parseBidderAssociatedEnt(bidders);
		logger.debug(LogUtils.format("搜索企业关联标号成功"));
		return bidders;
	}

	/**
	 * 更新关联关系<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/credit/updateCode", desc = "更新关联关系")
	@Service
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void updateCode(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("更行关联关系", data));
		String aid = SessionUtils.getTPID();
		String code = data.getParam("code");
		String name = data.getParam("name");
		this.activeRecordDAO
				.sql("UPDATE EKB_T_CORRELATE_ENTER SET V_CORRELATE_CODE=#{code} WHERE V_ENTERPRIS_NAME =#{name} AND V_TPID=#{aid}")
				.setParam("name", name).setParam("code", code)
				.setParam("aid", aid).update();
		this.activeRecordDAO
				.sql("UPDATE EKB_T_DECRYPT_INFO SET V_CORRELATE_CODE=#{code} WHERE V_BIDDER_NAME =#{name} AND V_TPID=#{aid}")
				.setParam("name", name).setParam("code", code)
				.setParam("aid", aid).update();
		logger.debug(LogUtils.format("更行关联关系成功"));
	}

	/**
	 * 查询标段组<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return List
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/querySection", desc = "查询标段组")
	@Service
	public List<Record<String, Object>> querySectionGroup(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("查询标段组", data));
		List<Record<String, Object>> sections = this.activeRecordDAO
				.sql("SELECT DISTINCT(V_BID_SECTION_GROUP_CODE) FROM EKB_T_SECTION_INFO WHERE V_TPID=#{tpid}")
				.setParam("tpid", SessionUtils.getTPID()).list();
		logger.debug(LogUtils.format("成功查询标段组"));
		return sections;
	}

	/**
	 * 
	 * 查询标段<br/>
	 * <p>
	 * 查询标段
	 * </p>
	 * 
	 * @param group
	 *            标段组编号
	 * @param data
	 *            AeolusData
	 * @return List
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/querySection/{group}", desc = "查询标段")
	@Service
	public List<Record<String, Object>> querySection(
			@PathParam("group") String group, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("查询标段组", data));
		List<Record<String, Object>> sections = this.activeRecordDAO
				.sql("SELECT * FROM EKB_T_SECTION_INFO WHERE V_TPID=#{tpid} AND V_BID_SECTION_GROUP_CODE=#{group}")
				.setParam("tpid", SessionUtils.getTPID())
				.setParam("group", group).list();
		logger.debug(LogUtils.format("成功查询标段组"));
		return sections;
	}

	/**
	 * 
	 * 查询投标人<br/>
	 * <p>
	 * 查询投标人
	 * </p>
	 * 
	 * @param group
	 *            标段组编号
	 * @param data
	 *            AeolusData
	 * @return List
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/queryBidder/{group}", desc = "查询投标人")
	@Service
	public List<Record<String, Object>> queryBidder(
			@PathParam("group") String group, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("查询投标人", data));
		String q = data.getParam("q");
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ekb_t_tender_list WHERE V_TPID=#{tpid} AND V_BID_SECTION_GROUP_CODE = #{group}");
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());
		param.setColumn("group", group);
		if (StringUtils.isNotEmpty(q))
		{
			param.setColumn("V_BIDDER_NAME", q);
			sql.append("AND V_BIDDER_NAME like CONCAT('%',#{V_BIDDER_NAME}, '%')");
		}
		sql.append("GROUP BY V_BIDDER_NAME ORDER BY V_BIDDER_NO");
		List<Record<String, Object>> bidders = this.activeRecordDAO
				.sql(sql.toString()).build(param).list();
		logger.debug(LogUtils.format("成功查询投标人"));
		return bidders;
	}


	/**
	 * 获取电子摇号信息<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/electronics/flow/{tpnid}", desc = "获取电子摇号信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstElectronics(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取电子摇号信息", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		String tpid = SessionUtils.getTPID();
		param.put("tpid", tpid);
		param.put("modelType", EOKBFlowCode.DYXF_ELECTRONICS);
		// 查询是否已经发起过确认
		Record<String, Object> cm = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_CHECK_MODEL)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND",
						"V_STATUS = '1' AND V_MODEL_TYPE = #{modelType}")
				.get(param);
		boolean alreadyLaunched = true;

		if (CollectionUtils.isEmpty(cm))
		{
			alreadyLaunched = false;
		}

		// 电子摇号视图
		model.putAll(GsglUtils.getFirstElectronics(tpid, alreadyLaunched,
				EOKBFlowCode.DYXF_ELECTRONICS));

		// 是否发起确认标识
		model.put("alreadyLaunched", alreadyLaunched);
		logger.debug(LogUtils.format("成功获取电子摇号信息"));
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/"
					+ EOKBFlowCode.FIRST_ENVELOPE + "/electronics.zbr.html",
					model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.FIRST_ENVELOPE
				+ "/electronics.tbr.html", model);
	}

	/**
	 * 随机分配修改<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 * 
	 */
	// 定义路径
	@Path(value = "/modifySection", desc = "随机分配修改")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void modifySection(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("随机分配修改", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		String sectionName = data.getParam("sectionName");
		String bidderCode = data.getParam("bidderCode");
		String sectionGroup = data.getParam("sectionGroup");
		if (StringUtils.isEmpty(sectionName) || StringUtils.isEmpty(bidderCode))
		{
			throw new ServiceException("标段名字或者标段编码不能为空！");
		}
		param.setColumn("V_BID_SECTION_NAME", sectionName);
		param.setColumn("tpid", SessionUtils.getTPID());
		param.setColumn("group", sectionGroup);
		// 获取标段信息
		Record<String, Object> recordSection = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND",
						"V_BID_SECTION_NAME = #{V_BID_SECTION_NAME}")
				.setCondition("AND", "V_BID_SECTION_GROUP_CODE= #{group}")
				.get(param);
		if (CollectionUtils.isEmpty(recordSection))
		{
			throw new ServiceException("查询不到对应的标段信息！");
		}
		param.setColumn("V_BIDDER_NO", bidderCode);
		param.setColumn("V_BID_SECTION_GROUP_CODE",
				recordSection.getString("V_BID_SECTION_GROUP_CODE"));
		// 获取投标信息
		Record<String, Object> recordTender = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND",
						"V_BID_SECTION_GROUP_CODE = #{V_BID_SECTION_GROUP_CODE}")
				.setCondition("AND", "V_BIDDER_NO = #{V_BIDDER_NO}")
				.setCondition("AND", "N_ENVELOPE_0=1").get(param);
		if (CollectionUtils.isEmpty(recordTender))
		{
			throw new ServiceException("查询不到对应的投标信息！");
		}
		recordTender.setColumn("V_BID_SECTION_ID",
				recordSection.getString("V_BID_SECTION_ID"));
		recordTender.setColumn("V_BID_SECTION_CODE",
				recordSection.getString("V_BID_SECTION_CODE"));
		this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_LIST)
				.modify(recordTender);
		logger.debug(LogUtils.format("随机分配修改成功", data));

	}

	/**
	 * 
	 * 电子摇号<br/>
	 * <p>
	 * 电子摇号
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/doElectronics", desc = "电子摇号")
	// GET访问方式
	@Service
	public void doElectronics(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("电子摇号", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("TPID", SessionUtils.getTPID());
		this.activeRecordDAO.auto()
				.table(ConstantEOKB.TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", " V_TPID=#{TPID}").remove(param);
		this.activeRecordDAO.auto()
				.table(ConstantEOKB.TableName.EKB_T_ELECTRONICS)
				.setCondition("AND", " V_TPID=#{TPID}").remove(param);
		// 电子摇号
		AllocateSectionUtils.allocateSectionOfAllGroup();
		logger.debug(LogUtils.format("电子摇号成功"));
	}

	/**
	 * 第一信封结果开标结果<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/firstPreview/flow/{tpnid}", desc = "第一信封结果开标结果")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstPreview(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封结果开标结果", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpid", tpid);
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// model.putAll(getXypsModelMap());
		model.putAll(GsglUtils.getBidResultViewForGroup(tpid,
				EOKBFlowCode.DYXF_OFFER));
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		logger.debug(LogUtils.format("获取第一信封结果开标结果成功"));
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/"
					+ EOKBFlowCode.FIRST_ENVELOPE + "/preview.zbr.html", model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.FIRST_ENVELOPE
				+ "/preview.tbr.html", model);
	}

	/**
	 * 第一信封结果开标结果无标段组<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/firstPreviewNoGroup/flow/{tpnid}", desc = "第一信封结果开标结果无标段组")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstPreviewNoGroup(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封结果开标结果", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		param.setColumn("modelType", EOKBFlowCode.DYXF_OFFER);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		// 查询是否已经发起过确认
		Record<String, Object> cm = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_CHECK_MODEL)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND",
						"V_STATUS = '1' AND V_MODEL_TYPE = #{modelType}")
				.get(param);
		boolean alreadyLaunched = true;
		if (CollectionUtils.isEmpty(cm))
		{
			alreadyLaunched = false;
		}
		model.put("alreadyLaunched", alreadyLaunched);
		model.putAll(GsglUtils.getBidResultView(alreadyLaunched, tpid,
				EOKBFlowCode.DYXF_OFFER));
		logger.debug(LogUtils.format("获取第一信封结果开标结果成功"));
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/"
					+ EOKBFlowCode.FIRST_ENVELOPE
					+ "/preview.no.group.zbr.html", model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.FIRST_ENVELOPE
				+ "/preview.no.group.tbr.html", model);
	}

	/**
	 * 第一信封评审结果<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/review/flow/{tpnid}", desc = "第一信封评审结果")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstReview(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封结果开标结果", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 获取当前招标项目的评标状态
		String psover = "0";
		Record<String, Object> evaluationStatus = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.get(param);
		String status = evaluationStatus.getColumn("V_BID_EVALUATION_STATUS");
		if (!StringUtils.equals("0", status))
		{
			psover = "1";
			model.putAll(GsglUtils.getfirstReviewView(tpid));
		}
		model.put("PSOVER", psover);
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		logger.debug(LogUtils.format("获取第一信封结果开标结果成功"));
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/"
					+ EOKBFlowCode.FIRST_ENVELOPE + "/review.zbr.html", model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/" + EOKBFlowCode.FIRST_ENVELOPE
				+ "/review.tbr.html", model);
	}

	/**
	 * 第一信封结束进入评审<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 * 
	 */
	// 定义路径
	@Path(value = "/first/end", desc = "第一信封结束进入评审")
	// GET访问方式
	@Service
	public void beginReview(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("第一信封结束进入评审"));
		// 设置项目ID
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 设置修改条件
		param.setColumn("tpid", tpid);
		// 评标状态0:未评标,2-1:第一信封评标完成,2:评标完成,10:评标终止\r\n
		param.setColumn("V_BID_OPEN_STATUS", EOKBFlowCode.FIRST_OVER);
		this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.modify(param);
		logger.debug(LogUtils.format("第一信封结束进入评审成功"));
	}

	/**
	 * 结束开标<br/>
	 * 
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/firstRs/end", desc = "结束开标")
	// GET访问方式
	@Service
	public void finishBid() throws FacadeException
	{
		logger.debug(LogUtils.format("结束开标"));
		// 设置项目ID
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 设置修改条件
		param.setColumn("tpid", tpid);
		// 评标状态0:未评标,2-1:第一信封评标完成,2:评标完成,10:评标终止\r\n
		param.setColumn("V_BID_OPEN_STATUS", EOKBFlowCode.BID_OVER);
		this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.modify(param);
		logger.debug(LogUtils.format("结束开标成功"));
	}
}
