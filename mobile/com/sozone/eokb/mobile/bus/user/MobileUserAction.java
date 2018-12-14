/**
 * 包名：com.sozone.eokb.mobile.bus.user
 * 文件名：MobileUserAction.java<br/>
 * 创建时间：2018-12-4 下午2:09:09<br/>
 * 创建者：wengdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.mobile.bus.user;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 手机版用户信息服务类<br/>
 * <p>
 * 手机版用户信息服务类<br/>
 * </p>
 * Time：2018-12-4 下午2:09:09<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/mobile/user", desc = "手机版用户信息服务类")
@Permission(Level.Authenticated)
public class MobileUserAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(MobileUserAction.class);

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
	 * 获取用户基本解密信息<br/>
	 * <p>
	 * 获取用户基本解密信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 用户基本投递信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "info", desc = "获取用户基本解密信息")
	@Service
	public List<Record<String, Object>> getUserInfos(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取用户基本解密信息", data));
		String tpID = data.getParam("tpID");

		Record<String, Object> projectInfo = this.activeRecordDAO.pandora()
				.SELECT_ALL_FROM(TableName.EKB_T_TENDER_PROJECT_INFO)
				.EQUAL("ID", tpID).get();

		if (CollectionUtils.isEmpty(projectInfo))
		{
			throw new FacadeException("", "未获取到招标项目信息");
		}
		List<Record<String, Object>> envelopeList = new LinkedList<Record<String, Object>>();
		int envelope = getKbFlowStatus(projectInfo);

		// 标段列表
		List<Record<String, Object>> sections = null;
		// 获取每个标段的投递信息
		for (int i = 1; i <= envelope; i++)
		{
			// 如果是标段组进行到第一信封，但是还未随机分配，不做处理
			if (1 == envelope)
			{
				if (1 == projectInfo.getInteger("N_IS_SECTION_GROUP"))
				{
					// 判断是否随机分配过
					int count = this.activeRecordDAO.pandora()
							.SELECT_COUNT_FROM("EKB_T_ELECTRONICS")
							.EQUAL("V_TPID", tpID).excute();
					if (0 == count)
					{
						break;
					}
				}
			}
			if (i > 2)
			{
				break;
			}
			// 当前投标人所投标段信息
			sections = this.activeRecordDAO
					.pandora()
					.SELECT("T.ID,B.V_BID_SECTION_NAME")
					.FROM("EKB_T_TENDER_LIST T")
					.LEFT_OUTER_JOIN(
							"EKB_T_SECTION_INFO B ON T.V_BID_SECTION_ID=B.V_BID_SECTION_ID AND T.V_TPID=B.V_TPID")
					.EQUAL("T.V_TPID", tpID)
					.EQUAL("T.N_ENVELOPE_" + (i - 1), 1)
					.EQUAL("V_BIDDER_ORG_CODE", SessionUtils.getCompanyCode())
					.NOT_EQUAL("B.V_BID_OPEN_STATUS", "0")
					.WHERE("B.V_BID_OPEN_STATUS NOT LIKE '10%'").list();

			Record<String, Object> temp = new RecordImpl<String, Object>();
			temp.setColumn("ENVELOPE", i);
			temp.setColumn("SECTIONS", sections);
			envelopeList.add(temp);
		}
		return envelopeList;
	}

	/**
	 * 
	 * 获取开标流程状态<br/>
	 * <p>
	 * 获取开标流程状态
	 * </p>
	 * 
	 * @param projectInfo
	 *            项目信息
	 * @return 信封数
	 * @throws FacadeException
	 *             FacadeException
	 */
	private int getKbFlowStatus(Record<String, Object> projectInfo)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取开标流程状态", projectInfo));
		// 流程ID
		String flowID = getFlowID(projectInfo);

		// 获取一级菜单
		List<Record<String, Object>> flowNodes = this.activeRecordDAO.pandora()
				.SELECT_ALL_FROM(TableName.TENDER_PROJECT_FLOW_VIEW)
				.EQUAL("V_FLOW_ID", flowID)
				.EQUAL("V_TPID", projectInfo.getString("ID"))
				.EQUAL("N_DEPTH", 1).ORDER_BY("N_INDEX").list();

		int envelope = 0;
		Record<String, Object> flowNode = null;
		// 下标index=3： 第一信封 ，index=4：第二信封
		for (int i = 2; i < flowNodes.size(); i++)
		{
			flowNode = flowNodes.get(i);
			// 节点状态是1，未开始。不做处理
			if (1 == flowNode.getInteger("N_STATUS"))
			{
				break;
			}
			envelope++;
		}
		return envelope;
	}

	/**
	 * 获取流程主键<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tenderProject
	 *            招标项目
	 * @return flowID
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static String getFlowID(Record<String, Object> tenderProject)
			throws ServiceException
	{
		// 获取招标项目流程信息
		String bemJson = tenderProject.getString("V_BEM_INFO_JSON");
		if (StringUtils.isEmpty(bemJson))
		{
			// 获取不到评标办法
			throw new ValidateException("", "无法获取招标项目对应的评标办法信息");
		}
		JSONObject bem = JSON.parseObject(bemJson);
		String code = bem.getString("V_CODE");
		// 判断是否有标段组
		Integer flag = tenderProject.getInteger("N_IS_SECTION_GROUP");
		if (null == flag)
		{
			flag = 0;
		}
		// 查询流程
		Record<String, Object> flow = ActiveRecordDAOImpl.getInstance()
				.pandora().SELECT("ID").FROM(TableName.FLOW_INFO)
				.EQUAL("V_BEM_CODE", code).EQUAL("N_IS_SECTION_GROUP", flag)
				.get();
		if (CollectionUtils.isEmpty(flow))
		{
			throw new ServiceException("", "无法获取到对应的开标流程");
		}
		return flow.getString("ID");
	}
}
