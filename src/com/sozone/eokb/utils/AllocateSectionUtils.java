/**
 * 包名：com.sozone.eokb.common.bus.utils;
 * 文件名：AllocateSectionUtils.java<br/>
 * 创建时间：2017-8-2 下午3:23:55<br/>
 * 创建者：jack<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.DAOException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;

/**
 * 标段分配工具类<br/>
 * Time：2017-8-2 下午3:23:55<br/>
 * 
 * @author jack
 * @version 1.0.0
 * @since 1.0.0
 */
public class AllocateSectionUtils
{

	private static Logger logger = LoggerFactory
			.getLogger(AllocateSectionUtils.class);

	/**
	 * 获取最左最小值
	 * 
	 * @param sectionGroupList
	 * @param mark
	 * @return
	 */
	private static int getLeftAndLeaveOfGroupList(
			List<Record<String, Object>> sectionGroupList, boolean[] mark)
	{

		// 下标
		int index = -1;
		// 最小
		Record<String, Object> minRecord = null;
		// 临时
		Record<String, Object> tempRecord = null;
		for (int i = 0; i < mark.length; i++)
		{
			// 排除不可分配标段
			if (mark[i])
			{
				index = i;
				minRecord = sectionGroupList.get(i);
				break;
			}
		}
		if (index == -1)
		{
			// 找不到可分配标段，直接退出
			return index;
		}

		for (int i = index + 1; i < mark.length; i++)
		{
			// 排除不可分配标段
			if (mark[i])
			{
				tempRecord = sectionGroupList.get(i);
				int minNum = minRecord.getInteger("V_NUM");
				int tempNum = tempRecord.getInteger("V_NUM");
				if (tempNum < minNum)
				{
					// 最左最小
					index = i;
					minRecord = sectionGroupList.get(i);
				}
			}
		}
		return index;
	}

	/**
	 * 获取Boolean数组，默认值<br/>
	 * 
	 * @param size
	 * @param b
	 * @return
	 */
	private static boolean[] getMarkBooleanArray(int size, boolean b)
	{
		if (size < 1)
		{
			return null;
		}
		boolean[] mark = new boolean[size];
		for (int i = 0; i < mark.length; i++)
		{
			mark[i] = b;
		}
		return mark;
	}

	/**
	 * 添加投标人到标段的投标人列表
	 * 
	 * @param record
	 * @param rd
	 */
	private static void addBidderToSectionGroupList(
			Record<String, Object> record, Record<String, Object> rd)
	{
		List<Record<String, Object>> list;
		// 判断企业列表是否存在
		if (record.containsKey("V_ENT_LIST"))
		{
			// 存在，则获取
			list = record.getColumn("V_ENT_LIST");
		}
		else
		{
			// 不存在，则创建
			list = new ArrayList<Record<String, Object>>();
		}
		// 将分配到标段信息赋值给企业
		rd.setColumn("V_BID_SECTION_ID", record.getString("V_BID_SECTION_ID"));
		// rd.setColumn("V_BID_SECTION_ID",
		// record.getString("V_BID_SECTION_ID"));
		// 将企业信息添加到该标段的企业列表
		list.add(rd);
		// 将标段的企业列表数量加1，设置V_NUM,不用list.size(),避免判断会比较复杂
		record.setColumn("V_NUM", record.getInteger("V_NUM") + 1);
		// 重新将列表设置回去，以免第一次创建未设置进去
		record.setColumn("V_ENT_LIST", list);
	}

