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
package com.sozone.eokb.bus.checkModel;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.task.CreditTask;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.utils.TimerUtils;

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
@Path(value = "/bus/modelCheck", desc = "组件检查接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class CheckModel
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(CheckModel.class);

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
	 * @param mid
	 *            mid
	 * @return 状态值
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/status/{mid}", desc = "获取确认状态值")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ResultVO<Record<String, Object>> getStatus(
			@PathParam("mid") String mid) throws FacadeException
	{
		logger.debug(LogUtils.format("获取确认状态值", mid));
		String tpid = SessionUtils.getTPID();
		Record<String, Object> record = this.activeRecordDAO
				.sql("SELECT V_STATUS, V_TPID, N_CONFIRM_TIME FROM EKB_T_CHECK_MODEL WHERE V_TPID=#{tpid} AND V_MODEL_TYPE=#{mid}")
				.setParam("tpid", tpid).setParam("mid", mid).get();
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
	 * 修改数据重新确认<br/>
	 * <p>
	 * 修改数据重新确认
	 * </p>
	 * 
	 * @param type
	 *            类型
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/recomfirm/{type}", desc = "修改数据重新确认")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void reComfirm(@PathParam("type") String type)
			throws FacadeException
	{
		logger.debug(LogUtils.format("修改数据重新确认", type));
		String tpid = SessionUtils.getTPID();

		// 销毁定时器
		TimerUtils.closeTimer(tpid, CreditTask.class);

		// 删除数据确认表
		this.activeRecordDAO
				.sql("DELETE FROM EKB_T_CHECK_DATA WHERE V_BUSNAME=#{type} AND V_TPID=#{tpid}")
				.setParam("tpid", tpid).setParam("type", type).delete();
		// 删除模块确认表
		this.activeRecordDAO
				.sql("DELETE FROM EKB_T_CHECK_MODEL WHERE V_MODEL_TYPE=#{type} AND V_TPID=#{tpid}")
				.setParam("tpid", tpid).setParam("type", type).delete();

	}

	/**
	 * 进入数据确认环节<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/updateStatus", desc = "进入数据确认环节")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void dateConfirm(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("进入数据确认环节"), data);
		// 设置项目ID
		String tpid = SessionUtils.getTPID();
		String type = data.getParam("type");

		long end = ConstantEOKB.CONFIRM_DURATION;

		// 确认时间
		String confirmTime = data.getParam("confirm_time");

		if (!StringUtils.isEmpty(confirmTime))
		{
			end = Integer.parseInt(confirmTime) * 60 * 1000;
		}

		// 启动定时器
		end = System.currentTimeMillis() + end;
		Date ed = new Date(end);
		CreditTask task = new CreditTask();
		task.setA(type);
		if (data.getParam("tableName") != null)
		{
			task.setB(data.getParam("tableName") + "");
		}
		TimerUtils.startTimer(SessionUtils.getTPID(), task, ed);

		// 先查
		Record<String, Object> statusRecord = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_CHECK_MODEL)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_MODEL_TYPE = #{type}")
				.get(new RecordImpl<String, Object>().setColumn("tpid", tpid)
						.setColumn("type", type));
		if (CollectionUtils.isEmpty(statusRecord))
		{
			statusRecord = new RecordImpl<String, Object>();
			statusRecord.setColumn("V_TPID", tpid);
			statusRecord.setColumn("V_MODEL_TYPE", type);
			statusRecord.setColumn("V_STATUS", 1);
			statusRecord.setColumn("ID", Random.generateUUID());
			statusRecord.setColumn("N_CONFIRM_TIME", end);
			// 不存在保存
			this.activeRecordDAO.auto().table(TableName.EKB_T_CHECK_MODEL)
					.save(statusRecord);
			return;
		}
		// 如果存在就修改
		this.activeRecordDAO
				.sql("UPDATE EKB_T_CHECK_MODEL SET V_STATUS = '1',N_CONFIRM_TIME=#{TIME} WHERE V_TPID=#{TPID} AND V_MODEL_TYPE = #{TYPE}")
				.setParam("TIME", end).setParam("TPID", tpid)
				.setParam("TYPE", type).update();
	}

	/**
	 * 
	 * 结束数据环节<br/>
	 * <p>
	 * 结束数据环节
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 */
	// 定义路径
	@Path(value = "/end", desc = "结束数据环节")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void endConfirm(AeolusData data) throws ServiceException
	{
		logger.debug(LogUtils.format("结束数据环节"), data);
		Record<String, Object> param = new RecordImpl<String, Object>();
		Record<String, Object> checkDataRecord = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		// 销毁定时器
		TimerUtils.closeTimer(tpid, CreditTask.class);

		// 设置项目ID
		String type = data.getParam("type");
		String tableName = ConstantEOKB.TableName.EKB_T_TENDER_LIST;
		if (data.getParam("tableName") == null
				&& StringUtils.equals(type, "DYXF_credit"))
		{
			tableName = ConstantEOKB.TableName.EKB_T_DECRYPT_INFO;
		}
		param.setColumn("tpid", tpid);
		// 获取投标人列表
		List<Record<String, Object>> list = this.activeRecordDAO.auto()
				.table(tableName).setCondition("AND", "V_TPID=#{tpid}")
				.list(param);
		String id = null;
		int count = 0;
		for (Record<String, Object> record : list)
		{
			param = new RecordImpl<String, Object>();
			id = record.getString("ID");
			param.setColumn("V_TPID", tpid);
			param.setColumn("V_BUSNAME", type);
			param.setColumn("V_BUSID", id);
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
				checkDataRecord.setColumn("V_BUSNAME", type);
				checkDataRecord.setColumn("V_BUSID", id);
				checkDataRecord.setColumn("V_STATUS", "1");
				checkDataRecord.setColumn("V_REMARK", "确认");
				this.activeRecordDAO.auto()
						.table(ConstantEOKB.TableName.EKB_T_CHECK_DATA)
						.save(checkDataRecord);
			}
		}
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
		String tpid = data.getParam("tpid");
		String id = data.getParam("id");
		String busName = data.getParam("busName");
		String remark = data.getParam("remark");
		String status = data.getParam("status");
		String groupid = data.getParam("groupid");
		String name = data.getParam("name");
		int count = 0;
		if (StringUtils.isNotEmpty(groupid))// 标段组
		{
			List<Record<String, Object>> list = activeRecordDAO.sql(
					"SELECT * FROM EKB_T_DECRYPT_INFO WHERE V_TPID='" + tpid
							+ "' AND V_BID_SECTION_GROUP_CODE='" + groupid
							+ "' AND V_BIDDER_NAME='" + name + "'").list();
			for (Record<String, Object> r : list)
			{
				param = new RecordImpl<String, Object>();
				param.setColumn("V_BUSNAME", busName);
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
					checkDataRecord.setColumn("V_BUSNAME", busName);
					checkDataRecord.setColumn("V_BUSID", r.getString("ID"));
					checkDataRecord.setColumn("V_STATUS", status);
					checkDataRecord.setColumn("V_REMARK", remark);
					this.activeRecordDAO.auto()
							.table(ConstantEOKB.TableName.EKB_T_CHECK_DATA)
							.save(checkDataRecord);
				}
			}
		}
		else if (StringUtils.isEmpty(groupid))// 非标段组
		{
			param = new RecordImpl<String, Object>();
			param.setColumn("V_BUSNAME", busName);
			param.setColumn("V_BUSID", id);
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
				checkDataRecord.setColumn("V_BUSNAME", busName);
				checkDataRecord.setColumn("V_BUSID", id);
				checkDataRecord.setColumn("V_STATUS", status);
				checkDataRecord.setColumn("V_REMARK", remark);
				this.activeRecordDAO.auto()
						.table(ConstantEOKB.TableName.EKB_T_CHECK_DATA)
						.save(checkDataRecord);
			}
		}
	}

}
