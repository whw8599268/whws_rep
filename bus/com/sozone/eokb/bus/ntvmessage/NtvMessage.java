/**
 * 包名：com.sozone.eokb.bus.ntvmessage
 * 文件名：NtvMessage.java<br/>
 * 创建时间：2018-3-2 下午2:10:14<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.ntvmessage;

import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 云视睿博通信服务接口<br/>
 * <p>
 * 云视睿博通信服务接口<br/>
 * </p>
 * Time：2018-3-2 下午2:10:14<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/ntvmessage", desc = "云视睿博通信服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class NtvMessage
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(NtvMessage.class);
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
	 * 保存发送信息<br/>
	 * <p>
	 * 保存发送信息
	 * </p>
	 * 
	 * @param data AeolusData
	 * @return AeolusData
	 * @throws FacadeException FacadeException
	 */
	@Path(value = "", desc = "")
	@HttpMethod(HttpMethod.POST)
	public Record<String, Object> saveMessageInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("保存发送信息", data));
		String tpid = SessionUtils.getTPID();
		String msgContent = data.getParam("msgContent");

		Record<String, Object> mesInfo = new RecordImpl<String, Object>();
		mesInfo.setColumn("ID", Random.generateUUID());
		mesInfo.setColumn("V_TPID", tpid);
		mesInfo.setColumn("V_SENDER_NAME",
				ApacheShiroUtils.getCurrentUserName());
		mesInfo.setColumn("V_MESSAGE_CONTENT", msgContent);
		long nowTime = new Date().getTime();
		mesInfo.setColumn("N_SEND_TIME", nowTime);

		this.activeRecordDAO.auto().table(TableName.EKB_T_NTV_MESSAG_MSG)
				.save(mesInfo);

		mesInfo.remove("ID");
		mesInfo.remove("V_TPID");
		mesInfo.remove("N_SEND_TIME");
		mesInfo.setColumn("V_SEND_TIME",
				DateFormatUtils.format(nowTime, "HH:mm:ss"));
		return mesInfo;
	}

	/**
	 * 获取消息记录分页信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProjectID
	 *            项目ID
	 * @param data
	 *            AeolusData
	 * @return 分页信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "hns/{tpid}", desc = "获取消息记录分页信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public Page<Record<String, Object>> getHistoricalNewsPage(
			@PathParam("tpid") String tenderProjectID, AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取消息记录分页信息", tenderProjectID, data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = data.getRecord();
		param.setColumn("V_TPID", tenderProjectID);
		return this.activeRecordDAO.auto()
				.table(TableName.EKB_T_NTV_MESSAG_MSG)
				.addSortOrder("N_SEND_TIME", "DESC").page(pageable, param);
	}
}
