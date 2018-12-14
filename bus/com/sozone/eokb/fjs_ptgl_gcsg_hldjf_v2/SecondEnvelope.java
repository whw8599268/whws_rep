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
package com.sozone.eokb.fjs_ptgl_gcsg_hldjf_v2;

import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
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
import com.sozone.aeolus.authorize.utlis.Random;
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
@Path(value = "/fjs_ptgl_gcsg_hldjf_v2/secondenvelope", desc = "第二信封解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class SecondEnvelope extends BaseAction
{

	/**
	 * 范本CODE
	 */
	private static String projectCode = ConstantEOKB.EOKBBemCode.FJS_PTGL_GCSG_HLDJF_V2;

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(SecondEnvelope.class);

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
	 * 打开分配标段页面<br/>
	 * <p>
	 * 打开分配标段页面
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * @param data
	 *            AeolusData
	 * @return 分配标段页面
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "oasi/flow/{tpnid}", desc = "打开分配标段页面")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView openAllocationSectionIndex(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取摇球结果", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		String tpid = SessionUtils.getTPID();
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);
		// 流程信息
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		// 第一数字信封解密视图
		model.put("GROUP_LIST", PtglUtils.getSecondEnvelopeGroupInfo(tpid));

		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode
				+ "/secondEnvelope/allocation.section.html", model);
	}

	/**
	 * 
	 * 保存分配结果<br/>
	 * <p>
	 * 保存分配结果
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "sai", desc = "保存分配结果")
	@HttpMethod(HttpMethod.POST)
	public void saveAllocationIfno(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存分配结果", data));

		String groupsInfo = data.getParam("groupsInfo");

		if (!StringUtils.isEmpty(groupsInfo))
		{
			// 获取投标人分配标段信息
			JSONArray jarr = JSON.parseArray(groupsInfo);
			if (CollectionUtils.isEmpty(jarr))
			{
				return;
			}

			Record<String, Object> param = new RecordImpl<String, Object>();

			// 投标人信息
			JSONObject tempRecord = null;
			JSONObject tempJson = null;
			JSONObject sectionJson = new JSONObject();
			String sectionId;
			String code;

			Record<String, Object> bidder = new RecordImpl<String, Object>();
			for (int i = 0; i < jarr.size(); i++)
			{
				tempRecord = jarr.getJSONObject(i);
				for (String id : tempRecord.keySet())
				{
					param.clear();
					sectionId = tempRecord.getJSONObject(id)
							.getString("YAOHAO");
					code = tempRecord.getJSONObject(id).getString("CODE");
					// 未选择标段不做处理
					if (StringUtils.isEmpty(sectionId))
					{
						continue;
					}
					param.put("ID", id);
					// 标段组code
					param.put("V_BID_SECTION_GROUP_CODE", code);
					bidder = this.activeRecordDAO.auto()
							.table(TableName.EKB_T_DECRYPT_INFO).get(param);
					// 保存标段信息到投标人扩展信息
					tempJson = bidder.getJSONObject("V_JSON_OBJ");
					sectionJson.put("V_BID_SECTION_ID", sectionId);
					tempJson.put("section", sectionJson);

					param.put("V_JSON_OBJ", tempJson.toJSONString());

					param.put("orgCode", bidder.getString("V_BIDDER_ORG_CODE"));
					param.remove("ID");

					// 更新解密表表
					this.activeRecordDAO
							.auto()
							.table(TableName.EKB_T_DECRYPT_INFO)
							.setCondition("AND",
									"V_BIDDER_ORG_CODE= #{orgCode}")
							.setCondition("AND",
									"V_BID_SECTION_GROUP_CODE= #{V_BID_SECTION_GROUP_CODE}")
							.modify(param);

					// 预防数据重复，先删除

					param.setColumn("tpid", SessionUtils.getTPID());
					param.setColumn("code", code);
					param.setColumn("orgCode",
							bidder.getString("V_BIDDER_ORG_CODE"));

					this.activeRecordDAO
							.auto()
							.table(TableName.EKB_T_TENDER_LIST)
							.setCondition("AND", "V_TPID=#{tpid}")
							.setCondition("AND",
									"V_BID_SECTION_GROUP_CODE=#{code}")
							.setCondition("AND", "V_BIDDER_ORG_CODE=#{orgCode}")
							.remove(param);
					bidder.setColumn("ID", Random.generateUUID());
					// 再插入
					this.activeRecordDAO.auto()
							.table(TableName.EKB_T_TENDER_LIST).save(bidder);
				}
			}
		}

	}

	/**
	 * 第二信封文件解密结果<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/gsdr/flow/{tpnid}", desc = "第二信封文件解密结果")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSecondDecryptRecult(
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

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		param.put("tpid", SessionUtils.getTPID());

		// 第二数字信封解密视图
		String tpid = SessionUtils.getTPID();
		model.putAll(PtglUtils.getSecondEnvelopeDecryptSituation(tpid));

		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode
					+ "/secondEnvelope/decrypt.zbr.html", model);

		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/secondEnvelope/decrypt.tbr.html",
				model);
	}

	/**
	 * 第二信封文件解密结果（标段组）<br/>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点ID
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/gsdrbg/flow/{tpnid}", desc = "第二信封文件解密结果（标段组）")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSecondDecryptRecultByGroup(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("第二信封文件解密结果（标段组）", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);

		param.put("tpid", SessionUtils.getTPID());

		// 第二数字信封解密视图
		String tpid = SessionUtils.getTPID();
		model.putAll(PtglUtils.getSecondEnvelopeDecryptByGroup(tpid));

		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode
					+ "/secondEnvelope/decrypt.group.zbr.html", model);

		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode
				+ "/secondEnvelope/decrypt.group.tbr.html", model);
	}

	/**
	 * 开标记录表二<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 * @throws ParseException
	 *             ParseException
	 */
	// 定义路径
	@Path(value = "/secondRecord", desc = "开标记录表二")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getSecondRecord(AeolusData data)
			throws FacadeException, ParseException
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
						+ "/eokb/fjs_ptgl_gcsg_hldjf_v1/secondEnvelope/second.record.view.html",
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
		ptgl.createDocFileForGcsgAndGdyh(data, tpid, projectCode,
				"secondRecord");
	}
}
