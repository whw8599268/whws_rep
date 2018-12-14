/**
 * 包名：com.sozone.eokb.xms_szgc_taxxm_tyb_v1
 * 文件名：Prebid.java<br/>
 * 创建时间：2018-8-6 下午7:55:35<br/>
 * 创建者：wengdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.xms_szgc_taxxm_tyb_v1;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 开标准备<br/>
 * <p>
 * 开标准备<br/>
 * </p>
 * Time：2018-8-6 下午7:55:35<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/xms_szgc_taxxm_tyb_v1/prebid", desc = "评标前准备服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class Prebid
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(FirstEnvelope.class);

	/**
	 * 项目CODE
	 */
	private static String projectCode = ConstantEOKB.EOKBBemCode.XMS_SZGC_TAXXM_TYB_V1;

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
	 * 抽取球号的页面<br/>
	 * <p>
	 * 抽取球号的页面
	 * </p>
	 * 
	 * @param data
	 *            data
	 * @param tpnoid
	 *            流程节点ID
	 * @return 抽取球号的页面
	 * @throws ServiceException
	 *             服务异常
	 */
	@Path(value = "oerv/{tpnoid}", desc = "打开抽取球号的页面")
	@Service
	public ModelAndView openExtractRollView(AeolusData data,
			@PathParam("tpnoid") String tpnoid) throws ServiceException
	{
		logger.debug(LogUtils.format("打开抽取球号的页面", data, tpnoid));
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tpnoid);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);
		ModelMap model = new ModelMap();
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		List<Record<String, Object>> bidders = this.activeRecordDAO.statement()
				.selectList("xms_fjsz_common.getBidderNoInfo", param);
		if (CollectionUtils.isEmpty(bidders))
		{
			throw new ServiceException("", "未获取到投标人信息");
		}

		Record<String, Object> section = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").get(param);

		if (CollectionUtils.isEmpty(section))
		{
			throw new ServiceException("", "未获取到标段信息");
		}
		model.put("V_BID_SECTION_NAME", section.getString("V_BID_SECTION_NAME"));
		model.put("BIDDERS", bidders);
		return new ModelAndView("/xms_szgc/eokb/" + projectCode
				+ "/prebid/extract.bidder.no.html", model);
	}

	/**
	 * 
	 * 抽取球号的页面<br/>
	 * <p>
	 * 抽取球号的页面
	 * </p>
	 * 
	 * @param data
	 *            data
	 * @param tpnoid
	 *            流程节点ID
	 * @return 抽取球号的页面
	 * @throws ServiceException
	 *             服务异常
	 */
	@Path(value = "oerrv/{tpnoid}", desc = "打开抽取球号的页面")
	@Service
	public ModelAndView openExtractRollResultView(AeolusData data,
			@PathParam("tpnoid") String tpnoid) throws ServiceException
	{
		logger.debug(LogUtils.format("打开抽取球号的页面", data, tpnoid));
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpnoid", tpnoid);
		param.setColumn("tpid", tpid);
		Record<String, Object> tpNode = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_FLOW_VIEW)
				.setCondition("AND", "ID = #{tpnoid}").get(param);
		ModelMap model = new ModelMap();
		model.put("TENDER_PROJECT_FLOW_NODE", tpNode);
		List<Record<String, Object>> bidders = this.activeRecordDAO.statement()
				.selectList("xms_fjsz_common.getBidderNoInfo", param);
		if (CollectionUtils.isEmpty(bidders))
		{
			throw new ServiceException("", "未获取到投标人信息");
		}

		Record<String, Object> section = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{tpid}").get(param);

		if (CollectionUtils.isEmpty(section))
		{
			throw new ServiceException("", "未获取到标段信息");
		}
		model.put("V_BID_SECTION_NAME", section.getString("V_BID_SECTION_NAME"));
		model.put("BIDDERS", bidders);
		return new ModelAndView("/xms_szgc/eokb/" + projectCode
				+ "/prebid/extract.bidder.no.result.html", model);
	}

	/**
	 * 
	 * 打开抽取球号结果的页面<br/>
	 * <p>
	 * 打开抽取球号结果的页面
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param id
	 *            投标人主键
	 * @param no
	 *            球号
	 * @throws ServiceException
	 *             服务异常
	 */
	@Path(value = "sbn/{id}/{no}", desc = "打开抽取球号结果的页面")
	@Service
	public void saveBidderNo(AeolusData data, @PathParam("id") String id,
			@PathParam("no") String no) throws ServiceException
	{
		logger.debug(LogUtils.format("打开抽取球号结果的页面", data, id, no));

		Record<String, Object> bidderNo = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		bidderNo.setColumn("V_TPID", tpid);
		bidderNo.setColumn("V_BIDDER_NO", no);
		// 验证是否球号已存在
		int count = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_BIDDER_NO)
				.setCondition("AND",
						"V_TPID=#{V_TPID} AND V_BIDDER_NO=#{V_BIDDER_NO}")
				.count(bidderNo);
		if (count > 0)
		{
			throw new ServiceException("", "球号" + no + "重复录入！");
		}

		bidderNo.setColumn("ID", SZUtilsID.getUUID());
		bidderNo.setColumn("V_BIDDER_ID", id);
		bidderNo.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		this.activeRecordDAO.auto().table(TableName.EKB_T_BIDDER_NO)
				.save(bidderNo);
	}

	/**
	 * 
	 * 清空投标人球号<br/>
	 * <p>
	 * 清空投标人球号
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param id
	 *            投标人主键
	 * @throws ServiceException
	 *             服务异常
	 */
	@Path(value = "remove/{id}", desc = "")
	@Service
	public void removeBidderNo(AeolusData data, @PathParam("id") String id)
			throws ServiceException
	{
		logger.debug(LogUtils.format("清空投标人球号", data, id));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("bidderId", id);
		this.activeRecordDAO.auto().table(TableName.EKB_T_BIDDER_NO)
				.setCondition("AND", "V_BIDDER_ID=#{bidderId}").remove(param);
	}
}
