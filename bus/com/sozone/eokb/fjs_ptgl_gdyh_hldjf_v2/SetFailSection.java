/**
 * 包名：com.sozone.eokb.fjs_ptgl_gcsg_hldjf_v1
 * 文件名：SetFailSection.java<br/>
 * 创建时间：2017-11-3 下午3:55:35<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_ptgl_gdyh_hldjf_v2;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 普通公路工程施工合理低价法-第二信封流标设置<br/>
 * <p>
 * 普通公路工程施工合理低价法-第二信封流标设置<br/>
 * </p>
 * Time：2017-11-3 下午3:55:35<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/fjs_ptgl_gdyh_hldjf_v2/sfs", desc = "设置流标服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class SetFailSection extends BaseAction
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(SetFailSection.class);

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
	 * 获取各标段第一信封评标情况<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 各标段第一信封评标情况
	 * @throws FacadeException
	 *             服务异常
	 */
	// 定义路径
	@Path(value = "", desc = "获取各标段第一信封评标情况")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getSectionReviewInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取各标段第一信封评标情况", data));
		String tpid = SessionUtils.getTPID();
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", tpid);
		boolean group = SessionUtils.isSectionGroup();
		// 如果是标段组的情况
		if (group)
		{
			return this.activeRecordDAO.statement().selectList(
					"fjs_ptgl_gcsg_hldjf_v1_secondenvelopeSetFailBid.getSectionReviewInfo_Group", params);
		}
		return this.activeRecordDAO.statement().selectList(
				"fjs_ptgl_gcsg_hldjf_v1_secondenvelopeSetFailBid.getSectionReviewInfo", params);

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
		if (CollectionUtils.isEmpty(sectionIDs))
		{
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
					section.setColumn("sid", sid);
					this.activeRecordDAO.auto()
							.table(TableName.EKB_T_SECTION_INFO)
							.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
							.setCondition("AND", "V_TPID = #{tpid}")
							.modify(section);
				}
			}
		}
	}

}
