package com.sozone.eokb.bus.sievealgorithm;

/**
 * 包名：com.sozone.algorithm
 * 文件名：SieveAlgorithm.java<br/>
 * 创建时间：2018-1-8 下午8:46:07<br/>
 * 创建者：jack<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.sf.jxls.transformer.XLSTransformer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Page;
import com.sozone.aeolus.dao.data.Pageable;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.download.util.AeolusDownloadUtils;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.ClassLoaderUtils;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.xms_fjsz.common.FjszUtils;

/**
 * 
 * 投标人随机编号与预选参与摇球投标人<br/>
 * <p>
 * 投标人随机编号与预选参与摇球投标人<br/>
 * </p>
 * Time：2018-5-23 下午2:46:24<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/sa", desc = "投标人随机编号与预选参与摇球投标人")
// 登录即可访问
@Permission(Level.Authenticated)
public class SieveAlgorithm
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(SieveAlgorithm.class);

	/**
	 * 开标EXCEL模板路径
	 */
	private static String eokbExcelPath = "/com/sozone/eokb/bus/sievealgorithm/bidder_deliver_time_template.xls";

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
	 * 
	 * 导出投标人投递信息列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/export_bidders", desc = "导出投标人投递信息列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void exportBidderList(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("导出投标人信息列表", data));
		HttpServletResponse response = data.getHttpServletResponse();
		String fileName = "投标人投递时间列表.xls";
		String mimeType = AeolusDownloadUtils.getMimeType(fileName);
		response.setContentType(mimeType);
		response.setHeader("Content-Disposition", "attachment;filename="
				+ AeolusDownloadUtils.encodeFileName(data, fileName));
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", SessionUtils.getTPID());
		// 投标人信息列表
		List<Record<String, Object>> bidders = getActiveRecordDAO().statement()
				.selectList("xms_fjsz_common.getBidderBiddingTimeInfo", params);
		params.setColumn("bidders", bidders);

		InputStream input = null;
		OutputStream out = null;
		try
		{
			input = ClassLoaderUtils.getResourceAsStream(eokbExcelPath,
					this.getClass());
			XLSTransformer transformer = new XLSTransformer();
			transformer.groupCollection("department.staff");
			Workbook resultWorkbook = transformer.transformXLS(input, params);
			// 获取输出流
			out = response.getOutputStream();
			resultWorkbook.write(out);
			out.flush();
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("导出投标人信息列表发生异常!"), e);
			throw new ServiceException("", "导出投标人信息列表发生异常!", e);
		}
		finally
		{
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * 
	 * 获取是否需要筛选标识<br/>
	 * <p>
	 * 由于施工经评审AB有可能超过450家需要进行筛选摇号，先判断家数，如果小于450家直接下一步，如果大于450家且未筛选，则进行
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 是否需要筛选标识
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "", desc = "获取是否需要筛选标识")
	@Service
	public boolean getCanSieveFlag(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取是否需要筛选标识", data));

		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		int count = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid}").count(param);
		if (count > 450)
		{
			return true;
		}

		return false;
	}

	/**
	 * 
	 * 获取是否需要筛选标识<br/>
	 * <p>
	 * 由于施工经评审AB有可能超过450家需要进行筛选摇号，先判断家数，如果小于450家直接下一步，如果大于450家且未筛选，则进行
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 是否需要筛选标识
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "bc", desc = "获取是否需要筛选标识")
	@Service
	public boolean getCanSieveBcFlag(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取是否需要筛选标识", data));

		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		int count = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid} AND N_ENVELOPE_9 =0")
				.count(param);
		if (count > 450)
		{
			return true;
		}

		return false;
	}

	/**
	 * 
	 * 获取是否需要筛选标识（经A）<br/>
	 * <p>
	 * 获取是否需要筛选标识（经A）
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 是否需要筛选标识
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "ja", desc = "获取是否需要筛选标识（经A）")
	@Service
	public boolean getCanSieveJaFlag(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取是否需要筛选标识（经A）", data));

		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		int count = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid} AND N_ENVELOPE_9 =0")
				.count(param);
		if (count > 450)
		{
			// 获取当前招标项目的所有标段
			Record<String, Object> section = getActiveRecordDAO().auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
					.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
					.setCondition("GROUP BY", "V_BID_SECTION_NAME").get(param);

			String sectionId = section.getString("V_BID_SECTION_ID");

			int method = FjszUtils.chouMethod(tpid, sectionId, "1");

			if (method == 2)
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * 
	 * 投标人随机编号与预选参与摇球投标人<br/>
	 * <p>
	 * 投标人随机编号与预选参与摇球投标人
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws ServiceException
	 *             ServiceException
	 * @throws ParseException
	 *             ParseException
	 */
	@Path(value = "ds", desc = "获取是否需要筛选标识")
	@Service
	public void doSieve(AeolusData data) throws ServiceException,
			ParseException
	{
		logger.debug(LogUtils.format("投标人随机编号与预选参与摇球投标人", data));
		String tpid = SessionUtils.getTPID();

		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		// 先判断是否筛选过企业
		int count = getActiveRecordDAO()
				.auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid} AND V_ROLL_NO IS NOT NULL")
				.count(param);
		if (count > 0)
		{
			return;
		}

		// 获取投递信息
		List<Record<String, Object>> bidders = getActiveRecordDAO().statement()
				.selectList("xms_fjsz_common.getSieveList", param);
		if (CollectionUtils.isEmpty(bidders))
		{
			throw new ServiceException("", "无法获取投递信息");
		}

		// 小于等于450家不需要抽取
		if (bidders.size() <= 450)
		{
			return;
		}

		// 筛选450家企业
		sieveAlgoritm(bidders);
	}

	/**
	 * 
	 * 获取筛选结果<br/>
	 * <p>
	 * 获取筛选结果
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return Page
	 * @throws ServiceException
	 *             ServiceException
	 */
	@Path(value = "gsr", desc = "获取筛选结果")
	@Service
	public Page<Record<String, Object>> getSieveResult(AeolusData data)
			throws ServiceException
	{
		logger.debug(LogUtils.format("获取筛选结果", data));
		Pageable pageable = data.getPageRequest();
		Record<String, Object> param = data.getRecord();
		param.setColumn("tpid", SessionUtils.getTPID());
		return getActiveRecordDAO().auto().table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid}")
				.addSortOrder("CAST(V_ROLL_NO AS SIGNED)", "ASC")
				.page(pageable, param);
	}

	/**
	 * 
	 * 开始筛选投标人-投标人信息<br/>
	 * <p>
	 * 开始筛选投标人-投标人信息
	 * </p>
	 * 
	 * @param bidders
	 * @throws ServiceException
	 * @throws ParseException
	 */
	private void sieveAlgoritm(List<Record<String, Object>> bidders)
			throws ServiceException, ParseException
	{
		logger.debug(LogUtils.format("开始筛选投标人-投标人信息", bidders));
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		int n = bidders.size();// 投标企业家数
		long j = 0;// 总秒数
		String status;
		int index = 0;
		for (int i = 0; i < n; i++)
		{
			long s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(
					bidders.get(i).getString("V_DELIVER_TIME")).getTime();
			j += s;
			status = bidders.get(i).getString("N_ENVELOPE_9");
			if (StringUtils.isEmpty(status) || StringUtils.equals("0", status))
			{
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("NO", index + 1);
				map.put("V_ORG_CODE", bidders.get(i).getString("V_ORG_CODE"));
				map.put("V_BIDDER_NAME",
						bidders.get(i).getString("V_BIDDER_NAME"));
				list.add(map);
				index++;
			}
		}
		long x1 = 0;
		String xx = String.valueOf(Math.round(j / n)) + nextSeqByFour(n);
		x1 = Long.parseLong(xx);
		Date date = new Date(x1);
		x1 = Long.parseLong(new SimpleDateFormat("HHmmss").format(date));
		logger.debug(LogUtils.format("平均时间:"
				+ new SimpleDateFormat("HH:mm:ss").format(date)));
		logger.debug(LogUtils.format("以所有投标人标书受理时间平均秒数（时分秒）:"
				+ Math.round(j / list.size())));
		logger.debug(LogUtils.format("种子编码:" + xx + "种子位数:" + xx.length()));
		Map<String, Map<String, Object>> recodeMap = recode(x1, list.size(),
				list);
		// 将投标人编码更新到list表

		logger.debug(LogUtils.format("投标总家数:" + recodeMap.size()));
		logger.debug(LogUtils.format("投标总家数详细信息:" + recodeMap));
		Map<String, Map<String, Object>> selectMap = sieve(x1, list.size(),
				450, recodeMap);
		Map<String, Object> temMap;
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		Record<String, Object> tempRecord = new RecordImpl<String, Object>();
		List<Record<String, Object>> list1 = new ArrayList<Record<String, Object>>();
		for (String key : selectMap.keySet())
		{
			temMap = selectMap.get(key);
			param.clear();
			tempRecord = new RecordImpl<String, Object>();
			// 摇中，设置该企业解密成功状态
			param.setColumn("N_ENVELOPE_0", "1");
			param.setColumn("orgCode", temMap.get("V_ORG_CODE"));
			param.setColumn("tpid", tpid);
			tempRecord.putAll(temMap);
			list1.add(tempRecord);
			// 更新投标人分配的号码
			getActiveRecordDAO().auto().table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_BIDDER_ORG_CODE=#{orgCode}")
					.setCondition("AND", "V_TPID=#{tpid}").modify(param);
		}
		// ListSortUtils.sort(list1, true, "V_ORG_CODE");
		// String temp = "";
		// for (Record<String, Object> r : list1)
		// {
		// if (temp == r.getString("V_ORG_CODE"))
		// {
		// System.out.println(temp);
		//
		// }
		// temp = r.getString("V_ORG_CODE");
		// }
		// System.out.println(list1);
		logger.debug(LogUtils.format("整理后投标总家数:" + selectMap.size()));
		logger.debug(LogUtils.format("整理投标总家数详细信息:" + selectMap));
	}

	/**
	 * 投标人随机编号与预选参与摇球投标人<br/>
	 * 
	 * @param x1
	 *            x1的初始值可以用自然顺序排列的最后一个投标人的识别代码是最后3位数(新修改：一个种子是这个标的投递家数，)
	 * @param x2
	 *            x2的初始值可以用随机顺序后的第一个投标人的报价（以万元为单位）的整数(新修改：一个种子是投递时间的平均数，投递时间取时 分
	 *            秒 ，)
	 * @param m
	 *            随机产生m个投标人
	 * @param list
	 *            抽标人信息
	 * @return 选出来的投标人
	 */
	public Map<String, Map<String, Object>> recodeAndSieve(long x1, long x2,
			int m, List<Map<String, Object>> list) throws ServiceException
	{
		int n = list.size();
		Map<String, Map<String, Object>> recodeMap = recode(x1, n, list);
		Map<String, Map<String, Object>> selectMap = sieve(x2, n, m, recodeMap);
		return selectMap;
	}

	/**
	 * 投标人随机编号模型<br/>
	 * 
	 * @param x
	 *            x的初始值可以用自然顺序排列的最后一个投标人的识别代码是最后3位数
	 * @param n
	 *            n个抽标人
	 * @param list
	 *            抽标人信息
	 * @return 编码的抽标人信息
	 * @throws ServiceException
	 *             ServiceException
	 */
	public Map<String, Map<String, Object>> recode(long x, int n,
			List<Map<String, Object>> list) throws ServiceException
	{
		// 处理计算机排序从0开始的问题
		List<Map<String, Object>> newlist = new ArrayList<Map<String, Object>>();
		newlist.add(new HashMap<String, Object>());
		newlist.addAll(list);
		long a = 16807;
		long M = (long) Math.pow(2, 31) - 1;
		for (int i = 1; i <= n; i++)
		{
			x = (a * x) % M;
		}
		x = (a * x) % M;
		int I1 = (int) Math.floor(x * 1.0 / M * n + 1);
		Map<String, Map<String, Object>> resultMap = new LinkedHashMap<String, Map<String, Object>>();
		Map<String, Object> map = newlist.get(1);
		map.put("RECODE", I1);

		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("orgCode", map.get("V_ORG_CODE"));
		param.setColumn("tpid", tpid);
		param.setColumn("V_ROLL_NO", map.get("NO"));
		// 更新投标人分配的号码
		getActiveRecordDAO().auto().table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_BIDDER_ORG_CODE=#{orgCode}")
				.setCondition("AND", "V_TPID=#{tpid}").modify(param);
		resultMap.put(String.valueOf(I1), map);
		int k = 1;
		while (k < n)
		{
			x = (a * x) % M;
			int IO = (int) Math.floor(x * 1.0 / M * n + 1);
			if (!resultMap.containsKey(String.valueOf(IO)))
			{
				param.clear();
				k++;
				map = newlist.get(k);

				param.setColumn("orgCode", map.get("V_ORG_CODE"));
				param.setColumn("V_ROLL_NO", map.get("NO"));
				param.setColumn("tpid", tpid);
				// 更新投标人分配的号码
				getActiveRecordDAO().auto().table(TableName.EKB_T_TENDER_LIST)
						.setCondition("AND", "V_BIDDER_ORG_CODE=#{orgCode}")
						.setCondition("AND", "V_TPID=#{tpid}").modify(param);

				map.put("RECODE", IO);
				resultMap.put(String.valueOf(IO), map);
			}
		}
		return resultMap;
	}

	/**
	 * 预选参与摇球投标人<br/>
	 * 
	 * @param x
	 *            x的初始值可以用随机顺序后的第一个投标人的报价（以万元为单位）的整数
	 * @param n
	 *            n个抽标人
	 * @param m
	 *            随机产生m个投标人
	 * @param recodeMap
	 *            编码的抽标人信息
	 * @return 选出来的投标人
	 * @throws ServiceException
	 *             ServiceException
	 */
	public Map<String, Map<String, Object>> sieve(long x, int n, int m,
			Map<String, Map<String, Object>> recodeMap) throws ServiceException
	{
		long a = 69069;
		long M = (long) Math.pow(2, 32) - 5;
		for (int i = 1; i <= m; i++)
		{
			x = (a * x) % M;
		}
		x = (a * x) % M;
		int J1 = (int) Math.floor(x * 1.0 / M * n + 1);

		Record<String, Object> param = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();
		param.setColumn("tpid", tpid);
		param.setColumn("N_ENVELOPE_0", "0");
		if (StringUtils.contains(SessionUtils.getTenderProjectTypeCode(),
				"jypbf"))
		{
			// 先把所有投标人转态设置为解密失败
			getActiveRecordDAO().auto().table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid}").modify(param);
		}
		else
		{
			// 先把所有投标人转态设置为解密失败
			getActiveRecordDAO().auto().table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID=#{tpid} AND N_ENVELOPE_9!=1")
					.modify(param);
		}

		Map<String, Map<String, Object>> resultMap = new LinkedHashMap<String, Map<String, Object>>();
		Map<String, Object> map = recodeMap.get(String.valueOf(J1));
		map.put("SELECT", true);
		resultMap.put(String.valueOf(J1), map);
		int k = 1;
		while (k < m)
		{
			x = (a * x) % M;
			int IO = (int) Math.floor(x * 1.0 / M * n + 1);
			if (!resultMap.containsKey(String.valueOf(IO)))
			{
				param.clear();
				k++;
				map = recodeMap.get(String.valueOf(IO));
				map.put("SELECT", true);
				resultMap.put(String.valueOf(IO), map);
			}
		}
		return resultMap;
	}

	private static String nextSeqByFour(int no)
	{
		DecimalFormat countFormat = new DecimalFormat("0000");
		return countFormat.format(no);
	}
}
