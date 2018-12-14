/**
 * 包名：com.sozone.eokb.bus.electronics
 * 文件名：Electronics.java<br/>
 * 创建时间：2018-1-8 下午4:02:17<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.electronics;

import java.util.List;

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
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 电子摇号过程<br/>
 * <p>
 * 电子摇号过程<br/>
 * </p>
 * Time：2018-1-8 下午4:02:17<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/electronics", desc = "电子摇号过程服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class Electronics extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Electronics.class);

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
	 * 展示电子摇号过程列表<br/>
	 * <p>
	 * 展示电子摇号过程列表
	 * </p>
	 * 
	 * @param grouCode
	 *            标段组编号
	 * @param data
	 *            AeolusData
	 * @return ModelAndView
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "process/{groupCode}", desc = "展示电子摇号过程列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView showElectronisc(
			@PathParam("groupCode") String grouCode, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("展示电子摇号过程列表", grouCode, data));
		String tpid = SessionUtils.getTPID();
		ModelMap model = new ModelMap();

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("groupCode", grouCode);

		List<Record<String, Object>> tenders = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_ELECTRONICS)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_SECTION_GROUP_CODE = #{groupCode}")
				.addSortOrder("N_NUMBER", "ASC").list(param);
		if (CollectionUtils.isEmpty(tenders))
		{
			throw new FacadeException("", "查询不到投标人信息");
		}
		model.put("TENDER_LIST", tenders);
		logger.debug(LogUtils.format("获取电子摇号的投标人信息", tenders));
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/electronics/electronics.show.html", model);
	}
}