	/**
	 * 
	 * 选择标段入围企业（电子摇号算法）<br/>
	 * <p>
	 * 1、获取投标人列表
	 * </p>
	 * <p>
	 * 2、获取标段列表
	 * </p>
	 * <p>
	 * 3、计算
	 * </p>
	 * <p>
	 * 4、将入围企业信息存储到入围企业B表
	 * </p>
	 * 
	 * @param aid
	 *            AID
	 * @param groupCode
	 *            标段组编号
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static void allocateSection(String aid, String groupCode)
			throws ServiceException
	{
		// 获取标段组与标段信息列表
		List<Record<String, Object>> sectionGroupList = getSectionGroupList(
				aid, groupCode);

		// 获取投标企业列表
		List<Record<String, Object>> list = getBidderList(aid, groupCode,
				sectionGroupList.size());

		// 按照算法排序要求，对企业信息进行排序，按照等级以及解密时间
		// Collections.sort(list, new BidderComparator());
		int n_number=1;//排序序号
		// 遍历所有企业信息
		for (Record<String, Object> rd : list)
		{
			// 对企业信息无法分配到的标段进行标记，在获取最左最小的标段排除这些无法分配到标段，最开始默认都可分配
			boolean[] mark = getMarkBooleanArray(sectionGroupList.size(), true);
			while (true)
			{
				// 获取最左最小的标段
				int index = getLeftAndLeaveOfGroupList(sectionGroupList, mark);
				// 如果找不到，就代表所有的标段企业都无法分配，就是每个标段下都有与他相关的企业
				if (index == -1)
				{
					break;
				}
				// 判断是否能被分配
				if (canAllocate(sectionGroupList.get(index), rd,n_number))
				{
					n_number++;
					// 将企业信息添加到被分配该标段企业列表里
					addBidderToSectionGroupList(sectionGroupList.get(index), rd);
					// 能被分配,完成该企业的标段分配
					break;
				}
				n_number++;
				// 不能被分配,将这标段标记为不可分配
				mark[index] = false;
			}
		}
		// 保存分配信息
		saveBidderList(list);
	}

	/**
	 * 分配AID（招标项目）下所有的标段组标段
	 * 
	 * @throws ServiceException
	 *             ServiceException
	 */
	public static void allocateSectionOfAllGroup() throws ServiceException
	{
		// BidOpenUtils.setAID("af9904febc0a4e1089b314286758a1f2");
		ActiveRecordDAO dao = getActiveRecordDAO();
		String aid = SessionUtils.getTPID();
		String sql = "SELECT V_TPID,V_BID_SECTION_GROUP_CODE FROM EKB_T_SECTION_INFO "
				+ " WHERE V_TPID = '"
				+ aid
				+ "' AND V_BID_OPEN_STATUS!='10-1' GROUP BY V_TPID,V_BID_SECTION_GROUP_CODE";
		// 获取AID对应所有标段组信息
		List<Record<String, Object>> list = dao.sql(sql).list();
		for (int i = 0; null != list && i < list.size(); i++)
		{
			Record<String, Object> record = list.get(i);
			// 对特定标段组进行标段分配
			allocateSection(aid, record.getString("V_BID_SECTION_GROUP_CODE"));
		}
	}

	/**
	 * 保存入围企业信息<br/>
	 * </p>
	 * 
	 * @param list
	 * @throws ServiceException
	 */
	private static void saveBidderList(List<Record<String, Object>> list)
			throws ServiceException
	{
		ActiveRecordDAO dao = getActiveRecordDAO();
		if (CollectionUtils.isEmpty(list))
		{
			return;
		}
		Record<String, Object> param = null;
		for (Record<String, Object> record : list)
		{
			String sectionID = record.getString("V_BID_SECTION_ID");
			if (StringUtils.isEmpty(sectionID))
			{
				continue;
			}

			param = new RecordImpl<String, Object>();
			param.setColumn("AID", record.getString("V_TPID"));
			param.setColumn("SECTION_ID", record.getString("V_BID_SECTION_ID"));
			param.setColumn("NAME", record.getString("V_BIDDER_NAME"));
			Record<String, Object> recordB = dao.auto()
					.table(TableName.EKB_T_DECRYPT_INFO)
					.setCondition("AND", "V_TPID = #{AID}")
					.setCondition("AND", "V_BID_SECTION_ID = #{SECTION_ID}")
					.setCondition("AND", "V_BIDDER_NAME = #{NAME}").get(param);
			recordB.setColumn("N_CREATE_TIME", System.currentTimeMillis());
			recordB.setColumn("V_BIDDER_NAME",
					record.getString("V_BIDDER_NAME"));
			recordB.setColumn("V_JSON_OBJ", recordB.getString("V_JSON_OBJ"));
			// 先删除再插入
			dao.auto().table(TableName.EKB_T_TENDER_LIST)
					.remove(recordB.getString("ID"));
			dao.auto().table(TableName.EKB_T_TENDER_LIST).save(recordB);
		}
	}

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

