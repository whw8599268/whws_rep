/**
 * 包名：com.sozone.eokb.bus.decrypt.service
 * 文件名：BidderDocumentParseServiceAdapter.java<br/>
 * 创建时间：2017-10-30 下午3:49:58<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt.service;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.Auto.Table;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 投标文件解析服务接口适配器<br/>
 * <p>
 * 投标文件解析服务接口适配器<br/>
 * </p>
 * Time：2017-10-30 下午3:49:58<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
public abstract class BidderDocumentParseServiceAdapter implements
		BidderDocumentParseService
{

	private static final Map<String, String> ARABIC_CN_NUMS = new HashMap<String, String>();

	static
	{
		ARABIC_CN_NUMS.put("0", "零");
		ARABIC_CN_NUMS.put("1", "一");
		ARABIC_CN_NUMS.put("2", "二");
		ARABIC_CN_NUMS.put("3", "三");
		ARABIC_CN_NUMS.put("4", "四");
		ARABIC_CN_NUMS.put("5", "五");
		ARABIC_CN_NUMS.put("6", "六");
		ARABIC_CN_NUMS.put("7", "七");
		ARABIC_CN_NUMS.put("8", "八");
		ARABIC_CN_NUMS.put("9", "九");
		ARABIC_CN_NUMS.put("10", "十");
	}

	/**
	 * activeRecordDAO属性的get方法
	 * 
	 * @return the activeRecordDAO
	 */
	protected static ActiveRecordDAO getActiveRecordDAO()
	{
		return ActiveRecordDAOImpl.getInstance();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sozone.eokb.bus.decrypt.service.BidderDocumentParseService#
	 * getBidderDeliveryInfo(java.lang.String, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public List<Record<String, Object>> getBidderDeliveryInfo(String tpid,
			String unitfy, String orgCode) throws ServiceException
	{
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("tpid", tpid);
		params.setColumn("unify_code", unitfy);
		params.setColumn("org_code", orgCode);
		return getActiveRecordDAO().statement().selectList(
				"decrypt.getBidderDeliveryInfo", params);
	}

	/**
	 * 获取唱标要素JSON对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param pbXml
	 *            PublicInfomation xml
	 * @return 唱标要素JSON对象
	 * @throws ServiceException
	 *             服务异常
	 */
	@SuppressWarnings("unchecked")
	protected JSONArray getSingJsonObject(File pbXml) throws ServiceException
	{
		// System.out.println("文件全路径：" + pbXml.getAbsolutePath());
		// System.out.println("文件是否存在:" + pbXml.exists());
		// System.out.println("文件是否可执行：" + pbXml.canExecute());
		// System.out.println("文件是否可写：" + pbXml.canWrite());
		// System.out.println("文件是否可读：" + pbXml.canRead());
		try
		{
			// 创建SAXReader对象
			SAXReader reader = new SAXReader();
			// 读取文件 转换成Document
			Document document = reader.read(pbXml);
			// 得到根节点
			Element root = document.getRootElement();
			List<Element> pis = root.elements("PublicInfomation");
			if (!CollectionUtils.isEmpty(pis))
			{
				Element id = null;
				Element value = null;
				String idText = null;
				String valueText = null;
				for (Element pi : pis)
				{
					id = pi.element("id");
					value = pi.element("value");
					if (null == id || null == value)
					{
						continue;
					}
					idText = id.getText();
					valueText = value.getText();
					String sjson = null;
					if (StringUtils.equals("HtmlResult", idText))
					{
						sjson = new String(Base64.decodeBase64(valueText),
								ConstantEOKB.DEFAULT_CHARSET);
						return JSON.parseArray(sjson);
					}
				}
			}

		}
		catch (Exception e)
		{
			throw new ServiceException("", "解析PublicInfomation.xml发生异常!", e);
		}
		throw new ServiceException("", "获取不到CbData数据节点");

		// try
		// {
		// Document document = Jsoup
		// .parse(pbXml, ConstantEOKB.DEFAULT_CHARSET);
		// Elements elements = document.getElementsByTag("PublicInfomation");
		// Iterator<Element> iterator = elements.iterator();
		// while (iterator.hasNext())
		// {
		// Element next = iterator.next();
		// Elements ids = next.getElementsByTag("id");
		// Elements values = next.getElementsByTag("value");
		// if (ids.size() != 1 || values.size() != 1)
		// {
		// continue;
		// }
		// Element idEle = ids.first();
		// Element valueEle = values.first();
		// String id = idEle.html();
		// String value = valueEle.html();
		// String sjson = null;
		// if (StringUtils.equals("HtmlResult", id))
		// {
		// sjson = new String(Base64.decodeBase64(value),
		// ConstantEOKB.DEFAULT_CHARSET);
		// return JSON.parseArray(sjson);
		// }
		// }
		// }
		// catch (IOException e)
		// {
		// throw new ServiceException("", "解析PublicInfomation.xml发生异常!", e);
		// }
		// throw new ServiceException("", "获取不到CbData数据节点");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sozone.eokb.bus.decrypt.service.BidderDocumentParseService#
	 * parseBidderDocument(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void parseBidderDocument(String tpid, String unitfy, String orgCode)
			throws ServiceException
	{
		// // 评标办法编码
		// String bemCode = SessionUtils.getTenderProjectTypeCode();
		// 是否使用标段组
		boolean group = SessionUtils.isSectionGroup();
		// 投标人投递信息列表
		List<Record<String, Object>> bidderDeliverys = getBidderDeliveryInfo(
				tpid, unitfy, orgCode);
		if (CollectionUtils.isEmpty(bidderDeliverys))
		{
			return;
		}
		// 信封索引
		int index = 0;
		// 信封名称
		String envelopeName = null;
		// mac地址
		String mac = null;
		// 标段与投递信息对应图,标段编号---投递信息
		Map<String, Record<String, Object>> sectionDelivers = new HashMap<String, Record<String, Object>>();
		// 标段编号
		String sectionCode = null;
		// 信封标识
		String ftid = null;
		// 文件名称
		String fileName = null;
		// publicInfomation xml
		File publicInfomationXml = null;
		// 投递信息临时对象
		Record<String, Object> temp = null;
		String filePath = "";
		// cbdata xml 内容
		// String cbdata = null;
		// 唱标信息列表
		JSONArray array = null;
		// 迭代投递信息,构造投标人信息
		for (Record<String, Object> bidderDelivery : bidderDeliverys)
		{
			// 标段编号
			sectionCode = bidderDelivery.getString("V_BID_SECTION_CODE");
			// 获取同标段的投递信息
			temp = sectionDelivers.get(sectionCode);
			if (null == temp)
			{
				temp = bidderDelivery;
				sectionDelivers.put(sectionCode, temp);
			}
			// 信封标识,注意这里一定要用当前迭代的变量,不能用map中缓存值,因为信封不一样
			ftid = bidderDelivery.getString("V_FTID");
			// xml文件名,注意这里一定要用当前迭代的变量,不能用map中缓存值,因为信封不一样所有要解析的xml文件路径也不一样
			fileName = FilenameUtils.getBaseName(bidderDelivery
					.getString("V_FILEADDR")) + ".xml";
			// mac地址
			mac = bidderDelivery.getString("V_MAC");
			// xml文件绝对路径
			filePath = SystemParamUtils
					.getString(SysParamKey.EBIDKB_DECRYPTFILE_PATH_URL)
					+ sectionCode
					+ File.separator
					+ orgCode
					+ File.separator
					+ "PublicInfomation" + File.separator + fileName;
			publicInfomationXml = new File(filePath);
			// // 解析获取CbData xml内容
			// cbdata = getCbDataXml(publicInfomationXml);
			// 获取唱标要素json对象
			array = getSingJsonObject(publicInfomationXml);
			// 获取信封索引
			index = getEnvelopeIndex(ftid);
			envelopeName = getEnvelopeName(index);

			// mac地址信息
			Record<String, Object> macInfo = new RecordImpl<String, Object>();
			macInfo.setColumn("ENVELOPE_INDEX", index);
			macInfo.setColumn("ENVELOPE_NAME", envelopeName);
			macInfo.setColumn("MAC", mac);

			// // 当前信封的唱标数据
			// Record<String, Object> sing = BidderElementParseUtils
			// .parseBidderDocument(bemCode, cbdata, index);
			// 获取唱标对象
			Record<String, Object> sobj = temp.getColumn("SingObj");
			// mac地址列表
			List<Record<String, Object>> macInfos = null;
			// 如果不存在
			if (null == sobj)
			{
				sobj = new RecordImpl<String, Object>();
				sobj.setColumn("objSing", array);
				temp.setColumn("SingObj", sobj);
				macInfos = new LinkedList<Record<String, Object>>();
				macInfos.add(macInfo);
				sobj.setColumn("macs", macInfos);
			}
			else
			{
				// 如果存在合并
				macInfos = sobj.getColumn("macs");
				macInfos.add(macInfo);
				sobj.setColumn("macs", macInfos);
				// 上一个信封
				JSONArray previous = sobj.getColumn("objSing");
				previous.addAll(array);
				sobj.setColumn("objSing", previous);
				temp.setColumn("SingObj", sobj);
			}
		}
		// 开始迭代
		Record<String, Object> record = null;
		// 参数
		Record<String, Object> params = new RecordImpl<String, Object>();
		for (String scode : sectionDelivers.keySet())
		{
			// 情况
			params.clear();
			// 投递信息
			temp = sectionDelivers.get(scode);
			// 非标段组的话插入解密信息表 标段的话直接插入投标人信息表
			if (!group)
			{
				// 构建非标段租的对象
				record = buildBidderInfo(temp, group);
				params.setColumn("sectionid",
						record.getString("V_BID_SECTION_ID"));
				params.setColumn("boc", record.getString("V_BIDDER_ORG_CODE"));
				// 先删除再插入
				getActiveRecordDAO()
						.auto()
						.table(TableName.EKB_T_TENDER_LIST)
						.setCondition("AND",
								"V_BID_SECTION_ID = #{sectionid} AND V_BIDDER_ORG_CODE = #{boc}")
						.remove(params);
				getActiveRecordDAO().auto().table(TableName.EKB_T_TENDER_LIST)
						.save(record);
				continue;
			}

			// 标段组情况直接插入解密表
			record = buildBidderInfo(temp, group);
			// 先删除再插入
			getActiveRecordDAO()
					.auto()
					.table(TableName.EKB_T_DECRYPT_INFO)
					.setCondition("AND",
							"V_BID_SECTION_ID = #{sectionid} AND V_BIDDER_ORG_CODE = #{boc}")
					.remove(params);
			getActiveRecordDAO().auto().table(TableName.EKB_T_DECRYPT_INFO)
					.save(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sozone.eokb.bus.decrypt.service.BidderDocumentParseService#
	 * parseBidderDocument()
	 */
	@Override
	public void parseBidderDocument() throws ServiceException
	{
		// 招标项目ID
		String tpid = SessionUtils.getTPID();
		// 统一社会信用代码
		String unitfy = SessionUtils.getSocialcreditNO();
		// 组织机构代码
		String orgcode = SessionUtils.getCompanyCode();
		parseBidderDocument(tpid, unitfy, orgcode);
	}

	/**
	 * 获取信封名称<br/>
	 * <p>
	 * </p>
	 * 
	 * @param index
	 *            信封索引
	 * @return 信封名称
	 */
	protected String getEnvelopeName(int index)
	{
		if (-1 == index)
		{
			return "投标文件";
		}
		index++;
		String cnNum = ARABIC_CN_NUMS.get(index + "");
		return "第" + cnNum + "数字信封";
	}

	/**
	 * 获取投递文件信封索引<br/>
	 * <p>
	 * </p>
	 * 
	 * @param ftid
	 *            文件id
	 * @return 索引位置
	 */
	protected int getEnvelopeIndex(String ftid)
	{
		// 投标文件
		if (StringUtils.equals(ConstantEOKB.BIDDER_DOC_ENVELOPE_TAG, ftid)
				|| StringUtils
						.equals(ConstantEOKB.XM_FJSZ_BIDDER_DOC_TAG, ftid))
		{
			return -1;
		}
		// 第一信封
		if (StringUtils.equals(ConstantEOKB.FIRST_ENVELOPE_TAG, ftid)
				|| StringUtils.equals(ConstantEOKB.SHARE_FIRST_ENVELOPE_TAG,
						ftid))
		{
			return 0;
		}
		// 第二信封
		if (StringUtils.equals(ConstantEOKB.SECOND_ENVELOPE_TAG, ftid))
		{
			return 1;
		}
		// 如果是第三信封
		if (StringUtils.equals(ConstantEOKB.THIRD_ENVELOPE_TAG, ftid))
		{
			return 2;
		}
		return 0;
	}

	/**
	 * 构建投标人信息对象<br/>
	 * <p>
	 * </p>
	 * 
	 * @param bidderDelivery
	 *            投递信息对象
	 * @param group
	 *            是否为标段组
	 * @return 投标人信息
	 * @throws ServiceException
	 *             服务异常
	 */
	protected Record<String, Object> buildBidderInfo(
			Record<String, Object> bidderDelivery, boolean group)
			throws ServiceException
	{
		// 唱标要素
		Record<String, Object> sings = bidderDelivery.getColumn("SingObj");
		String userID = ApacheShiroUtils.getCurrentUserID();
		String tpid = SessionUtils.getTPID();
		String unitfy = SessionUtils.getSocialcreditNO();
		String orgcode = SessionUtils.getCompanyCode();
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("ID", Random.generateUUID());
		record.setColumn("V_TPID", SessionUtils.getTPID());
		record.setColumn("V_BID_SECTION_ID",
				bidderDelivery.getString("V_BID_SECTION_ID"));
		record.setColumn("V_BID_SECTION_CODE",
				bidderDelivery.getString("V_BID_SECTION_CODE"));
		record.setColumn("V_BID_SECTION_GROUP_CODE",
				bidderDelivery.getString("V_BID_SECTION_GROUP_CODE"));
		// 投标人组织机构代码
		record.setColumn("V_BIDDER_ORG_CODE",
				bidderDelivery.getString("V_ORG_CODE"));
		// 投标人名称
		String bidderName = BidderElementParseUtils.getSingObjAttribute(
				"tbrmc", sings);
		
		//去前导与尾部空格
		bidderName = StringUtils.trim(bidderName);
		// 不能从投递表中去取，为了防止出现联合体投标情况
		// record.setColumn("V_BIDDER_NAME",
		// bidderDelivery.getString("V_BIDDER_NAME"));
		record.setColumn("V_BIDDER_NAME", bidderName);
		// 作废临时状态标识(1正常,0作废)
		record.setColumn("N_VOIDTAGTMP", "1");
		record.setColumn("N_ENVELOPE_0", "1");

		// 投标报价
		String tbbj = BidderElementParseUtils
				.getSingObjAttribute("tbbj", sings);
		if (StringUtils.isEmpty(tbbj))
		{
			throw new ServiceException("", "无法获取到投标文件中的投标报价!");
		}
		// 投标报价
		double price = Double.parseDouble(tbbj);
		record.setColumn("N_PRICE", price);
		// 投标人信用等级
		String tbRatingsInEvl = BidderElementParseUtils.getSingObjAttribute(
				"tbRatingsInEvl", sings);
		// 投标人编号
		String bidNo = generateBidderNo(tbRatingsInEvl,
				bidderDelivery.getString("V_DELIVER_NO"));
		record.setColumn("V_BIDDER_NO", bidNo);
		// 递交文件顺序号
		record.setColumn("N_SORT_FILE_BID",
				getSortFileBid(bidderDelivery.getString("V_DELIVER_NO")));
		// 关联企业编号
		record.setColumn("V_CORRELATE_CODE",
				getCorrelate(unitfy, orgcode, tpid));

		// JSON信息
		record.setColumn("V_JSON_OBJ", JSON.toJSONString(sings));
		record.setColumn("V_CREATE_USER", userID);
		record.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		record.setColumn("V_UPDATE_USER", userID);
		record.setColumn("N_UPDATE_TIME", System.currentTimeMillis());
		return record;
	}

	/**
	 * 获取关联企业编号
	 * 
	 * @param unitfy
	 *            统一社会信用代码
	 * @param orgcode
	 *            组织机构代码
	 * @param tpid
	 *            招标项目ID
	 * @return 关联企业编号
	 * @throws ServiceException
	 *             服务异常
	 */
	protected String getCorrelate(String unitfy, String orgcode, String tpid)
			throws ServiceException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		Table table = getActiveRecordDAO().auto()
				.table(TableName.EKB_T_CORRELATE_ENTER)
				.setCondition("AND", "V_TPID = #{tpid}");
		if (StringUtils.isNotEmpty(unitfy))
		{
			if (StringUtils.isNotEmpty(orgcode))
			{
				table.setCondition("AND",
						"(V_UNIFY_CODE = #{unitfy} OR V_ORG_CODE = #{orgcode})");
				param.setColumn("unitfy", unitfy);
				param.setColumn("orgcode", orgcode);
			}
			else
			{
				table.setCondition("AND", "V_UNIFY_CODE = #{unitfy}");
				param.setColumn("unitfy", unitfy);
			}
		}
		else
		{
			if (StringUtils.isNotEmpty(orgcode))
			{
				table.setCondition("AND", "V_ORG_CODE = #{orgcode}");
				param.setColumn("orgcode", orgcode);
			}
			else
			{
				throw new ServiceException("", "组织机构代码或统一社会代码不能为空!");
			}
		}
		Record<String, Object> record = table.get(param);
		if (CollectionUtils.isEmpty(record))
		{
			return "";
		}
		return StringUtils.trimToEmpty(record.getString("V_CORRELATE_CODE"));
	}

	/**
	 * 
	 * 获取递交文件顺序号<br/>
	 * <p>
	 * 获取递交文件顺序号
	 * </p>
	 * 
	 * @param deliverNo
	 *            投递顺序号(ekb_t_tbimportbidding)
	 * @return 顺序号
	 */
	protected int getSortFileBid(String deliverNo)
	{
		int sortFileBid = 0;
		try
		{
			sortFileBid = Integer.valueOf(deliverNo);
		}
		catch (Exception e)
		{
			sortFileBid = 0;
		}
		return sortFileBid;
	}

	/**
	 * 
	 * 投标人编号生成<br/>
	 * <p>
	 * 投标人编号生成
	 * </p>
	 * 
	 * @param creditRatingCode
	 *            信用等级 AA-1 A-2 B-3 C-4 D-5
	 * @param deliverNo
	 *            投递编号
	 * @return 投标人编号
	 * @throws ServiceException
	 *             服务异常
	 */
	protected String generateBidderNo(String creditRatingCode, String deliverNo)
			throws ServiceException
	{
		if (StringUtils.isEmpty(creditRatingCode)
				|| StringUtils.isEmpty(deliverNo))
		{
			throw new ServiceException("", "投标人编号生成错误！");
		}
		if (StringUtils.equals("AA", creditRatingCode))
		{
			return "1-" + deliverNo;
		}
		else if (StringUtils.equals("A", creditRatingCode))
		{
			return "2-" + deliverNo;
		}
		else if (StringUtils.equals("B", creditRatingCode))
		{
			return "3-" + deliverNo;
		}
		else if (StringUtils.equals("C", creditRatingCode))
		{
			return "4-" + deliverNo;
		}
		else if (StringUtils.equals("D", creditRatingCode))
		{
			return "5-" + deliverNo;
		}
		else
		{
			throw new ServiceException("", "投标人编号生成错误！");
		}
	}

}
