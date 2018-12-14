/**
 * 包名：com.sozone.ebidkp.jt.fjs.ljsg.hldjf.modelCheck
 * 文件名：ModelCheck.java<br/>
 * 创建时间：2017-8-11 下午1:53:08<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.bus.checkmodel;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 组件检查接口<br/>
 * <p>
 * 组件检查接口<br/>
 * </p>
 * Time：2017-8-11 下午1:53:08<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/mobile/modelCheck", desc = "组件检查接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class CheckModelAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(CheckModelAction.class);

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
	 * 获取确认状态值<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 状态值
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/status", desc = "获取确认状态值")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ResultVO<Record<String, Object>> getConfirmStatus(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取确认状态值", data));
		String tpID = data.getParam("tpID");
		String modelType = data.getParam("modelType");
		Record<String, Object> record = this.activeRecordDAO.pandora()
				.SELECT("V_STATUS,N_CONFIRM_TIME").FROM("EKB_T_CHECK_MODEL")
				.EQUAL("V_TPID", tpID).EQUAL("V_MODEL_TYPE", modelType).get();

		ResultVO<Record<String, Object>> result = new ResultVO<Record<String, Object>>();

		// 如果不存在
		if (CollectionUtils.isEmpty(record))
		{
			result.setSuccess(false);
			return result;
		}
		// 如果存在
		record.setColumn("NOW_TIME", System.currentTimeMillis());
		result.setSuccess(true);
		result.setResult(record);
		return result;
	}

	/**
	 * 
	 * 更新确认<br/>
	 * <p>
	 * 更新确认
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/confirm", desc = "更新确认")
	@HttpMethod(HttpMethod.POST)
	public void updateConfirm(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("修改信息", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		Record<String, Object> checkDataRecord = new RecordImpl<String, Object>();
		String tpid = data.getParam("tpID");
		String busID = data.getParam("bidderID");
		String modelType = data.getParam("modelType");
		String remark = data.getParam("remark");
		String status = data.getParam("status");
		String groupCode = data.getParam("groupCode");
		String companyName = SessionUtils.getCompanyName();
		int count = 0;
		if (StringUtils.isNotEmpty(groupCode))// 标段组
		{
			List<Record<String, Object>> list = activeRecordDAO.sql(
					"SELECT * FROM EKB_T_DECRYPT_INFO WHERE V_TPID='" + tpid
							+ "' AND V_BID_SECTION_GROUP_CODE='" + groupCode
							+ "' AND V_BIDDER_NAME='" + companyName + "'")
					.list();
			for (Record<String, Object> r : list)
			{
				param = new RecordImpl<String, Object>();
				param.setColumn("V_BUSNAME", modelType);
				param.setColumn("V_BUSID", r.getString("ID"));
				param.setColumn("V_STATUS", status);
				param.setColumn("V_REMARK", remark);
				// 预防插入重复数据
				count = this.activeRecordDAO.auto()
						.table(ConstantEOKB.TableName.EKB_T_CHECK_DATA)
						.setCondition("AND", "V_TPID=#{V_TPID}")
						.setCondition("AND", "V_BUSNAME=#{V_BUSNAME}")
						.setCondition("AND", "V_BUSID=#{V_BUSID}").count(param);
				if (count < 1)
				{
					checkDataRecord = new RecordImpl<String, Object>();
					checkDataRecord.setColumn("ID", Random.generateUUID());
					checkDataRecord.setColumn("V_TPID", tpid);
					checkDataRecord.setColumn("V_BUSNAME", modelType);
					checkDataRecord.setColumn("V_BUSID", r.getString("ID"));
					checkDataRecord.setColumn("V_STATUS", status);
					checkDataRecord.setColumn("V_REMARK", remark);
					this.activeRecordDAO.auto()
							.table(ConstantEOKB.TableName.EKB_T_CHECK_DATA)
							.save(checkDataRecord);
				}
			}
		}
		else if (StringUtils.isEmpty(groupCode))// 非标段组
		{
			param = new RecordImpl<String, Object>();
			param.setColumn("V_BUSNAME", modelType);
			param.setColumn("V_BUSID", busID);
			param.setColumn("V_STATUS", status);
			param.setColumn("V_REMARK", remark);
			// 预防插入重复数据
			count = this.activeRecordDAO.auto()
					.table(ConstantEOKB.TableName.EKB_T_CHECK_DATA)
					.setCondition("AND", "V_TPID=#{V_TPID}")
					.setCondition("AND", "V_BUSNAME=#{V_BUSNAME}")
					.setCondition("AND", "V_BUSID=#{V_BUSID}").count(param);
			if (count < 1)
			{
				checkDataRecord = new RecordImpl<String, Object>();
				checkDataRecord.setColumn("ID", Random.generateUUID());
				checkDataRecord.setColumn("V_TPID", tpid);
				checkDataRecord.setColumn("V_BUSNAME", modelType);
				checkDataRecord.setColumn("V_BUSID", busID);
				checkDataRecord.setColumn("V_STATUS", status);
				checkDataRecord.setColumn("V_REMARK", remark);
				this.activeRecordDAO.auto()
						.table(ConstantEOKB.TableName.EKB_T_CHECK_DATA)
						.save(checkDataRecord);
			}
		}
	}

}