	/**
	 * 获取投标人列表
	 * 
	 * @param aid
	 * @param groupCode
	 * @param sectionCount
	 * @return
	 * @throws ServiceException
	 */
	private static List<Record<String, Object>> getBidderList(String aid,
			String groupCode, int sectionCount) throws ServiceException
	{
		ActiveRecordDAO dao = getActiveRecordDAO();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("AID", aid);
		param.setColumn("GROUP_CODE", groupCode);
		/*
		 * List<Record<String, Object>> list = dao .auto()
		 * .table(TableName.EKB_T_DECRYPT_INFO) .setCondition("AND",
		 * "V_TPID = #{AID}") .setCondition("AND",
		 * "V_BID_SECTION_GROUP_CODE = #{GROUP_CODE}")
		 * .addSortOrder("V_BIDDER_NAME", "DESC").list(param);
		 */
		List<Record<String, Object>> list = dao
				.auto()
				.table(TableName.EKB_T_DECRYPT_INFO)
				.setCondition("AND", "V_TPID = #{AID}")
				.setCondition("AND", "V_BID_SECTION_GROUP_CODE = #{GROUP_CODE}")
				.setCondition("AND", "N_ENVELOPE_0 = 1")
				.addSortOrder("V_BIDDER_NO", "ASC").list(param);
		List<Record<String, Object>> resultList = new ArrayList<Record<String, Object>>();
		Record<String, Object> resultOne = null;
		// 临时组织机构代码
		String tempOrgCode = "";
		for (Record<String, Object> record : list)
		{
			// 多转一的过滤
			String orgCode = record.getString("V_BIDDER_ORG_CODE");
			if (StringUtils.equals(tempOrgCode, orgCode))
			{
				continue;
			}
			resultOne = new RecordImpl<String, Object>();
			resultOne.setColumn("V_TPID", aid);
			resultOne.setColumn("V_BIDDER_NO", record.getString("V_BIDDER_NO"));
			resultOne.setColumn("V_BIDDER_NAME",
					record.getString("V_BIDDER_NAME"));
			resultOne.setColumn("V_DECRYPT_TIME",
					record.getString("V_DECRYPT_TIME"));
			String json = record.getString("V_JSON_OBJ");
			//JSONObject jsonObj = (JSONObject) JSON.parse(json);
			//JSONObject objUser = (JSONObject) jsonObj.get("objUser");
			resultOne.setColumn("V_CREDIT_LEVEL",
					BidderElementParseUtils.getSingObjAttribute(json, "tbRatingsInEvl"));
			// String orgCode = record.getString("V_NAME");
			// 查询当前企业信息
			Record<String, Object> entInfo = dao
					.auto()
					.table(ConstantEOKB.TableName.EKB_T_CORRELATE_ENTER)
					.setCondition("AND", "V_TPID = #{TPID}")
					.setCondition("AND",
							"(V_UNIFY_CODE = #{code} OR V_ORG_CODE = #{code})")
					.get(new RecordImpl<String, Object>()
							.setColumn("TPID", aid).setColumn("code",
									record.getString("V_BIDDER_ORG_CODE")));
			if (CollectionUtils.isEmpty(entInfo))
			{
				throw new ValidateException("E-2003");
			}

			String entRelateNo = entInfo.getString("V_CORRELATE_CODE");
			resultOne.setColumn("V_ENT_RELATE_NO", entRelateNo);
			// 查询关联企业家数
			int relateNum = dao
					.auto()
					.table(ConstantEOKB.TableName.EKB_T_CORRELATE_ENTER)
					.setCondition("AND", "V_TPID = #{AID}")
					.setCondition("AND", "V_CORRELATE_CODE = #{CORRELATE_CODE}")
					.count(new RecordImpl<String, Object>().setColumn("AID",
							aid).setColumn("CORRELATE_CODE", entRelateNo));
			// 设置家数
			resultOne.setColumn("V_RELATE_NUM", relateNum);

			// String ac = objUser.getString("dgAC");
			// String[] split = StringUtils.split(ac, "##");
			// int length = split.length;
			// resultOne.setColumn("V_RELATE_NUM", length);
			// if (length > 0)
			// {
			// String[] split2 = StringUtils.split(split[0], ";");
			// if (split2.length > 0)
			// {
			// resultOne.setColumn("V_ENT_RELATE_NO", split2[0]);
			// }
			// }

			if (canAllocate(aid, groupCode, sectionCount,
					record.getString("V_BIDDER_NAME")))
			{
				resultList.add(resultOne);
			}
			tempOrgCode = orgCode;
		}

		return resultList;

		// List<Record<String, Object>> list = new ArrayList<Record<String,
		// Object>>();
		// Record<String, Object> record = new RecordImpl<String, Object>();
		// record.setColumn("V_AID", aid);
		// record.setColumn("V_NAME", "中铁十七局集团");
		// record.setColumn("V_CREDIT_LEVEL", "AA");
		// record.setColumn("V_ENT_RELATE_NO", "Z1");
		// record.setColumn("V_RELATE_NUM", 4);
		// record.setColumn("V_DECRYPT_TIME", "2017-08-01 15:50:00");
		// list.add(record);
		//
		// record = new RecordImpl<String, Object>();
		// record.setColumn("V_AID", aid);
		// record.setColumn("V_NAME", "中铁十七局一公司");
		// record.setColumn("V_CREDIT_LEVEL", "A");
		// record.setColumn("V_ENT_RELATE_NO", "Z1");
		// record.setColumn("V_RELATE_NUM", 4);
		// record.setColumn("V_DECRYPT_TIME", "2017-08-01 15:45:00");
		// list.add(record);
		//
		// record = new RecordImpl<String, Object>();
		// record.setColumn("V_AID", aid);
		// record.setColumn("V_NAME", "中铁十七局六公司");
		// record.setColumn("V_CREDIT_LEVEL", "B");
		// record.setColumn("V_ENT_RELATE_NO", "Z1");
		// record.setColumn("V_RELATE_NUM", 4);
		// record.setColumn("V_DECRYPT_TIME", "2017-08-01 15:47:00");
		// list.add(record);
		//
		// record = new RecordImpl<String, Object>();
		// record.setColumn("V_AID", aid);
		// record.setColumn("V_NAME", "中铁十七局三公司");
		// record.setColumn("V_CREDIT_LEVEL", "B");
		// record.setColumn("V_ENT_RELATE_NO", "Z1");
		// record.setColumn("V_RELATE_NUM", 4);
		// record.setColumn("V_DECRYPT_TIME", "2017-08-01 15:47:30");
		// list.add(record);
		//
		// record = new RecordImpl<String, Object>();
		// record.setColumn("V_AID", aid);
		// record.setColumn("V_NAME", "中铁十六局一公司");
		// record.setColumn("V_CREDIT_LEVEL", "A");
		// record.setColumn("V_ENT_RELATE_NO", "Z2");
		// record.setColumn("V_RELATE_NUM", 2);
		// record.setColumn("V_DECRYPT_TIME", "2017-08-01 15:40:30");
		// list.add(record);
		//
		// record = new RecordImpl<String, Object>();
		// record.setColumn("V_AID", aid);
		// record.setColumn("V_NAME", "中铁十六局三公司");
		// record.setColumn("V_CREDIT_LEVEL", "A");
		// record.setColumn("V_ENT_RELATE_NO", "Z2");
		// record.setColumn("V_RELATE_NUM", 2);
		// record.setColumn("V_DECRYPT_TIME", "2017-08-01 15:41:30");
		// list.add(record);
		//
		// record = new RecordImpl<String, Object>();
		// record.setColumn("V_AID", aid);
		// record.setColumn("V_NAME", "中铁九局");
		// record.setColumn("V_CREDIT_LEVEL", "B");
		// record.setColumn("V_ENT_RELATE_NO", "Z3");
		// record.setColumn("V_RELATE_NUM", 1);
		// record.setColumn("V_DECRYPT_TIME", "2017-08-01 15:43:30");
		// list.add(record);
		//
		// record = new RecordImpl<String, Object>();
		// record.setColumn("V_AID", aid);
		// record.setColumn("V_NAME", "中铁十八局");
		// record.setColumn("V_CREDIT_LEVEL", "AA");
		// record.setColumn("V_ENT_RELATE_NO", "Z4");
		// record.setColumn("V_RELATE_NUM", 1);
		// record.setColumn("V_DECRYPT_TIME", "2017-08-01 15:43:40");
		// list.add(record);
		//
		// record = new RecordImpl<String, Object>();
		// record.setColumn("V_AID", aid);
		// record.setColumn("V_NAME", "中交一局");
		// record.setColumn("V_CREDIT_LEVEL", "AA");
		// record.setColumn("V_ENT_RELATE_NO", "Z5");
		// record.setColumn("V_RELATE_NUM", 1);
		// record.setColumn("V_DECRYPT_TIME", "2017-08-01 15:43:50");
		// list.add(record);
		// return list;

	}

