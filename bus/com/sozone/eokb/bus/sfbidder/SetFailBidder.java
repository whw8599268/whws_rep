/**
 * 包名：com.sozone.eokb.bus.sfbidder
 * 文件名：SetFailBidder.java<br/>
 * 创建时间：2017-11-18 上午11:22:39<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.sfbidder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_gsgl.common.GsglUtils;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 投标人流标<br/>
 * <p>
 * 投标人流标<br/>
 * </p>
 * Time：2017-11-18 上午11:22:39<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/sfbidder", desc = "投标人流标设置服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class SetFailBidder extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(SetFailBidder.class);

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
	 * 获取解密成功的投标人列表<br/>
	 * <p>
	 * 获取解密成功的投标人列表
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/getBidders/{tpnid}", desc = "获取解密成功的投标人列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstCredit(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取解密成功的投标人列表", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 投标人视图
		model.put("SECTION_LIST", GsglUtils.getCorrelateModel());
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		logger.debug(LogUtils.format("成功获取解密成功的投标人列表", model));

		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/setting_fail_bidder/setting.fail.bidder.html",
				model);
	}

	/**
	 * 
	 * 获取解密成功的投标人列表（标段组）<br/>
	 * <p>
	 * 获取解密成功的投标人列表（标段组）
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/getBiddersGroup/{tpnid}", desc = "获取解密成功的投标人列表（标段组）")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getFirstCreditGroup(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取解密成功的投标人列表（标段组）", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 投标人视图
		model.put("SECTION_LIST", GsglUtils.getCorrelateModelGroup());
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		logger.debug(LogUtils.format("成功获取解密成功的投标人列表", model));

		return new ModelAndView(
				getTheme(data.getHttpServletRequest())
						+ "/eokb/bus/setting_fail_bidder/setting.fail.bidder.group.html",
				model);
	}

	/**
	 * 
	 * 设置关联企业废标<br/>
	 * <p>
	 * 设置关联企业废标
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "/makeBidderFail", desc = "设置关联企业废标")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void makeBidderFail(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("设置关联企业废标", data));
		String id = data.getParam("id");
		String groupCode = data.getParam("groupCode");
		String orgCode = data.getParam("orgCode");
		String tableName = null;
		if (SessionUtils.isSectionGroup())
		{
			tableName = ConstantEOKB.TableName.EKB_T_DECRYPT_INFO;
		}
		else
		{
			tableName = ConstantEOKB.TableName.EKB_T_TENDER_LIST;
		}
		Record<String, Object> param = new RecordImpl<String, Object>();
		// 标段组编号有值，则废除该企业在标段的所有资格
		if (!StringUtils.isEmpty(groupCode))
		{
			String tpid = SessionUtils.getTPID();
			param.setColumn("tpid", tpid);
			param.setColumn("code", orgCode);
			param.setColumn("groupCode", groupCode);

			String sql = "UPDATE "
					+ tableName
					+ "  SET N_ENVELOPE_0 =0 WHERE V_TPID=#{tpid} AND V_BIDDER_ORG_CODE =#{code} AND V_BID_SECTION_GROUP_CODE =#{groupCode}";
			this.activeRecordDAO.sql(sql).setParams(param).update();

		}
		// 关联编号没有值则废除该企业
		else
		{
			param.put("ID", id);
			param.put("N_ENVELOPE_0", 0);
			this.activeRecordDAO.auto().table(tableName).modify(param);
		}
	}

	/**
	 * 
	 * 关联企业编号修改<br/>
	 * <p>
	 * 关联企业编号修改
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/saveBidders", desc = "关联企业编号修改")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void saveCorrelateCode(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("关联企业编号修改", data));
		String tpid = SessionUtils.getTPID();
		String sections = data.getParam("sections");
		Record<String, Object> param = new RecordImpl<String, Object>();
		if (!StringUtils.isEmpty(sections))
		{
			JSONArray sectionsArray = JSON.parseArray(sections);
			JSONObject jobj = null;
			String sql = null;
			String tableName = null;
			Record<String, Object> temRecord = null;
			if (SessionUtils.isSectionGroup())
			{
				tableName = ConstantEOKB.TableName.EKB_T_DECRYPT_INFO;
			}
			else
			{
				tableName = ConstantEOKB.TableName.EKB_T_TENDER_LIST;
			}
			// 组织机构代码集
			List<String> orgCodes = new ArrayList<String>();
			for (int i = 0; i < sectionsArray.size(); i++)
			{
				jobj = sectionsArray.getJSONObject(i);
				for (String orgCode : jobj.keySet())
				{

					param.clear();
					param.setColumn("tpid", tpid);
					param.setColumn("orgCode", orgCode);
					param.setColumn("correlateCode", jobj.getString(orgCode));
					// 先查出原关联企业编号
					temRecord = this.activeRecordDAO
							.auto()
							.table(tableName)
							.setCondition("AND", "V_TPID=#{tpid}")
							.setCondition("AND", "V_BIDDER_ORG_CODE=#{orgCode}")
							.get(param);
					// 如果关联企业未做修改
					if (StringUtils.equals(jobj.getString(orgCode),
							temRecord.getString("V_CORRELATE_CODE")))
					{
						continue;
					}

					// 对修改过的企业不做修改
					if (orgCodes.contains(orgCode))
					{
						continue;
					}

					// 投标人信息表
					logger.debug(LogUtils.format("修改投标人信息表的关联编号", orgCode));
					sql = "UPDATE "
							+ tableName
							+ " SET V_CORRELATE_CODE = #{correlateCode} WHERE V_BIDDER_ORG_CODE = #{orgCode} AND V_TPID = #{tpid}";
					this.activeRecordDAO.sql(sql).build(param).update();
					// 关联企业表
					logger.debug(LogUtils.format("修改关联企业表的关联编号", orgCode));
					sql = "UPDATE EKB_T_CORRELATE_ENTER SET V_CORRELATE_CODE  = #{correlateCode} WHERE V_ORG_CODE = #{orgCode} AND V_TPID = #{tpid}";
					this.activeRecordDAO.sql(sql).build(param).update();
					// 该组织机构代码的企业已做修改
					orgCodes.add(orgCode);
				}
			}
		}
	}
}
