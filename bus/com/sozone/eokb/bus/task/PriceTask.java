/**
 * 包名：com.sozone.eokb.bus.task
 * 文件名：CreditTask.java<br/>
 * 创建时间：2017-9-5 下午2:57:00<br/>
 * 创建者：LDH<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.task;

import java.util.List;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;

import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.dao.StatefulDAO;
import com.sozone.aeolus.dao.StatefulDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.utils.SessionUtils;

/**
 * TODO 一句话描述类的主要作用<br/>
 * <p>
 * TODO 该类的详细描述<br/>
 * </p>
 * Time：2017-9-5 下午2:57:00<br/>
 * 
 * @author LDH
 * @version 1.0.0
 * @since 1.0.0
 */
public class PriceTask extends TimerTask
{
	private String a;
	private String b;

	@Override
	public void run()
	{
		String typeValue = getA();
		String tableName = "EKB_T_DECRYPT_INFO";
		List<Record<String, Object>> list = null;
		if (StringUtils.equals(typeValue, "DYXF_credit"))
		{
			tableName = "EKB_T_DECRYPT_INFO";
		}
		else if (StringUtils.equals(typeValue, "DYXF_electronics")
				|| StringUtils.equals(typeValue, "DEXF_offer")
				|| StringUtils.equals(typeValue, "DEXF_price"))
		{
			tableName = "EKB_T_TENDER_LIST";
		}
		else
		{
			return;
		}
		StatefulDAO dao = null;
		try
		{
			dao = new StatefulDAOImpl();
			// 获取投标信息表内容
			list = dao.sql(
					"SELECT * FROM " + tableName + " WHERE V_TPID='"
							+ SessionUtils.getTPID() + "'").list();
			for (Record<String, Object> record : list)
			{
				String id = record.getString("ID");
				Record<String, Object> recordCheck = dao.sql(
						"SELECT * FROM EKB_T_CHECK_DATA WHERE V_TPID='"
								+ SessionUtils.getTPID() + "' AND V_BUSNAME='"
								+ typeValue + "' AND V_BUSID='" + id + "'")
						.get();
				if (recordCheck == null)
				{
					Record<String, Object> r=new RecordImpl<String, Object>();
					r.setColumn("ID", Random.generateUUID());
					r.setColumn("V_TPID", SessionUtils.getTPID());
					r.setColumn("V_BUSNAME", typeValue);
					r.setColumn("V_BUSID", id);
					r.setColumn("V_STATUS", "1");
					r.setColumn("V_REMARK", "确认");
					dao.auto()
							.table(ConstantEOKB.TableName.EKB_T_CHECK_DATA)
							.save(r);
				}
			}
			// 提交事务
			dao.commit();
		}
		catch (Exception e)
		{
			if (null != dao)
			{
				// 回滚
				dao.rollback();
			}
		}
		finally
		{
			if (null != dao)
			{
				// 关闭
				dao.close();
			}
		}

	}

	public String getA()
	{
		return a;
	}

	public void setA(String a)
	{
		this.a = a;
	}

	public String getB()
	{
		return b;
	}

	public void setB(String b)
	{
		this.b = b;
	}
}
