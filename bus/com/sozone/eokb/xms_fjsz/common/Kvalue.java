/**
 * 包名：com.sozone.eokb.bus
 * 文件名：Kvalue.java<br/>
 * 创建时间：2018-10-19 上午11:54:12<br/>
 * 创建者：wengdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.xms_fjsz.common;

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
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 抽取K值服务类<br/>
 * <p>
 * 抽取K值服务类<br/>
 * </p>
 * Time：2018-10-19 上午11:54:12<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "kvalue", desc = "抽取K值服务类")
@Permission(Level.Authenticated )
public class Kvalue extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Kvalue.class);

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
	 * 抽取K值<br/>
	 * <p>
	 * 抽取K值
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
	@Path(value = "/chouquK/flow/{tpnid}", desc = "抽取K值")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView chouquK(@PathParam("tpnid") String tenderProjectNodeID,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("抽取K值", data));
		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		String status = data.getParam("status");
		if (StringUtils.isEmpty(status))
		{
			status = "";
		}
		param.setColumn("type", ConstantEOKB.K_VALUE + status);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		Record<String, Object> section = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").get(param);
		JSONObject jobj = section.getJSONObject("V_JSON_OBJ");
		if (CollectionUtils.isEmpty(jobj))
		{
			throw new FacadeException("", "获取标段扩展信息失败");
		}
		Double maxK = jobj.getDouble("MAX_K");
		Double minK = jobj.getDouble("MIN_K");
		if (null == maxK || null == minK)
		{
			throw new FacadeException("", "无法获取到k值信息");
		}
		// 查询k值JSON
		List<String> kJsons = this.activeRecordDAO.statement().loadList(
				"xms_fjsz_common.getKvalue", param);
		// 如果k值JSON为空,说明是初始化数据
		if (CollectionUtils.isEmpty(kJsons))
		{
			if (StringUtils.equals("1", status))
			{
				kJsons = FjszUtils.initDataK1(tpid, tenderProjectNodeID, maxK,
						minK);
			}
			else if (StringUtils.equals("2", status))
			{
				kJsons = FjszUtils.initDataK2(tpid, tenderProjectNodeID, maxK,
						minK);
			}
			else if (StringUtils.equals("3", status))
			{
				kJsons = FjszUtils.initDataK3(tpid, tenderProjectNodeID, maxK,
						minK);
			}
			else
			{
				kJsons = FjszUtils.initDataK4(tpid, tenderProjectNodeID, maxK,
						minK);
			}

		}

		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		model.put("KINFOS", FjszUtils.getKValueView(kJsons));
		model.put("MAX_K", maxK);
		model.put("MIN_K", minK);
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/k_value/chouquK" + (status == null ? "" : status)
				+ ".html", model);
	}

	/**
	 * 保存摇号结果<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            招标项目流程节点ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "save/{tpnid}", desc = "保存摇号结果")
	// PUT访问方式
	@HttpMethod(HttpMethod.POST)
	@Handler(OperationLogHandler.class)
	public void modifyKvalue(@PathParam("tpnid") String tenderProjectNodeID,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存摇号结果", tenderProjectNodeID, data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnid", tenderProjectNodeID);
		String tpid = SessionUtils.getTPID();
		if (StringUtils.isEmpty(tpid))
		{
			throw new ServiceException("", "找不到招标项目ID");
		}
		param.setColumn("tpid", tpid);
		// 各标段列表
		List<JSONObject> sms = data.getParams("SECTION_METHODS");
		if (!CollectionUtils.isEmpty(sms))
		{
			String sectionID = null;
			List<String> indexNOs = new ArrayList<String>();
			String yhno = null;
			String indexNO = null;
			String cv = null;
			Record<String, Object> record = null;
			Record<String, Object> temp = new RecordImpl<String, Object>();
			boolean empty = true;
			for (JSONObject hw : sms)
			{
				temp.clear();
				indexNOs.clear();
				sectionID = hw.getString("V_BID_SECTION_ID");
				param.setColumn("sid", sectionID);
				empty = true;
				// 如果有值需要修改
				record = this.activeRecordDAO.auto()
						.table(TableName.TENDER_PROJECT_NODE_DATA)
						.setCondition("AND", "V_TPID = #{tpid}")
						.setCondition("AND", "V_TPFN_ID = #{tpnid}")
						.setCondition("AND", "V_BUS_ID = #{sid}").get(param);
				if (CollectionUtils.isEmpty(record))
				{
					throw new ServiceException("", "未查询到K值初始化信息");
				}
				String json = record.getString("V_JSON_OBJ");
				JSONObject jobj = JSON.parseObject(json);
				JSONObject cinfo = jobj.getJSONObject("K_INFO");
				if (null == cinfo || cinfo.isEmpty())
				{
					throw new ServiceException("", "无法获取k值信息!");
				}
				JSONArray vsArray = cinfo.getJSONArray("K_VALUES");
				for (int i = 0; i <= vsArray.size(); i++)
				{
					indexNO = hw.getString(i + "");
					if (StringUtils.isNotEmpty(indexNO))
					{
						indexNOs.add(indexNO);
						empty = false;
						continue;
					}
					indexNOs.add("");
				}

				yhno = hw.getString("YAOHAO_RESULT");
				// 如果是空对象
				if (empty && StringUtils.isEmpty(yhno))
				{
					continue;
				}
				cv = hw.getString(ConstantEOKB.K_VALUE);

				if (!CollectionUtils.isEmpty(record))
				{

					JSONObject vl = null;
					for (int i = 0; i < vsArray.size(); i++)
					{
						vl = vsArray.getJSONObject(i);
						vl.put("NO", indexNOs.get(i));
					}
					jobj.put("YAOHAO_NO", yhno);
					jobj.put(ConstantEOKB.K_VALUE, cv);
					temp.setColumn("ID", record.getString("ID"));
					temp.setColumn("V_JSON_OBJ", jobj.toJSONString());
					this.activeRecordDAO.auto()
							.table(TableName.TENDER_PROJECT_NODE_DATA)
							.modify(temp);
				}
			}
		}
	}
}
