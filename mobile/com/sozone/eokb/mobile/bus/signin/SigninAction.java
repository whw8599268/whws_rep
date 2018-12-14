/**
 * 包名：com.sozone.eokb.common.bus.signin;
 * 文件名：SigninAction.java<br/>
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
package com.sozone.eokb.mobile.bus.signin;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.validate.RecordValidator;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 签到服务接口<br/>
 * Time：2017-7-25 下午7:11:15<br/>
 * 
 * @author jack
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/mobile/signin", desc = "签到服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class SigninAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(SigninAction.class);

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
	@Path(value = "/phone", desc = "保存用户联系电话信息")
	@Service
	public void saveBidderPhone(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("保存用户联系电话信息", data));
		Record<String, Object> param = data.getRecord();
		String tpID = param.getString("tpID");
		if (StringUtils.isEmpty(tpID))
		{
			throw new FacadeException("", "获取招标项目ID失败");
		}

		// 用户表ID
		String caID = ApacheShiroUtils.getCurrentUserID();

		// 先删除
		this.activeRecordDAO.pandora()
				.DELETE_FROM(TableName.EKB_T_BIDDER_PHONE)
				.EQUAL("V_CA_ID", caID).EQUAL("V_TPID", tpID).excute();

		// 去除电话号码的空格
		param.setColumn("V_BIDDER_PHONE",param.getString("V_BIDDER_PHONE").replaceAll(" ", ""));
		param.remove("tpID");
		param.setColumn("ID", SZUtilsID.getUUID());
		param.setColumn("V_TPID", tpID);
		param.setColumn("V_CA_ID", caID);
		param.setColumn("V_ORG_CODE", SessionUtils.getEntUniqueCode());
		param.setColumn("V_ENTERPRIS_NAME", SessionUtils.getLoginName());
		this.activeRecordDAO.pandora()
				.INSERT_INTO(TableName.EKB_T_BIDDER_PHONE).VALUES(param)
				.excute();

		// 再签到
		// 判断签到表是否有签到信息
		long count = this.activeRecordDAO.pandora()
				.SELECT_COUNT_FROM(TableName.EKB_T_SIGN_IN)
				.EQUAL("V_TPID", tpID)
				.EQUAL("V_SIGN_IN_CERTIFICATE", SessionUtils.getLoginName())
				.count();
		if (count < 1)
		{
			param.clear();
			// 签到表插入一条数据
			param.setColumn("ID", SZUtilsID.getUUID());
			param.setColumn("V_TPID", tpID);
			param.setColumn("V_SIGN_IN_CERTIFICATE",
					SessionUtils.getLoginName());
			param.setColumn("V_ORG_CODE", SessionUtils.getEntUniqueCode());
			param.setColumn("V_UNIFY_CODE", SessionUtils.getSocialcreditNO());
			param.setColumn("V_BIDDER_NAME", SessionUtils.getCompanyName());
			param.setColumn("V_SIGN_IN_TIME",
					DateUtils.getDate("yyyy-MM-dd HH:mm:ss"));
			RecordValidator.validateRecord(TableName.EKB_T_SIGN_IN, param);
			this.activeRecordDAO.pandora().INSERT_INTO(TableName.EKB_T_SIGN_IN)
					.VALUES(param).excute();
		}
	}

	/**
	 * 签到<br/>
	 * <p>
	 * 签到
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "", desc = "签到")
	@Service
	public void doSignIn(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("签到", data));
		Record<String, Object> param = data.getRecord();
		String tpID = param.getString("tpID");
		if (StringUtils.isEmpty(tpID))
		{
			throw new FacadeException("", "获取招标项目ID失败");
		}

		// 判断签到表是否有签到信息
		long count = this.activeRecordDAO.pandora()
				.SELECT_COUNT_FROM(TableName.EKB_T_SIGN_IN)
				.EQUAL("V_TPID", tpID)
				.EQUAL("V_SIGN_IN_CERTIFICATE", SessionUtils.getLoginName())
				.count();
		if (count < 1)
		{
			// 签到表插入一条数据
			param.setColumn("ID", SZUtilsID.getUUID());
			param.setColumn("V_TPID", tpID);
			param.setColumn("V_SIGN_IN_CERTIFICATE",
					SessionUtils.getLoginName());
			param.setColumn("V_UNIFY_CODE", SessionUtils.getSocialcreditNO());
			param.setColumn("V_BIDDER_NAME", SessionUtils.getCompanyName());
			param.setColumn("V_SIGN_IN_TIME",
					DateUtils.getDate("yyyy-MM-dd HH:mm:ss"));
			RecordValidator.validateRecord(TableName.EKB_T_SIGN_IN, param);
			this.activeRecordDAO.pandora().INSERT_INTO(TableName.EKB_T_SIGN_IN)
					.VALUES(param).excute();
		}
	}
}