	/**
	 * 判断投标人是否对标段里面的标段都进行投标，数量判断，是否能进行分配
	 * 
	 * @param aid
	 * @param groupCode
	 * @param sectionCount
	 * @param name
	 * @return
	 * @throws DAOException
	 */
	private static boolean canAllocate(String aid, String groupCode,
			int sectionCount, String name) throws DAOException
	{
		String sql = "SELECT COUNT(ID) AS NUM FROM EKB_T_DECRYPT_INFO "
				+ " WHERE V_TPID = #{aid} AND V_BIDDER_NAME = #{name} AND V_BID_SECTION_GROUP_CODE = #{groupCode}";
		ActiveRecordDAO dao = getActiveRecordDAO();
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("aid", aid);
		params.setColumn("name", name);
		params.setColumn("groupCode", groupCode);
		int count = dao.sql(sql).build(params).count();
		if (count == sectionCount)
		{
			return true;
		}
		return false;
	}

	/**
	 * 获取标段组的所有标段信息
	 * 
	 * @param aid
	 * @param groupCode
	 * @return
	 * @throws ServiceException
	 *             ServiceException
	 */
	private static List<Record<String, Object>> getSectionGroupList(String aid,
			String groupCode) throws ServiceException
	{

		ActiveRecordDAO dao = getActiveRecordDAO();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("AID", aid);
		param.setColumn("GROUP_CODE", groupCode);
		List<Record<String, Object>> list = dao
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID=#{AID}")
				.setCondition("AND", "V_BID_SECTION_GROUP_CODE = #{GROUP_CODE}")
				// TODO 这里排序字段有坑要注意下
				.addSortOrder("V_BID_SECTION_CODE", "ASC").list(param);
		for (Record<String, Object> record : list)
		{
			record.setColumn("V_NUM", 0);
		}
		return list;
		// List<Record<String, Object>> sectionGroupList = new
		// ArrayList<Record<String, Object>>();
		// Record<String, Object> record = new RecordImpl<String, Object>();
		// record.setColumn("ID", Random.generateUUID());
		// record.setColumn("V_GROUP_CODE", "K");
		// record.setColumn("V_SECTION_ID", Random.generateUUID());
		// record.setColumn("V_SECTION_NAME", "A1");
		// record.setColumn("V_NUM", 0);
		//
		// sectionGroupList.add(record);
		// record = new RecordImpl<String, Object>();
		// record.setColumn("ID", Random.generateUUID());
		// record.setColumn("V_GROUP_CODE", "K");
		// record.setColumn("V_SECTION_ID", Random.generateUUID());
		// record.setColumn("V_SECTION_NAME", "A2");
		// record.setColumn("V_NUM", 0);
		// sectionGroupList.add(record);
		//
		// record = new RecordImpl<String, Object>();
		// record.setColumn("ID", Random.generateUUID());
		// record.setColumn("V_GROUP_CODE", "K");
		// record.setColumn("V_SECTION_ID", Random.generateUUID());
		// record.setColumn("V_SECTION_NAME", "A3");
		// record.setColumn("V_NUM", 0);
		// sectionGroupList.add(record);
		// return sectionGroupList;
	}

