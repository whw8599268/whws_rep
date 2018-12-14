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
package com.sozone.eokb.bus.signin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.dao.validate.RecordValidator;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.LogFormatUtils;
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
@Path(value = "/bus/signin", desc = "签到服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class Signin
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Signin.class);

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
	 * 投标人签到信息列表<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/list", desc = "投标人签到信息列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("投标人签到信息列表", data));
		String tpID = SessionUtils.getTPID();
		return this.activeRecordDAO.auto().table(TableName.EKB_T_SIGN_IN)
				.setCondition("AND", " V_TPID = '" + tpID + "'")
				.addSortOrder("V_SIGN_IN_TIME", "DESC")
				.page(data.getPageRequest(), data.getRecord());
	}

	/**
	 * 投标人签到<br/>
	 * <p>
	 * 投标人签到
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param data
	 *            AeolusData
	 * @return ResultVO
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/tbr/{tpid}", desc = "投标人签到")
	@HttpMethod(HttpMethod.GET)
	public ResultVO<String> signInByBidder(@PathParam("tpid") String tpid,
			AeolusData data) throws FacadeException
	{
		logger.debug(LogFormatUtils.formatOperateMessage("", "投标人签到", data));
		// Record<String, Object> param = data.getRecord();
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("TPID", tpid);
		record.setColumn("SIGN_IN_CERTIFICATE", SessionUtils.getLoginName());
		ResultVO<String> result = null;
		// 判断签到表是否有签到信息
		Record<String, Object> signInRecord = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_SIGN_IN)
				.setCondition("AND", "V_TPID = #{TPID}")
				.setCondition("AND",
						"V_SIGN_IN_CERTIFICATE = #{SIGN_IN_CERTIFICATE}")
				.get(record);
		if (CollectionUtils.isEmpty(signInRecord))
		{
			// 签到表插入一条数据
			Record<String, Object> recordInsert = new RecordImpl<String, Object>();
			recordInsert.setColumn("ID", SZUtilsID.getUUID());
			recordInsert.setColumn("V_SIGN_IN_CERTIFICATE",
					SessionUtils.getLoginName());
			recordInsert.setColumn("V_UNIFY_CODE",
					SessionUtils.getSocialcreditNO());
			recordInsert.setColumn("V_ORG_CODE", SessionUtils.getCompanyCode());
			recordInsert.setColumn("V_BIDDER_NAME",
					SessionUtils.getCompanyName());
			recordInsert.setColumn("V_SIGN_IN_TIME",
					DateUtils.getDate("yyyy-MM-dd HH:mm:ss"));
			recordInsert.setColumn("V_TPID", tpid);
			recordInsert.setColumn("N_CREATE_TIME", System.currentTimeMillis());
			recordInsert.setColumn("V_CREATE_USER",
					SessionUtils.getCompanyCode());
			RecordValidator.validateRecord(TableName.EKB_T_SIGN_IN,
					recordInsert);
			this.activeRecordDAO.auto().table(TableName.EKB_T_SIGN_IN)
					.save(recordInsert);
			result = new ResultVO<String>(true);
		}
		else
		{
			result = new ResultVO<String>(false);
		}
		return result;

	}
}
