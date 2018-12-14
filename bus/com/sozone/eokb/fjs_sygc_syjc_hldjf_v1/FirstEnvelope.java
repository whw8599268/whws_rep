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
package com.sozone.eokb.fjs_sygc_syjc_hldjf_v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.fjs_sygc.common.SygcUtils;
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
@Path(value = "/fjs_sygc_syjc_hldjf_v1/firstenvelope", desc = "第一信封解密服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class FirstEnvelope extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FirstEnvelope.class);

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
	 * 获取水运解密情况信息<br/>
	 * <p>
	 * 获取水运解密情况信息
	 * </p>
	 * 
	 * @param tenderProjectNodeID
	 *            流程节点主键
	 * @param data AeolusData
	 * @return ModelAndView
	 * @throws FacadeException FacadeException
	 */
	// 定义路径
	@Path(value = "/credit/flow/{tpnid}", desc = "获取水运解密情况信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getDecryptSituation(
			@PathParam("tpnid") String tenderProjectNodeID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取水运解密情况信息", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tenderProjectNodeID);
		param.setColumn("tpid", SessionUtils.getTPID());

		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);

		String tpid = SessionUtils.getTPID();

		// 解密家数
		model.put("YX_N", SygcUtils.getDecryptSuccessCount(tpid));

		// 第一数字信封解密视图
		model.put("SECTION_LIST", SygcUtils.getDecryptSituation(tpid));
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		String projectCode = SessionUtils.getTenderProjectTypeCode();
		// 招标人
		if (!SessionUtils.isBidder())
		{
			return new ModelAndView(getTheme(data.getHttpServletRequest())
					+ "/eokb/" + projectCode + "/bidOpen/credit.zbr.html",
					model);
		}
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/bidOpen/credit.tbr.html", model);
	}

	/**
	 * 
	 * 获取水运解密情况记录表<br/>
	 * <p>
	 * 获取水运解密情况记录表
	 * </p>
	 * 
	 * @param data AeolusData
	 * @return ModelAndView
	 * @throws FacadeException FacadeException
	 */
	// 定义路径
	@Path(value = "/record", desc = "获取水运解密情况记录表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getDecryptRecord(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取水运解密情况信息", data));
		ModelMap model = new ModelMap();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());
		String tpid = SessionUtils.getTPID();

		// 第一数字信封解密视图
		model.put("SECTION_LIST", SygcUtils.getDecryptSituation(tpid));
		String projectCode = SessionUtils.getTenderProjectTypeCode();
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/" + projectCode + "/bidOpen/credit.tbr.html", model);
	}
}
