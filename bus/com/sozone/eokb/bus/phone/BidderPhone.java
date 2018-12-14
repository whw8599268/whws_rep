/**
 * 包名：com.sozone.eokb.bus.phone
 * 文件名：Phone.java<br/>
 * 创建时间：2018-6-4 下午4:00:21<br/>
 * 创建者：wemgdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.phone;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.eokb.bus.project.Project;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 投标人联系电话服务类<br/>
 * <p>
 * 投标人联系电话服务类<br/>
 * </p>
 * Time：2018-6-4 下午4:00:21<br/>
 * 
 * @author wemgdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/bp", desc = "投标人联系电话服务类")
// 登录即可访问
@Permission(Level.Authenticated)
public class BidderPhone
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Project.class);

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
	 * 保存用户投标信息<br/>
	 * <p>
	 * 保存用户投标信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/sbp", desc = "保存用户联系电话信息")
	@HttpMethod(HttpMethod.POST)
	public void saveBidderPhone(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存用户联系电话信息", data));
		Record<String, Object> param = data.getRecord();
		String tpid = param.getString("tpid");
		if (StringUtils.isEmpty(tpid))
		{
			throw new FacadeException("", "获取招标项目ID失败");
		}

		// 用户表ID
		String caID = ApacheShiroUtils.getCurrentUserID();
		ApacheShiroUtils.getCurrentUser().getString("V_SOCIALCREDIT_NO");
		param.setColumn("caID", caID);
		// 先删除
		this.activeRecordDAO.auto().table(TableName.EKB_T_BIDDER_PHONE)
				.setCondition("AND", "V_CA_ID=#{caID}")
				.setCondition("AND", "V_TPID=#{tpid}").remove(param);

		param.setColumn("ID", SZUtilsID.getUUID());
		param.setColumn("V_TPID", tpid);
		param.setColumn("V_CA_ID", caID);
		param.setColumn("V_ORG_CODE", SessionUtils.getEntUniqueCode());
		param.setColumn("V_ENTERPRIS_NAME", SessionUtils.getLoginName());
		this.activeRecordDAO.auto().table(TableName.EKB_T_BIDDER_PHONE)
				.save(param);
	}

	/**
	 * 
	 * 获取用户联系电话信息列表<br/>
	 * <p>
	 * 获取用户联系电话信息列表
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 * @return Page
	 */
	@Path(value = "gbpl", desc = "获取用户联系电话信息列表")
	@Service
	public Page<Record<String, Object>> getBidderPhoneList(AeolusData data)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取用户联系电话信息列表", data));
		Record<String, Object> param = data.getRecord();
		Pageable pageable = data.getPageRequest();
		param.setColumn("tpid", SessionUtils.getTPID());

		return this.activeRecordDAO.auto().table(TableName.EKB_T_BIDDER_PHONE)
				.setCondition("AND", "V_TPID=#{tpid}").page(pageable, param);
	}
}
