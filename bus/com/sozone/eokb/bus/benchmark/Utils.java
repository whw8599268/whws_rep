/**
 * 包名：com.sozone.eokb.fjs_gsgl_jdsg_hldjf_v1
 * 文件名：Utils.java<br/>
 * 创建时间：2017-8-29 下午4:46:23<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.benchmark;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.DAOException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.Arith;
import com.sozone.eokb.utils.ListSortUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * </p> Time：2017-8-29 下午4:46:23<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
public class Utils
{
	/**
	 * 
	 * 获取DAO<br/>
	 * <p>
	 * 获取DAO
	 * </p>
	 * 
	 * @return DAO
	 */
	private static ActiveRecordDAO getActiveRecordDAO()
	{
		return ActiveRecordDAOImpl.getInstance();
	}

	private static final NumberFormat FMT_D = new DecimalFormat("###,##0",
			new DecimalFormatSymbols());

	public static void createBasePriceByjgjg() throws DAOException
	{
		ActiveRecordDAO dao = getActiveRecordDAO();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpid", tpid);
		// 标段列表
		List<Record<String, Object>> sectionList = dao.auto()
				.table(ConstantEOKB.TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", " V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.addSortOrder("V_BID_SECTION_CODE", "ASC").list(param);
		if (CollectionUtils.isEmpty(sectionList))
		{
			throw new DAOException("", "查询不到标段信息");
		}

		// 投标人列表
		List<Record<String, Object>> bidderList = null;

		// 最高限价
		Double maxPrice = null;
		// 投标人报价
		Double price = null;
		// 有效报价总和
		Double allPrice = null;
		// 评标基准价
		Double evaluationPrice = null;
		for (Record<String, Object> section : sectionList)
		{
			// 有效投标数量
			double validCount = 0;
			allPrice = 0D;
			maxPrice = section.getDouble("N_CONTROL_PRICE");
			if (maxPrice == null)
			{
				throw new DAOException("", "标段最高限价不能为空");
			}
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			bidderList = dao.auto()
					.table(ConstantEOKB.TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.setCondition("AND", "N_ENVELOPE_1=1").list(param);
			if (CollectionUtils.isEmpty(bidderList))
			{
				throw new DAOException("", "查询不到投标人");
			}

			// 求有效投标价总和
			for (Record<String, Object> bidder : bidderList)
			{
				price = bidder.getDouble("N_PRICE");
				if (price <= maxPrice && price >= Arith.mul(maxPrice, 0.85))
				{
					allPrice = Arith.add(allPrice, price);
					validCount++;
				}
			}

			// 评标基准价为所有不小于最高限价的85%的评标价的算术平均值
			evaluationPrice = Math.round(allPrice / validCount) + 0d;
			param.setColumn("ID", section.getString("ID"));
			param.setColumn("N_EVALUATION_PRICE", evaluationPrice);
			dao.auto().table(TableName.EKB_T_SECTION_INFO).modify(param);
		}

	}

	/**
	 * 
	 * 计算评标基准价（勘察设计）<br/>
	 * <p>
	 * 选取第一信封排名前三的有效报价，计算三家报价的算术平均值，并取整
	 * </p>
	 * 
	 * @throws DAOException
	 */
	public static void createBasePriceByKCSJ() throws DAOException
	{
		ActiveRecordDAO dao = getActiveRecordDAO();
		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpid", tpid);
		// 标段列表
		List<Record<String, Object>> sectionList = dao.auto()
				.table(ConstantEOKB.TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", " V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.addSortOrder("V_BID_SECTION_CODE", "ASC").list(param);
		if (CollectionUtils.isEmpty(sectionList))
		{
			throw new DAOException("", "查询不到标段信息");
		}

		// 投标人列表
		List<Record<String, Object>> bidderList = null;

		// 最高限价
		Double maxPrice = null;
		// 评标基准价
		Double evaluationPrice = null;
		for (Record<String, Object> section : sectionList)
		{
			maxPrice = section.getDouble("N_CONTROL_PRICE");
			if (maxPrice == null)
			{
				throw new DAOException("", "标段最高限价不能为空");
			}
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sid", section.getString("V_BID_SECTION_ID"));
			param.setColumn("maxPrice", maxPrice);
			bidderList = dao.auto()
					.table(ConstantEOKB.TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
					.setCondition("AND", "N_ENVELOPE_1=1")
					.setCondition("AND", "N_PRICE<=#{maxPrice}").list(param);

			// 将投标人按照分数降序排序
			for (Record<String, Object> bidder : bidderList)
			{
				bidder.setColumn("N_TOTAL", bidder.getJSONObject("V_JSON_OBJ")
						.getDouble("N_TOTAL"));
			}
			ListSortUtils.sort(bidderList, false, "N_TOTAL");

			if (CollectionUtils.isEmpty(bidderList))
			{
				evaluationPrice = 0D;
			}

			else
			{
				// 在第一个信封（商务及技术文件）评审得分中，剔除按上述第（2）点规定不参加评标基准价计算的投标报价对应的得分后，
				// 由高到低的顺序选取前3个不同的得分（若不足三个，则全部选取），
				// 再对前3个不同的得分对应的评标价（同一得分的不同投标人的投标报价均参加基准价计算）做算术平均（取整数），
				// 并将该平均值作为评标基准价。 最多10名
				int index = 0;
				int count = 0;
				double sumPrice = 0d;
				double score = 0d;
				for (Record<String, Object> bidder : bidderList)
				{
					count++;

					sumPrice += bidder.getDouble("N_PRICE");
					if (1 == count)
					{
						score = bidder.getDouble("N_TOTAL");
						continue;
					}

					// 分数不同
					if (score != bidder.getDouble("N_TOTAL"))
					{
						index++;
					}
					if (index >= 2)
					{
						break;
					}
					// 最多10名
					if (count >= 10)
					{
						break;
					}
					score = bidder.getDouble("N_TOTAL");
				}
				evaluationPrice = sumPrice / count;
			}

			// 评标基准价为取整
			evaluationPrice = Math.round(evaluationPrice) + 0D;
			param.setColumn("ID", section.getString("ID"));
			param.setColumn("N_EVALUATION_PRICE", evaluationPrice);
			dao.auto().table(TableName.EKB_T_SECTION_INFO).modify(param);
		}

	}

	/**
	 * 基准价
	 * 
	 * @throws DAOException
	 */
	public static void createBasePrice() throws DAOException
	{
		ActiveRecordDAO dao = getActiveRecordDAO();

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("AID", SessionUtils.getTPID());
		param.setColumn("N_ENVELOPE_0", "1");
		// 获取投标信息
		/*
		 * List<Record<String, Object>> list = dao.auto()
		 * .table(ConstantEOKB.TableName.EKB_T_TENDER_LIST) .setCondition("AND",
		 * " V_TPID=#{AID}") .setCondition("AND",
		 * "N_VOIDTAGTMP=#{N_VOIDTAGTMP}") .addSortOrder("V_BID_SECTION_CODE",
		 * "ASC").list(param);
		 */
		List<Record<String, Object>> list = dao.auto()
				.table(ConstantEOKB.TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", " V_TPID=#{AID}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.addSortOrder("V_BID_SECTION_CODE", "ASC").list(param);

		JSONArray jsonArr = null;

		// 报价
		Double pc = null;
		for (Record<String, Object> record : list)
		{
			jsonArr = new JSONArray();
			// 初始化投标报价（有效报价）通过汇总表
			String sql_valid = "DELETE FROM "
					+ ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_VALID_OFFER
					+ " WHERE V_TPID = '" + SessionUtils.getTPID()
					+ "' AND V_BID_SECTION_ID='"
					+ record.getString("V_BID_SECTION_ID") + "'";
			dao.sql(sql_valid).delete();

			// 初始化基准价表
			sql_valid = "DELETE FROM "
					+ ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION
					+ " WHERE V_TPID = '" + SessionUtils.getTPID()
					+ "' AND V_BID_SECTION_ID='"
					+ record.getString("V_BID_SECTION_ID") + "'";
			dao.sql(sql_valid).delete();

			double f1 = 0;// 基准价系数
			int m = 0;// 家数为m家
			Double xi = 0D;// 最高限价
			double qi = 0;// 平均值
			double pi = 0;// 参加该标段基准价计算的所有投标报价之和
			int n = 0;// Hi企业个数
			double z = 0;// Gi中所有报价之和
			double ri = 0;// 投标基准价
			List<Record<String, Object>> Gi = new ArrayList<Record<String, Object>>();// 参与投标企业(有效报价)
			List<Record<String, Object>> ALLGi = new ArrayList<Record<String, Object>>();// 参与投标企业
			List<Record<String, Object>> Hi = new LinkedList<Record<String, Object>>();// 参加基准价计算的投标报价样本队列
			List<Record<String, Object>> L = new LinkedList<Record<String, Object>>();// Gi中的B级企业报价取出放入队列
			Record<String, Object> recordOffer = new RecordImpl<String, Object>();// 有效企业保存
			Record<String, Object> recordEvaluation = new RecordImpl<String, Object>();// 基准价方法保存
			param.setColumn("SECTION_ID", record.getString("V_BID_SECTION_ID"));
			/*** 获取最高价格 *****/
			String sql = "SELECT N_CONTROL_PRICE AS MAXPRICE FROM EKB_T_SECTION_INFO WHERE V_TPID='"
					+ SessionUtils.getTPID()
					+ "' AND V_BID_SECTION_ID='"
					+ record.getString("V_BID_SECTION_ID") + "'";
			Record<String, Object> recordPrice = dao.sql(sql).get();
			xi = recordPrice.getDouble("MAXPRICE");
			if (null == xi)
			{
				xi = 0D;
			}
			/*** 第一步放入GI *****/
			sql = "SELECT * from EKB_T_TENDER_LIST WHERE " + " V_TPID='"
					+ SessionUtils.getTPID() + "' AND V_BID_SECTION_ID='"
					+ record.getString("V_BID_SECTION_ID")
					+ "' AND N_ENVELOPE_1='1' ORDER BY V_BIDDER_NO";
			ALLGi = dao.sql(sql).list();
			sql = "SELECT * from EKB_T_TENDER_LIST WHERE N_PRICE>='"
					+ Arith.mul(xi, 0.85) + "' AND N_PRICE<='" + xi + "' "
					+ "AND V_TPID='" + SessionUtils.getTPID()
					+ "' AND V_BID_SECTION_ID='"
					+ record.getString("V_BID_SECTION_ID")
					+ "' AND N_ENVELOPE_1='1' ORDER BY V_BIDDER_NO";
			Gi = dao.sql(sql).list();
			m = Gi.size();
			if (m < 1)
			{
				continue;
			}
			// JSONObject jsonObjectR = JSONObject.parseObject(record
			// .getString("V_JSON_OBJ"));
			// JSONObject jsonObjectUserR = JSONObject.parseObject(jsonObjectR
			// .getString("objUser"));
			// 抽取有效等级企业

			for (int i = 0; i < ALLGi.size(); i++)
			{
				Record<String, Object> recordSection = ALLGi.get(i);
				JSONObject jsonObject = JSONObject.parseObject(recordSection
						.getString("V_JSON_OBJ"));

				recordOffer.setColumn(
						"V_TBTBRCR",
						BidderElementParseUtils.getSingObjAttribute(
								jsonObject.toString(), "tbRatingsInEvl"));
				recordOffer.setColumn("V_CBISAEFFECT",
						(StringUtils.equals(BidderElementParseUtils
								.getSingObjAttribute(jsonObject.toString(),
										"cbIsAEffect"), "1") ? "是" : "否"));
				recordEvaluation.setColumn("N_VALID_GRADE", 0);
				// 报价
				pc = recordSection.getDouble("N_PRICE");
				if (null == pc)
				{
					pc = 0D;
				}
				recordEvaluation.setColumn("N_VALID_OFFER", 0);
				if (pc >= Arith.mul(xi, 0.85) && pc <= xi)
				{
					recordEvaluation.setColumn("N_VALID_OFFER", 1);
				}
				// 单个标段大于5家企业
				if (m > 5)
				{
					// 判断投标人登记 AA或者A
					if (StringUtils.equals(BidderElementParseUtils
							.getSingObjAttribute(jsonObject.toString(),
									"tbRatingsInEvl"), "AA")
							|| StringUtils.equals(BidderElementParseUtils
									.getSingObjAttribute(jsonObject.toString(),
											"tbRatingsInEvl"), "A"))
					{
						if (pc >= Arith.mul(xi, 0.85) && pc <= xi)
						{
							recordEvaluation.setColumn("N_VALID_GRADE", 1);
							z += recordSection.getDouble("N_PRICE");
							Hi.add(recordSection);
						}

					}
					else if (StringUtils.equals(BidderElementParseUtils
							.getSingObjAttribute(jsonObject.toString(),
									"tbRatingsInEvl"), "B")
							|| StringUtils.equals(BidderElementParseUtils
									.getSingObjAttribute(jsonObject.toString(),
											"tbRatingsInEvl"), "C")
							|| StringUtils.equals(BidderElementParseUtils
									.getSingObjAttribute(jsonObject.toString(),
											"tbRatingsInEvl"), "D"))
					{
						if (pc >= Arith.mul(xi, 0.85) && pc <= xi)
						{
							L.add(recordSection);
							z += recordSection.getDouble("N_PRICE");
						}

					}
				}
				else
				{
					// 小于等于5家企业
					if (pc >= Arith.mul(xi, 0.85) && pc <= xi)
					{
						recordEvaluation.setColumn("N_VALID_GRADE", 1);
						z += recordSection.getDouble("N_PRICE");
						Hi.add(recordSection);
					}
				}

				// 删除有效表
				if (SessionUtils.isSectionGroup())
				{
					// 删除基准价有效表
					Record<String, Object> paramOffer = new RecordImpl<String, Object>();
					paramOffer.setColumn("AID", SessionUtils.getTPID());
					paramOffer
							.setColumn("GROUP_CODE", recordSection
									.getString("V_BID_SECTION_GROUP_CODE"));
					paramOffer.setColumn("SECTION_ID",
							recordSection.getString("V_BID_SECTION_ID"));
					paramOffer.setColumn("V_BIDDER_NO",
							recordSection.getString("V_BIDDER_NO"));
					dao.auto()
							.table(ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_VALID_OFFER)
							.setCondition("AND", " V_TPID=#{AID}")
							.setCondition("AND",
									"V_BID_SECTION_GROUP_CODE=#{GROUP_CODE}")
							.setCondition("AND",
									"V_BID_SECTION_ID=#{SECTION_ID}")
							.setCondition("AND", "V_BIDDER_NO=#{V_BIDDER_NO}")
							.remove(paramOffer);
					// 删除评标基准价计算记录表
					dao.auto()
							.table(ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION)
							.setCondition("AND", " V_TPID=#{AID}")
							.setCondition("AND",
									"V_BID_SECTION_GROUP_CODE=#{GROUP_CODE}")
							.setCondition("AND",
									"V_BID_SECTION_ID=#{SECTION_ID}")
							.setCondition("AND", "V_BIDDER_NO=#{V_BIDDER_NO}")
							.remove(paramOffer);
				}
				else
				{
					// 删除基准价有效表
					Record<String, Object> paramOffer = new RecordImpl<String, Object>();
					paramOffer.setColumn("AID", SessionUtils.getTPID());
					paramOffer.setColumn("SECTION_ID",
							recordSection.getString("V_BID_SECTION_ID"));
					paramOffer.setColumn("V_BIDDER_NO",
							recordSection.getString("V_BIDDER_NO"));
					dao.auto()
							.table(ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_VALID_OFFER)
							.setCondition("AND", " V_TPID=#{AID}")
							.setCondition("AND",
									"V_BID_SECTION_ID=#{SECTION_ID}")
							.setCondition("AND", "V_BIDDER_NO=#{V_BIDDER_NO}")
							.remove(paramOffer);
					// 删除评标基准价计算记录表
					dao.auto()
							.table(ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION)
							.setCondition("AND", " V_TPID=#{AID}")
							.setCondition("AND",
									"V_BID_SECTION_ID=#{SECTION_ID}")
							.setCondition("AND", "V_BIDDER_NO=#{V_BIDDER_NO}")
							.remove(paramOffer);
				}
				// 添加基准价有效表
				recordOffer.setColumn("ID", SZUtilsID.getUUID());
				recordOffer.setColumn("V_NAME",
						recordSection.getString("V_BIDDER_NAME"));
				recordOffer.setColumn("V_BIDDER_NO",
						recordSection.getString("V_BIDDER_NO"));
				recordOffer.setColumn("N_PRICE",
						recordSection.getString("N_PRICE"));
				recordOffer.setColumn("V_TPID",
						recordSection.getString("V_TPID"));
				recordOffer.setColumn("V_BID_SECTION_GROUP_CODE",
						recordSection.getString("V_BID_SECTION_GROUP_CODE"));
				recordOffer.setColumn("V_BID_SECTION_CODE",
						recordSection.getString("V_BID_SECTION_CODE"));
				recordOffer.setColumn("V_BID_SECTION_ID",
						recordSection.getString("V_BID_SECTION_ID"));
				dao.auto()
						.table(ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_VALID_OFFER)
						.save(recordOffer);
				// 添加评标基准价计算记录表
				recordEvaluation.setColumn("ID", SZUtilsID.getUUID());
				recordEvaluation.setColumn("V_NAME",
						recordSection.getString("V_BIDDER_NAME"));
				recordEvaluation.setColumn("V_BIDDER_NO",
						recordSection.getString("V_BIDDER_NO"));
				recordEvaluation.setColumn("N_PRICE",
						recordSection.getString("N_PRICE"));
				recordEvaluation.setColumn("N_CONTROL_PRICE", xi);// 最高限价
				recordEvaluation.setColumn("V_TPID",
						recordSection.getString("V_TPID"));
				recordEvaluation.setColumn("V_BID_SECTION_GROUP_CODE",
						recordSection.getString("V_BID_SECTION_GROUP_CODE"));
				recordEvaluation.setColumn("V_BID_SECTION_CODE",
						recordSection.getString("V_BID_SECTION_CODE"));
				recordEvaluation.setColumn("V_BID_SECTION_ID",
						recordSection.getString("V_BID_SECTION_ID"));
				dao.auto()
						.table(ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION)
						.save(recordEvaluation);
			}
			/*** 第二步 *****/
			/*
			 * for (Record<String, Object> recordFrist : Gi) { JSONObject
			 * jsonObject = JSONObject.parseObject(recordFrist
			 * .getString("V_JSON")); JSONObject jsonObjectUser =
			 * JSONObject.parseObject(jsonObject .getString("objUser")); //
			 * 判断投标人登记B C D if (StringUtils
			 * .equals(jsonObjectUser.getString("tbRatingsInEvl"), "B") ||
			 * StringUtils .equals(jsonObjectUser.getString("tbRatingsInEvl"),
			 * "C") || StringUtils
			 * .equals(jsonObjectUser.getString("tbRatingsInEvl"), "D") ) {
			 * L.add(recordFrist); } z += recordFrist.getDouble("N_PRICE"); }
			 */

			if (Hi.size() < (Math.round(((float) m * (float) 2 / (float) 3))))
			{
				int k = (int) (z % L.size());
				Record<String, Object> jRecord = new RecordImpl<String, Object>();
				jRecord.setColumn("publicity", "mod(" + FMT_D.format(z) + "，"
						+ L.size() + ")");
				jRecord.setColumn("bidderNo", L.get(k).getString("V_BIDDER_NO"));
				jRecord.setColumn("number", k);
				jRecord.setColumn("price",
						FMT_D.format(L.get(k).getDouble("N_PRICE")));
				jRecord.setColumn("allPrice", FMT_D.format(z));
				jsonArr.add(jRecord);
				// if (compare(record, L.get(k)))
				// {
				// recordEvaluation.setColumn("N_VALID_GRADE", 1);
				// }
				Hi.add(L.get(k));
				z = z - L.get(k).getDouble("N_PRICE");
				sql = "UPDATE "
						+ ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION
						+ " SET N_VALID_OFFER='1',N_VALID_GRADE='1' "
						+ "WHERE V_TPID=#{aid} AND V_BID_SECTION_ID=#{section_id} AND V_NAME=#{name}";
				dao.sql(sql)
						.setParam("aid", SessionUtils.getTPID())
						.setParam("section_id",
								record.getString("V_BID_SECTION_ID"))
						.setParam("name", L.get(k).getString("V_BIDDER_NAME"))
						.update();
				L.remove(k);
				Hi = putHI(z, L, Hi, m, L.size(), k, record, recordEvaluation,
						dao, jsonArr);
			}
			/*** 第三部生成评标基准价 *******/
			for (Record<String, Object> r : Hi)
			{
				if (null == r.getDouble("N_PRICE"))
				{
					r.setColumn("N_PRICE", "0");
				}
				pi += r.getLong("N_PRICE");
			}
			int k3 = 0;
			n = Hi.size();
			// 有效投标家数总价

			recordEvaluation.setColumn("N_VALID_SUM_PRICE", pi);

			// 大于5家企业
			if (n > 5)
			{
				k3 = (int) (pi % 5) + 1;
				qi = kqi(k3, pi, n, xi, Hi, recordEvaluation);
			}
			else if (n < 6)
			{
				k3 = (int) (pi % 3) + 1;
				if (k3 == 3)
				{
					k3 = 5;
				}
				qi = kqi(k3, pi, n, xi, Hi, recordEvaluation);
			}

			// 计算评标基准价
			f1 = 0;
			if (qi >= xi * 0.95)
			{
				f1 = Arith.add(Math.round(qi) % 7 * 0.005, 0.01);
				f1 = Utils.interval(f1, 0.01, 0.04, true, true);
			}
			else if (xi * 0.95 >= qi && qi > xi * 0.9)
			{
				f1 = Arith.sub(Math.round(qi) % 7 * 0.005, 0.01);
				f1 = Utils.interval(f1, -0.01, 0.02, true, true);
			}
			/*
			 * else if (xi * 0.9 >= qi) { f1 = Arith.sub(Math.round(qi) % 7 *
			 * 0.005, 0.03); f1 = Utils.interval(f1, -0.03, 0, true, true); }
			 */
			// 修改通知（补遗6,2017.12.20）
			// 将③中的下浮区间修改为与①中的对称，即修改为-4%～-1%。路基、路面、土建监理、试验检测招标文件同步修改
			else if (xi * 0.9 >= qi)
			{
				// 修改日期（2018.02.20）根据2017.12.28的勘误，由下浮系数f1＝mod(qi，7)*0.5%-3%修改为f1＝mod(qi，7)*0.5%-4%
				f1 = Arith.sub(Math.round(qi) % 7 * 0.005, 0.04);
				f1 = Utils.interval(f1, -0.04, -0.01, true, true);
			}
			ri = Math.round((Math.round(qi) * (1 - f1)));
			/*
			 * recordEvaluation.setColumn("N_DOWN", f1);
			 * recordEvaluation.setColumn("N_EVALUATION_PRICE", ri);
			 */
			// 评标基准价记录表
			sql = "UPDATE "
					+ ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION
					+ " SET N_DOWN=#{N_DOWN},N_EVALUATION_PRICE=#{N_EVALUATION_PRICE},N_METHOD=#{N_METHOD},"
					+ "N_AVG=#{N_AVG},N_CONTROL_WEIGHT=#{N_CONTROL_WEIGHT},N_WEIGHTING_AVG=#{N_WEIGHTING_AVG},N_MAXPRICE=#{N_MAXPRICE},"
					+ "N_MINPRICE=#{N_MINPRICE},N_SEVENTY_SUM=#{N_SEVENTY_SUM},N_FIFTY_SUM=#{N_FIFTY_SUM},N_EIGHT_PRICE=#{N_EIGHT_PRICE},"
					+ "N_ALLPRICE_AVG=#{N_ALLPRICE_AVG},V_DU_PRICE=#{V_DU_PRICE},N_DU_PRICE_AVG=#{N_DU_PRICE_AVG},N_VALID_SUM_PRICE=#{N_VALID_SUM_PRICE},V_JSON_OBJ=#{V_JSON_OBJ} "
					+ "WHERE V_TPID=#{aid} AND V_BID_SECTION_ID=#{section_id}";
			JSONObject jobj = new JSONObject();
			jobj.put("extract", jsonArr);
			dao.sql(sql)
					.setParam("N_DOWN", f1)
					.setParam("N_EVALUATION_PRICE", ri)
					.setParam("N_METHOD",
							recordEvaluation.getString("N_METHOD"))
					.setParam("N_AVG", recordEvaluation.getString("N_AVG"))
					.setParam("N_CONTROL_WEIGHT",
							recordEvaluation.getString("N_CONTROL_WEIGHT"))
					.setParam("N_WEIGHTING_AVG",
							recordEvaluation.getString("N_WEIGHTING_AVG"))
					.setParam("N_MAXPRICE",
							recordEvaluation.getString("N_MAXPRICE"))
					.setParam("N_MINPRICE",
							recordEvaluation.getString("N_MINPRICE"))
					.setParam("N_SEVENTY_SUM",
							recordEvaluation.getString("N_SEVENTY_SUM"))
					.setParam("N_FIFTY_SUM",
							recordEvaluation.getString("N_FIFTY_SUM"))
					.setParam("N_EIGHT_PRICE",
							recordEvaluation.getString("N_EIGHT_PRICE"))
					.setParam("N_ALLPRICE_AVG",
							recordEvaluation.getString("N_ALLPRICE_AVG"))
					.setParam("V_DU_PRICE",
							recordEvaluation.getString("V_DU_PRICE"))
					.setParam("N_DU_PRICE_AVG",
							recordEvaluation.getString("N_DU_PRICE_AVG"))
					.setParam("V_JSON_OBJ", jobj.toString())
					.setParam("N_VALID_SUM_PRICE",
							recordEvaluation.getDouble("N_VALID_SUM_PRICE"))
					.setParam("aid", SessionUtils.getTPID())
					.setParam("section_id",
							record.getString("V_BID_SECTION_ID")).update();
			// 生成或者修改评标基准价格
			sql = "UPDATE "
					+ ConstantEOKB.TableName.EKB_T_SECTION_INFO
					+ " SET N_EVALUATION_PRICE=#{eprice} WHERE V_TPID=#{aid} AND V_BID_SECTION_ID=#{section_id}";
			dao.sql(sql)
					.setParam("eprice", ri)
					.setParam("aid", SessionUtils.getTPID())
					.setParam("section_id",
							record.getString("V_BID_SECTION_ID")).update();
		}
	}

	/**
	 * 
	 * 递归生成HI<br/>
	 * <p>
	 * </p>
	 * 
	 * @param sumPrice
	 *            投标企业总价
	 * @param listSectionB
	 *            B级企业集合
	 * @param listSectionSecond
	 *            HI集合
	 * @param listSection
	 *            总集合
	 * @param b_size
	 *            B级企业数量
	 * @param k
	 *            余数
	 * @return
	 */
	public static List<Record<String, Object>> putHI(double z,
			List<Record<String, Object>> L, List<Record<String, Object>> Hi,
			int m, int b_size, int k, Record<String, Object> record,
			Record<String, Object> recordEvaluation, ActiveRecordDAO dao,
			JSONArray jsonArr)
	{
		// b_size--;
		if (Hi.size() == (Math.round(((float) m * (float) 2 / (float) 3))))
		{
			return Hi;
		}
		else
		{
			k = (int) (z % L.size());
			Record<String, Object> jRecord = new RecordImpl<String, Object>();
			jRecord.setColumn("publicity",
					"mod(" + FMT_D.format(z) + "，" + L.size() + ")");
			jRecord.setColumn("bidderNo", L.get(k).getString("V_BIDDER_NO"));
			jRecord.setColumn("number", k);
			jRecord.setColumn("price",
					FMT_D.format(L.get(k).getDouble("N_PRICE")));
			jRecord.setColumn("allPrice", FMT_D.format(z));
			jsonArr.add(jRecord);
			// if (compare(record, L.get(k)))
			// {
			// recordEvaluation.setColumn("N_VALID_GRADE", 1);
			// }
			Hi.add(L.get(k));
			z = z - L.get(k).getDouble("N_PRICE");

			String sql = "UPDATE "
					+ ConstantEOKB.TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION
					+ " SET N_VALID_OFFER='1',N_VALID_GRADE='1' "
					+ "WHERE V_TPID=#{aid} AND V_BID_SECTION_ID=#{section_id} AND V_NAME=#{name}";
			try
			{
				dao.sql(sql)
						.setParam("aid", SessionUtils.getTPID())
						.setParam("section_id",
								record.getString("V_BID_SECTION_ID"))
						.setParam("name", L.get(k).getString("V_BIDDER_NAME"))
						.update();
			}
			catch (DAOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			L.remove(k);
		}
		return putHI(z, L, Hi, m, L.size(), k, record, recordEvaluation, dao,
				jsonArr);
	}

	/**
	 * 
	 * 计算加权平均值<br/>
	 * <p>
	 * </p>
	 * 
	 * @return 加权平均值
	 */
	public static double kqi(int k, double pi, int n, double xi,
			List<Record<String, Object>> hil,
			Record<String, Object> recordEvaluation)
	{

		LinkedList<Double> hi = new LinkedList<Double>();
		Double d = null;
		for (Record<String, Object> r : hil)
		{
			d = r.getDouble("N_PRICE");
			hi.add(d);
		}
		recordEvaluation.setColumn("N_METHOD", k);// 方法几
		Collections.sort(hi);
		switch (k)
		{
			case 1:
				return kqi1(pi, n, recordEvaluation);
			case 2:
				return kqi2(pi, n, xi, recordEvaluation);
			case 3:
				return kqi3(pi, n, hi, hil, recordEvaluation);
			case 4:
				return kqi4(n, hi, recordEvaluation);
			case 5:
				return kqi5(xi, hi, recordEvaluation);
			default:
				return kqi1(pi, n, recordEvaluation);
		}
	}

	/**
	 * 取开闭值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param src
	 *            源值
	 * @param min
	 *            左值
	 * @param max
	 *            右值
	 * @param emin
	 *            左侧是否为闭区间
	 * @param emax
	 *            右侧是否为闭区间
	 * @return 取值
	 */
	private static double interval(double src, double min, double max,
			boolean emin, boolean emax)
	{
		// 如果小于最小值
		if (src < min)
		{
			return min;
		}
		// 如果等于最小值且左边是闭区间
		if (emin && src == min)
		{
			return min;
		}
		// 右开闭处理
		if (src > max)
		{
			return max;
		}
		if (emax && src == max)
		{
			return max;
		}
		//
		return src;
	}

	/**
	 * 求平均值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param list
	 * @return
	 */
	private static double avg(List<Double> list)
	{
		double total = sum(list);
		return total / list.size();
	}

	/**
	 * 求和<br/>
	 * <p>
	 * </p>
	 * 
	 * @param list
	 * @return
	 */
	private static double sum(List<Double> list)
	{
		double total = 0d;
		for (Double d : list)
		{
			total += d;
		}
		return total;
	}

	private static double kqi1(double pi, int n,
			Record<String, Object> recordEvaluation)
	{
		// 即k=1时,平均值qi＝pi/n
		recordEvaluation.setColumn("N_AVG", pi / n);
		return pi / n;
	}

	private static double kqi2(double pi, int n, double xi,
			Record<String, Object> recordEvaluation)
	{
		// f= mod(n,6)*4%+20% (20%≤f≤40%) 新版本 mod(pi,6)*4%+20% (20%≤f≤40%)
		recordEvaluation.setColumn("N_AVG", Math.round(pi / n));
		double f = pi % 6 * 0.04 + 0.2;
		f = interval(f, 0.2, 0.4, true, true);
		recordEvaluation.setColumn("N_CONTROL_WEIGHT", f);
		// 加权平均值qi=平均值*(100%-f)+ 最高限价*f
		// = pi/n*(100%-f)+ xi**f
		recordEvaluation
				.setColumn("N_WEIGHTING_AVG", pi / n * (1 - f) + xi * f);
		return pi / n * (1 - f) + xi * f;
	}

	private static double kqi3(double pi, int n, LinkedList<Double> hi,
			List<Record<String, Object>> hil,
			Record<String, Object> recordEvaluation)
	{
		// System.out.println(JSON.toJSONString(hil));
		// 当n≤8时，从Ｈi去掉一个最高和一家最低报价后，取其余投标价计算平均值qi；
		if (8 >= n)
		{
			recordEvaluation.setColumn("N_MAXPRICE", hi.removeLast()); // 最高报价
			recordEvaluation.setColumn("N_MINPRICE", hi.removeFirst()); // 最低报价
			recordEvaluation.setColumn("N_SEVENTY_SUM", sum(hi)); // 抽出百分之70%投标人的投标价总值
			recordEvaluation.setColumn("N_AVG", avg(hi));// 平均值
			return avg(hi);
		}
		// 当9≤n≤16时，从队列Ｈi中随机抽出70%的投标人（四舍五入）的投标价，再计算平均值:
		// 第一步：先将队列Ｈi按投标人编号升序排列。
		// L队列清空。j=n*70%, （n1四舍五入）
		// 第二步：若j=0,则抽取结束，计算L队列中投标报价的平均值qi。否则：
		// 计算余数k=mod(pi，n)+1，则从Hi队列中抽取第k家企业投标报价，加入队列L中。
		// 第三步：将Ｈi队列中序号k后面的投标报价前移一个位置。
		// 计算：j＝j－１, n=n-1
		// pi=pi－已被抽取的那家投标人的报价。
		// 重复第二步。

		ListSortUtils.sort(hil, true, "V_BIDDER_NO");
		if (9 <= n && n <= 16)
		{

			int count = (int) Math.round(hi.size() * 0.7);
			List<Double> temp = randomList(pi, n, hil, count);
			recordEvaluation.setColumn("N_SEVENTY_SUM", sum(temp)); // 抽出百分之70%投标人的投标价总值
			recordEvaluation.setColumn("N_AVG", avg(temp));// 平均值
			return avg(temp);
		}
		// 当n>16时, 从队列Ｈi中随机抽出50％的投标人（四舍五入）的投标价计算平均值。方法如上，只需将70%改为５０％即可。
		int count = (int) Math.round(hi.size() * 0.5);
		List<Double> temp = randomList(pi, n, hil, count);
		recordEvaluation.setColumn("N_FIFTY_SUM", sum(temp)); // 抽出百分之50%投标人的投标价总值
		recordEvaluation.setColumn("N_AVG", avg(temp));// 平均值
		return avg(temp);
	}

	private static double kqi4(int n, LinkedList<Double> hi,
			Record<String, Object> recordEvaluation)
	{
		// 当n≤10时，从队列Ｈi中去掉一个最高和最低报价后，取其余投标价计算平均值qi
		if (n <= 10)
		{
			recordEvaluation.setColumn("N_MAXPRICE", hi.removeLast()); // 最高报价
			recordEvaluation.setColumn("N_MINPRICE", hi.removeFirst()); // 最低报价
			recordEvaluation.setColumn("N_AVG", avg(hi)); // 平均值
			recordEvaluation.setColumn("N_EIGHT_PRICE", sum(hi));// 取总值
			return avg(hi);
		}
		// 当n>10时，从队列Ｈi中通过先去掉一个最低报价后，再去掉一个最高报价的顺序进行循环，取中间8个投标价计算平均值pi。
		while (true)
		{
			hi.removeFirst();
			if (hi.size() == 8)
			{
				break;
			}
			hi.removeLast();
			if (hi.size() == 8)
			{
				break;
			}
		}
		recordEvaluation.setColumn("N_EIGHT_PRICE", sum(hi));// 取中间8个投标价的总值
		recordEvaluation.setColumn("N_AVG", avg(hi));// 平均值
		return avg(hi);
	}

	private static double kqi5(double xi, LinkedList<Double> hi,
			Record<String, Object> recordEvaluation)
	{

		// 计算队列Ｈi中投标价的平均值pi，再将低于最高限价xi且高于该平均值qi的投标价再一次进行平均。[1000.0,1006.0]
		double pi = avg(hi);
		recordEvaluation.setColumn("N_ALLPRICE_AVG", pi);// 所有进入计算的投标价平均值
		// 这个如果出现只有一家的情况
		if (1 == hi.size())
		{
			recordEvaluation.setColumn("V_DU_PRICE", "不符合");
			recordEvaluation.setColumn("N_DU_PRICE_AVG", pi);
			return pi;
		}
		List<Double> temp = new LinkedList<Double>();
		for (Double h : hi)
		{
			// 这个地方需要讨论
			if (xi > h && h >= pi)
			{
				recordEvaluation.setColumn("V_DU_PRICE", "符合");
				temp.add(h);
			}
			else
			{
				recordEvaluation.setColumn("V_DU_PRICE", "不符合");
			}
		}
		/*
		 * // 这个如果出现平均值高于XI recordEvaluation.setColumn("N_DU_PRICE_AVG", xi);
		 * if(pi>=xi){ return xi; }
		 */
		if (temp.size() == 0)
		{
			recordEvaluation.setColumn("N_DU_PRICE_AVG", "0");
			return 0;
		}
		recordEvaluation.setColumn("N_DU_PRICE_AVG", avg(temp));
		return avg(temp);
	}

	/**
	 * 随机抽指定个数的值重组列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param n
	 * @param hil
	 * @param j
	 * @return
	 */
	public static List<Double> randomList(double pi, int n,
			List<Record<String, Object>> hil, int j)
	{
		List<Record<String, Object>> hit = copyList(hil);
		// 第一步：先将队列Ｈi按投标人编号升序排列。
		// L队列清空。j=n*70%, （n1四舍五入）
		// 第二步：若j=0,则抽取结束，计算L队列中投标报价的平均值qi。否则：
		// ----原来计算公式---- 计算余数k=mod(pi，n)+1，则从Hi队列中抽取第k家企业投标报价，加入队列L中。
		// ----现在计算---- 计算余数k=mod(pi，n)，则从Hi队列中抽取第k家企业投标报价，加入队列L中。
		// 第三步：将Ｈi队列中序号k后面的投标报价前移一个位置。
		// 计算：j＝j－１, n=n-1
		// pi=pi－已被抽取的那家投标人的报价。
		// 重复第二步。
		List<Double> result = new LinkedList<Double>();
		int k = 0;
		Double current = null;
		for (; j > 0; j--)
		{
			k = (int) (pi % n);
			// System.out.println("K:" + k);
			// System.out.println("N:" + n);
			// System.out.println("PI:" + pi);
			// print(hit);
			current = hit.get(k).getDouble("N_PRICE");
			result.add(current);
			// System.out.println(hit.get(k).getString("V_BIDDER_NAME") + ":"
			// + hit.get(k).getDouble("N_PRICE"));
			hit.remove(k);
			// System.out.println("------------------\r\n");
			pi -= current;
			n--;
		}
		return result;
	}

	// private static void print(List<Record<String, Object>> tt)
	// {
	// int index = 0;
	// for (Record<String, Object> t : tt)
	// {
	// // System.out.println(index + ":" + t.getString("V_BIDDER_NAME"));
	// index++;
	// }
	//
	// }

	/**
	 * 拷贝<br/>
	 * <p>
	 * </p>
	 * 
	 * @param source
	 * @return
	 */
	private static <T> List<T> copyList(List<T> source)
	{
		List<T> temp = new LinkedList<T>();
		for (T t : source)
		{
			temp.add(t);
		}
		return temp;
	}

	/**
	 * 
	 * 比较<br/>
	 * <p>
	 * 函数的详细描述
	 * </p>
	 * 
	 * @param list
	 * @param value
	 * @return
	 */
	public static boolean compare(Record<String, Object> r,
			Record<String, Object> r1)
	{
		if (StringUtils.equals(r.getString("V_BID_SECTION_ID"),
				r1.getString("V_BID_SECTION_ID"))
				&& StringUtils.equals(r.getString("V_BIDDER_NAME"),
						r1.getString("V_BIDDER_NAME")))
		{
			return true;
		}
		return false;
	}
}
