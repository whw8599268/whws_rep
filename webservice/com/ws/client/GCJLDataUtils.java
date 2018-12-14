/**
 * 包名：com.ws.client
 * 文件名：GCJLDataUtils.java<br/>
 * 创建时间：2018-5-14 下午5:53:57<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.ws.client;

import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;

/**
 * 工程监理信用分数据工具类<br/>
 * <p>
 * 工程监理信用分数据工具类<br/>
 * </p>
 * Time：2018-5-14 下午5:53:57<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class GCJLDataUtils
{

	/**
	 * 将返回结果转换成表数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param obj
	 *            JSON对象
	 * @param root
	 *            根对象
	 * @return 表数据
	 */
	public static Record<String, Object> quarterScoreToRecord(JSONObject obj,
			JSONObject root)
	{
		Record<String, Object> rs = new RecordImpl<String, Object>();
		rs.setColumn("V_YEAR", root.getString("year"));
		rs.setColumn("V_QUARTER", root.getString("quarter"));
		rs.setColumn("V_TYPE", root.getString("datatype"));
		// ----------------
		rs.setColumn("V_COMPANY_NAME", obj.getString("企业名称"));
		rs.setColumn("V_ORG_CODE", obj.getString("组织机构代码"));
		rs.setColumn("N_TOTAL_SCORE", obj.getFloat("总分值"));
		rs.setColumn("N_RANKING", obj.getInteger("省排名"));
		rs.setColumn("V_JSON_OBJ", obj);
		return rs;
	}

	/**
	 * 将返回结果转换成表数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param avg
	 *            平均分JSON对象
	 * @param type
	 *            类型
	 * @return 表数据
	 */
	public static Record<String, Object> quarterAvgScoreToRecord(
			JSONObject avg, String type)
	{
		Record<String, Object> rs = new RecordImpl<String, Object>();
		rs.setColumn("V_YEAR", avg.getString("年度"));
		rs.setColumn("V_QUARTER", avg.getString("季度"));
		rs.setColumn("V_TYPE", type);
		rs.setColumn("N_TOTAL_SCORE", avg.getFloat("信用平均分"));
		rs.setColumn("V_JSON_OBJ", avg);
		return rs;
	}

}
