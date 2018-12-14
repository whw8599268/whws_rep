/**
 * 包名：com.sozone.eokb.bus.bidcheck
 * 文件名：BidCheckAction.java<br/>
 * 创建时间：2018-6-4 下午4:34:38<br/>
 * 创建者：wemgdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.bidcheck;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;

/**
 * 开标环境检查服务<br/>
 * <p>
 * 开标环境检查服务<br/>
 * </p>
 * Time：2018-6-4 下午4:34:38<br/>
 * 
 * @author wemgdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/bca", desc = "开标环境检查服务")
// 登录即可访问
@Permission(Level.Authenticated)
public class BidCheckAction extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(BidCheckAction.class);

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
	 * 检测开标环境<br/>
	 * <p>
	 * 检测开标环境
	 * </p>
	 * 
	 * @param id
	 *            招标项目主键
	 * @param data
	 *            data
	 * @return 检查报告
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "cbe/{id}", desc = "检测开标环境")
	@Service
	public ModelAndView checkBidEnvironment(@PathParam("id") String id,
			AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("检测开标环境", data));

		// 获取招标项目
		Record<String, Object> project = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_PROJECT_INFO).get(id);
		if (CollectionUtils.isEmpty(project))
		{
			throw new ServiceException("", "查询不到对应的招标项目");
		}

		// 获取全部的标段信息
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", id);
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").list(param);
		if (CollectionUtils.isEmpty(sections))
		{
			throw new ServiceException("", "获取标段信息失败");
		}

		ModelMap model = setCheckResult(project, sections);

		String appType = project.getString("V_TENDERPROJECT_APP_TYPE");
		String view = "";
		if (StringUtils.equals(appType,
				ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_GSGL_TYPE))
		{
			view = "bid.environment.check.gsgl.html";
		}
		if (StringUtils.equals(appType,
				ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_PTGL_TYPE))
		{
			view = "bid.environment.check.ptgl.html";
		}
		if (StringUtils.equals(appType,
				ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_SYGC_TYPE))
		{
			view = "bid.environment.check.ptgl.html";
		}
		if (StringUtils.equals(appType,
				ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE))
		{
			view = "bid.environment.check.fjsz.html";
		}
		if (StringUtils.equals(appType,
				ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE))
		{
			view = "bid.environment.check.fjsz.html";
		}

		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/bidcheck/" + view, model);
	}

	/**
	 * 
	 * 设置检测结果<br/>
	 * 
	 * @param project
	 *            项目信息
	 * @param sections
	 *            标段列表
	 * @return 检测结果
	 * @throws ServiceException
	 *             ServiceException
	 */
	private ModelMap setCheckResult(Record<String, Object> project,
			List<Record<String, Object>> sections) throws ServiceException
	{
		logger.debug(LogUtils.format("设置检测结果", project, sections));

		// 开标文件存放根路径
		String kbRootPath = SystemParamUtils
				.getString(SysParamKey.EBIDKB_FILE_PATH_URL);
		// 投递文件存放根路径
		String tbRootPath = SystemParamUtils.getString(
				SysParamKey.EDE_ENTBID_FILE_PATH, "D:\fileEbid-fileTb");

		ModelMap model = new ModelMap();

		// 招标项目编号
		String projectCode = project.getString("V_TENDER_PROJECT_CODE");
		// 招标项目类型
		String appType = project.getString("V_TENDERPROJECT_APP_TYPE");
		// 招标编号
		String inviteno = project.getString("V_INVITENOTRUE");

		// 招标文件检测结果
		model.put("BIDDING_DOCUMENTS",
				BidCheckUtils.checkBiddingDocuments(kbRootPath, projectCode));
		// 补遗文件检测结果
		model.put("ADDENDUM_FILE",
				BidCheckUtils.checkAddendumFile(kbRootPath, projectCode));
		// 控制价检测结果
		model.put("CONTROL_PRICE", BidCheckUtils.checkControlPrice(sections));
		// 高速公路不需要检测控制价文件
		if (!StringUtils.equals(appType,
				ConstantEOKB.TENDERPROJECT_APP_TYPE.FJS_GSGL_TYPE))
		{
			// 控制价文件检测结果
			model.put("CONTROL_PRICE_FILE", BidCheckUtils
					.checkControlPriceFile(kbRootPath, projectCode));
		}
		// 投标文件检测结果
		model.put("TB_FILE", BidCheckUtils.checkTbFiles(project, tbRootPath,
				inviteno, sections));
		// 磁盘空间
		model.put("DISK_FREE_SPACE", BidCheckUtils.checkDiskFreeSpace());
		// 交易平台URL连接检测结果
		model.put("EDE_URL", BidCheckUtils.checkEdeUrl(appType));

		// 开标视频是否可用
		int videoStatus = SystemParamUtils
				.getInteger(ConstantEOKB.SysParamKey.EOV_VIDEO_STATUS);
		model.put("VS", videoStatus);
		if (videoStatus == 1)
		{
			// 视频URL连接检测结果
			model.put("EOV_URL", BidCheckUtils.checkEovUrl());
			// CDN URL连接检测结果
			// model.put("CDN_URL", BidCheckUtils.checkCdnUrl());
			// 视频录制 URL连接检测结果
			model.put("REC_URL", BidCheckUtils.checkRecUrl());
		}
		return model;
	}
}
