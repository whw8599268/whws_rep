/**
 * 包名：com.sozone.eokb.common.bus.signin;
 * 文件名：Signin.java<br/>
 * 创建时间：2017-7-27 上午11:45:27<br/>
 * 创建者：jack<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.signinPm;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import net.sf.jxls.transformer.XLSTransformer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 签到服务接口<br/>
 * Time：2017-7-25 下午7:11:15<br/>
 * 
 * @author jack
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/signpm", desc = "签到服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class SigninPm extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(SigninPm.class);

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
	 * 获取人员到会情况<br/>
	 * <p>
	 * 获取人员到会情况
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点主键
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/getBidders/flow/{tpnid}", desc = "获取人员到会情况")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getRollResult(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取人员到会情况", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", SessionUtils.getTPID());
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		// 查询有效的标段组
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("ORDER BY", "V_BID_SECTION_NAME").list(param);
		List<Record<String, Object>> bidders = null;

		String tableName = null;
		if (!SessionUtils.isSectionGroup())
		{
			tableName = ConstantEOKB.TableName.EKB_T_TENDER_LIST;
		}
		else
		{
			tableName = ConstantEOKB.TableName.EKB_T_DECRYPT_INFO;
		}

		// 投标人扩展信息
		String vjson = null;
		// 投标人签到信息
		JSONArray sceneSing = null;
		JSONObject sceSing = null;
		// 投标人唱标信息
		JSONArray objSing = null;
		JSONObject sing = null;

		// 统计到场企业数量
		int count = 0;
		// 投标人
		logger.debug(LogUtils.format("开始获取项目经理的签到信息", data));
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", SessionUtils.getTPID());
			param.setColumn("sectionId", section.getColumn("V_BID_SECTION_ID"));
			bidders = this.activeRecordDAO.auto().table(tableName)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sectionId}")
					.addSortOrder("N_SORT_FILE_BID", "ASC").list(param);

			for (Record<String, Object> bidder : bidders)
			{
				vjson = bidder.getString("V_JSON_OBJ");
				JSONObject jobj = JSON.parseObject(vjson);
				sceneSing = jobj.getJSONArray("sceneSing");
				if (!StringUtils.isEmpty(jobj.getString("sceneSing")))
				{
					for (int i = 0; i < sceneSing.size(); i++)
					{
						sceSing = sceneSing.getJSONObject(i);
						if (sceSing == null)
						{
							continue;
						}
						if (StringUtils.equals(
								sceSing.getString("managerArrive"), "到达")
								&& StringUtils.equals(
										sceSing.getString("personType"), "到达"))
						{
							count++;
						}
						bidder.putAll(sceSing);
					}
				}
				objSing = jobj.getJSONArray("objSing");
				if (!StringUtils.isEmpty(jobj.getString("objSing")))
				{
					for (int i = 0; i < objSing.size(); i++)
					{
						sing = objSing.getJSONObject(i);
						bidder.putAll(sing);
					}
				}
			}
			section.setColumn("TENDER_LIST", bidders);
			section.setColumn("PRESENT_NUMBER", count);
		}
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		model.put("SECTION_LIST", sections);
		logger.debug(LogUtils.format("成功获取项目经理的签到信息", data));
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + SessionUtils.getTenderProjectTypeCode()
				+ "/firstEnvelope/signin.pm.html", model);
	}

	/**
	 * 
	 * 保存投标人签到情况<br/>
	 * <p>
	 * 保存投标人签到情况
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "/saveBidders", desc = "保存投标人签到情况")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void saveSign(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("保存投标人签到情况", data));

		String json = data.getParam("sign");
		JSONObject jobj = null;
		Record<String, Object> bidder = null;
		Record<String, Object> param = new RecordImpl<String, Object>();
		JSONObject jsonObj = null;
		JSONArray jsonArry = new JSONArray();

		String tableName = null;
		if (!SessionUtils.isSectionGroup())
		{
			tableName = ConstantEOKB.TableName.EKB_T_TENDER_LIST;
		}
		else
		{
			tableName = ConstantEOKB.TableName.EKB_T_DECRYPT_INFO;
		}

		// 投标人签到信息
		JSONObject remark = new JSONObject();
		if (StringUtils.isNotEmpty(json))
		{
			JSONArray arr = JSON.parseArray(json);
			for (int i = 0; i < arr.size(); i++)
			{
				jobj = arr.getJSONObject(i);
				for (Entry<String, Object> entry : jobj.entrySet())
				{
					param.clear();
					// 获取投标人ID
					String id = entry.getKey();
					param.setColumn("ID", id);
					logger.debug(LogUtils.format("获取投标人信息", param));
					bidder = this.activeRecordDAO.auto()
							.table(ConstantEOKB.TableName.EKB_T_TENDER_LIST)
							.get(param);
					if (!CollectionUtils.isEmpty(bidder))
					{
						jsonObj = JSONObject.parseObject(bidder
								.getString("V_JSON_OBJ"));
						jsonObj.remove("sceneSing");
						jsonArry.clear();

						JSONObject jsonVal = (JSONObject) entry.getValue();
						// 去除无用json
						jsonVal.remove("sectionName");
						jsonVal.remove("bidderName");

						jsonArry.add(jsonVal);
						jsonObj.put("sceneSing", jsonArry);

						// 先把去除备注信息
						jsonObj.remove("remark");

						param.clear();
						if (StringUtils.equals(
								jsonVal.getString("managerArrive"), "未到")
								|| StringUtils.equals(
										jsonVal.getString("personType"), "未到"))
						{
							param.setColumn("N_ENVELOPE_0", 0);
							remark.clear();
							// 信息回填到备注
							remark.put("firstRemark", "自动放弃投标资格，不予以唱标（人员未到会）");
							jsonObj.put("remark", remark);
						}
						else
						{
							param.setColumn("N_ENVELOPE_0", 1);
						}
						param.setColumn("ID", id);
						param.setColumn("V_JSON_OBJ", jsonObj.toString());
						this.activeRecordDAO.auto().table(tableName)
								.modify(param);
					}
				}
			}
		}
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
		// 获取投标人
		List<Record<String, Object>> bidders = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid}")
				.addSortOrder("V_BIDDER_NO", "ASC").list(param);

		String fileName = "投标人名单";
		// 输出文件名
		String outFileName = fileName + ".xls";

		HttpServletResponse response = data.getHttpServletResponse();
		String mimeType = AeolusDownloadUtils.getMimeType(outFileName);
		response.setContentType(mimeType);

		response.setHeader("Content-Disposition", "attachment;filename="
				+ AeolusDownloadUtils.encodeFileName(data, outFileName));

		InputStream input = null;
		OutputStream out = null;

		// 联合体投标列表
		List<Record<String, Object>> unionContactList = new ArrayList<Record<String, Object>>();
		// union_enterprise_name 判断该唱标字段是否有值
		JSONObject jobj = null;
		JSONArray jarr = null;
		for (Record<String, Object> bidder : bidders)
		{
			jobj = bidder.getJSONObject("V_JSON_OBJ");
			if (!CollectionUtils.isEmpty(jobj))
			{
				jarr = jobj.getJSONArray("objSing");
				for (int i = 0; i < jarr.size(); i++)
				{
					jobj = jarr.getJSONObject(i);
					if (StringUtils.isNotBlank(jobj
							.getString("union_enterprise_name"))
							&& jobj.getString("union_enterprise_name").trim()
									.length() > 2)
					{
						unionContactList
								.add(new RecordImpl<String, Object>().setColumn(
										"V_BIDDER_NAME",
										jobj.getString("union_enterprise_name")));
						break;
					}
				}
			}
		}
		// 合并联合体投标
		bidders.addAll(unionContactList);

		param.setColumn("bidders", bidders);

		try
		{
			input = ClassLoaderUtils.getResourceAsStream(
					"/com/sozone/eokb/bus/signinPm/bidder_info_template.xls",
					SigninPm.class);
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
}
