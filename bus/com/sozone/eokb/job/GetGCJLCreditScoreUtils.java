/**
 * 包名：com.sozone.eokb.job
 * 文件名：GetGCJLCreditScoreUtils.java<br/>
 * 创建时间：2018-10-11 上午11:41:01<br/>
 * 创建者：YSZY<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.job;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.lang.StringUtils;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.transport.http.CommonsHttpMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.ws.client.GCJLDataUtils;
import com.ws.client.GCJLWebServiceClient;
import com.ws.client.GCJLWebServiceSoap;

/**
 * 工程监理信用分同步工具类<br/>
 * <p>
 * 工程监理信用分同步工具类<br/>
 * </p>
 * Time：2018-10-11 上午11:41:01<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class GetGCJLCreditScoreUtils
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(GetGCJLCreditScoreUtils.class);

	/**
	 * 获取数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param dao
	 *            持久层对象
	 * @param types
	 *            工程监理企业信用评价
	 * @param year
	 *            yyyy
	 * @param quarter
	 *            1,2,3,4
	 * @throws Exception
	 *             异常
	 */
	public static void getData(ActiveRecordDAO dao, String[] types,
			String year, String quarter) throws Exception
	{
		// 开始同步
		GCJLWebServiceClient client = new GCJLWebServiceClient();
		// create a default service endpoint
		GCJLWebServiceSoap service = client.getGCJLWebServiceSoap();
		// ----------------------------设置超时时间---------------------------------------
		Client c = Client.getInstance(service);
		HttpClientParams chp = new HttpClientParams();
		// 如果服务不需要传输大量的数据，保持长连接，还是建议关闭掉此功能，设置为false。否则，在业务量很大的情况下，很容易将服务端和自己都搞的很慢甚至拖死。
		chp.setParameter(HttpClientParams.USE_EXPECT_CONTINUE, Boolean.FALSE);
		// socket超时时间（单位：毫秒）,设置成10分钟
		chp.setIntParameter(HttpClientParams.SO_TIMEOUT, 1000 * 60 * 10);
		// 连接超时时间（单位：毫秒）,设置成10分钟
		chp.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT,
				30000 * 60 * 10);
		c.setProperty(CommonsHttpMessageSender.HTTP_CLIENT_PARAMS, chp);
		// -----------------------------------------------------------------------------------------------
		String result = null;

		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("year", year);
		params.setColumn("quarter", quarter);

		String sql = "DELETE FROM ${table} WHERE V_TYPE = #{type} AND V_YEAR = #{year} AND V_QUARTER = #{quarter}";
		// 这种只保存一季度的
		// String sql = "DELETE FROM ${table} WHERE V_TYPE = #{type}";
		for (String type : types)
		{
			params.setColumn("type", type);
			logger.info(LogUtils.format("开始同步工程监理企业信用分!", params));
			// 同步工程监理企业信用分
			result = service.getGCJL_QuarterScore(year, quarter, null);
			logger.info(LogUtils.format("工程监理企业信用分返回!", result));
			// 如果有返回值
			if (StringUtils.isNotEmpty(result))
			{
				params.setColumn("table", TableName.COMPANY_CREDIT_SCORE);
				// 先删除
				dao.sql(sql).setParams(params).delete();
				List<Record<String, Object>> rows = transform(result);
				logger.info(LogUtils.format("工程监理企业信用分返回数量!", rows.size()));
				dao.auto().table(TableName.COMPANY_CREDIT_SCORE).save(rows);
			}

			logger.info(LogUtils.format("开始同步工程监理企业平均信用分!", params));
			// 同步行业信用分
			result = service.getGCJL_QuarterScoreAvg(year, quarter);
			logger.info(LogUtils.format("工程监理企业平均信用分返回!", result));
			// 如果有返回值
			if (StringUtils.isNotEmpty(result))
			{
				params.setColumn("table", TableName.INDUSTRY_AVG_CREDIT_SCORE);
				// 先删除
				dao.sql(sql).setParams(params).delete();
				Record<String, Object> row = GCJLDataUtils
						.quarterAvgScoreToRecord(JSON.parseObject(result), type);
				row.setColumn("ID", getIDKey(row));
				row.setColumn("N_CREATE_TIME", System.currentTimeMillis());
				dao.auto().table(TableName.INDUSTRY_AVG_CREDIT_SCORE).save(row);
			}
		}
	}

	/**
	 * 转换<br/>
	 * <p>
	 * </p>
	 * 
	 * @param json
	 * @return
	 */
	private static List<Record<String, Object>> transform(String json)
	{
		List<Record<String, Object>> rows = new LinkedList<Record<String, Object>>();
		JSONObject root = JSON.parseObject(json);
		JSONArray array = root.getJSONArray("data");
		Record<String, Object> row = null;
		long ts = System.currentTimeMillis();
		for (int i = 0; i < array.size(); i++)
		{
			row = GCJLDataUtils.quarterScoreToRecord(array.getJSONObject(i),
					root);
			row.setColumn("ID", getIDKey(row));
			row.setColumn("N_CREATE_TIME", ts);
			// 把组织机构代码转换成带-的
			row.setColumn("V_ORG_CODE", transformOrgCode(row));
			rows.add(row);
		}
		return rows;
	}

	/**
	 * 把组织机构代码转换成带-的数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param row
	 * @return
	 */
	private static String transformOrgCode(Record<String, Object> row)
	{
		String orgCode = row.getString("V_ORG_CODE");
		// 如果为空
		if (StringUtils.isEmpty(orgCode))
		{
			return null;
		}
		// 省里的接口非常乱
		if (StringUtils.length(orgCode) != 9)
		{
			return orgCode;
		}
		// 158143183
		return StringUtils.substring(orgCode, 0, 8) + "-" + orgCode.charAt(8);
	}

	// public static void main(String[] args)
	// {
	// String orgCode = "58114428X";
	// orgCode = StringUtils.substring(orgCode, 0, 8) + "-"
	// + orgCode.charAt(8);
	// System.err.println(orgCode);
	// }

	/**
	 * 获取主键<br/>
	 * <p>
	 * </p>
	 * 
	 * @param row
	 * @return
	 */
	private static String getIDKey(Record<String, Object> row)
	{
		StringBuilder sb = new StringBuilder();
		// 组织结构代码
		sb.append(
				StringUtils.defaultIfEmpty(row.getString("V_COMPANY_NAME"), ""))
				.append("|");
		// 组织结构代码
		sb.append(StringUtils.defaultIfEmpty(row.getString("V_UNIFY_CODE"), ""))
				.append("|");
		// 组织结构代码
		sb.append(StringUtils.defaultIfEmpty(row.getString("V_ORG_CODE"), ""))
				.append("|");
		// 类别
		sb.append(StringUtils.defaultIfEmpty(row.getString("V_TYPE"), ""))
				.append("|");
		// 年份
		sb.append(StringUtils.defaultIfEmpty(row.getString("V_YEAR"), ""))
				.append("|");
		// 季度
		sb.append(StringUtils.defaultIfEmpty(row.getString("V_QUARTER"), ""));
		return DigestUtils.md5Hex(sb.toString());
	}
}