	/**
	 * 判断是否可分配该标段
	 * 
	 * @param record
	 * @param rd
	 * @return
	 * @throws DAOException 
	 * @throws ValidateException 
	 */
	private static boolean canAllocate(Record<String, Object> record,
			Record<String, Object> rd,int n_number) throws ValidateException, DAOException
	{
		int relateNum = rd.getInteger("V_RELATE_NUM");
		String bidderName = rd.getString("V_BIDDER_NAME");
		String bidderNO = rd.getString("V_BIDDER_NO");
		String sectionName = record.getString("V_BID_SECTION_NAME");
		ActiveRecordDAO dao = getActiveRecordDAO();
		Record<String, Object> electronicsRecord=new RecordImpl<String, Object>();
		electronicsRecord.setColumn("ID", SZUtilsID.getUUID());
		electronicsRecord.setColumn("V_TPID", SessionUtils.getTPID());
		electronicsRecord.setColumn("N_NUMBER", n_number);
		electronicsRecord.setColumn("V_BID_SECTION_ID", record.getString("V_BID_SECTION_ID"));
		electronicsRecord.setColumn("V_BID_SECTION_CODE", record.getString("V_BID_SECTION_CODE"));
		electronicsRecord.setColumn("V_BID_SECTION_NAME", record.getString("V_BID_SECTION_NAME"));
		electronicsRecord.setColumn("V_BID_SECTION_GROUP_CODE", record.getString("V_BID_SECTION_GROUP_CODE"));
		electronicsRecord.setColumn("V_BIDDER_NAME", rd.getString("V_BIDDER_NAME"));
		electronicsRecord.setColumn("V_BIDDER_NO", rd.getString("V_BIDDER_NO"));
		electronicsRecord.setColumn("V_CORRELATE_CODE", rd.getString("V_ENT_RELATE_NO"));
		// 关联企业数量为2以下，可分配
		if (relateNum < 2)
		{
			electronicsRecord.setColumn("V_TYPE", "1");
			dao.auto().table(TableName.EKB_T_ELECTRONICS).save(electronicsRecord);
			logger.debug("该企业(" + bidderNO + "-" + bidderName
					+ ")为非关联企业,分配到标段(" + sectionName + ")");
			return true;
		}
		// 如果标段没有已分配的企业列表信息可分配
		if (!record.containsKey("V_ENT_LIST"))
		{
			electronicsRecord.setColumn("V_TYPE", "2");
			dao.auto().table(TableName.EKB_T_ELECTRONICS).save(electronicsRecord);
			logger.debug("该企业(" + bidderNO + "-" + bidderName + ")为关联企业,分配到标段("
					+ sectionName + ")");
			return true;
		}
		List<Record<String, Object>> list = record.getColumn("V_ENT_LIST");
		String groupNo = rd.getString("V_ENT_RELATE_NO");
		// 关联企业编号为空，可分配
		if (StringUtils.isEmpty(groupNo))
		{
			electronicsRecord.setColumn("V_TYPE", "4");
			dao.auto().table(TableName.EKB_T_ELECTRONICS).save(electronicsRecord);
			logger.debug("该企业(" + bidderNO + "-" + bidderName + ")关联企业编号为空,分配到标段("
					+ sectionName + ")");
			return true;
		}
		// 关联企业企业判断
		for (Record<String, Object> ent : list)
		{
			String entGroupNo = ent.getString("V_ENT_RELATE_NO");
			if (StringUtils.equals(groupNo, entGroupNo))
			{
				electronicsRecord.setColumn("V_BIDDER_NAME_RELATION", ent.getString("V_BIDDER_NAME"));
				electronicsRecord.setColumn("V_BIDDER_NO_RELATION", ent.getString("V_BIDDER_NO"));
				electronicsRecord.setColumn("V_TYPE", "3");
				dao.auto().table(TableName.EKB_T_ELECTRONICS).save(electronicsRecord);
				logger.debug("该企业(" + bidderNO + "-" + bidderName + ")为关联企业,与("
						+ ent.getString("V_BIDDER_NO")+"-"+ent.getString("V_BIDDER_NAME") + ")企业,无法分配到标段("
						+ sectionName + ")");
				return false;
			}
		}
		electronicsRecord.setColumn("V_TYPE", "2");
		dao.auto().table(TableName.EKB_T_ELECTRONICS).save(electronicsRecord);
		logger.debug("该企业(" + bidderNO + "-" + bidderName + ")为关联企业,分配到标段("
				+ sectionName + ")");
		return true;
	}

}
