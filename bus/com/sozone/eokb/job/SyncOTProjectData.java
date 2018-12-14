/**
 * 包名：com.sozone.eokb.job
 * 文件名：SyncOTProjectData.java<br/>
 * 创建时间：2018-2-27 下午1:38:49<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.job;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.eokb.common.ConstantEOKB.TENDERPROJECT_APP_TYPE;
import com.sozone.eokb.common.ConstantEOKB.TableName;

/**
 * 同步数据服务接口<br/>
 * <p>
 * 同步数据服务接口<br/>
 * </p>
 * Time：2018-2-27 下午1:38:49<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/syncotpd", desc = "同步数据服务接口")
public class SyncOTProjectData
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(SyncOTProjectData.class);

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
	 * 获取符合搜索条件的招标项目信息分页封装<br/>
	 * <p>
	 * 已经同步的招标项目信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页请求封装
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "tps", desc = "获取符合搜索条件的招标项目信息分页封装")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getTenderProjectsPage(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取符合搜索条件的招标项目信息分页封装", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = data.getRecord();
		return this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO)
				.addSortOrder("V_BIDOPEN_TIME", "DESC")
				.addSortOrder("V_INVITENOTRUE", "DESC").page(pageable, param);
	}

	/**
	 * 测试同步交易平台数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页请求封装
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "test", desc = "测试同步交易平台数据")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getTenderProjects(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("测试同步交易平台数据", data));
		// 获取招标项目的类型
		String tenderProType = data.getParam("V_TENDERPROJECT_APP_TYPE");
		// 开标地区代码
		String areaCode = data.getParam("V_JYCS");
		// 开标会议室NO
		String roomNO = data.getParam("V_ROOM_ID");
		// 开标时间
		String openBidTime = data.getParam("V_BIDOPEN_TIME");
		List<Record<String, Object>> result = null;
		if (StringUtils.isEmpty(tenderProType))
		{
			return Collections.emptyList();
		}
		Record<String, String> params = new RecordImpl<String, String>();
		params.setColumn("OPENING_BID_TIME", openBidTime);
		params.setColumn("APP_TYPE", tenderProType);
		// 开标区域
		params.setColumn("AREA_CODE", areaCode);
		// 会议室
		params.setColumn("SITEA", roomNO);
		// 如果是厦门市房建市政
		if (TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE.equals(tenderProType)
				|| TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE.equals(tenderProType))
		{

			result = XiaMenTPDataService.getOpenBidProList(params,
					tenderProType, this.activeRecordDAO);
			if (null == result)
			{
				return Collections.emptyList();
			}
			return result;
		}
		// 否则调用高速
		result = GsPtSyTPDataService.getOpenBidProList(params, tenderProType,
				this.activeRecordDAO);
		if (null == result)
		{
			return Collections.emptyList();
		}
		return result;
	}

	/**
	 * 删除招标项目数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "rm/{tpid}", desc = "删除招标项目数据")
	// GET访问方式
	@HttpMethod(HttpMethod.DELETE)
	public void removeTenderProject(@PathParam("tpid") String tpid,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("删除招标项目数据", tpid, data));
		Record<String, Object> param = new RecordImpl<String, Object>()
				.setColumn("tpid", tpid);
		// 判断 是否已经有开标的标段了
		int count = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_BID_OPEN_STATUS !=0")
				.setCondition("AND", "V_TPID = #{tpid}").count(param);
		// 如果有不能删除
		if (count != 0)
		{
			throw new ServiceException("", "当前招标项目已经开标了不能删除!");
		}
		// 先删除标段
		this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}").remove(param);
		// 再删除招标项目
		this.activeRecordDAO.auto().table(TableName.EKB_T_TENDER_PROJECT_INFO)
				.remove(tpid);
	}

	/**
	 * 同步交易平台数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             自定义Facade异常
	 */
	// 定义路径
	@Path(value = "sync", desc = "同步交易平台数据")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void syncTenderProjects(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("测试同步交易平台数据", data));
		// 获取招标项目的类型
		String tenderProType = data.getParam("V_TENDERPROJECT_APP_TYPE");
		// 开标地区代码
		String areaCode = data.getParam("V_JYCS");
		// 开标会议室NO
		String roomNO = data.getParam("V_ROOM_ID");
		// 开标时间
		String openBidTime = data.getParam("V_BIDOPEN_TIME");
		// id
		String tpids = data.getParam("V_TENDER_PROJECT_IDS");
		// 如果是厦门市房建市政
		if (TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE.equals(tenderProType)
				|| TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE.equals(tenderProType))
		{

			XiaMenTPDataService.synchronizeData(openBidTime, tenderProType, "",
					areaCode, roomNO, tpids);
			return;
		}
		// 否则调用高速
		GsPtSyTPDataService.synchronizeData(openBidTime, tenderProType, "",
				areaCode, roomNO, tpids);
	}

}
