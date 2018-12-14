/**
 * 包名：com.sozone.eokb.bus.max
 * 文件名：MaxAddress.java<br/>
 * 创建时间：2017-11-27 上午10:36:57<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.mac;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * MAC地址服务接口<br/>
 * <p>
 * MAC地址服务接口<br/>
 * </p>
 * Time：2017-11-27 上午10:36:57<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/mac", desc = "MAC地址服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class MacAddress
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(MacAddress.class);

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
	 * 获取mac地址信息<br/>
	 * <p>
	 * 获取mac地址信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return mac地址信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/getMacInfo", desc = "获取mac地址信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ResultVO<Record<String, Object>> getMacInfo(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取mac地址信息", data));
		String tpid = SessionUtils.getTPID();
		ResultVO<Record<String, Object>> result = new ResultVO<Record<String, Object>>();
		List<Record<String, Object>> macInfos = null;
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);

		List<Record<String, Object>> sameMacs = new LinkedList<Record<String, Object>>();
		// 获取当前招标项目的有效的所有标段
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("GROUP BY", "V_BID_SECTION_ID").list(param);

		logger.debug(LogUtils.format("获取当前招标项目的有效的所有标段", sections));

		String statementName = null;
		if (SessionUtils.isSectionGroup())
		{
			statementName = "Mac.getMacInfoByGroup";
		}
		else
		{
			statementName = "Mac.getMacInfo";
		}

		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.setColumn("tpid", tpid);
			// 获取相同的MAX地址信息
			macInfos = this.activeRecordDAO.statement().selectList(
					statementName, param);
			if (!CollectionUtils.isEmpty(macInfos))
			{
				sameMacs.addAll(macInfos);
			}

		}
		if (CollectionUtils.isEmpty(sameMacs))
		{
			result.setSuccess(false);
			return result;
		}
		result.setSuccess(true);
		result.setRows(sameMacs);
		return result;
	}

	/**
	 * 
	 * 否决mac地址相同的投标人<br/>
	 * <p>
	 * 否决mac地址相同的投标人
	 * </p>
	 * 
	 * @param orgCode
	 *            组织机构号
	 * @param sid
	 *            标段ID
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "sfb/{orgCode}/{sid}", desc = "否决mac地址相同的投标人")
	@HttpMethod(HttpMethod.GET)
	public void setBidderFail(@PathParam("orgCode") String orgCode,
			@PathParam("sid") String sid, AeolusData data)
			throws FacadeException
	{

		logger.debug(LogUtils.format("否决mac地址相同的投标人", data));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", SessionUtils.getTPID());
		param.setColumn("sid", sid);
		param.setColumn("orgCode", orgCode);
		param.setColumn("N_ENVELOPE_0", 0);
		String tableName = ConstantEOKB.TableName.EKB_T_TENDER_LIST;
		if (SessionUtils.isSectionGroup())
		{
			tableName = ConstantEOKB.TableName.EKB_T_DECRYPT_INFO;
		}

		this.activeRecordDAO.auto().table(tableName)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
				.setCondition("AND", "V_BIDDER_ORG_CODE=#{orgCode}")
				.modify(param);

	}
}
